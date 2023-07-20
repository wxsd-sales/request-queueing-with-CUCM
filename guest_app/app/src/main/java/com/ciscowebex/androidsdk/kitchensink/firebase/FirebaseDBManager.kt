package com.ciscowebex.androidsdk.kitchensink.firebase

import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Comment


object FirebaseDBManager {
    private val database = FirebaseDatabase.getInstance()
    val db = Firebase.firestore
    var count=0
    var myRef = database.getReference().child("Options").child("help-topic-list-data")
    var upRef= database.getReference("Options")
    var dataMap: HashMap<String, String> = HashMap()
    var jsonString:String=""
    var url :String?=null
    var listener : UrlLoadedListener? =null

    var childEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.e("FirebaseDB", "onChildAdded:" + dataSnapshot.key!!)

            // A new comment has been added, add it to the displayed list
            val comment = dataSnapshot.getValue()

            // ...
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.e("FirebaseDB", "onChildChanged: ${dataSnapshot.key}")

            // A comment has changed, use the key to determine if we are displaying this
            // comment and if so displayed the changed comment.
            val newComment = dataSnapshot.getValue()
            val commentKey = dataSnapshot.key

            // ...
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Log.e("FirebaseDB", "onChildRemoved:" + dataSnapshot.value!!)

            // A comment has changed, use the key to determine if we are displaying this
            // comment and if so remove it.
            val commentKey = dataSnapshot.key

            // ...
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.e("FirebaseDB", "onChildMoved:" + dataSnapshot.key!!)

            // A comment has changed position, use the key to determine if we are
            // displaying this comment and if so move it.
            val movedComment = dataSnapshot.getValue()
            val commentKey = dataSnapshot.key

            // ...
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e("FirebaseDB", "postComments:onCancelled", databaseError.toException())
        }
    }

    init {
        fetchUrl()
    }
    fun getData() {
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
//                jsonString = snapshot.value.toString()
//                Log.e("jsonString",jsonString)
//                val jsonObject = JSONArray(jsonString)
//                var index = 0;
//                do {
//                    val item:JSONObject =  jsonObject[index++]  as JSONObject
//                    val type: String = item.get("option_title").toString()
//                    val amount: String = item.get("agent_email").toString()
//                    dataMap.set(type, amount)
//                }while (index<jsonObject.length())

                var agent_email_list : String = ""
                var option_title_list : String = ""
                count=1
                val children = snapshot!!.children
                children.forEach {
                    count++
                    Log.e("children",it.toString())
                    agent_email_list=it.child("agent_email").value!! as String
                    option_title_list=it.child("option_title").value!! as String
                    dataMap.set(option_title_list, agent_email_list)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    fun writeData(option_title: String, agent_email: String) {
        val myclass=MyClass(option_title,agent_email)
//        myRef.child("4").child("option_title").setValue(option_title)
//        myRef.child("4").child("agent_email").setValue(agent_email)
//        val newData= myRef.push()
//        Log.e("newDataKey",newData.key.toString())
        getData()
        Log.e("count",count.toString())
        myRef.child(count.toString()).setValue(myclass)
    }

    fun removeData() {
        val urlRef = database.getReference("Options").child("help-topic-list-data")
        urlRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                url = snapshot.value.toString()
                if(url!=null){
                    snapshot.getRef().removeValue()
                    count=0
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }



    fun fetchUrl(){
        val urlRef = database.getReference("main-screen-background-img")
        urlRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                url = snapshot.value.toString()
                listener?.onLoad()
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    fun setLoadListener(listener:UrlLoadedListener){
        this.listener = listener
        if(url!=null){
            listener?.onLoad()
        }
    }


    interface  UrlLoadedListener {
        fun onLoad()
    }
}