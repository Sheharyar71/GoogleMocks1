package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SipConfigDao {
    @Query("SELECT * FROM sip_config WHERE id = 1 LIMIT 1")
    fun getSipConfigFlow(): kotlinx.coroutines.flow.Flow<SipConfig?>

    @Query("SELECT * FROM sip_config WHERE id = 1 LIMIT 1")
    suspend fun getSipConfig(): SipConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSipConfig(config: SipConfig)
}
