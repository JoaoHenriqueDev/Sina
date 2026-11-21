package br.ia.sina.ui.guardian

import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import br.ia.sina.ui.theme.SinaColors
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

private val eightDigits = Regex("""(?<!\d)(\d{4})\s?(\d{4})(?!\d)""")

fun extractPairingCode(raw: String?): String? = raw?.let { eightDigits.find(it) }?.let { it.groupValues[1] + it.groupValues[2] }

@Composable
fun QrScanner(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnCode by rememberUpdatedState(onCode)
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED, ContextCompat.getMainExecutor(context)) { result ->
                    result.getValue(scanner)?.firstNotNullOfOrNull { extractPairingCode(it.rawValue) }?.let { latestOnCode(it) }
                },
            )
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            scanner.close()
        }
    }
    Box(modifier.semantics { contentDescription = "Câmera para escanear o código" }) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } },
            modifier = Modifier.fillMaxSize(),
        )
        ViewfinderCorners(Modifier.fillMaxSize().padding(24.dp))
    }
}

@Composable
fun ViewfinderCorners(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val k = size.minDimension / 240f
        val inset = 2f * k
        val len = 40f * k
        val r = 16f * k
        val stroke = Stroke(width = 4f * k)
        val corner = Path().apply {
            moveTo(inset, len); lineTo(inset, r + inset)
            quadraticTo(inset, inset, r + inset, inset); lineTo(len, inset)
        }
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            rotate(angle, pivot = center) { drawPath(corner, SinaColors.Orange, style = stroke) }
        }
    }
}
