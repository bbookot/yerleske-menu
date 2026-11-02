package app.yerleske.menu

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

val turkey: ZoneId = ZoneId.of("Europe/Istanbul")
val turkish: Locale = Locale.forLanguageTag("tr-TR")
fun today(): LocalDate = LocalDate.now(turkey)
fun dateLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", turkish))
data class MenuDay(val date: LocalDate, val meals: Map<String, List<String>>)

object MenuParser {
    fun lines(html: String): List<String> {
        val doc = Jsoup.parseBodyFragment(html)
        doc.select("br").after("\n")
        doc.select("p,li,div").forEach { it.appendText("\n") }
        return doc.body().wholeText().split('\n').map { it.replace('\u00a0', ' ').trim() }.filter { it.isNotBlank() }
    }
    fun parse(raw: String): List<MenuDay> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val date = runCatching { LocalDate.parse(obj.getString("date"), DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.getOrNull()
            date?.let { MenuDay(it, listOf("lunch_menu", "lunch2_menu", "dinner_menu", "dinner2_menu").associateWith { key -> lines(obj.optString(key, "")) }) }
        }.distinctBy { it.date }.sortedBy { it.date }
    }
}

class MenuStore(context: Context) {
    private val prefs = context.getSharedPreferences("menu", Context.MODE_PRIVATE)
    var evening: Boolean
        get() = prefs.getBoolean("evening", false)
        set(value) { prefs.edit().putBoolean("evening", value).apply() }
    var vegetarian: Boolean
        get() = prefs.getBoolean("vegetarian", false)
        set(value) { prefs.edit().putBoolean("vegetarian", value).apply() }
    var theme: Int
        get() = prefs.getInt("theme", 0)
        set(value) { prefs.edit().putInt("theme", value).apply() }
    val mealKey get() = if (evening) { if (vegetarian) "dinner2_menu" else "dinner_menu" } else { if (vegetarian) "lunch2_menu" else "lunch_menu" }
    val mealLabel get() = (if (evening) "Akşam" else "Öğle") + " · " + if (vegetarian) "Vejetaryen" else "Normal"
    fun days(month: YearMonth): List<MenuDay> = runCatching { MenuParser.parse(prefs.getString("data_$month", "[]")!!) }.getOrDefault(emptyList())
    fun updated(month: YearMonth): Long = prefs.getLong("updated_$month", 0L)
    fun widgetOffset(id: String, base: LocalDate): Int = if (prefs.getString("base_$id", "") == base.toString()) prefs.getInt("offset_$id", 0).coerceIn(0,3) else 0
    fun setWidgetOffset(id: String, base: LocalDate, offset: Int) { prefs.edit().putString("base_$id", base.toString()).putInt("offset_$id", offset.coerceIn(0,3)).apply() }
    suspend fun refreshWeek(start: LocalDate = WidgetPlanner.plan(ZonedDateTime.now(turkey)).base, skipFresh: Boolean = false) {
        // Kaynak yalnızca aylık yanıt verir. Yedi günlük pencerenin bütün ayları önceden alınır.
        val months = (listOf(YearMonth.from(today())) + WidgetPlanner.weekMonths(start)).distinct()
        var error: Exception? = null
        for (month in months) {
            val age = System.currentTimeMillis() - updated(month)
            if (skipFresh && updated(month) != 0L && age in 0..6*60*60*1000L) continue
            try { refresh(month) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e }
        }
        error?.let { throw it }
    }
    suspend fun refresh(month: YearMonth) = withContext(Dispatchers.IO) {
        val formatted = String.format(Locale.ROOT, "%04d%02d", month.year, month.monthValue)
        val url = "https://sks.yildiz.edu.tr/api/food-menu?type=student_food_menu&list_date=$formatted&sort_by=date_asc"
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Menü alınamadı (HTTP ${response.code})")
            val raw = response.body?.string() ?: throw IOException("Menü yanıtı boş")
            val days = MenuParser.parse(raw)
            if (days.any { YearMonth.from(it.date) != month }) throw IOException("Menü tarihleri beklenen ayla eşleşmiyor")
            prefs.edit().putString("data_$month", raw).putLong("updated_$month", System.currentTimeMillis()).commit()
        }
        Unit
    }
    companion object {
        val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()
    }
}

class MenuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<MenuWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("menu-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        scheduleWidgetClock(this)
    }
}
class MenuWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val result = runCatching { MenuStore(applicationContext).refreshWeek() }
        redrawWidgets(applicationContext)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}

fun scheduleWidgetClock(context: Context) {
    val now = ZonedDateTime.now(turkey)
    val transition = WidgetPlanner.nextTransition(now)
    val request = OneTimeWorkRequestBuilder<WidgetClockWorker>().setInitialDelay(Duration.between(now, transition).toMillis().coerceAtLeast(1000), TimeUnit.MILLISECONDS).build()
    WorkManager.getInstance(context).enqueueUniqueWork("widget-clock-${transition.toEpochSecond()}", ExistingWorkPolicy.KEEP, request)
}
class WidgetClockWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        redrawWidgets(applicationContext)
        scheduleWidgetClock(applicationContext)
        return Result.success()
    }
}

