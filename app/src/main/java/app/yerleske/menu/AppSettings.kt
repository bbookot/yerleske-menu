package app.yerleske.menu

import android.content.Context
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

enum class StartMeal(val label: String) { LAST("Son seçilen"), LUNCH("Öğle"), DINNER("Akşam") }
fun StartMeal.initialEvening(lastEvening: Boolean) = when (this) { StartMeal.LAST -> lastEvening; StartMeal.LUNCH -> false; StartMeal.DINNER -> true }
enum class AppSpacing(val label: String) { COMFORTABLE("Rahat"), COMPACT("Sıkı") }
data class AppOptions(
    val theme: Int = 0,
    val palette: WidgetPalette = WidgetPalette.GREEN,
    val font: WidgetFont = WidgetFont.NORMAL,
    val spacing: AppSpacing = AppSpacing.COMFORTABLE,
    val startMeal: StartMeal = StartMeal.LAST,
    val timedMeals: Boolean = false,
    val calories: Boolean = true,
    val hours: Boolean = true,
    val updated: Boolean = true,
)
class AppSettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private inline fun <reified E : Enum<E>> enum(key: String, default: E): E = enumValues<E>().find { it.name == prefs.getString(key, default.name) } ?: default
    fun read() = AppOptions(
        theme = MenuStore(context).theme.coerceIn(0,2),
        palette = enum("palette",WidgetPalette.GREEN), font = enum("font",WidgetFont.NORMAL),
        spacing = enum("spacing",AppSpacing.COMFORTABLE), startMeal = enum("start_meal",StartMeal.LAST),
        timedMeals = prefs.getBoolean("timed_meals",false), calories = prefs.getBoolean("calories",true),
        hours = prefs.getBoolean("hours",true), updated = prefs.getBoolean("updated",true),
    )
    fun write(value: AppOptions) {
        MenuStore(context).theme = value.theme.coerceIn(0,2)
        prefs.edit().putString("palette",value.palette.name).putString("font",value.font.name)
            .putString("spacing",value.spacing.name).putString("start_meal",value.startMeal.name)
            .putBoolean("timed_meals",value.timedMeals).putBoolean("calories",value.calories)
            .putBoolean("hours",value.hours).putBoolean("updated",value.updated).commit()
        applyApplicationNightMode(context,value.theme)
    }
}
fun applyApplicationNightMode(context: Context, theme: Int) {
    if (android.os.Build.VERSION.SDK_INT >= 31) {
        val mode = when (theme) { 1 -> android.app.UiModeManager.MODE_NIGHT_NO; 2 -> android.app.UiModeManager.MODE_NIGHT_YES; else -> android.app.UiModeManager.MODE_NIGHT_AUTO }
        context.getSystemService(android.app.UiModeManager::class.java).setApplicationNightMode(mode)
    }
}
fun AppOptions.isDark(systemDark: Boolean) = theme == 2 || theme == 0 && systemDark
fun Activity.applySavedTheme() {
    val systemDark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    setTheme(if (AppSettingsStore(this).read().isDark(systemDark)) R.style.AppTheme_Dark else R.style.AppTheme_Light)
}
@Composable fun AppSystemBars(options: AppOptions) {
    val view = LocalView.current
    val dark = options.isDark(isSystemInDarkTheme())
    SideEffect {
        val activity = view.context as? Activity
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window,view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
            @Suppress("DEPRECATION")
            window.setBackgroundDrawableResource(if (dark) R.color.app_dark else R.color.app_light)
        }
    }
}
@Composable
fun MenuTheme(options: AppOptions, content: @Composable () -> Unit) {
    val dark = options.isDark(isSystemInDarkTheme())
    val accent = if (dark) Color(options.palette.light) else Color(options.palette.card)
    val scheme = if (dark) darkColorScheme(primary = accent, onPrimary = Color(options.palette.darkInk),
        background = Color(0xFF12151B), surface = Color(0xFF12151B), surfaceContainer = Color(0xFF1B2029),
        primaryContainer = Color(options.palette.dark), onPrimaryContainer = Color(options.palette.light),
        secondaryContainer = Color(0xFF292F3A), onSecondaryContainer = Color(0xFFE8ECF3),
        onBackground = Color(0xFFE8ECF3), onSurface = Color(0xFFE8ECF3), onSurfaceVariant = Color(0xFFBCC3CF), outlineVariant = Color(0xFF3A414D))
    else lightColorScheme(primary = accent, onPrimary = Color.White, background = Color(0xFFFAFBFD), surface = Color(0xFFFAFBFD),
        surfaceContainer = Color(0xFFF0F3F8), primaryContainer = Color(options.palette.light), onPrimaryContainer = Color(options.palette.darkInk),
        secondaryContainer = Color(0xFFEDF2FA), onSecondaryContainer = Color(0xFF171C24),
        onBackground = Color(0xFF171C24), onSurface = Color(0xFF171C24), onSurfaceVariant = Color(0xFF505B69), outlineVariant = Color(0xFFD8DEE7))
    MaterialTheme(colorScheme = scheme, content = content)
}
