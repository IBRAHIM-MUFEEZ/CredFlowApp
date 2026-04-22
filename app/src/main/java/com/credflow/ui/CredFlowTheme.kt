package com.credflow.ui

import com.credflow.data.settings.AppThemeMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SignalBlue = Color(0xFF42A5F5)
private val SignalCyan = Color(0xFF58F1FF)
private val NeonMint = Color(0xFF3DFFB2)
private val WarningAmber = Color(0xFFFFB547)
private val AlertRed = Color(0xFFFF6B78)
private val DeepSpace = Color(0xFF050B14)
private val DeepSpaceAlt = Color(0xFF09121E)
private val PanelSurface = Color(0xFF0D1725)
private val PanelRaised = Color(0xFF132235)
private val Slate = Color(0xFF92A7BF)
private val SoftWhite = Color(0xFFE9F3FF)
private val CloudWhite = Color(0xFFF5F8FE)
private val IcePanel = Color(0xFFFFFFFF)
private val MistBlue = Color(0xFFE5EEF8)
private val HorizonBlue = Color(0xFFD4E5FA)
private val InkBlue = Color(0xFF11243A)
private val SteelBlue = Color(0xFF5E7085)

private val CredFlowDarkColors: ColorScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = SoftWhite,
    primaryContainer = Color(0xFF123447),
    onPrimaryContainer = SoftWhite,
    secondary = SignalBlue,
    onSecondary = SoftWhite,
    secondaryContainer = Color(0xFF132B43),
    onSecondaryContainer = SoftWhite,
    tertiary = NeonMint,
    onTertiary = SoftWhite,
    error = AlertRed,
    onError = SoftWhite,
    background = DeepSpace,
    onBackground = SoftWhite,
    surface = PanelSurface,
    onSurface = SoftWhite,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = Slate,
    outline = SignalCyan.copy(alpha = 0.26f)
)

private val CredFlowLightColors: ColorScheme = lightColorScheme(
    primary = SignalBlue,
    onPrimary = SoftWhite,
    primaryContainer = Color(0xFFD8ECFF),
    onPrimaryContainer = InkBlue,
    secondary = Color(0xFF1A69C7),
    onSecondary = SoftWhite,
    secondaryContainer = Color(0xFFDDEBFF),
    onSecondaryContainer = InkBlue,
    tertiary = Color(0xFF008E66),
    onTertiary = SoftWhite,
    error = Color(0xFFBA1A1A),
    onError = SoftWhite,
    background = CloudWhite,
    onBackground = InkBlue,
    surface = IcePanel,
    onSurface = InkBlue,
    surfaceVariant = MistBlue,
    onSurfaceVariant = SteelBlue,
    outline = SignalBlue.copy(alpha = 0.22f)
)

private val LocalCredFlowDarkTheme = staticCompositionLocalOf { true }

private val BaseTypography = Typography()
private val AppSans = FontFamily.SansSerif

private fun appStyle(
    base: TextStyle,
    weight: FontWeight,
    letterSpacing: Float = 0f
): TextStyle {
    return base.copy(
        fontFamily = AppSans,
        fontWeight = weight,
        letterSpacing = letterSpacing.sp
    )
}

private val CredFlowTypography = Typography(
    headlineLarge = appStyle(BaseTypography.headlineLarge, weight = FontWeight.Bold, letterSpacing = 0.1f),
    headlineMedium = appStyle(BaseTypography.headlineMedium, weight = FontWeight.Bold, letterSpacing = 0.08f),
    headlineSmall = appStyle(BaseTypography.headlineSmall, weight = FontWeight.Bold, letterSpacing = 0.06f),
    titleLarge = appStyle(BaseTypography.titleLarge, weight = FontWeight.Bold, letterSpacing = 0.04f),
    titleMedium = appStyle(BaseTypography.titleMedium, weight = FontWeight.Bold, letterSpacing = 0.03f),
    titleSmall = appStyle(BaseTypography.titleSmall, weight = FontWeight.Bold, letterSpacing = 0.02f),
    labelLarge = appStyle(BaseTypography.labelLarge, weight = FontWeight.Bold, letterSpacing = 0.12f),
    labelMedium = appStyle(BaseTypography.labelMedium, weight = FontWeight.SemiBold, letterSpacing = 0.08f),
    labelSmall = appStyle(BaseTypography.labelSmall, weight = FontWeight.Medium, letterSpacing = 0.04f),
    bodyLarge = appStyle(BaseTypography.bodyLarge, weight = FontWeight.Light, letterSpacing = 0f),
    bodyMedium = appStyle(BaseTypography.bodyMedium, weight = FontWeight.Light, letterSpacing = 0f),
    bodySmall = appStyle(BaseTypography.bodySmall, weight = FontWeight.Light, letterSpacing = 0f)
)

@Composable
fun CredFlowTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = themeMode == AppThemeMode.DARK

    CompositionLocalProvider(LocalCredFlowDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) CredFlowDarkColors else CredFlowLightColors,
            typography = CredFlowTypography,
            content = content
        )
    }
}

@Composable
fun CredFlowBackground(content: @Composable () -> Unit) {
    val useDarkTheme = LocalCredFlowDarkTheme.current
    val ambientColor = if (useDarkTheme) DeepSpaceAlt else HorizonBlue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (useDarkTheme) 0.18f else 0.08f),
                        ambientColor,
                        MaterialTheme.colorScheme.background
                    ),
                    center = Offset(220f, 120f),
                    radius = 1400f
                )
            )
    ) {
        TechBackdrop()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            content = content
        )
    }
}

@Composable
private fun TechBackdrop() {
    val useDarkTheme = LocalCredFlowDarkTheme.current
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = if (useDarkTheme) 0.08f else 0.05f)
    val pulseColor = MaterialTheme.colorScheme.tertiary.copy(alpha = if (useDarkTheme) 0.12f else 0.08f)
    val orbitColor = MaterialTheme.colorScheme.secondary.copy(alpha = if (useDarkTheme) 0.1f else 0.07f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = 44.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += cellSize
        }

        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += cellSize
        }

        drawCircle(
            color = pulseColor,
            radius = size.minDimension * 0.26f,
            center = Offset(size.width * 0.82f, size.height * 0.18f),
            blendMode = BlendMode.Screen
        )

        drawCircle(
            color = orbitColor,
            radius = size.minDimension * 0.18f,
            center = Offset(size.width * 0.14f, size.height * 0.84f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun AdaptiveHeaderRow(
    modifier: Modifier = Modifier,
    breakpoint: Dp = 420.dp,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val stackVertically = trailing == null || maxWidth < breakpoint

        if (stackVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                leading()
                if (trailing != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        trailing()
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    leading()
                }
                trailing()
            }
        }
    }
}

@Composable
fun ResponsiveTwoPane(
    modifier: Modifier = Modifier,
    breakpoint: Dp = 420.dp,
    spacing: Dp = 10.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < breakpoint) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    AdaptiveHeaderRow(
        modifier = modifier,
        leading = {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailing = trailing
    )
}

@Composable
fun FlowCard(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.5f),
                                accentColor,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
            )
            Column(modifier = Modifier.padding(18.dp)) {
                content()
            }
        }
    }
}

@Composable
fun HeroPanel(
    title: String,
    amount: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 450),
        label = "hero-scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp)
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .defaultMinSize(minHeight = 68.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun AccentValueRow(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DividerSpacer(height: Dp = 14.dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatMoney(value: Double): String {
    return "₹${String.format("%.2f", value)}"
}

fun accountAccent(accountKind: com.credflow.data.models.AccountKind): Color {
    return when (accountKind) {
        com.credflow.data.models.AccountKind.BANK_ACCOUNT -> SignalBlue
        com.credflow.data.models.AccountKind.CREDIT_CARD -> NeonMint
    }
}

fun warningColor(): Color = WarningAmber

fun dangerColor(): Color = AlertRed
