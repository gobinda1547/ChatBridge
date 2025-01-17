package com.gobinda.connection.internal

import android.util.Log

private const val LOG_TAG = "web_rtc_conn"

internal fun li(message: String) {
    Log.i(LOG_TAG, message)
}

internal fun le(message: String) {
    Log.e(LOG_TAG, message)
}