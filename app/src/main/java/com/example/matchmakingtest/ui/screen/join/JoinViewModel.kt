package com.example.matchmakingtest.ui.screen.join

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmakingtest.app.LOG_TAG
import com.example.matchmakingtest.app.MatchMakingFirebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinViewModel @Inject constructor() : ViewModel() {

    val matchMakingFirebase = MatchMakingFirebase()

    fun handleClick2() {
        Log.i(LOG_TAG, "handle click invoked once")
        for (i in 4..4) {
            val currentId = ('a' + (i - 1)).toString()
            val userInner = MatchMakingFirebase.UserInfoInner(currentId)
            val userInfo = MatchMakingFirebase.UserInfo(currentId, userInner)
            viewModelScope.launch(Dispatchers.IO) {
                val mates = matchMakingFirebase.requestGame(userInfo)?.toMutableList()
                mates?.add(userInfo)
                if (mates?.size == 4) {
                    Log.i(LOG_TAG, "found a match -> players : $mates")
                }
            }
        }
    }
}