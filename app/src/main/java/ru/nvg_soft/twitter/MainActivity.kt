package ru.nvg_soft.twitter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private var database= FirebaseDatabase.getInstance()
    private var myRef=database.reference
    var ListTweets = ArrayList<Ticket>()
    var adpater:MyTweetAdpater?=null
    var myemail:String?=null
    var userUID:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var b:Bundle=intent.extras!!
        myemail=b.getString("email")
        userUID=b.getString("uid")
        //Dummy data

        ListTweets.add(Ticket("0","","url","add"))
//        ListTweets.add(Ticket("0","him","url","user1"))
//        ListTweets.add(Ticket("0","здесь будет очень длинный текст для проверки перненрса по строкам, надеюсь что все у меня получится и ничего не пропадет","url","daniil"))
//        ListTweets.add(Ticket("0","здесь будет очень длинный текст для проверки перненрса по строкам, надеюсь что все у меня получится и ничего не пропадет","url","daniil"))


        adpater= MyTweetAdpater(this,ListTweets)
        lvTweets.adapter=adpater

        LoadPost()


    }

    inner class  MyTweetAdpater: BaseAdapter {
        var listNotesAdpater=ArrayList<Ticket>()
        var context: Context?=null
        constructor(context:Context, listNotesAdpater:ArrayList<Ticket>):super(){
            this.listNotesAdpater=listNotesAdpater
            this.context=context
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {


            var mytweet=listNotesAdpater[p0]

            if(mytweet.tweetPersonUID.equals("add")) {
                var myView = layoutInflater.inflate(R.layout.add_ticket, null)

                myView.iv_attach.setOnClickListener(View.OnClickListener {
                    loadImage()

                })

                myView.iv_post.setOnClickListener(View.OnClickListener {
                    //upload server

                    myRef.child("posts").push().setValue(
                        PostInfo(userUID!!,
                            myView.etPost.text.toString(), downloadURL!!))

                    myView.etPost.setText("")
                })
                return myView
            } else if(mytweet.tweetPersonUID.equals("loading")){
                var myView=layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            } else if(mytweet.tweetPersonUID.equals("ads")){
                var myView=layoutInflater.inflate(R.layout.ads_ticket,null)

                var mAdView = myView.findViewById(R.id.adView) as AdView
                val adRequest = AdRequest.Builder().build()
                mAdView.loadAd(adRequest)
                return myView
            }else{
                var myView=layoutInflater.inflate(R.layout.tweets_ticket,null)
                myView.txt_tweet.text = mytweet.tweetText

                //myView.tweet_picture.setImageURI(mytweet.tweetImageURL)
                Picasso.with(context).load(mytweet.tweetImageURL).into(myView.tweet_picture)


                myRef.child("Users").child(mytweet.tweetPersonUID!!)
                    .addValueEventListener(object : ValueEventListener {

                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            try {

                                var td= dataSnapshot!!.value as HashMap<String,Any>

                                for(key in td.keys){

                                    var userInfo= td[key] as String
                                    if(key.equals("ProfileImage")){
                                        Picasso.with(context).load(userInfo).into(myView.picture_path)
                                    }else{
                                        myView.txtUserName.text = userInfo
                                    }



                                }

                            }catch (ex:Exception){}


                        }

                        override fun onCancelled(p0: DatabaseError) {

                        }
                    })

                return myView
            }



        }

        override fun getItem(p0: Int): Any {
            return listNotesAdpater[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {

            return listNotesAdpater.size

        }



    }
    //load image
    val PICK_IMAGE_CODE = 123
    fun loadImage(){
        var intent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent,PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
      //  Log.d("myLog","OnActivityResult")
        if(requestCode==PICK_IMAGE_CODE  && data!=null && resultCode == RESULT_OK){
         //   Log.d("myLog","OnActivityResult2")
            val selectedImage=data.data
            val filePathColum= arrayOf(MediaStore.Images.Media.DATA)
            val cursor= contentResolver.query(selectedImage!!,filePathColum,null,null,null)
            cursor!!.moveToFirst()
            val coulomIndex=cursor!!.getColumnIndex(filePathColum[0])
            val picturePath=cursor!!.getString(coulomIndex)
            cursor!!.close()
        //    Log.d("myLog","PicturePath $picturePath")
            uploadImage(BitmapFactory.decodeFile(picturePath))
        }

    }
    var downloadURL:String?=""
    fun uploadImage(bitmap:Bitmap){
        ListTweets.add(0,Ticket("0","him","url","loading"))
        adpater!!.notifyDataSetChanged()

        val storage= FirebaseStorage.getInstance()
        val storgaRef=storage.getReferenceFromUrl("gs://twitter-67bcd.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataobj= Date()
        val imagePath= splitEmail(myemail!!) + "."+ df.format(dataobj)+ ".jpg"
        val ImageRef=storgaRef.child("images/"+imagePath )
        val baos= ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data= baos.toByteArray()
        val uploadTask=ImageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"fail to upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { taskSnapshot ->

            downloadURL= taskSnapshot.storage.downloadUrl.toString()!!
            ListTweets.removeAt(0)
            adpater!!.notifyDataSetChanged()

        }
    }

    fun splitEmail(email: String):String{
        return email.split("@")[0]
    }

    fun LoadPost(){

        myRef.child("posts")
            .addValueEventListener(object :ValueEventListener{

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    try {

                        ListTweets.clear()
                        ListTweets.add(Ticket("0","him","url","add"))
                        ListTweets.add(Ticket("0","him","url","ads"))
                        var td= dataSnapshot!!.value as HashMap<String,Any>

                        for(key in td.keys){

                            var post= td[key] as HashMap<String,Any>

                            ListTweets.add(Ticket(key,

                                post["text"] as String,
                                post["postImage"] as String
                                ,post["userUID"] as String))


                        }


                        adpater!!.notifyDataSetChanged()
                    }catch (ex:Exception){}


                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
    }

}
