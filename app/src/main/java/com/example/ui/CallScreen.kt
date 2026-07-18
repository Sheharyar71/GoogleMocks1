package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sip.CallState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun CallScreen(
    state: CallState,
    remoteUser: String,
    remoteAddress: String,
    isMuted: Boolean,
    handshakeLogs: List<String>,
    onMuteToggle: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Animated waveform helper state
    val waveStep = remember { mutableStateOf(0f) }
    LaunchedEffect(state) {
        while (state == CallState.ACTIVE) {
            waveStep.value += 0.2f
            delay(30)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SecureBlack)
            .padding(24.dp)
            .testTag("call_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Pulsing tactical ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .background(
                            color = if (state == CallState.ACTIVE) CyberGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )

                // Central security shield
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Encrypted Connection Shield",
                    tint = if (state == CallState.ACTIVE) CyberGreen else WarningAmber,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = remoteUser,
                color = SecureTextWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "peer @ $remoteAddress",
                color = SecureTextMuted,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = if (state == CallState.ACTIVE) CyberGreen.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (state == CallState.ACTIVE) CyberGreen else WarningAmber,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state == CallState.ACTIVE) "SECURE SRTP SESSION ACTIVE" else "ESTABLISHING SECURE HANDSHAKE...",
                        color = if (state == CallState.ACTIVE) SecureTextSuccess else WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Live Audio Encrypted Waveform visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state == CallState.ACTIVE && !isMuted) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = 40
                    val stepX = width / pointsCount
                    val midY = height / 2f
                    
                    for (i in 0 until pointsCount) {
                        val angle = (i * 0.4f) + waveStep.value
                        val amplitude = sin(angle) * (30.dp.toPx() * (1f + sin(waveStep.value * 0.5f) * 0.3f))
                        
                        // Encrypted noise spike variation for aesthetic realism
                        val jitter = if (i % 3 == 0) sin(angle * 5) * 5.dp.toPx() else 0f
                        
                        drawLine(
                            color = CyberGreen.copy(alpha = 0.8f - (i * 0.015f)),
                            start = androidx.compose.ui.geometry.Offset(i * stepX, midY - amplitude - jitter),
                            end = androidx.compose.ui.geometry.Offset(i * stepX, midY + amplitude + jitter),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            } else if (isMuted) {
                Text(
                    text = "LOCAL MICROPHONE MUTED",
                    color = SecureTextError,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                CircularProgressIndicator(
                    color = WarningAmber,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Cryptographic HANDSHAKE Console Monitor Logs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "SECURE Handshake Logs:",
                color = SecureTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val listState = rememberLazyListState()
            LaunchedEffect(handshakeLogs.size) {
                if (handshakeLogs.isNotEmpty()) {
                    listState.animateScrollToItem(handshakeLogs.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SecureCharcoal)
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("handshake_console")
                ) {
                    if (handshakeLogs.isEmpty()) {
                        item {
                            Text(
                                text = "> Initializing cryptographic engine...\n> Awaiting SIP SDP parameter negotiation...",
                                color = SecureTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        items(handshakeLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("Success", ignoreCase = true) || log.contains("AES-256", ignoreCase = true)) SecureTextSuccess else if (log.contains("ERR", ignoreCase = true)) SecureTextError else SecureTextWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row (Mute, Hang Up)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Switcher Button
            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = if (isMuted) SecureTextError.copy(alpha = 0.2f) else SecureSlate,
                        shape = CircleShape
                    )
                    .testTag("mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute Voice Stream Toggle",
                    tint = if (isMuted) SecureTextError else SecureTextWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Central End session / Hang up Button
            Button(
                onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecureTextError,
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("end_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Terminate Secure Connection",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
