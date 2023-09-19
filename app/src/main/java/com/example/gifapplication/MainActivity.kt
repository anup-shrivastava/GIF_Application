package com.example.gifapplication

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
    private val SHARE_GIF_CODE = 123
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SHARE_GIF_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "GIF shared successfully", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Sharing canceled", Toast.LENGTH_SHORT).show()
            }
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
        val adapter = GIFAdapter(gifResponse,viewModel,this)
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
        Glide.with(this).load(gifResponse.images.originalImg.url)
            .apply(viewModel.requestOptionsForGIF())
            .placeholder(viewModel.loadPlaceholderDrawable(mContext))
            .into(ivGif)
        view.findViewById<Button>(R.id.btnShare).setOnClickListener {
            progressLay.visibility = View.VISIBLE
            if (viewModel.checkPermission(mContext)){
                shareGIF(mContext,gifResponse)
                mDialog.dismiss()
            } else {
                if (viewModel.requestPermission(this)){
                    shareGIF(mContext,gifResponse)
                    mDialog.dismiss()
                }
            }
        }
        mDialog.show()
    }
    private fun shareGIF(context: Context, gifResponse:DataObject) {
        CoroutineScope(Job() + Dispatchers.IO).launch {
            val originalImageUrl = gifResponse.images.originalImg.url
            val resizedImageUrl = resizeImage(originalImageUrl, 480, 480)
            val downloadDirectory = File(GIFConstants.ROOT_PATH)
            if (!downloadDirectory.exists()) {
                downloadDirectory.mkdirs()
            }
            val fileName = "${gifResponse.userName}.gif"
            val filePath = "${GIFConstants.ROOT_PATH}/$fileName"

            val existingFile = Files.deleteIfExists(Paths.get(filePath))
//            if (existingFile.exists()) {
//                existingFile.delete()
//            }
            Glide.with(mContext)
                .downloadOnly().load(resizedImageUrl)
                .into(object : CustomTarget<File>() {
                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        try {
                            val fileType = "image/gif"
                            val contentValues = ContentValues().apply {
                                put(
                                    MediaStore.MediaColumns.DISPLAY_NAME,
                                    fileName
                                )
                                put(MediaStore.MediaColumns.MIME_TYPE, fileType)
                                put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    GIFConstants.ROOT_PATH
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
                                    }
                                    runOnUiThread {
                                        Toast.makeText(
                                            mContext,
                                            "GIF downloaded successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "image/webp"
                                        putExtra(Intent.EXTRA_STREAM, imageUri)
                                        putExtra(Intent.EXTRA_TEXT, gifResponse.title)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivityForResult(Intent.createChooser(shareIntent, "Share using"),SHARE_GIF_CODE)
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

    private fun resizeImage(originalImageUrl: String, width: Int, height: Int): String {
        return "$originalImageUrl?w=$width&h=$height" // Append query parameters to resize the image
    }
}