package com.lekien64.aifacewallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

@Composable
private fun AppScreen() {
    val dark = isSystemInDarkTheme()

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme()
    ) {
        val bg = if (dark) Color(0xFF10131A) else Color(0xFFF1F4FA)
        val accent = Color(0xFF7898FF)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            bg,
                            if (dark) Color(0xFF202A3B)
                            else Color(0xFFE2EAF8)
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                "AI FACE",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Text(
                "WALLPAPER",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Tạo hồ sơ khuôn mặt và biến video thành hình nền động.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "HỒ SƠ KHUÔN MẶT",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Chưa có hồ sơ",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Bộ quét 7 góc sẽ hướng dẫn chụp và kiểm tra chất lượng ảnh. Tính năng quét đang được phát triển.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(onClick = {}, enabled = false) {
                        Text("AI Face Scanner · Sắp ra mắt")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "CHẾ ĐỘ TẠO VIDEO",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Review Before Create",
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Bạn sẽ xem lại hồ sơ và mẫu video trước khi bắt đầu xử lý. Tự động tạo chỉ hoạt động khi được bật rõ ràng.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Xử lý ngoại tuyến · Dữ liệu lưu trên thiết bị",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
