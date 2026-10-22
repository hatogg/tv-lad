package ua.x44to.lad

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Програми, які можна запустити (спершу «телевізійні» з банерами, потім звичайні). */
fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val res = LinkedHashMap<String, AppEntry>()
    for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
        val list = try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
        } catch (e: Exception) {
            emptyList()
        }
        for (ri in list) {
            val ai = ri.activityInfo ?: continue
            if (ai.packageName == ctx.packageName || res.containsKey(ai.packageName)) continue
            val label = try { ri.loadLabel(pm).toString() } catch (e: Exception) { ai.packageName }
            res[ai.packageName] = AppEntry(ai.packageName, ai.name, label, cat == Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
    }
    return res.values.sortedBy { it.label.lowercase() }
}

/** Програми, які варто одразу показати в «Улюбленому», якщо вони є. */
val DEFAULT_FAVORITES = listOf(
    "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta", "com.google.android.youtube.tv",
    "com.megogo.application", "com.netflix.ninja", "ua.sweet.tv", "com.kyivstar.tv.mobile", "com.spotify.tv.android",
    "org.videolan.vlc", "com.android.chrome", "com.google.android.apps.mediashell",
)

fun launch(ctx: Activity, app: AppEntry) {
    val pm = ctx.packageManager
    val intent = (if (app.leanback) pm.getLeanbackLaunchIntentForPackage(app.pkg) else null)
        ?: pm.getLaunchIntentForPackage(app.pkg)
        ?: Intent(Intent.ACTION_MAIN).setComponent(ComponentName(app.pkg, app.activity))
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Не вдалося відкрити «${app.label}»", Toast.LENGTH_SHORT).show()
    }
}

fun openAppInfo(ctx: Activity, pkg: String) {
    try {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$pkg")))
    } catch (e: Exception) {
        openSystemSettings(ctx)
    }
}

fun openSystemSettings(ctx: Activity) {
    try {
        ctx.startActivity(Intent(Settings.ACTION_SETTINGS))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "Налаштування недоступні", Toast.LENGTH_SHORT).show()
    }
}

/** Банер (для телевізійних програм) або значок. */
fun loadArt(ctx: Context, app: AppEntry): Pair<ImageBitmap, Boolean>? {
    val pm = ctx.packageManager
    return try {
        val ai = pm.getActivityInfo(ComponentName(app.pkg, app.activity), 0)
        val banner: Drawable? = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
        if (banner != null) {
            Pair(draw(banner, 320, 180), true)
        } else {
            Pair(draw(ai.loadIcon(pm), 192, 192), false)
        }
    } catch (e: Exception) {
        null
    }
}

private fun draw(d: Drawable, w: Int, h: Int): ImageBitmap {
    val iw = if (d.intrinsicWidth > 0) d.intrinsicWidth else w
    val ih = if (d.intrinsicHeight > 0) d.intrinsicHeight else h
    val scale = minOf(w.toFloat() / iw, h.toFloat() / ih, 2f)
    return d.toBitmap((iw * scale).toInt().coerceAtLeast(1), (ih * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).asImageBitmap()
}

// ───────── входи телевізора ─────────

fun loadInputs(ctx: Context): List<TvIn> {
    val tim = ctx.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
    val list = try {
        tim.tvInputList
    } catch (e: Exception) {
        return emptyList()
    }
    return list.filter { it.parentId == null }.map { info ->
        val custom = if (Build.VERSION.SDK_INT >= 24) info.loadCustomLabel(ctx)?.toString() else null
        val label = custom?.takeIf { it.isNotBlank() } ?: info.loadLabel(ctx).toString()
        TvIn(info.id, label, info.type, info.isPassthroughInput)
    }.sortedWith(compareBy({ if (it.type == TvInputInfo.TYPE_HDMI) 0 else if (it.passthrough) 1 else 2 }, { naturalKey(it.label) }))
}

private fun naturalKey(s: String): String = s.replace(Regex("\\d+")) { it.value.padStart(4, '0') }.lowercase()

/** Перемкнути телевізор на вхід. mode: 0 — авто, 1 — системний ТВ, 2 — вбудований екран. */
fun switchTo(act: Activity, input: TvIn, mode: Int) {
    val uri = if (input.passthrough) TvContract.buildChannelUriForPassthroughInput(input.id) else TvContract.buildChannelsUriForInput(input.id)
    if (mode != 2) {
        val view = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (view.resolveActivity(act.packageManager) != null) {
            try {
                act.startActivity(view)
                return
            } catch (e: Exception) {
                // спробуємо вбудований екран
            }
        }
        if (mode == 1) {
            Toast.makeText(act, "Системний ТВ не відкриває входи — увімкніть «Вбудований екран» у налаштуваннях «Лад»", Toast.LENGTH_LONG).show()
            return
        }
    }
    if (!input.passthrough) {
        Toast.makeText(act, "Цей вхід відкривається лише через системний ТВ", Toast.LENGTH_LONG).show()
        return
    }
    act.startActivity(Intent(act, InputActivity::class.java).putExtra(InputActivity.EXTRA_ID, input.id))
}
