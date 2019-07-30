package com.dic.acsbtscdemo

class FelicaCommand {

    val BLOCKSIZE:Int = 16

    fun createPacketForWrite(idm:ByteArray, blockData:ByteArray):ByteArray{
        return createPacketForWrite(idm, blockData, true)
    }

    fun createPacketForWrite(idm:ByteArray, blockData:ByteArray, addLength:Boolean):ByteArray{
        var blocks:Int = (blockData.count() + BLOCKSIZE - 1)/BLOCKSIZE

        //3-byte block list
        var blockList = ByteArray(blocks*3)
        for (i in 0 until blocks){
            blockList[i*3] = 0x00
            blockList[i*3+1] = i.toByte()
            blockList[i*3+2] = 0x04
        }

        var len = 13 + blockList.count() + (BLOCKSIZE * blocks)

        if(addLength){
            len+=1
        }

        var packet = ByteArray(len)
        var pos:Int = 0
        if(addLength){
            packet[0] = len.toByte()
            pos+=1
        }
        packet[pos] = 0x08
        pos+=1

        //idm
        for(i in 0 until idm.count()){
            packet[pos+i] = idm[i]
        }
        pos+=8

        //service count
        packet[pos] = 0x01
        pos+=1

        //service code
        packet[pos] = 0x09
        packet[pos+1] = 0x00
        pos+=2

        //block count
        packet[pos] = blocks.toByte()
        pos+=1

        //block list
        for(i in 0 until blockList.count()){
            packet[pos] = blockList[i]
            pos+=1
        }

        //blockdata
        for(i in 0 until blockData.count()){
            packet[pos] = blockData[i]
            pos+=1
        }
        return packet
    }

    fun createPacketForRead(idm: ByteArray, blocks:Int):ByteArray{
        return createPacketForRead(idm, blocks, true)
    }

    fun createPacketForRead(idm: ByteArray, blocks:Int, addLength:Boolean):ByteArray{

        //3 byte block list
        var blockList = ByteArray(blocks*3)
        for(i in 0 until blocks){
            blockList[i*3] = 0x00
            blockList[i*3+1] = i.toByte()
            blockList[i*3+2] = 0x04
        }

        var len = 13 + blockList.count()

        if(addLength){
            len+=1
        }
        var packet = ByteArray(len)
        var pos:Int = 0
        if(addLength){
            packet[0] = len.toByte()
            pos = 1
        }
        packet[pos] = 0x06
        pos+=1

        //idm
        for(i in 0 until idm.count()){
            packet[pos+i] =idm[i]
        }
        pos+=8

        //service count
        packet[pos] = 0x01
        pos+=1

        //service code
        packet[pos] = 0x09
        packet[pos+1] = 0x00
        pos+=2

        //block count
        packet[pos] = blocks.toByte()
        pos+=1

        //block list
        for(i in 0 until blockList.count()){
            packet[pos] = blockList[i]
            pos+=1
        }

        return packet
    }

    fun getBlockData(response:ByteArray):ByteArray?{
        return getBlockData(response, true)
    }

    fun getBlockData(response:ByteArray, withLength:Boolean):ByteArray?{
        var minLen = 12
        if(withLength){
            minLen = 13
        }
        if(response.count()<minLen){
            return null
        }
        val blockCount = (response[minLen-1]).toInt()
        var blockData = ByteArray(blockCount*BLOCKSIZE)

        if(response.count()<(minLen+blockData.count())){
            return null
        }

        for(i in 0 until blockData.count()){
            blockData[i] = response[minLen+i]
        }
        return blockData
    }

    fun getPollingCommand():ByteArray{
        return getPollingCommand(true)
    }

    fun getPollingCommand(addLength: Boolean):ByteArray{
        return when(addLength){
            true -> BluetoothInstance.byteArrayOfInts(0x06, 0x00, 0xFF, 0xFF,0x00, 0x00)
            false -> BluetoothInstance.byteArrayOfInts(0x00, 0xFF, 0xFF, 0x00, 0x00)
        }
    }
}