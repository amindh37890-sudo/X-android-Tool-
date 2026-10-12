package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive 3D Holographic Core Canvas showing real-time rotating dimensional geometry,
 * privilege core, laser vectors, and holographic telemetry depth.
 */
@Composable
fun Hologram3DCore(
    modifier: Modifier = Modifier,
    isElevated: Boolean = true,
    privilegeTitle: String = "WIRELESS ADB ONLINE",
    interactiveRotation: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hologram_3d")
    val autoAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    var manualRotationX by remember { mutableFloatStateOf(15f) }
    var manualRotationY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF050B14),
                        Color(0xFF020408)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        NexusCyan.copy(alpha = 0.6f),
                        NexusViolet.copy(alpha = 0.3f),
                        NexusCyan.copy(alpha = 0.1f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .pointerInput(interactiveRotation) {
                if (interactiveRotation) {
                    detectDragGestures { _, dragAmount ->
                        manualRotationY = (manualRotationY + dragAmount.x * 0.5f) % 360f
                        manualRotationX = (manualRotationX - dragAmount.y * 0.3f).coerceIn(-45f, 45f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val currentAngle = (autoAngle + manualRotationY) * (Math.PI / 180.0)
        val tiltAngle = manualRotationX * (Math.PI / 180.0)

        // 3D Isometric Wireframe Projection
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (size.height * 0.38f) * pulseScale

            // Draw holographic outer grid rings
            val ringCount = 4
            for (i in 1..ringCount) {
                val r = radius * (i.toFloat() / ringCount)
                drawCircle(
                    color = NexusCyan.copy(alpha = 0.12f * (i.toFloat() / ringCount)),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f)
                )
            }

            // 3D Cube / Polyhedron 8 Vertices
            val vertices = listOf(
                Triple(-1f, -1f, -1f),
                Triple(1f, -1f, -1f),
                Triple(1f, 1f, -1f),
                Triple(-1f, 1f, -1f),
                Triple(-1f, -1f, 1f),
                Triple(1f, -1f, 1f),
                Triple(1f, 1f, 1f),
                Triple(-1f, 1f, 1f)
            )

            // Project 3D to 2D screen with rotation
            val projected = vertices.map { (vx, vy, vz) ->
                val halfSize = radius * 0.62f
                val x1 = vx * halfSize
                val y1 = vy * halfSize
                val z1 = vz * halfSize

                // Rotate around Y axis
                val cosY = cos(currentAngle)
                val sinY = sin(currentAngle)
                val rx = x1 * cosY + z1 * sinY
                val rz = -x1 * sinY + z1 * cosY

                // Tilt around X axis
                val cosX = cos(tiltAngle)
                val sinX = sin(tiltAngle)
                val ry = y1 * cosX - rz * sinX

                Offset(cx + rx.toFloat(), cy + ry.toFloat())
            }

            // Edges of 3D Prism
            val edges = listOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 0, // front face
                4 to 5, 5 to 6, 6 to 7, 7 to 4, // back face
                0 to 4, 1 to 5, 2 to 6, 3 to 7  // connecting edges
            )

            for ((startIdx, endIdx) in edges) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isElevated) NexusCyan else NexusAmber,
                            NexusViolet
                        ),
                        start = projected[startIdx],
                        end = projected[endIdx]
                    ),
                    start = projected[startIdx],
                    end = projected[endIdx],
                    strokeWidth = 2.5f
                )
            }

            // Glowing nodes at vertices
            projected.forEach { p ->
                drawCircle(
                    color = NexusCyan,
                    radius = 4f * pulseScale,
                    center = p
                )
                drawCircle(
                    color = NexusCyan.copy(alpha = 0.35f),
                    radius = 8f * pulseScale,
                    center = p
                )
            }

            // Central Pulsing Power Singularity
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isElevated) NexusCyan else NexusAmber,
                        NexusViolet.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = 35f * pulseScale
                ),
                radius = 35f * pulseScale,
                center = Offset(cx, cy)
            )
        }

        // Live Holographic Overlay Badges
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x9908121E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isElevated) NexusCyan else NexusAmber, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "3D HOLO-ENGINE ACTIVE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan
                        )
                    }
                }

                Surface(
                    color = Color(0x9908121E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusViolet.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "اسحب للتدوير 360°",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexusViolet,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom Core Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CORE TELEMETRY",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NexusTextSecondary
                    )
                    Text(
                        text = privilegeTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "X: ${manualRotationX.toInt()}° | Y: ${manualRotationY.toInt()}°",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NexusCyan.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 3-Stage Interactive Introduction Showcase (مقدمة التطبيق الحية ثلاثية الأبعاد)
 */
@Composable
fun InteractiveIntroDialog(
    onDismiss: () -> Unit
) {
    var stage by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_intro_dialog"),
        color = Color(0xF2060A10)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXUS X // INTRODUCTION",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0..2) {
                        Box(
                            modifier = Modifier
                                .width(if (i == stage) 22.dp else 8.dp)
                                .height(6.dp)
                                .background(
                                    if (i == stage) NexusCyan else NexusCyan.copy(alpha = 0.25f),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stage Visuals & 3D Interactive Canvas
            when (stage) {
                0 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Hologram3DCore(
                            isElevated = true,
                            privilegeTitle = "SUPERCHARGE ROOT & ADB",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "نظام الصلاحيات القصوى الحقيقي",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تحكم مباشر فوق مستوى النظام بالكامل بدون قيود Sandbox عبر Wireless Debugging و Termux والـ Root الحقيقي.",
                            fontSize = 13.sp,
                            color = NexusTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
                1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Hologram3DCore(
                            isElevated = false,
                            privilegeTitle = "100+ DEEP HAZARDOUS FEATURES",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "100 ميزة عميقة وخطرة مع تحذيرات حية",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusAmber,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تعديلات النواة Linux، كبح الـ Doze، إلغاء خنق المعالج، وحقن المدخلات وإدارة صلاحيات التطبيقات بدفعة واحدة مع التحليل الأمني المسبق.",
                            fontSize = 13.sp,
                            color = NexusTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
                2 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Hologram3DCore(
                            isElevated = true,
                            privilegeTitle = "INTERACTIVE NOTIFICATION PAIRING",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "الإقران وتنفيذ الأوامر من الإشعارات",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "أدخل رمز الإقران (PIN) اللاسلكي مباشرة من شريط الإشعارات بدون الحاجة لفتح الشاشة، ونفّذ أوامر الشل الفورية بنقرة واحدة.",
                            fontSize = 13.sp,
                            color = NexusTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (stage > 0) {
                    Button(
                        onClick = { stage-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusSurfaceVariant,
                            contentColor = NexusTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("السابق", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (stage < 2) {
                            stage++
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusCyan,
                        contentColor = Color(0xFF001F28)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(if (stage == 0) 1f else 1.3f)
                ) {
                    Text(
                        text = if (stage < 2) "التالي ❯" else "ابدأ الاستخدام الآن ⚡",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
