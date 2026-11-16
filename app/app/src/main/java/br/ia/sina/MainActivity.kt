package br.ia.sina

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import br.ia.sina.protection.AlertNotifier
import br.ia.sina.ui.navigation.SinaNavHost
import br.ia.sina.ui.theme.SinaTheme

class MainActivity : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingRoute.value = intent.getStringExtra(AlertNotifier.EXTRA_ROUTE)
        setContent {
            SinaTheme {
                SinaNavHost(pendingRoute = pendingRoute.value, onRouteConsumed = { pendingRoute.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(AlertNotifier.EXTRA_ROUTE)?.let { pendingRoute.value = it }
    }
}
