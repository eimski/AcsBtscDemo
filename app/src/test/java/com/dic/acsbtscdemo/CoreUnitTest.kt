package com.dic.acsbtscdemo

import org.junit.Assert
import org.junit.Test

class CoreUnitTest{


    @Test
    //write to memory block
    fun testCommandBuilder_buildDataWriteCommand(){
        val addrs = 0
        val data = BluetoothInstance.byteArrayOfInts(0x48, 0x65, 0x6C,0x6C, 0x6F, 0x20, 0x57, 0x6F, 0x72, 0x6C, 0x064)
        val cb = CommandBuilder()
        val result = cb.buildDataWriteCommand(addrs,data)
        val correct:ByteArray = BluetoothInstance.byteArrayOfInts(0xB0, 0x01, 0x01, 0x0B, 0x01, 0x30, 0x30, 0x30, 0x00, 0x00, 0x00,
            0x0B, 0x00, 0x00, 0x00, 0x00, 0x48, 0x65, 0x6C, 0x06C, 0x6F, 0x20,
            0x57, 0x6F, 0x72, 0x6C, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00)

        Assert.assertArrayEquals(correct, result[0])
    }

    @Test
    //generic command
    fun testCommandBuilder_buildCommand1(){
        val cb = CommandBuilder()
        val param = ByteArray(8)
        val res:ByteArray = cb.buildCommand(0xD0.toByte(), param)!!
        val correct:ByteArray = BluetoothInstance.byteArrayOfInts(0xD0, 0x01, 0x01, 0x00, 0x00, 0x30, 0x30, 0x30,
                                                                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        Assert.assertArrayEquals(correct, res)
    }

    @Test
    //simulate function data is null
    fun testCommandBuilder_buildCommand2(){
        val cb = CommandBuilder()
        val functionData = ByteArray(5808){0x1E}
        val param = BluetoothInstance.byteArrayOfInts(0x00, 0x00, 0x00, 0x10, 0x80, 0xB0, 0x00, 0x03)
        val res = cb.buildCommand(0xA3.toByte(), param, functionData)

        //test for one array cell. set next seqNo[4] to 0x0e for test
        var correct:ByteArray = BluetoothInstance.byteArrayOfInts(0xA3, 0x21, 0x0E, 0xB0, 0x0E, 0x30, 0x30, 0x30,
                                                                        0x00, 0x00, 0x00, 0x10, 0x80, 0xB0, 0x00, 0x03)
        val buffer = ByteArray(176){0x1E}
        correct+=buffer

        Assert.assertArrayEquals(correct, res[13])

    }

    @Test
    fun testFelicaCommand_createPacketForWrite(){
        val idm = BluetoothInstance.byteArrayOfInts(0x02, 0xFE, 0x00, 0x00,0x31, 0x00, 0x14, 0x59)
        val blockData = BluetoothInstance.byteArrayOfInts(0xD0, 0x01, 0x01, 0x00, 0x00, 0x30, 0x30, 0x30,
                                                            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val correct = BluetoothInstance.byteArrayOfInts(0x08, 0x02, 0xFE, 0x00, 0x00, 0x31, 0x00, 0x14,
                                                            0x59, 0x01, 0x09, 0x00, 0x01, 0x00, 0x00, 0x04,
                                                            0xD0, 0x01, 0x01, 0x00, 0x00, 0x30, 0x30, 0x30,
                                                            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val res = FelicaCommand().createPacketForWrite(idm, blockData, false)
        Assert.assertArrayEquals(correct, res)
    }

    @Test
    fun testFelicaCommand_createPacketForRead(){
        val idm = BluetoothInstance.byteArrayOfInts(0x02, 0xFE, 0x00, 0x00,0x31, 0x00, 0x14, 0x59)
        val correct = BluetoothInstance.byteArrayOfInts(0x06, 0x02, 0xFE, 0x00, 0x00, 0x31, 0x00, 0x14,
                                                                0x59, 0x01, 0x09, 0x00, 0x02, 0x00, 0x00, 0x04,
                                                                0x00, 0x01, 0x04)
        val res = FelicaCommand().createPacketForRead(idm, 2, false)
        Assert.assertArrayEquals(correct, res)
    }

    @Test
    fun testFelicaCommand_getBlockData(){
        val response = BluetoothInstance.byteArrayOfInts(0x2D, 0x07, 0x02, 0xFE, 0x00, 0x00, 0x31, 0x00,
                                                                0x14, 0x59, 0x00, 0x00, 0x02, 0xc0, 0x01, 0x01,
                                                                0xf0, 0x04, 0x00, 0x00, 0x00, 0x4D, 0x00, 0x00,
                                                                0x00, 0x35, 0x20, 0x00, 0x50, 0x48, 0x65, 0x6C,
                                                                0x6C, 0x6F, 0x20, 0x57, 0x6f, 0x72, 0x6C, 0x64,
                                                                0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val correct = BluetoothInstance.byteArrayOfInts(0xC0, 0x01, 0x01, 0xF0, 0x04, 0x00, 0x00, 0x00,
                                                                0x4D, 0x00, 0x00, 0x00, 0x35, 0x20, 0x00, 0x50,
                                                                0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x57, 0x6F,
                                                                0x72, 0x6C, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00)

        val res = FelicaCommand().getBlockData(response, true)

        Assert.assertArrayEquals(correct, res)
    }
}