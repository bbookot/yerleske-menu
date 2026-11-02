package app.yerleske.menu

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class Release(val code: Int, val version: String, val notes: String, val url: String, val size: Long)
class Updater(private val context: Context) {
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank()) throw IOException("GitHub deposu henüz bağlanmadı. Güncelleme denetimi yayımdan önce etkinleştirilecek.")
        val request = Request.Builder().url("https://api.github.com/repos/$repo/releases/latest").header("Accept", "application/vnd.github+json").build()
        MenuStore.http.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("Güncelleme denetlenemedi (HTTP ${response.code})")
            val obj = JSONObject(response.body?.string() ?: throw IOException("Güncelleme yanıtı boş"))
            if (obj.optBoolean("draft") || obj.optBoolean("prerelease")) return@withContext null
            val tag = obj.getString("tag_name")
            val match = Regex("v(\\d+)\\.(\\d+)\\.(\\d+)\\+(\\d+)").matchEntire(tag) ?: throw IOException("Sürüm etiketi tanınmadı")
            val code = match.groupValues[4].toIntOrNull() ?: throw IOException("Sürüm kodu geçersiz")
            val assets = obj.getJSONArray("assets")
            val asset = (0 until assets.length()).map { assets.getJSONObject(it) }.singleOrNull { it.getString("name") == "yerleske-menu.apk" } ?: throw IOException("Sürüm APK dosyası bulunamadı")
            val url = asset.getString("browser_download_url")
            if (!url.startsWith("https://github.com/$repo/releases/download/")) throw IOException("İndirme adresi doğrulanamadı")
            Release(code, tag.removePrefix("v").substringBefore('+'), obj.optString("body"), url, asset.getLong("size"))
        }
    }
    suspend fun download(release: Release): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(dir, "download.part")
        val apk = File(dir, "update.apk")
        if (release.size !in 1..150_000_000) throw IOException("APK boyutu geçersiz")
        try {
            MenuStore.http.newCall(Request.Builder().url(release.url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("APK indirilemedi (HTTP ${response.code})")
                val body = response.body ?: throw IOException("İndirme yanıtı boş")
                body.byteStream().use { input -> partial.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > release.size) throw IOException("İndirilen dosya beklenenden büyük")
                        output.write(buffer, 0, count)
                    }
                } }
            }
            if (partial.length() != release.size) throw IOException("İndirme tamamlanmadı")
            verify(partial, release.code)
            if (apk.exists() && !apk.delete()) throw IOException("Eski indirme temizlenemedi")
            if (!partial.renameTo(apk)) throw IOException("APK hazırlanamadı")
            apk
        } finally { partial.delete() }
    }
    @Suppress("DEPRECATION")
    private fun verify(file: File, expectedCode: Int) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val downloaded = pm.getPackageArchiveInfo(file.path, flags) ?: throw IOException("Geçersiz APK")
        val installed = pm.getPackageInfo(context.packageName, flags)
        fun signatures(info: android.content.pm.PackageInfo): Set<String> {
            val values = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return values.orEmpty().map { java.security.MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { byte -> "%02x".format(byte) } }.toSet()
        }
        val version = if (Build.VERSION.SDK_INT >= 28) downloaded.longVersionCode else downloaded.versionCode.toLong()
        if (downloaded.packageName != context.packageName || version != expectedCode.toLong() || version <= BuildConfig.VERSION_CODE) throw IOException("APK uygulama veya sürüm bilgisi eşleşmiyor")
        val actual = signatures(downloaded)
        if (actual.isEmpty() || actual != signatures(installed)) throw IOException("APK imzası eşleşmiyor. Kurulum durduruldu.")
    }
    fun install(apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return false
        }
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(context, "${context.packageName}.files", apk), "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
