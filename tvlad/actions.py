"""
Дії з журналом: усе, що програма змінює на телевізорі, записується, щоб «Відновити все» могло повернути як було.
Журнал лежить на комп'ютері: %LOCALAPPDATA%\\TVLad\\history-<серійний номер>.json.
"""
from __future__ import annotations

import json
import os
import re
import time

from . import catalog
from .adb import AdbError, data_dir, download
from .tv import ANIM_KEYS, TV


class History:
    def __init__(self, key: str, folder: str | None = None):
        safe = re.sub(r"[^A-Za-z0-9_.-]", "_", key)[:60] or "tv"
        self.path = os.path.join(folder or data_dir(), f"history-{safe}.json")
        self.data = {"disabled": [], "animations": None, "launchers": [], "log": []}
        try:
            with open(self.path, encoding="utf-8") as f:
                loaded = json.load(f)
            if isinstance(loaded, dict):
                self.data.update(loaded)
        except (OSError, ValueError):
            pass

    def save(self) -> None:
        tmp = self.path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(self.data, f, ensure_ascii=False, indent=1)
        os.replace(tmp, self.path)

    def log(self, text: str) -> None:
        self.data["log"] = (self.data.get("log") or [])[-199:] + [time.strftime("%d.%m %H:%M ") + text]
        self.save()

    @property
    def disabled(self) -> list[str]:
        return list(self.data.get("disabled") or [])

    @property
    def launchers(self) -> list[str]:
        return list(self.data.get("launchers") or [])

    @property
    def empty(self) -> bool:
        return not self.disabled and not self.launchers and not self.data.get("animations")


class Actions:
    def __init__(self, tv: TV, history: History):
        self.tv = tv
        self.h = history

    # ── швидкість ──
    def set_animations(self, value: float) -> None:
        if not self.h.data.get("animations"):
            self.h.data["animations"] = self.tv.animations()  # що було до першої зміни
        self.tv.set_animations(value)
        self.h.log({1.0: "Анімації: звичайні", 0.5: "Анімації: швидкі (0,5×)", 0.0: "Анімації вимкнено"}.get(value, f"Анімації {value}×"))

    def restore_animations(self) -> None:
        old = self.h.data.get("animations")
        if not old:
            return
        for k in ANIM_KEYS:
            v = (old.get(k) or "null").strip()
            if v in ("null", ""):
                self.tv.sh(f"settings delete global {k}")
            else:
                self.tv.sh(f"settings put global {k} {v}")
        self.h.data["animations"] = None
        self.h.log("Анімації повернуто як було")

    def boost(self) -> dict:
        """Прискорити все разом: анімації 0,5×, кеш, фонові програми."""
        self.set_animations(0.5)
        freed = self.tv.trim_caches()
        self.tv.kill_background()
        self.h.log(f"Прискорення: кеш −{freed // 1024} МБ, фонові програми вивантажено")
        return {"freed_kb": freed}

    # ── програми ──
    def disable(self, pkgs: list[str]) -> tuple[list[str], list[str]]:
        ok, bad = [], []
        for p in pkgs:
            if catalog.protected(p) and p not in catalog.KNOWN:
                bad.append(p)
                continue
            try:
                if self.tv.disable(p):
                    ok.append(p)
                    if p not in self.h.data["disabled"]:
                        self.h.data["disabled"].append(p)
                else:
                    bad.append(p)
            except AdbError:
                bad.append(p)
        if ok:
            self.h.log("Вимкнено: " + ", ".join(catalog.pretty(p) for p in ok))
        return ok, bad

    def enable(self, pkgs: list[str]) -> tuple[list[str], list[str]]:
        ok, bad = [], []
        for p in pkgs:
            try:
                if self.tv.enable(p):
                    ok.append(p)
                    if p in self.h.data["disabled"]:
                        self.h.data["disabled"].remove(p)
                else:
                    bad.append(p)
            except AdbError:
                bad.append(p)
        if ok:
            self.h.log("Увімкнено: " + ", ".join(catalog.pretty(p) for p in ok))
        return ok, bad

    def uninstall(self, pkgs: list[str]) -> tuple[list[str], list[str]]:
        ok, bad = [], []
        for p in pkgs:
            try:
                (ok if self.tv.uninstall(p) else bad).append(p)
            except AdbError:
                bad.append(p)
        if ok:
            self.h.log("Видалено: " + ", ".join(catalog.pretty(p) for p in ok))
        return ok, bad

    # ── головний екран ──
    def set_home(self, new_pkg: str) -> str:
        """Зробити [new_pkg] головним: вимикаємо інші головні екрани (їх можна повернути)."""
        tv = self.tv
        if not tv.installed(new_pkg):
            raise AdbError("Цей головний екран ще не встановлено на телевізорі.")
        if not tv.enabled(new_pkg):
            tv.enable(new_pkg)
        others = [p for p in tv.home_candidates() if p != new_pkg and p not in catalog.FALLBACK_HOME_PKGS]
        try:
            tv.set_home_activity(new_pkg)  # обрати за замовчуванням (де система дозволяє)
        except AdbError:
            pass
        done = []
        for p in others:
            if tv.disable(p):
                done.append(p)
        for p in done:
            if p not in self.h.data["launchers"]:
                self.h.data["launchers"].append(p)
        self.h.save()
        tv.press_home()
        time.sleep(1.0)
        if tv.current_home() != new_pkg:
            # щось пішло не так — повертаємо стандартний, щоб телевізор не лишився без головного екрана
            for p in done:
                tv.enable(p)
                if p in self.h.data["launchers"]:
                    self.h.data["launchers"].remove(p)
            self.h.save()
            tv.press_home()
            raise AdbError("Не вдалося зробити його головним — повернув стандартний головний екран.")
        self.h.data["home"] = new_pkg
        self.h.save()
        self.h.log(f"Головний екран: {catalog.pretty(new_pkg)} (вимкнено {', '.join(done) or '—'})")
        return new_pkg

    def restore_home(self) -> list[str]:
        back = []
        for p in self.h.launchers:
            try:
                if self.tv.enable(p):
                    back.append(p)
            except AdbError:
                pass
            if p in self.h.data["launchers"]:
                self.h.data["launchers"].remove(p)
        # свій головний екран (Лад, Projectivy) вимикаємо, щоб система не питала, який обрати;
        # «Зробити головним» увімкне його знову
        custom = self.h.data.get("home")
        if back and custom and custom not in back:
            try:
                self.tv.disable(custom)
            except AdbError:
                pass
        self.h.data["home"] = None
        self.h.save()
        if back:
            try:
                self.tv.set_home_activity(back[0])  # щоб кнопка «Додому» знову вела на стандартний
            except AdbError:
                pass
        self.tv.press_home()
        if back:
            self.h.log("Повернуто стандартний головний екран")
        return back

    def restore_all(self) -> dict:
        res = {"home": self.restore_home()}
        res["apps"] = self.enable(self.h.disabled)[0]
        self.restore_animations()
        return res

    # ── встановлення ──
    def install_smarttube(self, progress=None, hide_stock: bool = False) -> str:
        """YouTube без реклами: SmartTube з офіційного GitHub розробника."""
        apk = download(catalog.SMARTTUBE_URL, os.path.join(data_dir(), "apk", "smarttube_stable.apk"), progress)
        self.tv.install(apk)
        pkg = next((p for p in catalog.SMARTTUBE_PKGS if self.tv.installed(p)), None)
        if not pkg:
            raise AdbError("SmartTube встановився, але телевізор його не показує — спробуйте ще раз.")
        self.h.log("Встановлено SmartTube (YouTube без реклами)")
        if hide_stock and self.tv.installed(catalog.STOCK_YOUTUBE) and self.tv.enabled(catalog.STOCK_YOUTUBE):
            self.disable([catalog.STOCK_YOUTUBE])
        return pkg

    def install_lad(self, apk: str, make_home: bool = True) -> str:
        """Головний екран «Лад»: встановити (або оновити) і зробити головним."""
        if not os.path.exists(apk):
            raise AdbError("У цій збірці ТВ Лад немає файлу головного екрана «Лад».")
        self.tv.install(apk)
        self.h.log("Встановлено головний екран «Лад»")
        if make_home:
            self.set_home(catalog.LAD)
        return catalog.LAD

