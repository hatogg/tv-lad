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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LightMode
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import androidx.tv.material3.lightColorScheme
import kotlinx.coroutines.delay
import java.util.Calendar

// ───────── Material: спокійні поверхні, один акцентний колір ─────────

/** Акцентний колір (як Material You): основний, контейнер і текст на контейнері — для темної й світлої теми. */
class Accent(val name: String, val dark: Triple<Color, Color, Color>, val light: Triple<Color, Color, Color>)

val ACCENTS = listOf(
    Accent("Бірюзовий", Triple(Color(0xFF6FD8C1), Color(0xFF00504A), Color(0xFF8CF5DD)), Triple(Color(0xFF006B62), Color(0xFF8CF5DD), Color(0xFF00201C))),
    Accent("Синій", Triple(Color(0xFFAAC7FF), Color(0xFF284777), Color(0xFFD6E3FF)), Triple(Color(0xFF3F5F90), Color(0xFFD6E3FF), Color(0xFF001B3E))),
    Accent("Фіолетовий", Triple(Color(0xFFD0BCFF), Color(0xFF4F378B), Color(0xFFEADDFF)), Triple(Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFF21005D))),
    Accent("Зелений", Triple(Color(0xFF9CD67D), Color(0xFF225104), Color(0xFFB7F397)), Triple(Color(0xFF396A1E), Color(0xFFB7F397), Color(0xFF042100))),
    Accent("Помаранчевий", Triple(Color(0xFFFFB68B), Color(0xFF713700), Color(0xFFFFDBC8)), Triple(Color(0xFF8F4C1E), Color(0xFFFFDBC8), Color(0xFF331200))),
    Accent("Рожевий", Triple(Color(0xFFFFB0CB), Color(0xFF7B2949), Color(0xFFFFD9E3)), Triple(Color(0xFF984062), Color(0xFFFFD9E3), Color(0xFF3E001E))),
)

/** Нейтральні поверхні Material 3. */
class Palette(val light: Boolean, accent: Accent) {
    val primary = if (light) accent.light.first else accent.dark.first
    val container = if (light) accent.light.second else accent.dark.second
    val onContainer = if (light) accent.light.third else accent.dark.third
    val bg = if (light) Color(0xFFF8F9FC) else Color(0xFF111318)
    val card = if (light) Color(0xFFECEEF2) else Color(0xFF1D2025)
    val cardHigh = if (light) Color(0xFFE2E5EA) else Color(0xFF272A30)
    val text = if (light) Color(0xFF191C20) else Color(0xFFE2E2E9)
    val dim = if (light) Color(0xFF44474E) else Color(0xFFA9ACB4)
}

class KindLook(val icon: ImageVector, val hue: Color)

fun look(k: Kind): KindLook = when (k) {
    Kind.PLAYSTATION -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF3D7BFF))
    Kind.XBOX -> KindLook(Icons.Rounded.SportsEsports, Color(0xFF2EAD55))
    Kind.NINTENDO -> KindLook(Icons.Rounded.VideogameAsset, Color(0xFFE5393F))
    Kind.PC -> KindLook(Icons.Rounded.Computer, Color(0xFF8C6CF2))
    Kind.LAPTOP -> KindLook(Icons.Rounded.Laptop, Color(0xFF5B86C9))
    Kind.TVBOX -> KindLook(Icons.Rounded.Cast, Color(0xFFF08C2E))
    Kind.MEDIA -> KindLook(Icons.Rounded.Movie, Color(0xFFD64C9F))
    Kind.CAMERA -> KindLook(Icons.Rounded.Videocam, Color(0xFF4FA3A8))
    Kind.ANTENNA -> KindLook(Icons.Rounded.SettingsInputAntenna, Color(0xFFD9A419))
    Kind.HDMI -> KindLook(Icons.Rounded.SettingsInputHdmi, Color(0xFF8A8D96))
}

// ───────── пульт: натискання й утримання OK ─────────

private const val LONG_MS = 450L

/**
 * Натискання OK — дія; утримання (за часом, навіть якщо пульт не повторює кнопку) або кнопка Menu — меню.
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

private sealed interface Menu {
    data class ForInput(val input: TvIn) : Menu
    data class ForApp(val app: AppEntry) : Menu
    data object Settings : Menu
}

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun LadHome(state: LadState, act: Activity) {
    val pal = Palette(state.light, ACCENTS[state.wallpaper.coerceIn(0, ACCENTS.size - 1)])
    var menu by remember { mutableStateOf<Menu?>(null) }
    val first = remember { FocusRequester() }
    BackHandler(enabled = menu == null) { /* головний екран: «Назад» нікуди не веде */ }

    val scheme = if (pal.light) lightColorScheme(primary = pal.primary, surface = pal.card, onSurface = pal.text)
    else darkColorScheme(primary = pal.primary, surface = pal.card, onSurface = pal.text)
    MaterialTheme(colorScheme = scheme) {
        BoxWithConstraints(Modifier.fillMaxSize().background(pal.bg)) {
            val side = maxWidth * 0.05f
            val gap = 16.dp
            val appW: Dp = ((maxWidth - side * 2 - gap * 5) / 6).coerceIn(100.dp, 200.dp)
            val appH: Dp = appW * 0.5625f
            val inW: Dp = appW * 1.35f
            val inH: Dp = appH * 1.2f

            val shownInputs = state.inputs.filter { state.hidden[it.id] != true }
            val favApps = state.favorites.mapNotNull { p -> state.apps.firstOrNull { it.pkg == p } }
            val rest = state.apps.filter { it.pkg !in state.favorites && it.pkg !in state.hiddenApps }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp)) {
                item { Header(pal, side, onTv = { openSystemSettings(act) }, onLad = { menu = Menu.Settings }) }
                if (shownInputs.isNotEmpty()) {
                    item { Section("Входи", pal, side) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = side, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            items(shownInputs, key = { "in:" + it.id }) { i ->
                                val m = if (i == shownInputs.first()) Modifier.focusRequester(first) else Modifier
                                InputCard(state, i, pal, inW, inH, m, onClick = { switchTo(act, i, state.switchMode) }, onLong = { menu = Menu.ForInput(i) })
                            }
                        }
                    }
                }
                if (favApps.isNotEmpty()) {
                    item { Section("Улюблене", pal, side) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = side, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            items(favApps, key = { "fav:" + it.key }) { a ->
                                val m = if (shownInputs.isEmpty() && a == favApps.first()) Modifier.focusRequester(first) else Modifier
                                AppCard(state, a, pal, appW, appH, m, onClick = { launch(act, a) }, onLong = { menu = Menu.ForApp(a) })
                            }
                        }
                    }
                }
                if (rest.isNotEmpty()) {
                    item { Section("Програми", pal, side) }
                    val rows = rest.chunked(6)
                    items(rows.size) { r ->
                        Row(Modifier.padding(horizontal = side, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (a in rows[r]) {
                                val isFirst = shownInputs.isEmpty() && favApps.isEmpty() && r == 0 && a == rows[0].first()
                                val m = if (isFirst) Modifier.focusRequester(first) else Modifier
                                AppCard(state, a, pal, appW, appH, m, onClick = { launch(act, a) }, onLong = { menu = Menu.ForApp(a) })
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
            is Menu.ForInput -> InputMenu(state, pal, m.input) { menu = null }
            is Menu.ForApp -> AppMenu(state, pal, m.app, act) { menu = null }
            Menu.Settings -> SettingsMenu(state, pal, act, onInput = { menu = Menu.ForInput(it) }) { menu = null }
            null -> {}
        }
    }
}

@Composable
private fun Header(pal: Palette, side: Dp, onTv: () -> Unit, onLad: () -> Unit) {
    val now = rememberNow()
    Row(Modifier.fillMaxWidth().padding(horizontal = side).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Ua.time(now), color = pal.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.width(18.dp))
        Text(Ua.date(now).replaceFirstChar { it.uppercase() }, color = pal.dim, fontSize = 17.sp, modifier = Modifier.weight(1f))
        RoundButton(Icons.Rounded.Settings, "Налаштування ТВ", pal, onTv)
        Spacer(Modifier.width(12.dp))
        RoundButton(Icons.Rounded.Tune, "Налаштування «Лад»", pal, onLad)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, desc: String, pal: Palette, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp).tvPress(onClick, onClick),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = pal.card, contentColor = pal.dim,
            focusedContainerColor = pal.primary, focusedContentColor = pal.bg,
        ),
    ) {
        Icon(icon, contentDescription = desc, modifier = Modifier.size(24.dp).align(Alignment.Center))
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

@Composable
private fun Section(text: String, pal: Palette, side: Dp) {
    Text(text, color = pal.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = side, top = 18.dp, bottom = 2.dp))
}

// ───────── картки ─────────

/** Картка Material для ТВ: у фокусі трохи збільшується й отримує рамку акцентного кольору. */
@Composable
private fun FocusCard(
    w: Dp, h: Dp, pal: Palette, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = modifier.width(w).height(h).tvPress(onClick, onLong),
        shape = ClickableSurfaceDefaults.shape(CardShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, pal.primary), shape = CardShape)),
        colors = ClickableSurfaceDefaults.colors(containerColor = pal.card, focusedContainerColor = pal.cardHigh),
        interactionSource = src,
    ) {
        Box(Modifier.fillMaxSize().clip(CardShape)) { content(focused) }
    }
}

@Composable
private fun InputCard(state: LadState, i: TvIn, pal: Palette, w: Dp, h: Dp, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val k = state.kinds[i.id] ?: Kind.HDMI
    val lk = look(k)
    FocusCard(w, h, pal, modifier, onClick, onLong) { focused ->
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(h * 0.5f).background(if (focused) lk.hue else lk.hue.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(lk.icon, null, tint = if (focused) Color.White else lk.hue, modifier = Modifier.size(h * 0.28f))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.nameOf(i), color = pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (k != Kind.HDMI) Text(i.label, color = pal.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AppCard(state: LadState, a: AppEntry, pal: Palette, w: Dp, h: Dp, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val art = state.art[a.key]
    Column(modifier.width(w)) {
        FocusCard(w, h, pal, Modifier, onClick, onLong) {
            when {
                art != null && art.banner -> Image(art.image, a.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                art != null -> Image(art.image, a.label, modifier = Modifier.size(h * 0.6f).align(Alignment.Center))
                else -> Text(a.label.take(1).uppercase(), color = pal.dim, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Center))
            }
        }
        Text(a.label, color = pal.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
    }
}

// ───────── меню ─────────

@Composable
private fun MenuBox(title: String, sub: String?, pal: Palette, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(containerColor = pal.cardHigh, contentColor = pal.text),
            modifier = Modifier.width(460.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = pal.text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                if (sub != null) Text(sub, color = pal.dim, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector?, pal: Palette, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        // tvPress: спрацьовує лише на натискання, що почалося тут, — «хвіст» утримання OK, яким відкрили меню, не вибирає пункт
        modifier = modifier.fillMaxWidth().height(48.dp).tvPress(onClick, onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) pal.container.copy(alpha = 0.55f) else Color.Transparent, contentColor = pal.text,
            focusedContainerColor = pal.container, focusedContentColor = pal.onContainer,
        ),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
            }
            Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Label(text: String, pal: Palette) {
    Text(text, color = pal.dim, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
private fun FocusFirst(fr: FocusRequester) {
    LaunchedEffect(Unit) {
        delay(120)
        try { fr.requestFocus() } catch (e: IllegalStateException) { }
    }
}

@Composable
private fun InputMenu(state: LadState, pal: Palette, i: TvIn, onDismiss: () -> Unit) {
    val cur = state.kinds[i.id] ?: Kind.HDMI
    val fr = remember { FocusRequester() }
    MenuBox(state.nameOf(i), "${i.label} — що підключено до цього входу?", pal, onDismiss) {
        LazyColumn(Modifier.height(330.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(Kind.entries.size) { n ->
                val k = Kind.entries[n]
                val m = if (k == cur) Modifier.focusRequester(fr) else Modifier
                MenuItem(if (k == Kind.HDMI) "Звичайний вхід (${i.label})" else k.title, look(k).icon, pal, m, selected = k == cur) {
                    state.setKind(i.id, k)
                    onDismiss()
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
            item { MenuItem("Перемістити ліворуч", Icons.Rounded.ChevronLeft, pal) { state.moveInput(i.id, -1); onDismiss() } }
            item { MenuItem("Перемістити праворуч", Icons.Rounded.ChevronRight, pal) { state.moveInput(i.id, 1); onDismiss() } }
            item { MenuItem("Сховати цей вхід", Icons.Rounded.VisibilityOff, pal) { state.setHidden(i.id, true); onDismiss() } }
        }
    }
    FocusFirst(fr)
}

@Composable
private fun AppMenu(state: LadState, pal: Palette, a: AppEntry, act: Activity, onDismiss: () -> Unit) {
    val fav = state.isFavorite(a.pkg)
    val fr = remember { FocusRequester() }
    MenuBox(a.label, null, pal, onDismiss) {
        MenuItem(if (fav) "Прибрати з «Улюбленого»" else "Додати в «Улюблене»", if (fav) Icons.Rounded.StarBorder else Icons.Rounded.Star, pal,
            Modifier.focusRequester(fr)) {
            state.toggleFavorite(a.pkg)
            onDismiss()
        }
        if (fav) {
            MenuItem("Перемістити ліворуч", Icons.Rounded.ChevronLeft, pal) { state.moveFavorite(a.pkg, -1); onDismiss() }
            MenuItem("Перемістити праворуч", Icons.Rounded.ChevronRight, pal) { state.moveFavorite(a.pkg, 1); onDismiss() }
        }
        MenuItem("Сховати з екрана", Icons.Rounded.VisibilityOff, pal) {
            state.toggleHiddenApp(a.pkg)
            onDismiss()
        }
        MenuItem("Про програму (видалити, очистити)", Icons.Rounded.Info, pal) { onDismiss(); openAppInfo(act, a.pkg) }
    }
    FocusFirst(fr)
}

@Composable
private fun SettingsMenu(state: LadState, pal: Palette, act: Activity, onInput: (TvIn) -> Unit, onDismiss: () -> Unit) {
    val hiddenInputs = state.inputs.filter { state.hidden[it.id] == true }
    val shown = state.inputs.filter { state.hidden[it.id] != true }
    val hiddenApps = state.apps.filter { state.isHidden(it.pkg) }
    val fr = remember { FocusRequester() }
    MenuBox("Налаштування «Лад»", "Утримання OK або кнопка Menu на картці — її меню", pal, onDismiss) {
        LazyColumn(Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item { Label("Тема", pal) }
            item {
                MenuItem(if (state.light) "Світла" else "Темна", if (state.light) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, pal,
                    Modifier.focusRequester(fr)) { state.chooseLight(!state.light) }
            }
            item { Label("Акцентний колір", pal) }
            items(ACCENTS.size) { n ->
                val acc = ACCENTS[n]
                Surface(
                    onClick = { state.pickWallpaper(n) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).tvPress({ state.pickWallpaper(n) }, { state.pickWallpaper(n) }),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent, contentColor = pal.text,
                        focusedContainerColor = pal.container, focusedContentColor = pal.onContainer,
                    ),
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).background(if (state.light) acc.light.first else acc.dark.first, CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Text(acc.name, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (state.wallpaper == n) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (shown.isNotEmpty()) {
                item { Label("Що підключено до входів", pal) }
                items(shown, key = { "s:" + it.id }) { i ->
                    MenuItem("${i.label}: ${state.nameOf(i)}", look(state.kinds[i.id] ?: Kind.HDMI).icon, pal) { onInput(i) }
                }
            }
            item { Label("Як перемикати входи HDMI", pal) }
            val modes = listOf("Автоматично (радимо)", "Лише через системний ТВ", "Вбудований екран «Лад»")
            items(modes.size) { n ->
                MenuItem(modes[n], null, pal, selected = state.switchMode == n) { state.setMode(n) }
            }
            if (hiddenApps.isNotEmpty()) {
                item { Label("Сховані програми", pal) }
                items(hiddenApps, key = { "h:" + it.pkg }) { a ->
                    MenuItem("Показати: ${a.label}", Icons.Rounded.Visibility, pal) { state.toggleHiddenApp(a.pkg) }
                }
            }
            if (hiddenInputs.isNotEmpty()) {
                item { Label("Сховані входи", pal) }
                items(hiddenInputs, key = { it.id }) { i ->
                    MenuItem("Показати: ${state.nameOf(i)}", Icons.Rounded.Visibility, pal) { state.setHidden(i.id, false) }
                }
            }
            item {
                val v = try { act.packageManager.getPackageInfo(act.packageName, 0).versionName } catch (e: Exception) { "" }
                Text("«Лад» $v · повернути стандартний головний екран — у «ТВ Лад» на комп'ютері.",
                    color = pal.dim, fontSize = 12.sp, modifier = Modifier.padding(start = 18.dp, top = 10.dp))
            }
        }
    }
    FocusFirst(fr)
}
