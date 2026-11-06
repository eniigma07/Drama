package com.drama.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.drama.app.data.Catalog
import com.drama.app.data.Genres
import com.drama.app.data.Series
import com.drama.app.data.Store
import com.drama.app.data.formatViews
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kolejność „Dla Ciebie”: pełne serie najpierw, studia przemieszane, nieoglądane wyżej. */
fun feedOrder(): List<Series> {
    val full = Catalog.series.filter { it.isFull }
    val rest = Catalog.series.filter { !it.isFull }
    fun mix(list: List<Series>): List<Series> {
        val by = list.groupBy { it.studio }.mapValues { (_, v) -> v.sortedByDescending { it.views }.toMutableList() }
        val out = ArrayList<Series>()
        while (by.values.any { it.isNotEmpty() }) by.values.forEach { if (it.isNotEmpty()) out += it.removeAt(0) }
        return out
    }
    val f = mix(full)
    val unwatched = f.filter { Store.seriesProgress(it) < 0.05f }
    val watched = f.filter { Store.seriesProgress(it) >= 0.05f }
    return unwatched + watched + mix(rest)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedTab(open: (Series) -> Unit, play: (Series) -> Unit) {
    val items = remember(Catalog.generated) { feedOrder() }
    if (items.isEmpty()) { EmptyCatalog(); return }
    val pager = rememberPagerState { items.size }
    VerticalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
        FeedPage(items[page], open, play)
    }
}

@Composable
private fun FeedPage(s: Series, open: (Series) -> Unit, play: (Series) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var fav by remember(s.id) { mutableStateOf(Store.isFav(s.id)) }
    val prog = Store.seriesProgress(s)
    Box(Modifier.fillMaxSize().background(Color.Black).clickable { play(s) }) {
        AsyncImage(
            model = s.thumbOrDefault(), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(28.dp),
        )
        Box(Modifier.fillMaxSize().background(Color(0x99000000)))
        AsyncImage(
            model = s.hiThumb(), contentDescription = s.title, contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp).fillMaxWidth().aspectRatio(16f / 9f),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xF0000000))))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 18.dp, end = 80.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindBadge(s)
                Spacer(Modifier.width(8.dp))
                Text(s.studio, color = C.Muted, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(s.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(seriesMeta(s) + (if (s.views > 0) " · ${formatViews(s.views)} wyśw." else ""), color = C.Muted, style = MaterialTheme.typography.bodyMedium)
            if (s.genres.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.genres.take(3).forEach { g ->
                        Text(g, color = C.Text, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            GradientButton(if (prog > 0.02f) "Oglądaj dalej" else "Oglądaj") { play(s) }
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SideAction(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Moja lista", if (fav) C.Accent else Color.White) {
                Store.toggleFav(s.id); fav = Store.isFav(s.id)
            }
            SideAction(Icons.Rounded.Info, "Szczegóły", Color.White) { open(s) }
        }
    }
}

@Composable
private fun SideAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x44000000)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun EmptyCatalog() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Katalog jest pusty. Połącz się z internetem i otwórz aplikację ponownie.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}

@Composable
fun DiscoverTab(open: (Series) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    var studio by rememberSaveable { mutableStateOf<String?>(null) }
    val filtering = query.isNotBlank() || genre != null || studio != null

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Szukaj tytułu, np. billionaire") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Wyczyść") } },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = C.Accent,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Catalog.studios()) { st -> Chip(st, studio == st) { studio = if (studio == st) null else st } }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Genres.all) { g -> Chip(g, genre == g) { genre = if (genre == g) null else g } }
        }
        if (filtering) {
            val q = query.trim().lowercase()
            val res = Catalog.series.filter { s ->
                (q.isEmpty() || s.title.lowercase().contains(q)) &&
                    (genre == null || genre in s.genres) && (studio == null || s.studio == studio)
            }.sortedWith(compareBy({ if (it.isFull) 0 else 1 }, { -it.views }))
            Text("Wyniki: ${res.size}", color = C.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(res, key = { it.id }) { s -> PosterCard(s, width = 200) { open(s) } }
            }
        } else {
            val full = remember(Catalog.generated) { Catalog.series.filter { it.isFull }.sortedByDescending { it.views } }
            val partial = remember(Catalog.generated) { Catalog.series.filter { !it.isFull }.sortedByDescending { it.views } }
            LazyColumn(Modifier.fillMaxSize()) {
                item { Rail("Pełne serie: najmniej reklam", full.take(30), open) }
                Genres.all.forEach { g ->
                    val list = full.filter { g in it.genres }
                    if (list.size >= 3) item(key = "g$g") { Rail(g, list.take(25), open) }
                }
                Catalog.studios().forEach { st ->
                    val list = full.filter { it.studio == st }
                    if (list.isNotEmpty()) item(key = "s$st") { Rail("Od $st", list.take(25), open) }
                }
                if (partial.isNotEmpty()) item { Rail("Niepełne (tylko część na YouTube)", partial.take(30), open) }
                item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (selected) Color.White else C.Muted, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) C.Accent else C.Surface)
            .border(1.dp, if (selected) C.Accent else C.Surface2, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun Rail(title: String, list: List<Series>, open: (Series) -> Unit) {
    Column {
        SectionTitle(title)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { s -> PosterCard(s) { open(s) } }
        }
    }
}

@Composable
fun MyListTab(open: (Series) -> Unit, play: (Series) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val cont = Store.continueWatching()
    val favs = Store.favorites()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        item { Text("Moja lista", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 14.dp)) }
        if (cont.isNotEmpty()) {
            item { Text("Oglądaj dalej", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) }
            items(cont, key = { "c" + it.first.id }) { (s, _) ->
                SeriesRow(s, "${(Store.seriesProgress(s) * 100).toInt()}% obejrzane · ${s.studio}") { play(s) }
            }
        }
        item { Text("Ulubione", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) }
        if (favs.isEmpty()) item {
            Text("Stuknij serduszko przy serii, żeby ją tu zapisać.", color = C.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        items(favs, key = { "f" + it.id }) { s -> SeriesRow(s) { open(s) } }
        item {
            Spacer(Modifier.height(28.dp))
            val date = if (Catalog.generated > 0) SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("pl")).format(Date(Catalog.generated * 1000)) else "–"
            Text("Biblioteka: ${Catalog.series.size} serii, aktualizacja $date", color = C.Faint, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Źródło: oficjalne kanały YouTube studiów. Nowe serie dochodzą same codziennie.",
                color = C.Faint, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).background(C.Surface).clickable(enabled = !refreshing) {
                    refreshing = true
                    scope.launch {
                        val ok = Catalog.refresh()
                        info = if (ok) "Biblioteka zaktualizowana." else "Masz już najnowszą bibliotekę."
                        refreshing = false
                    }
                }.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (refreshing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = C.Accent)
                else Icon(Icons.Rounded.Refresh, null, tint = C.Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sprawdź nowe serie", color = C.Text, style = MaterialTheme.typography.bodyMedium)
            }
            info?.let { Text(it, color = C.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun SeriesScreen(id: String, onBack: () -> Unit, play: (Series, String?) -> Unit) {
    val s = Catalog.byId(id) ?: run { onBack(); return }
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var fav by remember { mutableStateOf(Store.isFav(s.id)) }
    val prog = Store.seriesProgress(s)
    LazyColumn(Modifier.fillMaxSize().background(C.Bg)) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
                AsyncImage(model = s.hiThumb(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to C.Bg)))
                IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(4.dp).clip(CircleShape).background(Color(0x66000000))) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", tint = Color.White)
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(s.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KindBadge(s)
                    Spacer(Modifier.width(8.dp))
                    Text(seriesMeta(s), color = C.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                if (s.genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.genres.joinToString(" · "), color = C.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                if (s.kind == "partial") {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Studio udostępniło na YouTube tylko część tej historii. Reszta jest płatna w ich aplikacji.",
                        color = C.Gold, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientButton(if (prog > 0.02f) "Oglądaj dalej" else "Oglądaj", Modifier.weight(1f)) { play(s, null) }
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = { Store.toggleFav(s.id); fav = Store.isFav(s.id) }, modifier = Modifier.clip(CircleShape).background(C.Surface)) {
                        Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Moja lista", tint = if (fav) C.Accent else Color.White)
                    }
                }
                if (s.videos.size > 1) {
                    Text(if (s.kind == "episodes") "Odcinki (${s.videos.size})" else "Części (${s.videos.size})",
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
                }
            }
        }
        if (s.videos.size > 1) items(s.videos, key = { it.id }) { vid ->
            val p = Store.progress(vid.id)
            Row(Modifier.fillMaxWidth().clickable { play(s, vid.id) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(C.Surface2)) {
                    AsyncImage(model = "https://i.ytimg.com/vi/${vid.id}/mqdefault.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    if (p > 0.01f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p.coerceIn(0f, 1f)).height(3.dp).background(C.Accent))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(vid.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(com.drama.app.data.formatDuration(vid.duration), color = C.Faint, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item { Spacer(Modifier.height(30.dp).navigationBarsPadding()) }
    }
}
