package com.dic.acsbtscdemo

import android.bluetooth.*
import android.content.Context
import android.widget.TextView
import android.widget.Toast

import com.acs.bluetooth.Acr1255uj1Reader
import com.acs.bluetooth.BluetoothReader
import com.acs.bluetooth.BluetoothReaderGattCallback
import com.acs.bluetooth.BluetoothReaderManager

class BluetoothInstance{


    companion object{

        var adapter:BluetoothAdapter? = null
        var device:BluetoothDevice? = null
        var btCallback:BluetoothReaderGattCallback = BluetoothReaderGattCallback()
        var core:SmartTagCore? = null


        var reader:BluetoothReader? = null
        var readerManager:BluetoothReaderManager? = BluetoothReaderManager()
        var gatt:BluetoothGatt? = null
        val masterKey:ByteArray = byteArrayOfInts(0x41, 0x43, 0x52, 0x31, 0x32, 0x35, 0x35, 0x55, 0x2D, 0x4A, 0x31, 0x20, 0x41, 0x75, 0x74, 0x68)

        var prevFragmentId = MainActivity.FragmentId.OptionsMenu

        fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

        fun  enablePolling(){
            val cmd = byteArrayOfInts(0xE0, 0x00, 0x00, 0x40, 0x01)
            this.reader?.transmitEscapeCommand(cmd)
        }

        fun disablePolling(){
            val cmd = byteArrayOfInts(0xE0, 0x00, 0x00, 0x40, 0x00)
            this.reader?.transmitEscapeCommand(cmd)
        }

    }

}