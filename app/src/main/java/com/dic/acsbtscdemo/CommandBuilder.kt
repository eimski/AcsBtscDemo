package com.dic.acsbtscdemo

import java.util.ArrayList

class CommandBuilder {

    var maxBlocks = 12
    private var mSeq: Byte = 1

    private var mSecCode1: ByteArray? = null
    private var mSecCode2: ByteArray? = null
    private var mSecCode3: ByteArray? = null

    private val nextSeq: Byte
        get() {
            val result = mSeq
            if(mSeq == 255.toByte()){
                mSeq = 1
            }
            else{
                mSeq++
            }

            return result
        }

    init {
        mSecCode1 = byteArrayOf(0x30, 0x30, 0x30)
        mSecCode2 = byteArrayOf(0x30, 0x30, 0x30)
        mSecCode3 = byteArrayOf(0x30, 0x30, 0x30)
    }

    fun setSecurityCode1(code: ByteArray) {
        mSecCode1 = code
    }

    fun setSecurityCode2(code: ByteArray) {
        mSecCode2 = code
    }

    fun setSecurityCode3(code: ByteArray) {
        mSecCode3 = code
    }

    /**
     * Creates a command when there is no function data.
     * @param functionNo
     * @param paramData
     * @return
     */
    fun buildCommand(
        functionNo: Byte,
        paramData: ByteArray
    ): ByteArray? {
        val list = buildCommand(
            functionNo,
            paramData, null
        )
        return if (list.isEmpty()) {
            null
        } else {
            list[0]
        }
    }

    /***
     * Creates smart-tag command.
     * Divides command into some frames if necessary.
     * @param functionNo
     * @param paramData
     * @param innerData
     * @return
     * @throws Exception
     */
    fun buildCommand(
        functionNo: Byte,
        paramData: ByteArray,
        functionData: ByteArray?
    ): ArrayList<ByteArray> {

        var dataBlocks: Int
        var innerData: ByteArray? = null
        var splitCount = 0
        if (functionData == null) {
            dataBlocks = 0
            splitCount = 1
        } else {
            dataBlocks = functionData.size / 16
            if (functionData.size % 16 > 0) {
                //functionDataは16で割り切れるようにする
                dataBlocks++
                //innerData = ByteArray(dataBlocks * 16) error here
                innerData!!.plus(functionData)
                System.arraycopy(functionData, 0, innerData, 0, functionData.size)
            } else {
                innerData = functionData
            }
            //フレーム分割数
            splitCount = getSplitCount(dataBlocks)
        }

        val result = ArrayList<ByteArray>()
        var offset = 0
        var frameBlocks = 0
        for (i in 0 until splitCount) {
            val dataLen: Int
            if (i == splitCount - 1) {
                //last frame
                frameBlocks = getLastBlockCount(dataBlocks) + 1
                //Common.addLogi(String.format("last block count = %d", frameBlocks));
                if (innerData == null) {
                    dataLen = 0
                } else {
                    dataLen = innerData.size - offset
                }
            } else {
                frameBlocks = maxBlocks
                dataLen = (frameBlocks - 1) * 16
            }

            val cmd = ByteArray(frameBlocks * 16)
            cmd[0] = functionNo
            cmd[1] = splitCount.toByte()
            cmd[2] = (i + 1).toByte()
            cmd[3] = ((frameBlocks - 1) * 16).toByte()

            if (functionNo == 0xd0.toByte()) {
                cmd[4] = 0
            } else {
                cmd[4] = nextSeq
            }

            //security code
            val secCode = getSecurityCode(functionNo, paramData)
            if (secCode != null) {
                cmd[5] = secCode[0]
                cmd[6] = secCode[1]
                cmd[7] = secCode[2]
            } else {
                cmd[5] = 0x30
                cmd[6] = 0x30
                cmd[7] = 0x30
            }

            //set function parameter data.
            System.arraycopy(paramData, 0, cmd, 8, paramData.size)

            //set function data.
            if (innerData != null) {
                System.arraycopy(innerData, offset, cmd, 16, dataLen)
            }
            //Common.addLogi("command: " +  Common.makeHexText(cmd));
            result.add(cmd)
            offset += dataLen
        }
        return result
    }

    /**
     * Creates a command to write user data.
     * Divides command into some frames if necessary.
     * @param startAdress
     * @param functionData
     * @return
     */
    fun buildDataWriteCommand(startAddress: Int, functionData: ByteArray?): ArrayList<ByteArray> {
        val unit = maxBlocks * 16 - 16
        val splitCount = (functionData!!.size + unit - 1) / unit

        val result = ArrayList<ByteArray>()
        var offset = 0
        var dataLen = (maxBlocks - 1) * 16//ヘッダを除いたデータのみのサイズ
        var frameBlocks = 0
        for (i in 0 until splitCount) {
            if (i == splitCount - 1) {
                //last block
                dataLen = functionData.size - offset
                frameBlocks = (dataLen + 15) / 16
                frameBlocks++
            } else {
                frameBlocks = maxBlocks
            }

            val cmd = ByteArray(frameBlocks * 16)
            cmd[0] = COMMAND_DATA_WRITE
            cmd[1] = splitCount.toByte()
            cmd[2] = (i + 1).toByte()
            cmd[3] = dataLen.toByte()
            cmd[4] = nextSeq

            //security code
            if (mSecCode2 != null) {
                cmd[5] = mSecCode2!![0]
                cmd[6] = mSecCode2!![1]
                cmd[7] = mSecCode2!![2]
            } else {
                cmd[5] = 0x30
                cmd[6] = 0x30
                cmd[7] = 0x30
            }
            //Address
            val address = startAddress + offset
            val hAByte = (address shr 8).toByte()
            val lAByte = (address and 0x00FF).toByte()

            //Length
            val hLByte = (dataLen shr 8).toByte()
            val lLByte = (dataLen and 0x00FF).toByte()
            val paramData = byteArrayOf(hAByte, lAByte, hLByte, lLByte, 0, 0, 0, 0)

            //set function parameter data.
            System.arraycopy(paramData, 0, cmd, 8, paramData.size)
            //set function data.
            if (functionData != null) {
                System.arraycopy(functionData, offset, cmd, 16, dataLen)
            }
            result.add(cmd)

            offset += dataLen
        }
        return result
    }

    fun setSeq(seq: Byte) {
        this.mSeq = seq
    }

    private fun getSplitCount(totalBlocks: Int): Int {
        val unit = maxBlocks - 1
        return (totalBlocks + unit - 1) / unit
    }

    private fun getLastBlockCount(dataBlocks: Int): Int {
        if (dataBlocks == 0)
            return 0

        val mod = dataBlocks % (maxBlocks - 1)
        return if (mod == 0) {
            maxBlocks - 1
        } else {
            mod
        }
    }

    /**
     * Returns the security code.
     * @param functionNo
     * @return
     */
    private fun getSecurityCode(functionNo: Byte, paramData: ByteArray): ByteArray? {
        var code: ByteArray? = null
        when (functionNo) {
            COMMAND_SHOW_DISPLAY_OLD, COMMAND_SHOW_DISPLAY, COMMAND_SHOW_DISPLAY3, COMMAND_CLEAR, COMMAND_SAVE_LAYOUT -> code =
                mSecCode1
            COMMAND_DATA_WRITE -> code = mSecCode2
            COMMAND_DATA_READ -> code = mSecCode3
            COMMAND_CHANGE_SECURITY_CODE -> code = getSecurityCodeByType(paramData[0])
            else -> code = byteArrayOf(0x30, 0x30, 0x30)
        }
        return code
    }

    /**
     * Returns the security code.
     * @param type
     * @return
     */
    private fun getSecurityCodeByType(type: Byte): ByteArray? {
        when (type) {
            SECURITY_CODE_TYPE1 -> return mSecCode1
            SECURITY_CODE_TYPE2 -> return mSecCode2
            SECURITY_CODE_TYPE3 -> return mSecCode3
        }
        return null
    }

    companion object {
        val COMMAND_DATA_WRITE = 0xB0.toByte()
        val COMMAND_SHOW_DEMO = 0x30.toByte()
        val COMMAND_CHANGE_SECURITY_CODE = 0xBD.toByte()

        val COMMAND_DATA_READ = 0xC0.toByte()
        val COMMAND_CHECK_STATUS = 0xD0.toByte()
        val COMMAND_CLEAR = 0xA1.toByte()
        val COMMAND_SHOW_DISPLAY_OLD = 0xA0.toByte()
        val COMMAND_SAVE_LAYOUT = 0xB2.toByte()
        val COMMAND_SHOW_DISPLAY = 0xA2.toByte()
        val COMMAND_SHOW_DISPLAY3 = 0xA3.toByte()

        val SECURITY_CODE_TYPE1: Byte = 1
        val SECURITY_CODE_TYPE2: Byte = 2
        val SECURITY_CODE_TYPE3: Byte = 3
    }
}
