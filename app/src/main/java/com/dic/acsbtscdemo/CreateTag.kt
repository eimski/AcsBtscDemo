package com.dic.acsbtscdemo

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import kotlinx.android.synthetic.main.fragment_create_tag.*
/**
 * Tag to create name tag fragment
 * @author DIC
 * @version 1.0.0
 */
class CreateTag: Fragment() {

    private lateinit var photo: Bitmap

    //Can be any number. Not used but a requirement for android function
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_create_tag, container, false)


        val btnShoot = view.findViewById<View>(R.id.buttonShootPhoto)
        btnShoot.setOnClickListener {
            if ((activity as MainActivity).hasNoPermissions()) {
                (activity as MainActivity).requestPermission()
            }
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                intent.resolveActivity(activity!!.packageManager)?.also {
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
        val btnCreateTag = view.findViewById<View>(R.id.buttonCreateNameTag)
        btnCreateTag.setOnClickListener {

            if (BluetoothInstance.reader != null) {
                val imageViewDrawable = view?.findViewById<ImageView>(R.id.imageView)!!.drawable as BitmapDrawable
                photo = imageViewDrawable.bitmap
                BluetoothInstance.core?.bmpByte = createByteImage(photo)
                BluetoothInstance.core?.process = SmartTagCore.Process.DisplayImage
                BluetoothInstance.enablePolling()
            }
        }
        BluetoothInstance.prevFragmentId = MainActivity.FragmentId.CreateTag
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAnimation()
    }

    fun loadAnimation(){
        val btt = AnimationUtils.loadAnimation(context, R.anim.bottom_to_top)
        val fin = AnimationUtils.loadAnimation(context, R.anim.fade_in)

        val imageview = view?.findViewById<ImageView>(R.id.imageView)
        val buttonShootPhoto = view?.findViewById<Button>(R.id.buttonShootPhoto)
        val buttonCreate = view?.findViewById<Button>(R.id.buttonCreateNameTag)
        val organisation = view?.findViewById<EditText>(R.id.editOrganisation)
        val name = view?.findViewById<EditText>(R.id.editName)

        imageview?.startAnimation(fin)
        buttonShootPhoto?.startAnimation(fin)
        buttonCreate?.startAnimation(fin)
        organisation?.startAnimation(btt)
        name?.startAnimation(btt)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (data != null) {
                val img = data.extras?.get("data") as Bitmap
                val imgHt = (img.width * 1.3).toInt()
                val imgCrop = Bitmap.createBitmap(img, 0, 0, img.width, imgHt)
                val imgView = view?.findViewById<ImageView>(R.id.imageView)
                imgView?.setImageBitmap(imgCrop)
                photo = imgCrop
            }
        }
    }


    //e-paper image creation
    private fun createByteImage(photo: Bitmap): ByteArray {

        val dp = DisplayPainter(DisplayPainter.DISPLAY_TYPE_300X200)
        val scaledPhoto = Bitmap.createScaledBitmap(photo, 118, 158, false)
        dp.putImage(scaledPhoto, 15, 20, true)
        dp.putRectangle(5, 10, 140, 180, 2)

        val organizationText = textToBmp(editOrganisation.text.toString(), 12, 140,true)
        var posx = getRefPtVertCenter(150, 300, organizationText.width)
        dp.putImage(organizationText, posx, 40, false)

        val nameText = textToBmp(editName.text.toString(), 20, 150, false)
        posx = getRefPtVertCenter(150, 300, nameText.width)
        dp.putImage(nameText, posx, 100, false)
        return dp.localDisplayImage
    }

    //Convert string text into bmp and scale them to fit limited spaces
    private fun textToBmp(text:String, fontsize:Int, maxTextWidth:Int, backgroundFill:Boolean): Bitmap {

        val paint = Paint()
        if(backgroundFill)
            paint.color = Color.WHITE
        else
            paint.color = Color.BLACK
        paint.textSize = fontsize.toFloat()

        var bitmap = Bitmap.createBitmap(paint.measureText(text).toInt() * 1.2.toInt() + 2 , (fontsize * 1.2).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if(backgroundFill)
            canvas.drawColor(Color.BLACK)
        else
            canvas.drawColor(Color.WHITE)
        canvas.drawText(text,1f, fontsize.toFloat(), paint)

        if(bitmap.width>maxTextWidth)
           bitmap = Bitmap.createScaledBitmap(bitmap, maxTextWidth, fontsize, true)

        return bitmap
    }

    private fun getRefPtVertCenter(leftPos:Int, rightPos:Int, picWidth:Int):Int{

        val ref = (rightPos - leftPos)/2 - (picWidth/2)
        return leftPos + ref
    }

}