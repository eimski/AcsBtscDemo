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


class BtDevice(context:Context) : LinearLayout(context){

    private var viewModel = BluetoothViewModel()
    private var btDevice:BluetoothDevice? = null
    init{
        View.inflate(context, R.layout.component_bluetooth_device, this)
        buttonPair.setOnClickListener {
            if(!btDevice!!.name.contains("ACR1255U")){
                Toast.makeText(context, "Device not supported", Toast.LENGTH_SHORT).show()
            }
            else{
                //(context as MainActivity).setViewPager(0)
                (context as MainActivity).replaceViewFragment(0)
                BluetoothInstance.device = btDevice

            }
        }
    }

    fun setBluetoothDevice(device:BluetoothDevice){
        btDevice = device
    }
}