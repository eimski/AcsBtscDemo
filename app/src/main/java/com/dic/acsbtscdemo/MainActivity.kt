package com.dic.acsbtscdemo

import android.arch.lifecycle.ViewModelProvider
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.FragmentManager
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mHandler: Handler? = null
    private var viewPager:ViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()
        setContentView(R.layout.activity_main)

        //via view pager
        /*viewPager = findViewById(R.id.container)
        setupViewPager(viewPager!!)*/

        //via fragmentManager
        replaceViewFragment(0)
    }

    override fun onBackPressed() {

        val menuFragment = supportFragmentManager.findFragmentByTag("OptionsMenu")
        if (menuFragment == null){
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            replaceViewFragment(0)
        }
        else{
            BluetoothInstance.disablePolling()
            BluetoothInstance.gatt?.disconnect()
            super.onBackPressed()
        }

    }

    //init fragment pager
    private fun setupViewPager(vp:ViewPager){
        val statePagerAdapter:StatePagerAdapter? = StatePagerAdapter(supportFragmentManager)
        statePagerAdapter!!.addFragment(OptionsMenu(), "Options Menu")
        statePagerAdapter.addFragment(ScanDevices(), "Scan Bluetooth")
        vp.adapter = statePagerAdapter
    }

    fun setViewPager(fragmentNum:Int){
        viewPager!!.currentItem = fragmentNum
    }

    fun replaceViewFragment(fragmentNum:Int){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        when(fragmentNum){
            0-> { fragmentTransaction.replace(android.R.id.content, OptionsMenu(), "OptionsMenu")}
            1-> { fragmentTransaction.replace(android.R.id.content, ScanDevices(), "ScanDevices")}
        }
        fragmentTransaction.commit()
    }

}
