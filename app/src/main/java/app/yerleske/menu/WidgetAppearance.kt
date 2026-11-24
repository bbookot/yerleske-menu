package app.yerleske.menu

enum class WidgetPalette(val label: String, val card: Int, val light: Int, val dark: Int, val darkInk: Int, val star: Int) {
    BLUE("Kart mavisi", 0xFF3467D9.toInt(), 0xFFEAF1FF.toInt(), 0xFF142A51.toInt(), 0xFF152B52.toInt(), 0xFF234D96.toInt()),
    NAVY("Lacivert", 0xFF244877.toInt(), 0xFFECF0F7.toInt(), 0xFF121F35.toInt(), 0xFF1C314F.toInt(), 0xFF122A4B.toInt()),
    GREEN("Yeşil", 0xFF24634E.toInt(), 0xFFE7F3EB.toInt(), 0xFF12352B.toInt(), 0xFF163B2D.toInt(), 0xFF103E31.toInt()),
    BURGUNDY("Bordo", 0xFF823B51.toInt(), 0xFFF9EBEF.toInt(), 0xFF391A27.toInt(), 0xFF572435.toInt(), 0xFF552236.toInt()),
    PURPLE("Mor", 0xFF69509D.toInt(), 0xFFF1ECFA.toInt(), 0xFF2B203F.toInt(), 0xFF3E2A60.toInt(), 0xFF44316D.toInt()),
    CHARCOAL("Antrasit", 0xFF485563.toInt(), 0xFFEDF0F3.toInt(), 0xFF20272E.toInt(), 0xFF28343F.toInt(), 0xFF283540.toInt())
}
enum class WidgetMode(val label: String) { CARD("Kart"), LIGHT("Açık"), DARK("Koyu"), SYSTEM("Sistem") }
enum class WidgetStarStyle(val label: String) { CLASSIC("Klasik"), GOLD_BRUSH("Altın fırça") }
enum class WidgetFont(val label: String, val scale: Float) { SMALL("Küçük", .9f), NORMAL("Normal boy", 1f), LARGE("Büyük", 1.2f) }
data class WidgetOptions(
    val palette: WidgetPalette = WidgetPalette.BLUE,
    val mode: WidgetMode = WidgetMode.CARD,
    val star: Int = 2,
    val starStyle: WidgetStarStyle = WidgetStarStyle.CLASSIC,
    val font: WidgetFont = WidgetFont.NORMAL,
    val vegetarian: Boolean = false,
    val hours: Boolean = true,
    val calories: Boolean = false,
    val updated: Boolean = false,
    val opacity: Int = 100,
) {
    fun normalized() = copy(star = star.coerceIn(0,3), opacity = opacity.coerceIn(50,100))
    fun colors(systemDark: Boolean): WidgetColors {
        val safe = normalized()
        val dark = mode == WidgetMode.DARK || mode == WidgetMode.SYSTEM && systemDark
        val light = mode == WidgetMode.LIGHT || mode == WidgetMode.SYSTEM && !systemDark
        val bg = if (dark) palette.dark else if (light) palette.light else palette.card
        val fg = if (light) palette.darkInk else 0xFFFFFFFF.toInt()
        val decoration = if (dark) 0xFFC3D2EB.toInt() else if (light) palette.card else palette.star
        val strength = listOf(0f,.3f,.7f,1f)[safe.star] * if (dark || light) .38f else 1f
        return WidgetColors(
            withAlpha(bg, safe.opacity / 100f), fg,
            withAlpha(fg, .82f), withAlpha(decoration, strength * safe.opacity / 100f),
            withAlpha(if (dark) 0xFFFFFFFF.toInt() else palette.darkInk, .06f * safe.opacity / 100f)
        )
    }
}
data class WidgetColors(val background: Int, val text: Int, val secondary: Int, val star: Int, val panel: Int)
fun withAlpha(color: Int, alpha: Float): Int = ((alpha.coerceIn(0f,1f) * 255).toInt() shl 24) or (color and 0x00FFFFFF)
fun widgetMealText(raw: String, calories: Boolean): String = if (calories) raw else raw.replace(Regex("\\s*\\([^)]*kcal\\)", RegexOption.IGNORE_CASE), "")
object StarShape {
    val points = listOf(345f to -48f,362f to 61f,436f to 12f,389f to 93f,501f to 110f,389f to 127f,436f to 208f,362f to 159f,345f to 268f,328f to 159f,254f to 208f,301f to 127f,189f to 110f,301f to 93f,254f to 12f,328f to 61f)
}
