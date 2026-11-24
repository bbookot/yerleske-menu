package app.yerleske.menu

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

/** Static, small decoration shared by the settings preview and the real widget. */
fun goldStarBitmap(alpha: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(360, 220, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val path = Path().apply {
        StarShape.points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        close()
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.5f
        strokeJoin = Paint.Join.MITER
        shader = LinearGradient(190f, 0f, 360f, 220f,
            intArrayOf(0xFF96701F.toInt(), 0xFFB18A27.toInt(), 0xFFDDB957.toInt(), 0xFFAD8224.toInt(), 0xFFD4AA43.toInt(), 0xFF96701F.toInt()),
            floatArrayOf(0f, .22f, .38f, .5f, .72f, 1f), Shader.TileMode.CLAMP)
    }
    val layer = canvas.saveLayer(0f, 0f, 360f, 220f, Paint().apply { this.alpha = alpha.coerceIn(0,255) })
    canvas.drawPath(path, paint)
    // Clip fine dry-brush strands to the existing outline; nothing covers the menu.
    val outline = Path()
    paint.getFillPath(path, outline)
    canvas.save()
    canvas.clipPath(outline)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    for (i in 0..90) {
        paint.color = if (i % 3 == 0) 0x55FFE49A else 0x28734F12
        paint.strokeWidth = if (i % 4 == 0) .35f else .18f
        val y = i * 3.7f - 90f
        canvas.drawLine(180f, y, 370f, y + 105f, paint)
    }
    canvas.restore()
    canvas.restoreToCount(layer)
    return bitmap
}
