
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

private val Accent = Color(0xFF91AAFF)

private data class HeadPose(
    val detected: Boolean = false,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val landmarks: Int = 0
)

private fun estimateHeadPose(
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    frontCamera: Boolean
): HeadPose? {
    if (landmarks.size < 264) return null

    val nose = landmarks[1]
    val leftEye = landmarks[33]
    val rightEye = landmarks[263]
    val mouthLeft = landmarks[61]
    val mouthRight = landmarks[291]

    val eyeCenterX = (leftEye.x() + rightEye.x()) / 2f
    val eyeCenterY = (leftEye.y() + rightEye.y()) / 2f
    val eyeWidth = kotlin.math.abs(leftEye.x() - rightEye.x())

    if (eyeWidth < 0.001f) return null

    val noseOffset = (nose.x() - eyeCenterX) / eyeWidth

    var yaw = Math.toDegrees(
        atan((noseOffset * 2.0f).toDouble())
    ).toFloat()

    if (frontCamera) yaw = -yaw

    val mouthCenterY = (mouthLeft.y() + mouthRight.y()) / 2f
    val eyeToMouth = mouthCenterY - eyeCenterY

    if (eyeToMouth <= 0.001f) return null

    val nosePosition = (nose.y() - eyeCenterY) / eyeToMouth

    val pitch = ((nosePosition - 0.52f) * 120f)
        .coerceIn(-45f, 45f)

    return HeadPose(
        detected = true,
        yaw = yaw.coerceIn(-60f, 60f),
        pitch = pitch,
        landmarks = landmarks.size
    )
}

private fun directionText(yaw: Float, pitch: Float): String {
    return when {
        yaw < -12f -> "Đang quay trái"
        yaw > 12f -> "Đang quay phải"
        pitch < -10f -> "Đang ngẩng lên"
        pitch > 10f -> "Đang cúi xuống"
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
    ) { granted ->
        hasPermission = granted
    }

    var currentAngle by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var pose by remember { mutableStateOf(HeadPose()) }
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
                CameraPreview(
                    useFrontCamera = useFrontCamera,
                    onPoseResult = { newPose ->
                        pose = newPose
                        faceStatus = if (newPose.detected) {
                            directionText(newPose.yaw, newPose.pitch)
                        } else {
                            "Chưa phát hiện khuôn mặt"
                        }
                    },
                    onError = { message ->
                        faceStatus = message
                    }
                )
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
            Text(
                "AI PHÂN TÍCH TƯ THẾ ĐẦU",
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                faceStatus,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PoseValue("YAW", pose.yaw)
                PoseValue("PITCH", pose.pitch)
                Column {
                    Text("LANDMARKS", color = Accent, fontSize = 11.sp)
                    Text(
                        "${pose.landmarks}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Số đo ước lượng, chưa hiệu chuẩn",
                color = Color(0xFFB9C3D8),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(14.dp))

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
                    pose = HeadPose()
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
        val cameraExecutor = Executors.newSingleThreadExecutor()
        var faceLandmarker: FaceLandmarker? = null
        var cameraProvider: ProcessCameraProvider? = null

        var smoothYaw = 0f
        var smoothPitch = 0f
        var hasPreviousPose = false

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
                .setResultListener { result, _ ->
                    val face = result.faceLandmarks().firstOrNull()

                    if (face == null) {
                        hasPreviousPose = false
                        onPoseResult(HeadPose())
                    } else {
                        val measured = estimateHeadPose(face, useFrontCamera)

                        if (measured != null) {
                            if (!hasPreviousPose) {
                                smoothYaw = measured.yaw
                                smoothPitch = measured.pitch
                                hasPreviousPose = true
                            } else {
                                smoothYaw = smoothYaw * 0.7f + measured.yaw * 0.3f
                                smoothPitch = smoothPitch * 0.7f + measured.pitch * 0.3f
                            }

                            onPoseResult(
                                HeadPose(
                                    detected = true,
                                    yaw = smoothYaw,
                                    pitch = smoothPitch,
                                    landmarks = measured.landmarks
                                )
                            )
                        }
                    }
                }
                .setErrorListener { error ->
                    onError("Lỗi MediaPipe: ${error.message ?: "Không xác định"}")
                }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (error: Exception) {
            onError("Không tải được model nhận diện")
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val landmarker = faceLandmarker

                        if (landmarker != null) {
                            val bitmap = imageProxy.toBitmap()
                            val mpImage = BitmapImageBuilder(bitmap).build()

                            val imageOptions =
                                ImageProcessingOptions.builder()
                                    .setRotationDegrees(
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    .build()

                            landmarker.detectAsync(
                                mpImage,
                                imageOptions,
                                SystemClock.uptimeMillis()
                            )
                        }
                    } catch (error: Exception) {
                        onError("Đang xử lý hình ảnh...")
                    } finally {
                        imageProxy.close()
                    }
                }

                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    analysis
                )
            } catch (error: Exception) {
                onError("Không thể khởi động camera")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProvider?.unbindAll()
            faceLandmarker?.close()
            cameraExecutor.shutdown()
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
