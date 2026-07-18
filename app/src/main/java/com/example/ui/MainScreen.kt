package com.example.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.SecureBluetoothDevice
import com.example.data.CallLog
import com.example.data.SipConfig
import com.example.sip.CallState
import com.example.sip.PeerUser
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val remoteUser by viewModel.remoteUser.collectAsStateWithLifecycle()
    val remoteAddress by viewModel.remoteAddress.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val handshakeLogs by viewModel.handshakeLogs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(SecureBlack)) {
        // Switch between Call Overlay or Normal Tab view based on CallState
        if (callState == CallState.ACTIVE || callState == CallState.DIALING || callState == CallState.DISCONNECTED) {
            CallScreen(
                state = callState,
                remoteUser = remoteUser,
                remoteAddress = remoteAddress,
                isMuted = isMuted,
                handshakeLogs = handshakeLogs,
                onMuteToggle = { viewModel.toggleMute() },
                onEndCall = { viewModel.endCall() }
            )
        } else if (callState == CallState.RINGING) {
            IncomingCallScreen(
                remoteUser = remoteUser,
                remoteAddress = remoteAddress,
                onAnswer = { viewModel.acceptCall() },
                onDecline = { viewModel.declineCall() }
            )
        } else {
            // Normal Bottom Navigation App Layout
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = SecureCharcoal,
                        tonalElevation = 8.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("Peers", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberGreen,
                                selectedTextColor = CyberGreen,
                                indicatorColor = TacticalTeal,
                                unselectedIconColor = SecureTextMuted,
                                unselectedTextColor = SecureTextMuted
                            ),
                            icon = { Icon(Icons.Default.People, contentDescription = "Coworker Directory") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberGreen,
                                selectedTextColor = CyberGreen,
                                indicatorColor = TacticalTeal,
                                unselectedIconColor = SecureTextMuted,
                                unselectedTextColor = SecureTextMuted
                            ),
                            icon = { Icon(Icons.Default.History, contentDescription = "Secure Call Logs") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            label = { Text("Bluetooth", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberGreen,
                                selectedTextColor = CyberGreen,
                                indicatorColor = TacticalTeal,
                                unselectedIconColor = SecureTextMuted,
                                unselectedTextColor = SecureTextMuted
                            ),
                            icon = { Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth Earpiece Config") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberGreen,
                                selectedTextColor = CyberGreen,
                                indicatorColor = TacticalTeal,
                                unselectedIconColor = SecureTextMuted,
                                unselectedTextColor = SecureTextMuted
                            ),
                            icon = { Icon(Icons.Default.Shield, contentDescription = "SIP & Encryption Config") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SecureBlack)
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> PeerDirectoryTab(viewModel)
                        1 -> CallLogsTab(viewModel)
                        2 -> BluetoothTab(viewModel)
                        3 -> SettingsTab(viewModel) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cryptographic configurations updated securely.")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- 1. PEER DIRECTORY TAB ----------------------
@Composable
fun PeerDirectoryTab(viewModel: MainViewModel) {
    val activePeers by viewModel.activePeers.collectAsStateWithLifecycle()
    var showManualDial by remember { mutableStateOf(false) }

    var peerNameInput by remember { mutableStateOf("") }
    var peerIpInput by remember { mutableStateOf("127.0.0.1") }
    var peerPortInput by remember { mutableStateOf("5060") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECURE DIRECTORY",
                    color = SecureTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Coworkers registered on secure SIP server",
                    color = SecureTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Quick dial button
            IconButton(
                onClick = { showManualDial = !showManualDial },
                modifier = Modifier
                    .background(SecureSlate, shape = CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (showManualDial) Icons.Default.Close else Icons.Default.Call,
                    contentDescription = "Manual Call Toggle",
                    tint = CyberGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collapsible Manual SIP URI Dialer row
        AnimatedVisibility(visible = showManualDial) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANUAL PEER-TO-PEER SIP CALL",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = peerNameInput,
                        onValueChange = { peerNameInput = it },
                        label = { Text("Peer/Coworker Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            focusedLabelColor = CyberGreen,
                            unfocusedBorderColor = SecureSlate,
                            unfocusedLabelColor = SecureTextMuted,
                            focusedTextColor = SecureTextWhite,
                            unfocusedTextColor = SecureTextWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("manual_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = peerIpInput,
                            onValueChange = { peerIpInput = it },
                            label = { Text("Peer IP / Domain") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                focusedLabelColor = CyberGreen,
                                unfocusedBorderColor = SecureSlate,
                                unfocusedLabelColor = SecureTextMuted,
                                focusedTextColor = SecureTextWhite,
                                unfocusedTextColor = SecureTextWhite
                            ),
                            modifier = Modifier.weight(1.5f).testTag("manual_ip_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = peerPortInput,
                            onValueChange = { peerPortInput = it },
                            label = { Text("SIP Port") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                focusedLabelColor = CyberGreen,
                                unfocusedBorderColor = SecureSlate,
                                unfocusedLabelColor = SecureTextMuted,
                                focusedTextColor = SecureTextWhite,
                                unfocusedTextColor = SecureTextWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("manual_port_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (peerNameInput.isNotBlank()) {
                                val port = peerPortInput.toIntOrNull() ?: 5060
                                viewModel.startCall(peerNameInput, peerIpInput, port)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = OnCyberGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("manual_dial_submit")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIATE SECURE HANDSHAKE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("peer_list")
        ) {
            items(activePeers) { peer ->
                PeerItemCard(
                    peer = peer,
                    onCallClick = {
                        viewModel.startCall(peer.name, peer.ip, peer.sipPort)
                    }
                )
            }
        }
    }
}

@Composable
fun PeerItemCard(peer: PeerUser, onCallClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Presence indicator icon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(SecureSlate, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SecureTextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (peer.presenceStatus) {
                                    "Online" -> SecureTextSuccess
                                    "Busy" -> WarningAmber
                                    else -> SecureTextMuted
                                },
                                shape = CircleShape
                            )
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = peer.name,
                        color = SecureTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${peer.sipAddress} • Port ${peer.sipPort}",
                        color = SecureTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .background(CyberGreen, shape = CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place Secure call to ${peer.name}",
                    tint = OnCyberGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ---------------------- 2. CALL LOGS TAB ----------------------
@Composable
fun CallLogsTab(viewModel: MainViewModel) {
    val logs by viewModel.callLogs.collectAsStateWithLifecycle()
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECURE DISCUSSIONS LOG",
                    color = SecureTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All stored records are kept on device database",
                    color = SecureTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Wipe audit trail logs button
            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearLogs() },
                    modifier = Modifier
                        .background(SecureTextError.copy(alpha = 0.15f), shape = CircleShape)
                        .size(40.dp)
                        .testTag("wipe_logs_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Sanitize discussion records",
                        tint = SecureTextError
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Empty secure logs",
                        tint = SecureSlate,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO RECENT DISCUSSION LOGS",
                        color = SecureTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "All historic discussions are cleanly sanitized.",
                        color = SecureTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().testTag("logs_list")) {
                items(logs) { log ->
                    CallLogItemCard(log = log, sdf = sdf)
                }
            }
        }
    }
}

@Composable
fun CallLogItemCard(log: CallLog, sdf: SimpleDateFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = when (log.callStatus) {
                                "INCOMING" -> SecureTextSuccess.copy(alpha = 0.1f)
                                "OUTGOING" -> TacticalTeal.copy(alpha = 0.1f)
                                else -> SecureTextError.copy(alpha = 0.1f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (log.callStatus) {
                            "INCOMING" -> Icons.Default.CallReceived
                            "OUTGOING" -> Icons.Default.CallMade
                            else -> Icons.Default.PhoneMissed
                        },
                        contentDescription = null,
                        tint = when (log.callStatus) {
                            "INCOMING" -> SecureTextSuccess
                            "OUTGOING" -> TacticalTeal
                            else -> SecureTextError
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = log.callerName,
                        color = SecureTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        color = SecureTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (log.durationSeconds > 0) "${log.durationSeconds}s" else "No answer",
                    color = SecureTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                Surface(
                    color = CyberGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = log.cipherSuiteUsed,
                            color = SecureTextSuccess,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- 3. BLUETOOTH COMM TAB ----------------------
@Composable
fun BluetoothTab(viewModel: MainViewModel) {
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isBluetoothScanning.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedBluetoothDevice.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BLUETOOTH AUDIO LINK",
                    color = SecureTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pair and connect secure wireless headsets",
                    color = SecureTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(
                onClick = { viewModel.scanBluetooth() },
                modifier = Modifier
                    .background(if (isScanning) CyberGreen.copy(alpha = 0.2f) else SecureSlate, shape = CircleShape)
                    .size(40.dp)
                    .testTag("scan_bt_btn")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = CyberGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Scan devices", tint = CyberGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Connection Details Header
        if (connectedDevice != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberGreen.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp), // Using subtle green border
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Active Headset",
                            tint = CyberGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = connectedDevice!!.name,
                                color = SecureTextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "E2E Link Active (${connectedDevice!!.address})",
                                color = SecureTextSuccess,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.disconnectBluetooth() },
                        colors = ButtonDefaults.buttonColors(containerColor = SecureTextError.copy(alpha = 0.2f), contentColor = SecureTextError),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp).testTag("disconnect_bt_btn")
                    ) {
                        Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = "DISCOVERED SECURE PERIPHERALS",
            color = SecureTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize().testTag("bt_devices_list")) {
            items(bluetoothDevices) { device ->
                BluetoothDeviceCard(
                    device = device,
                    onConnectClick = { viewModel.connectBluetooth(device) }
                )
            }
        }
    }
}

@Composable
fun BluetoothDeviceCard(device: SecureBluetoothDevice, onConnectClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    tint = if (device.isConnected) CyberGreen else SecureTextMuted,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = device.name,
                        color = SecureTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${device.address} • Signal: ${device.signalStrength}dBm",
                        color = SecureTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (device.isConnected) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Connected", tint = CyberGreen)
            } else {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SecureSlate, contentColor = SecureTextWhite),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp).testTag("connect_bt_${device.address}")
                ) {
                    Text("PAIR LINK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------- 4. SECURITY SETTINGS TAB ----------------------
@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    onSaveCallback: () -> Unit
) {
    val config by viewModel.sipConfig.collectAsStateWithLifecycle()

    var username by remember(config) { mutableStateOf(config.sipUsername) }
    var domain by remember(config) { mutableStateOf(config.sipDomain) }
    var sipPort by remember(config) { mutableStateOf(config.sipPort.toString()) }
    var password by remember(config) { mutableStateOf(config.sipPassword) }

    var keyHex by remember(config) { mutableStateOf(config.encryptionKeyHex) }
    var srtpEnabled by remember(config) { mutableStateOf(config.isSrtpEnabled) }
    var selectedCipher by remember(config) { mutableStateOf(config.selectedCipherSuite) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SECURITY ENGINE SHIELD",
            color = SecureTextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure SIP endpoints & custom military-grade encryption",
            color = SecureTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("settings_scroll")
        ) {
            // SIP Authentication configurations card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CyberGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIP CREDENTIALS & SERVICE CONFIG",
                                color = SecureTextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("SIP Username / Authorization ID") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen, focusedLabelColor = CyberGreen,
                                unfocusedBorderColor = SecureSlate, unfocusedTextColor = SecureTextWhite,
                                focusedTextColor = SecureTextWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("sip_username_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = domain,
                                onValueChange = { domain = it },
                                label = { Text("SIP Server Address") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberGreen, focusedLabelColor = CyberGreen,
                                    unfocusedBorderColor = SecureSlate, unfocusedTextColor = SecureTextWhite,
                                    focusedTextColor = SecureTextWhite
                                ),
                                modifier = Modifier.weight(1.5f).testTag("sip_domain_field")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = sipPort,
                                onValueChange = { sipPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberGreen, focusedLabelColor = CyberGreen,
                                    unfocusedBorderColor = SecureSlate, unfocusedTextColor = SecureTextWhite,
                                    focusedTextColor = SecureTextWhite
                                ),
                                modifier = Modifier.weight(1f).testTag("sip_port_field")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("SIP Auth Password") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen, focusedLabelColor = CyberGreen,
                                unfocusedBorderColor = SecureSlate, unfocusedTextColor = SecureTextWhite,
                                focusedTextColor = SecureTextWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("sip_password_field")
                        )
                    }
                }
            }

            // Cryptographic E2E Keys configuration card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "END-TO-END CRYPTO ALGORITHMS",
                                color = SecureTextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = keyHex,
                            onValueChange = { keyHex = it },
                            label = { Text("Master Encryption Hex Key (256-bit)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen, focusedLabelColor = CyberGreen,
                                unfocusedBorderColor = SecureSlate, unfocusedTextColor = SecureTextWhite,
                                focusedTextColor = SecureTextWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("crypto_key_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle SRTP security
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Enforce SRTP Payload Encryption", color = SecureTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Protects voice packet streams dynamically", color = SecureTextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = srtpEnabled,
                                onCheckedChange = { srtpEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnCyberGreen,
                                    checkedTrackColor = CyberGreen,
                                    uncheckedThumbColor = SecureSlate,
                                    uncheckedTrackColor = SecureCharcoal
                                ),
                                modifier = Modifier.testTag("srtp_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cipher suite indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cryptographic Cipher Suite", color = SecureTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("End-to-End hardware authenticated", color = SecureTextMuted, fontSize = 11.sp)
                            }
                            Surface(
                                color = SecureSlate,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = selectedCipher,
                                    color = CyberGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Local networking details card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecureCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyberGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOCAL ENDPOINT INFORMATION",
                                color = SecureTextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SIP Listen Port", color = SecureTextMuted, fontSize = 13.sp)
                            Text("UDP ${config.localSipPort}", color = SecureTextWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("RTP Media Stream Port", color = SecureTextMuted, fontSize = 13.sp)
                            Text("UDP ${config.localRtpPort}", color = SecureTextWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Save security configurations button
        Button(
            onClick = {
                viewModel.updateSipCredentials(username, domain, sipPort, password)
                viewModel.updateEncryptionSettings(keyHex, srtpEnabled, selectedCipher)
                onSaveCallback()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = OnCyberGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_settings_btn")
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SECURE & SAVE CONFIGURATIONS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
