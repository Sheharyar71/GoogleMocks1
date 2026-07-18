package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sip_config")
data class SipConfig(
    @PrimaryKey val id: Int = 1, // Single record
    val sipUsername: String = "coworker_alpha",
    val sipDomain: String = "127.0.0.1",
    val sipPort: Int = 5060,
    val sipPassword: String = "tactical_pass_99",
    val encryptionKeyHex: String = "4142434445464748494a4b4c4d4e4f505152535455565758595a313233343536", // 256-bit key in Hex (ABCDEF...)
    val selectedCipherSuite: String = "AES-256-GCM",
    val isSrtpEnabled: Boolean = true,
    val localSipPort: Int = 5060,
    val localRtpPort: Int = 5004,
    val isMuted: Boolean = false,
    val bluetoothDeviceName: String = ""
)
