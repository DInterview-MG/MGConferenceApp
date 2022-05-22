package org.mg.conferenceapp

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy

object Utils {

    val GSON : Gson = GsonBuilder()
        .setNumberToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
        .create()

    val MAIN_HANDLER = Handler(Looper.getMainLooper())

    // Throws an exception if we're not on the main thread.
    fun checkOnMainThread() {
        if(Looper.getMainLooper() != Looper.myLooper()) {
            throw RuntimeException("Running on invalid thread")
        }
    }
}