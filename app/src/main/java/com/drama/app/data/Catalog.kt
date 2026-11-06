package com.drama.app.data

import android.content.Context
import com.drama.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Video(val id: String, val title: String, val duration: Int, val order: Int)

class Series(
    val id: String,
    val title: String,
    val studio: String,
    /** full = cała seria w jednym/kilku filmach, episodes = playlista z odcinkami, partial = niepełna */
    val kind: String,
    val videos: List<Video>,
    val views: Long,
    val thumb: String?,
    val duration: Int,
) {
    val genres: List<String> by lazy { Genres.of(title) }
    val isFull get() = kind == "full" || kind == "episodes"
    fun thumbOrDefault() = thumb ?: "https://i.ytimg.com/vi/${videos.first().id}/hqdefault.jpg"
    fun hiThumb() = thumbOrDefault()
}

object Genres {
    private val rules = listOf(
        "Miliarderzy" to Regex("billionaire|ceo|tycoon|rich|heir\\b|mogul|boss's|president", RegexOption.IGNORE_CASE),
        "Zemsta" to Regex("revenge|regret|payback|betray|cheat|karma|too late", RegexOption.IGNORE_CASE),
        "Ukryta tożsamość" to Regex("secret|hidden|disguis|undercover|real identity|heiress|mistaken", RegexOption.IGNORE_CASE),
        "Małżeństwo" to Regex("wife|husband|marri|divorce|bride|wedding|contract", RegexOption.IGNORE_CASE),
        "Wilkołaki i fantasy" to Regex("alpha|luna|wolf|werewolf|vampire|dragon|fated|mate\\b|lycan|witch", RegexOption.IGNORE_CASE),
        "Odrodzenie" to Regex("reborn|rebirth|second chance|time travel|back to", RegexOption.IGNORE_CASE),
        "Mafia" to Regex("mafia|don\\b|mob\\b|gangster|cartel", RegexOption.IGNORE_CASE),
        "Dzieci i ciąża" to Regex("baby|pregnan|twins|son\\b|daughter|kid|quadruplets|triplets", RegexOption.IGNORE_CASE),
    )

    val all = rules.map { it.first }
    fun of(title: String) = rules.filter { it.second.containsMatchIn(title) }.map { it.first }
}

object Catalog {
    @Volatile var series: List<Series> = emptyList(); private set
    @Volatile var generated: Long = 0; private set
    private lateinit var cacheFile: File

    fun byId(id: String) = series.firstOrNull { it.id == id }

    fun studios(): List<String> = series.groupingBy { it.studio }.eachCount().entries.sortedByDescending { it.value }.map { it.key }

    /** Wczytuje katalog: najpierw zapisany na telefonie, potem wbudowany w aplikację. */
    fun loadLocal(ctx: Context) {
        cacheFile = File(ctx.filesDir, "catalog.json")
        val text = if (cacheFile.exists()) cacheFile.readText() else ctx.assets.open("catalog.json").bufferedReader().use { it.readText() }
        try { parse(text) } catch (_: Exception) {
            parse(ctx.assets.open("catalog.json").bufferedReader().use { it.readText() })
        }
        val bundled = try {
            JSONObject(ctx.assets.open("catalog.json").bufferedReader().use { it.readText() }).optLong("generated")
        } catch (_: Exception) { 0L }
        if (bundled > generated) parse(ctx.assets.open("catalog.json").bufferedReader().use { it.readText() })
    }

    /** Pobiera najnowszy katalog (odświeżany codziennie na GitHubie). */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        try {
            val c = URL(BuildConfig.CATALOG_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.instanceFollowRedirects = true
            if (c.responseCode != 200) return@withContext false
            val text = c.inputStream.bufferedReader().use { it.readText() }
            val gen = JSONObject(text).optLong("generated")
            if (gen <= generated) return@withContext false
            parse(text)
            cacheFile.writeText(text)
            true
        } catch (_: Exception) { false }
    }

    private fun parse(text: String) {
        val o = JSONObject(text)
        val arr = o.getJSONArray("series")
        val list = ArrayList<Series>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            val va = s.getJSONArray("videos")
            val vids = (0 until va.length()).map {
                val v = va.getJSONObject(it)
                Video(v.getString("id"), v.optString("title"), v.optInt("duration"), v.optInt("order"))
            }
            if (vids.isEmpty()) continue
            list += Series(
                s.getString("id"), s.getString("title"), s.getString("studio"), s.getString("kind"), vids,
                s.optLong("views"), s.optString("thumb").ifBlank { null }, s.optInt("duration"),
            )
        }
        series = list
        generated = o.optLong("generated")
    }
}

fun formatDuration(sec: Int): String {
    if (sec <= 0) return ""
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "$h h ${m} min" else "$m min"
}

fun formatViews(v: Long): String = when {
    v >= 1_000_000 -> "%.1f mln".format(v / 1_000_000.0)
    v >= 1_000 -> "${v / 1000} tys."
    else -> "$v"
}
