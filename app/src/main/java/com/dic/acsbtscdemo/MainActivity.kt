package com.dic.acsbtscdemo

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.widget.*
import kotlin.system.exitProcess
/**
 * Main activity handles all fragment activity
 * @author DIC
 * @version 1.0.0
 */
class MainActivity : AppCompatActivity() {

    private var viewPager:ViewPager? = null

    private lateinit var optionsMenu:OptionsMenu
    private lateinit var scanDevices: ScanDevices
    private lateinit var createTag: CreateTag

    enum class FragmentId(val id:Int){OptionsMenu(0), ScanDevices(1), CreateTag(2)}

    private val permissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()
        setContentView(R.layout.activity_main)
        //via view pager
        /*viewPager = findViewById(R.id.container)
        setupViewPager(viewPager!!)*/
        //via fragmentManager
        //val p = ProgressBar(this,null,R.id.progressBarHorizontal)
        //val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        //this.addContentView(p, params)

        replaceViewFragment(FragmentId.OptionsMenu)
    }


    override fun onBackPressed() {

        val menuFragment = supportFragmentManager.findFragmentByTag("OptionsMenu")

        //Required user to press back twice if other fragment view other than Options Menu was shown
        if(menuFragment == null || menuFragment.isHidden){
            Toast.makeText(this, "Press back again to quit", Toast.LENGTH_SHORT).show()
            replaceViewFragment(FragmentId.OptionsMenu)
        }
        else{
            BluetoothInstance.disablePolling()
            exitProcess(-1)
        }

    }

    //init fragment pager
    private fun setupViewPager(vp:ViewPager){
        val statePagerAdapter:StatePagerAdapter? = StatePagerAdapter(supportFragmentManager)
        statePagerAdapter!!.addFragment(OptionsMenu(), "Options Menu")
        statePagerAdapter.addFragment(ScanDevices(), "Scan Bluetooth")
        statePagerAdapter.addFragment(CreateTag(), "Create Name Tag")
        vp.adapter = statePagerAdapter
    }

    fun setViewPager(fragmentNum:Int){
        viewPager!!.currentItem = fragmentNum
    }

    fun replaceViewFragment(id: FragmentId){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        /*when(id){
            FragmentId.ScanDevices-> { fragmentTransaction.replace(android.R.id.content, ScanDevices(), "ScanDevices")}
            FragmentId.OptionsMenu-> { fragmentTransaction.replace(android.R.id.content, OptionsMenu(), "OptionsMenu")}
            FragmentId.CreateTag-> { fragmentTransaction.replace(android.R.id.content, CreateTag(), "CreateTag")}
        }*/
        val scnfrag = supportFragmentManager.findFragmentByTag("ScanDevices")
        val optfrag = supportFragmentManager.findFragmentByTag("OptionsMenu")
        val crtfrag = supportFragmentManager.findFragmentByTag("CreateTag")

        //determine which fragment to be shown when button is pressed
        when(id){
             FragmentId.ScanDevices-> {
                 scanDevices = ScanDevices()
                 fragmentTransaction.replace(android.R.id.content, scanDevices, "ScanDevices")
             }
             FragmentId.OptionsMenu-> {
                 if(scnfrag!= null){
                     fragmentTransaction.remove(scnfrag)
                 }
                 if(optfrag != null){
                     fragmentTransaction.show(optfrag)
                     optionsMenu.loadAnimation()
                 }
                 else{
                     optionsMenu = OptionsMenu()
                     fragmentTransaction.add(android.R.id.content, optionsMenu,"OptionsMenu")
                 }
                 if(crtfrag != null){
                     fragmentTransaction.hide(crtfrag)
                 }

             }
             FragmentId.CreateTag-> {
                 if(scnfrag!= null){
                     fragmentTransaction.remove(scnfrag)
                 }
                 if(crtfrag != null){
                     fragmentTransaction.show(crtfrag)
                     createTag.loadAnimation()
                 }
                 else{
                     createTag = CreateTag()
                     fragmentTransaction.add(android.R.id.content, createTag,"CreateTag")
                 }
                 if(optfrag != null){
                     fragmentTransaction.hide(optfrag)
                 }

             }
         }
        fragmentTransaction.commit()
    }

    //check if the app has permission and request user to enable it
    fun hasNoPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.BLUETOOTH
        ) != PackageManager.PERMISSION_GRANTED|| ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0)
    }


}
