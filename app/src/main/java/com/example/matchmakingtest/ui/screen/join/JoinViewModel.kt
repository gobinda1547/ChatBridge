package com.example.matchmakingtest.ui.screen.join

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmakingtest.app.LOG_TAG
import com.example.matchmakingtest.app.MatchMakingFirebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinViewModel @Inject constructor() : ViewModel() {

    val matchMakingFirebase = MatchMakingFirebase()

    fun handleClick(userId: String) {
        Log.i(LOG_TAG, "handle click invoked once")
        viewModelScope.launch(Dispatchers.IO) {
            val userInfo = MatchMakingFirebase.UserInfo(userId, userId)
            val mates = matchMakingFirebase.requestGame(userInfo)?.toMutableList()
            mates?.let { startGame(mates) }
        }
    }

    fun startGame(mates: List<MatchMakingFirebase.UserInfo>) {
        Log.i(LOG_TAG, "startGame: mates: $mates")
    }
}