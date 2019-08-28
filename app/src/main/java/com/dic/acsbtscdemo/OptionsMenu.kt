package com.dic.acsbtscdemo

import android.app.AlertDialog
import android.bluetooth.*
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.app.Fragment
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import com.acs.bluetooth.*
import kotlin.concurrent.thread


class OptionsMenu: Fragment() {

    private lateinit var messageHandler:Handler
    private lateinit var progressBar: ProgressBar
    private lateinit var progressDialog:AlertDialog
    private lateinit var progressText:TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_options,container,false)

        messageHandler = object:Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message?) {

                when(msg?.what){
                    SmartTagCore.MessageType.Toast.value->
                        Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show()

                    SmartTagCore.MessageType.EditBox.value->{
                        //set font for all
                        val title = TextView(context)
                        title.typeface = Typeface.create("casual", Typeface.NORMAL)
                        title.setTextColor(Color.parseColor("#F57F17"))
                        title.textSize = 18.00f
                        title.text = "Memory Read"

                        val response = TextView(context)
                        response.typeface = Typeface.create("monospace", Typeface.NORMAL)
                        response.textSize = 18.00f
                        response.setTextColor(Color.parseColor("#444444"))
                        response.text = String(msg.obj as ByteArray, Charsets.UTF_8)
                        response.gravity = Gravity.CENTER_HORIZONTAL

                        AlertDialog.Builder(context)
                            .setCustomTitle(title)
                            .setView(response)
                            .setCancelable(false)
                            .setPositiveButton("Ok"){ _: DialogInterface?, _: Int -> }
                            .show()
                    }

                    SmartTagCore.MessageType.Progress.value->{
                        if(msg.obj as Int == 0) {
                            progressDialog.show()
                        }

                        progressBar.progress = msg.obj as Int
                        val count = progressBar.progress
                        val display = "Processing $count%"
                        progressText.text = display

                        if (msg.obj as Int == 100) {
                            thread{
                                progressDialog.dismiss()
                            }
                        }
                    }

                    SmartTagCore.MessageType.Error.value->{
                        if(progressDialog.isShowing)
                            progressDialog.dismiss()

                        Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        val scanButton = view.findViewById<View>(R.id.btn_ScanBluetooth)
        scanButton.setOnClickListener{
            buttonEffect(it)
            if(BluetoothInstance.gatt != null)
                BluetoothInstance.gatt!!.disconnect()
            else{
                (activity as MainActivity).replaceViewFragment(MainActivity.FragmentId.ScanDevices)
            }
        }

        val showDemoButton = view.findViewById<Button>(R.id.buttonDemoImage)
        showDemoButton.setOnClickListener {
            if(BluetoothInstance.gatt != null){
                BluetoothInstance.core?.process = SmartTagCore.Process.ShowDemo
            }
            BluetoothInstance.enablePolling()
        }
        val clearDisplayButton = view.findViewById<Button>(R.id.buttonClearDisplay)
        clearDisplayButton.setOnClickListener {
            if(BluetoothInstance.gatt != null){
                BluetoothInstance.core?.process = SmartTagCore.Process.ClearDisplay
            }
            BluetoothInstance.enablePolling()
        }
        val memoryWriteButton = view.findViewById<Button>(R.id.buttonWriteMemory)
        memoryWriteButton.setOnClickListener {
            //set font for all
            val input = EditText(context)
            input.gravity = Gravity.CENTER_HORIZONTAL
            input.setSingleLine()
            input.typeface = Typeface.create("monospace", Typeface.NORMAL)
            input.hint = "Write a message"
            input.textSize = 24.00f

            val title = TextView(context)
            title.typeface = Typeface.create("casual", Typeface.NORMAL)
            title.text = "Memory Write"
            title.setTextColor(Color.parseColor("#F57F17"))
            title.textSize = 18.00f

            if (BluetoothInstance.gatt != null) {
                val dialog = AlertDialog.Builder(context)
                    .setCustomTitle(title)
                    .setView(input)
                    .setCancelable(true)
                    .setPositiveButton("Cancel") { _: DialogInterface?, _: Int -> }
                    .setNegativeButton("Ok") { _: DialogInterface?, _: Int ->
                        BluetoothInstance.core?.inputText = input.text.toString()
                        BluetoothInstance.core?.process = SmartTagCore.Process.WriteData
                        BluetoothInstance.enablePolling()
                    }
                dialog.show()
            }
        }
        val memoryReadButton = view.findViewById<Button>(R.id.buttonReadMemory)
        memoryReadButton.setOnClickListener {
            if(BluetoothInstance.gatt != null){
                BluetoothInstance.core?.process = SmartTagCore.Process.ReadUserData
            }
            BluetoothInstance.enablePolling()
        }
        val createNameTagButton = view.findViewById<Button>(R.id.buttonCreateNameTag)
        createNameTagButton.setOnClickListener {
            (activity as MainActivity).replaceViewFragment(MainActivity.FragmentId.CreateTag)
        }
        initUi(view)
        setListeners()

        BluetoothInstance.prevFragmentId = MainActivity.FragmentId.OptionsMenu
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val progressLayout = LayoutInflater.from(context).inflate(R.layout.progressbar,null)
        progressBar = progressLayout.findViewById(R.id.progressBarHorizontal)
        progressText = progressLayout.findViewById(R.id.progressInfo)

        val builder:AlertDialog.Builder? = activity?.let{
                AlertDialog.Builder(it)
        }
        builder?.setView(progressLayout)
        builder?.setCancelable(false)
        progressDialog = builder!!.create()
        loadAnimation()
    }


    fun loadAnimation(){
        val ttb = AnimationUtils.loadAnimation(context, R.anim.top_to_bottom)
        val btt = AnimationUtils.loadAnimation(context, R.anim.bottom_to_top)

        val btnScan = view?.findViewById<ImageButton>(R.id.btn_ScanBluetooth)
        val title = view?.findViewById<TextView>(R.id.title)
        val version = view?.findViewById<TextView>(R.id.version)
        val batterylevel = view?.findViewById<TextView>(R.id.batterylevel)
        val batteryvalue = view?.findViewById<TextView>(R.id.lblBattery)
        val paireddevice = view?.findViewById<TextView>(R.id.paireddevice)
        val device = view?.findViewById<TextView>(R.id.lblPairedDevice)
        val status = view?.findViewById<TextView>(R.id.lblStatus)
        val scrollview = view?.findViewById<ScrollView>(R.id.scrollView_main)

        btnScan?.startAnimation(ttb)
        title?.startAnimation(ttb)
        version?.startAnimation(ttb)
        batterylevel?.startAnimation(ttb)
        batteryvalue?.startAnimation(ttb)
        paireddevice?.startAnimation(ttb)
        device?.startAnimation(ttb)
        status?.startAnimation(ttb)
        scrollview?.startAnimation(btt)
    }

    private fun initUi(view:View){
        val pairDevice = view.findViewById<TextView>(R.id.lblPairedDevice)
        if(BluetoothInstance.device == null){
            if(BluetoothInstance.reader != null)
                (BluetoothInstance.reader as Acr1255uj1Reader).batteryLevel

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

    private fun setListeners(): Boolean {
        if(BluetoothInstance.device == null)
            return false

        BluetoothInstance.btCallback.setOnConnectionStateChangeListener { gatt, state, newState ->
            if (newState == BluetoothReader.STATE_CONNECTED) {
                if (BluetoothInstance.readerManager != null) {
                    BluetoothInstance.readerManager!!.detectReader(gatt, BluetoothInstance.btCallback )
                    BluetoothInstance.gatt = gatt
                }
                val msg = "Attempting to start service discovery: " + gatt.discoverServices()
                updateStatus(msg)
            } else if (newState == BluetoothReader.STATE_DISCONNECTED) {
                BluetoothInstance.gatt!!.close()
                BluetoothInstance.gatt = null
                val msg = "Disconnected from GATT server."
                showToast(msg)
                (activity as MainActivity).replaceViewFragment(MainActivity.FragmentId.ScanDevices)
            }
        }

        BluetoothInstance.readerManager!!.setOnReaderDetectionListener {
            BluetoothInstance.reader = it
            BluetoothInstance.core = SmartTagCore(messageHandler)
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
                    if(BluetoothInstance.core != null){
                        BluetoothInstance.core?.startProcess()
                    }
                }else{
                    if(progressDialog.isShowing)
                        progressDialog.dismiss()
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
        setGatt()
        return true
    }

    private fun setGatt(){

        val device = BluetoothInstance.adapter?.getRemoteDevice(BluetoothInstance.device!!.address)
        val btGatt = device!!.connectGatt(context, false, BluetoothInstance.btCallback)
        btGatt.connect()
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

