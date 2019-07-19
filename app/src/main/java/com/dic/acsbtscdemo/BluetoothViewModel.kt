package com.dic.acsbtscdemo
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.bluetooth.BluetoothDevice

import android.bluetooth.BluetoothAdapter

class BluetoothViewModel: ViewModel() {
    val deviceList = mutableListOf<BluetoothDevice>()

}