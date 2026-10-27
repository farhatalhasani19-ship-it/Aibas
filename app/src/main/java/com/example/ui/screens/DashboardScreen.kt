package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.DiaryRepository
import com.example.ui.components.RomanticCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    diary: Diary,
    repository: DiaryRepository,
    onNavigateToSection: (String) -> Unit
) {
    val entries by repository.observeEntries(diary.id).collectAsState(initial = emptyList())
    val memories by repository.observeMemories(diary.id).collectAsState(initial = emptyList())
    val letters by repository.observeLetters(diary.id).collectAsState(initial = emptyList())
    val timeline by repository.observeTimeline(diary.id).collectAsState(initial = emptyList())

    val currentUserName = repository.getCurrentUserDisplayName()

    // Days together calculation
    val daysTogether = remember(diary.anniversaryDate) {
        if (diary.anniversaryDate.isNotBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val annDate = sdf.parse(diary.anniversaryDate)
                if (annDate != null) {
                    val diff = System.currentTimeMillis() - annDate.time
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    if (days >= 0) days else 0
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Pick random memory
    val randomMemory = remember(memories) {
        if (memories.isNotEmpty()) memories.random() else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Greeting Banner
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Halo, Sayangg 💜",
                            style = MaterialTheme.typography.headlineMedium,
                            color = LavenderPrimary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("greeting_title")
                        )
                        Text(
                            text = "${diary.partnerAName} & ${diary.partnerBName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftRoseSecondary
                        )
                    }

                    // Online real-time sync badge
                    Surface(
                        color = Color(0x334CAF50),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Sync",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (daysTogether != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    RomanticCard(
                        backgroundColor = Color(0xFF281447),
                        borderColor = LavenderContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = SoftRoseSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "$daysTogether Hari Bersama 💜",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Anniversary: ${diary.anniversaryDate}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Catatan",
                    value = "${entries.size}",
                    icon = Icons.Default.Book,
                    tint = LavenderPrimary,
                    modifier = Modifier.weight(1f).clickable { onNavigateToSection("diary") }
                )
                StatCard(
                    title = "Memori",
                    value = "${memories.size}",
                    icon = Icons.Default.PhotoLibrary,
                    tint = SoftRoseSecondary,
                    modifier = Modifier.weight(1f).clickable { onNavigateToSection("memories") }
                )
                StatCard(
                    title = "Surat",
                    value = "${letters.size}",
                    icon = Icons.Default.Mail,
                    tint = StarGoldTertiary,
                    modifier = Modifier.weight(1f).clickable { onNavigateToSection("letters") }
                )
            }
        }

        // Surprise Kangen Banner (Instant Access)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSection("surprise") }
                    .testTag("kangen_quick_banner"),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF3B185F),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftRoseSecondary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lagi Kangen Banget? 💜",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pencet ini untuk membuka pesan cinta spesial",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = { onNavigateToSection("surprise") },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRoseSecondary, contentColor = SoftRoseOnSecondary),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Buka ✨", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Random Saved Memory Card
        if (randomMemory != null) {
            item {
                RomanticCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ Kenangan Manis Hari Ini",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarGoldTertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = randomMemory.album,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${randomMemory.caption}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Diabadikan oleh ${randomMemory.authorDisplayName} • ${randomMemory.dateOfMemory}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Latest Diary Entries Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📖 Catatan Diary Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    color = LavenderPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigateToSection("diary") }) {
                    Text("Lihat Semua", color = SoftRoseSecondary, fontSize = 13.sp)
                }
            }

            if (entries.isEmpty()) {
                RomanticCard {
                    Text(
                        text = "Belum ada catatan diary. Yuk tulis cerita manismu pertama kali hari ini! 💜",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    entries.take(3).forEach { entry ->
                        RomanticCard(
                            modifier = Modifier.clickable { onNavigateToSection("diary") }
                        ) {
                            Text(
                                text = entry.title,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.content,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Oleh ${entry.authorDisplayName}",
                                    color = LavenderPrimary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = entry.dateOfMemory,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Navigation Grid / Sections
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✨ Jelajahi Dunia Kita",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavigationTile(
                        title = "Our Diary",
                        subtitle = "Catatan harian berdua",
                        icon = Icons.Default.Book,
                        tint = LavenderPrimary,
                        modifier = Modifier.weight(1f).clickable { onNavigateToSection("diary") }
                    )
                    NavigationTile(
                        title = "Our Memories",
                        subtitle = "Galeri foto scrapbook",
                        icon = Icons.Default.PhotoLibrary,
                        tint = SoftRoseSecondary,
                        modifier = Modifier.weight(1f).clickable { onNavigateToSection("memories") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavigationTile(
                        title = "Love Letters",
                        subtitle = "Surat cinta & amplop",
                        icon = Icons.Default.Mail,
                        tint = StarGoldTertiary,
                        modifier = Modifier.weight(1f).clickable { onNavigateToSection("letters") }
                    )
                    NavigationTile(
                        title = "Our Timeline",
                        subtitle = "Linimasa tonggak cinta",
                        icon = Icons.Default.Timeline,
                        tint = LavenderPrimary,
                        modifier = Modifier.weight(1f).clickable { onNavigateToSection("timeline") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavigationTile(
                        title = "Little Things",
                        subtitle = "Hal-hal kecil tentang kita",
                        icon = Icons.Default.Star,
                        tint = SoftRoseSecondary,
                        modifier = Modifier.weight(1f).clickable { onNavigateToSection("littleThings") }
                    )
                    NavigationTile(
                        title = "Pengaturan",
                        subtitle = "Akses & Google Drive",
                        icon = Icons.Default.Settings,
                        tint = StarGoldTertiary,
                        modifier = Modifier.weight(1f).clickable { onNavigateToSection("settings") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MidnightCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun NavigationTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MidnightCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
