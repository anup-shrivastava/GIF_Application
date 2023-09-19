package com.example.gifapplication

import android.os.Environment

class GIFConstants {
    companion object{
        var APP_KEY = "APdSDivo9D6ywGo6n5pgcN9KVpg3bZnW"
        var TAG = "gif_application"
        var BASE_URL = "https://api.giphy.com/v1/gifs/"
        var ROOT_PATH = "${Environment.DIRECTORY_DOWNLOADS}/GIF"
    }
}