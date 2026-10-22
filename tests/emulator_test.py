"""
Перевірка на справжньому Android TV (емулятор у CI): підключення по мережі, відомості, програми,
анімації, кеш, головний екран — і повернення всього назад.
Запуск: python tests/emulator_test.py <host> <port> <звіт.txt> [launcher.apk]
"""
import os
import shutil
import sys
import tempfile
import time
import traceback

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from tvlad import catalog  # noqa: E402
from tvlad.actions import Actions, History  # noqa: E402
from tvlad.adb import Adb, AdbError  # noqa: E402
from tvlad.tv import TV  # noqa: E402

host, port, report = sys.argv[1], int(sys.argv[2]), sys.argv[3]
apk = sys.argv[4] if len(sys.argv) > 4 else None
lines, ok = [], True


def step(name, fn):
    global ok
    try:
        r = fn()
        lines.append(f"OK   {name}" + (f" — {r}" if r not in (None, "") else ""))
        return r
    except Exception:  # noqa: BLE001
        ok = False
        lines.append(f"FAIL {name}\n{traceback.format_exc()}")
        return None


adb = Adb(os.environ.get("TVLAD_ADB") or shutil.which("adb"))
tv = TV(adb)
act = Actions(tv, History("emulator", tempfile.mkdtemp()))


def connect():
    st = adb.connect(host, port)
    if st == "unauthorized":
        assert adb.wait_authorized(30), "unauthorized"
    assert adb.state() == "device", adb.state()
    return adb.serial


step("підключення по мережі", connect)
info = step("відомості", lambda: (lambda i: f"{i.title}, Android {i.android} (SDK {i.sdk}), вільно {i.storage_free_kb // 1024} МБ, "
                                             f"RAM {i.ram_total_kb // 1024} МБ")(tv.info()))
apps = tv.apps()
shown = [(a, catalog.classify(a.pkg, a.system)) for a in apps]
shown = [(a, k) for a, k in shown if k]
step("список програм", lambda: f"усього {len(apps)}, показуємо {len(shown)}, захищено {len(apps) - len(shown)}")
lines.append("     показуємо: " + ", ".join(sorted(a.pkg for a, _ in shown)))


def animations():
    act.set_animations(0.5)
    assert tv.animations()["window_animation_scale"] == "0.5"
    act.set_animations(0)
    assert tv.animations()["animator_duration_scale"] == "0"
    act.restore_animations()
    return str(tv.animations())


step("анімації та їх повернення", animations)


def disable_enable():
    cand = next((a.pkg for a, k in shown if a.system and a.enabled and k.group in (catalog.GROUP_SAFE, catalog.GROUP_SYSTEM)), None)
    assert cand, "немає кандидата"
    done, bad = act.disable([cand])
    assert done == [cand], bad
    assert not tv.enabled(cand)
    act.restore_all()
    assert tv.enabled(cand)
    return cand


step("вимкнути й увімкнути програму", disable_enable)
def protected_stays():
    done, _ = act.disable(["com.android.systemui", "com.android.tv.settings"])
    assert done == [], done
    assert tv.enabled("com.android.systemui")


step("захищене не вимикається", protected_stays)
step("очищення кешу", lambda: f"звільнено {tv.trim_caches() // 1024} МБ")
step("фонові програми", tv.kill_background)
home0 = step("поточний головний екран", tv.current_home)
step("головні екрани", lambda: ", ".join(tv.home_candidates()))


def no_projectivy():
    try:
        act.set_home(catalog.PROJECTIVY)
    except AdbError as e:
        assert tv.current_home() == home0
        return f"відмовлено як слід: {e}"
    raise AssertionError("set_home без встановленого лаунчера мав відмовити")


step("захист: лаунчер не встановлено", no_projectivy)

if apk and os.path.exists(apk):
    def other_launcher():
        rc, out, err = adb.run(["-s", adb.serial, "install", "-r", apk], timeout=240)
        assert "Success" in out + err, out + err
        cands = tv.home_candidates()
        new = next(p for p in cands if p != home0)
        act.set_home(new)
        assert tv.current_home() == new
        act.restore_home()
        time.sleep(1)
        assert tv.current_home() == home0, tv.current_home()
        return f"{new} → головний і назад"

    step("зміна головного екрана й повернення", other_launcher)
else:
    lines.append("SKIP зміна головного екрана (немає APK лаунчера)")

lines.append("RESULT " + ("OK" if ok else "FAIL"))
with open(report, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print("\n".join(lines))
sys.exit(0 if ok else 1)
