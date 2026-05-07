package com.temurx.checklymanager.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.temurx.checklymanager.data.TaskStatus
import java.util.Date

object DemoSeeder {

    private const val TAG = "DemoSeeder"
    private const val SAMPLE_PHOTO =
        "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600&q=80"

    private data class Template(
        val title: String,
        val description: String,
        val requiresPhoto: Boolean,
        val startOffsetMin: Long,
        val dueOffsetMin: Long,
        val status: String,
        val withPhoto: Boolean = false,
        val withStarted: Boolean = false,
        val withFinished: Boolean = false,
    )

    private val templates = listOf(
        Template(
            title = "Set up brunch service",
            description = "Lay napkins, fill water carafes, light candles before the first wave.",
            requiresPhoto = false,
            startOffsetMin = 120,
            dueOffsetMin = 240,
            status = TaskStatus.NOT_YET_AVAILABLE,
        ),
        Template(
            title = "Wipe down all tables",
            description = "Use the sanitizer spray on every dining surface and dry with a clean cloth.",
            requiresPhoto = false,
            startOffsetMin = -5,
            dueOffsetMin = 60,
            status = TaskStatus.AVAILABLE,
        ),
        Template(
            title = "Restock bar mixers",
            description = "Tonic, soda, ginger beer — refill from cellar. Snap a photo of the stocked shelf.",
            requiresPhoto = true,
            startOffsetMin = -30,
            dueOffsetMin = 30,
            status = TaskStatus.IN_PROGRESS,
            withStarted = true,
        ),
        Template(
            title = "Polish silverware",
            description = "Run all forks, knives, and spoons through the polishing cloth before service.",
            requiresPhoto = false,
            startOffsetMin = -240,
            dueOffsetMin = -120,
            status = TaskStatus.FINISHED,
            withStarted = true,
            withFinished = true,
        ),
        Template(
            title = "Mop kitchen floor",
            description = "Full mop after lunch service. Photo required to confirm completion.",
            requiresPhoto = true,
            startOffsetMin = -240,
            dueOffsetMin = -90,
            status = TaskStatus.FINISHED,
            withStarted = true,
            withFinished = true,
            withPhoto = true,
        ),
        Template(
            title = "Clean restrooms",
            description = "Refill soap, paper, sanitize stalls. Photo required after.",
            requiresPhoto = true,
            startOffsetMin = -300,
            dueOffsetMin = -60,
            status = TaskStatus.OVERDUE,
        ),
    )

    fun seedAllStaff(
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
        onDone: (totalTasks: Int) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("staff_list").get()
            .addOnSuccessListener { snap ->
                val staffIds = snap.documents.map { it.id }
                if (staffIds.isEmpty()) {
                    onDone(0)
                    return@addOnSuccessListener
                }
                seedFor(db, staffIds, 0, 0, onProgress, onDone, onError)
            }
            .addOnFailureListener { onError(it) }
    }

    private fun seedFor(
        db: FirebaseFirestore,
        staffIds: List<String>,
        index: Int,
        written: Int,
        onProgress: (Int, Int) -> Unit,
        onDone: (Int) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        if (index >= staffIds.size) {
            onDone(written)
            return
        }
        val staffId = staffIds[index]
        onProgress(index + 1, staffIds.size)

        val batch = db.batch()
        val tasksCol = db.collection("staff_task").document(staffId).collection("tasks")
        val now = System.currentTimeMillis()

        for (t in templates) {
            val ref = tasksCol.document()
            val start = Timestamp(Date(now + t.startOffsetMin * 60_000))
            val due = Timestamp(Date(now + t.dueOffsetMin * 60_000))
            val data = mutableMapOf<String, Any?>(
                "id" to ref.id,
                "title" to t.title,
                "description" to t.description,
                "startTime" to start,
                "dueTime" to due,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "status" to t.status,
                "isCompleted" to (t.status == TaskStatus.FINISHED),
                "completed" to (t.status == TaskStatus.FINISHED),
                "requiresPhoto" to t.requiresPhoto,
                "photoUrls" to if (t.withPhoto) listOf(SAMPLE_PHOTO) else emptyList<String>(),
                "createdBy" to "DemoSeeder",
            )
            if (t.withStarted) data["startedAt"] = Timestamp(Date(now + t.startOffsetMin * 60_000))
            if (t.withFinished) {
                val finishedAt = Timestamp(Date(now + t.dueOffsetMin * 60_000 - 30 * 60_000))
                data["finishedAt"] = finishedAt
                data["completedAt"] = finishedAt
            }
            batch.set(ref, data)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "seeded ${templates.size} tasks for staffId=$staffId")
                seedFor(db, staffIds, index + 1, written + templates.size, onProgress, onDone, onError)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "seed failed for staffId=$staffId", e)
                onError(e)
            }
    }
}
