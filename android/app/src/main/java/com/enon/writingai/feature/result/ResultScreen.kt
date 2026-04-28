package com.enon.writingai.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.model.displayTitle
import com.enon.writingai.domain.model.hasUsableAnalysis
import com.enon.writingai.domain.model.hasUsableOcrText
import com.enon.writingai.ui.components.AppPrimaryButton
import com.enon.writingai.ui.components.AppSecondaryButton
import com.enon.writingai.ui.common.AppScreenScaffold
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: ResultViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.refreshLatest()
    }

    var showOriginal by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val result = viewModel.result

    LaunchedEffect(result?.id) {
        if (result != null) {
            delay(3_000)
            onBack()
        }
    }

    AppScreenScaffold(
        title = "Scan Result",
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ResultHeroCard(result = result)

            ViewModeToggle(
                showOriginal = showOriginal,
                onToggle = { showOriginal = it },
            )

            if (showOriginal) {
                OriginalPreviewCard(result = result)
            } else {
                ExtractedTextCard(result = result)
            }

            if (result?.hasUsableAnalysis() == true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ResultMetricCard(
                        title = "Confidence",
                        value = "${String.format(Locale.US, "%.1f", result.text.confidence * 100f)}%",
                        modifier = Modifier.weight(1f),
                    )
                    ResultMetricCard(
                        title = "Flagged",
                        value = result.flaggedRegions.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    ResultMetricCard(
                        title = "Saved",
                        value = result.createdAt.takeLast(5),
                        modifier = Modifier.weight(1f),
                    )
                }
            } else if (result != null) {
                ResultMetricCard(
                    title = "Saved",
                    value = result.createdAt.takeLast(5),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Result saved",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = if (result?.hasUsableOcrText() == true) {
                            "This scan is already stored in local history, so you can return to it anytime."
                        } else {
                            "This image was saved, but there is no OCR text available to show for this result."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppSecondaryButton(
                    label = "Export TXT",
                    onClick = viewModel::exportTxt,
                    enabled = result != null && !viewModel.exportState.inProgress,
                    modifier = Modifier.weight(1f),
                )
                AppSecondaryButton(
                    label = "Export PDF",
                    onClick = viewModel::exportPdf,
                    enabled = result != null && !viewModel.exportState.inProgress,
                    modifier = Modifier.weight(1f),
                )
            }

            viewModel.exportState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppSecondaryButton(
                    label = "Main menu",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                AppPrimaryButton(
                    label = "Open history",
                    onClick = onHistoryClick,
                    enabled = result != null,
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}

@Composable
private fun ResultHeroCard(result: ScanResult?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                ) {
                    Text(
                        text = if (result == null) "Waiting for output" else "Latest completed scan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                Text(
                    text = result?.displayTitle() ?: "No scan result yet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = result?.createdAt ?: "Run the OCR pipeline to review extracted text and the source preview here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExtractedTextCard(result: ScanResult?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Extracted text",
                style = MaterialTheme.typography.titleMedium,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                SelectionContainer {
                    Text(
                        text = when {
                            result == null -> "Run a scan to view extracted text here."
                            result.hasUsableOcrText() -> result.text.normalizedText
                            else -> "No extracted text is available for this saved scan."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (result == null || result.hasUsableOcrText().not()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginalPreviewCard(result: ScanResult?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Original source",
                style = MaterialTheme.typography.titleMedium,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                if (result?.sourceUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No source preview is available for this result.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    AsyncImage(
                        model = result?.sourceUri,
                        contentDescription = "Original scan preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ViewModeToggle(
    showOriginal: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToggleButton(
                label = "TEXT VIEW",
                selected = !showOriginal,
                onClick = { onToggle(false) },
                modifier = Modifier.weight(1f),
            )
            ToggleButton(
                label = "ORIGINAL",
                selected = showOriginal,
                onClick = { onToggle(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Box(
            modifier = Modifier.padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
