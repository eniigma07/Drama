package com.drama.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.drama.app.data.Catalog
import com.drama.app.data.Series
import com.drama.app.data.Store
import com.drama.app.ui.AppTheme
import com.drama.app.ui.C
import com.drama.app.ui.DiscoverTab
import com.drama.app.ui.FeedTab
import com.drama.app.ui.MyListTab
import com.drama.app.ui.PlayerScreen
import com.drama.app.ui.SeriesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        Catalog.loadLocal(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(C.Surface.toArgb()),
        )
        setContent { AppTheme { Box(Modifier.fillMaxSize().background(C.Bg)) { App() } } }
    }

    override fun onPause() {
        super.onPause()
        Store.persist()
    }
}

@Composable
private fun App() {
    val nav = rememberNavController()
    LaunchedEffect(Unit) { Catalog.refresh() }
    val open: (Series) -> Unit = { nav.navigate("series/" + Uri.encode(it.id)) }
    val play: (Series, String?) -> Unit = { s, v -> nav.navigate("play/" + Uri.encode(s.id) + "?v=" + (v ?: "")) }
    NavHost(nav, startDestination = "home") {
        composable("home") { Home(open) { play(it, null) } }
        composable("series/{id}") { e ->
            SeriesScreen(Uri.decode(e.arguments?.getString("id")!!), onBack = { nav.popBackStack() }, play = play)
        }
        composable(
            "play/{id}?v={v}",
            arguments = listOf(navArgument("v") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { e ->
            val v = e.arguments?.getString("v")?.ifBlank { null }
            PlayerScreen(Uri.decode(e.arguments?.getString("id")!!), v) { nav.popBackStack() }
        }
    }
}

@Composable
private fun Home(open: (Series) -> Unit, play: (Series) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> FeedTab(open, play)
                1 -> DiscoverTab(open)
                else -> MyListTab(open, play)
            }
        }
        NavigationBar(containerColor = C.Surface, tonalElevation = 0.dp) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = C.Accent, selectedTextColor = C.Accent, indicatorColor = C.Accent.copy(alpha = 0.15f),
                unselectedIconColor = C.Muted, unselectedTextColor = C.Muted,
            )
            NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Rounded.PlayCircle, null) }, label = { Text("Dla Ciebie") }, colors = colors)
            NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Rounded.Explore, null) }, label = { Text("Odkrywaj") }, colors = colors)
            NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Rounded.Bookmarks, null) }, label = { Text("Moja lista") }, colors = colors)
        }
    }
}
