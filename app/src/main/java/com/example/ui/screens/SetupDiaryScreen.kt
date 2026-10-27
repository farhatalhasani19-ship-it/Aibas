package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Share
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
import com.example.data.model.Diary
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.components.StarryBackground
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SetupDiaryScreen(
    repository: DiaryRepository,
    onDiaryReady: (Diary) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCreatingNew by remember { mutableStateOf(true) }
    var diaryName by remember { mutableStateOf("Dairy Sayangg 💜") }
    var partnerName by remember { mutableStateOf(repository.getCurrentUserDisplayName()) }
    var anniversaryDate by remember { mutableStateOf("2024-02-14") }
    var inviteCodeInput by remember { mutableStateOf("") }

    var createdDiary by remember { mutableStateOf<Diary?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    StarryBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Pengaturan Awal Pasangan 💜",
                style = MaterialTheme.typography.headlineMedium,
                color = LavenderPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pilih apakah kamu ingin memulai diary baru atau bergabung dengan diary yang sudah dibuat pasanganmu.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (createdDiary == null) {
                // Toggle mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = isCreatingNew,
                        onClick = { isCreatingNew = true; errorMessage = null },
                        label = { Text("Mulai Diary Baru ✨") },
                        modifier = Modifier.weight(1f).testTag("tab_create_diary"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LavenderContainer,
                            selectedLabelColor = LavenderOnContainer
                        )
                    )
                    FilterChip(
                        selected = !isCreatingNew,
                        onClick = { isCreatingNew = false; errorMessage = null },
                        label = { Text("Gabung Diary 🔗") },
                        modifier = Modifier.weight(1f).testTag("tab_join_diary"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LavenderContainer,
                            selectedLabelColor = LavenderOnContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                RomanticCard {
                    if (isCreatingNew) {
                        Text(
                            text = "Buat Diary Baru",
                            style = MaterialTheme.typography.titleMedium,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = diaryName,
                            onValueChange = { diaryName = it },
                            label = { Text("Nama Diary") },
                            modifier = Modifier.fillMaxWidth().testTag("input_diary_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderPrimary,
                                unfocusedBorderColor = MidnightCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text("Nama Panggilan Kamu") },
                            modifier = Modifier.fillMaxWidth().testTag("input_partner_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderPrimary,
                                unfocusedBorderColor = MidnightCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = anniversaryDate,
                            onValueChange = { anniversaryDate = it },
                            label = { Text("Tanggal Jadian (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_anniversary_date"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderPrimary,
                                unfocusedBorderColor = MidnightCardBorder
                            )
                        )
                    } else {
                        Text(
                            text = "Gabung Diary Pasangan",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftRoseSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.uppercase() },
                            label = { Text("Kode Undangan (Contoh: DS-AB12CD)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_invite_code"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftRoseSecondary,
                                unfocusedBorderColor = MidnightCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text("Nama Panggilan Kamu") },
                            modifier = Modifier.fillMaxWidth().testTag("input_partner_b_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftRoseSecondary,
                                unfocusedBorderColor = MidnightCardBorder
                            )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF897D),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                if (isCreatingNew) {
                                    val diary = repository.createDiary(
                                        name = diaryName,
                                        partnerAName = partnerName,
                                        anniversaryDate = anniversaryDate
                                    )
                                    createdDiary = diary
                                } else {
                                    val diary = repository.joinDiaryByInviteCode(
                                        inviteCode = inviteCodeInput,
                                        partnerBName = partnerName
                                    )
                                    onDiaryReady(diary)
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Terjadi kesalahan"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && (if (isCreatingNew) diaryName.isNotBlank() else inviteCodeInput.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_diary_setup_btn"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LavenderOnPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isCreatingNew) "Buat Diary Kita 💜" else "Gabung Sekarang 💜",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Created state: Show invite code to share with Partner B!
                RomanticCard {
                    Text(
                        text = "Diary Berhasil Dibuat! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        color = StarGoldTertiary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Bagikan kode undangan rahasia ini ke pasanganmu agar bisa bergabung ke dalam diary berdua:",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFF371E63),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = createdDiary?.inviteCode ?: "",
                                color = LavenderPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", createdDiary?.inviteCode))
                                    Toast.makeText(context, "Kode disalin!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_invite_code_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Kode",
                                    tint = StarGoldTertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        createdDiary?.let { onDiaryReady(it) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("enter_dashboard_btn"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary)
                ) {
                    Text("Masuk ke Beranda Diary 💜", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
