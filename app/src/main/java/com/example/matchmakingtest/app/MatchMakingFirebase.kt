package com.example.matchmakingtest.app

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull

class MatchMakingFirebase {

    suspend fun requestGame(userInfo: UserInfo): List<UserInfo>? {
        val myRef = FirebaseDatabase.getInstance().getReference("GameRoom")
        return try {
            tryToWrite(myRef, userInfo).firstOrNull()?.toList()?.map { item ->
                Log.i(LOG_TAG, "current item = $item")
                UserInfo(item.first, item.second)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tryToWrite(dbRef: DatabaseReference, userInfo: UserInfo) = callbackFlow {
        val transaction = object : Transaction.Handler {
            var userList: Map<String, UserInfoInner> = mapOf()

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val userInfoMap = convertReceivedDataIntoMap(currentData.value)
                if (userInfoMap.size == 3) {
                    userList = userInfoMap
                    currentData.value = null
                    return Transaction.success(currentData)
                }
                if (userInfoMap.size < 4) {
                    userInfoMap[userInfo.userId] = userInfo.inner
                    currentData.value = userInfoMap
                    return Transaction.success(currentData)
                }
                return Transaction.abort()
            }

            override fun onComplete(e: DatabaseError?, status: Boolean, s: DataSnapshot?) {
                val sendValue = if (status) userList else null
                trySend(sendValue)
                close()
            }
        }
        dbRef.runTransaction(transaction)
        awaitClose()
    }


    fun convertReceivedDataIntoMap(obj: Any?): MutableMap<String, UserInfoInner> {
        return try {
            @Suppress("UNCHECKED_CAST")
            obj as MutableMap<String, UserInfoInner>
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    data class UserInfo(val userId: String, val inner: UserInfoInner)

    data class UserInfoInner(val name: String)
}