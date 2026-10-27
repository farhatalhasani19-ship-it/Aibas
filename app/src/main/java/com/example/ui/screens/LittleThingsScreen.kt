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
import com.example.data.model.LittleThing
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LittleThingsScreen(
    diary: Diary,
    repository: DiaryRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val littleThings by repository.observeLittleThings(diary.id).collectAsState(initial = emptyList())

    val categories = listOf(
        "Semua",
        "Makanan Favorit",
        "Lagu Kita",
        "Tempat Impian",
        "Kebiasaan Lucu",
        "Panggilan Sayang",
        "Mimpi Bersama",
        "Hal Bikin Senyum"
    )

    var selectedCategory by remember { mutableStateOf("Semua") }
    var showAddDialog by remember { mutableStateOf(false) }

    var formCategory by remember { mutableStateOf("Makanan Favorit") }
    var formTitle by remember { mutableStateOf("") }
    var formNotes by remember { mutableStateOf("") }

    val filteredItems = remember(littleThings, selectedCategory) {
        if (selectedCategory == "Semua") littleThings
        else littleThings.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Little Things About Us ✨",
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_little_things")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = LavenderPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBase)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    formCategory = if (selectedCategory != "Semua") selectedCategory else "Makanan Favorit"
                    formTitle = ""
                    formNotes = ""
                    showAddDialog = true
                },
                containerColor = SoftRoseSecondary,
                contentColor = SoftRoseOnSecondary,
                modifier = Modifier.testTag("fab_add_little_thing")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Hal Kecil")
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftRoseContainer,
                            selectedLabelColor = SoftRoseOnContainer,
                            containerColor = MidnightCard
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada catatan hal kecil di kategori ini.\nCatat kebiasaan lucu atau hal favoritnya! 💜",
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        RomanticCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SoftRoseContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        color = SoftRoseOnContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.toggleFavoriteLittleThing(diary.id, item.id, item.isFavorite)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorit",
                                            tint = if (item.isFavorite) SoftRoseSecondary else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteLittleThing(diary.id, item.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus",
                                            tint = TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            if (item.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Dicatat oleh ${item.authorDisplayName}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Little Thing Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Catat Hal Kecil ✨", color = SoftRoseSecondary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pilih Kategori:", fontSize = 12.sp, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories.filter { it != "Semua" }) { cat ->
                            FilterChip(
                                selected = formCategory == cat,
                                onClick = { formCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftRoseContainer,
                                    selectedLabelColor = SoftRoseOnContainer
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Hal Kecil (Contoh: Suka es krim cokelat)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_little_thing_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRoseSecondary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formNotes,
                        onValueChange = { formNotes = it },
                        label = { Text("Catatan Tambahan (Opsional)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRoseSecondary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formTitle.isNotBlank()) {
                            coroutineScope.launch {
                                repository.addLittleThing(
                                    diaryId = diary.id,
                                    memberUids = diary.memberUids,
                                    category = formCategory,
                                    title = formTitle,
                                    notes = formNotes
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRoseSecondary, contentColor = SoftRoseOnSecondary)
                ) {
                    Text("Simpan ✨", fontWeight = FontWeight.Bold)
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
