package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Diary
import com.example.data.model.SurpriseConfig
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.components.StarryBackground
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurpriseScreen(
    diary: Diary,
    repository: DiaryRepository,
    onBack: () -> Unit,
    onOpenLetter: () -> Unit
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val surpriseConfig by repository.observeSurprise(diary.id).collectAsState(
        initial = SurpriseConfig(
            message = "Aku kangen banget sama kamu! Semoga hari kamu menyenangkan ya sayang, jangan lupa senyum hari ini 💜✨"
        )
    )

    var isRevealed by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedMessage by remember { mutableStateOf("") }

    // Pulsing heart animation when not revealed
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Surprise for You 💜",
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_surprise")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = LavenderPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editedMessage = surpriseConfig.message
                            showEditDialog = true
                        },
                        modifier = Modifier.testTag("edit_surprise_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah Pesan", tint = SoftRoseSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBase)
            )
        },
        containerColor = MidnightBase
    ) { padding ->
        StarryBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isRevealed) {
                    Text(
                        text = "Untuk Sayangku...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sentuh tombol di bawah kapan pun kamu merasa rindu, lelah, atau butuh pelukan hangat.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // The Big Interactive Button
                    Button(
                        onClick = { isRevealed = true },
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseScale)
                            .testTag("kangen_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B21A8),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = SoftRoseSecondary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pencet Ini\nKalau Lagi Kangen 💜",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    // Revealed State with gentle soft animation!
                    AnimatedVisibility(
                        visible = isRevealed,
                        enter = fadeIn(animationSpec = tween(800)) + scaleIn(animationSpec = tween(800))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4A154B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = SoftRoseSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Aku Selalu Ada Untukmu 💜",
                                style = MaterialTheme.typography.headlineSmall,
                                color = LavenderPrimary,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            RomanticCard(
                                backgroundColor = Color(0xFF28114A),
                                borderColor = SoftRoseSecondary.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = surpriseConfig.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 26.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )

                                if (surpriseConfig.authorDisplayName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Peluk hangat dari: ${surpriseConfig.authorDisplayName} 💜",
                                        color = SoftRoseSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenLetter,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StarGoldTertiary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StarGoldTertiary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buka Surat", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { isRevealed = false },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Tutup Pesan 💜", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Custom Surprise Message Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah Pesan Kangen 💜", color = LavenderPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tuliskan pesan manis yang akan muncul saat pasanganmu memencet tombol 'Lagi Kangen':",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = editedMessage,
                        onValueChange = { editedMessage = it },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("input_custom_surprise_msg"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedMessage.isNotBlank()) {
                            coroutineScope.launch {
                                repository.updateSurprise(
                                    diaryId = diary.id,
                                    memberUids = diary.memberUids,
                                    message = editedMessage,
                                    attachedLetterId = "",
                                    imageUrl = ""
                                )
                                showEditDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary)
                ) {
                    Text("Simpan Pesan 💜", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = MidnightCardElevated
        )
    }
}
