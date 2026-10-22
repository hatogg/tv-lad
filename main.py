"""ТВ Лад — порядок у телевізорі KIVI (і будь-якому Android TV) з комп'ютера."""
import sys


def entry() -> int:
    argv = sys.argv
    if "--selftest" in argv:
        from tvlad.selftest import run
        i = argv.index("--selftest")
        out = argv[i + 1] if len(argv) > i + 1 else "selftest.txt"
        shots = argv[argv.index("--shots") + 1] if "--shots" in argv else None
        return run(out, with_adb="--adb" in argv, shots=shots)
    if "--report" in argv:
        # без вікна: TVLad.exe --report 192.168.1.45[:5555] звіт.txt
        from tvlad.adb import Adb, AdbError, download_adb, find_adb
        from tvlad.tv import TV
        i = argv.index("--report")
        target = argv[i + 1]
        out = argv[i + 2] if len(argv) > i + 2 else "tv-report.txt"
        host, _, port = target.partition(":")
        try:
            adb = Adb(find_adb() or download_adb())
            if adb.connect(host, int(port or 5555)) != "device" and not adb.wait_authorized(90):
                raise AdbError("телевізор не дозволив підключення")
            text = TV(adb).report()
        except AdbError as e:
            text = f"ПОМИЛКА: {e}"
        with open(out, "w", encoding="utf-8") as f:
            f.write(text)
        return 0 if not text.startswith("ПОМИЛКА") else 1
    from tvlad.app import main
    return main(argv)


if __name__ == "__main__":
    sys.exit(entry())
