package app.yerleske.menu

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import java.time.YearMonth
import java.time.Instant
import java.time.format.DateTimeFormatter

class MenuWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = MenuWidget() }
class MenuWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp,110.dp), DpSize(250.dp,230.dp), DpSize(300.dp,320.dp)))
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = MenuStore(context)
        val day = today()
        val month = YearMonth.from(day)
        val menu = store.days(month).find { it.date == day }?.meals?.get(store.mealKey).orEmpty()
        val updated = store.updated(month)
        provideContent {
            GlanceTheme {
                val compact = LocalSize.current.height < 200.dp
                Column(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).appWidgetBackground().cornerRadius(24.dp).padding(16.dp)) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Yerleşke Menü", GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp))
                        Text("↻", GlanceModifier.padding(8.dp).clickable(actionRunCallback<RefreshMenu>()), style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 22.sp))
                    }
                    Text(dateLabel(day) + " · " + store.mealLabel, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp), maxLines = 2)
                    Spacer(GlanceModifier.height(8.dp))
                    if (menu.isEmpty()) Text("Bu gün için menü yayımlanmamış. Ayrıntılar için dokunun.", GlanceModifier.clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp))
                    else Text(menu.joinToString(if (compact) " · " else "\n"), GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (compact) 12.sp else 14.sp), maxLines = if (compact) 2 else 12)
                    if (!compact) {
                        Spacer(GlanceModifier.height(8.dp))
                        val stale = updated == 0L || System.currentTimeMillis() - updated > 24*60*60*1000L
                        Text(if (updated == 0L) "Menüyü almak için ↻" else (if (stale) "Eski veri · " else "Son kontrol · ") + Instant.ofEpochMilli(updated).atZone(turkey).format(DateTimeFormatter.ofPattern("d MMM HH:mm", turkish)), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp))
                    }
                }
            }
        }
    }
}
class RefreshMenu : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        runCatching { MenuStore(context).refresh(YearMonth.from(today())) }
        MenuWidget().updateAll(context)
    }
}
