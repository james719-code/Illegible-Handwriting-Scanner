package com.enon.writingai.feature.preprocessing

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enon.writingai.feature.capture.components.CaptureStatusCard
import com.enon.writingai.ui.components.AppPrimaryButton
import com.enon.writingai.ui.common.AppScreenScaffold
import com.enon.writingai.ui.common.AppTextAction

@Composable
fun PreprocessScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: PreprocessViewModel = viewModel(),
) {
    val scrollState = rememberScrollState()
    val stageTitles = listOf(
        "Recover faint pen strokes",
        "Reduce paper grain",
        "Normalize for OCR",
    )

    AppScreenScaffold(
        title = "Prepare Page",
        onBack = onBack,
        topBarAction = { AppTextAction(label = "Scan", onClick = onContinue) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreprocessHeroCard(stepCount = viewModel.steps.size)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PipelineMetricCard(
                    title = "Pipeline",
                    value = "${viewModel.steps.size} stages",
                    modifier = Modifier.weight(1f),
                )
                PipelineMetricCard(
                    title = "Output",
                    value = "OCR-ready sample",
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Cleanup stages",
                style = MaterialTheme.typography.titleMedium,
            )

            viewModel.steps.forEachIndexed { index, detail ->
                PipelineStageCard(
                    stage = index + 1,
                    title = stageTitles.getOrElse(index) { "Optimization stage" },
                    detail = detail,
                )
            }

            CaptureStatusCard(
                title = "What changes here",
                message = "These steps increase contrast, reduce background noise, and stabilize the page before OCR starts.",
            )

            AppPrimaryButton(
                label = "Run OCR pipeline",
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun PreprocessHeroCard(stepCount: Int) {
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
                    .size(110.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                        shape = CircleShape,
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
                ) {
                    Text(
                        text = "Automatic cleanup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                Text(
                    text = "Tune the page before OCR reads it",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "The app runs $stepCount cleanup stages to improve handwriting contrast and reduce recognition errors.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PipelineMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
private fun PipelineStageCard(
    stage: Int,
    title: String,
    detail: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stage.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
