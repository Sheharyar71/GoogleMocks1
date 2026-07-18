package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun IncomingCallScreen(
    remoteUser: String,
    remoteAddress: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_inner"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_outer"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SecureBlack)
            .padding(24.dp)
            .testTag("incoming_call_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Dynamic Pulsing Call Ring Area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Text(
                text = "INCOMING SECURE CALL",
                color = CyberGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer Pulse Ring 2
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale2)
                        .background(
                            color = CyberGreen.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                )

                // Inner Pulse Ring 1
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale1)
                        .background(
                            color = CyberGreen.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )

                // Central Active phone icon container
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(SecureSlate, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Ringing Icon",
                        tint = CyberGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = remoteUser,
                color = SecureTextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Secure SIP Address: sip:$remoteUser@$remoteAddress",
                color = SecureTextMuted,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        // Tactical encryption announcement card
        Surface(
            color = SecureCharcoal,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "E2E Secured Logo",
                    tint = CyberGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "End-to-End Encrypted Link",
                        color = SecureTextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Session parameters and keys will be negotiated using MODP Diffie-Hellman on answer.",
                        color = SecureTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Large Tappable Accept / Decline Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DECLINE button
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecureTextError, // #BA1A1A
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(80.dp)
                    .testTag("decline_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline and Reject Call",
                    modifier = Modifier.size(32.dp)
                )
            }

            // ACCEPT SECURELY button
            Button(
                onClick = onAnswer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006622), // From Sophisticated Dark Design Specification
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(80.dp)
                    .testTag("answer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Accept and Handshake Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
