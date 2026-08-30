package com.novatube.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NovaTubeTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)
)
