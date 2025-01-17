package com.example.matchmakingtest.app

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.HashMap

class MatchMakingFirebase {

    suspend fun requestGame(userInfo: UserInfo): List<UserInfo>? {
        val myRef = FirebaseDatabase.getInstance().getReference("GameRoom")
        return tryToWrite(myRef, userInfo).firstOrNull()?.toList()?.map {
            UserInfo(it.first, it.second)
        }
    }

    private fun tryToWrite(dbRef: DatabaseReference, userInfo: UserInfo) = callbackFlow {
        val transaction = object : Transaction.Handler {
            var userList: MutableMap<String, UserInfoInner> = mutableMapOf()

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val userInfoMap = convertIntoUserInfoMap(currentData.value)
                if (userInfoMap.size == 3) {
                    userList = userInfoMap
                    currentData.value = HashMap<String, UserInfo>()
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
                val sendValue = if (s?.value == null && status) userList else null
                trySend(sendValue)
                close()
            }
        }
        dbRef.runTransaction(transaction)
        awaitClose()
    }

    private fun convertIntoUserInfoMap(obj: Any?): MutableMap<String, UserInfoInner> {
        val finalConversion = mutableMapOf<String, UserInfoInner>()
        try {
            @Suppress("UNCHECKED_CAST")
            val initialConversion = obj as MutableMap<String, Any>
            for ((key, value) in initialConversion) {
                finalConversion[key] = convertToUserInfoInner(value)
            }
        } catch (_: Exception) {
        }
        return finalConversion
    }

    private fun convertToUserInfoInner(any: Any): UserInfoInner {
        @Suppress("UNCHECKED_CAST")
        val itemAsMap = any as HashMap<String, String>
        return UserInfoInner(
            name = itemAsMap["name"]!!
        )
    }

    data class UserInfo(val userId: String, val inner: UserInfoInner) {
        override fun toString(): String {
            return userId
        }
    }

    data class UserInfoInner(val name: String)
}