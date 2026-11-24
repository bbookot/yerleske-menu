package app.yerleske.menu

import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.time.YearMonth
import java.time.Instant
import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private var updateRequest by mutableIntStateOf(0)
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { if (it.resultCode == RESULT_OK) updateRequest++ }
    override fun onCreate(savedInstanceState: Bundle?) { applySavedTheme(); super.onCreate(savedInstanceState); setContent { MenuScreen() } }
    @Composable fun MenuScreen() {
        val store = remember { MenuStore(this) }
        val settings = remember { AppSettingsStore(this) }
        val scope = rememberCoroutineScope()
        var options by remember { mutableStateOf(settings.read()) }
        var now by remember { mutableStateOf(ZonedDateTime.now(turkey)) }
        var following by remember { mutableStateOf(true) }
        var selected by remember { mutableStateOf(WidgetPlanner.plan(now).date) }
        var days by remember { mutableStateOf(store.days(YearMonth.from(selected))) }
        var evening by remember { mutableStateOf(options.startMeal.initialEvening(store.evening)) }
        var vegetarian by remember { mutableStateOf(store.vegetarian) }
        var loading by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var updated by remember { mutableLongStateOf(store.updated(YearMonth.from(selected))) }
        var release by remember { mutableStateOf<Release?>(null) }
        var checking by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(false) }
        val updater = remember { Updater(this) }
        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
                val next = settings.read()
                if (options.timedMeals != next.timedMeals) following = true
                if (options.startMeal != next.startMeal) evening = next.startMeal.initialEvening(store.evening)
                options = next; vegetarian = store.vegetarian; now = ZonedDateTime.now(turkey)
            } }
            lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(Unit) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) {
            now = ZonedDateTime.now(turkey)
            delay(Duration.between(now,WidgetPlanner.nextTransition(now)).toMillis().coerceIn(100,30_000))
        } } }
        val automatic = WidgetPlanner.plan(now,timedMeals = options.timedMeals)
        LaunchedEffect(automatic.date,options.timedMeals,following) {
            if (following) selected = automatic.date
        }
        fun returnToAutomatic() {
            val clickedNow = ZonedDateTime.now(turkey)
            now = clickedNow; following = true
            selected = WidgetPlanner.plan(clickedNow,timedMeals = options.timedMeals).date
        }
        suspend fun refresh() {
            loading = true
            try {
                store.refresh(YearMonth.from(selected)); store.refreshWeek(skipFresh = true)
                days = store.days(YearMonth.from(selected)); updated = store.updated(YearMonth.from(selected))
                message = if (days.isEmpty()) "Bu ayın menüsü henüz yayımlanmamış." else null
                redrawWidgets(this@MainActivity)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = "Menü yenilenemedi. Kayıtlı veriler gösteriliyor. İnternet bağlantını kontrol edip yeniden dene." }
            finally { loading = false }
        }
        suspend fun checkUpdate() {
            checking = true
            try {
                val latest = updater.check()
                if (latest != null && latest.code > BuildConfig.VERSION_CODE) release = latest
                else message = if (latest == null) "Henüz yayımlanmış bir sürüm yok." else "Uygulama güncel."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = e.message ?: "Güncelleme denetlenemedi." }
            finally { checking = false }
        }
        LaunchedEffect(updateRequest) { if (updateRequest > 0) checkUpdate() }
        LaunchedEffect(selected.year,selected.monthValue) {
            days = store.days(YearMonth.from(selected)); updated = store.updated(YearMonth.from(selected))
            if (updated == 0L || System.currentTimeMillis() - updated > 6*60*60*1000L) refresh()
            else try { store.refreshWeek(skipFresh = true); redrawWidgets(this@MainActivity) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = "Gelecek haftanın bazı menüleri alınamadı; kayıtlı menüler kullanılabilir." }
        }
        MenuTheme(options) {
            AppSystemBars(options)
            Surface(Modifier.fillMaxSize(),color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                        Text("YERLEŞKE",Modifier.weight(1f),fontSize = 13.sp,letterSpacing = 2.sp,color = MaterialTheme.colorScheme.primary,fontWeight = FontWeight.Bold)
                        TextButton(onClick = { settingsLauncher.launch(Intent(this@MainActivity,AppSettingsActivity::class.java)) }) { Text("⚙ Ayarlar") }
                    }
                    Text("Yemek menüsü",fontSize = 30.sp,fontWeight = FontWeight.Bold)
                    Text(dateLabel(selected),fontSize = 21.sp,fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { following = false; selected = selected.minusDays(1) },modifier = Modifier.weight(1f),contentPadding = PaddingValues(8.dp)) { Text("‹ Önceki") }
                        Button(onClick = { returnToAutomatic() },modifier = Modifier.weight(1f)) { Text("Bugün") }
                        FilledTonalButton(onClick = { following = false; selected = selected.plusDays(1) },modifier = Modifier.weight(1f),contentPadding = PaddingValues(8.dp)) { Text("Sonraki ›") }
                    }
                    if (!options.timedMeals) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(following,onClick = { returnToAutomatic() },label = { Text("Otomatik") })
                        FilterChip(!following && !evening,onClick = { following = false; evening = false; store.evening = false },label = { Text("Öğle") })
                        FilterChip(!following && evening,onClick = { following = false; evening = true; store.evening = true },label = { Text("Akşam") })
                    }
                    if (following || options.timedMeals) Text("Saate göre otomatik · " + if (selected == automatic.date && automatic.date != now.toLocalDate()) "Sonraki hizmet günü" else if (selected != now.toLocalDate()) "İki öğün" else if (automatic.lunch && automatic.dinner) "Öğle ve akşam" else if (automatic.lunch) "Öğle" else "Akşam",fontSize = 13.sp,color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(!vegetarian,onClick = { vegetarian = false; store.vegetarian = false; scope.launch { redrawWidgets(this@MainActivity) } },label = { Text("Normal") })
                        FilterChip(vegetarian,onClick = { vegetarian = true; store.vegetarian = true; scope.launch { redrawWidgets(this@MainActivity) } },label = { Text("Vejetaryen") })
                    }
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    val closedReason = ServiceCalendar.reason(selected)
                    if (closedReason != null) Text(closedReason,Modifier.padding(vertical = 20.dp))
                    else {
                        val meals = AppMenuPlanner.meals(now,selected,options.timedMeals,if (!following && !options.timedMeals) evening else null)
                        meals.forEach { dinner ->
                            val key = if (dinner) { if (vegetarian) "dinner2_menu" else "dinner_menu" } else { if (vegetarian) "lunch2_menu" else "lunch_menu" }
                            val lines = days.find { it.date == selected }?.meals?.get(key).orEmpty()
                            if (following || options.timedMeals) Text(if (dinner) "AKŞAM" else "ÖĞLE",color = MaterialTheme.colorScheme.primary,fontWeight = FontWeight.SemiBold,fontSize = 14.sp)
                            HorizontalDivider()
                            if (lines.isEmpty() && !loading) Text("Bu tarih ve öğün için yayımlanmış menü yok.",Modifier.padding(vertical = 16.dp))
                            Column { lines.forEachIndexed { index,line ->
                                Row(Modifier.fillMaxWidth().padding(vertical = if (options.spacing == AppSpacing.COMPACT) 8.dp else 16.dp),horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Text("%02d".format(index+1),color = MaterialTheme.colorScheme.onSurfaceVariant,fontSize = 13.sp)
                                    Text(widgetMealText(line,options.calories),fontSize = (16*options.font.scale).sp,lineHeight = (23*options.font.scale).sp)
                                }
                                HorizontalDivider()
                            } }
                            if (options.hours) Text("Servis saati: " + if (dinner) "17.00–19.15" else "11.30–14.30",fontSize = 12.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ServiceCalendar.calendarWarning(selected)?.let { Text(it,fontSize = 12.sp) }
                    if (options.updated) Text(if (updated == 0L) "Henüz veri alınmadı" else "Son kontrol: " + Instant.ofEpochMilli(updated).atZone(turkey).format(DateTimeFormatter.ofPattern("d MMM HH:mm",turkish)) + if (System.currentTimeMillis()-updated > 24*60*60*1000L) " · Eski veri" else "",fontSize = 12.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(enabled = !loading,onClick = { scope.launch { refresh() } },modifier = Modifier.fillMaxWidth()) { Text("Menüyü yenile") }
                    message?.let { Text(it,color = MaterialTheme.colorScheme.primary,fontSize = 13.sp) }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { startActivity(Intent(this@MainActivity,WidgetSettingsActivity::class.java)) }) { Text("Widget ayarları") }
                        TextButton(enabled = !checking,onClick = { scope.launch { checkUpdate() } }) { Text(if (checking) "Denetleniyor…" else "Güncelleme denetle") }
                    }
                    Text("Sürüm ${BuildConfig.VERSION_NAME} · Bağımsız bir projedir; YTÜ'nün resmî uygulaması değildir. Kaynak: sks.yildiz.edu.tr. Hafta sonları ve resmî tatillerde hizmet yoktur.",fontSize = 11.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                release?.let { next -> AlertDialog(onDismissRequest = { if (!downloading) release = null },title = { Text("Yeni sürüm: ${next.version}") },text = { Text(if (downloading) "İndiriliyor ve imza doğrulanıyor…" else next.notes.take(2000).ifBlank { "Yeni sürüm kuruluma hazır." }) },confirmButton = {
                    TextButton(enabled = !downloading,onClick = { scope.launch {
                        downloading = true
                        try { val apk = updater.download(next); if (updater.install(apk)) release = null else message = "Kurulum iznini verdikten sonra tekrar İndir ve güncelle'ye dokun." }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = e.message ?: "Güncelleme kurulamadı."; release = null }
                        finally { downloading = false }
                    } }) { Text("İndir ve güncelle") }
                },dismissButton = { TextButton(enabled = !downloading,onClick = { release = null }) { Text("Daha sonra") } }) }
            }
        }
    }
}
