package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import com.example.data.model.DiaryEntry
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    diary: Diary,
    repository: DiaryRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val entries by repository.observeEntries(diary.id).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var sortNewestFirst by remember { mutableStateOf(true) }

    // Dialog state for adding/editing entry
    var showEditDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<DiaryEntry?>(null) }

    var formTitle by remember { mutableStateOf("") }
    var formContent by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var formTag by remember { mutableStateOf("Kencan") }
    var showDeleteConfirmDialog by remember { mutableStateOf<DiaryEntry?>(null) }

    val availableTags = listOf("Semua", "Kencan", "Rindu", "Deep Talk", "Liburan", "Lucu", "Janji")

    val filteredEntries = remember(entries, searchQuery, selectedTag, sortNewestFirst) {
        entries.filter { entry ->
            val matchQuery = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.content.contains(searchQuery, ignoreCase = true)
            val matchTag = selectedTag == null || selectedTag == "Semua" || entry.tags.contains(selectedTag)
            matchQuery && matchTag
        }.let { list ->
            if (sortNewestFirst) list else list.reversed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Our Diary 📖",
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_diary")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = LavenderPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { sortNewestFirst = !sortNewestFirst },
                        modifier = Modifier.testTag("sort_entries_btn")
                    ) {
                        Icon(
                            imageVector = if (sortNewestFirst) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "Urutkan",
                            tint = SoftRoseSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBase)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    entryToEdit = null
                    formTitle = ""
                    formContent = ""
                    formDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    formTag = "Kencan"
                    showEditDialog = true
                },
                containerColor = LavenderPrimary,
                contentColor = LavenderOnPrimary,
                modifier = Modifier.testTag("fab_add_entry")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tulis Diary")
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari catatan kenangan kita...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LavenderPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("search_diary_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LavenderPrimary,
                    unfocusedBorderColor = MidnightCardBorder,
                    focusedContainerColor = MidnightCard,
                    unfocusedContainerColor = MidnightCard
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tags Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableTags) { tag ->
                    val isSelected = (selectedTag == tag) || (selectedTag == null && tag == "Semua")
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTag = if (tag == "Semua") null else tag },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LavenderContainer,
                            selectedLabelColor = LavenderOnContainer,
                            containerColor = MidnightCard
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTag != null)
                                "Tidak ada catatan yang cocok dengan pencarian."
                            else
                                "Belum ada diary yang ditulis.\nSentuh tombol + untuk mulai menulis cerita hari ini! 💜",
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        RomanticCard(
                            modifier = Modifier.clickable {
                                entryToEdit = entry
                                formTitle = entry.title
                                formContent = entry.content
                                formDate = entry.dateOfMemory
                                formTag = entry.tags.firstOrNull() ?: "Kencan"
                                showEditDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = LavenderContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = entry.tags.firstOrNull() ?: "Diary",
                                            color = LavenderOnContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.dateOfMemory,
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.toggleFavoriteEntry(diary.id, entry.id, entry.isFavorite)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorit",
                                            tint = if (entry.isFavorite) SoftRoseSecondary else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = entry }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus",
                                            tint = TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = entry.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "✍️ Ditulis oleh ${entry.authorDisplayName}",
                                color = LavenderPrimary,
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Entry Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (entryToEdit == null) "Tulis Cerita Baru 💜" else "Ubah Cerita 💜",
                    color = LavenderPrimary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Judul Cerita") },
                        modifier = Modifier.fillMaxWidth().testTag("input_entry_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formDate,
                        onValueChange = { formDate = it },
                        label = { Text("Tanggal (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_entry_date"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formTag,
                        onValueChange = { formTag = it },
                        label = { Text("Kategori / Tag (Kencan, Rindu, dll)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_entry_tag"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formContent,
                        onValueChange = { formContent = it },
                        label = { Text("Tuliskan cerita kita di sini...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("input_entry_content"),
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
                        if (formTitle.isNotBlank() && formContent.isNotBlank()) {
                            coroutineScope.launch {
                                if (entryToEdit == null) {
                                    repository.addEntry(
                                        diaryId = diary.id,
                                        memberUids = diary.memberUids,
                                        title = formTitle,
                                        content = formContent,
                                        dateOfMemory = formDate,
                                        tags = listOf(formTag.ifBlank { "Diary" }),
                                        isFavorite = false
                                    )
                                } else {
                                    repository.updateEntry(
                                        entryToEdit!!.copy(
                                            title = formTitle,
                                            content = formContent,
                                            dateOfMemory = formDate,
                                            tags = listOf(formTag.ifBlank { "Diary" })
                                        )
                                    )
                                }
                                showEditDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary),
                    modifier = Modifier.testTag("save_entry_btn")
                ) {
                    Text("Simpan 💜", fontWeight = FontWeight.Bold)
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

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        val entry = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Hapus Catatan?", color = SoftRoseSecondary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah kamu yakin ingin menghapus catatan \"${entry.title}\"? Cerita ini tidak dapat dikembalikan.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteEntry(diary.id, entry.id)
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = MidnightCardElevated
        )
    }
}
