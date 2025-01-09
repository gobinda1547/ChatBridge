package com.gobinda.connection.internal

import android.util.Log

private const val LOG_TAG = "web_rtc_conn"

internal fun li(message: String) {
    Log.i(LOG_TAG, message)
}

internal fun li(tag: String, message: String) {
    Log.i(tag + LOG_TAG, message)
}

internal fun le(message: String) {
    Log.e(LOG_TAG, message)
}

internal fun le(tag: String, message: String) {
    Log.e(tag + LOG_TAG, message)
}