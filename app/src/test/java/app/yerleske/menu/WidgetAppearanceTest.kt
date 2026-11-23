package app.yerleske.menu

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class WidgetAppearanceTest {
    private fun luminance(argb: Int): Double {
        val rgb = listOf(argb shr 16 and 255, argb shr 8 and 255, argb and 255).map { component ->
            val value = component / 255.0
            if (value <= .04045) value / 12.92 else ((value+.055)/1.055).pow(2.4)
        }
        return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722
    }
    @Test fun everyOpaquePaletteHasReadableTextInEveryMode() {
        for (palette in WidgetPalette.entries) for (mode in WidgetMode.entries) for (dark in listOf(false,true)) {
            val colors = WidgetOptions(palette = palette,mode = mode).colors(dark)
            val l1 = luminance(colors.background); val l2 = luminance(colors.text)
            val ratio = (maxOf(l1,l2)+.05)/(minOf(l1,l2)+.05)
            assertTrue("$palette $mode dark=$dark contrast=$ratio",ratio >= 4.5)
        }
    }
    @Test fun systemModeFollowsNightWhileExplicitModesStayFixed() {
        assertNotEquals(WidgetOptions(mode = WidgetMode.SYSTEM).colors(false),WidgetOptions(mode = WidgetMode.SYSTEM).colors(true))
        for (mode in listOf(WidgetMode.CARD,WidgetMode.LIGHT,WidgetMode.DARK)) assertEquals(WidgetOptions(mode = mode).colors(false),WidgetOptions(mode = mode).colors(true))
    }
    @Test fun transparencyChangesBackgroundAndNotText() {
        val opaque = WidgetOptions().colors(false)
        val transparent = WidgetOptions(opacity = 50).colors(false)
        assertEquals(255,opaque.background ushr 24)
        assertEquals(127,transparent.background ushr 24)
        assertEquals(255,transparent.text ushr 24)
        assertEquals(opaque.text,transparent.text)
    }
    @Test fun starCanBeHiddenAndIntensityIsOrdered() {
        val alphas = (0..3).map { WidgetOptions(star = it).colors(false).star ushr 24 }
        assertEquals(0,alphas.first())
        assertEquals(alphas.sorted().distinct(),alphas)
        assertEquals(3,WidgetOptions(star = 999).normalized().star)
        assertEquals(50,WidgetOptions(opacity = -10).normalized().opacity)
    }
    @Test fun calorieVisibilityPreservesOtherMealDetails() {
        val meal = "TAVUK BAGET(el.dil.pat) (350 kcal)"
        assertEquals(meal,widgetMealText(meal,true))
        assertEquals("TAVUK BAGET(el.dil.pat)",widgetMealText(meal,false))
        assertEquals("SALATA",widgetMealText("SALATA",false))
    }
}
