package com.example.gifapplication

import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(),GifOnClickListener {
    private lateinit var mContext:Context
    private lateinit var viewModel: GIFViewModel
    private lateinit var gifList:ArrayList<DataObject>
    private lateinit var gifSearchList:ArrayList<DataObject>
    private lateinit var rvGIF:RecyclerView
    private lateinit var etSearchGIF:TextInputLayout
    private lateinit var progressLay:LinearLayout
    private lateinit var tvError:TextView
    private val apiHandler = Handler()
    private var apiRunnable: Runnable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this
        viewModel = ViewModelProvider(this)[GIFViewModel::class.java]
        rvGIF = findViewById(R.id.rvGIF)
        etSearchGIF = findViewById(R.id.etSearchGIF)
        progressLay = findViewById(R.id.progress_ly)
        tvError = findViewById(R.id.tvError)
        gifList = ArrayList()
        gifSearchList = ArrayList()
        initializer()
        getMVVM()

        etSearchGIF.editText?.doOnTextChanged { text, _, _, _ ->
            progressLay.visibility = View.VISIBLE
            apiRunnable?.let { apiHandler.removeCallbacks(it) }
            apiRunnable = Runnable {
                if (text?.length ?: 0 >= 2) {
                    searchGIF(text.toString())
                }
                if (text?.isEmpty() == true){
                    progressLay.visibility = View.GONE
                    setData(gifList)
                }
            }
            apiHandler.postDelayed(apiRunnable!!, 1500)
        }
    }

    private fun searchGIF(text: String) {
        CoroutineScope(Job()+Dispatchers.IO).launch {
            viewModel.getSearchGif(text)
        }
    }

    private fun initializer() {
        CoroutineScope(Job() + Dispatchers.IO).launch {
            progressLay.visibility = View.VISIBLE
            viewModel.getAllGif()
        }
    }

    private fun getMVVM(){
        viewModel.getGIFDetails().observe(this, Observer {
            if (it != null){
                progressLay.visibility = View.GONE
                if (it.data.size > 0){
                    gifList.clear()
                    gifList = it.data as ArrayList<DataObject>
                    setData(gifList)
                } else {
                    tvError.visibility = View.VISIBLE
                }

            }
        })

        viewModel.getSearchGIFDetails().observe(this, Observer {
            if (it != null){
                progressLay.visibility = View.GONE
                if (it.data.size > 0){
                    gifSearchList.clear()
                    gifSearchList = it.data as ArrayList<DataObject>
                    setData(gifSearchList)
                } else{
                    tvError.visibility = View.VISIBLE
                }

            }
        })
    }

    private fun setData(gifResponse: ArrayList<DataObject>) {
        val gridLayoutManager = GridLayoutManager(this,2)
        val adapter = GIFAdapter(gifResponse,mContext,this)
        rvGIF.layoutManager = gridLayoutManager
        rvGIF.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onGifClick(gifResponse:DataObject) {
        loadGif(gifResponse)
    }

    private fun loadGif(gifResponse: DataObject) {
        val builderDialog: android.app.AlertDialog.Builder =
            android.app.AlertDialog.Builder(this)
        val view: View =
            LayoutInflater.from(this).inflate(R.layout.custom_dialog, null)
        val mDialog = builderDialog.create()
        mDialog.setView(view)
        mDialog.setCancelable(true)
        val ivGif:ImageView = view.findViewById(R.id.ivGIF)
        view.findViewById<TextView>(R.id.tvTitle).text = gifResponse.title
        Glide.with(this).load(gifResponse.images.originalImg.url).placeholder(R.drawable.android_gif).into(ivGif)
        view.findViewById<Button>(R.id.btnDownload).setOnClickListener {
            progressLay.visibility = View.VISIBLE
            if (viewModel.checkPermission(mContext)){
                downloadGif(gifResponse)
                mDialog.dismiss()
            } else {
                if (viewModel.requestPermission(this)){
                    downloadGif(gifResponse)
                    mDialog.dismiss()
                }
            }
        }
        mDialog.show()
    }

    private fun downloadGif(gifResponse: DataObject) {
        CoroutineScope(Job() + Dispatchers.IO).launch {
            Glide.with(mContext)
                .downloadOnly().load(gifResponse.images.originalImg.url)
                .into(object : CustomTarget<File>() {
                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        try {
                            val fileType = "image/gif"
                            val contentValues = ContentValues().apply {
                                put(
                                    MediaStore.MediaColumns.DISPLAY_NAME,
                                    "${gifResponse.userName}.gif"
                                )
                                put(MediaStore.MediaColumns.MIME_TYPE, fileType)
                                put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_DOWNLOADS
                                )
                            }
                            val contentResolver = mContext.contentResolver
                            val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                            val imageUri: Uri? = contentResolver.insert(contentUri, contentValues)
                            imageUri.let {
                                if (it != null) {
                                    contentResolver.openOutputStream(it).use { outPutStream ->
                                        resource.inputStream().use { inputStream ->
                                            if (outPutStream != null) {
                                                inputStream.copyTo(outPutStream)
                                            }
                                        }
                                        runOnUiThread {
                                            Toast.makeText(
                                                mContext,
                                                "GIF downloaded successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                        progressLay.visibility = View.GONE
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        Log.d("cleared", "cleared")
                    }

                })
        }

    }



}