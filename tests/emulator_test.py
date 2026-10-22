"""
Перевірка на справжньому Android TV (емулятор у CI): підключення по мережі, відомості, програми,
анімації, кеш, головний екран — і повернення всього назад.
Запуск: python tests/emulator_test.py <host> <port> <звіт.txt> [launcher.apk] [lad-home.apk] [тека знімків]
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
lad_apk = sys.argv[5] if len(sys.argv) > 5 else None
shots = sys.argv[6] if len(sys.argv) > 6 else None
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



def screencap(name):
    if not shots:
        return
    import subprocess
    os.makedirs(shots, exist_ok=True)
    data = subprocess.run([adb.exe, "-s", adb.serial, "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
    with open(os.path.join(shots, name + ".png"), "wb") as f:
        f.write(data)


def lad_alive():
    out = tv.sh("ps -A 2>/dev/null || ps")
    return "ua.x44to.lad" in out


if lad_apk and os.path.exists(lad_apk):
    def lad_install():
        act.install_lad(lad_apk)
        assert tv.current_home() == catalog.LAD, tv.current_home()
        time.sleep(4)
        assert lad_alive(), "«Лад» не запустився"
        screencap("lad-1-home")
        return "встановлено й зроблено головним"

    step("«Лад»: встановлення й головний екран", lad_install)

    def ui_text():
        tv.sh("uiautomator dump /sdcard/ui.xml >/dev/null 2>&1")
        return tv.sh("cat /sdcard/ui.xml")

    def lad_demo():
        tv.sh("am start -n ua.x44to.lad/.MainActivity --ez lad.demo true")
        time.sleep(5)
        screencap("lad-2-shelf-input")
        # утримання OK на першому вході → меню → вибрати «Xbox»
        tv.sh("input keyevent --longpress KEYCODE_DPAD_CENTER")
        time.sleep(2)
        screencap("lad-3-input-menu")
        assert "що підключено" in ui_text(), "меню входу не відкрилось від утримання OK"
        tv.sh("input keyevent KEYCODE_DPAD_DOWN")
        tv.sh("input keyevent KEYCODE_DPAD_CENTER")
        time.sleep(2)
        assert "Xbox" in ui_text(), "вибір у меню не спрацював"
        # кнопка Menu теж відкриває меню
        tv.sh("input keyevent KEYCODE_MENU")
        time.sleep(2)
        assert "що підключено" in ui_text(), "кнопка Menu не відкрила меню"
        tv.sh("input keyevent KEYCODE_BACK")
        time.sleep(1)
        for _ in range(4):
            tv.sh("input keyevent KEYCODE_DPAD_RIGHT")
        tv.sh("input keyevent KEYCODE_DPAD_DOWN")
        time.sleep(2)
        screencap("lad-4-shelf-app")
        tv.sh("input keyevent KEYCODE_DPAD_DOWN")
        tv.sh("input keyevent KEYCODE_DPAD_DOWN")
        time.sleep(2)
        screencap("lad-5-grid")
        assert lad_alive(), "«Лад» упав під час навігації"
        crashes = tv.sh("logcat -d -b crash 2>/dev/null | tail -40")
        assert "ua.x44to.lad" not in crashes, crashes
        return ""

    step("«Лад»: вітрина, утримання OK, Menu, сітка (без падінь)", lad_demo)

    def lad_settings():
        for _ in range(10):
            tv.sh("input keyevent KEYCODE_DPAD_DOWN")
        for _ in range(10):
            tv.sh("input keyevent KEYCODE_DPAD_RIGHT")
        time.sleep(1)
        tv.sh("input keyevent KEYCODE_DPAD_CENTER")
        time.sleep(2)
        screencap("lad-6-settings")
        tv.sh("input keyevent KEYCODE_BACK")
        assert lad_alive()
        return ""

    step("«Лад»: налаштування", lad_settings)

    def lad_restore():
        act.restore_home()
        time.sleep(2)
        assert tv.current_home() == home0, tv.current_home()
        return f"повернуто {home0}"

    step("повернути стандартний після «Лад»", lad_restore)

lines.append("RESULT " + ("OK" if ok else "FAIL"))
with open(report, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print("\n".join(lines))
sys.exit(0 if ok else 1)
