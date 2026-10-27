package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val diaryId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Diary(
    @DocumentId val id: String = "",
    val name: String = "Dairy Sayangg 💜",
    val ownerUid: String = "",
    val partnerUid: String? = null,
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val partnerAName: String = "Sayang A",
    val partnerBName: String = "Sayang B",
    val anniversaryDate: String = "", // e.g. "2024-02-14"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DiaryEntry(
    @DocumentId val id: String = "",
    val diaryId: String = "",
    val title: String = "",
    val content: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val dateOfMemory: String = "", // e.g. "2024-10-22"
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Memory(
    @DocumentId val id: String = "",
    val diaryId: String = "",
    val caption: String = "",
    val album: String = "Our Little Dates",
    val dateOfMemory: String = "",
    val imageUrl: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val isFavorite: Boolean = false,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LoveLetter(
    @DocumentId val id: String = "",
    val diaryId: String = "",
    val title: String = "",
    val content: String = "",
    val template: String = "Open When You Miss Me",
    val isDraft: Boolean = false,
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val isFavorite: Boolean = false,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TimelineEvent(
    @DocumentId val id: String = "",
    val diaryId: String = "",
    val title: String = "",
    val date: String = "", // e.g. "2024-01-01"
    val description: String = "",
    val location: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LittleThing(
    @DocumentId val id: String = "",
    val diaryId: String = "",
    val category: String = "Favorite Foods",
    val title: String = "",
    val notes: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val isFavorite: Boolean = false,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SurpriseConfig(
    @DocumentId val id: String = "config",
    val diaryId: String = "",
    val message: String = "Aku kangen banget sama kamu! Semoga hari kamu menyenangkan ya sayang, jangan lupa senyum hari ini 💜✨",
    val authorDisplayName: String = "",
    val attachedLetterId: String = "",
    val imageUrl: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)
