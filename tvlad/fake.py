"""Уявний телевізор KIVI для самоперевірки й знімків екрана (без справжнього пристрою)."""
from __future__ import annotations

import os
import re

from .adb import AdbError

SYSTEM = [
    "android", "com.android.systemui", "com.android.tv.settings", "com.android.vending", "com.google.android.gms",
    "com.google.android.tvlauncher", "com.google.android.tvrecommendations", "com.google.android.play.games",
    "com.google.android.videos", "com.google.android.youtube.tv", "com.google.android.youtube.tvmusic",
    "com.google.android.youtube.tvkids", "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
    "com.megogo.application", "com.google.android.apps.mediashell", "com.google.android.katniss",
    "com.google.android.backdrop", "com.android.printspooler", "com.google.android.feedback",
    "com.mediatek.wwtv.tvcenter", "com.android.providers.tv", "com.google.android.leanback.ime",
    "com.kivi.mediacenter", "com.kivi.tvguide", "com.android.dreams.basic", "com.google.android.tts",
]
USER = ["com.spocky.projengmenu", "ua.sweet.tv", "com.vlc.android.tv"]


class FakeAdb:
    def __init__(self, with_projectivy: bool = True):
        self.serial = "192.168.1.45:5555"
        self.system = list(SYSTEM)
        self.user = list(USER if with_projectivy else [u for u in USER if u != "com.spocky.projengmenu"])
        self.disabled: set[str] = set()
        self.settings = {"window_animation_scale": "1.0", "transition_animation_scale": "1.0", "animator_duration_scale": "null"}
        self.free_kb = 3_512_000
        self.preferred: str | None = None
        self.log: list[str] = []

    def install(self, apk: str, timeout: float = 600) -> None:
        name = os.path.basename(apk).lower()
        pkg = "com.teamsmart.videomanager.tv" if "smarttube" in name else ("ua.x44to.lad" if "lad" in name else "com.example.app")
        if pkg not in self.user:
            self.user.append(pkg)
        self.log.append(f"install {apk}")

    # те саме, що в справжнього Adb
    def state(self) -> str:
        return "device"

    def connect(self, host: str, port: int = 5555) -> str:
        self.serial = f"{host}:{port}"
        return "device"

    def wait_authorized(self, timeout: float = 90, tick=None) -> bool:
        return True

    def disconnect(self) -> None:
        pass

    def homes(self) -> list[str]:
        h = [p for p in ("com.google.android.tvlauncher", "com.spocky.projengmenu", "ua.x44to.lad") if p in self.all() and p not in self.disabled]
        return h

    def all(self) -> list[str]:
        return self.system + self.user

    def shell(self, cmd: str, timeout: float = 60) -> str:
        self.log.append(cmd)
        if cmd == "getprop":
            return ("[ro.product.brand]: [KIVI]\n[ro.product.model]: [43U750NB]\n[ro.build.version.release]: [11]\n"
                    "[ro.build.version.sdk]: [30]\n[ro.serialno]: [KV43U750NB0001]\n")
        if cmd.startswith("df -k"):
            return f"Filesystem 1K-blocks Used Available Use% Mounted on\n/dev/block/dm-5 10485760 {10485760 - self.free_kb} {self.free_kb} 66% /data\n"
        if cmd == "cat /proc/meminfo":
            return "MemTotal:        1931264 kB\nMemFree:          112640 kB\nMemAvailable:     688128 kB\n"
        m = re.match(r"pm list packages(?: (-[sd3]))?(?: (\S+))?$", cmd)
        if m:
            flag, flt = m.group(1), m.group(2)
            src = {"-s": self.system, "-3": self.user, "-d": sorted(self.disabled), None: self.all()}[flag]
            return "".join(f"package:{p}\n" for p in src if not flt or flt in p)
        m = re.match(r"pm disable-user --user 0 (\S+)$", cmd)
        if m:
            p = m.group(1)
            if p not in self.all():
                return f"Error: Unknown package: {p}"
            self.disabled.add(p)
            return f"Package {p} new state: disabled-user"
        m = re.match(r"pm enable (\S+)$", cmd)
        if m:
            self.disabled.discard(m.group(1))
            return f"Package {m.group(1)} new state: enabled"
        m = re.match(r"pm uninstall (\S+)$", cmd)
        if m:
            p = m.group(1)
            if p in self.user:
                self.user.remove(p)
                return "Success"
            return "Failure [DELETE_FAILED_INTERNAL_ERROR]"
        m = re.match(r"settings get global (\w+)$", cmd)
        if m:
            return self.settings.get(m.group(1), "null") + "\n"
        m = re.match(r"settings put global (\w+) (\S+)$", cmd)
        if m:
            self.settings[m.group(1)] = m.group(2)
            return ""
        m = re.match(r"settings delete global (\w+)$", cmd)
        if m:
            self.settings[m.group(1)] = "null"
            return "Deleted 1 rows\n"
        if cmd.startswith("pm trim-caches"):
            self.free_kb += 412_000
            return ""
        if "query-activities" in cmd:
            return "".join(f"priority=0 preferredOrder=0 match=0x108000\n{p}/.Main\n" for p in self.homes()) + \
                "priority=-1000\ncom.android.tv.settings/.FallbackHome\n"
        m = re.match(r"cmd package set-home-activity (\S+)/\S+$", cmd)
        if m:
            self.preferred = m.group(1)
            return "Success\n"
        if "resolve-activity" in cmd:
            h = self.homes()
            if self.preferred in h:
                return f"priority=0\n{self.preferred}/.Main\n"
            if len(h) == 1:
                return f"priority=0 preferredOrder=0\n{h[0]}/.Main\n"
            if not h:
                return "priority=-1000\ncom.android.tv.settings/.FallbackHome\n"
            return "priority=0\nandroid/com.android.internal.app.ResolverActivity\n" if "com.google.android.tvlauncher" not in h \
                else "priority=0\ncom.google.android.tvlauncher/.MainActivity\n"
        if cmd.startswith(("am ", "input ", "monkey ", "cmd package bg-dexopt-job")):
            return ""
        raise AdbError(f"FakeAdb: невідома команда {cmd}")
