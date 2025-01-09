package com.gobinda.connection.log

import android.util.Log

private const val LOG_TAG = "web_rtc_conn"

fun li(message: String) {
    Log.i(LOG_TAG, message)
}

fun li(tag: String, message: String) {
    Log.i(tag + LOG_TAG, message)
}

fun le(message: String) {
    Log.e(LOG_TAG, message)
}

fun le(tag: String, message: String) {
    Log.e(tag + LOG_TAG, message)
}