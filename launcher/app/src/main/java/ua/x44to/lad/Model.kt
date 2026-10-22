package ua.x44to.lad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Що підключено до входу — від цього залежать назва, значок і колір плитки. */
enum class Kind(val title: String) {
    PLAYSTATION("PlayStation"),
    XBOX("Xbox"),
    NINTENDO("Nintendo"),
    PC("ПК"),
    LAPTOP("Ноутбук"),
    TVBOX("Приставка"),
    MEDIA("Медіаплеєр"),
    CAMERA("Камера"),
    ANTENNA("Антена"),
    HDMI("HDMI"),
}

/** Вхід телевізора (HDMI, антена…), як його бачить система. */
data class TvIn(val id: String, val label: String, val type: Int, val passthrough: Boolean)

/** Програма для плитки. */
data class AppEntry(val pkg: String, val activity: String, val label: String, val leanback: Boolean) {
    val key: String get() = pkg
}

/** Налаштування «Лад» (локально на телевізорі). */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("lad", Context.MODE_PRIVATE)

    var favorites: List<String>
        get() = arr(sp.getString("favorites", null))
        set(v) = sp.edit().putString("favorites", JSONArray(v).toString()).apply()

    /** Чи вже ставили «Улюблене» за замовчуванням (щоб не повертати прибране). */
    var favoritesSeeded: Boolean
        get() = sp.getBoolean("favoritesSeeded", false)
        set(v) = sp.edit().putBoolean("favoritesSeeded", v).apply()

    var wallpaper: Int
        get() = sp.getInt("wallpaper", 0)
        set(v) = sp.edit().putInt("wallpaper", v).apply()

    /** 0 — автоматично, 1 — лише системний ТВ, 2 — вбудований екран. */
    var switchMode: Int
        get() = sp.getInt("switchMode", 0)
        set(v) = sp.edit().putInt("switchMode", v).apply()

    var hintShown: Boolean
        get() = sp.getBoolean("hintShown", false)
        set(v) = sp.edit().putBoolean("hintShown", v).apply()

    fun kindOf(id: String): Kind? = inputs().optJSONObject(id)?.optString("kind")?.let { k -> Kind.entries.firstOrNull { it.name == k } }

    fun hidden(id: String): Boolean = inputs().optJSONObject(id)?.optBoolean("hidden") ?: false

    fun setKind(id: String, kind: Kind) = updateInput(id) { it.put("kind", kind.name) }

    fun setHidden(id: String, hidden: Boolean) = updateInput(id) { it.put("hidden", hidden) }

    /** Порядок входів (перетягування вліво/вправо). */
    var inputOrder: List<String>
        get() = arr(sp.getString("inputOrder", null))
        set(v) = sp.edit().putString("inputOrder", JSONArray(v).toString()).apply()

    private fun inputs(): JSONObject = try {
        JSONObject(sp.getString("inputs", "{}") ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    private fun updateInput(id: String, f: (JSONObject) -> Unit) {
        val all = inputs()
        val o = all.optJSONObject(id) ?: JSONObject()
        f(o)
        all.put(id, o)
        sp.edit().putString("inputs", all.toString()).apply()
    }

    private fun arr(s: String?): List<String> = try {
        val a = JSONArray(s ?: "[]")
        List(a.length()) { a.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

/** Здогадатися, що підключено, з назви входу (часто телевізор бачить назву пристрою через HDMI-CEC). */
fun guessKind(label: String, type: Int): Kind {
    val l = label.lowercase()
    return when {
        "playstation" in l || Regex("\\bps[345]\\b").containsMatchIn(l) || "sony interactive" in l -> Kind.PLAYSTATION
        "xbox" in l -> Kind.XBOX
        "nintendo" in l || "switch" in l -> Kind.NINTENDO
        "pc" == l || " pc" in l || "пк" in l || "computer" in l || "комп" in l -> Kind.PC
        "laptop" in l || "ноутбук" in l -> Kind.LAPTOP
        type == android.media.tv.TvInputInfo.TYPE_TUNER -> Kind.ANTENNA
        else -> Kind.HDMI
    }
}

/** Назва дня й місяця українською (незалежно від мови системи). */
object Ua {
    private val days = listOf("неділя", "понеділок", "вівторок", "середа", "четвер", "п'ятниця", "субота")
    private val months = listOf("січня", "лютого", "березня", "квітня", "травня", "червня", "липня", "серпня", "вересня", "жовтня", "листопада", "грудня")

    fun date(c: java.util.Calendar): String =
        "${days[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]}, ${c.get(java.util.Calendar.DAY_OF_MONTH)} ${months[c.get(java.util.Calendar.MONTH)]}"

    fun time(c: java.util.Calendar): String =
        "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
}
