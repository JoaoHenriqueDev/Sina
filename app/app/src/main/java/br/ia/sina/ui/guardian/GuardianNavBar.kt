package br.ia.sina.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.ia.sina.ui.components.Divider
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.theme.SinaColors

enum class GuardianTab(val label: String, val icon: LineIcons) {
    PEOPLE("Protegidos", LineIcons.Shield),
    ALERTS("Alertas", LineIcons.Bell),
    SETTINGS("Ajustes", LineIcons.Gear),
}

@Composable
fun GuardianNavBar(current: GuardianTab, onSelect: (GuardianTab) -> Unit, alertBadge: Int = 0) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Divider()
        Row(Modifier.fillMaxWidth().height(71.dp)) {
            GuardianTab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab, onClick = { if (!selected) onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier
                            .size(64.dp, 32.dp)
                            .then(if (selected) Modifier.background(SinaColors.OrangeLight, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        LineIcon(tab.icon, if (selected) SinaColors.Dark else SinaColors.TextSecondary)
                        if (tab == GuardianTab.ALERTS && alertBadge > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 34.dp, y = 2.dp)
                                    .heightIn(min = 16.dp)
                                    .widthIn(min = 16.dp)
                                    .background(SinaColors.ScamStrong, CircleShape)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (alertBadge > 9) "9+" else "$alertBadge", style = SinaText.medium(11), color = Color.White)
                            }
                        }
                    }
                    Text(tab.label, style = SinaText.medium(12), color = if (selected) SinaColors.TextPrimary else SinaColors.TextSecondary)
                }
            }
        }
    }
}
