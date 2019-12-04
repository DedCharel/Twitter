package ru.nvg_soft.twitter

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.RequiresPermission
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Login : AppCompatActivity() {
    private var mAuth: FirebaseAuth?=null
    private var databese = FirebaseDatabase.getInstance()
    private var myRef = databese.getReference()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()

        ivImagePerson.setOnClickListener(View.OnClickListener {
            checkPermission()
        })
    }

    fun loginToFirebase(email:String, password:String){
        mAuth!!.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){task ->
                if(task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful login",Toast.LENGTH_LONG).show()

                    saveImageInFirebase()

                }else{
                    Toast.makeText(applicationContext,"Fail login",Toast.LENGTH_LONG).show()
                }
            }
    }
    fun splitEmail(email: String):String{
        return email.split("@")[0]
    }

    fun saveImageInFirebase(){
        var currentUser = mAuth!!.currentUser
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl("gs://twitter-67bcd.appspot.com")
        val df = SimpleDateFormat("ddMMyyHHmmss")
        val dataobj = Date()
        val imagePath = splitEmail(currentUser!!.email.toString()) + df.format(dataobj)+ ".jpg"
        val imageRef = storageRef.child("images/" + imagePath)
        ivImagePerson.isDrawingCacheEnabled = true;
        ivImagePerson.buildDrawingCache()

        val drawable = ivImagePerson.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext, "file to upload",Toast.LENGTH_LONG).show()}
            .addOnSuccessListener { taskSnapshot ->
                var dowonloadURL = taskSnapshot!!.toString()
                myRef.child("Users").child(currentUser.uid).child("email").setValue(currentUser.email)
                myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(dowonloadURL)
                loadTweets()
            }



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
    override fun onStart() {
        super.onStart()
        loadTweets()
    }
    fun loadTweets(){
        var currentUser = mAuth!!.currentUser
        Log.d("myLog","User ${currentUser!!.email} id ${currentUser.uid}")
        if (currentUser!=null) {

            var intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)

            startActivity(intent)
        }
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
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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
    fun btnLoginEvent(view:View){
        loginToFirebase(etMail.text.toString(), etPassword.text.toString())
    }
}
