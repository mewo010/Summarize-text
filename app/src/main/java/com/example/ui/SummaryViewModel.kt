package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiSummarizerService
import com.example.data.SummaryEntity
import com.example.data.SummaryLength
import com.example.data.SummaryMode
import com.example.data.SummaryRepository
import com.example.data.SummaryTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SamplePreset(
    val id: String,
    val title: String,
    val category: String,
    val text: String
)

enum class AppDestination {
    SUMMARIZE,
    VAULT,
    WORKFLOW
}

data class SummarizerUiState(
    val showSplash: Boolean = true,
    val currentTab: AppDestination = AppDestination.SUMMARIZE,
    val isDarkTheme: Boolean = true,
    val inputText: String = "",
    val selectedMode: SummaryMode = SummaryMode.EXECUTIVE,
    val selectedLength: SummaryLength = SummaryLength.BALANCED,
    val selectedTone: SummaryTone = SummaryTone.DIRECT,
    val isSummarizing: Boolean = false,
    val loadingStepIndex: Int = 0,
    val loadingProgress: Float = 0f,
    val activeResult: SummaryEntity? = null,
    val usedLiveAi: Boolean = false,
    val statusBanner: String? = null,
    val searchQuery: String = "",
    val filterFavoritesOnly: Boolean = false,
    val isSpeaking: Boolean = false
)

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SummaryRepository
    private val geminiService = GeminiSummarizerService()

    private val _uiState = MutableStateFlow(SummarizerUiState())
    val uiState: StateFlow<SummarizerUiState> = _uiState.asStateFlow()

    val savedSummaries: StateFlow<List<SummaryEntity>>

    val presets = listOf(
        SamplePreset(
            id = "preset_ai_arch",
            title = "AI Systems Study",
            category = "Tech & Research",
            text = "It is important to note that contemporary large language models utilize transformer architectures with multi-head self-attention mechanisms in order to process long-range dependencies across thousands of tokens simultaneously. Due to the fact that quadratic memory scaling occurs as context windows expand, engineering teams are increasingly deploying sparse attention patterns, speculative decoding, and quantized key-value caches to reduce inference latency by up to 65% on edge and mobile hardware. Nevertheless, maintaining factual grounding requires hybrid retrieval pipelines that verify generated claims against curated vector indices before presenting final answers to end users."
        ),
        SamplePreset(
            id = "preset_product_sync",
            title = "Q4 Product Sync",
            category = "Meeting Notes",
            text = "At this point in time, the mobile engineering team has completed 85% of the offline caching migration for the Android release scheduled on November 18. Design review highlighted that users struggled with multi-step onboarding forms, so we must simplify the home workspace into a single-tap action card by Friday. Furthermore, QA requested automated GitHub Actions workflows that compile the signed APK artifact and publish release notes directly to the GitHub Releases tab whenever a version tag is pushed."
        ),
        SamplePreset(
            id = "preset_legal_clause",
            title = "Dense Contract Clause",
            category = "Long Sentences",
            text = "In light of the fact that the Licensee may from time to time deploy proprietary modifications to the underlying software development kit across distributed client environments, the Licensor hereby grants a non-exclusive, revocable, worldwide right to distribute compiled binary artifacts, provided that all attribution notices, cryptographic checksums, and security disclosures remain intact and unmodified within the accompanying documentation."
        ),
        SamplePreset(
            id = "preset_climate_report",
            title = "Grid Storage Brief",
            category = "Executive Report",
            text = "Global utility-scale battery storage capacity nearly doubled over the past twelve months as lithium-iron-phosphate cell costs declined by 28% and grid operators prioritized millisecond frequency regulation. Analysts project that coupling solar microgrids with four-hour iron-air and sodium-ion storage buffers will eliminate peak evening curtailment in sun-belt regions while lowering wholesale balancing costs for municipal utilities."
        )
    )

    init {
        val dao = AppDatabase.getInstance(application).summaryDao()
        repository = SummaryRepository(dao)
        savedSummaries = repository.allSummaries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Automatically dismiss the animated launch splash screen after 1.35s
        viewModelScope.launch {
            delay(1350)
            _uiState.update { it.copy(showSplash = false) }
        }

        // Seed initial sample text and pre-populated vault item if empty on first launch
        _uiState.update {
            it.copy(
                inputText = presets.first().text,
                usedLiveAi = geminiService.isApiKeyConfigured()
            )
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(showSplash = false) }
    }

    fun selectTab(tab: AppDestination) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun updateInputText(newText: String) {
        _uiState.update { it.copy(inputText = newText, statusBanner = null) }
    }

    fun applyPreset(preset: SamplePreset) {
        _uiState.update {
            it.copy(
                inputText = preset.text,
                statusBanner = "Loaded preset: ${preset.title}"
            )
        }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                activeResult = null,
                statusBanner = null
            )
        }
    }

    fun selectMode(mode: SummaryMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun selectLength(length: SummaryLength) {
        _uiState.update { it.copy(selectedLength = length) }
    }

    fun selectTone(tone: SummaryTone) {
        _uiState.update { it.copy(selectedTone = tone) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(filterFavoritesOnly = !it.filterFavoritesOnly) }
    }

    fun setSpeakingState(speaking: Boolean) {
        _uiState.update { it.copy(isSpeaking = speaking) }
    }

    fun summarizeCurrentText() {
        val currentState = _uiState.value
        val rawText = currentState.inputText.trim()
        if (rawText.isEmpty() || currentState.isSummarizing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSummarizing = true,
                    loadingStepIndex = 0,
                    loadingProgress = 0.18f,
                    statusBanner = null
                )
            }

            // Progress animation coroutine so the user sees the animated prism loading screen icon & stages
            val progressJob = launch {
                delay(260)
                _uiState.update { it.copy(loadingStepIndex = 1, loadingProgress = 0.56f) }
                delay(280)
                _uiState.update { it.copy(loadingStepIndex = 2, loadingProgress = 0.88f) }
            }

            val result = geminiService.summarizeText(
                sourceText = rawText,
                mode = currentState.selectedMode,
                length = currentState.selectedLength,
                tone = currentState.selectedTone
            )

            // Ensure smooth minimum animation visibility
            progressJob.join()
            _uiState.update { it.copy(loadingProgress = 1.0f) }
            delay(120)

            val insertedId = repository.insert(result.entity)
            val savedEntity = result.entity.copy(id = insertedId)

            _uiState.update {
                it.copy(
                    isSummarizing = false,
                    activeResult = savedEntity,
                    usedLiveAi = result.usedLiveAi,
                    statusBanner = result.noticeMessage
                )
            }
        }
    }

    fun toggleFavorite(summary: SummaryEntity) {
        viewModelScope.launch {
            val updatedFav = !summary.isFavorite
            repository.toggleFavorite(summary.id, updatedFav)
            _uiState.update { state ->
                if (state.activeResult?.id == summary.id) {
                    state.copy(activeResult = state.activeResult.copy(isFavorite = updatedFav))
                } else {
                    state
                }
            }
        }
    }

    fun loadSummaryIntoWorkspace(summary: SummaryEntity) {
        val mode = SummaryMode.entries.find { it.name == summary.modeName } ?: SummaryMode.EXECUTIVE
        val length = SummaryLength.entries.find { it.name == summary.lengthName } ?: SummaryLength.BALANCED
        val tone = SummaryTone.entries.find { it.name == summary.toneName } ?: SummaryTone.DIRECT
        _uiState.update {
            it.copy(
                currentTab = AppDestination.SUMMARIZE,
                inputText = summary.originalText,
                selectedMode = mode,
                selectedLength = length,
                selectedTone = tone,
                activeResult = summary
            )
        }
    }

    fun deleteSummary(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _uiState.update { state ->
                if (state.activeResult?.id == id) {
                    state.copy(activeResult = null)
                } else {
                    state
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _uiState.update { it.copy(activeResult = null) }
        }
    }
}
