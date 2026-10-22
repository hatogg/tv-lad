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
    from tvlad.app import main
    return main(argv)


if __name__ == "__main__":
    sys.exit(entry())
