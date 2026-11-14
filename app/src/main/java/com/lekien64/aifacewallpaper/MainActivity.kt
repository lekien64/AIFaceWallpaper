
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
        setContent {
            AppScreen()
        }
    }
}

@Composable
private fun AppScreen() {
    val dark = isSystemInDarkTheme()

    val backgroundTop = if (dark) Color(0xFF10141F)
        else Color(0xFFE8EEFA)

    val backgroundBottom = if (dark) Color(0xFF202C42)
        else Color(0xFFD8E4F7)

    val mainText = if (dark) Color(0xFFF4F6FF)
        else Color(0xFF18243A)

    val secondaryText = if (dark) Color(0xFFB9C3D8)
        else Color(0xFF596780)

    val accent = if (dark) Color(0xFF91AAFF)
        else Color(0xFF426CE0)

    val cardColor = if (dark) Color(0xFF30394B).copy(alpha = 0.76f)
        else Color(0xFFFFFFFF).copy(alpha = 0.78f)

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(backgroundTop, backgroundBottom)
                    )
                )
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(22.dp))

            Text(
                text = "AI FACE",
                color = accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp
            )

            Text(
                text = "WALLPAPER",
                color = mainText,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Tạo hồ sơ khuôn mặt và biến video thành hình nền động.",
                color = secondaryText,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )

            GlassCard(cardColor) {
                Text(
                    text = "HỒ SƠ KHUÔN MẶT",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(15.dp))

                Text(
                    text = "Chưa có hồ sơ",
                    color = mainText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = "Bộ quét 7 góc sẽ hướng dẫn chụp và kiểm tra chất lượng ảnh khuôn mặt.",
                    color = secondaryText,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "AI Face Scanner · Sắp ra mắt",
                        fontSize = 15.sp
                    )
                }
            }

            GlassCard(cardColor) {
                Text(
                    text = "CHẾ ĐỘ TẠO VIDEO",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Review Before Create",
                    color = mainText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = "Bạn sẽ xem lại hồ sơ và mẫu video trước khi bắt đầu xử lý. Tự động tạo chỉ hoạt động khi được bật rõ ràng.",
                    color = secondaryText,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Xử lý ngoại tuyến · Dữ liệu lưu trên thiết bị",
                color = secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun GlassCard(
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            content = content
        )
    }
}
