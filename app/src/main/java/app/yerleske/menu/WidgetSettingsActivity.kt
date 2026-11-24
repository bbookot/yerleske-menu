package app.yerleske.menu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class WidgetSettingsActivity : ComponentActivity() {
    private var settingsChanged = false
    override fun onPause() {
        super.onPause()
        // A quick Back/Home can cancel the preview's debounce. Persist a redraw
        // independently of this activity so the last choice still reaches widgets.
        if (settingsChanged) {
            WorkManager.getInstance(this).enqueueUniqueWork("widget-appearance-apply", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WidgetClockWorker>().build())
            settingsChanged = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContent {
            // Widget SYSTEM mode follows the launcher, independently of the app's theme.
            val systemDark = android.content.res.Resources.getSystem().configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val appOptions = AppSettingsStore(this).read()
            MenuTheme(appOptions) {
                AppSystemBars(appOptions)
                SettingsScreen(systemDark)
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun SettingsScreen(systemDark: Boolean) {
        val store = remember { WidgetSettingsStore(this) }
        var options by remember { mutableStateOf(store.read()) }
        var timedMeals by remember { mutableStateOf(AppSettingsStore(this).read().timedMeals) }
        var resetDialog by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        fun change(value: WidgetOptions) { store.write(value); options = value.normalized(); settingsChanged = true }
        LaunchedEffect(options,timedMeals) {
            delay(150)
            try { redrawWidgets(this@WidgetSettingsActivity); error = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "Widget yenilenemedi. Ana ekrandaki yenile düğmesini deneyebilirsin." }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { finish() }) { Text("‹ Geri") }
                    Text("Widget ayarları", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("Tüm widget'lara ortak uygulanır. Değişiklikler anında kaydedilir.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Önizleme · örnek menü", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                WidgetPreview(options, systemDark)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                SettingTitle("Renk paleti")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetPalette.entries.forEach { palette ->
                        FilterChip(options.palette == palette, onClick = { change(options.copy(palette = palette)) }, label = { Text(palette.label) }, leadingIcon = { Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(palette.card))) })
                    }
                }
                SettingTitle("Görünüm modu")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetMode.entries.forEach { mode -> FilterChip(options.mode == mode, onClick = { change(options.copy(mode = mode)) }, label = { Text(mode.label) }) }
                }
                Text("Kart: mevcut canlı görünüm. Sistem: telefonla birlikte açık/koyu değişir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SettingTitle("Yıldız stili")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetStarStyle.entries.forEach { style -> FilterChip(options.starStyle == style, onClick = { change(options.copy(starStyle = style)) }, label = { Text(style.label) }) }
                }
                Text("Altın fırça: koyu sarı yıldız ve hafif fırça parlaklığı. Lacivert paletle deneyebilirsin.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingTitle("Yıldız belirginliği")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kapalı", "Silik", "Orta", "Belirgin").forEachIndexed { index, label -> FilterChip(options.star == index, onClick = { change(options.copy(star = index)) }, label = { Text(label) }) }
                }
                SettingTitle("Yazı boyutu")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetFont.entries.forEach { font -> FilterChip(options.font == font, onClick = { change(options.copy(font = font)) }, label = { Text(font.label) }) }
                }
                Text("Yemek listesi sığmazsa widget içinde kaydırabilirsin.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SettingTitle("Menü türü")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!options.vegetarian, onClick = { change(options.copy(vegetarian = false)) }, label = { Text("Normal menü") })
                    FilterChip(options.vegetarian, onClick = { change(options.copy(vegetarian = true)) }, label = { Text("Vejetaryen menü") })
                }
                SettingToggle("Saate göre tek öğün göster",timedMeals) {
                    val appStore = AppSettingsStore(this@WidgetSettingsActivity)
                    appStore.write(appStore.read().copy(timedMeals = it)); timedMeals = it; settingsChanged = true
                    scheduleWidgetClock(this@WidgetSettingsActivity)
                }
                Text("Uygulamayla ortak ayar: 06.00–14.30 öğle, 14.30–19.15 akşam; ardından sonraki hizmet gününün iki öğünü. İleri tarihlerde iki öğün görünür.",fontSize = 12.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingTitle("Gösterilecek bilgiler")
                SettingToggle("Servis saatleri", options.hours) { change(options.copy(hours = it)) }
                SettingToggle("Kalori bilgileri", options.calories) { change(options.copy(calories = it)) }
                SettingToggle("Son yenileme zamanı", options.updated) { change(options.copy(updated = it)) }

                SettingTitle("Arka plan saydamlığı")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100,85,70,50).forEach { opacity -> FilterChip(options.opacity == opacity, onClick = { change(options.copy(opacity = opacity)) }, label = { Text(if (opacity == 100) "Opak" else "%${100-opacity} saydam") }) }
                }
                Text("Saydamlık arttıkça duvar kâğıdı daha çok görünür. Yazılar tam görünür kalır; okunabilirlik duvar kâğıdına göre değişebilir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { resetDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Varsayılana dön") }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (resetDialog) AlertDialog(onDismissRequest = { resetDialog = false }, title = { Text("Widget ayarları sıfırlansın mı?") }, text = { Text("Mavi kart görünümüne ve normal menüye dönülür. Kayıtlı yemek verileri korunur.") }, confirmButton = { TextButton(onClick = { change(WidgetOptions()); resetDialog = false }) { Text("Sıfırla") } }, dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Vazgeç") } })
    }
}

@Composable
private fun SettingTitle(text: String) { Text(text, Modifier.padding(top = 8.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
@Composable
private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun WidgetPreview(options: WidgetOptions, systemDark: Boolean) {
    val colors = options.colors(systemDark)
    Box(Modifier.fillMaxWidth().height(255.dp).clip(RoundedCornerShape(20.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = 18.dp.toPx()
            for (x in 0..(size.width / cell).toInt()) for (y in 0..(size.height / cell).toInt()) drawRect(if ((x+y)%2 == 0) Color(0xFFAEB7C5) else Color(0xFFD4D9E2), topLeft = androidx.compose.ui.geometry.Offset(x*cell,y*cell), size = androidx.compose.ui.geometry.Size(cell,cell))
        }
        Box(Modifier.fillMaxSize().background(Color(colors.background))) {
            if (options.star > 0 && options.starStyle == WidgetStarStyle.GOLD_BRUSH) {
                val gold = remember(colors.star ushr 24) { goldStarBitmap(colors.star ushr 24).asImageBitmap() }
                Image(gold, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            } else if (options.star > 0) Canvas(Modifier.fillMaxSize()) {
                val path = Path().apply { StarShape.points.forEachIndexed { i, (x,y) -> if (i == 0) moveTo(x/360*size.width,y/220*size.height) else lineTo(x/360*size.width,y/220*size.height) }; close() }
                drawPath(path, Color(colors.star), style = Stroke(size.width/360*5.5f))
            }
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("YTÜ · YEMEK · " + if (options.vegetarian) "VEJ" else "NORMAL", color = Color(colors.text), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Öğle ve akşam", color = Color(colors.text), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ÖĞLE" to "11.30 – 14.30", "AKŞAM" to "17.00 – 19.15").forEach { (meal,hours) ->
                        Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(colors.panel)).padding(8.dp)) {
                            Text(meal, color = Color(colors.text), fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp)
                            if (options.hours) Text(hours, color = Color(colors.secondary), fontSize = 9.sp, lineHeight = 12.sp)
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                listOf("Çorba (130 kcal)", if (options.vegetarian) "Sebze yemeği (260 kcal)" else "Ana yemek (350 kcal)", "Pilav (280 kcal)", "Ayran (120 kcal)").forEach { Text(widgetMealText(it,options.calories), color = Color(colors.text), fontSize = (11*options.font.scale).sp, lineHeight = (13*options.font.scale).sp) }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Bugün", color = Color(colors.text), fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("‹",color = Color(colors.text)); Text("›",color = Color(colors.text)) }
                }
                if (options.updated) Text("Son kontrol · şimdi", color = Color(colors.secondary), fontSize = 9.sp)
            }
        }
    }
}
