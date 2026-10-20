package com.goals.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Full-screen app background: the plain theme background, or frost and embers for Fire & Ice. */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    if (!GoalsTheme.style.fireIce) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val style = GoalsTheme.style
    // Fixed seeds keep the decoration identical across recompositions and launches.
    val frost = remember { frostBranches(Random(3)) }
    val embers = remember { emberSparks(Random(11)) }
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E1B2C), Color(0xFF0A111C), Color(0xFF0D0F16))))
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF2E6FC4).copy(alpha = 0.28f), Color.Transparent),
                center = Offset(0f, 0f), radius = size.width * 0.8f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFFFF4A10).copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width, size.height * 0.85f), radius = size.width * 0.75f,
            ),
        )
        drawFrost(frost, style.frost)
        drawEmbers(embers, style.fire, style.ember)
    }
}

/** A frost crystal segment in unit coordinates (fractions of the screen width). */
private class Branch(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val alpha: Float)

private fun frostBranches(rnd: Random): List<Branch> {
    val out = ArrayList<Branch>()
    fun grow(x: Float, y: Float, angle: Float, length: Float, depth: Int) {
        if (depth == 0 || length < 0.01f) return
        val x2 = x + cos(angle) * length
        val y2 = y + sin(angle) * length
        out += Branch(x, y, x2, y2, 0.05f + 0.035f * depth)
        // Side shoots at roughly ±60°, like ice dendrites.
        val shoots = 2 + rnd.nextInt(2)
        repeat(shoots) { i ->
            val t = (i + 1f) / (shoots + 1f)
            val bx = x + (x2 - x) * t
            val by = y + (y2 - y) * t
            val side = if (i % 2 == 0) 1f else -1f
            grow(bx, by, angle + side * (0.9f + rnd.nextFloat() * 0.3f), length * 0.38f, depth - 1)
        }
        grow(x2, y2, angle + (rnd.nextFloat() - 0.5f) * 0.4f, length * 0.6f, depth - 1)
    }
    // Crystals creep in from the top-left corner and along the top edge.
    grow(-0.02f, 0.02f, 0.75f, 0.22f, 4)
    grow(0.0f, 0.18f, 0.25f, 0.16f, 3)
    grow(0.22f, -0.01f, 1.3f, 0.12f, 3)
    grow(0.40f, -0.01f, 1.9f, 0.07f, 2)
    return out
}

private fun DrawScope.drawFrost(branches: List<Branch>, color: Color) {
    val w = size.width
    for (b in branches) {
        drawLine(
            color.copy(alpha = b.alpha),
            Offset(b.x1 * w, b.y1 * w), Offset(b.x2 * w, b.y2 * w),
            strokeWidth = 0.9f * density, cap = StrokeCap.Round,
        )
    }
}

private class Spark(val x: Float, val y: Float, val radius: Float, val alpha: Float, val hot: Boolean, val trail: Float)

private fun emberSparks(rnd: Random): List<Spark> = List(46) {
    // Concentrated toward the bottom-right corner.
    val x = 1f - rnd.nextFloat() * rnd.nextFloat() * 0.75f
    val y = 1f - rnd.nextFloat() * rnd.nextFloat() * 0.6f
    Spark(x, y, 0.6f + rnd.nextFloat() * 1.6f, 0.35f + rnd.nextFloat() * 0.6f, rnd.nextBoolean(), if (rnd.nextFloat() < 0.35f) 4f + rnd.nextFloat() * 8f else 0f)
}

private fun DrawScope.drawEmbers(sparks: List<Spark>, fire: Color, ember: Color) {
    for (s in sparks) {
        val c = (if (s.hot) ember else fire).copy(alpha = s.alpha)
        val center = Offset(s.x * size.width, s.y * size.height)
        if (s.trail > 0f) {
            // Short upward streak, as if drifting on heat.
            drawLine(c, center, center + Offset(-s.trail * 0.3f * density, -s.trail * density), s.radius * density, cap = StrokeCap.Round)
        } else {
            drawCircle(c.copy(alpha = s.alpha * 0.35f), s.radius * 2.6f * density, center)
            drawCircle(c, s.radius * density, center)
        }
    }
}
