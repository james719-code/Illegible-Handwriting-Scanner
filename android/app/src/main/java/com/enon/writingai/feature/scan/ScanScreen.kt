package com.enon.writingai.feature.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enon.writingai.domain.model.ProcessingStatus
import com.enon.writingai.feature.capture.components.CaptureStatusCard
import com.enon.writingai.ui.components.AppPrimaryButton
import com.enon.writingai.ui.components.AppSecondaryButton
import com.enon.writingai.ui.common.AppScreenScaffold
import com.enon.writingai.ui.common.AppTextAction

private data class ScanStage(
    val threshold: Int,
    val title: String,
    val detail: String,
)

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onResult: () -> Unit,
    viewModel: ScanViewModel = viewModel(),
) {
    val scrollState = rememberScrollState()
    val progressFraction by animateFloatAsState(
        targetValue = (viewModel.uiState.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "scan-progress",
    )
    val stages = listOf(
        ScanStage(
            threshold = 8,
            title = "Load selected page",
            detail = "Prepare the captured or imported image for the pipeline.",
        ),
        ScanStage(
            threshold = 30,
            title = "Enhance handwriting",
            detail = "Apply the cleanup stages from preprocessing.",
        ),
        ScanStage(
            threshold = 76,
            title = "Analyze legibility",
            detail = "Detect areas that may be harder to read accurately.",
        ),
        ScanStage(
            threshold = 90,
            title = "Extract text",
            detail = "Run OCR and normalize the recognized output.",
        ),
        ScanStage(
            threshold = 96,
            title = "Save the result",
            detail = "Store the completed scan in local history.",
        ),
    )

    AppScreenScaffold(
        title = "Process Page",
        onBack = onBack,
        topBarAction = {
            if (viewModel.uiState.status == ProcessingStatus.Completed) {
                AppTextAction(label = "Reset", onClick = viewModel::reset)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScanProgressHero(
                progress = viewModel.uiState.progress,
                progressFraction = progressFraction,
                stageLabel = viewModel.uiState.stageLabel,
                note = viewModel.uiState.note,
                engineLabel = viewModel.uiState.engineLabel,
            )

            CaptureStatusCard(
                title = when (viewModel.uiState.status) {
                    ProcessingStatus.Idle -> "Ready"
                    ProcessingStatus.Captured,
                    ProcessingStatus.Imported,
                    ProcessingStatus.Preprocessed -> "Prepared"
                    ProcessingStatus.Scanning -> "Current stage"
                    ProcessingStatus.Completed -> "Completed"
                    ProcessingStatus.Failed -> "Attention needed"
                },
                message = viewModel.uiState.note,
            )

            Text(
                text = "Pipeline status",
                style = MaterialTheme.typography.titleMedium,
            )

            stages.forEach { stage ->
                ScanStageRow(
                    title = stage.title,
                    detail = stage.detail,
                    status = when {
                        viewModel.uiState.progress >= stage.threshold + 10 ||
                            viewModel.uiState.status == ProcessingStatus.Completed -> {
                            ScanStageStatus.Complete
                        }

                        viewModel.uiState.progress >= stage.threshold -> ScanStageStatus.Active
                        else -> ScanStageStatus.Upcoming
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppSecondaryButton(
                    label = "Review result",
                    onClick = onResult,
                    enabled = viewModel.uiState.status == ProcessingStatus.Completed,
                    modifier = Modifier.weight(1f),
                )
                AppPrimaryButton(
                    label = when (viewModel.uiState.status) {
                        ProcessingStatus.Idle -> "Start processing"
                        ProcessingStatus.Captured,
                        ProcessingStatus.Imported,
                        ProcessingStatus.Preprocessed -> "Continue processing"
                        ProcessingStatus.Scanning -> "Processing page"
                        ProcessingStatus.Completed -> "Run again"
                        ProcessingStatus.Failed -> "Try again"
                    },
                    onClick = viewModel::advanceScan,
                    enabled = viewModel.uiState.status != ProcessingStatus.Scanning,
                    modifier = Modifier.weight(1.15f),
                )
            }
        }
    }
}

@Composable
private fun ScanProgressHero(
    progress: Int,
    progressFraction: Float,
    stageLabel: String,
    note: String,
    engineLabel: String,
) {
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(104.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$progress%",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
                        ) {
                            Text(
                                text = engineLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                        Text(
                            text = stageLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private enum class ScanStageStatus {
    Complete,
    Active,
    Upcoming,
}

@Composable
private fun ScanStageRow(
    title: String,
    detail: String,
    status: ScanStageStatus,
) {
    val icon: ImageVector
    val tint = when (status) {
        ScanStageStatus.Complete -> {
            icon = Icons.Default.CheckCircle
            MaterialTheme.colorScheme.primary
        }

        ScanStageStatus.Active -> {
            icon = Icons.Default.Autorenew
            MaterialTheme.colorScheme.tertiary
        }

        ScanStageStatus.Upcoming -> {
            icon = Icons.Default.RadioButtonUnchecked
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
