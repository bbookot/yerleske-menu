package app.yerleske.menu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.YearMonth
import java.time.Instant
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MenuScreen() }
    }
    @Composable
    fun MenuScreen() {
        val store = remember { MenuStore(this) }
        val scope = rememberCoroutineScope()
        var selected by remember { mutableStateOf(today()) }
        var days by remember { mutableStateOf(store.days(YearMonth.from(selected))) }
        var evening by remember { mutableStateOf(store.evening) }
        var vegetarian by remember { mutableStateOf(store.vegetarian) }
        var theme by remember { mutableIntStateOf(store.theme) }
        var loading by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var updated by remember { mutableLongStateOf(store.updated(YearMonth.from(selected))) }
        var release by remember { mutableStateOf<Release?>(null) }
        var checking by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(false) }
        val updater = remember { Updater(this) }
        suspend fun refresh() {
            loading = true
            try {
                store.refresh(YearMonth.from(selected))
                store.refreshWeek(skipFresh = true)
                days = store.days(YearMonth.from(selected))
                updated = store.updated(YearMonth.from(selected))
                message = if (days.isEmpty()) "Bu ayın menüsü henüz yayımlanmamış." else null
                redrawWidgets(this@MainActivity)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = "Menü yenilenemedi. Kayıtlı veriler gösteriliyor. İnternet bağlantını kontrol edip yeniden dene." }
            finally { loading = false }
        }
        LaunchedEffect(selected.year, selected.monthValue) {
            days = store.days(YearMonth.from(selected))
            updated = store.updated(YearMonth.from(selected))
            if (updated == 0L || System.currentTimeMillis() - updated > 6*60*60*1000L) refresh()
            else {
                try { store.refreshWeek(skipFresh = true); redrawWidgets(this@MainActivity) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = "Gelecek haftanın bazı menüleri alınamadı; kayıtlı menüler kullanılabilir." }
            }
        }
        val dark = when(theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
        MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFB4D78B)) else lightColorScheme(primary = Color(0xFF285A42), background = Color(0xFFF5F5EF), surface = Color(0xFFFDFDF7))) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("YERLEŞKE", fontSize = 12.sp, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Bugün ne var?", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { theme = (theme + 1) % 3; store.theme = theme }) { Text(listOf("Sistem", "Açık", "Koyu")[theme]) }
                    }
                    Text("YTÜ öğrenci yemek menüsü", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(enabled = !loading, onClick = { selected = selected.minusDays(1) }) { Text("‹ Önceki") }
                                TextButton(enabled = !loading, onClick = { selected = today() }) { Text("Bugün") }
                                TextButton(enabled = !loading, onClick = { selected = selected.plusDays(1) }) { Text("Sonraki ›") }
                            }
                            Text(dateLabel(selected), fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(!evening, onClick = { evening = false; store.evening = false; scope.launch { redrawWidgets(this@MainActivity) } }, label = { Text("Öğle") })
                                FilterChip(evening, onClick = { evening = true; store.evening = true; scope.launch { redrawWidgets(this@MainActivity) } }, label = { Text("Akşam") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(!vegetarian, onClick = { vegetarian = false; store.vegetarian = false; scope.launch { redrawWidgets(this@MainActivity) } }, label = { Text("Normal") })
                                FilterChip(vegetarian, onClick = { vegetarian = true; store.vegetarian = true; scope.launch { redrawWidgets(this@MainActivity) } }, label = { Text("Vejetaryen") })
                            }
                            HorizontalDivider()
                            val key = if (evening) { if (vegetarian) "dinner2_menu" else "dinner_menu" } else { if (vegetarian) "lunch2_menu" else "lunch_menu" }
                            val closedReason = ServiceCalendar.reason(selected)
                            val menu = if (closedReason != null) emptyList() else days.find { it.date == selected }?.meals?.get(key).orEmpty()
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (menu.isEmpty() && !loading) Text(closedReason ?: "Bu tarih ve öğün için yayımlanmış menü yok.", Modifier.padding(vertical = 16.dp))
                            ServiceCalendar.calendarWarning(selected)?.let { Text(it, fontSize = 12.sp) }
                            menu.forEachIndexed { index, line ->
                                Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("%02d".format(index + 1), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    Text(line, fontSize = 16.sp)
                                }
                            }
                            Text(if (updated == 0L) "Henüz veri alınmadı" else "Son kontrol: " + Instant.ofEpochMilli(updated).atZone(turkey).format(DateTimeFormatter.ofPattern("d MMM HH:mm", turkish)) + if (System.currentTimeMillis() - updated > 24*60*60*1000L) " · Eski veri" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(enabled = !loading, onClick = { scope.launch { refresh() } }, modifier = Modifier.fillMaxWidth()) { Text("Menüyü yenile") }
                        }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
                    Text("Ana ekranında da yanında", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("Widget 14.30'a kadar öğle ve akşamı, 19.15'e kadar yalnız akşamı, sonra sonraki hizmet gününü gösterir. ‹ ve › ile üç gün ileri bakabilirsin. 7 günlük veri önceden hazırlanır. Hafta sonu ve resmî tatillerde hizmet yoktur. Geçişler Android pil yönetimi nedeniyle gecikebilir.", fontSize = 14.sp)
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sürüm ${BuildConfig.VERSION_NAME}", Modifier.padding(top = 12.dp), fontSize = 12.sp)
                        TextButton(enabled = !checking, onClick = { scope.launch {
                            checking = true
                            try {
                                val latest = updater.check()
                                if (latest != null && latest.code > BuildConfig.VERSION_CODE) release = latest
                                else message = if (latest == null) "Henüz yayımlanmış bir sürüm yok." else "Uygulama güncel."
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { message = e.message ?: "Güncelleme denetlenemedi." }
                            finally { checking = false }
                        } }) { Text(if (checking) "Denetleniyor…" else "Güncelleme denetle") }
                    }
                    Text("Bağımsız bir projedir; YTÜ'nün resmi uygulaması değildir. Kaynak: sks.yildiz.edu.tr. Menü ve servis bilgileri değişebilir. Kişisel hesap, reklam veya analiz takibi içermez.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                release?.let { next -> AlertDialog(onDismissRequest = { if (!downloading) release = null }, title = { Text("Yeni sürüm: ${next.version}") }, text = { Text(if (downloading) "İndiriliyor ve imza doğrulanıyor…" else next.notes.take(2000).ifBlank { "Yeni sürüm kuruluma hazır." }) }, confirmButton = {
                    TextButton(enabled = !downloading, onClick = { scope.launch {
                        downloading = true
                        try {
                            val apk = updater.download(next)
                            if (updater.install(apk)) release = null
                            else message = "Kurulum iznini verdikten sonra tekrar İndir ve güncelle'ye dokun."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = e.message ?: "Güncelleme kurulamadı."; release = null }
                        finally { downloading = false }
                    } }) { Text("İndir ve güncelle") }
                }, dismissButton = { TextButton(enabled = !downloading, onClick = { release = null }) { Text("Daha sonra") } }) }
            }
        }
    }
}

