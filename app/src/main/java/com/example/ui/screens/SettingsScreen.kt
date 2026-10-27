package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.Diary
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    diary: Diary,
    repository: DiaryRepository,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isBackingUp by remember { mutableStateOf(false) }
    var backupStatusText by remember { mutableStateOf<String?>(null) }
    var exportedJsonData by remember { mutableStateOf<String?>(null) }

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var isDriveConnected by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan & Cadangan ⚙️",
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_settings")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = LavenderPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBase)
            )
        },
        containerColor = MidnightBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Partner & Access Card
            RomanticCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = LavenderPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Akses Pasangan Diary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Buku harian ini dibagikan secara privat antara 2 orang:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "👑 Pemilik (Partner A):", color = TextMuted, fontSize = 13.sp)
                    Text(text = diary.partnerAName, fontWeight = FontWeight.Bold, color = LavenderPrimary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "💜 Pasangan (Partner B):", color = TextMuted, fontSize = 13.sp)
                    Text(text = diary.partnerBName, fontWeight = FontWeight.Bold, color = SoftRoseSecondary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MidnightCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Kode Undangan:", color = TextMuted, fontSize = 12.sp)
                        Text(text = diary.inviteCode, fontWeight = FontWeight.ExtraBold, color = StarGoldTertiary, fontSize = 18.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", diary.inviteCode))
                            Toast.makeText(context, "Kode undangan disalin!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StarGoldTertiary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StarGoldTertiary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Kode", fontSize = 12.sp)
                    }
                }
            }

            // Google Drive & Backup Section
            RomanticCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = StarGoldTertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Drive & Cadangan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Folder Khusus: Dairy Sayangg 💜\nDatabase Cloud Firestore adalah sumber data utama yang tersinkronisasi secara real-time. Kamu dapat mengekspor salinan cadangan arsip ke Google Drive kapan saja.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0x224CAF50),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Status: Terhubung ke Cloud Diary Storage",
                            color = Color(0xFF81C784),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Export JSON Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isBackingUp = true
                            try {
                                val json = repository.exportDiaryDataJson(diary.id)
                                exportedJsonData = json
                                backupStatusText = "Cadangan JSON berhasil dibuat! Siap diekspor ke Google Drive."

                                // Share intent so user can save directly to Google Drive or local files
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TITLE, "Dairy_Sayangg_Backup_${diary.id}.json")
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Simpan Cadangan ke Google Drive")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                backupStatusText = "Gagal membuat cadangan: ${e.localizedMessage}"
                            } finally {
                                isBackingUp = false
                            }
                        }
                    },
                    enabled = !isBackingUp,
                    modifier = Modifier.fillMaxWidth().testTag("export_json_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LavenderOnPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadangkan ke Google Drive / JSON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Export Readable HTML Book
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val html = repository.exportDiaryHtml(diary.id)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, html)
                                    type = "text/html"
                                    putExtra(Intent.EXTRA_TITLE, "Dairy_Sayangg_Keepsake.html")
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Ekspor Buku Kenangan HTML")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal mengekspor HTML", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_html_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StarGoldTertiary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StarGoldTertiary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Buku Kenangan (HTML)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (backupStatusText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = backupStatusText ?: "",
                        color = StarGoldTertiary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // About Brand Card
            RomanticCard {
                Text(
                    text = "Dairy Sayangg 💜",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LavenderPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"Every little moment with you deserves to be remembered.\"",
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = SoftRoseSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Versi 1.0 • Dilindungi Keamanan Zero-Trust Cloud Firestore & Google Authentication.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // Sign Out Button
            Button(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381427), contentColor = Color(0xFFFF897D)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FF897D)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun Google", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Sign out confirmation
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Keluar dari Diary?", color = SoftRoseSecondary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah kamu yakin ingin keluar dari akun Google? Kamu dapat masuk kembali kapan saja untuk melihat cerita berdua.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            Firebase.auth.signOut()
                            try {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                // Ignore
                            }
                            showSignOutConfirm = false
                            onSignOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Text("Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = MidnightCardElevated
        )
    }
}
