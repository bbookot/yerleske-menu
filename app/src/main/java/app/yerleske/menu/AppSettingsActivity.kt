package app.yerleske.menu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.work.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class AppSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { AppSettingsStore(this) }
            var options by remember { mutableStateOf(store.read()) }
            var vegetarian by remember { mutableStateOf(MenuStore(this).vegetarian) }
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { options = store.read(); vegetarian = MenuStore(this@AppSettingsActivity).vegetarian } }
                lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
            }
            fun change(value: AppOptions) {
                store.write(value); options = value
                WorkManager.getInstance(this).enqueueUniqueWork("widget-appearance-apply", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WidgetClockWorker>().build())
            }
            MenuTheme(options) {
                AppSystemBars(options)
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { finish() }) { Text("‹ Geri") }
                            Text("Uygulama ayarları", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                        SettingsHeading("GÖRÜNÜM")
                        SettingsChoice("Tema", listOf("Sistem","Açık","Koyu")[options.theme], listOf("Sistem","Açık","Koyu")) { change(options.copy(theme = it)) }
                        SettingsChoice("Vurgu rengi", options.palette.label, WidgetPalette.entries.map { it.label }) { change(options.copy(palette = WidgetPalette.entries[it])) }
                        SettingsChoice("Yazı boyutu",options.font.label,WidgetFont.entries.map { it.label }) { change(options.copy(font = WidgetFont.entries[it])) }
                        SettingsChoice("Liste aralığı",options.spacing.label,AppSpacing.entries.map { it.label }) { change(options.copy(spacing = AppSpacing.entries[it])) }
                        SettingsHeading("MENÜ")
                        SettingsToggleRow("Saate göre tek öğün göster", options.timedMeals) { change(options.copy(timedMeals = it)) }
                        Text("Uygulama ve widget için ortak: 06.00–14.30 yalnız öğle; 14.30–19.15 yalnız akşam. Akşamdan sonra sonraki hizmet gününün iki öğünü, 06.00’ya kadar birlikte görünür. İleri tarihlerde iki öğün gösterilir.",Modifier.padding(vertical = 10.dp),fontSize = 12.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!options.timedMeals) {
                            SettingsChoice("Tarih gezintisinde ilk öğün",options.startMeal.label,StartMeal.entries.map { it.label }) { change(options.copy(startMeal = StartMeal.entries[it])) }
                            Text("Elle tarih gezerken bu öğün seçilir. Son seçilen, en son baktığın öğündür. İlk açılış ve Bugün düğmesi her zaman otomatik menüyü gösterir; akşam servisi bitince sonraki hizmet gününün iki öğünü açılır.",fontSize = 12.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SettingsChoice("Menü türü",if (vegetarian) "Vejetaryen" else "Normal",listOf("Normal","Vejetaryen")) {
                            vegetarian = it == 1; MenuStore(this@AppSettingsActivity).vegetarian = vegetarian; change(options)
                        }
                        SettingsHeading("BİLGİLER")
                        SettingsToggleRow("Kalori bilgileri",options.calories) { change(options.copy(calories = it)) }
                        SettingsToggleRow("Servis saatleri",options.hours) { change(options.copy(hours = it)) }
                        SettingsToggleRow("Son kontrol zamanı",options.updated) { change(options.copy(updated = it)) }
                        Spacer(Modifier.height(24.dp))
                        SettingsLink("Widget ayarları") { startActivity(Intent(this@AppSettingsActivity,WidgetSettingsActivity::class.java)) }
                        SettingsLink("Güncelleme denetle") { setResult(RESULT_OK); finish() }
                        Text("Ayarlar anında kaydedilir ve uygulama güncellemelerinde korunur. Uygulama görünümü widget görünümünden bağımsızdır.",Modifier.padding(vertical = 24.dp),fontSize = 12.sp,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
@Composable private fun SettingsHeading(label: String) {
    Text(label,Modifier.padding(top = 28.dp,bottom = 10.dp),color = MaterialTheme.colorScheme.primary,fontWeight = FontWeight.SemiBold,fontSize = 13.sp)
}
@Composable private fun SettingsLink(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp),verticalAlignment = Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontSize = 16.sp); Text("›",fontSize = 22.sp)
    }
    HorizontalDivider()
}
@Composable private fun SettingsChoice(label: String, value: String, choices: List<String>, onChoice: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 18.dp),verticalAlignment = Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontSize = 16.sp)
        Text(value,color = MaterialTheme.colorScheme.onSurfaceVariant,fontSize = 14.sp)
        Text(" ›",fontSize = 22.sp)
    }
    HorizontalDivider()
    if (open) AlertDialog(onDismissRequest = { open = false },title = { Text(label) },text = {
        Column { choices.forEachIndexed { index, choice ->
            Row(Modifier.fillMaxWidth().clickable { onChoice(index); open = false }.padding(vertical = 4.dp),verticalAlignment = Alignment.CenterVertically) {
                RadioButton(choice == value,onClick = { onChoice(index); open = false }); Text(choice)
            }
        } }
    },confirmButton = { TextButton(onClick = { open = false }) { Text("Kapat") } })
}
@Composable private fun SettingsToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),verticalAlignment = Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontSize = 16.sp); Switch(checked,onCheckedChange = onChange)
    }
    HorizontalDivider()
}
