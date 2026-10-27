package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.OpeningBookAnimation
import com.example.ui.components.StarryBackground
import com.example.ui.theme.*

@Composable
fun OpeningScreen(
    onOpenDiaryClicked: () -> Unit
) {
    var isBookOpen by remember { mutableStateOf(false) }

    StarryBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Book Animation
            OpeningBookAnimation(
                isOpen = isBookOpen,
                modifier = Modifier
                    .padding(bottom = 28.dp)
                    .testTag("opening_book_graphic")
            )

            Text(
                text = "Welcome to Our Little Universe 💜",
                style = MaterialTheme.typography.headlineMedium,
                color = LavenderPrimary,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("welcome_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tempat kecil untuk menyimpan cerita kita, tawa kita, rindu kita, dan semua hal sederhana yang ternyata berarti begitu besar.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("welcome_quote")
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    isBookOpen = true
                    onOpenDiaryClicked()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp)
                    .testTag("open_diary_button"),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LavenderPrimary,
                    contentColor = LavenderOnPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = SoftRoseSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Buka Diary Kita 💜",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Dairy Sayangg • Beautiful memories deserve a beautiful home.",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
