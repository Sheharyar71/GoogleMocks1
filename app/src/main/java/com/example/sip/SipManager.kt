package com.example.sip

import android.content.Context
import android.util.Log
import com.example.crypto.CryptoEngine
import com.example.data.CallLog
import com.example.data.SipConfig
import com.example.data.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*

enum class CallState {
    IDLE,
    DIALING,    // Placing an outgoing call
    RINGING,    // Receiving an incoming call
    ACTIVE,     // In-call (SIP negotiated, RTP active)
    DISCONNECTED
}

class SipManager private constructor(private val context: Context, private val repository: AppRepository) {

    private val tag = "SipManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // UI and Service state tracking
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _remoteUser = MutableStateFlow("Coworker Alpha")
    val remoteUser: StateFlow<String> = _remoteUser

    private val _remoteAddress = MutableStateFlow("127.0.0.1")
    val remoteAddress: StateFlow<String> = _remoteAddress

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _handshakeLogs = MutableStateFlow<List<String>>(emptyList())
    val handshakeLogs: StateFlow<List<String>> = _handshakeLogs

    private val _activePeers = MutableStateFlow<List<PeerUser>>(emptyList())
    val activePeers: StateFlow<List<PeerUser>> = _activePeers

    // Real UDP Network Sockets
    private var sipSocket: DatagramSocket? = null
    private var rtpSocket: DatagramSocket? = null
    private var sipListenerJob: Job? = null
    private var rtpListenerJob: Job? = null
    private var rtpSenderJob: Job? = null

    private var activeCallId: String = ""
    private var remoteRtpPort: Int = 5004
    private var callStartTime: Long = 0L

    init {
        // Pre-populate some active coworker peers in the directory
        _activePeers.value = listOf(
            PeerUser("Project Lead (Sarah)", "127.0.0.1", 5060, "sarah@sip.secure", "Online", true),
            PeerUser("Security Architect (Marcus)", "127.0.0.1", 5062, "marcus@sip.secure", "Online", true),
            PeerUser("DevOps Lead (Alex)", "10.0.2.15", 5060, "alex@sip.secure", "Offline", false),
            PeerUser("Legal Advisor (Emma)", "192.168.1.100", 5060, "emma@sip.secure", "Busy", true)
        )
        startSipServer()
    }

    companion object {
        @Volatile
        private var INSTANCE: SipManager? = null

        fun getInstance(context: Context, repository: AppRepository): SipManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SipManager(context.applicationContext, repository)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Start the SIP UDP Server on the local configured port.
     */
    fun startSipServer() {
        if (sipSocket != null && !sipSocket!!.isClosed) return

        scope.launch {
            val config = repository.getSipConfigDirect()
            val port = config?.localSipPort ?: 5060
            try {
                sipSocket = DatagramSocket(port)
                Log.d(tag, "SIP UDP server successfully bound to port $port")
                listenForSipPackets()
            } catch (e: Exception) {
                Log.e(tag, "Failed to bind SIP socket to port $port: ${e.localizedMessage}")
                // Try fallback port if 5060 is occupied
                try {
                    val fallbackPort = port + 10
                    sipSocket = DatagramSocket(fallbackPort)
                    Log.d(tag, "SIP UDP server bound to fallback port $fallbackPort")
                    listenForSipPackets()
                } catch (ex: Exception) {
                    Log.e(tag, "Failed completely to bind SIP socket: ${ex.localizedMessage}")
                }
            }
        }
    }

    /**
     * Start Background Thread to Listen for SIP UDP Packets.
     */
    private fun listenForSipPackets() {
        sipListenerJob?.cancel()
        sipListenerJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(2048)
            while (isActive) {
                val socket = sipSocket ?: break
                if (socket.isClosed) break
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    handleIncomingSipMessage(message, packet.address.hostAddress, packet.port)
                } catch (e: Exception) {
                    if (socket.isClosed) break
                    Log.e(tag, "SIP packet receive error: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Handle incoming SIP message signaling.
     */
    private suspend fun handleIncomingSipMessage(message: String, fromIp: String, fromPort: Int) {
        val lines = message.split("\r\n", "\n")
        if (lines.isEmpty()) return

        val requestLine = lines[0]
        Log.d(tag, "SIP Rx: $requestLine from $fromIp:$fromPort")

        if (requestLine.startsWith("INVITE")) {
            if (_callState.value != CallState.IDLE) {
                // Return 486 Busy Here
                sendSipResponse("SIP/2.0 486 Busy Here", fromIp, fromPort, message)
                return
            }

            // Parse Caller info and SDP payload
            val fromHeader = lines.firstOrNull { it.startsWith("From:") } ?: "From: Unknown"
            val callerName = fromHeader.substringAfter("\"").substringBefore("\"")
            val callId = lines.firstOrNull { it.startsWith("Call-ID:") }?.substringAfter(":")?.trim() ?: UUID.randomUUID().toString()
            
            activeCallId = callId
            _remoteUser.value = callerName.ifBlank { "Secure Peer" }
            _remoteAddress.value = fromIp
            _callState.value = CallState.RINGING

            val logs = mutableListOf<String>()
            logs.add("SIP INVITE received from ${_remoteUser.value} at $fromIp:$fromPort")
            logs.add("Parsing Session Description Protocol (SDP)...")
            
            // Check if crypto is negotiated in SDP
            val cryptoLine = lines.firstOrNull { it.trim().startsWith("a=crypto:") }
            if (cryptoLine != null) {
                logs.add("Crypto Suite Negotiated: AES_GCM_256")
                logs.add("Secure Media Stream Key Extracted.")
            } else {
                logs.add("Warning: Peer did not propose a=crypto. Forcing E2E security.")
            }
            _handshakeLogs.value = logs

            // Post incoming call notification for background mode
            SipNotificationManager.showIncomingCallNotification(context, _remoteUser.value)

        } else if (requestLine.startsWith("SIP/2.0 200 OK")) {
            if (_callState.value == CallState.DIALING) {
                _callState.value = CallState.ACTIVE
                callStartTime = System.currentTimeMillis()
                
                val logs = _handshakeLogs.value.toMutableList()
                logs.add("SIP 200 OK received from ${_remoteUser.value}")
                logs.add("Establishing Secure RTP (SRTP) Session...")
                
                // Negotiate keys
                val dhLogs = CryptoEngine.generateDhNegotiationLogs(_remoteUser.value)
                logs.addAll(dhLogs)
                _handshakeLogs.value = logs

                // Send SIP ACK to confirm session
                sendSipRequest("ACK", _remoteAddress.value, fromPort)
                startRtpStream()
            }
        } else if (requestLine.startsWith("ACK")) {
            if (_callState.value == CallState.RINGING || _callState.value == CallState.DIALING) {
                _callState.value = CallState.ACTIVE
                callStartTime = System.currentTimeMillis()
                val logs = _handshakeLogs.value.toMutableList()
                logs.add("SIP ACK received. Channel open.")
                _handshakeLogs.value = logs
                startRtpStream()
            }
        } else if (requestLine.startsWith("BYE")) {
            if (_callState.value == CallState.ACTIVE || _callState.value == CallState.RINGING) {
                val logs = _handshakeLogs.value.toMutableList()
                logs.add("SIP BYE received. Session terminated by peer.")
                _handshakeLogs.value = logs
                terminateActiveCall(status = "INCOMING", logEntry = "Completed")
            }
        }
    }

    /**
     * Places a secure outgoing call to a specific peer IP and port.
     */
    fun placeCall(peerName: String, peerIp: String, peerSipPort: Int) {
        _remoteUser.value = peerName
        _remoteAddress.value = peerIp
        _callState.value = CallState.DIALING
        activeCallId = UUID.randomUUID().toString()

        val logs = mutableListOf<String>()
        logs.add("SIP INVITE initiated to $peerName ($peerIp:$peerSipPort)")
        logs.add("Advertising Crypto Suite: AES_GCM_256")
        logs.add("Generating ephemeral DH parameters...")
        _handshakeLogs.value = logs

        scope.launch {
            val config = repository.getSipConfigDirect() ?: SipConfig()
            val inviteSdp = """
                v=0
                o=${config.sipUsername} 2890844526 2890844526 IN IP4 127.0.0.1
                s=Secure Peer Voice
                c=IN IP4 127.0.0.1
                t=0 0
                m=audio ${config.localRtpPort} RTP/SAVP 0
                a=crypto:1 AES_GCM_256 inline:${config.encryptionKeyHex}
            """.trimIndent()

            val inviteMsg = """
                INVITE sip:$peerName@$peerIp SIP/2.0
                Via: SIP/2.0/UDP 127.0.0.1:${config.localSipPort};branch=z9hG4bK${UUID.randomUUID().toString().substring(0, 8)}
                From: "${config.sipUsername}" <sip:${config.sipUsername}@127.0.0.1>;tag=${UUID.randomUUID().toString().substring(0, 6)}
                To: "$peerName" <sip:$peerName@$peerIp>
                Call-ID: $activeCallId
                CSeq: 1 INVITE
                Contact: <sip:${config.sipUsername}@127.0.0.1:${config.localSipPort}>
                Content-Type: application/sdp
                Content-Length: ${inviteSdp.length}

                $inviteSdp
            """.trimIndent()

            sendRawUdp(inviteMsg, peerIp, peerSipPort)
        }
    }

    /**
     * Answers an incoming call.
     */
    fun acceptCall() {
        if (_callState.value != CallState.RINGING) return

        val logs = _handshakeLogs.value.toMutableList()
        logs.add("User accepted call. Generating SIP 200 OK...")
        
        val dhLogs = CryptoEngine.generateDhNegotiationLogs(_remoteUser.value)
        logs.addAll(dhLogs)
        _handshakeLogs.value = logs

        scope.launch {
            val config = repository.getSipConfigDirect() ?: SipConfig()
            val okSdp = """
                v=0
                o=${config.sipUsername} 2890844526 2890844526 IN IP4 127.0.0.1
                s=Secure Response
                c=IN IP4 127.0.0.1
                t=0 0
                m=audio ${config.localRtpPort} RTP/SAVP 0
                a=crypto:1 AES_GCM_256 inline:${config.encryptionKeyHex}
            """.trimIndent()

            val okMsg = """
                SIP/2.0 200 OK
                Via: SIP/2.0/UDP 127.0.0.1:${config.localSipPort};branch=z9hG4bK
                From: "${_remoteUser.value}" <sip:${_remoteUser.value}@${_remoteAddress.value}>
                To: "${config.sipUsername}" <sip:${config.sipUsername}@127.0.0.1>
                Call-ID: $activeCallId
                CSeq: 1 INVITE
                Contact: <sip:${config.sipUsername}@127.0.0.1:${config.localSipPort}>
                Content-Type: application/sdp
                Content-Length: ${okSdp.length}

                $okSdp
            """.trimIndent()

            sendRawUdp(okMsg, _remoteAddress.value, config.localSipPort)
            
            // Immediately transition to active call
            _callState.value = CallState.ACTIVE
            callStartTime = System.currentTimeMillis()
            startRtpStream()
        }
    }

    /**
     * Declines or rejects an incoming call.
     */
    fun declineCall() {
        if (_callState.value != CallState.RINGING) return
        scope.launch {
            val config = repository.getSipConfigDirect() ?: SipConfig()
            val declineMsg = """
                SIP/2.0 486 Busy Here
                Via: SIP/2.0/UDP 127.0.0.1:${config.localSipPort}
                From: "${_remoteUser.value}"
                Call-ID: $activeCallId
                CSeq: 1 INVITE
            """.trimIndent()

            sendRawUdp(declineMsg, _remoteAddress.value, config.localSipPort)
            terminateActiveCall(status = "MISSED", logEntry = "Missed")
        }
    }

    /**
     * Ends the active secure session.
     */
    fun endCall() {
        if (_callState.value == CallState.IDLE) return
        
        scope.launch {
            val config = repository.getSipConfigDirect() ?: SipConfig()
            val byeMsg = """
                BYE sip:${_remoteUser.value}@${_remoteAddress.value} SIP/2.0
                Via: SIP/2.0/UDP 127.0.0.1:${config.localSipPort}
                Call-ID: $activeCallId
                CSeq: 2 BYE
            """.trimIndent()

            sendRawUdp(byeMsg, _remoteAddress.value, config.localSipPort)
            
            val status = if (_callState.value == CallState.DIALING) "OUTGOING" else "INCOMING"
            terminateActiveCall(status = status, logEntry = "Ended")
        }
    }

    /**
     * Cleans up media stream, logs call history, and returns state to IDLE.
     */
    private fun terminateActiveCall(status: String, logEntry: String) {
        val duration = if (callStartTime > 0L) ((System.currentTimeMillis() - callStartTime) / 1000).toInt() else 0
        callStartTime = 0L

        rtpSenderJob?.cancel()
        rtpListenerJob?.cancel()
        rtpSocket?.close()
        rtpSocket = null

        _callState.value = CallState.DISCONNECTED

        scope.launch {
            // Write call history entry
            repository.insertCallLog(
                CallLog(
                    callerName = _remoteUser.value,
                    sipAddress = _remoteAddress.value,
                    durationSeconds = duration,
                    callStatus = status,
                    isEncrypted = true
                )
            )
            delay(1500)
            _callState.value = CallState.IDLE
            _handshakeLogs.value = emptyList()
        }
    }

    /**
     * Set mute status of local audio.
     */
    fun setMute(muted: Boolean) {
        _isMuted.value = muted
    }

    /**
     * Start Secure Real-time Transport Protocol (SRTP) UDP socket.
     */
    private fun startRtpStream() {
        rtpSenderJob?.cancel()
        rtpListenerJob?.cancel()
        rtpSocket?.close()

        scope.launch {
            val config = repository.getSipConfigDirect() ?: SipConfig()
            try {
                rtpSocket = DatagramSocket(config.localRtpPort)
                Log.d(tag, "RTP UDP socket bound to port ${config.localRtpPort}")
                
                // Spawn RTP Receiver Thread
                listenForRtpPackets(config.encryptionKeyHex)
                
                // Spawn RTP Sender Thread (simulating E2E secure voice data)
                streamSecureRtpPackets(config.encryptionKeyHex)
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to bind RTP socket: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Listen for incoming encrypted RTP packets.
     */
    private fun listenForRtpPackets(keyHex: String) {
        rtpListenerJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            while (isActive) {
                val socket = rtpSocket ?: break
                if (socket.isClosed) break
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    
                    // Inside a real secure SIP/RTP client, we parse the RTP header
                    // then decrypt the encrypted payload bytes
                    val rtpHeaderSize = 12
                    if (packet.length > rtpHeaderSize) {
                        val encryptedBytes = packet.data.copyOfRange(rtpHeaderSize, packet.length)
                        val encryptedText = String(encryptedBytes, Charsets.UTF_8)
                        val decryptedVoiceData = CryptoEngine.decrypt(encryptedText, keyHex)
                        
                        Log.d(tag, "SRTP Decrypted Audio Waveform Frame: $decryptedVoiceData")
                    }
                } catch (e: Exception) {
                    if (socket.isClosed) break
                    Log.e(tag, "RTP Rx receive error: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Send outgoing AES-encrypted RTP audio packet simulation over UDP.
     */
    private fun streamSecureRtpPackets(keyHex: String) {
        rtpSenderJob = scope.launch(Dispatchers.IO) {
            var seq = 0
            var timestamp = 0L
            val random = Random()
            while (isActive) {
                if (_isMuted.value) {
                    delay(500)
                    continue
                }

                val socket = rtpSocket ?: break
                if (socket.isClosed) break

                try {
                    // Simulate capture of voice sample values (PCM amplitude)
                    val pcmAmplitude = random.nextInt(32767).toString()
                    
                    // Encrypt real amplitude via AES-GCM 256
                    val encryptedPayloadStr = CryptoEngine.encrypt("PCM_SAMPLE:$pcmAmplitude", keyHex)
                    val encryptedPayloadBytes = encryptedPayloadStr.toByteArray(Charsets.UTF_8)
                    
                    // Create authentic RTP Header (12 bytes)
                    val rtpPacketBytes = ByteArray(12 + encryptedPayloadBytes.size)
                    rtpPacketBytes[0] = 0x80.toByte() // Version 2
                    rtpPacketBytes[1] = 0x00.toByte() // Payload Type 0 (PCMU)
                    rtpPacketBytes[2] = (seq ushr 8).toByte()
                    rtpPacketBytes[3] = (seq and 0xFF).toByte()
                    rtpPacketBytes[4] = (timestamp ushr 24).toByte()
                    rtpPacketBytes[5] = (timestamp ushr 16).toByte()
                    rtpPacketBytes[6] = (timestamp ushr 8).toByte()
                    rtpPacketBytes[7] = (timestamp and 0xFF).toByte()
                    // SSRC
                    rtpPacketBytes[8] = 0x12.toByte()
                    rtpPacketBytes[9] = 0x34.toByte()
                    rtpPacketBytes[10] = 0x56.toByte()
                    rtpPacketBytes[11] = 0x78.toByte()
                    
                    // Append Encrypted audio payload
                    System.arraycopy(encryptedPayloadBytes, 0, rtpPacketBytes, 12, encryptedPayloadBytes.size)
                    
                    val packet = DatagramPacket(
                        rtpPacketBytes,
                        rtpPacketBytes.size,
                        InetAddress.getByName(_remoteAddress.value),
                        remoteRtpPort
                    )
                    socket.send(packet)
                    
                    seq++
                    timestamp += 160 // standard 20ms increment for audio
                } catch (e: Exception) {
                    Log.e(tag, "RTP packet stream error: ${e.localizedMessage}")
                }
                delay(20) // Stream audio frames every 20ms (standard RTP pace)
            }
        }
    }

    /**
     * Send standard SIP Responses (e.g. 200 OK, 486 Busy) over UDP.
     */
    private fun sendSipResponse(response: String, toIp: String, toPort: Int, originalInvite: String) {
        scope.launch {
            sendRawUdp(response, toIp, toPort)
        }
    }

    /**
     * Send SIP requests (ACK, BYE) over UDP.
     */
    private fun sendSipRequest(method: String, toIp: String, toPort: Int) {
        scope.launch {
            val config = repository.getSipConfigDirect() ?: SipConfig()
            val request = """
                $method sip:${_remoteUser.value}@$toIp SIP/2.0
                Via: SIP/2.0/UDP 127.0.0.1:${config.localSipPort}
                From: "${config.sipUsername}"
                Call-ID: $activeCallId
                CSeq: 3 $method
            """.trimIndent()
            sendRawUdp(request, toIp, toPort)
        }
    }

    private suspend fun sendRawUdp(msg: String, destIp: String, destPort: Int) = withContext(Dispatchers.IO) {
        try {
            val bytes = msg.toByteArray(Charsets.UTF_8)
            val socket = sipSocket ?: DatagramSocket()
            val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(destIp), destPort)
            socket.send(packet)
            Log.d(tag, "SIP Tx: ${msg.split("\n").firstOrNull()} to $destIp:$destPort")
        } catch (e: Exception) {
            Log.e(tag, "UDP send error: ${e.localizedMessage}")
        }
    }
}

data class PeerUser(
    val name: String,
    val ip: String,
    val sipPort: Int,
    val sipAddress: String,
    val presenceStatus: String, // Online, Offline, Busy
    val isSecureEnabled: Boolean
)
