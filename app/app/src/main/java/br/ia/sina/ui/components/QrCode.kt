package br.ia.sina.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun qrBitmap(content: String, size: Int = 25 * 8): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 0))
    val pixels = IntArray(matrix.width * matrix.height) { i ->
        if (matrix[i % matrix.width, i / matrix.width]) 0xFF2A2C30.toInt() else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}

@Composable
fun QrCode(content: String, size: Dp, contentDescription: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { qrBitmap(content).asImageBitmap() }
    Image(bitmap, contentDescription, modifier.size(size), filterQuality = FilterQuality.None)
}
