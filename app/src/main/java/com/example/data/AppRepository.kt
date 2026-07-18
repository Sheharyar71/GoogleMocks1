package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val database: AppDatabase) {
    val allCallLogs: Flow<List<CallLog>> = database.callLogDao().getAllCallLogsFlow()
    val sipConfig: Flow<SipConfig?> = database.sipConfigDao().getSipConfigFlow()

    suspend fun insertCallLog(callLog: CallLog) = withContext(Dispatchers.IO) {
        database.callLogDao().insertCallLog(callLog)
    }

    suspend fun clearCallLogs() = withContext(Dispatchers.IO) {
        database.callLogDao().clearAllCallLogs()
    }

    suspend fun getSipConfigDirect(): SipConfig? = withContext(Dispatchers.IO) {
        database.sipConfigDao().getSipConfig()
    }

    suspend fun saveSipConfig(config: SipConfig) = withContext(Dispatchers.IO) {
        database.sipConfigDao().insertSipConfig(config)
    }

    suspend fun ensureDefaultConfigExists() = withContext(Dispatchers.IO) {
        if (database.sipConfigDao().getSipConfig() == null) {
            database.sipConfigDao().insertSipConfig(SipConfig())
        }
    }
}
