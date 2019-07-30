package com.dic.acsbtscdemo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.acs.bluetooth.BluetoothReader


class SmartTagCore{
    val builder = CommandBuilder()
    val felica = FelicaCommand()
    private lateinit var idm:ByteArray


    enum class Process{ Complete, ShowDemo, ShowImage, ClearDisplay, WriteData, ReadUserData }
    private enum class PreProcess{GetIdm, CheckStatus, Nothing}

    var process = Process.Complete
    private var prep = PreProcess.Nothing

    init{
        builder.maxBlocks = 12
        BluetoothInstance.reader!!.setOnResponseApduAvailableListener { reader: BluetoothReader, apdu: ByteArray, errorCode: Int ->

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
                            Process.ReadUserData -> readUserData()
                            Process.Complete -> processCompleted()
                        }
                    }
                    else{
                        //TODO:return error to UI
                    }
                }
                PreProcess.Nothing ->{ }
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

    fun startProcess(){
        //get idm
        val cmd = BluetoothInstance.byteArrayOfInts(0xFF, 0xCA, 0x00, 0x00, 0x00)
        BluetoothInstance.reader!!.transmitApdu(cmd)
        prep = PreProcess.GetIdm
    }

    private fun checkStatus(){
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val cmd = builder.buildCommand(CommandBuilder.COMMAND_CHECK_STATUS, param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
        prep = PreProcess.CheckStatus
    }

    private fun showDemo(){
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val cmd = builder.buildCommand((CommandBuilder.COMMAND_SHOW_DEMO + demoNumber()).toByte(), param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
        prep = PreProcess.Nothing
        //process = Process.Complete

    }

    private fun showImage(){

    }

    private fun clearDisplay(){
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val cmd = builder.buildCommand(CommandBuilder.COMMAND_CLEAR, param)
        val fcmd = felica.createPacketForWrite(idm, cmd!!)
        BluetoothInstance.reader!!.transmitApdu(fcmd)
        prep = PreProcess.Nothing
    }

    private fun writeData(){

    }

    private fun readUserData(){

    }

    private fun processCompleted(){
       //BluetoothInstance.disablePolling()
    }

    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}

