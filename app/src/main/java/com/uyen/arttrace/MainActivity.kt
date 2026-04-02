package com.uyen.arttrace

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraScreen()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var opacity by remember { mutableStateOf(0.5f) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) imageUri = uri
    }

    LaunchedEffect(Unit) {
        cameraPermission.launchPermissionRequest()
    }

    if (cameraPermission.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Layer 1: Live camera feed
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = CameraPreview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    }, ContextCompat.getMainExecutor(context))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Reference image overlay
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Reference image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(opacity)
                )
            }

            // Layer 3: UI controls at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (imageUri != null) {
                    Text(
                        text = "Opacity: ${(opacity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(onClick = {
                    imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }) {
                    Text("Import from Gallery")
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required", color = Color.White, fontSize = 18.sp)
        }
    }
}