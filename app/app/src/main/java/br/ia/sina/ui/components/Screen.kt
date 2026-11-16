package br.ia.sina.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.ui.unit.sp
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes

object SinaText {
    private val base = TextStyle(fontWeight = FontWeight.Normal)
    fun hero(size: Int = 36, line: Int = 42) = base.copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight.Medium)
    val TopBar = base.copy(fontSize = 22.sp, lineHeight = 28.sp)
    val Brand = base.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    val HeroBody = base.copy(fontSize = 16.sp, lineHeight = 24.sp)
    val HeroBodyLarge = base.copy(fontSize = 19.sp, lineHeight = 28.sp)
    val Button = base.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)
    val Section = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
    val Chip = base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    fun body(size: Int, line: Int = (size * 1.4f).toInt(), weight: FontWeight = FontWeight.Normal) =
        base.copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
    fun medium(size: Int, line: TextUnit = TextUnit.Unspecified) = base.copy(fontSize = size.sp, lineHeight = line, fontWeight = FontWeight.Medium)
}

@Composable
fun DarkPage(
    modifier: Modifier = Modifier,
    bottomNav: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(SinaColors.Dark)) {
        Column(Modifier.weight(1f).fillMaxWidth().statusBarsPadding(), content = content)
        if (bottomNav != null) {
            Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) { bottomNav() }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, close: Boolean = false, bottomPadding: Dp = 0.dp) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = bottomPadding).height(64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).semantics { contentDescription = if (close) "Fechar" else "Voltar" }) {
            LineIcon(if (close) LineIcons.Close else LineIcons.Back, Color.White)
        }
        Text(title, style = SinaText.TopBar, color = Color.White, modifier = Modifier.semantics { heading() })
    }
}

@Composable
fun BrandBar(onSettings: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 24.dp, end = if (onSettings != null) 4.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SinaLogo(36.dp, withBackground = false)
            Text("Sina", style = SinaText.Brand, color = Color.White)
        }
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp).semantics { contentDescription = "Ajustes" }) {
                LineIcon(LineIcons.Gear, Color.White)
            }
        }
    }
}

@Composable
fun HeroTitle(text: String, size: Int = 36, line: Int = 42, modifier: Modifier = Modifier) {
    Text(text, style = SinaText.hero(size, line), color = Color.White, modifier = modifier.semantics { heading() })
}

@Composable
fun HeroText(text: String, modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text,
        style = if (large) SinaText.HeroBodyLarge else SinaText.HeroBody,
        color = if (large) SinaColors.OnDarkSubtitleStrong else SinaColors.OnDarkSubtitle,
        modifier = modifier,
    )
}

@Composable
fun ColumnScope.Sheet(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    scroll: Boolean = true,
    hasBottomNav: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .weight(1f)
            .fillMaxWidth()
            .background(SinaColors.Sheet, SinaShapes.Sheet)
            .then(if (hasBottomNav) Modifier else Modifier.navigationBarsPadding())
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(padding)
            .then(if (hasBottomNav) Modifier else Modifier.padding(bottom = 24.dp)),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
fun ColumnScope.PushDown() = Spacer(Modifier.weight(1f))

@Composable
fun DarkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: LineIcons? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SinaShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = SinaColors.Dark,
            contentColor = Color.White,
            disabledContainerColor = SinaColors.Dark.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.height(56.dp),
    ) {
        if (icon != null) {
            LineIcon(icon, SinaColors.Orange, strokeWidth = if (icon == LineIcons.PersonAdd || icon == LineIcons.Phone) 2.2f else 2.4f)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = SinaText.Button)
    }
}

@Composable
fun OutlineDarkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = SinaShapes.Button,
        border = BorderStroke(2.dp, SinaColors.Dark),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SinaColors.Dark),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = SinaText.Button)
    }
}

@Composable
fun PlainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = SinaColors.TextPrimary, size: Int = 16) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text, style = SinaText.medium(size))
    }
}

fun dialPhone(context: android.content.Context, phone: String) {
    context.startActivity(
        android.content.Intent(android.content.Intent.ACTION_DIAL, "tel:$phone".toUri())
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Composable
fun SheetSection(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = SinaText.Section, color = SinaColors.TextSecondary, modifier = modifier.semantics { heading() })
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(SinaColors.CardBorder))
}

@Composable
fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: LineIcons? = null,
    iconStroke: Float = 2.2f,
    iconSize: Dp = 18.dp,
    dot: Color? = null,
    border: Color? = null,
) {
    Row(
        modifier
            .height(32.dp)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier.background(container, CircleShape))
            .padding(start = if (icon != null) 8.dp else 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (icon != null) 8.dp else 6.dp),
    ) {
        if (icon != null) LineIcon(icon, content, size = iconSize, strokeWidth = iconStroke)
        if (dot != null) Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text(text, style = SinaText.Chip, color = content)
    }
}

@Composable
fun CircleBadge(icon: LineIcons, container: Color, tint: Color, size: Dp = 40.dp, iconSize: Dp = 22.dp, stroke: Float = 2f, accent: Pair<String, Color>? = null) {
    Box(Modifier.size(size).background(container, CircleShape), contentAlignment = Alignment.Center) {
        LineIcon(icon, tint, size = iconSize, strokeWidth = stroke, accent = accent)
    }
}

@Composable
fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White, SinaShapes.Card)
            .border(1.dp, SinaColors.CardBorder, SinaShapes.Card),
        content = content,
    )
}
