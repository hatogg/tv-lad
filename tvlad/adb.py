"""
Зв'язок з телевізором через ADB по Wi‑Fi.

Використовується офіційний adb від Google (platform-tools): при першому запуску програма сама
завантажує його з dl.google.com у свою теку. Ключ підключення (adbkey) — той самий, що й у adb,
тож телевізор питає дозволу лише один раз («Завжди дозволяти з цього комп'ютера»).
"""
from __future__ import annotations

import concurrent.futures
import ipaddress
import os
import re
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
import zipfile

PT_URLS = {
    "win32": "https://dl.google.com/android/repository/platform-tools-latest-windows.zip",
    "darwin": "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip",
    "linux": "https://dl.google.com/android/repository/platform-tools-latest-linux.zip",
}
WIN = sys.platform == "win32"
NO_WINDOW = 0x08000000 if WIN else 0  # без миготіння чорних вікон консолі
PKG_RE = re.compile(r"^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$")


class AdbError(Exception):
    """Помилка, зрозуміла людині (текст можна показувати як є)."""


def data_dir() -> str:
    if WIN:
        base = os.environ.get("LOCALAPPDATA") or os.path.expanduser("~")
    else:
        base = os.environ.get("XDG_DATA_HOME") or os.path.expanduser("~/.local/share")
    d = os.path.join(base, "TVLad")
    os.makedirs(d, exist_ok=True)
    return d


def find_adb() -> str | None:
    env = os.environ.get("TVLAD_ADB")
    if env and os.path.isfile(env):
        return env
    own = os.path.join(data_dir(), "platform-tools", "adb.exe" if WIN else "adb")
    if os.path.isfile(own):
        return own
    return shutil.which("adb")


def download_adb(progress=None) -> str:
    """Завантажити platform-tools від Google (≈ 7 МБ). progress(done_bytes, total_bytes)."""
    url = PT_URLS.get(sys.platform, PT_URLS["linux"])
    part = os.path.join(data_dir(), "platform-tools.zip.part")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "TVLad"})
        with urllib.request.urlopen(req, timeout=60) as r, open(part, "wb") as f:
            total = int(r.headers.get("Content-Length") or 0)
            done = 0
            while True:
                chunk = r.read(1 << 16)
                if not chunk:
                    break
                f.write(chunk)
                done += len(chunk)
                if progress:
                    progress(done, total)
        with zipfile.ZipFile(part) as z:
            z.extractall(data_dir())
    except (OSError, zipfile.BadZipFile) as e:
        raise AdbError(f"Не вдалося завантажити adb від Google: {e}") from e
    finally:
        try:
            os.remove(part)
        except OSError:
            pass
    path = os.path.join(data_dir(), "platform-tools", "adb.exe" if WIN else "adb")
    if not os.path.isfile(path):
        raise AdbError("Архів adb завантажився, але adb у ньому не знайдено")
    if not WIN:
        os.chmod(path, 0o755)
    return path


def friendly(out: str) -> str:
    low = out.lower()
    if "refused" in low:
        return ("Телевізор відхилив з'єднання. Перевірте, що в «Для розробників» увімкнено «Налагодження USB» "
                "(і «Налагодження через мережу», якщо такий пункт є).")
    if "timed out" in low or "no route" in low or "unreachable" in low or "cannot connect" in low:
        return "Телевізор не відповідає за цією адресою. Перевірте IP і що телевізор у тій самій Wi‑Fi мережі."
    if "unknown host" in low or "no such host" in low or "cannot resolve" in low:
        return "Неправильна адреса. Впишіть IP телевізора, напр. 192.168.1.45."
    if "unauthorized" in low or "authenticate" in low:
        return "Телевізор ще не дозволив підключення — підтвердьте запит на екрані телевізора."
    if "offline" in low:
        return "Телевізор зараз недоступний (offline). Спробуйте ще раз або перезапустіть налагодження на телевізорі."
    if "not found" in low or "no devices" in low:
        return "Телевізор не підключено."
    return out.strip() or "Невідома помилка adb"


class Adb:
    def __init__(self, exe: str):
        self.exe = exe
        self.serial: str | None = None

    def run(self, args: list[str], timeout: float = 30) -> tuple[int, str, str]:
        try:
            p = subprocess.run([self.exe, *args], capture_output=True, text=True, encoding="utf-8",
                               errors="replace", timeout=timeout, creationflags=NO_WINDOW)
        except subprocess.TimeoutExpired as e:
            raise AdbError("Телевізор не відповідає (час очікування вийшов).") from e
        except OSError as e:
            raise AdbError(f"Не вдалося запустити adb: {e}") from e
        return p.returncode, p.stdout or "", p.stderr or ""

    def start_server(self) -> None:
        self.run(["start-server"], timeout=40)

    def version(self) -> str:
        return self.run(["version"], timeout=20)[1].splitlines()[0] if self.exe else ""

    def use(self, serial: str) -> None:
        self.serial = serial

    def connect(self, host: str, port: int = 5555) -> str:
        """Підключитися по мережі. Повертає стан: device (готово) або unauthorized (чекаємо дозволу на TV)."""
        host = host.strip()
        if not host:
            raise AdbError("Впишіть IP-адресу телевізора.")
        self.start_server()
        target = f"{host}:{port}"
        _, out, err = self.run(["connect", target], timeout=25)
        text = (out + "\n" + err).strip()
        low = text.lower()
        if "failed to authenticate" in low:
            self.serial = target
            return "unauthorized"
        if "connected to" in low and "failed" not in low and "cannot" not in low and "unable" not in low:
            self.serial = target
            return self.state()
        raise AdbError(friendly(text))

    def pair(self, host: str, port: int, code: str) -> None:
        """Android 11+: «Бездротове налагодження» → «Створити пару за кодом»."""
        self.start_server()
        _, out, err = self.run(["pair", f"{host.strip()}:{port}", code.strip()], timeout=30)
        text = (out + "\n" + err).strip()
        if "successfully paired" not in text.lower():
            raise AdbError("Не вдалося створити пару: " + friendly(text))

    def state(self) -> str:
        if not self.serial:
            return "none"
        _, out, err = self.run(["-s", self.serial, "get-state"], timeout=15)
        text = (out + err).strip().lower()
        if "unauthorized" in text:
            return "unauthorized"
        if out.strip() == "device":
            return "device"
        if "offline" in text:
            return "offline"
        return "none"

    def wait_authorized(self, timeout: float = 90, tick=None) -> bool:
        end = time.time() + timeout
        while time.time() < end:
            st = self.state()
            if st == "device":
                return True
            if st == "none" and self.serial and ":" in self.serial:
                # після відмови/таймауту на телевізорі adb розриває з'єднання — пробуємо ще раз
                host, port = self.serial.rsplit(":", 1)
                self.run(["connect", f"{host}:{port}"], timeout=20)
            if tick:
                tick(int(end - time.time()))
            time.sleep(1.5)
        return False

    def shell(self, cmd: str, timeout: float = 60) -> str:
        if not self.serial:
            raise AdbError("Телевізор не підключено.")
        rc, out, err = self.run(["-s", self.serial, "shell", cmd], timeout=timeout)
        low = err.lower()
        if rc != 0 and any(w in low for w in ("device offline", "not found", "unauthorized", "no devices", "closed")):
            raise AdbError(friendly(err))
        return out if out.strip() else err

    def disconnect(self) -> None:
        if self.serial and ":" in self.serial:
            try:
                self.run(["disconnect", self.serial], timeout=10)
            except AdbError:
                pass
        self.serial = None


# ───────── пошук телевізора в мережі ─────────

def local_ipv4() -> str | None:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("10.255.255.255", 1))  # пакети не надсилаються — лише визначаємо свою адресу
        ip = s.getsockname()[0]
        return None if ip.startswith("127.") else ip
    except OSError:
        return None
    finally:
        s.close()


def port_open(host: str, port: int, timeout: float = 0.5) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def scan(port: int = 5555, progress=None) -> list[str]:
    """Пристрої у своїй /24-мережі з відкритим портом ADB."""
    me = local_ipv4()
    if not me:
        return []
    net = ipaddress.ip_network(me + "/24", strict=False)
    hosts = [str(h) for h in net.hosts() if str(h) != me]
    found: list[str] = []
    done = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=96) as ex:
        futs = {ex.submit(port_open, h, port): h for h in hosts}
        for f in concurrent.futures.as_completed(futs):
            done += 1
            if f.result():
                found.append(futs[f])
            if progress:
                progress(done, len(hosts))
    return sorted(found, key=lambda h: tuple(int(x) for x in h.split(".")))
