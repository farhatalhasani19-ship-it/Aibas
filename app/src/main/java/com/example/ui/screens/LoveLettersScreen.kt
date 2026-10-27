package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Diary
import com.example.data.model.LoveLetter
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.components.RomanticEnvelope
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveLettersScreen(
    diary: Diary,
    repository: DiaryRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val letters by repository.observeLetters(diary.id).collectAsState(initial = emptyList())
    val currentUid = repository.requireUserId()

    val templates = listOf(
        "Open When You Miss Me",
        "Open When You're Tired",
        "Open When You're Sad",
        "Open When You Need a Hug",
        "A Letter for Our Future"
    )

    var selectedTab by remember { mutableStateOf("Semua Surat") } // "Semua Surat", "Draft Pribadi"
    var showAddDialog by remember { mutableStateOf(false) }

    var formTitle by remember { mutableStateOf("") }
    var formContent by remember { mutableStateOf("") }
    var formTemplate by remember { mutableStateOf("Open When You Miss Me") }
    var formIsDraft by remember { mutableStateOf(false) }

    val filteredLetters = remember(letters, selectedTab) {
        if (selectedTab == "Draft Pribadi") {
            letters.filter { it.isDraft && it.authorUid == currentUid }
        } else {
            letters.filter { !it.isDraft || it.authorUid == currentUid }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Love Letters 💌",
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_letters")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = LavenderPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBase)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    formTitle = ""
                    formContent = ""
                    formTemplate = "Open When You Miss Me"
                    formIsDraft = false
                    showAddDialog = true
                },
                containerColor = StarGoldTertiary,
                contentColor = StarGoldOnTertiary,
                modifier = Modifier.testTag("fab_add_letter")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Tulis Surat Cinta")
            }
        },
        containerColor = MidnightBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Tabs Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "Semua Surat",
                    onClick = { selectedTab = "Semua Surat" },
                    label = { Text("Surat Terbuka 💌") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderContainer,
                        selectedLabelColor = LavenderOnContainer
                    )
                )

                FilterChip(
                    selected = selectedTab == "Draft Pribadi",
                    onClick = { selectedTab = "Draft Pribadi" },
                    label = { Text("Draft Rahasiaku 🔒") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderContainer,
                        selectedLabelColor = LavenderOnContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredLetters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == "Draft Pribadi")
                                "Tidak ada draft surat rahasia."
                            else
                                "Belum ada surat cinta.\nTuliskan surat pertamamu untuk si dia! 💜",
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLetters, key = { it.id }) { letter ->
                        Column {
                            // Animated Interactive Romantic Envelope
                            RomanticEnvelope(
                                title = letter.title,
                                content = letter.content,
                                author = letter.authorDisplayName,
                                template = letter.template
                            )

                            // Quick actions below envelope for draft or management
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (letter.isDraft) {
                                    Surface(
                                        color = Color(0x33FFB300),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "🔒 Draft Rahasia (Hanya Kamu)",
                                            color = StarGoldTertiary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.publishLetter(diary.id, letter.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LavenderPrimary,
                                            contentColor = LavenderOnPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Publikasikan 💌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                if (letter.authorUid == currentUid) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteLetter(diary.id, letter.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Surat",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Letter Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tulis Surat Cinta 💌", color = StarGoldTertiary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Judul Surat") },
                        modifier = Modifier.fillMaxWidth().testTag("input_letter_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StarGoldTertiary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    Text("Pilih Template Surat:", fontSize = 12.sp, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(templates) { tmpl ->
                            FilterChip(
                                selected = formTemplate == tmpl,
                                onClick = { formTemplate = tmpl },
                                label = { Text(tmpl, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StarGoldContainer,
                                    selectedLabelColor = StarGoldOnContainer
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = formContent,
                        onValueChange = { formContent = it },
                        label = { Text("Isi surat cinta terdalammu...") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth().testTag("input_letter_content"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StarGoldTertiary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = formIsDraft,
                            onCheckedChange = { formIsDraft = it },
                            colors = CheckboxDefaults.colors(checkedColor = StarGoldTertiary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan sebagai draft rahasia (hanya bisa dibaca olehmu)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formTitle.isNotBlank() && formContent.isNotBlank()) {
                            coroutineScope.launch {
                                repository.addLetter(
                                    diaryId = diary.id,
                                    memberUids = diary.memberUids,
                                    title = formTitle,
                                    content = formContent,
                                    template = formTemplate,
                                    isDraft = formIsDraft
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StarGoldTertiary, contentColor = StarGoldOnTertiary)
                ) {
                    Text("Kirim / Simpan 💜", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = MidnightCardElevated
        )
    }
}
