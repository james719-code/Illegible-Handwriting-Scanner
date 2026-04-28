package com.enon.writingai.feature.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.enon.writingai.feature.capture.camera.CameraManager
import com.enon.writingai.feature.capture.camera.ImageCaptureHelper
import com.enon.writingai.feature.capture.components.CaptureStatusCard
import com.enon.writingai.ui.components.AppPrimaryButton
import com.enon.writingai.ui.components.AppSecondaryButton
import com.enon.writingai.ui.common.AppScreenScaffold
import com.enon.writingai.ui.common.AppTextAction

@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: CaptureViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraManager = remember(context) { CameraManager(context) }
    val imageCaptureHelper = remember(context) { ImageCaptureHelper(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        requestedPermission = true
        hasCameraPermission = granted
        if (!granted) {
            viewModel.onCameraError("Camera permission is required to scan handwritten pages.")
        }
    }

    LaunchedEffect(hasCameraPermission, lifecycleOwner) {
        if (hasCameraPermission) {
            cameraManager.bindCamera(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                onReady = viewModel::onCameraReady,
                onError = viewModel::onCameraError,
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    val activity = context.findActivity()
    val permanentlyDenied = !hasCameraPermission &&
        requestedPermission &&
        activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    AppScreenScaffold(
        title = "Scan Document",
        onBack = onBack,
        topBarAction = if (viewModel.canContinue) {
            { AppTextAction(label = "Use", onClick = onContinue) }
        } else {
            null
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    if (viewModel.uiState.capturedUri == null) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize(),
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.14f)),
                        )

                        ViewfinderOverlay(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            CapturePill(
                                icon = Icons.Default.CenterFocusStrong,
                                label = "Detecting edge",
                                highlighted = true,
                            )
                            CapturePill(
                                icon = Icons.Default.Tune,
                                label = "Auto-focus optimized",
                                highlighted = false,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassControl(
                                icon = Icons.Default.Tune,
                                contentDescription = "Camera options",
                            )

                            CaptureShutter(
                                onClick = {
                                    val outputFile = imageCaptureHelper.createOutputFile()
                                    cameraManager.captureToFile(
                                        outputFile = outputFile,
                                        onCaptured = { savedUri ->
                                            viewModel.onPhotoCaptured(
                                                capturedUri = savedUri.toString(),
                                                outputName = imageCaptureHelper.displayName(outputFile),
                                            )
                                        },
                                        onError = viewModel::onCameraError,
                                    )
                                },
                            )

                            GlassControl(
                                icon = Icons.Default.Image,
                                contentDescription = "Latest preview",
                            )
                        }
                    } else {
                        AsyncImage(
                            model = viewModel.uiState.capturedUri,
                            contentDescription = "Captured document preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.10f)),
                            contentScale = ContentScale.Fit,
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.08f)),
                        )

                        CapturePill(
                            icon = Icons.Default.Image,
                            label = "Preview captured photo",
                            highlighted = true,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 18.dp, top = 18.dp),
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AppSecondaryButton(
                                label = "Retake photo",
                                onClick = viewModel::retakeCapture,
                            )
                            AppPrimaryButton(
                                label = "Use this capture",
                                onClick = onContinue,
                            )
                        }
                    }
                }

                Text(
                    text = if (viewModel.uiState.capturedUri == null) {
                        viewModel.uiState.guidance
                    } else {
                        "Preview the captured page. Retake it if the text is blurry, cropped, or tilted."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                CaptureStatusCard(
                    title = "Camera permission",
                    message = if (permanentlyDenied) {
                        "Camera access is blocked. Open app settings to enable permission."
                    } else {
                        "Allow camera access to capture handwritten pages."
                    },
                )
                AppSecondaryButton(
                    label = "Grant camera permission",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
                if (permanentlyDenied) {
                    AppPrimaryButton(
                        label = "Open app settings",
                        onClick = { context.openAppSettings() },
                    )
                }
            }

            CaptureStatusCard(
                title = "Camera",
                message = viewModel.uiState.cameraStatus,
            )
            CaptureStatusCard(
                title = "Output file",
                message = viewModel.uiState.outputName,
            )
            if (viewModel.uiState.capturedUri == null) {
                AppPrimaryButton(
                    label = "Use this capture",
                    onClick = onContinue,
                    enabled = viewModel.canContinue,
                )
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.17f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        CornerBracket(
            modifier = Modifier.align(Alignment.TopStart),
            alignment = Alignment.TopStart,
        )
        CornerBracket(
            modifier = Modifier.align(Alignment.TopEnd),
            alignment = Alignment.TopEnd,
        )
        CornerBracket(
            modifier = Modifier.align(Alignment.BottomStart),
            alignment = Alignment.BottomStart,
        )
        CornerBracket(
            modifier = Modifier.align(Alignment.BottomEnd),
            alignment = Alignment.BottomEnd,
        )
    }
}

@Composable
private fun CornerBracket(
    modifier: Modifier,
    alignment: Alignment,
) {
    Box(
        modifier = modifier
            .padding(10.dp)
            .size(26.dp),
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                .align(if (alignment == Alignment.BottomStart || alignment == Alignment.BottomEnd) Alignment.BottomCenter else Alignment.TopCenter),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(26.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                .align(if (alignment == Alignment.TopEnd || alignment == Alignment.BottomEnd) Alignment.CenterEnd else Alignment.CenterStart),
        )
    }
}

@Composable
private fun CapturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

@Composable
private fun GlassControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
) {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f),
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CaptureShutter(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(82.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.82f),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Capture page",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
