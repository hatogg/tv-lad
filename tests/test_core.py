"""Перевірки без телевізора: розбір відповідей Android, захист системних програм, дії з журналом."""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from tvlad import catalog  # noqa: E402
from tvlad.actions import Actions, History  # noqa: E402
from tvlad.adb import AdbError, friendly  # noqa: E402
from tvlad.fake import FakeAdb  # noqa: E402
from tvlad.tv import TV, parse_activities, parse_df, parse_getprop, parse_meminfo, parse_packages  # noqa: E402


class Parse(unittest.TestCase):
    def test_packages(self):
        out = "package:com.a.b\npackage:/system/app/X/X.apk=com.x.y\npackage:android\nјunk\npackage:bad name\n"
        self.assertEqual(parse_packages(out), ["com.a.b", "com.x.y", "android"])

    def test_getprop(self):
        p = parse_getprop("[ro.product.model]: [43U750NB]\n[ro.build.version.sdk]: [30]\n[empty]: []\n")
        self.assertEqual(p["ro.product.model"], "43U750NB")
        self.assertEqual(p["empty"], "")

    def test_df(self):
        out = "Filesystem     1K-blocks    Used Available Use% Mounted on\n/dev/block/dm-5  10485760 6973760   3512000  67% /data\n"
        self.assertEqual(parse_df(out), (10485760, 3512000))
        self.assertEqual(parse_df("garbage"), (0, 0))

    def test_meminfo(self):
        self.assertEqual(parse_meminfo("MemTotal: 1931264 kB\nMemFree: 1 kB\nMemAvailable:  688128 kB\n"), (1931264, 688128))

    def test_activities(self):
        out = ("priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n"
               "com.google.android.tvlauncher/.MainActivity\n"
               "priority=-1000\ncom.android.tv.settings/.FallbackHome\n")
        self.assertEqual(parse_activities(out), ["com.google.android.tvlauncher", "com.android.tv.settings"])

    def test_activities_long_form(self):
        out = ("priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n"
               "  ActivityInfo:\n    name=com.google.android.leanbacklauncher.MainActivity\n"
               "    packageName=com.google.android.leanbacklauncher\n    enabled=true\n")
        self.assertEqual(parse_activities(out), ["com.google.android.leanbacklauncher"])

    def test_friendly(self):
        self.assertIn("відхилив", friendly("failed to connect to 192.168.1.2:5555: Connection refused"))
        self.assertIn("не відповідає", friendly("cannot connect to 10.0.0.9:5555: Connection timed out"))


class Catalog(unittest.TestCase):
    def test_protected(self):
        for p in ("android", "com.android.systemui", "com.android.tv.settings", "com.google.android.gms",
                  "com.mediatek.wwtv.tvcenter", "com.android.providers.tv", "com.google.android.tvlauncher",
                  "com.google.android.leanback.ime", "com.kivi.ota.update", "com.vendor.hdmi.cec", "com.android.vending"):
            self.assertTrue(catalog.protected(p), p)
            self.assertIsNone(catalog.classify(p, True), p)
        for p in ("com.google.android.play.games", "com.netflix.ninja", "com.kivi.mediacenter", "com.google.android.tvrecommendations"):
            self.assertFalse(catalog.protected(p), p)

    def test_groups(self):
        self.assertEqual(catalog.classify("com.google.android.play.games", True).group, catalog.GROUP_SAFE)
        self.assertTrue(catalog.classify("com.google.android.play.games", True).preselect)
        self.assertEqual(catalog.classify("com.netflix.ninja", True).group, catalog.GROUP_APPS)
        self.assertEqual(catalog.classify("com.netflix.ninja", False).group, catalog.GROUP_USER)
        self.assertEqual(catalog.classify("com.kivi.mediacenter", True).group, catalog.GROUP_SYSTEM)
        self.assertEqual(catalog.classify("ua.sweet.tv", False).group, catalog.GROUP_USER)
        self.assertEqual(catalog.classify("com.google.android.apps.mediashell", True).group, catalog.GROUP_CAREFUL)

    def test_pretty(self):
        self.assertEqual(catalog.pretty("com.netflix.ninja"), "Netflix")
        self.assertEqual(catalog.pretty("com.kivi.mediacenter"), "Kivi Mediacenter")


class FakeTv(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.adb = FakeAdb()
        self.tv = TV(self.adb)
        self.act = Actions(self.tv, History("KV-test", self.dir))

    def test_info(self):
        i = self.tv.info()
        self.assertEqual(i.title, "KIVI 43U750NB")
        self.assertEqual(i.sdk, 30)
        self.assertEqual(i.storage_free_kb, 3512000)

    def test_apps_and_disable_enable(self):
        apps = {a.pkg: a for a in self.tv.apps()}
        self.assertTrue(apps["com.google.android.play.games"].system)
        self.assertFalse(apps["ua.sweet.tv"].system)
        ok, bad = self.act.disable(["com.google.android.play.games", "com.android.systemui"])
        self.assertEqual(ok, ["com.google.android.play.games"])
        self.assertEqual(bad, ["com.android.systemui"])  # захищене не вимикаємо навіть якщо попросили
        self.assertFalse({a.pkg: a for a in self.tv.apps()}["com.google.android.play.games"].enabled)
        res = self.act.restore_all()
        self.assertEqual(res["apps"], ["com.google.android.play.games"])
        self.assertTrue(self.tv.enabled("com.google.android.play.games"))
        self.assertEqual(History("KV-test", self.dir).disabled, [])

    def test_animations_and_restore(self):
        self.act.set_animations(0.5)
        self.assertEqual(self.adb.settings["window_animation_scale"], "0.5")
        self.act.set_animations(0)
        self.assertEqual(self.adb.settings["animator_duration_scale"], "0")
        self.act.restore_animations()
        self.assertEqual(self.adb.settings["window_animation_scale"], "1.0")
        self.assertEqual(self.adb.settings["animator_duration_scale"], "null")

    def test_boost(self):
        r = self.act.boost()
        self.assertEqual(r["freed_kb"], 412_000)

    def test_home(self):
        self.assertEqual(self.tv.current_home(), "com.google.android.tvlauncher")
        self.assertEqual(self.tv.home_candidates(), ["com.google.android.tvlauncher", "com.spocky.projengmenu"])
        self.act.set_home(catalog.PROJECTIVY)
        self.assertEqual(self.tv.current_home(), catalog.PROJECTIVY)
        self.assertEqual(History("KV-test", self.dir).launchers, ["com.google.android.tvlauncher"])
        self.act.restore_home()
        self.assertEqual(self.tv.current_home(), "com.google.android.tvlauncher")

    def test_home_not_installed(self):
        tv = TV(FakeAdb(with_projectivy=False))
        act = Actions(tv, History("KV-2", self.dir))
        with self.assertRaises(AdbError):
            act.set_home(catalog.PROJECTIVY)
        self.assertEqual(tv.current_home(), "com.google.android.tvlauncher")

    def test_install_lad(self):
        apk = os.path.join(self.dir, "lad-home.apk")
        open(apk, "wb").write(b"PK")
        self.act.install_lad(apk)
        self.assertEqual(self.tv.current_home(), catalog.LAD)
        self.act.restore_home()
        self.assertEqual(self.tv.current_home(), "com.google.android.tvlauncher")
        with self.assertRaises(AdbError):
            self.act.install_lad(os.path.join(self.dir, "missing.apk"))

    def test_bad_package_name(self):
        with self.assertRaises(AdbError):
            self.tv.disable("com.x; reboot")


if __name__ == "__main__":
    unittest.main()
