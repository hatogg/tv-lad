@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package ua.x44to.lad

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.delay
import java.util.Calendar

// ───────── вигляд у стилі tvOS ─────────

class Wallpaper(val name: String, val colors: List<Color>, val light: Boolean = false) {
    val text: Color get() = if (light) Color(0xFF1C1C1E) else Color.White
    val dim: Color get() = if (light) Color(0x991C1C1E) else Color(0xB3FFFFFF)
    val tile: Color get() = if (light) Color(0x1F000000) else Color(0x24FFFFFF)
    val base: Color get() = colors.last()
}

val WALLPAPERS = listOf(
    Wallpaper("Темна", listOf(Color(0xFF1C1C1E), Color(0xFF242428), Color(0xFF151517))),
    Wallpaper("Світла", listOf(Color(0xFFF2F2F7), Color(0xFFE6E6EC), Color(0xFFDADAE2)), light = true),
    Wallpaper("Північ", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
    Wallpaper("Аврора", listOf(Color(0xFF0B1026), Color(0xFF1B2A5C), Color(0xFF1D6E73))),
    Wallpaper("Захід", listOf(Color(0xFF1F1135), Color(0xFF5B2350), Color(0xFFB8455B))),
    Wallpaper("Ліс", listOf(Color(0xFF07201A), Color(0xFF0E3B2E), Color(0xFF2B5D3A))),
)

class KindLook(val icon: ImageVector, val from: Color, val to: Color)

fun look(k: Kind): KindLook = when (k) {
    Kind.PLAYSTATION -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF0A2A72), Color(0xFF2B7BF0))
    Kind.XBOX -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF0C4A16), Color(0xFF32B455))
    Kind.NINTENDO -> KindLook(Icons.Rounded.VideogameAsset, Color(0xFF7A0A10), Color(0xFFE8333A))
    Kind.PC -> KindLook(Icons.Rounded.Computer, Color(0xFF2C1660), Color(0xFF8160F0))
    Kind.LAPTOP -> KindLook(Icons.Rounded.Laptop, Color(0xFF22324D), Color(0xFF5B7BAA))
    Kind.TVBOX -> KindLook(Icons.Rounded.Cast, Color(0xFF5C300B), Color(0xFFEA9636))
    Kind.MEDIA -> KindLook(Icons.Rounded.Movie, Color(0xFF4A1040), Color(0xFFD0449C))
    Kind.CAMERA -> KindLook(Icons.Rounded.Videocam, Color(0xFF243B3F), Color(0xFF58979C))
    Kind.ANTENNA -> KindLook(Icons.Rounded.SettingsInputAntenna, Color(0xFF4D3A05), Color(0xFFD1A52A))
    Kind.HDMI -> KindLook(Icons.Rounded.SettingsInputHdmi, Color(0xFF2C2C30), Color(0xFF55555C))
}

/** Що зараз у фокусі — від цього залежить «вітрина» зверху (Top Shelf). */
private sealed interface Focus {
    data class In(val input: TvIn) : Focus
    data class App(val app: AppEntry) : Focus
    data object LadSettings : Focus
}

private sealed interface Menu {
    data class ForInput(val input: TvIn) : Menu
    data class ForApp(val app: AppEntry) : Menu
    data object Settings : Menu
}

private val TileShape = RoundedCornerShape(14.dp)

// ───────── головний екран ─────────

@Composable
fun LadHome(state: LadState, act: Activity) {
    val wp = WALLPAPERS[state.wallpaper.coerceIn(0, WALLPAPERS.size - 1)]
    var menu by remember { mutableStateOf<Menu?>(null) }
    var focus by remember { mutableStateOf<Focus?>(null) }
    val first = remember { FocusRequester() }
    BackHandler(enabled = menu == null) { /* головний екран: «Назад» нікуди не веде */ }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, surface = Color(0xFF2C2C2E), onSurface = Color.White)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(wp.colors)),
        ) {
            val side = 56.dp
            val gap = 28.dp
            val tileW: Dp = ((maxWidth - side * 2 - gap * 4) / 5).coerceIn(110.dp, 200.dp)
            val tileH: Dp = tileW * 0.6f
            val shelfH: Dp = maxHeight * 0.47f

            val shownInputs = state.inputs.filter { state.hidden[it.id] != true }
            val favApps = state.favorites.mapNotNull { p -> state.apps.firstOrNull { it.pkg == p } }
            val rest = state.apps.filter { it.pkg !in state.favorites }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 60.dp)) {
                item { TopShelf(state, focus, wp, shelfH) }
                // верхній ряд: входи (PlayStation, ПК…) і улюблені програми — як у tvOS
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = side, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        items(shownInputs, key = { "in:" + it.id }) { i ->
                            val m = if (i == shownInputs.firstOrNull()) Modifier.focusRequester(first) else Modifier
                            InputTile(state, i, wp, tileW, tileH, m, onFocus = { focus = Focus.In(i) },
                                onClick = { switchTo(act, i, state.switchMode) }, onLong = { menu = Menu.ForInput(i) })
                        }
                        items(favApps, key = { "fav:" + it.key }) { a ->
                            val m = if (shownInputs.isEmpty() && a == favApps.firstOrNull()) Modifier.focusRequester(first) else Modifier
                            AppTile(state, a, wp, tileW, tileH, m, onFocus = { focus = Focus.App(a) },
                                onClick = { launch(act, a) }, onLong = { menu = Menu.ForApp(a) })
                        }
                    }
                }
                // решта програм — сіткою по 5
                val cells: List<Any> = rest + listOf(Focus.LadSettings)
                val rows = cells.chunked(5)
                items(rows.size) { r ->
                    Row(Modifier.padding(horizontal = side, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (c in rows[r]) {
                            val isFirst = shownInputs.isEmpty() && favApps.isEmpty() && r == 0 && c == rows[0].first()
                            val m = if (isFirst) Modifier.focusRequester(first) else Modifier
                            if (c is AppEntry) {
                                AppTile(state, c, wp, tileW, tileH, m, onFocus = { focus = Focus.App(c) },
                                    onClick = { launch(act, c) }, onLong = { menu = Menu.ForApp(c) })
                            } else {
                                Tile("Налаштування «Лад»", wp, tileW, tileH, m, onFocus = { focus = Focus.LadSettings },
                                    onClick = { menu = Menu.Settings }, onLong = { menu = Menu.Settings }) {
                                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF636366), Color(0xFF8E8E93)))))
                                    Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(tileH * 0.45f).align(Alignment.Center))
                                }
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
            Menu.Settings -> SettingsMenu(state, act) { menu = null }
            null -> {}
        }
    }
}

// ───────── вітрина (Top Shelf) ─────────

@Composable
private fun TopShelf(state: LadState, focus: Focus?, wp: Wallpaper, height: Dp) {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000L * (60 - now.get(Calendar.SECOND)).coerceAtLeast(1))
        }
    }
    Box(Modifier.fillMaxWidth().height(height)) {
        Crossfade(targetState = focus, animationSpec = tween(350), label = "shelf") { f ->
            Box(Modifier.fillMaxSize()) {
                when (f) {
                    is Focus.In -> {
                        val k = state.kinds[f.input.id] ?: Kind.HDMI
                        val lk = look(k)
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lk.from, lk.to))))
                        Icon(lk.icon, null, tint = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 70.dp).size(height * 0.8f))
                        ShelfText(if (k == Kind.HDMI) "Вхід" else "Вхід · ${f.input.label}", state.nameOf(f.input),
                            "Натисніть OK — телевізор перемкнеться", Color.White)
                    }
                    is Focus.App -> {
                        val art = state.art[f.app.key]
                        if (art != null) {
                            Image(art.blur, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Color(0x59000000)))
                            Image(
                                art.image, f.app.label,
                                contentScale = if (art.banner) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 70.dp)
                                    .width(height * 0.95f)
                                    .height(height * 0.95f * 0.5625f)
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(wp.colors)))
                        }
                        ShelfText("Програма", f.app.label, if (state.isFavorite(f.app.pkg)) "У верхньому ряду" else "Утримуйте OK — у верхній ряд",
                            if (art != null) Color.White else wp.text)
                    }
                    else -> {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(wp.colors)))
                        ShelfText("Лад", "Добрий " + partOfDay(now), Ua.date(now).replaceFirstChar { it.uppercase() }, wp.text)
                    }
                }
                // плавний перехід вітрини в тло
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(height * 0.45f)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, wp.colors.first()))),
                )
            }
        }
        val onShelf = when (focus) {
            is Focus.In -> Color.White
            is Focus.App -> if (state.art[focus.app.key] != null) Color.White else wp.text
            else -> wp.text
        }
        Text(Ua.time(now), color = onShelf, fontSize = 22.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 56.dp))
    }
}

@Composable
private fun BoxScope.ShelfText(kicker: String, title: String, sub: String, color: Color) {
    Column(Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 34.dp).fillMaxWidth(0.55f)) {
        Text(kicker.uppercase(), color = color.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        Text(title, color = color, fontSize = 38.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, color = color.copy(alpha = 0.75f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun partOfDay(c: Calendar): String = when (c.get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "ранок"
    in 12..17 -> "день"
    in 18..22 -> "вечір"
    else -> "вечір"
}

// ───────── плитки ─────────

/** Плитка в стилі tvOS: на фокусі піднімається, відкидає тінь, по ній пробігає відблиск; назва — лише під вибраною. */
@Composable
private fun Tile(
    label: String, wp: Wallpaper, w: Dp, h: Dp, modifier: Modifier,
    onFocus: () -> Unit, onClick: () -> Unit, onLong: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shine = remember { Animatable(-0.6f) }
    LaunchedEffect(focused) {
        if (focused) {
            onFocus()
            shine.snapTo(-0.6f)
            shine.animateTo(1.6f, tween(1100, easing = FastOutSlowInEasing))
        }
    }
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "label")
    Column(modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            onLongClick = onLong,
            modifier = Modifier.width(w).height(h),
            shape = ClickableSurfaceDefaults.shape(TileShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.17f),
            glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color(0x8C000000), elevation = 20.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = wp.tile, focusedContainerColor = wp.tile),
            interactionSource = src,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(TileShape)
                    .drawWithContent {
                        drawContent()
                        if (focused) {
                            // м'який блиск зверху й смуга відблиску, що пробігає по плитці
                            drawRect(Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color.Transparent), endY = size.height * 0.55f))
                            val x = size.width * shine.value
                            drawRect(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color(0x47FFFFFF), Color.Transparent),
                                    start = Offset(x - size.width * 0.35f, 0f), end = Offset(x + size.width * 0.05f, size.height),
                                ),
                            )
                        }
                    },
            ) { content() }
        }
        Text(
            label, color = wp.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp).alpha(labelAlpha),
        )
    }
}

@Composable
private fun InputTile(state: LadState, i: TvIn, wp: Wallpaper, w: Dp, h: Dp, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit, onLong: () -> Unit) {
    val k = state.kinds[i.id] ?: Kind.HDMI
    val lk = look(k)
    Tile(state.nameOf(i), wp, w, h, modifier, onFocus, onClick, onLong) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lk.from, lk.to))))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(lk.icon, null, tint = Color.White, modifier = Modifier.size(h * 0.36f))
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
                val c = Color(art.color)
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0.25f)))))
                Image(art.image, a.label, modifier = Modifier.size(h * 0.6f).align(Alignment.Center))
            }
            else -> Text(a.label.take(1).uppercase(), color = wp.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
    }
}

// ───────── меню (довге натискання OK) ─────────

@Composable
private fun MenuBox(title: String, sub: String? = null, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xF02C2C2E), contentColor = Color.White),
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
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color(0x66000000), elevation = 10.dp)),
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
    LaunchedEffect(Unit) {
        delay(100)
        try { fr.requestFocus() } catch (e: IllegalStateException) { }
    }
}

@Composable
private fun AppMenu(state: LadState, a: AppEntry, act: Activity, onDismiss: () -> Unit) {
    val fav = state.isFavorite(a.pkg)
    MenuBox(a.label, a.pkg, onDismiss) {
        MenuItem(if (fav) "Прибрати з верхнього ряду" else "У верхній ряд", if (fav) Icons.Rounded.StarBorder else Icons.Rounded.Star) {
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
        LazyColumn(Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("Тло", color = Color(0x99FFFFFF), fontSize = 13.sp) }
            items(WALLPAPERS.size) { n ->
                val w = WALLPAPERS[n]
                Surface(
                    onClick = { state.pickWallpaper(n) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                    border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(12.dp))),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                ) {
                    Row(
                        Modifier.fillMaxSize().background(Brush.horizontalGradient(w.colors)).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(w.name, color = w.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (state.wallpaper == n) Icon(Icons.Rounded.Check, contentDescription = null, tint = w.text, modifier = Modifier.size(20.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Text("Як перемикати входи HDMI", color = Color(0x99FFFFFF), fontSize = 13.sp) }
            val modes = listOf("Автоматично (радимо)", "Лише через системний ТВ", "Вбудований екран «Лад»")
            items(modes.size) { n ->
                MenuItem(modes[n], null, selected = state.switchMode == n) { state.setMode(n) }
            }
            if (hiddenInputs.isNotEmpty()) {
                item { Spacer(Modifier.height(4.dp)) }
                item { Text("Сховані входи", color = Color(0x99FFFFFF), fontSize = 13.sp) }
                items(hiddenInputs, key = { it.id }) { i ->
                    MenuItem("Показати: ${state.nameOf(i)}", Icons.Rounded.Visibility) { state.setHidden(i.id, false) }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { MenuItem("Системні налаштування телевізора", Icons.Rounded.Settings) { onDismiss(); openSystemSettings(act) } }
            item {
                val v = try { act.packageManager.getPackageInfo(act.packageName, 0).versionName } catch (e: Exception) { "" }
                Text("«Лад» $v. Повернути стандартний головний екран — у програмі «ТВ Лад» на комп'ютері.",
                    color = Color(0x99FFFFFF), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
