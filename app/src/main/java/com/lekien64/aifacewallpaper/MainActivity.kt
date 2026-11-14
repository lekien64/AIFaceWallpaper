
package com.lekien64.aifacewallpaper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

private val Accent = Color(0xFF91AAFF)

@Composable
private fun AppScreen() {
    var scannerOpen by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Color(0xFF10141F),
            surface = Color(0xFF283448)
        )
    ) {
        if (scannerOpen) {
            ScannerScreen(onBack = { scannerOpen = false })
        } else {
            HomeScreen(onOpenScanner = { scannerOpen = true })
        }
    }
}

@Composable
private fun HomeScreen(onOpenScanner: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF10141F), Color(0xFF202C42))
                )
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text("AI FACE", color = Accent, fontSize = 15.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 5.sp)

        Text("WALLPAPER", color = Color.White,
            fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)

        Text(
            "Tạo hồ sơ khuôn mặt và biến video thành hình nền động.",
            color = Color(0xFFB9C3D8), fontSize = 16.sp
        )

        GlassCard {
            Text("HỒ SƠ KHUÔN MẶT", color = Accent,
                fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            Text("Chưa có hồ sơ", color = Color.White,
                fontSize = 23.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            Text(
                "Quét 7 góc khuôn mặt để tạo hồ sơ AI cá nhân.",
                color = Color(0xFFB9C3D8)
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onOpenScanner,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("MỞ AI FACE SCANNER")
            }
        }

        GlassCard {
            Text("CHẾ ĐỘ TẠO VIDEO", color = Accent,
                fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            Text("Review Before Create", color = Color.White,
                fontSize = 21.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            Text(
                "Xem lại hồ sơ và mẫu video trước khi xử lý.",
                color = Color(0xFFB9C3D8)
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Xử lý ngoại tuyến · Dữ liệu lưu trên thiết bị",
            color = Color(0xFFB9C3D8), fontSize = 12.sp
        )
    }
}

@Composable
private fun ScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    var currentAngle by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(true) }

    val angles = listOf(
        "Chính diện",
        "Trái 30°",
        "Phải 30°",
        "Trái 60°",
        "Phải 60°",
        "Hơi ngẩng lên",
        "Hơi cúi xuống"
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF10141F))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Quay lại", color = Color.White)
            }

            Spacer(Modifier.weight(1f))

            Text(
                "AI FACE SCANNER",
                color = Accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Color(0xFF202B3D),
                    RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission) {
                CameraPreview(useFrontCamera)
            } else {
                Text(
                    "Cần cấp quyền camera để bắt đầu quét.",
                    color = Color.White
                )
            }

            Box(
                Modifier
                    .size(230.dp, 300.dp)
                    .border(
                        2.dp,
                        Accent.copy(alpha = 0.9f),
                        RoundedCornerShape(48.dp)
                    )
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Đưa khuôn mặt vào khung",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Giữ điện thoại ngang tầm mắt",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        GlassCard {
            Text("GÓC ĐANG QUÉT", color = Accent,
                fontWeight = FontWeight.Bold, fontSize = 12.sp)

            Spacer(Modifier.height(8.dp))

            Text(
                angles[currentAngle],
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (currentAngle + 1) / 7f },
                modifier = Modifier.fillMaxWidth(),
                color = Accent,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Góc ${currentAngle + 1}/7 · Tiến độ hướng dẫn",
                color = Color(0xFFB9C3D8),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    useFrontCamera = !useFrontCamera
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Đổi camera")
            }

            Button(
                onClick = {
                    currentAngle = (currentAngle + 1) % angles.size
                },
                modifier = Modifier.weight(1f),
                enabled = hasPermission
            ) {
                Text("Góc tiếp theo")
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CameraPreview(useFrontCamera: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val providerFuture = ProcessCameraProvider.getInstance(context)

            providerFuture.addListener({
                val provider = providerFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview
                    )
                } catch (_: Exception) {
                    // Camera unavailable or selector unsupported.
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF303B50).copy(alpha = 0.78f)
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            content = content
        )
    }
}
