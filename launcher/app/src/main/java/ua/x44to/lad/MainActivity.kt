package ua.x44to.lad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.tv.TvInputInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/** Стан головного екрана: програми, входи, улюблене, налаштування. */
class LadState(private val ctx: Context, var demo: Boolean) {
    val prefs = Prefs(ctx)
    val apps = mutableStateListOf<AppEntry>()
    val inputs = mutableStateListOf<TvIn>()
    val favorites = mutableStateListOf<String>()
    val art = mutableStateMapOf<String, Pair<ImageBitmap, Boolean>>()
    val kinds = mutableStateMapOf<String, Kind>()
    val hidden = mutableStateMapOf<String, Boolean>()
    var wallpaper by mutableIntStateOf(prefs.wallpaper)
    var switchMode by mutableIntStateOf(prefs.switchMode)
    var hintShown by mutableStateOf(prefs.hintShown)
    private val io = Executors.newSingleThreadExecutor()

    fun reload(onMain: (Runnable) -> Unit) {
        val newApps = loadApps(ctx)
        var newInputs = loadInputs(ctx)
        if (newInputs.isEmpty() && demo) newInputs = DEMO_INPUTS
        val order = prefs.inputOrder
        newInputs = newInputs.sortedBy { i -> order.indexOf(i.id).let { if (it < 0) Int.MAX_VALUE else it } }
        apps.clear()
        apps.addAll(newApps)
        inputs.clear()
        inputs.addAll(newInputs)
        for (i in newInputs) {
            kinds[i.id] = prefs.kindOf(i.id) ?: if (demo) DEMO_KINDS[i.id] ?: guessKind(i.label, i.type) else guessKind(i.label, i.type)
            hidden[i.id] = prefs.hidden(i.id)
        }
        var fav = prefs.favorites
        if (!prefs.favoritesSeeded) {
            fav = DEFAULT_FAVORITES.filter { p -> newApps.any { it.pkg == p } }
            prefs.favorites = fav
            prefs.favoritesSeeded = true
        }
        favorites.clear()
        favorites.addAll(fav.filter { p -> newApps.any { it.pkg == p } })
        // значки й банери — у фоні, щоб екран з'являвся одразу
        val missing = newApps.filter { it.key !in art }
        io.execute {
            for (a in missing) {
                val img = loadArt(ctx, a) ?: continue
                onMain(Runnable { art[a.key] = img })
            }
        }
    }

    fun isFavorite(pkg: String) = pkg in favorites

    fun toggleFavorite(pkg: String) {
        if (pkg in favorites) favorites.remove(pkg) else favorites.add(pkg)
        prefs.favorites = favorites.toList()
    }

    fun moveFavorite(pkg: String, by: Int) {
        val i = favorites.indexOf(pkg)
        val j = i + by
        if (i < 0 || j !in favorites.indices) return
        favorites.removeAt(i)
        favorites.add(j, pkg)
        prefs.favorites = favorites.toList()
    }

    fun setKind(id: String, kind: Kind) {
        kinds[id] = kind
        prefs.setKind(id, kind)
        hintShown = true
        prefs.hintShown = true
    }

    fun setHidden(id: String, h: Boolean) {
        hidden[id] = h
        prefs.setHidden(id, h)
    }

    fun moveInput(id: String, by: Int) {
        val i = inputs.indexOfFirst { it.id == id }
        val j = i + by
        if (i < 0 || j !in inputs.indices) return
        val x = inputs.removeAt(i)
        inputs.add(j, x)
        prefs.inputOrder = inputs.map { it.id }
    }

    fun setWallpaper(n: Int) {
        wallpaper = n
        prefs.wallpaper = n
    }

    fun setMode(n: Int) {
        switchMode = n
        prefs.switchMode = n
    }

    fun dismissHint() {
        hintShown = true
        prefs.hintShown = true
    }

    fun nameOf(i: TvIn): String {
        val k = kinds[i.id] ?: Kind.HDMI
        return if (k == Kind.HDMI) i.label else k.title
    }

    companion object {
        /** Для знімків екрана в емуляторі, де немає справжніх входів HDMI. */
        val DEMO_INPUTS = listOf(
            TvIn("demo/HW1", "HDMI 1", TvInputInfo.TYPE_HDMI, true),
            TvIn("demo/HW2", "HDMI 2", TvInputInfo.TYPE_HDMI, true),
            TvIn("demo/HW3", "HDMI 3", TvInputInfo.TYPE_HDMI, true),
            TvIn("demo/TUNER", "Антена (T2)", TvInputInfo.TYPE_TUNER, false),
        )
        val DEMO_KINDS = mapOf("demo/HW1" to Kind.PLAYSTATION, "demo/HW2" to Kind.PC)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var state: LadState
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = LadState(applicationContext, intent?.getBooleanExtra(EXTRA_DEMO, false) == true)
        setContent { LadHome(state, this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_DEMO, false) && !state.demo) {
            state.demo = true
            refresh()
        }
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packages, f, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(packages)
        } catch (e: IllegalArgumentException) {
            // не був зареєстрований
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        state.reload { r -> runOnUiThread(r) }
    }

    companion object {
        const val EXTRA_DEMO = "lad.demo"
    }
}
