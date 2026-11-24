package app.yerleske.menu

import android.content.Context

class WidgetSettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
    private inline fun <reified E : Enum<E>> enum(key: String, default: E): E = enumValues<E>().find { it.name == prefs.getString(key, default.name) } ?: default
    fun read(): WidgetOptions = WidgetOptions(
        palette = enum("palette", WidgetPalette.BLUE),
        mode = enum("mode", WidgetMode.CARD),
        star = prefs.getInt("star", 2),
        starStyle = enum("star_style", WidgetStarStyle.CLASSIC),
        font = enum("font", WidgetFont.NORMAL),
        vegetarian = MenuStore(context).vegetarian,
        hours = prefs.getBoolean("hours", true),
        calories = prefs.getBoolean("calories", false),
        updated = prefs.getBoolean("updated", false),
        opacity = prefs.getInt("opacity", 100),
    ).normalized()
    fun write(options: WidgetOptions) {
        val value = options.normalized()
        prefs.edit().putString("palette",value.palette.name).putString("mode",value.mode.name)
            .putInt("star",value.star).putString("font",value.font.name)
            .putString("star_style",value.starStyle.name)
            .putBoolean("hours",value.hours).putBoolean("calories",value.calories)
            .putBoolean("updated",value.updated).putInt("opacity",value.opacity).apply()
        MenuStore(context).vegetarian = value.vegetarian
    }
}
