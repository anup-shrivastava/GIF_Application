package com.example.gifapplication

import com.google.gson.annotations.SerializedName

data class GIFResponse(
    @SerializedName("data") var data:List<DataObject>
)

data class DataObject(
    @SerializedName("id") var userName:String,
    @SerializedName("title") var title:String,
    @SerializedName("images") var images:DataImage
)

data class DataImage(
    @SerializedName("original") var originalImg:OgImage
)

data class OgImage(
    val url:String,
    val mp4:String
)