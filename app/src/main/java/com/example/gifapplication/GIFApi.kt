package com.example.gifapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GIFApi {
    @GET("trending")
    fun getAllGif(
        @Query("api_key") app_key:String
    ):Call<GIFResponse>

    @GET("search")
    fun getSearchGif(
        @Query("api_key") app_key:String,
        @Query("q") searchTerm:String
    ):Call<GIFResponse>
}