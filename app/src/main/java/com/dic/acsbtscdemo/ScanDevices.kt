package com.dic.acsbtscdemo


import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.component_bluetooth_device.view.*
import android.support.v4.view.accessibility.AccessibilityEventCompat.setAction
import android.content.Intent



class ScanDevices : Fragment(){

    private var deviceCollection = mutableListOf<String>()

    /**
     * ScanCallback for bluetooth device scan
     */
    private val scanCallback = object:ScanCallback(){

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val devList = activity!!.findViewById<LinearLayout>(R.id.deviceList)

            if(result!!.device.name.isNullOrBlank())
                return

            if(deviceCollection.contains(result.device.name))
                return

            val btt = AnimationUtils.loadAnimation(context, R.anim.bottom_to_top)
            val btDevice = BtDevice(activity!!)
            btDevice.deviceName.text = result.device.name
            btDevice.setBluetoothDevice(result.device)
            devList.addView(btDevice)
            deviceCollection.add(result.device.name)
            btDevice.startAnimation(btt)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)

            Toast.makeText(
                activity,"onBatchScanResults()",
                Toast.LENGTH_SHORT
            ).show()

            //devList.addView(btDevice)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(
                activity,"onScanFailed()",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_scan_devices, container, false)
        BluetoothInstance.device = null
        deviceCollection.clear()
        initBluetooth()
        BluetoothInstance.prevFragmentId = MainActivity.FragmentId.ScanDevices
        if ((activity as MainActivity).hasNoPermissions()) {
            (activity as MainActivity).requestPermission()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        initBluetooth()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BluetoothInstance.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun initBluetooth(){

        if(!BluetoothAdapter.getDefaultAdapter().isEnabled){

            val intentOpenBluetoothSettings = Intent()
            intentOpenBluetoothSettings.action = android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            startActivity(intentOpenBluetoothSettings)
        }
        else{

            if (!activity!!.packageManager!!.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(
                    activity, R.string.error_bluetooth_le_not_supported,
                    Toast.LENGTH_SHORT
                ).show()
                activity?.finish()
                return
            }

            val manager = context!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            BluetoothInstance.adapter = manager.adapter
            BluetoothInstance.adapter!!.bluetoothLeScanner.startScan(scanCallback)

        }
    }

    private fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
}

