package com.example.gifapplication

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GIFViewModel(application: Application):AndroidViewModel(application) {
    internal var gifList:MutableLiveData<GIFResponse> = MutableLiveData()
    internal var gifSearchList:MutableLiveData<GIFResponse> = MutableLiveData()
    private val PERMISSION_REQUEST_CODE = 100


    fun getGIFDetails():MutableLiveData<GIFResponse>{
        return gifList
    }

    fun getSearchGIFDetails():MutableLiveData<GIFResponse>{
        return gifSearchList
    }

    fun getAllGif(){
        val retroClient = RetroClient().getRetrofitInstance(GIFConstants.BASE_URL)?.create(GIFApi::class.java)
        val call = retroClient?.getAllGif(GIFConstants.APP_KEY)
        call?.enqueue(object :Callback<GIFResponse>{
            override fun onResponse(call: Call<GIFResponse>, response: Response<GIFResponse>) {
                if (response.isSuccessful){
                    gifList.value = response.body()
                }
            }

            override fun onFailure(call: Call<GIFResponse>, t: Throwable) {
                Log.e(GIFConstants.TAG,t.message.toString())
            }

        })
    }

    fun getSearchGif(searchTerm:String){
        val retroClient = RetroClient().getRetrofitInstance(GIFConstants.BASE_URL)?.create(GIFApi::class.java)
        val call = retroClient?.getSearchGif(GIFConstants.APP_KEY,searchTerm)
        call?.enqueue(object :Callback<GIFResponse>{
            override fun onResponse(call: Call<GIFResponse>, response: Response<GIFResponse>) {
                if (response.isSuccessful){
                    gifSearchList.value = response.body()
                }
            }

            override fun onFailure(call: Call<GIFResponse>, t: Throwable) {
                Log.e(GIFConstants.TAG,t.message.toString())
            }

        })
    }

    fun checkPermission(context: Context):Boolean {
        val result = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(context: Activity):Boolean {
        return if (ActivityCompat.shouldShowRequestPermissionRationale(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(context.applicationContext, "Write External Storage permission allows us to save files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show()
            false
        } else {
            ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            true
        }
    }
}