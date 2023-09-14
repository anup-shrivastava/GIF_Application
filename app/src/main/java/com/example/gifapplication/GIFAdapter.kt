package com.example.gifapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GIFAdapter(private var gifResponse: ArrayList<DataObject>, private var mContext:Context,private var gifOnClickListener: GifOnClickListener) : RecyclerView.Adapter<GIFAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_gif_adapter, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (gifResponse.size > 0) gifResponse.size else 0
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val gif = gifResponse[position]
        Glide.with(holder.itemView.context).load(gif.images.originalImg.url).placeholder(R.drawable.android_gif).into(holder.ivGIF)
        holder.itemView.setOnClickListener {
            gifOnClickListener.onGifClick(gif)
        }
    }
    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        var ivGIF:ImageView = itemView.findViewById(R.id.ivGIFSmall)
    }
}