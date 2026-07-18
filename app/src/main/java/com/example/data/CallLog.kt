package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val sipAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val callStatus: String, // "INCOMING", "OUTGOING", "MISSED", "REJECTED"
    val isEncrypted: Boolean = true,
    val cipherSuiteUsed: String = "AES-256-GCM"
)
