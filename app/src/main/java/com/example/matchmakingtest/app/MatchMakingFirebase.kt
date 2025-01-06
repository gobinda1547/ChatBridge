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
        return tryToWrite(myRef, userInfo).firstOrNull()
    }

    private fun tryToWrite(dbRef: DatabaseReference, userInfo: UserInfo) = callbackFlow {
        val uniqueKey: String? = dbRef.push().key

        if (uniqueKey == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val transaction = object : Transaction.Handler {
            var userList: List<UserInfo> = mutableListOf()

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val userInfoMap = convertIntoUnreadable(currentData.value)
                if (userInfoMap.size == 3) {
                    userList = convertIntoReadable(userInfoMap.values.toList())
                    currentData.value = HashMap<String, UserInfo>()
                    return Transaction.success(currentData)
                }
                if (userInfoMap.size < 4) {
                    userInfoMap[uniqueKey] = userInfo
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

    private fun convertIntoUnreadable(obj: Any?): MutableMap<Any, Any> {
        return try {
            @Suppress("UNCHECKED_CAST")
            obj as MutableMap<Any, Any>
        } catch (_: Exception) {
            mutableMapOf<Any, Any>()
        }
    }

    fun convertIntoReadable(list: List<Any>): List<UserInfo> {
        @Suppress("UNCHECKED_CAST")
        return list.map { itemAsAny ->
            val itemAsMap = itemAsAny as HashMap<String, String>
            UserInfo(
                userId = itemAsMap["userId"]!!,
                name = itemAsMap["name"]!!
            )
        }
    }

    data class UserInfo(val userId: String, val name: String) {
        override fun toString(): String {
            return userId
        }
    }
}