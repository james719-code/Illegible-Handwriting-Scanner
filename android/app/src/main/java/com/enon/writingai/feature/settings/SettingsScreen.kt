package com.enon.writingai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enon.writingai.feature.result.components.ResultSummaryCard
import com.enon.writingai.ui.components.AppSecondaryButton
import com.enon.writingai.ui.common.AppScreenScaffold

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onAboutClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val scrollState = rememberScrollState()

    AppScreenScaffold(title = "Settings", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsHeroCard(engineName = viewModel.uiState.engineName)

            ResultSummaryCard(
                title = "Active AI profile",
                detail = viewModel.uiState.engineName,
            )

            Text(
                text = "Processing controls",
                style = MaterialTheme.typography.titleMedium,
            )

            SettingToggle(
                label = "Enable binarization",
                description = "Boosts contrast for uneven lighting and faint pen strokes before OCR begins.",
                checked = viewModel.uiState.enableBinarization,
                onCheckedChange = viewModel::toggleBinarization,
            )
            SettingToggle(
                label = "Save intermediate images",
                description = "Keep cleanup snapshots for debugging, QA review, or training comparisons.",
                checked = viewModel.uiState.saveIntermediateImages,
                onCheckedChange = viewModel::toggleIntermediateImages,
            )

            ResultSummaryCard(
                title = "Workflow tip",
                detail = "For the cleanest output, keep binarization on and only save intermediate images when you need to inspect the pipeline.",
            )

            AppSecondaryButton(
                label = "Open guide",
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun SettingsHeroCard(engineName: String) {
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
                    .size(108.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                        shape = CircleShape,
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                ) {
                    Text(
                        text = "Pipeline profile",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                Text(
                    text = "Tune how the app prepares handwriting for OCR",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = engineName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
