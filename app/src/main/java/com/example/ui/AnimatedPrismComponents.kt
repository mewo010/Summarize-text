package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberSpark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrismViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas-rendered animated geometric prism & light-beam condensation icon.
 * Used on both the launch loading screen and the active AI summarization loading card.
 */
@Composable
fun AnimatedPrismLoadingIcon(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "prism_loading")

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val counterAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val beamPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beam_phase"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .testTag("animated_prism_loading_icon")
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = this.size.minDimension / 2f

        // 1. Ambient outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ElectricCyan.copy(alpha = 0.28f * pulseScale),
                    PrismViolet.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius
            ),
            radius = maxRadius,
            center = center
        )

        // 2. Outer rotating segmented ring (representing long complex sentences)
        rotate(degrees = orbitAngle, pivot = center) {
            val ringRadius = maxRadius * 0.78f
            for (i in 0 until 6) {
                val startAngle = i * 60f + 8f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(ElectricCyan, PrismViolet, AmberSpark, ElectricCyan)
                    ),
                    startAngle = startAngle,
                    sweepAngle = 36f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = maxRadius * 0.065f, cap = StrokeCap.Round)
                )
            }
        }

        // 3. Inner counter-rotating hexagon ring (representing distillation)
        rotate(degrees = counterAngle, pivot = center) {
            val innerRing = maxRadius * 0.56f
            val hexPath = Path()
            for (i in 0 until 6) {
                val rad = Math.toRadians((i * 60.0))
                val x = center.x + innerRing * cos(rad).toFloat()
                val y = center.y + innerRing * sin(rad).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()
            drawPath(
                path = hexPath,
                color = PrismViolet.copy(alpha = 0.55f),
                style = Stroke(width = maxRadius * 0.04f)
            )
        }

        // 4. Converging light particles flowing inward toward the prism core
        for (i in 0 until 3) {
            val angleRad = Math.toRadians((i * 120.0 + orbitAngle * 0.5))
            val dist = maxRadius * (0.75f - 0.45f * beamPhase)
            val px = center.x + dist * cos(angleRad).toFloat()
            val py = center.y + dist * sin(angleRad).toFloat()
            drawCircle(
                color = if (i % 2 == 0) ElectricCyan else AmberSpark,
                radius = maxRadius * 0.06f * (1f - 0.3f * beamPhase),
                center = Offset(px, py)
            )
        }

        // 5. Central Crystalline Prism Triangle + Core Spark
        val prismRadius = maxRadius * 0.34f * pulseScale
        val prismPath = Path().apply {
            moveTo(center.x, center.y - prismRadius)
            lineTo(center.x + prismRadius * 0.9f, center.y + prismRadius * 0.72f)
            lineTo(center.x - prismRadius * 0.9f, center.y + prismRadius * 0.72f)
            close()
        }

        drawPath(
            path = prismPath,
            brush = Brush.linearGradient(
                colors = listOf(ElectricCyan, PrismViolet, AmberSpark),
                start = Offset(center.x - prismRadius, center.y - prismRadius),
                end = Offset(center.x + prismRadius, center.y + prismRadius)
            )
        )

        drawPath(
            path = prismPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = maxRadius * 0.045f, cap = StrokeCap.Round)
        )

        // Bright center spark
        drawCircle(
            color = Color.White,
            radius = maxRadius * 0.075f * pulseScale,
            center = Offset(center.x, center.y + prismRadius * 0.1f)
        )
    }
}

/**
 * Full-screen animated launch splash experience with custom prism icon, brand identity,
 * and an instant skip button for fast navigation.
 */
@Composable
fun AnimatedLaunchSplashScreen(
    onSkipSplash: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBg,
                        Color(0xFF0F172A),
                        Color(0xFF171332),
                        ObsidianBg
                    )
                )
            )
            .testTag("launch_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedPrismLoadingIcon(size = 132.dp)

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = ElectricCyan.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = ElectricCyan.copy(alpha = 0.35f),
                    shape = CircleShape
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI SENTENCE & TEXT DISTILLER",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onSkipSplash,
                modifier = Modifier.testTag("skip_splash_button")
            ) {
                Text(
                    text = "Enter Workspace →",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
        }
    }
}

/**
 * Interactive animated loading card displayed while summarizing long sentences or passages.
 */
@Composable
fun SummarizingAnimatedStatusCard(
    stepIndex: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "summary_progress"
    )

    val steps = listOf(
        stringResource(R.string.loading_step_1),
        stringResource(R.string.loading_step_2),
        stringResource(R.string.loading_step_3)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summarizing_loading_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(ElectricCyan, PrismViolet, AmberSpark)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedPrismLoadingIcon(size = 84.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.loading_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = steps.getOrElse(stepIndex) { steps.last() },
                label = "step_text_transition"
            ) { currentStepText ->
                Text(
                    text = currentStepText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = ElectricCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StagePill(
                    title = "1. Parse",
                    isDone = stepIndex >= 1,
                    isActive = stepIndex == 0
                )
                StagePill(
                    title = "2. Distill",
                    isDone = stepIndex >= 2,
                    isActive = stepIndex == 1
                )
                StagePill(
                    title = "3. Polish",
                    isDone = progress >= 0.99f,
                    isActive = stepIndex == 2
                )
            }
        }
    }
}

@Composable
private fun StagePill(
    title: String,
    isDone: Boolean,
    isActive: Boolean
) {
    val tint = when {
        isDone -> EmeraldMint
        isActive -> ElectricCyan
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Bolt,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal
        )
    }
}
