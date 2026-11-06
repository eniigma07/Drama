package com.drama.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** Ulubione i postęp oglądania (tylko na telefonie). */
object Store {
    private lateinit var p: SharedPreferences
    private val favs = LinkedHashSet<String>()
    private val pos = HashMap<String, Float>() // videoId -> sekunda
    private val len = HashMap<String, Float>() // videoId -> długość
    private val last = HashMap<String, Pair<String, Long>>() // seriesId -> (videoId, czas)
    var version by mutableIntStateOf(0); private set

    fun init(ctx: Context) {
        p = ctx.getSharedPreferences("drama", Context.MODE_PRIVATE)
        favs += p.getString("favs", "")!!.split('\n').filter { it.isNotBlank() }
        p.getString("pos", null)?.let { s -> val o = JSONObject(s); o.keys().forEach { pos[it] = o.getDouble(it).toFloat() } }
        p.getString("len", null)?.let { s -> val o = JSONObject(s); o.keys().forEach { len[it] = o.getDouble(it).toFloat() } }
        p.getString("last", null)?.let { s ->
            val o = JSONObject(s)
            o.keys().forEach { k -> val a = o.getJSONArray(k); last[k] = a.getString(0) to a.getLong(1) }
        }
    }

    fun isFav(id: String) = id in favs
    fun toggleFav(id: String) {
        if (!favs.remove(id)) favs += id
        p.edit().putString("favs", favs.joinToString("\n")).apply()
        version++
    }
    fun favorites(): List<Series> = favs.reversed().mapNotNull { Catalog.byId(it) }

    fun position(videoId: String) = pos[videoId] ?: 0f
    fun length(videoId: String) = len[videoId] ?: 0f

    /** 0..1 – jak daleko obejrzany film */
    fun progress(videoId: String): Float {
        val l = len[videoId] ?: return 0f
        if (l <= 0) return 0f
        return (pos[videoId] ?: 0f) / l
    }

    fun seriesProgress(s: Series): Float {
        val total = s.videos.sumOf { maxOf(it.duration, 1) }.toFloat()
        val done = s.videos.sumOf { v -> (progress(v.id).coerceIn(0f, 1f) * maxOf(v.duration, 1)).toDouble() }.toFloat()
        return (done / total).coerceIn(0f, 1f)
    }

    fun save(seriesId: String, videoId: String, second: Float, duration: Float, flush: Boolean = false) {
        pos[videoId] = second
        if (duration > 0) len[videoId] = duration
        last[seriesId] = videoId to System.currentTimeMillis()
        if (flush) persist()
    }

    fun persist() {
        val po = JSONObject(); pos.forEach { (k, v) -> po.put(k, v.toDouble()) }
        val lo = JSONObject(); len.forEach { (k, v) -> lo.put(k, v.toDouble()) }
        val la = JSONObject(); last.forEach { (k, v) -> la.put(k, org.json.JSONArray(listOf(v.first, v.second))) }
        p.edit().putString("pos", po.toString()).putString("len", lo.toString()).putString("last", la.toString()).apply()
        version++
    }

    /** Ostatnio oglądane serie (najnowsze pierwsze), bez skończonych. */
    fun continueWatching(): List<Pair<Series, String>> =
        last.entries.sortedByDescending { it.value.second }
            .mapNotNull { (sid, v) -> Catalog.byId(sid)?.let { it to v.first } }
            .filter { (s, _) -> seriesProgress(s) < 0.97f }

    /** Film, od którego wznowić serię. */
    fun resumeVideo(s: Series): Video {
        val lastId = last[s.id]?.first
        val v = s.videos.firstOrNull { it.id == lastId } ?: return s.videos.first()
        if (progress(v.id) > 0.97f) {
            val i = s.videos.indexOf(v)
            return s.videos.getOrNull(i + 1) ?: v
        }
        return v
    }

    fun clearHistory() {
        pos.clear(); len.clear(); last.clear(); persist()
    }
}
