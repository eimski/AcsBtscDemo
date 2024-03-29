package com.dic.acsbtscdemo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.widget.Toast
import com.acs.bluetooth.BluetoothReaderManager
import kotlinx.android.synthetic.main.component_bluetooth_device.view.*

/**
 * Class represents one bluetooth device discovered during scanning
 * @author DIC
 * @version 1.0.0
 */

class BtDevice(context:Context) : LinearLayout(context){

    private lateinit var btDevice:BluetoothDevice
    init{
        View.inflate(context, R.layout.component_bluetooth_device, this)
        buttonPair.setOnClickListener {

            if(!btDevice.name.contains("ACR1255U")){
                Toast.makeText(context, "Device not supported", Toast.LENGTH_SHORT).show()
            }
            else{
                //(context as MainActivity).setViewPager(0)
                (context as MainActivity).replaceViewFragment(MainActivity.FragmentId.OptionsMenu)
                BluetoothInstance.device = btDevice

            }
        }
    }


    fun setBluetoothDevice(device:BluetoothDevice){
        btDevice = device
    }
}