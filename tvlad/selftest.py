"""Самоперевірка зібраної програми (exe): без телевізора, на уявному KIVI. Результат — у файл."""
from __future__ import annotations

import os
import sys
import tempfile
import traceback


def run(out_path: str, with_adb: bool = False, shots: str | None = None) -> int:
    lines: list[str] = []
    ok = True

    def check(name, fn):
        nonlocal ok
        try:
            r = fn()
            lines.append(f"OK   {name}" + (f" — {r}" if r else ""))
        except Exception:  # noqa: BLE001
            ok = False
            lines.append(f"FAIL {name}\n{traceback.format_exc()}")

    from . import catalog
    from .actions import Actions, History
    from .fake import FakeAdb
    from .tv import TV

    tmp = tempfile.mkdtemp()

    def core():
        adb = FakeAdb()
        tv = TV(adb)
        act = Actions(tv, History("selftest", tmp))
        assert tv.info().title == "KIVI 43U750NB"
        assert act.boost()["freed_kb"] > 0
        assert act.disable(["com.google.android.play.games"])[0] == ["com.google.android.play.games"]
        assert act.disable(["com.android.systemui"])[0] == []
        act.set_home(catalog.PROJECTIVY)
        assert tv.current_home() == catalog.PROJECTIVY
        res = act.restore_all()
        assert tv.current_home() == "com.google.android.tvlauncher", res
        assert adb.settings["window_animation_scale"] == "1.0"
        return f"{len(tv.apps())} програм на уявному TV"

    def gui():
        from .app import Main, make_app, screenshots
        make_app([sys.argv[0]])
        w = Main()
        assert len(w.pages) == 5
        w.close()
        if shots:
            files = screenshots(shots, tmp)
            return f"знімки: {len(files)}"
        return ""

    def adb_download():
        from .adb import Adb, download_adb, find_adb
        exe = find_adb() or download_adb()
        return Adb(exe).version()

    check("логіка (уявний телевізор)", core)
    check("вікно програми", gui)
    if with_adb:
        check("завантаження adb від Google", adb_download)
    lines.append("RESULT " + ("OK" if ok else "FAIL"))
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    return 0 if ok else 1
