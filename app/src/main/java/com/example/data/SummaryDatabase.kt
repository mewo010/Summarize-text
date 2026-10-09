package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

enum class SummaryMode(val label: String, val badge: String, val promptInstruction: String) {
    EXECUTIVE(
        label = "Executive Brief",
        badge = "Brief",
        promptInstruction = "Write a polished executive summary paragraph highlighting the core thesis, strategic context, and main conclusion."
    ),
    BULLETS(
        label = "Key Bullets",
        badge = "Bullets",
        promptInstruction = "Synthesize the text into scannable, high-signal bullet points covering every essential fact without fluff."
    ),
    TLDR(
        label = "One-Line TL;DR",
        badge = "TL;DR",
        promptInstruction = "Distill the entire text into a single, ultra-punchy, memorable 1-2 sentence takeaway."
    ),
    ELI5(
        label = "Simple / Plain",
        badge = "Simple",
        promptInstruction = "Rewrite and summarize the complex sentences in plain, everyday, jargon-free language anyone can immediately grasp."
    ),
    ACTION_ITEMS(
        label = "Action Items",
        badge = "Actions",
        promptInstruction = "Extract concrete next steps, decisions, responsibilities, and actionable takeaways from the text."
    ),
    ACADEMIC(
        label = "Structured Abstract",
        badge = "Abstract",
        promptInstruction = "Summarize with rigorous analytical precision: Background, Core Findings, and Implications."
    )
}

enum class SummaryLength(val label: String, val targetRatioDesc: String) {
    CONCISE("Concise (~20%)", "Keep it extremely brief (around 15-25% of original length)."),
    BALANCED("Balanced (~40%)", "Provide a balanced summary (around 35-45% of original length)."),
    DETAILED("Detailed (~60%)", "Preserve nuanced details and supporting points (around 55-65% of original length).")
}

enum class SummaryTone(val label: String) {
    DIRECT("Direct & Crisp"),
    PROFESSIONAL("Professional"),
    CONVERSATIONAL("Conversational"),
    ANALYTICAL("Analytical")
}

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalText: String,
    val summaryText: String,
    val keyTakeawaysJoined: String, // separated by "|||"
    val actionItemsJoined: String,  // separated by "|||"
    val keywordsJoined: String,     // separated by ","
    val simplifiedSentencesJoined: String, // separated by "|||"
    val modeName: String,
    val lengthName: String,
    val toneName: String,
    val originalWordCount: Int,
    val summaryWordCount: Int,
    val compressionPercent: Int,
    val readingTimeSavedSec: Int,
    val sentiment: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val keyTakeaways: List<String>
        get() = keyTakeawaysJoined.split("|||").map { it.trim() }.filter { it.isNotEmpty() }

    val actionItems: List<String>
        get() = actionItemsJoined.split("|||").map { it.trim() }.filter { it.isNotEmpty() }

    val keywords: List<String>
        get() = keywordsJoined.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val simplifiedSentences: List<String>
        get() = simplifiedSentencesJoined.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
}

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries ORDER BY createdAt DESC")
    fun getAllSummaries(): Flow<List<SummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity): Long

    @Query("UPDATE summaries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM summaries")
    suspend fun clearAll()
}

@Database(entities = [SummaryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summaryDao(): SummaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "brevity_summaries.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SummaryRepository(private val summaryDao: SummaryDao) {
    val allSummaries: Flow<List<SummaryEntity>> = summaryDao.getAllSummaries()

    suspend fun insert(summary: SummaryEntity): Long = summaryDao.insertSummary(summary)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        summaryDao.updateFavorite(id, isFavorite)

    suspend fun deleteById(id: Long) = summaryDao.deleteById(id)

    suspend fun clearAll() = summaryDao.clearAll()
}
