"""
Які програми на Android TV можна чіпати, а які — ні.

Захищені (PROTECTED) у списку взагалі не показуються: без них телевізор не вмикається, не перемикає
входи, не ловить Wi‑Fi чи не слухається пульта. Решта поділена на групи з поясненнями.
Будь-яку вимкнену програму можна ввімкнути назад («Відновлення»).
"""
from __future__ import annotations

from dataclasses import dataclass

GROUP_SAFE = "safe"        # можна вимкнути без наслідків
GROUP_APPS = "apps"        # звичайні програми й стрімінги — на ваш смак
GROUP_CAREFUL = "careful"  # працює, але зникне корисна функція
GROUP_SYSTEM = "system"    # невідомі системні — лише якщо знаєте, що це
GROUP_USER = "user"        # встановлені вами

GROUP_TITLES = {
    GROUP_SAFE: "Можна вимкнути",
    GROUP_APPS: "Програми й онлайн-кінотеатри",
    GROUP_CAREFUL: "Обережно: зникне корисна функція",
    GROUP_SYSTEM: "Інші системні (лише якщо знаєте, що це)",
    GROUP_USER: "Встановлені вами",
}
GROUP_ORDER = [GROUP_SAFE, GROUP_APPS, GROUP_USER, GROUP_CAREFUL, GROUP_SYSTEM]


@dataclass(frozen=True)
class Known:
    name: str
    group: str
    tip: str = ""
    preselect: bool = False


KNOWN: dict[str, Known] = {
    # ── можна вимкнути ──
    "com.google.android.play.games": Known("Google Play Ігри", GROUP_SAFE, "Каталог ігор Google; самі ігри працюватимуть", True),
    "com.google.android.videos": Known("Google TV (фільми й серіали)", GROUP_SAFE, "Магазин фільмів Google; в Україні майже не потрібен", True),
    "com.google.android.youtube.tvkids": Known("YouTube Kids", GROUP_SAFE, "Дитячий YouTube", True),
    "com.android.printspooler": Known("Друк", GROUP_SAFE, "Телевізору друк не потрібен", True),
    "com.android.bips": Known("Служба друку", GROUP_SAFE, "Телевізору друк не потрібен", True),
    "com.google.android.feedback": Known("Відгуки Google", GROUP_SAFE, "Надсилання звітів у Google", True),
    "com.google.android.tvrecommendations": Known("Рекомендації на головному екрані", GROUP_SAFE,
                                                  "Ряди «рекомендовано» на стандартному головному екрані"),
    "com.google.android.backdrop": Known("Заставка Google (фонові фото)", GROUP_SAFE, "Залишиться звичайна заставка"),
    "com.android.dreams.basic": Known("Заставка «Кольори»", GROUP_SAFE),
    "com.android.dreams.phototable": Known("Заставка «Фото»", GROUP_SAFE),
    "com.google.android.apps.tv.dreamx": Known("Заставка Google TV", GROUP_SAFE),
    "com.android.gallery3d": Known("Галерея", GROUP_SAFE),
    "com.android.calendar": Known("Календар", GROUP_SAFE),
    "com.android.email": Known("Пошта", GROUP_SAFE),
    "com.android.browser": Known("Старий браузер", GROUP_SAFE),
    "com.google.android.apps.tv.dreamx.dreams": Known("Заставка Google TV", GROUP_SAFE),
    # ── телевізори KIVI (Realtek/Hikeen) — за звітом 40FR55BU ──
    "com.adups.fota": Known("Adups FOTA (оновлення «повітрям»)", GROUP_SAFE,
                            "Нових прошивок для моделі вже не буде; сервіс відомий збором даних", True),
    "com.adups.fota.sysoper": Known("Adups FOTA (служба)", GROUP_SAFE, "Частина Adups FOTA", True),
    "com.redhotapp.kiviupdate": Known("Оновлення KIVI", GROUP_SAFE, "Тримає ~23 МБ пам'яті; прошивку можна поставити з флешки", True),
    "com.realsil.ota": Known("Realtek OTA (оновлення)", GROUP_SAFE, "Оновлення прошивки Realtek", True),
    "com.google.android.leanbacklauncher.partnercustomizer": Known("Налаштування лаунчера Google", GROUP_SAFE,
                                                                   "Для стандартного лаунчера Google, якого тут немає", True),
    "com.google.android.leanbacklauncher.partnerwidget": Known("Віджет лаунчера Google", GROUP_SAFE, "Для лаунчера Google", True),
    "com.google.android.tvsetup.partnercustomizer": Known("Налаштування майстра Google TV", GROUP_SAFE, "Потрібне лише при першому запуску", True),
    "com.google.android.instantapps.supervisor": Known("Миттєві програми Google", GROUP_SAFE, "На телевізорі не використовується", True),
    "com.google.android.apps.turbo": Known("Device Health Services", GROUP_SAFE, "Економія батареї — телевізору не потрібно", True),
    "com.google.android.syncadapters.contacts": Known("Синхронізація контактів Google", GROUP_SAFE, "", True),
    "com.google.android.syncadapters.calendar": Known("Синхронізація календаря Google", GROUP_SAFE, "", True),
    "com.google.android.printservice.recommendation": Known("Підбір служб друку", GROUP_SAFE, "", True),
    "com.google.android.tv.bugreportsender": Known("Надсилання звітів про збої", GROUP_SAFE, "", True),
    "com.google.android.backuptransport": Known("Резервне копіювання Google", GROUP_SAFE, "", True),
    "com.realtek.avcapture.test": Known("Тестова програма Realtek", GROUP_SAFE, "Залишилась із заводу", True),
    "com.realtek.camera": Known("Камера", GROUP_SAFE, "У телевізора немає камери", True),
    "com.android.soundrecorder": Known("Диктофон", GROUP_SAFE, "", True),
    "com.android.wallpaper.livepicker": Known("Живі шпалери", GROUP_SAFE, "", True),
    "com.kivi.authorizer": Known("Акаунт KIVI", GROUP_SAFE),
    "com.teamviewer.quicksupport.market": Known("TeamViewer QuickSupport", GROUP_SAFE, "Віддалена допомога"),
    "com.funshion.airplay": Known("AirPlay (Funshion)", GROUP_CAREFUL, "Трансляція з iPhone/Mac"),
    "com.funshion.tvdlnarender": Known("DLNA-трансляція (Funshion)", GROUP_CAREFUL, "«Відтворити на телевізорі» з телефона"),
    "com.wezom.kiviremoteserver": Known("KIVI Remote (керування з телефона)", GROUP_CAREFUL,
                                       "Їсть ~30 МБ і процесор; вимикайте, якщо не керуєте телевізором з телефона"),
    "com.rtk.android.miracast": Known("Miracast (дублювання екрана телефона)", GROUP_CAREFUL, "Потрібен для показу екрана телефона"),
    "net.megogo.tv.preinstall": Known("MEGOGO", GROUP_APPS),
    "org.smarttube.stable": Known("SmartTube", GROUP_USER, "YouTube без реклами"),
    "org.smarttube.beta": Known("SmartTube Beta", GROUP_USER, "YouTube без реклами"),
    "org.videolan.vlc": Known("VLC", GROUP_APPS, "Відеоплеєр для файлів з флешки"),
    # ── програми ──
    "com.google.android.youtube.tv": Known("YouTube", GROUP_APPS),
    "com.google.android.youtube.tvmusic": Known("YouTube Music", GROUP_APPS),
    "com.netflix.ninja": Known("Netflix", GROUP_APPS),
    "com.amazon.amazonvideo.livingroom": Known("Prime Video", GROUP_APPS),
    "com.disney.disneyplus": Known("Disney+", GROUP_APPS),
    "com.spotify.tv.android": Known("Spotify", GROUP_APPS),
    "com.megogo.application": Known("MEGOGO", GROUP_APPS),
    "tv.twitch.android.app": Known("Twitch", GROUP_APPS),
    "com.plexapp.android": Known("Plex", GROUP_APPS),
    "org.xbmc.kodi": Known("Kodi", GROUP_APPS),
    "com.google.android.play.games.tv": Known("Google Play Ігри", GROUP_SAFE, "", True),
    "com.google.android.apps.youtube.kids": Known("YouTube Kids", GROUP_SAFE, "", True),
    "com.spocky.projengmenu": Known("Projectivy Launcher", GROUP_USER, "Головний екран"),
    "ua.x44to.lad": Known("Лад (головний екран)", GROUP_USER, "Головний екран від ТВ Лад"),
    "com.teamsmart.videomanager.tv": Known("SmartTube", GROUP_USER, "YouTube без реклами"),
    "com.liskovsoft.smarttubetv.beta": Known("SmartTube Beta", GROUP_USER, "YouTube без реклами"),
    # ── обережно ──
    "com.google.android.apps.mediashell": Known("Chromecast (трансляція з телефона)", GROUP_CAREFUL,
                                                "Без нього не вийде транслювати відео з телефона на телевізор"),
    "com.google.android.katniss": Known("Голосовий пошук і Асистент Google", GROUP_CAREFUL,
                                        "Перестане працювати кнопка мікрофона на пульті"),
    "com.google.android.speech.pumpkin": Known("Розпізнавання мовлення", GROUP_CAREFUL, "Потрібне для голосового пошуку"),
    "com.google.android.tts": Known("Синтез мовлення Google", GROUP_CAREFUL, "Озвучення тексту"),
    "com.google.android.marvin.talkback": Known("TalkBack (озвучення екрана)", GROUP_CAREFUL, "Для людей з вадами зору"),
    "com.google.android.apps.tv.launcherx": Known("Головний екран Google TV", GROUP_CAREFUL),
    "com.android.vending": Known("Google Play Маркет", GROUP_CAREFUL, "Без нього не встановити й не оновити програми"),
}

# ніколи не чіпаємо (і не показуємо)
PROTECTED_EXACT = {
    "android", "com.android.tv", "com.android.vending", "com.android.shell", "com.android.systemui",
    "com.google.android.gms", "com.google.android.gsf", "com.google.android.tvlauncher",
    "com.google.android.leanbacklauncher", "com.google.android.apps.tv.launcherx",
    "com.android.documentsui", "com.android.musicfx", "com.android.keychain", "com.android.certinstaller",
}
PROTECTED_PREFIXES = (
    "com.android.providers.", "com.android.tv.", "com.google.android.tv.", "com.android.settings",
    "com.android.server.", "com.android.location.", "com.android.inputdevices", "com.android.packageinstaller",
    "com.google.android.packageinstaller", "com.android.permissioncontroller", "com.google.android.permissioncontroller",
    "com.android.externalstorage", "com.android.networkstack", "com.google.android.networkstack",
    "com.android.wifi", "com.google.android.wifi", "com.android.bluetooth", "com.android.media",
    "com.google.android.media", "com.android.tethering", "com.google.android.tethering", "com.android.webview",
    "com.google.android.webview", "com.google.android.ext.", "com.android.ext.", "com.google.android.tungsten",
    "com.google.android.leanback.", "com.google.android.inputmethod", "com.android.inputmethod",
    "com.google.android.gms.", "com.google.android.gsf.", "com.google.android.configupdater",
    "com.google.android.modulemetadata", "com.android.modulemetadata", "com.google.android.overlay",
    "com.android.overlay", "com.android.internal.", "com.android.wallpaper", "com.android.backupconfirm",
    "com.android.sharedstoragebackup", "com.android.statementservice", "com.android.proxyhandler",
    "com.android.vpndialogs", "com.android.carrierconfig", "com.android.cts.", "com.android.dynsystem",
    "com.android.localtransport", "com.android.storagemanager", "com.android.traceur", "com.android.se",
    "com.android.nfc", "com.android.emergency", "com.android.cellbroadcast", "com.google.android.cellbroadcast",
    "com.google.android.apps.tv.launcherx",
    # мікросхеми телевізорів: тюнер, входи HDMI, картинка, звук
    "com.mediatek.", "com.mstar.", "com.realtek.", "com.rtk.", "com.amlogic.", "com.droidlogic.", "com.hisilicon.",
    "com.novatek.", "com.cvte.", "com.softwinner.", "com.allwinner.", "com.rockchip.", "com.tpv.", "com.mitv.",
)
PROTECTED_WORDS = (
    "launcher", "setupwizard", "setup", "provision", "ota", "update", "tvinput", "inputservice", "hdmi", "cec",
    "remote", "keyboard", ".ime", "bluetooth", "wifi", "framework", "systemui", "inputmethod", "tuner",
    "tvcenter", "factory", "dtv", "livetv", "tvservice", "tvsystem", "picture", "panel", "audio", "sound",
    "dolby", "partner", "overlay", "permission", "installer", "frameworkres", "devicepolicy",
)


def protected(pkg: str) -> bool:
    if pkg in KNOWN and pkg not in PROTECTED_EXACT:
        return False
    if pkg in PROTECTED_EXACT or pkg.startswith(PROTECTED_PREFIXES):
        return True
    low = pkg.lower()
    return any(w in low for w in PROTECTED_WORDS)


def pretty(pkg: str) -> str:
    """Назва, якщо програма відома, інакше — зрозуміліший вигляд ідентифікатора."""
    k = KNOWN.get(pkg)
    if k:
        return k.name
    parts = [p for p in pkg.split(".") if p not in ("com", "android", "tv", "app", "apps", "ua", "org", "net", "google")]
    words = (parts[-2:] if len(parts) > 1 else parts) or [pkg]
    return " ".join(w.replace("_", " ").capitalize() for w in words)


def classify(pkg: str, system: bool) -> Known | None:
    """Група для показу; None — програму не показуємо (захищена)."""
    if protected(pkg):
        return None
    k = KNOWN.get(pkg)
    if k:
        if k.group == GROUP_USER or (not system and k.group in (GROUP_APPS, GROUP_SAFE)):
            return Known(k.name, GROUP_USER if not system else k.group, k.tip, False)
        return k
    if not system:
        return Known(pretty(pkg), GROUP_USER)
    if pkg.startswith(AOSP_NS):
        return None  # невідомі частини самого Android чи Google — не показуємо, щоб нічого не зламати
    return Known(pretty(pkg), GROUP_SYSTEM, "Встановлено виробником")


AOSP_NS = ("android.", "com.android.", "com.google.", "androidx.")


PROJECTIVY = "com.spocky.projengmenu"
LAD = "ua.x44to.lad"
SMARTTUBE_PKGS = ("org.smarttube.stable", "org.smarttube.beta", "com.teamsmart.videomanager.tv",
                  "com.liskovsoft.smarttubetv.beta", "com.liskovsoft.smarttubetv")
SMARTTUBE_URL = "https://github.com/yuliskov/SmartTube/releases/download/latest/smarttube_stable.apk"
STOCK_YOUTUBE = "com.google.android.youtube.tv"
STOCK_LAUNCHERS = ("com.google.android.tvlauncher", "com.google.android.leanbacklauncher",
                   "com.google.android.apps.tv.launcherx")
# «запасний» головний екран системи — його не вимикаємо ніколи
FALLBACK_HOME_PKGS = ("com.android.tv.settings", "com.android.settings", "com.android.provision",
                      "com.google.android.tungsten.setupwraith")
