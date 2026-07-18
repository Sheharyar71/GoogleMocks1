package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.SecureBluetoothDevice
import com.example.bluetooth.SecureBluetoothManager
import com.example.crypto.CryptoEngine
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.SipConfig
import com.example.sip.CallState
import com.example.sip.PeerUser
import com.example.sip.SipManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val bluetoothManager: SecureBluetoothManager
    private var sipManager: SipManager? = null

    // Exposed States
    val callLogs = MutableStateFlow<List<com.example.data.CallLog>>(emptyList())
    val sipConfig = MutableStateFlow(SipConfig())
    val callState = MutableStateFlow(CallState.IDLE)
    val remoteUser = MutableStateFlow("Secure Coworker")
    val remoteAddress = MutableStateFlow("127.0.0.1")
    val isMuted = MutableStateFlow(false)
    val handshakeLogs = MutableStateFlow<List<String>>(emptyList())
    val activePeers = MutableStateFlow<List<PeerUser>>(emptyList())

    // Bluetooth States
    val bluetoothDevices = MutableStateFlow<List<SecureBluetoothDevice>>(emptyList())
    val isBluetoothScanning = MutableStateFlow(false)
    val connectedBluetoothDevice = MutableStateFlow<SecureBluetoothDevice?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        bluetoothManager = SecureBluetoothManager(application)

        viewModelScope.launch {
            repository.ensureDefaultConfigExists()
            
            // Connect DB flows
            repository.allCallLogs.collect { logs ->
                callLogs.value = logs
            }
        }

        viewModelScope.launch {
            repository.sipConfig.collect { config ->
                if (config != null) {
                    sipConfig.value = config
                }
            }
        }

        // Connect Bluetooth flows
        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devs ->
                bluetoothDevices.value = devs
            }
        }
        viewModelScope.launch {
            bluetoothManager.isScanning.collect { scanning ->
                isBluetoothScanning.value = scanning
            }
        }
        viewModelScope.launch {
            bluetoothManager.connectedDevice.collect { dev ->
                connectedBluetoothDevice.value = dev
            }
        }
    }

    /**
     * Set the active SipManager once bound to the background service.
     */
    fun attachSipManager(manager: SipManager) {
        this.sipManager = manager
        
        // Pipe flows from SipManager directly into ViewModel exposed states
        viewModelScope.launch {
            manager.callState.collect { state ->
                callState.value = state
            }
        }
        viewModelScope.launch {
            manager.remoteUser.collect { user ->
                remoteUser.value = user
            }
        }
        viewModelScope.launch {
            manager.remoteAddress.collect { addr ->
                remoteAddress.value = addr
            }
        }
        viewModelScope.launch {
            manager.isMuted.collect { muted ->
                isMuted.value = muted
            }
        }
        viewModelScope.launch {
            manager.handshakeLogs.collect { logs ->
                handshakeLogs.value = logs
            }
        }
        viewModelScope.launch {
            manager.activePeers.collect { peers ->
                activePeers.value = peers
            }
        }
    }

    // Call Actions
    fun startCall(peerName: String, peerIp: String, peerPort: Int) {
        sipManager?.placeCall(peerName, peerIp, peerPort)
    }

    fun acceptCall() {
        sipManager?.acceptCall()
    }

    fun declineCall() {
        sipManager?.declineCall()
    }

    fun endCall() {
        sipManager?.endCall()
    }

    fun toggleMute() {
        val newMute = !isMuted.value
        sipManager?.setMute(newMute)
        
        viewModelScope.launch {
            val current = sipConfig.value
            repository.saveSipConfig(current.copy(isMuted = newMute))
        }
    }

    // Settings Updates
    fun updateSipCredentials(username: String, domain: String, portStr: String, password: String) {
        viewModelScope.launch {
            val port = portStr.toIntOrNull() ?: 5060
            val updated = sipConfig.value.copy(
                sipUsername = username,
                sipDomain = domain,
                sipPort = port,
                sipPassword = password
            )
            repository.saveSipConfig(updated)
            // Restart SIP server to bind on updated details
            sipManager?.startSipServer()
        }
    }

    fun updateEncryptionSettings(keyHex: String, srtpEnabled: Boolean, cipherSuite: String) {
        viewModelScope.launch {
            // Validate key hex, otherwise keep current
            val validatedKey = if (keyHex.length >= 8) keyHex else sipConfig.value.encryptionKeyHex
            val updated = sipConfig.value.copy(
                encryptionKeyHex = validatedKey,
                isSrtpEnabled = srtpEnabled,
                selectedCipherSuite = cipherSuite
            )
            repository.saveSipConfig(updated)
        }
    }

    // Call logs
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    // Bluetooth
    fun scanBluetooth() {
        bluetoothManager.startScanning()
    }

    fun connectBluetooth(device: SecureBluetoothDevice) {
        bluetoothManager.connectDevice(device)
        viewModelScope.launch {
            val updated = sipConfig.value.copy(bluetoothDeviceName = device.name)
            repository.saveSipConfig(updated)
        }
    }

    fun disconnectBluetooth() {
        bluetoothManager.disconnectDevice()
        viewModelScope.launch {
            val updated = sipConfig.value.copy(bluetoothDeviceName = "")
            repository.saveSipConfig(updated)
        }
    }
}
