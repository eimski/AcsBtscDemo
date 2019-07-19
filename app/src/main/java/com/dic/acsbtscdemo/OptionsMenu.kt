package com.dic.acsbtscdemo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.ActivityCompat.invalidateOptionsMenu
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.getSystemService
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.acs.bluetooth.*

import com.acs.bluetooth.Acr1255uj1Reader.OnBatteryLevelAvailableListener
import com.acs.bluetooth.Acr1255uj1Reader.OnBatteryLevelChangeListener
import kotlinx.android.synthetic.main.fragment_options.*


import kotlinx.android.synthetic.main.fragment_scan_devices.*

class OptionsMenu: Fragment() {

    private var viewModel = BluetoothViewModel()
    //private val masterKey:ByteArray = byteArrayOfInts(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_options,container,false)
        initUi(view)
        val scanButton = view.findViewById<View>(R.id.btn_ScanBluetooth)
        scanButton.setOnClickListener{
            buttonEffect(it)
            //(activity as MainActivity).setViewPager(1)
            (activity as MainActivity).replaceViewFragment(1)

        }

        val showDemoButton = view.findViewById<View>(R.id.buttonDemoImage)
        showDemoButton.setOnClickListener {
        }
        connectReader()
        return view
    }

    private fun initUi(view:View){
        val pairDevice = view.findViewById<TextView>(R.id.lblPairedDevice)
        var battLevel = view.findViewById<TextView>(R.id.lblBattery)
        val status = view.findViewById<TextView>(R.id.lblStatus)

        if(BluetoothInstance.device == null){
            pairDevice.text = "unknown"
            return
        }
        pairDevice.text = BluetoothInstance.device!!.name
    }

    private fun buttonEffect(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background.setColorFilter(-0x1f0b8adf, PorterDuff.Mode.SRC_ATOP)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    v.background.clearColorFilter()
                    v.invalidate()
                }
            }
            false
        }
    }

    private fun showBatteryLevel(view:View){
        val battText = view.findViewById<TextView>(R.id.lblBattery)
        (BluetoothInstance.reader as Acr1255uj1Reader)
            .setOnBatteryLevelChangeListener{ btReader:BluetoothReader, batteryLvl:Int ->
            battText.text = batteryLvl.toString() + "%"
        }
    }

    private fun showDeviceName(){

    }

    private fun showStatus(message:String){

    }

    private fun connectReader(): Boolean {

        if(BluetoothInstance.device == null)
            return false


        val btCallback = BluetoothReaderGattCallback()
        btCallback.setOnConnectionStateChangeListener { gatt, state, newState ->
            var msg = ""
            if (newState == BluetoothReader.STATE_CONNECTED) {
                if (BluetoothInstance.readerManager != null) {
                    BluetoothInstance.readerManager!!.detectReader(
                        gatt, btCallback
                    )
                }
                msg = "Attempting to start service discovery:" + gatt.discoverServices()
            } else if (newState == BluetoothReader.STATE_DISCONNECTED) {
                //onDisconnected(gatt);
                msg = "Disconnected from GATT server."
            }

            //Toast.makeText(this!!, msg, Toast.LENGTH_SHORT).show()
        }

        BluetoothInstance.readerManager = BluetoothReaderManager()
        BluetoothInstance.readerManager!!.setOnReaderDetectionListener {
            BluetoothInstance.reader = it
            (BluetoothInstance.reader as Acr1255uj1Reader).enableNotification(true)
            BluetoothInstance.reader!!.setOnEnableNotificationCompleteListener{reader, errorCode ->
                /*val status = view!!.findViewById<TextView>(R.id.lblStatus)
                if(errorCode == BluetoothGatt.GATT_SUCCESS)
                    status.text = "The device is unable to set notification"
                    //Toast.makeText(view1.context, "The device is ready to use", Toast.LENGTH_SHORT).show()
                else
                    status.text = "The device is unable to set notification"*/

                val authOk = BluetoothInstance.reader!!.authenticate(BluetoothInstance.masterKey)
                if (authOk) {
                    // Toast.makeText(context, "Authentication Successful!", Toast.LENGTH_SHORT).show()
                }
            }

            BluetoothInstance.reader!!.setOnAuthenticationCompleteListener{ reader, errorCode ->

                Toast.makeText(activity, "Hello", Toast.LENGTH_SHORT).show()
                //val status = view!!.findViewById<TextView>(R.id.lblStatus)
                if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                    //status.text = "Authentication successful"
                } else {
                    //status.text = "Authentication fail"
                }
            }
        }
        val device = BluetoothInstance.adapter?.getRemoteDevice(BluetoothInstance.device!!.address)
        val btGatt:BluetoothGatt = device!!.connectGatt(context, false, btCallback)
        val isConnect = btGatt.connect()
        return true
    }

    fun Context.toast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

