package com.drama.app.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.drama.app.data.Catalog
import com.drama.app.data.Store
import com.drama.app.data.formatDuration
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun PlayerScreen(seriesId: String, startVideo: String?, onBack: () -> Unit) {
    val s = Catalog.byId(seriesId) ?: run { onBack(); return }
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var current by remember { mutableStateOf(s.videos.firstOrNull { it.id == startVideo } ?: Store.resumeVideo(s)) }
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val errorHolder = remember { mutableStateOf<String?>(null) }
    var error by errorHolder
    val fsHolder = remember { mutableStateOf<View?>(null) }
    var exitFs by remember { mutableStateOf<(() -> Unit)?>(null) }
    var fav by remember { mutableStateOf(Store.isFav(s.id)) }
    val cur = remember { object { var id = ""; var sec = 0f; var dur = 0f; var lastSave = 0L } }

    fun openInYouTube() {
        val t = Store.position(current.id).toInt()
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${current.id}&t=${t}s")))
    }

    val view = remember {
        YouTubePlayerView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFullscreenListener(object : FullscreenListener {
                override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                    (fullscreenView.parent as? ViewGroup)?.removeView(fullscreenView)
                    exitFs = exitFullscreen
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    activity?.window?.let { w ->
                        WindowCompat.getInsetsController(w, w.decorView).apply {
                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                    fsHolder.value = fullscreenView
                }
                override fun onExitFullscreen() {
                    exitFs = null
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    activity?.window?.let { w -> WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars()) }
                    fsHolder.value = null
                }
            })
            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) { player = youTubePlayer }
                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    cur.sec = second
                    val now = System.currentTimeMillis()
                    if (cur.id.isNotEmpty() && now - cur.lastSave > 5000) {
                        cur.lastSave = now
                        Store.save(s.id, cur.id, second, cur.dur, flush = now % 4 == 0L)
                    }
                }
                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) { cur.dur = duration }
                override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                    if (state == PlayerConstants.PlayerState.PAUSED) Store.save(s.id, cur.id, cur.sec, cur.dur, flush = true)
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        Store.save(s.id, cur.id, cur.dur, cur.dur, flush = true)
                        val i = s.videos.indexOfFirst { it.id == cur.id }
                        s.videos.getOrNull(i + 1)?.let { current = it }
                    }
                }
                override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                    this@apply.post {
                        errorHolder.value = (
                            when (error) {
                                PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER,
                                PlayerConstants.PlayerError.REQUEST_MISSING_HTTP_REFERER ->
                                    "Studio pozwala oglądać ten film tylko w aplikacji YouTube."
                                PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Ten film został usunięty z YouTube albo jest niedostępny w Twoim kraju."
                                else -> "Nie udało się odtworzyć filmu. Sprawdź internet albo otwórz go w YouTube."
                            }
                        )
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        lifecycle.addObserver(view)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (cur.id.isNotEmpty()) Store.save(s.id, cur.id, cur.sec, cur.dur, flush = true)
            lifecycle.removeObserver(view)
            view.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(player, current) {
        val p = player ?: return@LaunchedEffect
        if (cur.id.isNotEmpty() && cur.id != current.id) Store.save(s.id, cur.id, cur.sec, cur.dur, flush = true)
        error = null
        cur.id = current.id; cur.sec = 0f; cur.dur = current.duration.toFloat()
        val start = Store.position(current.id).let { if (Store.progress(current.id) > 0.97f) 0f else maxOf(0f, it - 3f) }
        p.loadVideo(current.id, start)
        Store.save(s.id, current.id, start, cur.dur, flush = true)
    }

    val fsv = fsHolder.value
    BackHandler(enabled = fsv != null) { exitFs?.invoke() }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White) }
            Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { Store.toggleFav(s.id); fav = Store.isFav(s.id) }) {
                Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Ulubione", tint = if (fav) C.Accent else Color.White)
            }
        }
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
            error?.let { msg ->
                Column(
                    Modifier.fillMaxSize().background(Color(0xEE000000)).padding(20.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(msg, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(22.dp)).background(C.Grad).clickable { openInYouTube() }.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Otwórz w YouTube", color = Color.White)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).background(C.Bg).navigationBarsPadding()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(s.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KindBadge(s)
                        Spacer(Modifier.width(8.dp))
                        Text(seriesMeta(s), color = C.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (s.kind == "partial") {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Studio udostępniło na YouTube tylko część tej historii. Reszta jest płatna w ich aplikacji.",
                            color = C.Gold, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.clickable { openInYouTube() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = C.Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Otwórz w YouTube", color = C.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (s.videos.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        Text(if (s.kind == "episodes") "Odcinki" else "Części", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            if (s.videos.size > 1) itemsIndexed(s.videos) { i, v ->
                val playing = v.id == current.id
                @Suppress("UNUSED_VARIABLE") val ver = Store.version
                val prog = Store.progress(v.id)
                Row(
                    Modifier.fillMaxWidth().clickable { current = v }.background(if (playing) C.Surface2 else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (playing) C.Accent else C.Surface2), contentAlignment = Alignment.Center) {
                        if (playing) Icon(Icons.Rounded.Equalizer, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        else Text("${i + 1}", color = C.Text, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = if (playing) C.Text else C.Muted)
                        if (prog > 0.01f) {
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(C.Surface2)) {
                                Box(Modifier.fillMaxWidth(prog.coerceIn(0f, 1f)).fillMaxHeight().background(C.Accent))
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(formatDuration(v.duration), color = C.Faint, style = MaterialTheme.typography.labelMedium)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (fsv != null) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.runtime.key(fsv) { AndroidView(factory = { fsv }, modifier = Modifier.fillMaxSize()) }
        }
    }
    }
}
