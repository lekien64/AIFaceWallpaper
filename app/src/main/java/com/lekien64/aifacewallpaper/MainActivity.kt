
package com.lekien64.aifacewallpaper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.util.concurrent.Executors
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.roundToInt

private val Accent = Color(0xFF91AAFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

private data class HeadPose(
    val detected: Boolean = false,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val landmarks: Int = 0
)

private data class Rotation3D(
    val r00: Float, val r01: Float, val r02: Float,
    val r10: Float, val r11: Float, val r12: Float,
    val r20: Float, val r21: Float, val r22: Float
)

private fun normalizeRotation(m: FloatArray): Rotation3D? {
    if (m.size < 16) return null

    val sx = sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2])
    val sy = sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6])
    val sz = sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10])

    if (sx < 0.0001f || sy < 0.0001f || sz < 0.0001f) {
        return null
    }

    return Rotation3D(
        m[0] / sx, m[4] / sy, m[8] / sz,
        m[1] / sx, m[5] / sy, m[9] / sz,
        m[2] / sx, m[6] / sy, m[10] / sz
    )
}

private fun relativeRotation(
    current: Rotation3D,
    baseline: Rotation3D
): Rotation3D {
    // Relative rotation = current * transpose(baseline)
    return Rotation3D(
        current.r00 * baseline.r00 + current.r01 * baseline.r01 + current.r02 * baseline.r02,
        current.r00 * baseline.r10 + current.r01 * baseline.r11 + current.r02 * baseline.r12,
        current.r00 * baseline.r20 + current.r01 * baseline.r21 + current.r02 * baseline.r22,

        current.r10 * baseline.r00 + current.r11 * baseline.r01 + current.r12 * baseline.r02,
        current.r10 * baseline.r10 + current.r11 * baseline.r11 + current.r12 * baseline.r12,
        current.r10 * baseline.r20 + current.r11 * baseline.r21 + current.r12 * baseline.r22,

        current.r20 * baseline.r00 + current.r21 * baseline.r01 + current.r22 * baseline.r02,
        current.r20 * baseline.r10 + current.r21 * baseline.r11 + current.r22 * baseline.r12,
        current.r20 * baseline.r20 + current.r21 * baseline.r21 + current.r22 * baseline.r22
    )
}

private fun rotationToPose(
    rotation: Rotation3D,
    landmarks: Int
): HeadPose {
    // Euler angles for Z-Y-X rotation order.
    val pitch = asin((-rotation.r20).coerceIn(-1f, 1f))
    val yaw = atan2(rotation.r10, rotation.r00)
    val roll = atan2(rotation.r21, rotation.r22)

    val degrees = (180.0 / Math.PI).toFloat()

    return HeadPose(
        detected = true,
        yaw = yaw * degrees,
        pitch = pitch * degrees,
        roll = roll * degrees,
        landmarks = landmarks
    )
}

private fun directionText(pose: HeadPose): String {
    return when {
        pose.yaw < -12f -> "Đang quay trái"
        pose.yaw > 12f -> "Đang quay phải"
        pose.pitch < -10f -> "Đang ngẩng lên"
        pose.pitch > 10f -> "Đang cúi xuống"
        else -> "Gần chính diện"
    }
}

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

        Text(
            "AI FACE",
            color = Accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp
        )

        Text(
            "WALLPAPER",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Tạo hồ sơ khuôn mặt và biến video thành hình nền động.",
            color = Color(0xFFB9C3D8),
            fontSize = 16.sp
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
            color = Color(0xFFB9C3D8),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    var currentAngle by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var pose by remember { mutableStateOf(HeadPose()) }
    var calibrated by remember { mutableStateOf(false) }
    var faceStatus by remember { mutableStateOf("Đang chờ camera") }

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
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF202B3D), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission) {
                CameraPreview(
                    useFrontCamera = useFrontCamera,
                    onPoseResult = { newPose ->
                        pose = newPose
                        faceStatus = if (newPose.detected) {
                            directionText(newPose)
                        } else {
                            "Chưa phát hiện khuôn mặt"
                        }
                    },
                    onError = { faceStatus = it }
                )
            } else {
                Text("Cần cấp quyền camera", color = Color.White)
            }

            Box(
                Modifier
                    .size(230.dp, 300.dp)
                    .border(2.dp, Accent, RoundedCornerShape(48.dp))
            )

            Text(
                "Đưa khuôn mặt vào khung",
                Modifier.align(Alignment.BottomCenter).padding(20.dp),
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        GlassCard {
            Text(
                "PHÂN TÍCH TƯ THẾ 3D",
                color = Accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                faceStatus,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PoseValue("YAW", pose.yaw)
                PoseValue("PITCH", pose.pitch)
                PoseValue("ROLL", pose.roll)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                if (calibrated) "Đã hiệu chuẩn mốc chính diện" else "Chưa hiệu chuẩn",
                color = if (calibrated) Color(0xFF8FE0B0) else Color(0xFFFFCF80),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    // This button is enabled only after a face is detected.
                    calibrationRequested = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pose.detected
            ) {
                Text("HIỆU CHUẨN CHÍNH DIỆN")
            }

            Spacer(Modifier.height(12.dp))

            Text("GÓC ĐANG HƯỚNG DẪN", color = Accent,
                fontWeight = FontWeight.Bold, fontSize = 12.sp)

            Spacer(Modifier.height(6.dp))

            Text(
                angles[currentAngle],
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                progress = { (currentAngle + 1) / 7f },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                color = Accent
            )

            Text(
                "Góc ${currentAngle + 1}/7 · Chưa tự động xác nhận",
                color = Color(0xFFB9C3D8),
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier.weight(1f)
            ) {
                Text("Đổi camera")
            }

            Button(
                onClick = { currentAngle = (currentAngle + 1) % 7 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Góc tiếp theo")
            }
        }
    }
}

private var calibrationRequested = false

@Composable
private fun PoseValue(label: String, value: Float) {
    Column {
        Text(label, color = Accent, fontSize = 11.sp)
        Text(
            "${value.roundToInt()}°",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CameraPreview(
    useFrontCamera: Boolean,
    onPoseResult: (HeadPose) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    DisposableEffect(previewView, useFrontCamera, lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        var landmarker: FaceLandmarker? = null
        var provider: ProcessCameraProvider? = null
        var baseline: Rotation3D? = null

        var smoothYaw = 0f
        var smoothPitch = 0f
        var smoothRoll = 0f
        var hasPrevious = false

        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("face_landmarker.task")
                        .build()
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputFacialTransformationMatrixes(true)
                .setResultListener { result, _ ->
                    val face = result.faceLandmarks().firstOrNull()
                    val matrix = result.facialTransformationMatrixes()
                        .orElse(emptyList())
                        .firstOrNull()

                    if (face == null || matrix == null) {
                        hasPrevious = false
                        onPoseResult(HeadPose())
                    } else {
                        val current = normalizeRotation(matrix)

                        if (current != null) {
                            if (calibrationRequested) {
                                baseline = current
                                calibrationRequested = false
                                hasPrevious = false
                            }

                            val reference = baseline
                            val relative = if (reference != null) {
                                relativeRotation(current, reference)
                            } else {
                                current
                            }

                            val measured = rotationToPose(relative, face.size)

                            if (!hasPrevious) {
                                smoothYaw = measured.yaw
                                smoothPitch = measured.pitch
                                smoothRoll = measured.roll
                                hasPrevious = true
                            } else {
                                smoothYaw = smoothYaw * 0.75f + measured.yaw * 0.25f
                                smoothPitch = smoothPitch * 0.75f + measured.pitch * 0.25f
                                smoothRoll = smoothRoll * 0.75f + measured.roll * 0.25f
                            }

                            onPoseResult(
                                measured.copy(
                                    yaw = smoothYaw,
                                    pitch = smoothPitch,
                                    roll = smoothRoll
                                )
                            )
                        }
                    }
                }
                .setErrorListener {
                    onError("Lỗi MediaPipe: ${it.message ?: "Không xác định"}")
                }
                .build()

            landmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            onError("Không tải được model nhận diện")
        }

        val future = ProcessCameraProvider.getInstance(context)

        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { image ->
                    try {
                        val bitmap = image.toBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()

                        val processing = ImageProcessingOptions.builder()
                            .setRotationDegrees(image.imageInfo.rotationDegrees)
                            .build()

                        landmarker?.detectAsync(
                            mpImage,
                            processing,
                            SystemClock.uptimeMillis()
                        )
                    } catch (e: Exception) {
                        onError("Đang xử lý camera")
                    } finally {
                        image.close()
                    }
                }

                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                onError("Không thể khởi động camera")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            landmarker?.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView }
    )
}

@Composable
private fun GlassCard(
    content: @Composable ColumnScope.() -> Unit
) {
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
