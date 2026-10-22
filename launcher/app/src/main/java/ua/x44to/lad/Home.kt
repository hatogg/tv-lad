@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package ua.x44to.lad

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.delay
import java.util.Calendar

// ───────── вигляд ─────────

class Wallpaper(val name: String, val colors: List<Color>, val accent: Color)

val WALLPAPERS = listOf(
    Wallpaper("Північ", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), Color(0xFF2EC4A6)),
    Wallpaper("Аврора", listOf(Color(0xFF0B1026), Color(0xFF1B2A5C), Color(0xFF1D6E73)), Color(0xFF7CF0C8)),
    Wallpaper("Захід", listOf(Color(0xFF1F1135), Color(0xFF5B2350), Color(0xFFB8455B)), Color(0xFFFFB38A)),
    Wallpaper("Ліс", listOf(Color(0xFF07201A), Color(0xFF0E3B2E), Color(0xFF2B5D3A)), Color(0xFFB6E388)),
    Wallpaper("Графіт", listOf(Color(0xFF0E0F12), Color(0xFF1A1C21), Color(0xFF2A2D35)), Color(0xFF8AB4F8)),
)

class KindLook(val icon: ImageVector, val from: Color, val to: Color)

fun look(k: Kind): KindLook = when (k) {
    Kind.PLAYSTATION -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF0A2A72), Color(0xFF1F6FE5))
    Kind.XBOX -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF0C4A16), Color(0xFF2FA84F))
    Kind.NINTENDO -> KindLook(Icons.Rounded.VideogameAsset, Color(0xFF7A0A10), Color(0xFFE4252C))
    Kind.PC -> KindLook(Icons.Rounded.Computer, Color(0xFF33186B), Color(0xFF7A55E8))
    Kind.LAPTOP -> KindLook(Icons.Rounded.Laptop, Color(0xFF22324D), Color(0xFF4F6D9A))
    Kind.TVBOX -> KindLook(Icons.Rounded.Cast, Color(0xFF5C300B), Color(0xFFE08A2C))
    Kind.MEDIA -> KindLook(Icons.Rounded.Movie, Color(0xFF4A1040), Color(0xFFC2378F))
    Kind.CAMERA -> KindLook(Icons.Rounded.Videocam, Color(0xFF243B3F), Color(0xFF4E8A8F))
    Kind.ANTENNA -> KindLook(Icons.Rounded.SettingsInputAntenna, Color(0xFF4D3A05), Color(0xFFC79A1E))
    Kind.HDMI -> KindLook(Icons.Rounded.SettingsInputHdmi, Color(0xFF2A2F3A), Color(0xFF4A5363))
}

private val White = Color(0xFFFFFFFF)
private val Dim = Color(0xB3FFFFFF)

// ───────── екран ─────────

private sealed class Menu {
    data class ForInput(val input: TvIn) : Menu()
    data class ForApp(val app: AppEntry) : Menu()
    data object Settings : Menu()
}

@Composable
fun LadHome(state: LadState, act: Activity) {
    val wp = WALLPAPERS[state.wallpaper.coerceIn(0, WALLPAPERS.size - 1)]
    var menu by remember { mutableStateOf<Menu?>(null) }
    val first = remember { FocusRequester() }
    BackHandler(enabled = menu == null) { /* це головний екран — «Назад» нікуди не веде */ }

    MaterialTheme(colorScheme = darkColorScheme(primary = wp.accent, surface = Color(0xFF1B1F27), onSurface = White)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(wp.colors)),
        ) {
            val shownInputs = state.inputs.filter { state.hidden[it.id] != true }
            val favApps = state.favorites.mapNotNull { p -> state.apps.firstOrNull { it.pkg == p } }
            val rest = state.apps.filter { it.pkg !in state.favorites }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Header(wp) { menu = Menu.Settings } }
                if (shownInputs.isNotEmpty()) {
                    item { Section("Входи") }
                    if (!state.hintShown && shownInputs.all { state.kinds[it.id] == Kind.HDMI || state.kinds[it.id] == Kind.ANTENNA }) {
                        item {
                            Text(
                                "Порада: натисніть і тримайте OK на вході, щоб позначити, що підключено — PlayStation, ПК…",
                                color = Dim, fontSize = 13.sp,
                            )
                        }
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                            items(shownInputs, key = { it.id }) { i ->
                                val m = if (i == shownInputs.first()) Modifier.focusRequester(first) else Modifier
                                InputTile(state, i, wp, m, onClick = { switchTo(act, i, state.switchMode) }, onLong = { menu = Menu.ForInput(i) })
                            }
                        }
                    }
                }
                if (favApps.isNotEmpty()) {
                    item { Section("Улюблене") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                            items(favApps, key = { it.key }) { a ->
                                val m = if (shownInputs.isEmpty() && a == favApps.first()) Modifier.focusRequester(first) else Modifier
                                AppTile(state, a, wp, m, onClick = { launch(act, a) }, onLong = { menu = Menu.ForApp(a) })
                            }
                        }
                    }
                }
                item { Section(if (favApps.isEmpty()) "Програми" else "Усі програми") }
                val rows = rest.chunked(5)
                items(rows.size) { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        rows[r].forEach { a ->
                            val m = if (shownInputs.isEmpty() && favApps.isEmpty() && r == 0 && a == rows[0].first()) Modifier.focusRequester(first) else Modifier
                            AppTile(state, a, wp, m, onClick = { launch(act, a) }, onLong = { menu = Menu.ForApp(a) })
                        }
                    }
                }
            }
        }
        LaunchedEffect(state.inputs.size, state.apps.size) {
            if (state.inputs.isNotEmpty() || state.apps.isNotEmpty()) {
                delay(120)
                try {
                    first.requestFocus()
                } catch (e: IllegalStateException) {
                    // ще не на екрані
                }
            }
        }
        when (val m = menu) {
            is Menu.ForInput -> InputMenu(state, m.input, wp) { menu = null }
            is Menu.ForApp -> AppMenu(state, m.app, act) { menu = null }
            Menu.Settings -> SettingsMenu(state, act) { menu = null }
            null -> {}
        }
    }
}

@Composable
private fun Header(wp: Wallpaper, onSettings: () -> Unit) {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000L * (60 - now.get(Calendar.SECOND)).coerceAtLeast(1))
        }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Ua.time(now), color = White, fontSize = 52.sp, fontWeight = FontWeight.Light)
            Text(Ua.date(now).replaceFirstChar { it.uppercase() }, color = Dim, fontSize = 16.sp)
        }
        Surface(
            onClick = onSettings,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0x33FFFFFF), contentColor = White,
                focusedContainerColor = wp.accent, focusedContentColor = Color(0xFF06231D),
            ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Налаштування", modifier = Modifier.align(Alignment.Center).size(26.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun InputTile(state: LadState, i: TvIn, wp: Wallpaper, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val k = state.kinds[i.id] ?: Kind.HDMI
    val lk = look(k)
    Card(
        onClick = onClick,
        onLongClick = onLong,
        modifier = modifier.width(200.dp).height(112.dp),
        shape = CardDefaults.shape(RoundedCornerShape(18.dp)),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, White), shape = RoundedCornerShape(18.dp))),
        glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = wp.accent.copy(alpha = 0.6f), elevation = 18.dp)),
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(lk.from, lk.to)))
                .padding(14.dp),
        ) {
            Icon(lk.icon, contentDescription = null, tint = White, modifier = Modifier.size(34.dp).align(Alignment.TopStart))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(state.nameOf(i), color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (k != Kind.HDMI) Text(i.label, color = Dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AppTile(state: LadState, a: AppEntry, wp: Wallpaper, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val art = state.art[a.key]
    Column(modifier.width(160.dp)) {
        Card(
            onClick = onClick,
            onLongClick = onLong,
            modifier = Modifier.width(160.dp).height(90.dp),
            shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
            scale = CardDefaults.scale(focusedScale = 1.1f),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, White), shape = RoundedCornerShape(14.dp))),
            glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = wp.accent.copy(alpha = 0.5f), elevation = 14.dp)),
            colors = CardDefaults.colors(containerColor = Color(0x26FFFFFF), focusedContainerColor = Color(0x40FFFFFF)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    art != null && art.second -> Image(art.first, contentDescription = a.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    art != null -> Image(art.first, contentDescription = a.label, modifier = Modifier.size(54.dp))
                    else -> Text(a.label.take(1).uppercase(), color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(a.label, color = Dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────── меню (довге натискання OK) ─────────

@Composable
private fun MenuBox(title: String, sub: String? = null, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xF21B1F27), contentColor = White),
            modifier = Modifier.width(520.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (sub != null) Text(sub, color = Dim, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                content()
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector?, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(46.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color(0x332EC4A6) else Color(0x14FFFFFF), contentColor = White,
            focusedContainerColor = White, focusedContentColor = Color(0xFF111418),
        ),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InputMenu(state: LadState, i: TvIn, wp: Wallpaper, onDismiss: () -> Unit) {
    val cur = state.kinds[i.id] ?: Kind.HDMI
    val fr = remember { FocusRequester() }
    MenuBox(state.nameOf(i), "${i.label} · що підключено до цього входу?", onDismiss) {
        LazyColumn(Modifier.height(330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Kind.entries.size) { n ->
                val k = Kind.entries[n]
                val m = if (k == cur) Modifier.focusRequester(fr) else Modifier
                MenuItem(if (k == Kind.HDMI) "Звичайний вхід (${i.label})" else k.title, look(k).icon, m, selected = k == cur) {
                    state.setKind(i.id, k)
                    onDismiss()
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
            item { MenuItem("Перемістити ліворуч", Icons.Rounded.ChevronLeft) { state.moveInput(i.id, -1); onDismiss() } }
            item { MenuItem("Перемістити праворуч", Icons.Rounded.ChevronRight) { state.moveInput(i.id, 1); onDismiss() } }
            item { MenuItem("Сховати цей вхід", Icons.Rounded.VisibilityOff) { state.setHidden(i.id, true); onDismiss() } }
        }
    }
    LaunchedEffect(Unit) {
        delay(100)
        try { fr.requestFocus() } catch (e: IllegalStateException) { }
    }
}

@Composable
private fun AppMenu(state: LadState, a: AppEntry, act: Activity, onDismiss: () -> Unit) {
    val fav = state.isFavorite(a.pkg)
    MenuBox(a.label, a.pkg, onDismiss) {
        MenuItem(if (fav) "Прибрати з «Улюбленого»" else "Додати в «Улюблене»", if (fav) Icons.Rounded.StarBorder else Icons.Rounded.Star) {
            state.toggleFavorite(a.pkg)
            onDismiss()
        }
        if (fav) {
            MenuItem("Перемістити ліворуч", Icons.Rounded.ChevronLeft) { state.moveFavorite(a.pkg, -1); onDismiss() }
            MenuItem("Перемістити праворуч", Icons.Rounded.ChevronRight) { state.moveFavorite(a.pkg, 1); onDismiss() }
        }
        MenuItem("Про програму (видалити, очистити)", Icons.Rounded.Info) { onDismiss(); openAppInfo(act, a.pkg) }
    }
}

@Composable
private fun SettingsMenu(state: LadState, act: Activity, onDismiss: () -> Unit) {
    val hiddenInputs = state.inputs.filter { state.hidden[it.id] == true }
    MenuBox("Налаштування «Лад»", null, onDismiss) {
        LazyColumn(Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("Шпалери", color = Dim, fontSize = 13.sp) }
            items(WALLPAPERS.size) { n ->
                val w = WALLPAPERS[n]
                Surface(
                    onClick = { state.pickWallpaper(n) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, White), shape = RoundedCornerShape(12.dp))),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                ) {
                    Row(
                        Modifier.fillMaxSize().background(Brush.horizontalGradient(w.colors)).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(w.name, color = White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Box(Modifier.size(14.dp).background(w.accent, CircleShape))
                        if (state.wallpaper == n) {
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Text("Як перемикати входи HDMI", color = Dim, fontSize = 13.sp) }
            val modes = listOf("Автоматично (радимо)", "Лише через системний ТВ", "Вбудований екран «Лад»")
            items(modes.size) { n ->
                MenuItem(modes[n], null, selected = state.switchMode == n) { state.setMode(n) }
            }
            if (hiddenInputs.isNotEmpty()) {
                item { Spacer(Modifier.height(4.dp)) }
                item { Text("Сховані входи", color = Dim, fontSize = 13.sp) }
                items(hiddenInputs, key = { it.id }) { i ->
                    MenuItem("Показати: ${state.nameOf(i)}", Icons.Rounded.Visibility) { state.setHidden(i.id, false) }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { MenuItem("Системні налаштування телевізора", Icons.Rounded.Settings) { onDismiss(); openSystemSettings(act) } }
            item {
                val v = try { act.packageManager.getPackageInfo(act.packageName, 0).versionName } catch (e: Exception) { "" }
                Text("«Лад» $v · встановлено з «ТВ Лад». Повернути стандартний головний екран можна там само.", color = Dim, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
