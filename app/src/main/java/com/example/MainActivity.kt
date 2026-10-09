package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SummaryEntity
import com.example.ui.AnimatedLaunchSplashScreen
import com.example.ui.AnimatedPrismLoadingIcon
import com.example.ui.AppDestination
import com.example.ui.GitHubPipelineScreen
import com.example.ui.SavedVaultScreen
import com.example.ui.SummarizeWorkspaceScreen
import com.example.ui.SummaryViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val summaryViewModel: SummaryViewModel = viewModel()
            val uiState by summaryViewModel.uiState.collectAsStateWithLifecycle()
            val savedSummaries by summaryViewModel.savedSummaries.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                BrevityAppRoot(
                    viewModel = summaryViewModel,
                    savedSummaries = savedSummaries
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrevityAppRoot(
    viewModel: SummaryViewModel,
    savedSummaries: List<SummaryEntity>
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Native Android TextToSpeech controller for listening to summaries
    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsHolder[0]?.language = Locale.US
            }
        }
        ttsHolder[0] = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Handle system back button when on secondary tabs
    if (uiState.currentTab != AppDestination.SUMMARIZE) {
        BackHandler {
            viewModel.selectTab(AppDestination.SUMMARIZE)
        }
    }

    Crossfade(
        targetState = uiState.showSplash,
        label = "splash_to_main_crossfade"
    ) { showSplash ->
        if (showSplash) {
            AnimatedLaunchSplashScreen(
                onSkipSplash = { viewModel.dismissSplash() }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 720.dp

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedPrismLoadingIcon(
                                        size = 32.dp,
                                        modifier = Modifier.padding(end = 10.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                NavigationBarItem(
                                    selected = uiState.currentTab == AppDestination.SUMMARIZE,
                                    onClick = { viewModel.selectTab(AppDestination.SUMMARIZE) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = stringResource(R.string.nav_summarize)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_summarize)) },
                                    modifier = Modifier.testTag("nav_tab_summarize")
                                )

                                NavigationBarItem(
                                    selected = uiState.currentTab == AppDestination.VAULT,
                                    onClick = { viewModel.selectTab(AppDestination.VAULT) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (savedSummaries.isNotEmpty()) {
                                                    Badge { Text(savedSummaries.size.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmarks,
                                                contentDescription = stringResource(R.string.nav_history)
                                            )
                                        }
                                    },
                                    label = { Text(stringResource(R.string.nav_history)) },
                                    modifier = Modifier.testTag("nav_tab_vault")
                                )

                                NavigationBarItem(
                                    selected = uiState.currentTab == AppDestination.WORKFLOW,
                                    onClick = { viewModel.selectTab(AppDestination.WORKFLOW) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.RocketLaunch,
                                            contentDescription = stringResource(R.string.nav_workflow)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_workflow)) },
                                    modifier = Modifier.testTag("nav_tab_workflow")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavRail) {
                            NavigationRail(
                                modifier = Modifier.testTag("side_navigation_rail")
                            ) {
                                NavigationRailItem(
                                    selected = uiState.currentTab == AppDestination.SUMMARIZE,
                                    onClick = { viewModel.selectTab(AppDestination.SUMMARIZE) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = stringResource(R.string.nav_summarize)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_summarize)) },
                                    modifier = Modifier.testTag("rail_tab_summarize")
                                )
                                NavigationRailItem(
                                    selected = uiState.currentTab == AppDestination.VAULT,
                                    onClick = { viewModel.selectTab(AppDestination.VAULT) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Bookmarks,
                                            contentDescription = stringResource(R.string.nav_history)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_history)) },
                                    modifier = Modifier.testTag("rail_tab_vault")
                                )
                                NavigationRailItem(
                                    selected = uiState.currentTab == AppDestination.WORKFLOW,
                                    onClick = { viewModel.selectTab(AppDestination.WORKFLOW) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.RocketLaunch,
                                            contentDescription = stringResource(R.string.nav_workflow)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_workflow)) },
                                    modifier = Modifier.testTag("rail_tab_workflow")
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (uiState.currentTab) {
                                    AppDestination.SUMMARIZE -> {
                                        SummarizeWorkspaceScreen(
                                            uiState = uiState,
                                            presets = viewModel.presets,
                                            onInputTextChanged = viewModel::updateInputText,
                                            onSelectPreset = viewModel::applyPreset,
                                            onPasteClipboard = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                                if (!pasted.isNullOrBlank()) {
                                                    viewModel.updateInputText(pasted)
                                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onClearInput = viewModel::clearInput,
                                            onSelectMode = viewModel::selectMode,
                                            onSelectLength = viewModel::selectLength,
                                            onSelectTone = viewModel::selectTone,
                                            onSummarizeClick = viewModel::summarizeCurrentText,
                                            onCopySummary = { text ->
                                                copyToClipboard(context, "Brevity Summary", text)
                                                Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            onShareSummary = { entity ->
                                                shareSummaryIntent(context, entity)
                                            },
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onToggleSpeak = { text ->
                                                val tts = ttsHolder[0]
                                                if (uiState.isSpeaking) {
                                                    tts?.stop()
                                                    viewModel.setSpeakingState(false)
                                                } else {
                                                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "brevity_utt")
                                                    viewModel.setSpeakingState(true)
                                                }
                                            }
                                        )
                                    }

                                    AppDestination.VAULT -> {
                                        SavedVaultScreen(
                                            summaries = savedSummaries,
                                            searchQuery = uiState.searchQuery,
                                            filterFavoritesOnly = uiState.filterFavoritesOnly,
                                            onSearchQueryChange = viewModel::updateSearchQuery,
                                            onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                                            onOpenSummary = viewModel::loadSummaryIntoWorkspace,
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onShareSummary = { entity -> shareSummaryIntent(context, entity) },
                                            onDeleteSummary = viewModel::deleteSummary,
                                            onClearAll = viewModel::clearAllHistory
                                        )
                                    }

                                    AppDestination.WORKFLOW -> {
                                        GitHubPipelineScreen(
                                            onCopyWorkflowYaml = { yaml ->
                                                copyToClipboard(context, "GitHub Workflow YAML", yaml)
                                                Toast.makeText(context, "Workflow YAML copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareSummaryIntent(context: Context, summary: SummaryEntity) {
    val shareBody = buildString {
        appendLine("✨ ${summary.title}")
        appendLine("(${summary.compressionPercent}% shorter • Saved ${summary.readingTimeSavedSec}s)")
        appendLine()
        appendLine(summary.summaryText)
        if (summary.keyTakeaways.isNotEmpty()) {
            appendLine()
            appendLine("Key Takeaways:")
            summary.keyTakeaways.forEach { appendLine("• $it") }
        }
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, summary.title)
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    val chooser = Intent.createChooser(sendIntent, "Share Summary").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
