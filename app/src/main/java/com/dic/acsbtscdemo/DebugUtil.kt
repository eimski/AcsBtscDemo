package com.dic.acsbtscdemo

import android.util.Log

object DebugUtil {
    private val DEBUG_FLG = true
    private val TAG = "SmartTagCore"

    /**
     * Outputs the information log.
     */
    fun addLogi(message: String) {
        if (DEBUG_FLG) {
            Log.i(TAG, message)
        }
    }

    /**
     * Outputs the error log.
     */
    fun addLoge(message: String) {
        if (DEBUG_FLG) {
            Log.e(TAG, message)
        }
    }

    /**
     * Outputs the debug log.
     */
    fun addLogd(message: String) {
        if (DEBUG_FLG) {
            Log.d(TAG, message)
        }
    }

    fun makeHexText(data: ByteArray): String {
        if (DEBUG_FLG) {
            val sb = StringBuffer()
            for (i in data.indices) {
                if (i != 0) {
                    sb.append("-")
                }
                sb.append(String.format("%02X", data[i]))
            }
            return sb.toString()
        }
        return ""
    }
}
