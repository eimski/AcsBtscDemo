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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_options,container,false)
        initUi(view)
        val scanButton = view.findViewById<View>(R.id.btn_ScanBluetooth)
        scanButton.setOnClickListener{
            buttonEffect(it)
            if(BluetoothInstance.gatt == null){
                //(activity as MainActivity).setViewPager(1)
                (activity as MainActivity).replaceViewFragment(1)
            }
            else{
                BluetoothInstance.gatt!!.disconnect()
            }
        }

        val showDemoButton = view.findViewById<View>(R.id.buttonDemoImage)
        showDemoButton.setOnClickListener {
            BluetoothInstance.enablePolling()
        }
        connectReader()
        return view
    }

    private fun initUi(view:View){
        val pairDevice = view.findViewById<TextView>(R.id.lblPairedDevice)

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

    private fun connectReader(): Boolean {

        if(BluetoothInstance.device == null)
            return false


        /*if(BluetoothInstance.gatt != null){
            if(BluetoothInstance.gatt!!.getConnectionState(BluetoothInstance.device) == BluetoothGatt.STATE_CONNECTED)
                return true
        }*/

        val btCallback = BluetoothReaderGattCallback()
        btCallback.setOnConnectionStateChangeListener { gatt, state, newState ->

            if (newState == BluetoothReader.STATE_CONNECTED) {
                if (BluetoothInstance.readerManager != null) {
                    BluetoothInstance.readerManager!!.detectReader(gatt, btCallback)
                    BluetoothInstance.gatt = gatt
                }
                val msg = "Attempting to start service discovery: " + gatt.discoverServices()
                updateStatus(msg)
            } else if (newState == BluetoothReader.STATE_DISCONNECTED) {
                BluetoothInstance.gatt!!.close()
                val msg = "Disconnected from GATT server."
                showToast(msg)
                (activity as MainActivity).replaceViewFragment(1)
            }
        }

        BluetoothInstance.readerManager = BluetoothReaderManager()
        BluetoothInstance.readerManager!!.setOnReaderDetectionListener {
            BluetoothInstance.reader = it
            (BluetoothInstance.reader as Acr1255uj1Reader).enableNotification(true)
            BluetoothInstance.reader!!.setOnEnableNotificationCompleteListener{reader, errorCode ->

                if(errorCode == BluetoothGatt.GATT_SUCCESS)
                    showToast("The device is ready to use")
                else
                    showToast("The device is unable to set notification")

                //authenticate device
                BluetoothInstance.reader!!.authenticate(BluetoothInstance.masterKey)
            }

            BluetoothInstance.reader!!.setOnAuthenticationCompleteListener{ reader, errorCode ->

                if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                    updateStatus("Authentication successful")
                } else {
                    updateStatus("Authentication fail")
                }
            }

            BluetoothInstance.reader!!.setOnCardStatusChangeListener{ reader, cardStatus ->
                if(cardStatus == BluetoothReader.CARD_STATUS_PRESENT){
                    updateStatus("Card present")
                }else{
                    updateStatus("Card not detected")
                }
            }

            (BluetoothInstance.reader as Acr1255uj1Reader)
                .setOnBatteryLevelAvailableListener { _: BluetoothReader, batteryLvl: Int, status: Int ->
                    activity!!.runOnUiThread {
                        val batteryText = this.view!!.findViewById<TextView>(R.id.lblBattery)
                        batteryText.text = batteryLvl.toString() + "%"
                    }
                }

            (BluetoothInstance.reader as Acr1255uj1Reader)
                .setOnBatteryLevelChangeListener { _: BluetoothReader, batteryLvl: Int ->
                    activity!!.runOnUiThread {
                        val batteryText = this.view!!.findViewById<TextView>(R.id.lblBattery)
                        batteryText.text = batteryLvl.toString() + "%"
                    }
                }
        }
        val device = BluetoothInstance.adapter?.getRemoteDevice(BluetoothInstance.device!!.address)
        val btGatt:BluetoothGatt = device!!.connectGatt(context, false, btCallback)
        btGatt.connect()
        return true
    }

    private fun showToast(message:String){
        activity!!.runOnUiThread {
            Toast.makeText(this.context,message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(message:String){
        activity!!.runOnUiThread {
            val status = this.view!!.findViewById<TextView>(R.id.lblStatus)
            status.text =  message
        }
    }
}

