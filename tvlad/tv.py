"""Що вміємо робити з телевізором (команди Android через adb shell)."""
from __future__ import annotations

import re
from dataclasses import dataclass

from . import catalog
from .adb import PKG_RE, AdbError

ANIM_KEYS = ("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
HOME_INTENT = "-a android.intent.action.MAIN -c android.intent.category.HOME"


# ───────── розбір відповідей (окремо — щоб перевіряти тестами) ─────────

def parse_packages(out: str) -> list[str]:
    res = []
    for line in out.splitlines():
        line = line.strip()
        if line.startswith("package:"):
            name = line[len("package:"):].strip()
            if "=" in name:  # формат «package:/шлях/base.apk=назва»
                name = name.rsplit("=", 1)[1]
            if PKG_RE.match(name) or name == "android":
                res.append(name)
    return res


def parse_getprop(out: str) -> dict[str, str]:
    props = {}
    for m in re.finditer(r"^\[([^\]]+)\]: \[(.*)\]\s*$", out, re.M):
        props[m.group(1)] = m.group(2)
    return props


def parse_df(out: str) -> tuple[int, int]:
    """(усього КБ, вільно КБ) з `df -k /data`."""
    lines = [l for l in out.splitlines() if l.strip()]
    for line in reversed(lines):
        cols = line.split()
        nums = [c for c in cols if c.isdigit()]
        if len(nums) >= 3:
            total, _used, avail = int(nums[0]), int(nums[1]), int(nums[2])
            return total, avail
    return 0, 0


def parse_meminfo(out: str) -> tuple[int, int]:
    vals = {}
    for m in re.finditer(r"^(\w+):\s+(\d+)\s*kB", out, re.M):
        vals[m.group(1)] = int(m.group(2))
    total = vals.get("MemTotal", 0)
    avail = vals.get("MemAvailable", vals.get("MemFree", 0) + vals.get("Cached", 0))
    return total, avail


def parse_activities(out: str) -> list[str]:
    """Пакети з виводу `cmd package query-activities/resolve-activity` (коротка форма --brief
    або повна, як на Android 7: рядки «packageName=…»)."""
    pkgs = []
    for line in out.splitlines():
        m = re.match(r"^\s*([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)\s*$", line)
        if not m:
            m = re.match(r"^\s*packageName=([A-Za-z0-9_.]+)\s*$", line)
        if m and m.group(1) not in pkgs:
            pkgs.append(m.group(1))
    return pkgs


def parse_components(out: str) -> list[str]:
    """«пакет/активність» з виводу `--brief` (як є, для set-home-activity)."""
    res = []
    for line in out.splitlines():
        m = re.match(r"^\s*([A-Za-z0-9_.]+/[A-Za-z0-9_.$]+)\s*$", line)
        if m and m.group(1) not in res:
            res.append(m.group(1))
    return res


def parse_scale(v: str) -> float:
    try:
        return float(v.strip())
    except ValueError:
        return 1.0  # «null» — значення за замовчуванням


@dataclass
class App:
    pkg: str
    system: bool
    enabled: bool


@dataclass
class Info:
    brand: str
    model: str
    android: str
    sdk: int
    serial: str
    storage_total_kb: int
    storage_free_kb: int
    ram_total_kb: int
    ram_free_kb: int

    @property
    def title(self) -> str:
        name = f"{self.brand} {self.model}".strip()
        return name or "Android TV"


def check_pkg(pkg: str) -> str:
    if not PKG_RE.match(pkg):
        raise AdbError(f"Дивна назва пакета: {pkg!r}")
    return pkg


class TV:
    def __init__(self, adb):
        self.adb = adb

    def sh(self, cmd: str, timeout: float = 60) -> str:
        return self.adb.shell(cmd, timeout=timeout)

    # ── відомості ──
    def info(self) -> Info:
        props = parse_getprop(self.sh("getprop"))
        total, free = parse_df(self.sh("df -k /data"))
        mt, ma = parse_meminfo(self.sh("cat /proc/meminfo"))
        try:
            sdk = int(props.get("ro.build.version.sdk", "0"))
        except ValueError:
            sdk = 0
        brand = props.get("ro.product.brand") or props.get("ro.product.manufacturer") or ""
        return Info(
            brand=brand.upper() if brand.lower() == "kivi" else brand,
            model=props.get("ro.product.model", ""),
            android=props.get("ro.build.version.release", "?"),
            sdk=sdk,
            serial=props.get("ro.serialno") or (self.adb.serial or "tv"),
            storage_total_kb=total, storage_free_kb=free, ram_total_kb=mt, ram_free_kb=ma,
        )

    def free_kb(self) -> int:
        return parse_df(self.sh("df -k /data"))[1]

    # ── програми ──
    def apps(self) -> list[App]:
        allp = parse_packages(self.sh("pm list packages"))
        system = set(parse_packages(self.sh("pm list packages -s")))
        disabled = set(parse_packages(self.sh("pm list packages -d")))
        # вимкнені для користувача (disable-user) теж потрапляють у -d; деякі системи їх не показують у загальному списку
        names = sorted(set(allp) | disabled)
        return [App(p, p in system, p not in disabled) for p in names]

    def installed(self, pkg: str) -> bool:
        return check_pkg(pkg) in parse_packages(self.sh(f"pm list packages {pkg}"))

    def enabled(self, pkg: str) -> bool:
        return check_pkg(pkg) not in parse_packages(self.sh(f"pm list packages -d {pkg}"))

    def disable(self, pkg: str) -> bool:
        out = self.sh(f"pm disable-user --user 0 {check_pkg(pkg)}")
        return "disabled" in out.lower() and "error" not in out.lower() and "exception" not in out.lower()

    def enable(self, pkg: str) -> bool:
        out = self.sh(f"pm enable {check_pkg(pkg)}")
        return "new state: enabled" in out.lower() or "new state: default" in out.lower()

    def install(self, apk: str) -> None:
        self.adb.install(apk)

    def abi(self) -> str:
        return self.sh("getprop ro.product.cpu.abi").strip()

    def uninstall(self, pkg: str) -> bool:
        return "success" in self.sh(f"pm uninstall {check_pkg(pkg)}", timeout=120).lower()

    # ── швидкість ──
    def animations(self) -> dict[str, str]:
        return {k: self.sh(f"settings get global {k}").strip() for k in ANIM_KEYS}

    def set_animations(self, value: float | str) -> None:
        v = value if isinstance(value, str) else ("0" if value == 0 else f"{value:g}")
        for k in ANIM_KEYS:
            if v in ("null", ""):
                self.sh(f"settings delete global {k}")
            else:
                self.sh(f"settings put global {k} {v}")

    def trim_caches(self) -> int:
        """Очистити кеш усіх програм. Повертає, скільки КБ звільнилось."""
        before = self.free_kb()
        self.sh("pm trim-caches 999G", timeout=180)
        after = self.free_kb()
        return max(0, after - before)

    def fstrim(self) -> bool:
        """Повідомити пам'яті eMMC про звільнені блоки (TRIM) — запис знову швидшає. Не всюди дозволено."""
        try:
            out = self.sh("sm fstrim", timeout=600)
        except AdbError:
            return False
        return "error" not in out.lower() and "exception" not in out.lower() and "unknown" not in out.lower()

    def reboot(self) -> None:
        """Повний перезапуск (телевізор місяцями лише «спить» — пам'ять засмічується)."""
        try:
            self.adb.run(["-s", self.adb.serial, "reboot"], timeout=20)
        except AdbError:
            pass

    def kill_background(self) -> None:
        self.sh("am kill-all")

    def deep_optimize(self) -> str:
        """Перекомпілювати програми під цей процесор (довго: 5–20 хв)."""
        return self.sh("cmd package bg-dexopt-job", timeout=3600).strip()

    # ── головний екран ──
    def _query(self, what: str) -> str:
        """query-activities / resolve-activity; на старих Android без «--brief»."""
        out = self.sh(f"cmd package {what} --brief {HOME_INTENT}")
        if not parse_activities(out):
            out = self.sh(f"cmd package {what} {HOME_INTENT}")
        if not parse_activities(out):
            out = self.sh(f"pm {what} {HOME_INTENT}")
        return out

    def home_candidates(self) -> list[str]:
        out = self._query("query-activities")
        return [p for p in parse_activities(out) if p not in catalog.FALLBACK_HOME_PKGS and p != "android"]

    def set_home_activity(self, pkg: str) -> bool:
        """Зробити [pkg] головним екраном за замовчуванням (Android 7+). False — система не дозволила."""
        out = self._query("query-activities")
        comp = next((c for c in parse_components(out) if c.split("/")[0] == check_pkg(pkg)), None)
        if not comp:
            return False
        res = self.sh(f"cmd package set-home-activity {comp}")
        return "success" in res.lower()

    def current_home(self) -> str | None:
        """Пакет головного екрана, що відкривається кнопкою Home; None — система питатиме, який обрати."""
        out = self._query("resolve-activity")
        pk = parse_activities(out)
        if not pk:
            return None
        # коротка форма: останній рядок «пакет/активність»; повна — перший «packageName=»
        p = pk[-1] if "/" in out else pk[0]
        if p == "android":  # ResolverActivity — кілька варіантів без вибору за замовчуванням
            return None
        return p

    def open_store(self, pkg: str) -> None:
        self.sh(f"am start -a android.intent.action.VIEW -d market://details?id={check_pkg(pkg)}")

    def press_home(self) -> None:
        self.sh("input keyevent KEYCODE_HOME")

    def launch(self, pkg: str) -> None:
        self.sh(f"monkey -p {check_pkg(pkg)} -c android.intent.category.LEANBACK_LAUNCHER 1")

    # ── діагностика ──
    REPORT = [
        ("Пристрій", "getprop ro.product.brand; getprop ro.product.model; getprop ro.board.platform; getprop ro.hardware; "
                     "getprop ro.build.version.release; getprop ro.build.display.id; getprop ro.build.fingerprint"),
        ("Час роботи", "uptime"),
        ("Оперативна пам'ять", "cat /proc/meminfo | head -20"),
        ("Хто їсть пам'ять", "dumpsys meminfo | sed -n '/Total PSS by process/,/Total PSS by OOM/p' | head -45"),
        ("Процесор зараз", "top -n 1 -m 15 2>/dev/null || top -b -n 1 | head -25"),
        ("Навантаження процесора (останні хвилини)", "dumpsys cpuinfo | head -30"),
        ("Сховище", "df -h 2>/dev/null || df"),
        ("Розміри програм", "dumpsys diskstats"),
        ("Знос пам'яті eMMC", "for f in /sys/class/mmc_host/mmc*/mmc*/life_time /sys/class/mmc_host/mmc*/mmc*/pre_eol_info "
                              "/sys/class/mmc_host/mmc*/mmc*/name /sys/class/mmc_host/mmc*/mmc*/manfid; do echo \"$f: $(cat $f 2>/dev/null)\"; done"),
        ("Температура", "for z in /sys/class/thermal/thermal_zone*; do echo \"$(cat $z/type 2>/dev/null): $(cat $z/temp 2>/dev/null)\"; done"),
        ("Головний екран", "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null"),
        ("Анімації", "settings get global window_animation_scale; settings get global transition_animation_scale; "
                     "settings get global animator_duration_scale"),
        ("Програми користувача", "pm list packages -3"),
        ("Системні програми", "pm list packages -s"),
        ("Вимкнені програми", "pm list packages -d"),
        ("Запущені служби", "dumpsys activity services | grep -E 'ServiceRecord\\{' | sed 's/.* u0 //' | sort | uniq | head -120"),
        ("Входи телевізора", "dumpsys tv_input 2>/dev/null | head -80"),
    ]

    def report(self, progress=None) -> str:
        """Звіт про стан телевізора (для аналізу, що гальмує)."""
        import time as _t
        out = [f"# ТВ Лад — звіт {_t.strftime('%Y-%m-%d %H:%M')}", ""]
        try:
            i = self.info()
            out += [f"Модель: {i.title} · Android {i.android} (SDK {i.sdk})",
                    f"Оперативна пам'ять: {i.ram_total_kb // 1024} МБ, вільно ≈ {i.ram_free_kb // 1024} МБ",
                    f"Вбудована пам'ять: вільно {i.storage_free_kb // 1024} МБ з {i.storage_total_kb // 1024} МБ", ""]
        except AdbError as e:
            out.append(f"(відомості недоступні: {e})")
        for n, (title, cmd) in enumerate(self.REPORT):
            if progress:
                progress(n, len(self.REPORT))
            try:
                text = self.sh(cmd, timeout=120).rstrip()
            except AdbError as e:
                text = f"(немає даних: {e})"
            if title == "Розміри програм":
                text = summarize_diskstats(text) or text[:3000]
            out += [f"## {title}", "```", text or "(порожньо)", "```", ""]
        return "\n".join(out)


def summarize_diskstats(out: str) -> str:
    """Найбільші програми з `dumpsys diskstats` (Android 8+; на старших — порожньо)."""
    import json as _json

    def arr(name):
        m = re.search(r"^" + re.escape(name) + r": (\[.*\])\s*$", out, re.M)
        if not m:
            return None
        try:
            return _json.loads(m.group(1))
        except ValueError:
            return None

    names, apps, data, cache = arr("Package Names"), arr("App Sizes"), arr("App Data Sizes"), arr("Cache Sizes")
    head = "\n".join(l for l in out.splitlines() if l.startswith(("Data-Free", "Cache-Free", "System-Free", "Latency")))
    if not names or not apps:
        return head
    rows = []
    for k, p in enumerate(names):
        a = apps[k] if k < len(apps) else 0
        d = data[k] if data and k < len(data) else 0
        c = cache[k] if cache and k < len(cache) else 0
        rows.append((a + d + c, p, a, d, c))
    rows.sort(reverse=True)
    lines = [head, "", f"{'усього МБ':>9} {'програма':>9} {'дані':>7} {'кеш':>7}  пакет"]
    for t, p, a, d, c in rows[:30]:
        lines.append(f"{t / 1048576:9.0f} {a / 1048576:9.0f} {d / 1048576:7.0f} {c / 1048576:7.0f}  {p}")
    return "\n".join(lines)

