package com.gobinda.connection.picker

import com.gobinda.connection.ConnectionMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class RoomPicker(private val mediator: ConnectionMediator) {

    fun pickOrWait(myRoomId: String) = callbackFlow<String?> {
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
                val sendValue = if (s?.value == null && status) partnerInfo else null
                trySend(sendValue)
                close()
            }
        }
        mediator.waitingRoomRef.runTransaction(transaction)
        awaitClose()
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