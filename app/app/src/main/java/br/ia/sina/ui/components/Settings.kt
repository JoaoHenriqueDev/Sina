package br.ia.sina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.ia.sina.ui.theme.SinaColors

@Composable
fun SettingsHeader(text: String) {
    SheetSection(text, Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp))
}

@Composable
fun SettingsDivider() = Divider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

@Composable
fun SettingsItem(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, minHeight: Dp = 56.dp, titleColor: Color = SinaColors.TextPrimary) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = minHeight)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = SinaText.body(17, 23), color = titleColor)
        if (subtitle != null) Text(subtitle, style = SinaText.body(14, 20), color = SinaColors.TextSecondary)
    }
}

@Composable
fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SinaText.body(17, 23), color = SinaColors.TextPrimary)
            Text(subtitle, style = SinaText.body(14, 20), color = SinaColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onClick() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = SinaColors.Orange,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = SinaColors.CardSelected,
                uncheckedThumbColor = SinaColors.Neutral,
                uncheckedBorderColor = SinaColors.Neutral,
            ),
        )
    }
}

@Composable
fun PersonRow(name: String, subtitle: String?, actionLabel: String, onAction: () -> Unit, avatarColor: Color = SinaColors.OrangeLight, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 72.dp).padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(name, 40.dp, avatarColor, SinaColors.Dark)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = SinaText.body(17, 23), color = SinaColors.TextPrimary)
            if (subtitle != null) Text(subtitle, style = SinaText.body(14, 20), color = SinaColors.TextSecondary)
        }
        TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(actionLabel, style = SinaText.medium(15), color = SinaColors.ScamStrong)
        }
    }
}

@Composable
fun AddRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LineIcon(LineIcons.Plus, SinaColors.Dark)
        Text(text, style = SinaText.body(17, 23), color = SinaColors.TextPrimary)
    }
}

@Composable
fun Avatar(name: String, size: Dp, container: Color, content: Color) {
    Box(Modifier.size(size).background(container, CircleShape), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" }, style = SinaText.medium(if (size > 44.dp) 18 else 16), color = content)
    }
}

@Composable
fun SettingsNote(text: String) {
    Text(text, style = SinaText.body(14, 20), color = SinaColors.TextSecondary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
}
