package com.dic.acsbtscdemo

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import com.acs.bluetooth.Acr1255uj1Reader
import com.acs.bluetooth.BluetoothReader
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Logic for various smartcard capability
 * @author DIC
 * @version 1.0.0
 */
class SmartTagCore(messageHandler: Handler){
    private val builder = CommandBuilder()
    private val felica = FelicaCommand()
    private lateinit var idm:ByteArray
    private lateinit var cmdList:ArrayList<ByteArray>
    private var cmdListCount = 0
    private var cmdErrorCount = 0
    lateinit var bmpByte:ByteArray
    var inputText:String = ""
    private val BLOCK_SIZE = 16
    private var readSize = 0

    enum class MessageType(val value:Int){ Toast(0), EditBox(1), Progress(2), Error(3)}
    enum class Process{ Nothing, ShowDemo, ShowImage, ClearDisplay, WriteData, ReadUserData, CardResponse, ProcessRead, DisplayImage }
    private enum class PreProcess{GetIdm, CheckStatus, Nothing, ProcessingLongTask}

    var process = Process.Nothing
    private var prep = PreProcess.Nothing
    private val mHandler = messageHandler

    //init defines all logic process that the smart card to handle when running the app
    init{
        builder.maxBlocks = 12
        (BluetoothInstance.reader as Acr1255uj1Reader).setOnResponseApduAvailableListener { reader: BluetoothReader, apdu: ByteArray, errorCode: Int ->
            when(prep){
                PreProcess.GetIdm ->{
                    idm = apdu
                    checkStatus()

                }
                PreProcess.CheckStatus ->{

                    val byte0 = apdu[apdu.count()-1]
                    val byte1 = apdu[apdu.count()-2]

                    if(byte0 == 0x00.toByte() && byte1 == 0x00.toByte()){
                        when(process) {
                            Process.ShowDemo -> showDemo()
                            Process.ShowImage -> showImage()
                            Process.ClearDisplay -> clearDisplay()
                            Process.WriteData -> writeData()
                            Process.ReadUserData -> readUserDataCmd(0, 176)
                            Process.DisplayImage -> showImage()
                            else -> {}
                        }
                    }
                    else if(byte0 == 0x00.toByte() && byte1 == 0x90.toByte()){
                        when(process){
                            Process.CardResponse ->{
                                cardResponseReady()
                                Process.ProcessRead
                            }
                            Process.ProcessRead -> {
                                processRead(apdu)
                                processCompleted()
                            }
                            Process.Nothing->{}
                            else -> {}
                        }
                    }
                    else{
                        val msg = mHandler.obtainMessage(MessageType.Toast.value, "Status error")
                        msg.sendToTarget()
                    }
                }
                PreProcess.Nothing ->{ }
                PreProcess.ProcessingLongTask ->{
                    //this process is used for one batch of command sequences with multiples of individual command
                    //awaits for return apdu and checks for no error
                    val byte0 = apdu[apdu.count()-1]
                    val byte1 = apdu[apdu.count()-2]
                    //check if there's error in response apdu
                    if(byte0 == 0x00.toByte() && byte1 == 0x00.toByte()) {
                        if(cmdListCount < cmdList.size){
                            cmdErrorCount = 0
                            cmdListCount++
                            showImageBytesSend(idm, cmdList[cmdListCount])
                        }
                        else{
                            prep = PreProcess.Nothing
                            processCompleted()
                        }
                        val pcount = (cmdListCount*100/44)
                        if(cmdListCount < 43)
                            progressUpdate(pcount)
                        else
                            progressUpdate(100)
                    }
                    else if(cmdListCount > 5){
                        prep = PreProcess.Nothing
                        val msg = mHandler.obtainMessage(MessageType.Error.value, "Command send error!")
                        msg.sendToTarget()
                    }
                    else{
                        //resend same command if there is error
                        showImageBytesSend(idm, cmdList[cmdListCount])
                        cmdErrorCount++
                    }
                }
            }
        }
    }

    private var demoCount = 0
    private fun demoNumber():Byte{
        if(demoCount<3)
            demoCount++
        else
            demoCount = 0
        return demoCount.toByte()
    }

    //Always 1st command send before any process begin
    fun startProcess(){
        //get idm
        val cmd = BluetoothInstance.byteArrayOfInts(0xFF, 0xCA, 0x00, 0x00, 0x00)
        BluetoothInstance.reader!!.transmitApdu(cmd)
        prep = PreProcess.GetIdm
    }

    //Always 2nd command send before any process begin
    private fun checkStatus(){
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val cmd = builder.buildCommand(CommandBuilder.COMMAND_CHECK_STATUS, param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
        prep = PreProcess.CheckStatus
    }

    //smart card to show demo image with running number
    private fun showDemo(){
        val msg = mHandler.obtainMessage(MessageType.Toast.value, "Demo image $demoCount")
        msg.sendToTarget()
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val cmd = builder.buildCommand((CommandBuilder.COMMAND_SHOW_DEMO + demoNumber()).toByte(), param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
        processCompleted()

    }

    //smart card to show bmp image. Uses long process to transmit a sequence of 44 byte array commands
    private fun showImage(){
        val msg = mHandler.obtainMessage(MessageType.Toast.value, "Writing image data")
        msg.sendToTarget()

        val pos = convertTo3Bytes(0,0)
        val size = convertTo3Bytes(300, 200)
        val param = byteArrayOf(pos[0], pos[1], pos[2], size[0], size[1], size[2], 0x00, 0x03)
        cmdList = builder.buildCommand(CommandBuilder.COMMAND_SHOW_DISPLAY3, param, bmpByte)
        cmdListCount = 0
        cmdErrorCount = 0
        prep = PreProcess.ProcessingLongTask
        showImageBytesSend(idm, cmdList[cmdListCount])
        //0% show progress dialogue
        progressUpdate(0)
    }

    //Used by long process to transmit only one set of byte array command
    private fun showImageBytesSend(idm:ByteArray, cmdListElement:ByteArray){
        val fcmd = felica.createPacketForWrite(idm,cmdListElement)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
    }

    //clears the e-paper display
    private fun clearDisplay(){
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val cmd = builder.buildCommand(CommandBuilder.COMMAND_CLEAR, param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
        processCompleted()
    }

    //writes text byte representation into smart card memory
    private fun writeData(){
        val param = ByteArray(128){0x00}
        val text = inputText.toByteArray(Charsets.UTF_8)

        //clears first 128 bytes of previously in memory chars. Chars after that will not be overwritten
        val lastIndex = when {
            inputText.length < 128 -> inputText.length
            else -> 128
        }
        text.copyInto(param,0,0,lastIndex)
        val cmd = builder.buildDataWriteCommand(0, param)
        for(i in cmd){
            val fcmd = felica.createPacketForWrite(idm, i)
            BluetoothInstance.reader!!.transmitApdu(fcmd)
        }
        inputText = ""
        processCompleted()
    }

    //read user written data from memory
    private fun readUserDataCmd(startAddress:Int, sizeToRead:Int){

        val maxReadLength = (builder.maxBlocks * BLOCK_SIZE) - BLOCK_SIZE
        val splitcount = (sizeToRead + maxReadLength - 1)/maxReadLength
        var dataLen:Int
        if(sizeToRead > maxReadLength)
            dataLen = maxReadLength
        else
            dataLen = sizeToRead

        var offset = 0
        var address = startAddress

        for(i in 0 until splitcount){
            if(i == splitcount){
                dataLen = sizeToRead - offset
            }
            readUserDataByBlock(address.toByte(), dataLen)
            offset += dataLen
            address += dataLen
        }
        process = Process.CardResponse
    }

    private fun readUserDataByBlock(readPos:Byte, size:Int){

        readSize = size
        //address
        val hAByte = (readPos.toInt() shl 8).toByte()
        val lAByte = readPos and 0x00FF.toByte()

        //length
        val hLByte = (size shl 8).toByte()
        val lLByte = size.toByte() and 0x00FF.toByte()

        //set function to parameter data
        val param = byteArrayOf(hAByte, lAByte,hLByte, lLByte, 0x00, 0x00, 0x00, 0x00)

        val cmd = builder.buildCommand(CommandBuilder.COMMAND_DATA_READ, param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!,false)
        var readcmd = byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
        readcmd[4] = (fcmd.count() + 1).toByte()
        readcmd[5] = (fcmd.count() + 1).toByte()
        readcmd += fcmd

        BluetoothInstance.reader!!.transmitApdu(readcmd)
    }

    private fun cardResponseReady(){

        val blocks = (readSize + BLOCK_SIZE - 1)/ BLOCK_SIZE
        val fcmd = felica.createPacketForRead(idm, blocks + 1, false)
        var readcmd = byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
        readcmd[4] = (fcmd.count() + 1).toByte()
        readcmd[5] = (fcmd.count() + 1).toByte()
        readcmd += fcmd

        BluetoothInstance.reader!!.transmitApdu(readcmd)
        process = Process.ProcessRead
    }

    //sends data stored in smart card memory to UI thread and display it in alert box
    private fun processRead(data:ByteArray){
        if(data[data.count()-2] == 0x90.toByte() && data[data.count()-1] == 0x00.toByte()){

            val result = data.copyOfRange(18, data.count()-2)
            val msg = mHandler.obtainMessage(MessageType.EditBox.value, result)
            msg.sendToTarget()
        }
        processCompleted()
    }

    private fun processCompleted(){
        prep = PreProcess.Nothing
        process = Process.Nothing
        val msg = mHandler.obtainMessage(MessageType.Toast.value, "Completed")
        msg.sendToTarget()
    }

    //send realtime progress data to ui thread
    private fun progressUpdate(num:Int){
        val msg = mHandler.obtainMessage(MessageType.Progress.value, num)
        msg.sendToTarget()
    }

    //convert 2 integers into 3 bytes. Uses to convert position and dimension
    private fun convertTo3Bytes(a: Int, b: Int): ByteArray {
        val result = ByteArray(3)
        result[0] = (a and 0x000FFF shr 4).toByte()

        var wk1 = (a and 0x0000000F shl 4).toByte()
        wk1 = wk1 or (b and 0x00000F00 shr 8).toChar().toByte()
        result[1] = wk1

        result[2] = (b and 0x000000FF).toByte()

        return result
    }

    //for debugging
    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }


}

