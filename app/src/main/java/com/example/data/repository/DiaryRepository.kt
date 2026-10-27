package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DiaryRepository(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun getCurrentUserDisplayName(): String {
        return auth.currentUser?.displayName?.ifBlank { "Sayang" } ?: "Sayang"
    }

    // --- USER PROFILE ---

    suspend fun getUserProfile(uid: String): UserProfile? {
        val path = "users/$uid"
        return try {
            val snap = db.collection("users").document(uid).get().await()
            if (snap.exists()) snap.toObject(UserProfile::class.java) else null
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            val data = mapOf(
                "uid" to uid,
                "email" to (auth.currentUser?.email ?: profile.email),
                "displayName" to profile.displayName.ifBlank { getCurrentUserDisplayName() },
                "diaryId" to (profile.diaryId ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // --- DIARY MANAGEMENT ---

    suspend fun createDiary(name: String, partnerAName: String, anniversaryDate: String): Diary {
        val uid = requireUserId()
        val inviteCode = "DS-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
        val diaryRef = db.collection("diaries").document()
        val diaryId = diaryRef.id

        val diaryData = hashMapOf(
            "name" to name.ifBlank { "Dairy Sayangg 💜" },
            "ownerUid" to uid,
            "partnerUid" to "",
            "inviteCode" to inviteCode,
            "memberUids" to listOf(uid),
            "partnerAName" to partnerAName.ifBlank { getCurrentUserDisplayName() },
            "partnerBName" to "Menunggu Pasangan...",
            "anniversaryDate" to anniversaryDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            diaryRef.set(diaryData).await()

            // Initialize surprise message
            val surpriseRef = diaryRef.collection("surprise").document("config")
            surpriseRef.set(
                hashMapOf(
                    "diaryId" to diaryId,
                    "message" to "Aku kangen banget sama kamu! Semoga hari kamu selalu indah dan bahagia ya sayang 💜✨",
                    "authorDisplayName" to partnerAName.ifBlank { getCurrentUserDisplayName() },
                    "attachedLetterId" to "",
                    "imageUrl" to "",
                    "memberUids" to listOf(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Link user to this diary
            saveUserProfile(UserProfile(uid = uid, displayName = partnerAName, diaryId = diaryId))

            return Diary(
                id = diaryId,
                name = name,
                ownerUid = uid,
                partnerUid = "",
                inviteCode = inviteCode,
                memberUids = listOf(uid),
                partnerAName = partnerAName,
                partnerBName = "Menunggu Pasangan...",
                anniversaryDate = anniversaryDate
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, diaryRef.path)
            throw e
        }
    }

    suspend fun joinDiaryByInviteCode(inviteCode: String, partnerBName: String): Diary {
        val uid = requireUserId()
        val cleanCode = inviteCode.trim().uppercase()
        val query = db.collection("diaries").whereEqualTo("inviteCode", cleanCode).limit(1)

        try {
            val snap = query.get().await()
            if (snap.isEmpty) {
                throw IllegalArgumentException("Kode undangan tidak ditemukan. Pastikan kodenya benar ya sayang!")
            }
            val doc = snap.documents.first()
            val diary = doc.toObject(Diary::class.java)
                ?: throw IllegalStateException("Format data diary tidak valid.")

            val existingMembers = diary.memberUids.toMutableList()
            if (existingMembers.contains(uid)) {
                // Already a member
                saveUserProfile(UserProfile(uid = uid, displayName = partnerBName, diaryId = doc.id))
                return diary.copy(id = doc.id)
            }

            if (existingMembers.size >= 2 && !existingMembers.contains(uid)) {
                throw IllegalStateException("Diary ini sudah memiliki 2 pasangan yang terdaftar.")
            }

            if (!existingMembers.contains(uid)) {
                existingMembers.add(uid)
            }

            doc.reference.update(
                mapOf(
                    "partnerUid" to uid,
                    "partnerBName" to partnerBName.ifBlank { getCurrentUserDisplayName() },
                    "memberUids" to existingMembers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update surprise member list as well
            val surpriseRef = doc.reference.collection("surprise").document("config")
            surpriseRef.update("memberUids", existingMembers).await()

            // Update user profile
            saveUserProfile(UserProfile(uid = uid, displayName = partnerBName, diaryId = doc.id))

            return diary.copy(
                id = doc.id,
                partnerUid = uid,
                partnerBName = partnerBName,
                memberUids = existingMembers
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "diaries")
            throw e
        }
    }

    fun observeDiary(diaryId: String): Flow<Diary?> = callbackFlow {
        if (diaryId.isBlank()) {
            trySend(null)
            return@callbackFlow
        }
        val docRef = db.collection("diaries").document(diaryId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val diary = snapshot.toObject(Diary::class.java)?.copy(id = snapshot.id)
                trySend(diary)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateDiarySettings(diaryId: String, partnerAName: String, partnerBName: String, anniversaryDate: String) {
        val path = "diaries/$diaryId"
        try {
            db.collection("diaries").document(diaryId).update(
                mapOf(
                    "partnerAName" to partnerAName,
                    "partnerBName" to partnerBName,
                    "anniversaryDate" to anniversaryDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // --- ENTRIES (OUR DIARY) ---

    fun observeEntries(diaryId: String): Flow<List<DiaryEntry>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val colRef = db.collection("diaries").document(diaryId).collection("entries")
        val registration = colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DiaryEntry::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addEntry(
        diaryId: String,
        memberUids: List<String>,
        title: String,
        content: String,
        dateOfMemory: String,
        tags: List<String>,
        isFavorite: Boolean
    ) {
        val uid = requireUserId()
        val path = "diaries/$diaryId/entries"
        val docRef = db.collection("diaries").document(diaryId).collection("entries").document()
        val data = hashMapOf(
            "diaryId" to diaryId,
            "title" to title,
            "content" to content,
            "authorUid" to uid,
            "authorDisplayName" to getCurrentUserDisplayName(),
            "dateOfMemory" to dateOfMemory,
            "tags" to tags,
            "isFavorite" to isFavorite,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun updateEntry(entry: DiaryEntry) {
        val path = "diaries/${entry.diaryId}/entries/${entry.id}"
        val docRef = db.collection("diaries").document(entry.diaryId).collection("entries").document(entry.id)
        val data = mapOf(
            "title" to entry.title,
            "content" to entry.content,
            "dateOfMemory" to entry.dateOfMemory,
            "tags" to entry.tags,
            "isFavorite" to entry.isFavorite,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun deleteEntry(diaryId: String, entryId: String) {
        val path = "diaries/$diaryId/entries/$entryId"
        try {
            db.collection("diaries").document(diaryId).collection("entries").document(entryId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    suspend fun toggleFavoriteEntry(diaryId: String, entryId: String, isFav: Boolean) {
        val path = "diaries/$diaryId/entries/$entryId"
        try {
            db.collection("diaries").document(diaryId).collection("entries").document(entryId)
                .update("isFavorite", !isFav).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // --- MEMORIES (OUR MEMORIES) ---

    fun observeMemories(diaryId: String): Flow<List<Memory>> = callbackFlow {
        val colRef = db.collection("diaries").document(diaryId).collection("memories")
        val registration = colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Memory::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addMemory(
        diaryId: String,
        memberUids: List<String>,
        caption: String,
        album: String,
        dateOfMemory: String,
        imageUrl: String
    ) {
        val uid = requireUserId()
        val path = "diaries/$diaryId/memories"
        val docRef = db.collection("diaries").document(diaryId).collection("memories").document()
        val data = hashMapOf(
            "diaryId" to diaryId,
            "caption" to caption,
            "album" to album,
            "dateOfMemory" to dateOfMemory,
            "imageUrl" to imageUrl,
            "authorUid" to uid,
            "authorDisplayName" to getCurrentUserDisplayName(),
            "isFavorite" to false,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun deleteMemory(diaryId: String, memoryId: String) {
        val path = "diaries/$diaryId/memories/$memoryId"
        try {
            db.collection("diaries").document(diaryId).collection("memories").document(memoryId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    suspend fun toggleFavoriteMemory(diaryId: String, memoryId: String, currentFav: Boolean) {
        val path = "diaries/$diaryId/memories/$memoryId"
        try {
            db.collection("diaries").document(diaryId).collection("memories").document(memoryId)
                .update("isFavorite", !currentFav).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // --- LOVE LETTERS ---

    fun observeLetters(diaryId: String): Flow<List<LoveLetter>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val colRef = db.collection("diaries").document(diaryId).collection("letters")
        val registration = colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val letter = doc.toObject(LoveLetter::class.java)?.copy(id = doc.id)
                        // If it's a draft, only show to the author
                        if (letter != null && (!letter.isDraft || letter.authorUid == uid)) {
                            letter
                        } else null
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addLetter(
        diaryId: String,
        memberUids: List<String>,
        title: String,
        content: String,
        template: String,
        isDraft: Boolean
    ) {
        val uid = requireUserId()
        val path = "diaries/$diaryId/letters"
        val docRef = db.collection("diaries").document(diaryId).collection("letters").document()
        val data = hashMapOf(
            "diaryId" to diaryId,
            "title" to title,
            "content" to content,
            "template" to template,
            "isDraft" to isDraft,
            "authorUid" to uid,
            "authorDisplayName" to getCurrentUserDisplayName(),
            "isFavorite" to false,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun updateLetter(letter: LoveLetter) {
        val path = "diaries/${letter.diaryId}/letters/${letter.id}"
        val docRef = db.collection("diaries").document(letter.diaryId).collection("letters").document(letter.id)
        val data = mapOf(
            "title" to letter.title,
            "content" to letter.content,
            "template" to letter.template,
            "isDraft" to letter.isDraft,
            "isFavorite" to letter.isFavorite,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun publishLetter(diaryId: String, letterId: String) {
        val path = "diaries/$diaryId/letters/$letterId"
        try {
            db.collection("diaries").document(diaryId).collection("letters").document(letterId)
                .update(
                    mapOf(
                        "isDraft" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun deleteLetter(diaryId: String, letterId: String) {
        val path = "diaries/$diaryId/letters/$letterId"
        try {
            db.collection("diaries").document(diaryId).collection("letters").document(letterId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // --- TIMELINE ---

    fun observeTimeline(diaryId: String): Flow<List<TimelineEvent>> = callbackFlow {
        val colRef = db.collection("diaries").document(diaryId).collection("timeline")
        val registration = colRef.orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TimelineEvent::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addTimelineEvent(
        diaryId: String,
        memberUids: List<String>,
        title: String,
        date: String,
        description: String,
        location: String
    ) {
        val uid = requireUserId()
        val path = "diaries/$diaryId/timeline"
        val docRef = db.collection("diaries").document(diaryId).collection("timeline").document()
        val data = hashMapOf(
            "diaryId" to diaryId,
            "title" to title,
            "date" to date,
            "description" to description,
            "location" to location,
            "authorUid" to uid,
            "authorDisplayName" to getCurrentUserDisplayName(),
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun deleteTimelineEvent(diaryId: String, eventId: String) {
        val path = "diaries/$diaryId/timeline/$eventId"
        try {
            db.collection("diaries").document(diaryId).collection("timeline").document(eventId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // --- LITTLE THINGS ABOUT US ---

    fun observeLittleThings(diaryId: String): Flow<List<LittleThing>> = callbackFlow {
        val colRef = db.collection("diaries").document(diaryId).collection("littleThings")
        val registration = colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(LittleThing::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addLittleThing(
        diaryId: String,
        memberUids: List<String>,
        category: String,
        title: String,
        notes: String
    ) {
        val uid = requireUserId()
        val path = "diaries/$diaryId/littleThings"
        val docRef = db.collection("diaries").document(diaryId).collection("littleThings").document()
        val data = hashMapOf(
            "diaryId" to diaryId,
            "category" to category,
            "title" to title,
            "notes" to notes,
            "authorUid" to uid,
            "authorDisplayName" to getCurrentUserDisplayName(),
            "isFavorite" to false,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun deleteLittleThing(diaryId: String, itemId: String) {
        val path = "diaries/$diaryId/littleThings/$itemId"
        try {
            db.collection("diaries").document(diaryId).collection("littleThings").document(itemId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    suspend fun toggleFavoriteLittleThing(diaryId: String, itemId: String, currentFav: Boolean) {
        val path = "diaries/$diaryId/littleThings/$itemId"
        try {
            db.collection("diaries").document(diaryId).collection("littleThings").document(itemId)
                .update("isFavorite", !currentFav).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // --- SURPRISE FOR YOU ---

    fun observeSurprise(diaryId: String): Flow<SurpriseConfig> = callbackFlow {
        val docRef = db.collection("diaries").document(diaryId).collection("surprise").document("config")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val cfg = snapshot.toObject(SurpriseConfig::class.java)
                if (cfg != null) trySend(cfg)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateSurprise(diaryId: String, memberUids: List<String>, message: String, attachedLetterId: String, imageUrl: String) {
        val path = "diaries/$diaryId/surprise/config"
        val docRef = db.collection("diaries").document(diaryId).collection("surprise").document("config")
        val data = mapOf(
            "diaryId" to diaryId,
            "message" to message,
            "authorDisplayName" to getCurrentUserDisplayName(),
            "attachedLetterId" to attachedLetterId,
            "imageUrl" to imageUrl,
            "memberUids" to memberUids,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // --- BACKUP & EXPORT / RESTORE ---

    suspend fun exportDiaryDataJson(diaryId: String): String {
        val diary = db.collection("diaries").document(diaryId).get().await().toObject(Diary::class.java)
        val entries = db.collection("diaries").document(diaryId).collection("entries").get().await().toObjects(DiaryEntry::class.java)
        val memories = db.collection("diaries").document(diaryId).collection("memories").get().await().toObjects(Memory::class.java)
        val letters = db.collection("diaries").document(diaryId).collection("letters").get().await().toObjects(LoveLetter::class.java)
        val timeline = db.collection("diaries").document(diaryId).collection("timeline").get().await().toObjects(TimelineEvent::class.java)
        val littleThings = db.collection("diaries").document(diaryId).collection("littleThings").get().await().toObjects(LittleThing::class.java)

        val root = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("diaryName", diary?.name ?: "Dairy Sayangg")
            put("partnerAName", diary?.partnerAName ?: "")
            put("partnerBName", diary?.partnerBName ?: "")
            put("anniversaryDate", diary?.anniversaryDate ?: "")

            val entriesArr = JSONArray()
            entries.forEach { e ->
                entriesArr.put(JSONObject().apply {
                    put("title", e.title)
                    put("content", e.content)
                    put("authorDisplayName", e.authorDisplayName)
                    put("dateOfMemory", e.dateOfMemory)
                    put("isFavorite", e.isFavorite)
                    put("tags", JSONArray(e.tags))
                })
            }
            put("entries", entriesArr)

            val lettersArr = JSONArray()
            letters.forEach { l ->
                lettersArr.put(JSONObject().apply {
                    put("title", l.title)
                    put("content", l.content)
                    put("template", l.template)
                    put("isDraft", l.isDraft)
                    put("authorDisplayName", l.authorDisplayName)
                })
            }
            put("letters", lettersArr)

            val timelineArr = JSONArray()
            timeline.forEach { t ->
                timelineArr.put(JSONObject().apply {
                    put("title", t.title)
                    put("date", t.date)
                    put("description", t.description)
                    put("location", t.location)
                })
            }
            put("timeline", timelineArr)

            val littleThingsArr = JSONArray()
            littleThings.forEach { lt ->
                littleThingsArr.put(JSONObject().apply {
                    put("category", lt.category)
                    put("title", lt.title)
                    put("notes", lt.notes)
                })
            }
            put("littleThings", littleThingsArr)
        }

        return root.toString(2)
    }

    suspend fun exportDiaryHtml(diaryId: String): String {
        val diary = db.collection("diaries").document(diaryId).get().await().toObject(Diary::class.java)
        val entries = db.collection("diaries").document(diaryId).collection("entries").get().await().toObjects(DiaryEntry::class.java)
        val letters = db.collection("diaries").document(diaryId).collection("letters").get().await().toObjects(LoveLetter::class.java)
        val timeline = db.collection("diaries").document(diaryId).collection("timeline").get().await().toObjects(TimelineEvent::class.java)

        return buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8"/>
                    <title>${diary?.name ?: "Dairy Sayangg 💜"}</title>
                    <style>
                        body { font-family: 'Georgia', serif; background: #0F081C; color: #FCF7FF; padding: 40px; }
                        h1, h2, h3 { color: #C78FFF; }
                        .card { background: #22123F; border: 1px solid #4B2375; border-radius: 12px; padding: 20px; margin-bottom: 20px; }
                        .meta { color: #D8C9EA; font-size: 13px; font-style: italic; }
                    </style>
                </head>
                <body>
                    <h1>Dairy Sayangg 💜</h1>
                    <p class="meta">${diary?.partnerAName} & ${diary?.partnerBName} — Anniversary: ${diary?.anniversaryDate}</p>
                    <hr style="border-color: #4B2375; margin: 24px 0;" />
                    <h2>📖 Catatan Harian Kita (${entries.size})</h2>
            """.trimIndent())
            entries.forEach { e ->
                append("""
                    <div class="card">
                        <h3>${e.title}</h3>
                        <p class="meta">Ditulis oleh ${e.authorDisplayName} • Tanggal: ${e.dateOfMemory}</p>
                        <p>${e.content.replace("\n", "<br/>")}</p>
                    </div>
                """.trimIndent())
            }
            append("<h2>💌 Surat Cinta (${letters.size})</h2>")
            letters.filter { !it.isDraft }.forEach { l ->
                append("""
                    <div class="card">
                        <h3>${l.title} (${l.template})</h3>
                        <p class="meta">Dari ${l.authorDisplayName}</p>
                        <p>${l.content.replace("\n", "<br/>")}</p>
                    </div>
                """.trimIndent())
            }
            append("<h2>✨ Linimasa Kenangan</h2>")
            timeline.forEach { t ->
                append("""
                    <div class="card">
                        <h3>${t.title}</h3>
                        <p class="meta">${t.date} ${if (t.location.isNotBlank()) "• " + t.location else ""}</p>
                        <p>${t.description}</p>
                    </div>
                """.trimIndent())
            }
            append("""
                </body>
                </html>
            """.trimIndent())
        }
    }
}
