package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class StructuredSummaryOutput(
    val headline: String = "",
    val summary: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val simplifiedSentences: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val sentiment: String = "Informative"
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}

data class SummarizationResult(
    val entity: SummaryEntity,
    val usedLiveAi: Boolean,
    val noticeMessage: String? = null
)

class GeminiSummarizerService {

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")
    }

    suspend fun summarizeText(
        sourceText: String,
        mode: SummaryMode,
        length: SummaryLength,
        tone: SummaryTone
    ): SummarizationResult = withContext(Dispatchers.IO) {
        val cleanText = sourceText.trim()
        val originalWords = countWords(cleanText)

        if (isApiKeyConfigured()) {
            try {
                val prompt = buildPrompt(cleanText, mode, length, tone)
                val schema = buildSummarySchema()
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(
                        parts = listOf(
                            Part(
                                text = "You are Brevity, an expert editorial AI specializing in distilling long sentences, complex documents, and dense passages into crystal-clear summaries while preserving accuracy and nuance."
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = schema,
                        temperature = 0.3f
                    )
                )

                val response = GeminiRetrofitClient.service.generateContent(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = request
                )
                val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawJson.isNullOrBlank()) {
                    val parsed = GeminiRetrofitClient.json.decodeFromString<StructuredSummaryOutput>(rawJson)
                    val entity = createEntityFromOutput(
                        cleanText = cleanText,
                        originalWords = originalWords,
                        output = parsed,
                        mode = mode,
                        length = length,
                        tone = tone
                    )
                    return@withContext SummarizationResult(
                        entity = entity,
                        usedLiveAi = true,
                        noticeMessage = null
                    )
                }
            } catch (e: Exception) {
                val fallbackOutput = localExtractiveSynthesize(cleanText, mode, length, tone)
                val entity = createEntityFromOutput(
                    cleanText = cleanText,
                    originalWords = originalWords,
                    output = fallbackOutput,
                    mode = mode,
                    length = length,
                    tone = tone
                )
                return@withContext SummarizationResult(
                    entity = entity,
                    usedLiveAi = false,
                    noticeMessage = "Gemini API call encountered an issue (${e.localizedMessage ?: "Network error"}). Generated using on-device extractive engine."
                )
            }
        }

        // On-device intelligent extractive + sentence-simplifier engine when API key is placeholder
        val fallbackOutput = localExtractiveSynthesize(cleanText, mode, length, tone)
        val entity = createEntityFromOutput(
            cleanText = cleanText,
            originalWords = originalWords,
            output = fallbackOutput,
            mode = mode,
            length = length,
            tone = tone
        )
        SummarizationResult(
            entity = entity,
            usedLiveAi = false,
            noticeMessage = "Using on-device smart distiller. Set GEMINI_API_KEY in AI Studio Secrets for cloud Gemini 3.5 Flash synthesis."
        )
    }

    private fun buildPrompt(
        text: String,
        mode: SummaryMode,
        length: SummaryLength,
        tone: SummaryTone
    ): String {
        return """
            Summarize and simplify the following text.
            Mode: ${mode.label} - ${mode.promptInstruction}
            Length Target: ${length.label} - ${length.targetRatioDesc}
            Tone: ${tone.label}

            Also break down the most complex sentences into simplified, easy-to-read rewrites in `simplifiedSentences`.

            SOURCE TEXT:
            \"\"\"
            $text
            \"\"\"
        """.trimIndent()
    }

    private fun buildSummarySchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("headline") {
                put("type", "STRING")
                put("description", "A punchy 5-10 word title for this summary.")
            }
            putJsonObject("summary") {
                put("type", "STRING")
                put("description", "The main synthesized summary matching the requested mode, length, and tone.")
            }
            putJsonObject("keyTakeaways") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "3 to 5 high-impact bullet takeaways.")
            }
            putJsonObject("actionItems") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "2 to 4 concrete next steps, implications, or action items.")
            }
            putJsonObject("simplifiedSentences") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "Plain-language rewrites of the longest or densest sentences in the input.")
            }
            putJsonObject("keywords") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "4 to 6 key topic tags.")
            }
            putJsonObject("sentiment") {
                put("type", "STRING")
                put("description", "One-word or two-word tone classification, e.g., Analytical, Urgent, Optimistic, Technical.")
            }
        }
        putJsonArray("required") {
            add(kotlinx.serialization.json.JsonPrimitive("headline"))
            add(kotlinx.serialization.json.JsonPrimitive("summary"))
            add(kotlinx.serialization.json.JsonPrimitive("keyTakeaways"))
            add(kotlinx.serialization.json.JsonPrimitive("actionItems"))
            add(kotlinx.serialization.json.JsonPrimitive("simplifiedSentences"))
            add(kotlinx.serialization.json.JsonPrimitive("keywords"))
            add(kotlinx.serialization.json.JsonPrimitive("sentiment"))
        }
    }

    private fun createEntityFromOutput(
        cleanText: String,
        originalWords: Int,
        output: StructuredSummaryOutput,
        mode: SummaryMode,
        length: SummaryLength,
        tone: SummaryTone
    ): SummaryEntity {
        val summaryWords = countWords(output.summary)
        val compression = if (originalWords > 0) {
            ((1.0 - (summaryWords.toDouble() / originalWords.toDouble())) * 100.0)
                .roundToInt()
                .coerceIn(5, 95)
        } else {
            50
        }
        // Average reading speed ~ 200 words per minute (~3.3 words/sec)
        val wordsSaved = max(0, originalWords - summaryWords)
        val secondsSaved = max(1, (wordsSaved / 3.3).roundToInt())

        return SummaryEntity(
            title = output.headline.ifBlank { "Distilled ${mode.badge} Summary" },
            originalText = cleanText,
            summaryText = output.summary,
            keyTakeawaysJoined = output.keyTakeaways.joinToString("|||"),
            actionItemsJoined = output.actionItems.joinToString("|||"),
            keywordsJoined = output.keywords.joinToString(","),
            simplifiedSentencesJoined = output.simplifiedSentences.joinToString("|||"),
            modeName = mode.name,
            lengthName = length.name,
            toneName = tone.name,
            originalWordCount = originalWords,
            summaryWordCount = summaryWords,
            compressionPercent = compression,
            readingTimeSavedSec = secondsSaved,
            sentiment = output.sentiment.ifBlank { "Analytical" }
        )
    }

    /**
     * Deterministic TF-IDF + clause-simplification extractive engine used when offline or before
     * the user configures a cloud GEMINI_API_KEY.
     */
    internal fun localExtractiveSynthesize(
        text: String,
        mode: SummaryMode,
        length: SummaryLength,
        tone: SummaryTone
    ): StructuredSummaryOutput {
        val sentences = splitIntoSentences(text)
        val stopWords = setOf(
            "the", "and", "that", "have", "for", "not", "with", "you", "this", "but",
            "his", "from", "they", "say", "her", "she", "will", "one", "all", "would",
            "there", "their", "what", "out", "about", "who", "get", "which", "when",
            "make", "can", "like", "time", "just", "him", "know", "take", "people",
            "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think",
            "also", "back", "after", "use", "two", "how", "our", "work", "first",
            "well", "way", "even", "new", "want", "because", "any", "these", "give",
            "most", "are", "was", "were", "been", "has", "had", "while", "where"
        )

        val wordFreq = mutableMapOf<String, Int>()
        val words = Regex("[A-Za-z]{4,}").findAll(text.lowercase()).map { it.value }.toList()
        for (w in words) {
            if (w !in stopWords) {
                wordFreq[w] = (wordFreq[w] ?: 0) + 1
            }
        }

        val topKeywords = wordFreq.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key.replaceFirstChar { c -> c.uppercase() } }
            .ifEmpty { listOf("Core Idea", "Synthesis", "Clarity", "Key Point") }

        val scoredSentences = sentences.mapIndexed { index, sentence ->
            val sWords = Regex("[A-Za-z]{4,}").findAll(sentence.lowercase()).map { it.value }.toList()
            val keywordScore = sWords.sumOf { wordFreq[it] ?: 0 }.toDouble() / max(1, sWords.size)
            val positionBoost = when {
                index == 0 -> 1.35
                index == sentences.lastIndex -> 1.15
                else -> 1.0
            }
            Triple(index, sentence, keywordScore * positionBoost)
        }

        val targetSentenceCount = when (length) {
            SummaryLength.CONCISE -> max(1, (sentences.size * 0.28).roundToInt().coerceAtMost(2))
            SummaryLength.BALANCED -> max(1, (sentences.size * 0.45).roundToInt().coerceAtMost(4))
            SummaryLength.DETAILED -> max(1, (sentences.size * 0.65).roundToInt().coerceAtMost(6))
        }

        val selectedSentences = scoredSentences
            .sortedByDescending { it.third }
            .take(targetSentenceCount)
            .sortedBy { it.first }
            .map { simplifyClause(it.second) }

        val simplifiedList = sentences
            .sortedByDescending { it.length }
            .take(3)
            .map { simplifyClause(it) }

        val mainSummary = when (mode) {
            SummaryMode.TLDR -> selectedSentences.firstOrNull() ?: simplifyClause(text)
            SummaryMode.BULLETS -> selectedSentences.joinToString("\n• ", prefix = "• ")
            SummaryMode.ELI5 -> "In plain terms: " + selectedSentences.joinToString(" ")
            SummaryMode.ACTION_ITEMS -> selectedSentences.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
            SummaryMode.ACADEMIC -> "Abstract — " + selectedSentences.joinToString(" ")
            SummaryMode.EXECUTIVE -> selectedSentences.joinToString(" ")
        }

        val headline = buildHeadlineFromKeywords(topKeywords, selectedSentences.firstOrNull() ?: text)

        val takeaways = scoredSentences
            .sortedByDescending { it.third }
            .take(4)
            .map { simplifyClause(it.second) }
            .ifEmpty { listOf(simplifyClause(text)) }

        val actions = listOf(
            "Focus on ${topKeywords.take(2).joinToString(" & ")} as the primary driver.",
            "Review simplified sentence breakdowns to communicate key findings faster.",
            "Share or archive this ${mode.badge.lowercase()} brief with stakeholders."
        )

        val sentiment = when {
            text.contains("risk", ignoreCase = true) || text.contains("warning", ignoreCase = true) -> "Cautionary"
            text.contains("growth", ignoreCase = true) || text.contains("breakthrough", ignoreCase = true) -> "Optimistic"
            text.contains("must", ignoreCase = true) || text.contains("deadline", ignoreCase = true) -> "Action-Oriented"
            else -> tone.label
        }

        return StructuredSummaryOutput(
            headline = headline,
            summary = mainSummary,
            keyTakeaways = takeaways,
            actionItems = actions,
            simplifiedSentences = simplifiedList,
            keywords = topKeywords,
            sentiment = sentiment
        )
    }

    private fun simplifyClause(sentence: String): String {
        var cleaned = sentence.trim()
        // Strip verbose introductory filler phrases for cleaner sentence compression
        val fillerPrefixes = listOf(
            "It is important to note that ",
            "It should be noted that ",
            "In order to ",
            "Due to the fact that ",
            "At this point in time, ",
            "For all intents and purposes, ",
            "As a matter of fact, ",
            "In light of the fact that "
        )
        for (prefix in fillerPrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).replaceFirstChar { it.uppercase() }
            }
        }
        cleaned = cleaned
            .replace(Regex("(?i)due to the fact that"), "because")
            .replace(Regex("(?i)in order to"), "to")
            .replace(Regex("(?i)at this point in time"), "now")
            .replace(Regex("(?i)utilize"), "use")
            .replace(Regex("(?i)facilitate"), "help")
            .replace(Regex("(?i)subsequently"), "then")
            .replace(Regex("(?i)nevertheless"), "still")
            .replace(Regex("\\s+"), " ")
        if (!cleaned.endsWith(".") && !cleaned.endsWith("!") && !cleaned.endsWith("?")) {
            cleaned += "."
        }
        return cleaned
    }

    private fun buildHeadlineFromKeywords(keywords: List<String>, firstSentence: String): String {
        val cleanWords = firstSentence
            .replace(Regex("[^A-Za-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return if (keywords.size >= 2) {
            "${keywords[0]} & ${keywords[1]}: Core Insights"
        } else if (cleanWords.size >= 5) {
            cleanWords.take(6).joinToString(" ")
        } else {
            "Distilled Text Summary"
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        val raw = text.split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length > 8 }
        return if (raw.isEmpty()) listOf(text.trim()) else raw
    }

    companion object {
        fun countWords(text: String): Int {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return 0
            return trimmed.split(Regex("\\s+")).size
        }

        fun countSentences(text: String): Int {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return 0
            return max(1, trimmed.split(Regex("[.!?]+")).count { it.isNotBlank() })
        }
    }
}
