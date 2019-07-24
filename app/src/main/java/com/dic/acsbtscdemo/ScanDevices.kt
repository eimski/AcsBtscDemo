package com.dic.acsbtscdemo

import android.app.PendingIntent.getActivity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat.getSystemService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.component_bluetooth_device.view.*
import kotlinx.android.synthetic.main.fragment_scan_devices.*


class ScanDevices : Fragment(){

    private var mHandler: Handler? = null
    private var viewModel = BluetoothViewModel()

    /**
     * ScanCallback for bluetooth device scan
     */
    private val scanCallback = object:ScanCallback(){

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val devList = activity!!.findViewById<LinearLayout>(R.id.deviceList)

            if(result!!.device.name.isNullOrBlank())
                return

            val btDevice = BtDevice(activity!!)
            btDevice.deviceName.text = result.device.name
            btDevice.setBluetoothDevice(result.device)
            devList.addView(btDevice)
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
        viewModel = ViewModelProviders.of(activity!!).get(BluetoothViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_scan_devices, container, false)
        initBluetooth()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BluetoothInstance.adapter!!.bluetoothLeScanner.stopScan(scanCallback)
    }

    private fun initBluetooth(){
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

    private fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
}

