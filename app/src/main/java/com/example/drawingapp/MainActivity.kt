package com.example.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception




class MainActivity : AppCompatActivity() {
        private var drawingView: DrawingView? =  null
        private var mCurrentImagePaint : ImageButton? = null
    var customDialog : Dialog? = null

    val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){result->
            if(result.resultCode== RESULT_OK && result.data!=null){
               val imageBg : ImageView = findViewById(R.id.iv_background)
                //URI is the location of the pic in the device
                imageBg.setImageURI(result.data?.data)
            }

        }




    private val requestPermissions : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
                permissions->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted){
                    Toast.makeText(this,"Permission Granted for Storage Files",
                    Toast.LENGTH_LONG).show()
                    // path to move to gallery
                    val pickIntent= Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                }else{
                    if (permissionName== Manifest.permission.READ_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this,"Permission Denied for Storage Files",
                            Toast.LENGTH_LONG).show()
                }

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setBrushSize(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_brush_color)
        mCurrentImagePaint= linearLayoutPaintColors[1] as ImageButton
        mCurrentImagePaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected1)
        )


        val ib_undo : ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener{
              drawingView?.onClickUndo()
        }
        val ib_redo : ImageButton = findViewById(R.id.ib_redo)
        ib_redo.setOnClickListener{
            drawingView?.onClickRedo()
        }
        val ib_save : ImageButton = findViewById(R.id.ib_save)
        ib_save.setOnClickListener{
                if(isReadStorageAllowed()){
                    customProgressDialogFun()
                 lifecycleScope.launch{
                     val flDrawingView: FrameLayout =findViewById(R.id.fl_drawing_view_container)
                     saveBitmapFile(getBitmapFromView(flDrawingView))
                 }

                }


        }
        val galleryPermission: ImageButton = findViewById(R.id.ib_pic_gallery)
        galleryPermission.setOnClickListener{
            requestPermission()
        }
        val brushSize: ImageButton = findViewById(R.id.ib_brush)
        brushSize.setOnClickListener{
           showBrushSizeChooser()
        }

    }
    private fun showBrushSizeChooser(){

        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("BrushSize: ")

        val smallBrush : ImageButton =brushDialog.findViewById(R.id.ib_smallBrush)
        smallBrush.setOnClickListener{
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBrush : ImageButton =brushDialog.findViewById(R.id.ib_mediumBrush)
        mediumBrush.setOnClickListener{
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBrush : ImageButton =brushDialog.findViewById(R.id.ib_largeBrush)
        largeBrush.setOnClickListener{
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
    fun paintClicked(view: View){
       // Toast.makeText(this,"paint is selected", Toast.LENGTH_SHORT).show()
        if(view!=mCurrentImagePaint){
            val  imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected1)
            )
            mCurrentImagePaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mCurrentImagePaint= view
        }

        }

    private fun isReadStorageAllowed():Boolean{
        val result= ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return  result==PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){  showRationaleDialog("Drawing App",
        "Needs Access to external Storage")
        }else{
            requestPermissions.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

            ))
        }
    }


    private fun showRationaleDialog(title: String, message: String){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
            .setMessage("This is Alert Dialog")
            .setIcon(R.drawable.alert)
            .setPositiveButton("Cancel"){
                    dialog,
                    _ ->dialog.dismiss()
            }
        builder.create().show()


    }


// to save - we have to save the drawing but the drawing is a view,
// so view cannot be saved but a bitmap can be saved
// so convert view into bitmap


    private fun getBitmapFromView (view: View): Bitmap{
        val returnBitmap = Bitmap.createBitmap(
            view.width,view.height, Bitmap.Config.ARGB_8888
        )
    // to save drawing
    val canvas = Canvas(returnBitmap)
    // to save bgIMage
    val bgDrawable = view.background
    if(bgDrawable!= null){
        bgDrawable.draw(canvas)
    }else{
        canvas.drawColor(Color.WHITE)
    }
    //finalize
    view.draw(canvas)
     return returnBitmap
    }

// to save the drawing using coroutines
    private suspend fun saveBitmapFile(mBitmap : Bitmap?): String{
        var result =""
        withContext(Dispatchers.IO){
                if(mBitmap!= null)
                {
                    try{
                        val bytes= ByteArrayOutputStream()
                        mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                        val f = File(externalCacheDir?.absoluteFile.toString()
                        +File.separator + "DrawingApp" + System.currentTimeMillis()/100 + ".png")

                        val fo = FileOutputStream(f)
                        fo.write(bytes.toByteArray())
                        fo.close()

                        result = f.absolutePath


                        runOnUiThread{
                            cancelProgressDialog()
                            if(result.isNotEmpty()) {
                                Toast.makeText(applicationContext,
                                "File saved  at $result",
                                Toast.LENGTH_SHORT).show()
                                shareImage(result)
                            }else{
                                Toast.makeText(applicationContext,
                                "File was not saved ",
                                Toast.LENGTH_SHORT).show()

                            }
                        }
                    }catch (e:Exception) {
                        result = ""
                        e.printStackTrace()
                    }
                }
        }

   return result
    }
    private fun customProgressDialogFun(){
        customDialog = Dialog(this)
        customDialog?.setContentView(R.layout.custom_progress)

        customDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customDialog!=null){
            customDialog?.dismiss()
            customDialog = null
        }
    }
// to share files
private fun shareImage(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
                path, uri->
            val shareIntent=Intent()
            shareIntent.action =Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type= "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))

        }

}

}