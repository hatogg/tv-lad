@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package ua.x44to.lad

import android.app.Activity
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.delay
import java.util.Calendar

// ───────── теми: м'які кольорові плями на тлі, як на Apple TV ─────────

class Orb(val color: Color, val x: Float, val y: Float, val r: Float)

class Wallpaper(val name: String, val base: Color, val orbs: List<Orb>, val light: Boolean = false) {
    val text: Color get() = if (light) Color(0xFF1C1C1E) else Color.White
    val dim: Color get() = if (light) Color(0x991C1C1E) else Color(0xA6FFFFFF)
    val glass: Color get() = if (light) Color(0x8CFFFFFF) else Color(0x1FFFFFFF)
}

val WALLPAPERS = listOf(
    Wallpaper("Сутінки", Color(0xFF0D0F15), listOf(
        Orb(Color(0xFF1E9E8C), 0.12f, 0.05f, 0.55f), Orb(Color(0xFF4B4BC8), 0.88f, 0.12f, 0.55f), Orb(Color(0xFFA0307A), 0.62f, 1.0f, 0.6f))),
    Wallpaper("Океан", Color(0xFF06111F), listOf(
        Orb(Color(0xFF1F5FD0), 0.1f, 0.1f, 0.6f), Orb(Color(0xFF1AA39A), 0.9f, 0.3f, 0.55f), Orb(Color(0xFF0B2D7A), 0.5f, 1.0f, 0.65f))),
    Wallpaper("Захід", Color(0xFF150A18), listOf(
        Orb(Color(0xFFE0483F), 0.15f, 0.1f, 0.55f), Orb(Color(0xFFF2A048), 0.85f, 0.15f, 0.5f), Orb(Color(0xFF6E2A86), 0.55f, 1.0f, 0.65f))),
    Wallpaper("Ліс", Color(0xFF06120D), listOf(
        Orb(Color(0xFF237A4C), 0.12f, 0.08f, 0.6f), Orb(Color(0xFF7FB342), 0.88f, 0.2f, 0.45f), Orb(Color(0xFF0D5446), 0.5f, 1.0f, 0.65f))),
    Wallpaper("Графіт", Color(0xFF111214), listOf(
        Orb(Color(0xFF3A3E48), 0.15f, 0.1f, 0.6f), Orb(Color(0xFF2B2E36), 0.85f, 0.9f, 0.6f))),
    Wallpaper("Ранок", Color(0xFFF3F4F8), listOf(
        Orb(Color(0xFF9CCBFF), 0.1f, 0.1f, 0.55f), Orb(Color(0xFFFFC7DD), 0.9f, 0.2f, 0.5f), Orb(Color(0xFFB9EBD7), 0.5f, 1.0f, 0.6f)), light = true),
)

class KindLook(val icon: ImageVector, val from: Color, val to: Color)

fun look(k: Kind): KindLook = when (k) {
    Kind.PLAYSTATION -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF0B3BA8), Color(0xFF2F86FF))
    Kind.XBOX -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF0E6B22), Color(0xFF3BC764))
    Kind.NINTENDO -> KindLook(Icons.Rounded.VideogameAsset, Color(0xFFB0121B), Color(0xFFFF4A50))
    Kind.PC -> KindLook(Icons.Rounded.Computer, Color(0xFF4A23A8), Color(0xFF9C7CFF))
    Kind.LAPTOP -> KindLook(Icons.Rounded.Laptop, Color(0xFF2F4672), Color(0xFF6F97D6))
    Kind.TVBOX -> KindLook(Icons.Rounded.Cast, Color(0xFF9A4A0C), Color(0xFFFFA94D))
    Kind.MEDIA -> KindLook(Icons.Rounded.Movie, Color(0xFF7A1561), Color(0xFFE85AB4))
    Kind.CAMERA -> KindLook(Icons.Rounded.Videocam, Color(0xFF2E5357), Color(0xFF6FB6BB))
    Kind.ANTENNA -> KindLook(Icons.Rounded.SettingsInputAntenna, Color(0xFF8A6200), Color(0xFFF2C230))
    Kind.HDMI -> KindLook(Icons.Rounded.SettingsInputHdmi, Color(0xFF3A3A40), Color(0xFF6E6E78))
}

private fun Color.lighten(f: Float) = lerp(this, Color.White, f)
private fun Color.darken(f: Float) = lerp(this, Color.Black, f)

/** Колір плитки для програми без банера: з її значка, але не надто темний і не надто світлий. */
private fun tileColor(c: Color): Color = when {
    c.luminance() < 0.08f -> c.lighten(0.28f)
    c.luminance() > 0.7f -> c.darken(0.35f)
    else -> c
}

// ───────── пульт: натискання й утримання OK ─────────

private const val LONG_MS = 450L

/**
 * Натискання OK — дія, утримання (за часом, навіть якщо пульт не повторює кнопку) або кнопка Menu — меню.
 * Відпускання OK, яке почалося деінде (напр. до відкриття меню), ігнорується.
 */
private fun Modifier.tvPress(onClick: () -> Unit, onLong: () -> Unit): Modifier = composed {
    var downAt by remember { mutableLongStateOf(-1L) }
    var longFired by remember { mutableStateOf(false) }
    onPreviewKeyEvent { e ->
        val k = e.nativeKeyEvent
        when (k.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                if (k.action == KeyEvent.ACTION_DOWN) {
                    if (k.repeatCount == 0) {
                        downAt = k.eventTime
                        longFired = false
                    } else if (!longFired && downAt >= 0 && (k.isLongPress || k.eventTime - downAt >= LONG_MS)) {
                        longFired = true
                        onLong()
                    }
                } else if (k.action == KeyEvent.ACTION_UP) {
                    if (downAt >= 0 && !longFired) {
                        if (k.eventTime - downAt >= LONG_MS) onLong() else onClick()
                    }
                    downAt = -1L
                    longFired = false
                }
                true
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_SETTINGS -> {
                if (k.action == KeyEvent.ACTION_UP) onLong()
                true
            }
            else -> false
        }
    }
}

// ───────── головний екран ─────────

private sealed interface Focus {
    data class In(val input: TvIn) : Focus
    data class App(val app: AppEntry) : Focus
    data object LadSettings : Focus
    data object TvSettings : Focus
}

private sealed interface Menu {
    data class ForInput(val input: TvIn) : Menu
    data class ForApp(val app: AppEntry) : Menu
    data object Settings : Menu
}

private val TileShape = RoundedCornerShape(16.dp)

@Composable
fun LadHome(state: LadState, act: Activity) {
    val wp = WALLPAPERS[state.wallpaper.coerceIn(0, WALLPAPERS.size - 1)]
    var menu by remember { mutableStateOf<Menu?>(null) }
    var focus by remember { mutableStateOf<Focus?>(null) }
    val first = remember { FocusRequester() }
    BackHandler(enabled = menu == null) { /* головний екран: «Назад» нікуди не веде */ }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, surface = Color(0xFF2C2C2E), onSurface = Color.White)) {
        val backdrop = remember(wp) { backdropBitmap(wp) }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(wp.base),
        ) {
            // тло малюється раз у маленьку картинку (м'які плями добре розтягуються) — це майже нічого не коштує
            Image(backdrop, null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            val side = maxWidth * 0.06f
            val gap = 24.dp
            val tileW: Dp = ((maxWidth - side * 2 - gap * 4) / 5).coerceIn(110.dp, 220.dp)
            val tileH: Dp = tileW * 0.6f
            val shelfH: Dp = maxHeight * 0.34f

            val shownInputs = state.inputs.filter { state.hidden[it.id] != true }
            val favApps = state.favorites.mapNotNull { p -> state.apps.firstOrNull { it.pkg == p } }
            val rest = state.apps.filter { it.pkg !in state.favorites && it.pkg !in state.hiddenApps }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 26.dp, bottom = 60.dp)) {
                item { Header(wp, side) }
                item { Shelf(state, focus, wp, side, shelfH) }
                if (shownInputs.isNotEmpty()) {
                    item { Caption("Входи", wp, side) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = side, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            items(shownInputs, key = { "in:" + it.id }) { i ->
                                val m = if (i == shownInputs.first()) Modifier.focusRequester(first) else Modifier
                                InputTile(state, i, wp, tileW, tileH, m, onFocus = { focus = Focus.In(i) },
                                    onClick = { switchTo(act, i, state.switchMode) }, onLong = { menu = Menu.ForInput(i) })
                            }
                        }
                    }
                }
                if (favApps.isNotEmpty()) {
                    item { Caption("Улюблене", wp, side) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = side, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            items(favApps, key = { "fav:" + it.key }) { a ->
                                val m = if (shownInputs.isEmpty() && a == favApps.first()) Modifier.focusRequester(first) else Modifier
                                AppTile(state, a, wp, tileW, tileH, m, onFocus = { focus = Focus.App(a) },
                                    onClick = { launch(act, a) }, onLong = { menu = Menu.ForApp(a) })
                            }
                        }
                    }
                }
                item { Caption("Програми", wp, side) }
                val cells: List<Any> = rest + listOf(Focus.TvSettings, Focus.LadSettings)
                val rows = cells.chunked(5)
                items(rows.size) { r ->
                    Row(Modifier.padding(horizontal = side, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (c in rows[r]) {
                            val isFirst = shownInputs.isEmpty() && favApps.isEmpty() && r == 0 && c == rows[0].first()
                            val m = if (isFirst) Modifier.focusRequester(first) else Modifier
                            when (c) {
                                is AppEntry -> AppTile(state, c, wp, tileW, tileH, m, onFocus = { focus = Focus.App(c) },
                                    onClick = { launch(act, c) }, onLong = { menu = Menu.ForApp(c) })
                                Focus.TvSettings -> IconTile("Налаштування ТВ", Icons.Rounded.Settings, Color(0xFF55555C), Color(0xFF8E8E96), wp, tileW, tileH, m,
                                    onFocus = { focus = Focus.TvSettings }, onClick = { openSystemSettings(act) })
                                else -> IconTile("Налаштування «Лад»", Icons.Rounded.Tune, Color(0xFF1E8E7E), Color(0xFF46D3B8), wp, tileW, tileH, m,
                                    onFocus = { focus = Focus.LadSettings }, onClick = { menu = Menu.Settings })
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(state.inputs.size, state.apps.size) {
            if (state.inputs.isNotEmpty() || state.apps.isNotEmpty()) {
                delay(150)
                try {
                    first.requestFocus()
                } catch (e: IllegalStateException) {
                    // ще не на екрані
                }
            }
        }
        when (val m = menu) {
            is Menu.ForInput -> InputMenu(state, m.input) { menu = null }
            is Menu.ForApp -> AppMenu(state, m.app, act) { menu = null }
            Menu.Settings -> SettingsMenu(state, act, onInput = { menu = Menu.ForInput(it) }) { menu = null }
            null -> {}
        }
    }
}

@Composable
private fun Header(wp: Wallpaper, side: Dp) {
    val now = rememberNow()
    Row(Modifier.fillMaxWidth().padding(horizontal = side).padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Добр${greeting(now)}", color = wp.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(Ua.date(now).replaceFirstChar { it.uppercase() }, color = wp.dim, fontSize = 15.sp)
        }
        Box(
            Modifier
                .background(wp.glass, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(Ua.time(now), color = wp.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun rememberNow(): Calendar {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000L * (60 - now.get(Calendar.SECOND)).coerceAtLeast(1))
        }
    }
    return now
}

private fun greeting(c: Calendar): String = when (c.get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "ого ранку"
    in 12..17 -> "ий день"
    else -> "ий вечір"
}

@Composable
private fun Caption(text: String, wp: Wallpaper, side: Dp) {
    Text(text, color = wp.dim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = side, top = 14.dp))
}

// ───────── вітрина ─────────

@Composable
private fun Shelf(state: LadState, focus: Focus?, wp: Wallpaper, side: Dp, height: Dp) {
    Box(
        Modifier
            .padding(horizontal = side)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        val f = focus
        run {
            Box(Modifier.fillMaxSize()) {
                when (f) {
                    is Focus.In -> {
                        val k = state.kinds[f.input.id] ?: Kind.HDMI
                        val lk = look(k)
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lk.from.darken(0.25f), lk.to))))
                        Icon(lk.icon, null, tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp).size(height * 0.55f))
                        ShelfText(if (k == Kind.HDMI) "Вхід" else "Вхід · ${f.input.label}", state.nameOf(f.input), "Натисніть OK — і телевізор перемкнеться")
                    }
                    is Focus.App -> {
                        val art = state.art[f.app.key]
                        val c = tileColor(Color(art?.color ?: 0xFF3A3A40.toInt()))
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c.darken(0.55f), c.darken(0.15f)))))
                        if (art != null) {
                            if (art.banner) {
                                Image(art.image, f.app.label, contentScale = ContentScale.Crop, modifier = Modifier
                                    .align(Alignment.CenterEnd).padding(end = 36.dp)
                                    .height(height * 0.72f).width(height * 0.72f * 16f / 9f)
                                    .clip(RoundedCornerShape(18.dp)))
                            } else {
                                Image(art.image, f.app.label, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 70.dp).size(height * 0.6f))
                            }
                        }
                        ShelfText("Програма", f.app.label, if (state.isFavorite(f.app.pkg)) "В «Улюбленому»" else "Утримуйте OK або Menu — меню")
                    }
                    Focus.TvSettings -> {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2A2A2E), Color(0xFF5A5A62)))))
                        Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 60.dp).size(height * 0.55f))
                        ShelfText("Система", "Налаштування телевізора", "Мережа, звук, зображення, джерела")
                    }
                    Focus.LadSettings -> {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0E4D44), Color(0xFF2EC4A6)))))
                        Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 60.dp).size(height * 0.55f))
                        ShelfText("Лад", "Налаштування екрана", "Тло, входи HDMI, як перемикати")
                    }
                    null -> {
                        Box(Modifier.fillMaxSize().background(wp.glass))
                        ShelfText("Лад", "Ласкаво просимо", "Оберіть вхід чи програму пультом", if (wp.light) wp.text else Color.White)
                    }
                }
                // легкий «скляний» відблиск угорі картки
                Box(Modifier.fillMaxWidth().height(height * 0.5f).background(Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color.Transparent))))
            }
        }
    }
}

@Composable
private fun BoxScope.ShelfText(kicker: String, title: String, sub: String, color: Color = Color.White) {
    Column(Modifier.align(Alignment.CenterStart).padding(start = 36.dp).fillMaxWidth(0.55f)) {
        Text(kicker.uppercase(), color = color.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = color, fontSize = 36.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = color.copy(alpha = 0.8f), fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ───────── плитки ─────────

/** Плитка: на фокусі збільшується з апаратною тінню й легким блиском; назва — лише під вибраною. Без важких ефектів. */
@Composable
private fun Tile(
    label: String, wp: Wallpaper, w: Dp, h: Dp, modifier: Modifier,
    onFocus: () -> Unit, onClick: () -> Unit, onLong: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    Column(modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(w).height(h)
                .graphicsLayer {
                    shadowElevation = if (focused) 14.dp.toPx() else 0f
                    shape = TileShape
                    clip = false
                }
                .tvPress(onClick, onLong),
            shape = ClickableSurfaceDefaults.shape(TileShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            interactionSource = src,
        ) {
            Box(Modifier.fillMaxSize().clip(TileShape)) {
                content()
                if (focused) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color.Transparent))))
            }
        }
        Text(label, color = wp.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp).alpha(if (focused) 1f else 0f))
    }
}

@Composable
private fun InputTile(state: LadState, i: TvIn, wp: Wallpaper, w: Dp, h: Dp, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit, onLong: () -> Unit) {
    val k = state.kinds[i.id] ?: Kind.HDMI
    val lk = look(k)
    Tile(state.nameOf(i), wp, w, h, modifier, onFocus, onClick, onLong) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lk.from, lk.to))))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(lk.icon, null, tint = Color.White, modifier = Modifier.size(h * 0.38f))
            Spacer(Modifier.height(2.dp))
            Text(state.nameOf(i), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppTile(state: LadState, a: AppEntry, wp: Wallpaper, w: Dp, h: Dp, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit, onLong: () -> Unit) {
    val art = state.art[a.key]
    Tile(a.label, wp, w, h, modifier, onFocus, onClick, onLong) {
        when {
            art != null && art.banner -> Image(art.image, a.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            art != null -> {
                val c = tileColor(Color(art.color))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.lighten(0.18f), c.darken(0.22f)))))
                Image(art.image, a.label, modifier = Modifier.size(h * 0.58f).align(Alignment.Center))
            }
            else -> {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF4A4A52), Color(0xFF2E2E34)))))
                Text(a.label.take(1).uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun IconTile(label: String, icon: ImageVector, from: Color, to: Color, wp: Wallpaper, w: Dp, h: Dp, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit) {
    Tile(label, wp, w, h, modifier, onFocus, onClick, onClick) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(to, from))))
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(h * 0.42f).align(Alignment.Center))
    }
}

// ───────── меню ─────────

@Composable
private fun MenuBox(title: String, sub: String? = null, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xF22A2A2E), contentColor = Color.White),
            modifier = Modifier.width(500.dp),
        ) {
            Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (sub != null) Text(sub, color = Color(0x99FFFFFF), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                content()
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector?, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        // tvPress: спрацьовує лише на натискання, що почалося тут, — «хвіст» утримання OK, яким відкрили меню, не вибирає пункт
        modifier = modifier.fillMaxWidth().height(48.dp).tvPress(onClick, onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x1AFFFFFF), contentColor = Color.White,
            focusedContainerColor = Color.White, focusedContentColor = Color(0xFF1C1C1E),
        ),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun FocusFirst(fr: FocusRequester) {
    LaunchedEffect(Unit) {
        delay(120)
        try { fr.requestFocus() } catch (e: IllegalStateException) { }
    }
}

@Composable
private fun InputMenu(state: LadState, i: TvIn, onDismiss: () -> Unit) {
    val cur = state.kinds[i.id] ?: Kind.HDMI
    val fr = remember { FocusRequester() }
    MenuBox(state.nameOf(i), "${i.label} — що підключено до цього входу?", onDismiss) {
        LazyColumn(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Kind.entries.size) { n ->
                val k = Kind.entries[n]
                val m = if (k == cur) Modifier.focusRequester(fr) else Modifier
                MenuItem(if (k == Kind.HDMI) "Звичайний вхід (${i.label})" else k.title, look(k).icon, m, selected = k == cur) {
                    state.setKind(i.id, k)
                    onDismiss()
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { MenuItem("Перемістити ліворуч", Icons.Rounded.ChevronLeft) { state.moveInput(i.id, -1); onDismiss() } }
            item { MenuItem("Перемістити праворуч", Icons.Rounded.ChevronRight) { state.moveInput(i.id, 1); onDismiss() } }
            item { MenuItem("Сховати цей вхід", Icons.Rounded.VisibilityOff) { state.setHidden(i.id, true); onDismiss() } }
        }
    }
    FocusFirst(fr)
}

@Composable
private fun AppMenu(state: LadState, a: AppEntry, act: Activity, onDismiss: () -> Unit) {
    val fav = state.isFavorite(a.pkg)
    val fr = remember { FocusRequester() }
    MenuBox(a.label, null, onDismiss) {
        MenuItem(if (fav) "Прибрати з «Улюбленого»" else "Додати в «Улюблене»", if (fav) Icons.Rounded.StarBorder else Icons.Rounded.Star,
            Modifier.focusRequester(fr)) {
            state.toggleFavorite(a.pkg)
            onDismiss()
        }
        if (fav) {
            MenuItem("Перемістити ліворуч", Icons.Rounded.ChevronLeft) { state.moveFavorite(a.pkg, -1); onDismiss() }
            MenuItem("Перемістити праворуч", Icons.Rounded.ChevronRight) { state.moveFavorite(a.pkg, 1); onDismiss() }
        }
        MenuItem("Сховати з екрана", Icons.Rounded.VisibilityOff) {
            state.toggleHiddenApp(a.pkg)
            onDismiss()
        }
        MenuItem("Про програму (видалити, очистити)", Icons.Rounded.Info) { onDismiss(); openAppInfo(act, a.pkg) }
    }
    FocusFirst(fr)
}

@Composable
private fun SettingsMenu(state: LadState, act: Activity, onInput: (TvIn) -> Unit, onDismiss: () -> Unit) {
    val hiddenInputs = state.inputs.filter { state.hidden[it.id] == true }
    val shown = state.inputs.filter { state.hidden[it.id] != true }
    val fr = remember { FocusRequester() }
    MenuBox("Налаштування «Лад»", "Кнопка Menu або утримання OK на плитці — її меню", onDismiss) {
        LazyColumn(Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (shown.isNotEmpty()) {
                item { Text("Що підключено до входів", color = Color(0x99FFFFFF), fontSize = 13.sp) }
                items(shown, key = { "s:" + it.id }) { i ->
                    val m = if (i == shown.first()) Modifier.focusRequester(fr) else Modifier
                    MenuItem("${i.label}: ${state.nameOf(i)}", look(state.kinds[i.id] ?: Kind.HDMI).icon, m) { onInput(i) }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
            item { Text("Тло", color = Color(0x99FFFFFF), fontSize = 13.sp) }
            items(WALLPAPERS.size) { n ->
                val w = WALLPAPERS[n]
                val m = if (shown.isEmpty() && n == 0) Modifier.focusRequester(fr) else Modifier
                Surface(
                    onClick = { state.pickWallpaper(n) },
                    modifier = m.fillMaxWidth().height(48.dp).tvPress({ state.pickWallpaper(n) }, { state.pickWallpaper(n) }),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                    border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(12.dp))),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                ) {
                    Row(
                        Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(w.base) + w.orbs.map { it.color })).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(w.name, color = if (w.light) Color(0xFF1C1C1E) else Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (state.wallpaper == n) Icon(Icons.Rounded.Check, contentDescription = null, tint = if (w.light) Color(0xFF1C1C1E) else Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Text("Як перемикати входи HDMI", color = Color(0x99FFFFFF), fontSize = 13.sp) }
            val modes = listOf("Автоматично (радимо)", "Лише через системний ТВ", "Вбудований екран «Лад»")
            items(modes.size) { n ->
                MenuItem(modes[n], null, selected = state.switchMode == n) { state.setMode(n) }
            }
            val hiddenApps = state.apps.filter { state.isHidden(it.pkg) }
            if (hiddenApps.isNotEmpty()) {
                item { Spacer(Modifier.height(4.dp)) }
                item { Text("Сховані програми", color = Color(0x99FFFFFF), fontSize = 13.sp) }
                items(hiddenApps, key = { "h:" + it.pkg }) { a ->
                    MenuItem("Показати: ${a.label}", Icons.Rounded.Visibility) { state.toggleHiddenApp(a.pkg) }
                }
            }
            if (hiddenInputs.isNotEmpty()) {
                item { Spacer(Modifier.height(4.dp)) }
                item { Text("Сховані входи", color = Color(0x99FFFFFF), fontSize = 13.sp) }
                items(hiddenInputs, key = { it.id }) { i ->
                    MenuItem("Показати: ${state.nameOf(i)}", Icons.Rounded.Visibility) { state.setHidden(i.id, false) }
                }
            }
            item {
                val v = try { act.packageManager.getPackageInfo(act.packageName, 0).versionName } catch (e: Exception) { "" }
                Text("«Лад» $v. Повернути стандартний головний екран — у програмі «ТВ Лад» на комп'ютері.",
                    color = Color(0x99FFFFFF), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    FocusFirst(fr)
}

/** Тло з кольоровими плямами у 384×216 — малюється один раз при зміні теми. */
private fun backdropBitmap(wp: Wallpaper): androidx.compose.ui.graphics.ImageBitmap {
    val w = 384
    val h = 216
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)
    cv.drawColor(wp.base.toArgb())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    for (o in wp.orbs) {
        val cx = w * o.x
        val cy = h * o.y
        val r = w * o.r
        val c = o.color.copy(alpha = if (wp.light) 0.75f else 0.55f).toArgb()
        paint.shader = android.graphics.RadialGradient(cx, cy, r, c, android.graphics.Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r, paint)
    }
    return bmp.asImageBitmap()
}
