package app.yerleske.menu

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

private val deltaKey = ActionParameters.Key<Int>("delta")
private val revisionKey = longPreferencesKey("revision")
suspend fun redrawWidgets(context: Context, only: GlanceId? = null) {
    val ids = only?.let { listOf(it) } ?: GlanceAppWidgetManager(context).getGlanceIds(MenuWidget::class.java)
    for (id in ids) {
        updateAppWidgetState(context, id) { it[revisionKey] = System.nanoTime() }
        MenuWidget().update(context, id)
    }
}
class MenuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MenuWidget()
    // Replace the parent's goAsync update with state invalidation and widget.update.
    @android.annotation.SuppressLint("MissingSuperCall")
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch { try { redrawWidgets(context); scheduleWidgetClock(context) } finally { pending.finish() } }
    }
}
private fun themed(options: WidgetOptions, color: (WidgetColors) -> Int): ColorProvider =
    DayNightColorProvider(Color(color(options.colors(false))), Color(color(options.colors(true))))

class MenuWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp,180.dp), DpSize(280.dp,200.dp), DpSize(320.dp,280.dp)))
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            key(currentState<Preferences>()[revisionKey]) {
                val store = MenuStore(context)
                val options = WidgetSettingsStore(context).read()
                val ink = themed(options) { it.text }
                val muted = themed(options) { it.secondary }
                val base = WidgetPlanner.plan(ZonedDateTime.now(turkey)).base
                val plan = WidgetPlanner.plan(ZonedDateTime.now(turkey), store.widgetOffset(id.toString(), base))
                val month = YearMonth.from(plan.date)
                val day = store.days(month).find { it.date == plan.date }
                val suffix = if (options.vegetarian) "2_menu" else "_menu"
                val lunch = day?.meals?.get("lunch$suffix").orEmpty()
                val dinner = day?.meals?.get("dinner$suffix").orEmpty()
                val updated = store.updated(month)
                val compact = LocalSize.current.height < 250.dp
                Box(GlanceModifier.fillMaxSize().background(themed(options) { it.background }).appWidgetBackground().cornerRadius(20.dp)) {
                    if (options.star > 0) Image(ImageProvider(R.drawable.widget_star_mask), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(themed(options) { it.star }))
                    Column(GlanceModifier.fillMaxSize().padding(if (compact) 10.dp else 16.dp)) {
                        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                                Text("YTÜ · YEMEK · " + if (options.vegetarian) "VEJ" else "NORMAL", style = TextStyle(color = ink, fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 12.sp), maxLines = 1)
                                Text(dateLabel(plan.date), style = TextStyle(color = ink, fontWeight = FontWeight.Bold, fontSize = ((if (compact) 13 else 17)*options.font.scale).sp), maxLines = 1)
                            }
                            Text("↻", GlanceModifier.padding(if (compact) 4.dp else 8.dp).clickable(actionRunCallback<RefreshMenu>()), style = TextStyle(color = ink, fontSize = 23.sp))
                        }
                        plan.notice?.let { Text(it, style = TextStyle(color = ink, fontSize = 10.sp), maxLines = 2) }
                        ServiceCalendar.calendarWarning(plan.date)?.let { Text(it, style = TextStyle(color = ink, fontSize = 10.sp), maxLines = 2) }
                        Spacer(GlanceModifier.height(if (compact) 4.dp else 8.dp))
                        if (ServiceCalendar.reason(plan.date) == null) {
                            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                                if (plan.lunch) {
                                    MealPanel("ÖĞLE", "11.30 – 14.30", lunch, compact, options, GlanceModifier.defaultWeight())
                                    if (plan.dinner) Spacer(GlanceModifier.width(8.dp))
                                }
                                if (plan.dinner) MealPanel("AKŞAM", "17.00 – 19.15", dinner, compact, options, GlanceModifier.defaultWeight())
                            }
                        } else Text("Bu gün yemek hizmeti verilmiyor.", GlanceModifier.defaultWeight(), style = TextStyle(color = ink, fontSize = 14.sp))
                        Spacer(GlanceModifier.height(4.dp))
                        Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (plan.offset == 0) "Bugün" else "+${plan.offset} gün", GlanceModifier.padding(horizontal = 12.dp, vertical = 8.dp).clickable(actionRunCallback<NavigateMenu>(actionParametersOf(deltaKey to 0))), style = TextStyle(color = ink, fontSize = 11.sp, textAlign = TextAlign.Center))
                            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Nav("‹", plan.offset > 0, -1, ink, muted)
                                Spacer(GlanceModifier.defaultWeight())
                                Nav("›", plan.offset < 3, 1, ink, muted)
                            }
                        }
                        if (options.updated) {
                            val stale = updated == 0L || System.currentTimeMillis() - updated > 24*60*60*1000L
                            Text(if (updated == 0L) "Menü henüz alınmadı · ↻ ile yenile" else (if (stale) "Eski veri · " else "Son kontrol · ") + Instant.ofEpochMilli(updated).atZone(turkey).format(DateTimeFormatter.ofPattern("d MMM HH:mm", turkish)), style = TextStyle(color = muted, fontSize = 9.sp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun MealPanel(title: String, hours: String, meals: List<String>, compact: Boolean, options: WidgetOptions, modifier: GlanceModifier) {
    val ink = themed(options) { it.text }
    Column(modifier.fillMaxHeight().background(themed(options) { it.panel }).cornerRadius(14.dp).padding(if (compact) 8.dp else 12.dp)) {
        Text(title, GlanceModifier.clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = ink, fontWeight = FontWeight.Bold, fontSize = (11*options.font.scale).sp))
        if (options.hours) Text(hours, style = TextStyle(color = themed(options) { it.secondary }, fontSize = 9.sp))
        Spacer(GlanceModifier.height(4.dp))
        val lines = meals.ifEmpty { listOf("Menü henüz yayımlanmadı") }
        LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
            itemsIndexed(lines, itemId = { index, _ -> index.toLong() }) { _, line ->
                Text(widgetMealText(line,options.calories), GlanceModifier.fillMaxWidth().padding(vertical = 1.dp).clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = ink, fontSize = ((if (compact) 10 else 12)*options.font.scale).sp))
            }
        }
    }
}
@Composable
private fun Nav(label: String, enabled: Boolean, delta: Int, ink: ColorProvider, muted: ColorProvider) {
    val modifier = GlanceModifier.padding(horizontal = 12.dp, vertical = 4.dp)
    Text(label, if (enabled) modifier.clickable(actionRunCallback<NavigateMenu>(actionParametersOf(deltaKey to delta))) else modifier, style = TextStyle(color = if (enabled) ink else muted, fontSize = 23.sp))
}
class RefreshMenu : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { MenuStore(context).refreshWeek() }
        redrawWidgets(context)
        scheduleWidgetClock(context)
    }
}
class NavigateMenu : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val store = MenuStore(context)
        val base = WidgetPlanner.plan(ZonedDateTime.now(turkey)).base
        val delta = parameters[deltaKey] ?: 0
        val offset = if (delta == 0) 0 else store.widgetOffset(glanceId.toString(), base) + delta
        store.setWidgetOffset(glanceId.toString(), base, offset)
        redrawWidgets(context, glanceId)
    }
}
