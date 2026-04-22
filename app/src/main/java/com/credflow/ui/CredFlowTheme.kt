package com.credflow.ui

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
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.credflow.data.settings.AppThemeMode

private val InkWashCharcoal = Color(0xFF4A4A4A)
private val InkWashCharcoalDeep = Color(0xFF353535)
private val InkWashCharcoalSoft = Color(0xFF5C5C5C)
private val InkWashMist = Color(0xFFCBCBCB)
private val InkWashCream = Color(0xFFFFFFE3)
private val InkWashBlue = Color(0xFF6D8196)
private val InkWashBlueDeep = Color(0xFF556779)
private val InkWashBlueLight = Color(0xFF96A6B6)
private val InkWashHighlight = Color(0xFFF4EFB9)
private val InkWashDanger = Color(0xFFE0A5A5)
private val InkWashCanvas = Color(0xFFF4F3ED)
private val White = Color(0xFFFFFFFF)

private val CredFlowLightColors: ColorScheme = lightColorScheme(
    primary = InkWashBlue,
    onPrimary = InkWashCream,
    primaryContainer = Color(0xFFDDE6EE),
    onPrimaryContainer = InkWashCharcoal,
    secondary = InkWashCharcoal,
    onSecondary = InkWashCream,
    secondaryContainer = Color(0xFFE7E4DD),
    onSecondaryContainer = InkWashCharcoal,
    tertiary = InkWashCream,
    onTertiary = InkWashCharcoal,
    error = InkWashDanger,
    onError = InkWashCharcoal,
    background = InkWashCanvas,
    onBackground = InkWashCharcoal,
    surface = White,
    onSurface = InkWashCharcoal,
    surfaceVariant = Color(0xFFF1EFE8),
    onSurfaceVariant = InkWashBlueDeep,
    outline = InkWashMist
)

private val CredFlowDarkColors: ColorScheme = darkColorScheme(
    primary = InkWashBlue,
    onPrimary = InkWashCream,
    primaryContainer = InkWashBlueDeep,
    onPrimaryContainer = InkWashCream,
    secondary = InkWashCream,
    onSecondary = InkWashCharcoal,
    secondaryContainer = InkWashCharcoalSoft,
    onSecondaryContainer = InkWashCream,
    tertiary = InkWashMist,
    onTertiary = InkWashCharcoal,
    error = InkWashDanger,
    onError = InkWashCharcoal,
    background = InkWashCharcoalDeep,
    onBackground = InkWashCream,
    surface = InkWashCharcoal,
    onSurface = InkWashCream,
    surfaceVariant = InkWashCharcoalSoft,
    onSurfaceVariant = InkWashMist,
    outline = InkWashBlueLight
)

private val LocalCredFlowDarkTheme = staticCompositionLocalOf { true }
private val AppSans = FontFamily.SansSerif
private val BaseTypography = Typography()

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
    headlineLarge = appStyle(BaseTypography.headlineLarge, FontWeight.Bold, 0.02f),
    headlineMedium = appStyle(BaseTypography.headlineMedium, FontWeight.Bold, 0.02f),
    headlineSmall = appStyle(BaseTypography.headlineSmall, FontWeight.SemiBold, 0.01f),
    titleLarge = appStyle(BaseTypography.titleLarge, FontWeight.SemiBold, 0.01f),
    titleMedium = appStyle(BaseTypography.titleMedium, FontWeight.SemiBold, 0.01f),
    titleSmall = appStyle(BaseTypography.titleSmall, FontWeight.Medium, 0.01f),
    bodyLarge = appStyle(BaseTypography.bodyLarge, FontWeight.Normal),
    bodyMedium = appStyle(BaseTypography.bodyMedium, FontWeight.Normal),
    bodySmall = appStyle(BaseTypography.bodySmall, FontWeight.Normal),
    labelLarge = appStyle(BaseTypography.labelLarge, FontWeight.SemiBold, 0.03f),
    labelMedium = appStyle(BaseTypography.labelMedium, FontWeight.Medium, 0.03f),
    labelSmall = appStyle(BaseTypography.labelSmall, FontWeight.Medium, 0.04f)
)

private val CredFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(18.dp),
    small = RoundedCornerShape(22.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp)
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
            shapes = CredFlowShapes,
            content = content
        )
    }
}

@Composable
fun CredFlowBackground(content: @Composable () -> Unit) {
    val useDarkTheme = LocalCredFlowDarkTheme.current
    val backgroundBrush = if (useDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2F3134),
                Color(0xFF3B3F45),
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                White,
                Color(0xFFF8F6F0),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        GlassBackdrop()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            content = content
        )
    }
}

@Composable
private fun GlassBackdrop() {
    val useDarkTheme = LocalCredFlowDarkTheme.current
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = if (useDarkTheme) 0.16f else 0.1f)
    val secondaryGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = if (useDarkTheme) 0.08f else 0.14f)
    val tertiaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = if (useDarkTheme) 0.06f else 0.08f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = primaryGlow,
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.18f, size.height * 0.1f),
            style = Fill
        )
        drawCircle(
            color = secondaryGlow,
            radius = size.minDimension * 0.24f,
            center = Offset(size.width * 0.92f, size.height * 0.18f),
            style = Fill
        )
        drawCircle(
            color = tertiaryGlow,
            radius = size.minDimension * 0.22f,
            center = Offset(size.width * 0.76f, size.height * 0.86f),
            style = Fill
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
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
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
    val useDarkTheme = LocalCredFlowDarkTheme.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useDarkTheme) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            } else {
                White.copy(alpha = 0.78f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (useDarkTheme) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.56f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (useDarkTheme) 0.13f else 0.08f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (useDarkTheme) 0.98f else 0.82f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (useDarkTheme) 0.68f else 0.52f)
                        ),
                        start = Offset.Zero,
                        end = Offset(960f, 960f)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 12.dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (useDarkTheme) 0.08f else 0.1f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
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
    val useDarkTheme = LocalCredFlowDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(
                width = 1.dp,
                color = if (useDarkTheme) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.56f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(32.dp)
            )
            .background(
                Brush.linearGradient(
                    colors = if (useDarkTheme) {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
                        )
                    } else {
                        listOf(
                            White.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f)
                        )
                    }
                )
            )
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (useDarkTheme) 0.14f else 0.12f))
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f), RoundedCornerShape(18.dp))
            .defaultMinSize(minHeight = 72.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
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
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
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
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.74f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
        com.credflow.data.models.AccountKind.BANK_ACCOUNT -> InkWashMist
        com.credflow.data.models.AccountKind.CREDIT_CARD -> InkWashBlue
    }
}

fun warningColor(): Color = InkWashHighlight

fun dangerColor(): Color = InkWashDanger
