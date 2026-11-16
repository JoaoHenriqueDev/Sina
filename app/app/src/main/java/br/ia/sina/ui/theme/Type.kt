package br.ia.sina.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Roboto = FontFamily.SansSerif

val Typography = Typography(
    displaySmall = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp),
    titleLarge = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)
