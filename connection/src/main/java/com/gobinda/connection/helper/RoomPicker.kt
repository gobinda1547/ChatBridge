package com.gobinda.connection.helper

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class RoomPicker(database: FirebaseDatabase) {

    private val waitingRoomRef: DatabaseReference = database.getReference("waiting")

    fun pickOrWait(myRoomId: String, timeout: Long) = callbackFlow<String?> {
        val currentJob = launch {
            delay(timeout)
            trySend(null)
            close()
        }
        val transaction = object : Transaction.Handler {
            var partnerInfo: String? = null

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                currentData.getValue(String::class.java)?.let { partnerInfo ->
                    this.partnerInfo = partnerInfo
                    currentData.value = null
                    return Transaction.success(currentData)
                }
                currentData.value = myRoomId
                return Transaction.success(currentData)
            }

            override fun onComplete(e: DatabaseError?, status: Boolean, s: DataSnapshot?) {
                val retValue = when {
                    s?.value != null && status && e == null -> myRoomId
                    s?.value == null && status && e == null -> partnerInfo
                    else -> null
                }
                trySend(retValue)
                close()
            }
        }
        waitingRoomRef.runTransaction(transaction)
        awaitClose { currentJob.cancel() }
    }

    private fun convertDataToStringList(data: Any?): MutableList<String> {
        val inputList = data as? List<*> ?: return mutableListOf()
        return inputList.mapNotNull { it as? String }.toMutableList()
    }

    private data class SharedInfo(
        val myRoomId: String,
        val myTimeStamp: Long
    )

}