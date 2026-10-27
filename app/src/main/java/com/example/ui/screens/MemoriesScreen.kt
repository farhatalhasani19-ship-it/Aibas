package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Diary
import com.example.data.model.Memory
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    diary: Diary,
    repository: DiaryRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val memories by repository.observeMemories(diary.id).collectAsState(initial = emptyList())

    val albums = listOf(
        "Semua Album",
        "Our First Moments",
        "Our Little Dates",
        "Random Happiness",
        "My Favorite Person",
        "Moments I Never Want to Forget"
    )

    var selectedAlbum by remember { mutableStateOf("Semua Album") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFullscreenMemory by remember { mutableStateOf<Memory?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Memory?>(null) }

    var formCaption by remember { mutableStateOf("") }
    var formAlbum by remember { mutableStateOf("Our Little Dates") }
    var formDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var formImageUrl by remember { mutableStateOf("") }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            formImageUrl = uri.toString()
        }
    }

    val filteredMemories = remember(memories, selectedAlbum) {
        if (selectedAlbum == "Semua Album") memories
        else memories.filter { it.album == selectedAlbum }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Our Memories 📸",
                        color = LavenderPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_memories")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = LavenderPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBase)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    formCaption = ""
                    formAlbum = if (selectedAlbum != "Semua Album") selectedAlbum else "Our Little Dates"
                    formDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    formImageUrl = ""
                    showAddDialog = true
                },
                containerColor = SoftRoseSecondary,
                contentColor = SoftRoseOnSecondary,
                modifier = Modifier.testTag("fab_add_memory")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Tambah Foto")
            }
        },
        containerColor = MidnightBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Album Tabs Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(albums) { album ->
                    val isSelected = selectedAlbum == album
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAlbum = album },
                        label = { Text(album) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftRoseContainer,
                            selectedLabelColor = SoftRoseOnContainer,
                            containerColor = MidnightCard
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada foto di album ini.\nSentuh tombol + untuk menambah momen spesial kita! 💜",
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFullscreenMemory = memory }
                                .testTag("memory_card_${memory.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = MidnightCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardBorder)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                ) {
                                    if (memory.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = memory.imageUrl,
                                            contentDescription = memory.caption,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF2E154E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = SoftRoseSecondary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    // Favorite badge
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.toggleFavoriteMemory(diary.id, memory.id, memory.isFavorite)
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = if (memory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorit",
                                            tint = if (memory.isFavorite) SoftRoseSecondary else Color.White
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = memory.caption,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = memory.dateOfMemory,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Oleh ${memory.authorDisplayName}",
                                        color = LavenderPrimary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Abadikan Kenangan 📸", color = SoftRoseSecondary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = formCaption,
                        onValueChange = { formCaption = it },
                        label = { Text("Keterangan Foto") },
                        modifier = Modifier.fillMaxWidth().testTag("input_memory_caption"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRoseSecondary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formDate,
                        onValueChange = { formDate = it },
                        label = { Text("Tanggal Momen (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRoseSecondary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formAlbum,
                        onValueChange = { formAlbum = it },
                        label = { Text("Nama Album") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRoseSecondary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = formImageUrl,
                        onValueChange = { formImageUrl = it },
                        label = { Text("URL Foto / Gambar") },
                        modifier = Modifier.fillMaxWidth().testTag("input_memory_image_url"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRoseSecondary,
                            unfocusedBorderColor = MidnightCardBorder
                        )
                    )

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderContainer, contentColor = LavenderOnContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih dari Galeri HP", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formCaption.isNotBlank()) {
                            coroutineScope.launch {
                                repository.addMemory(
                                    diaryId = diary.id,
                                    memberUids = diary.memberUids,
                                    caption = formCaption,
                                    album = formAlbum.ifBlank { "Our Little Dates" },
                                    dateOfMemory = formDate,
                                    imageUrl = formImageUrl
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRoseSecondary, contentColor = SoftRoseOnSecondary)
                ) {
                    Text("Simpan 💜", fontWeight = FontWeight.Bold)
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

    // Fullscreen View Dialog
    if (selectedFullscreenMemory != null) {
        val memory = selectedFullscreenMemory!!
        Dialog(onDismissRequest = { selectedFullscreenMemory = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MidnightCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (memory.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = memory.imageUrl,
                            contentDescription = memory.caption,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = memory.caption,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Album: ${memory.album} • ${memory.dateOfMemory}",
                        color = SoftRoseSecondary,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Diunggah oleh ${memory.authorDisplayName}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                showDeleteConfirmDialog = memory
                                selectedFullscreenMemory = null
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFFF5252))
                        }

                        Button(
                            onClick = { selectedFullscreenMemory = null },
                            colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = LavenderOnPrimary)
                        ) {
                            Text("Tutup")
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteConfirmDialog != null) {
        val memory = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Hapus Memori?", color = SoftRoseSecondary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah kamu yakin ingin menghapus foto kenangan \"${memory.caption}\"?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteMemory(diary.id, memory.id)
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
