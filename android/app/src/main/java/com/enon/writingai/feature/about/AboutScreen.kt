package com.enon.writingai.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.enon.writingai.R
import com.enon.writingai.feature.result.components.ResultSummaryCard
import com.enon.writingai.ui.common.AppScreenScaffold

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionLabel = remember(context) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        "v$versionName ($versionCode)"
    }
    val scrollState = rememberScrollState()

    AppScreenScaffold(
        title = stringResource(R.string.about_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.about_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            ResultSummaryCard(
                title = stringResource(R.string.about_summary_title),
                detail = stringResource(R.string.about_summary),
            )
            Text(
                text = stringResource(R.string.about_tutorial_title),
                style = MaterialTheme.typography.titleMedium,
            )
            TutorialStepCard(
                step = 1,
                title = stringResource(R.string.about_step_1_title),
                detail = stringResource(R.string.about_step_1_detail),
            )
            TutorialStepCard(
                step = 2,
                title = stringResource(R.string.about_step_2_title),
                detail = stringResource(R.string.about_step_2_detail),
            )
            TutorialStepCard(
                step = 3,
                title = stringResource(R.string.about_step_3_title),
                detail = stringResource(R.string.about_step_3_detail),
            )
            TutorialStepCard(
                step = 4,
                title = stringResource(R.string.about_step_4_title),
                detail = stringResource(R.string.about_step_4_detail),
            )
            TutorialStepCard(
                step = 5,
                title = stringResource(R.string.about_step_5_title),
                detail = stringResource(R.string.about_step_5_detail),
            )
            ResultSummaryCard(
                title = stringResource(R.string.about_tips_title),
                detail = stringResource(R.string.about_tips_detail),
            )
            ResultSummaryCard(
                title = stringResource(R.string.about_support_title),
                detail = stringResource(R.string.about_support),
            )
            ResultSummaryCard(
                title = stringResource(R.string.about_version_title),
                detail = versionLabel,
            )
        }
    }
}

@Composable
private fun TutorialStepCard(
    step: Int,
    title: String,
    detail: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
