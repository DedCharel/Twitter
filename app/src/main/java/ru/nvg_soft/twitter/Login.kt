package ru.nvg_soft.twitter

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.RequiresPermission
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ivImagePerson.setOnClickListener(View.OnClickListener {
            checkPermission()
        })
    }

    val READIMAGE:Int = 253
    fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)!=
                    PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), READIMAGE)
                    return
            }
        }
        loadImage()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            READIMAGE ->{
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    loadImage()
                }else{
                    Toast.makeText(this,"Cannot access your image",Toast.LENGTH_LONG).show()
                }
            } else ->super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }
    val PICK_IMAGE_CODE = 123
    fun loadImage(){
        var intent = Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent,PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_CODE && data!=null){
            val selectedImage = data.data
            val filePathColum = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage,filePathColum, null,null, null)
            cursor.moveToFirst()
            val columIndex = cursor.getColumnIndex(filePathColum[0])
            val picturePath = cursor.getString(columIndex)
            ivImagePerson.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }
    fun btnLoginEvent(view:View){}
}
