"""ТВ Лад — вікно програми (PyQt6)."""
from __future__ import annotations

import html
import os
import sys

from PyQt6.QtCore import QSettings, QStandardPaths, Qt, QThread, QTimer, QUrl, pyqtSignal
from PyQt6.QtGui import QColor, QDesktopServices, QFont, QIcon
from PyQt6.QtWidgets import (QApplication, QButtonGroup, QComboBox, QFrame, QGridLayout, QHBoxLayout, QHeaderView,
                             QFileDialog, QLabel, QLineEdit, QListWidget, QMainWindow, QMessageBox, QPlainTextEdit, QProgressBar, QPushButton,
                             QScrollArea, QSizePolicy, QSpinBox, QStackedWidget, QTreeWidget, QTreeWidgetItem,
                             QVBoxLayout, QWidget)

from . import catalog
from .actions import Actions, History
from .adb import Adb, AdbError, download_adb, find_adb, scan
from .tv import TV, Info, parse_scale

APP_NAME = "ТВ Лад"
VERSION = "1.0"

ACCENT = "#2EC4A6"
QSS = f"""
* {{ font-family: "Segoe UI", "Noto Sans", "DejaVu Sans", sans-serif; font-size: 10pt; color: #E8ECF1; }}
QMainWindow, #content {{ background: #12151A; }}
#sidebar {{ background: #0C0E12; border-right: 1px solid #1E232B; }}
#brand {{ font-size: 19pt; font-weight: 700; color: #FFFFFF; }}
#brandSub {{ color: #7D8796; font-size: 9pt; }}
#nav {{ text-align: left; padding: 11px 16px; border: none; border-radius: 10px; background: transparent; color: #AEB7C4; font-size: 10.5pt; }}
#nav:hover {{ background: #171B22; color: #FFFFFF; }}
#nav:checked {{ background: #1B2A2A; color: {ACCENT}; font-weight: 600; }}
#nav:disabled {{ color: #4A525E; }}
#pageTitle {{ font-size: 18pt; font-weight: 700; color: #FFFFFF; }}
#pageSub {{ color: #8C96A4; }}
#card {{ background: #1A1E25; border: 1px solid #232934; border-radius: 14px; }}
#cardTitle {{ font-size: 11.5pt; font-weight: 600; color: #FFFFFF; }}
#muted {{ color: #8C96A4; }}
#ok {{ color: {ACCENT}; font-weight: 600; }}
#warn {{ color: #FFB454; }}
#err {{ color: #FF6B6B; }}
#big {{ font-size: 15pt; font-weight: 700; color: #FFFFFF; }}
#chip {{ background: #171B22; border-radius: 10px; padding: 8px 10px; color: #AEB7C4; }}
QPushButton {{ background: #232934; border: 1px solid #2C3440; border-radius: 9px; padding: 8px 16px; }}
QPushButton:hover {{ background: #2B323E; }}
QPushButton:disabled {{ color: #59616D; background: #1B1F26; border-color: #222831; }}
QPushButton#primary {{ background: {ACCENT}; border: none; color: #062B24; font-weight: 700; }}
QPushButton#primary:hover {{ background: #45D6B9; }}
QPushButton#primary:disabled {{ background: #1F4740; color: #6E8F88; }}
QPushButton#danger {{ background: #3A1F24; border: 1px solid #5A2A31; color: #FFB3B3; }}
QLineEdit, QSpinBox, QComboBox {{ background: #12151A; border: 1px solid #2C3440; border-radius: 8px; padding: 7px 10px; selection-background-color: {ACCENT}; }}
QLineEdit:focus, QSpinBox:focus, QComboBox:focus {{ border-color: {ACCENT}; }}
QComboBox QAbstractItemView {{ background: #1A1E25; border: 1px solid #2C3440; selection-background-color: #24403B; }}
QTreeWidget, QListWidget {{ background: #151920; border: 1px solid #232934; border-radius: 12px; padding: 4px; alternate-background-color: #171B22; }}
QTreeWidget::item, QListWidget::item {{ padding: 5px 2px; }}
QTreeWidget::item:selected {{ background: #1F3532; }}
QTreeView::indicator {{ width: 16px; height: 16px; border-radius: 4px; border: 2px solid #5A6472; background: #12151A; }}
QTreeView::indicator:checked {{ background: {ACCENT}; border-color: {ACCENT}; }}
QTreeView::indicator:hover {{ border-color: {ACCENT}; }}
QScrollBar:horizontal {{ background: transparent; height: 10px; }}
QScrollBar::handle:horizontal {{ background: #2C3440; border-radius: 5px; min-width: 30px; }}
QHeaderView::section {{ background: #151920; color: #7D8796; border: none; border-bottom: 1px solid #232934; padding: 6px; }}
QProgressBar {{ background: #1A1E25; border: none; border-radius: 4px; height: 6px; }}
QProgressBar::chunk {{ background: {ACCENT}; border-radius: 4px; }}
QScrollArea {{ border: none; background: transparent; }}
QScrollBar:vertical {{ background: transparent; width: 10px; }}
QScrollBar::handle:vertical {{ background: #2C3440; border-radius: 5px; min-height: 30px; }}
QScrollBar::add-line, QScrollBar::sub-line {{ height: 0; }}
#status {{ background: #0C0E12; border-top: 1px solid #1E232B; color: #8C96A4; padding: 6px 14px; }}
"""


def resource(name: str) -> str:
    base = getattr(sys, "_MEIPASS", os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    return os.path.join(base, "assets", name)


def mb(kb: int) -> str:
    if kb >= 1024 * 1024:
        return f"{kb / 1024 / 1024:.1f} ГБ".replace(".", ",")
    return f"{kb // 1024} МБ"


# ───────── фонові завдання ─────────

class Task(QThread):
    done = pyqtSignal(object)
    failed = pyqtSignal(str)

    def __init__(self, fn, *args):
        super().__init__()
        self.fn = fn
        self.args = args

    def run(self):
        try:
            self.done.emit(self.fn(*self.args))
        except AdbError as e:
            self.failed.emit(str(e))
        except Exception as e:  # noqa: BLE001 — показуємо людині, а не падаємо
            self.failed.emit(f"Несподівана помилка: {e}")


# ───────── дрібні елементи ─────────

def label(text: str = "", name: str | None = None, wrap: bool = True) -> QLabel:
    lb = QLabel(text)
    if name:
        lb.setObjectName(name)
    lb.setWordWrap(wrap)
    lb.setTextFormat(Qt.TextFormat.RichText if "<" in text else Qt.TextFormat.PlainText)
    return lb


def button(text: str, kind: str | None = None, slot=None) -> QPushButton:
    b = QPushButton(text)
    if kind:
        b.setObjectName(kind)
    b.setCursor(Qt.CursorShape.PointingHandCursor)
    if slot:
        b.clicked.connect(slot)
    return b


class Card(QFrame):
    def __init__(self, title: str = "", text: str = ""):
        super().__init__()
        self.setObjectName("card")
        self.lay = QVBoxLayout(self)
        self.lay.setContentsMargins(20, 16, 20, 18)
        self.lay.setSpacing(8)
        if title:
            self.lay.addWidget(label(title, "cardTitle"))
        if text:
            self.lay.addWidget(label(text, "muted"))

    def row(self, *widgets, stretch: bool = True) -> QHBoxLayout:
        h = QHBoxLayout()
        h.setSpacing(10)
        for w in widgets:
            h.addWidget(w)
        if stretch:
            h.addStretch(1)
        self.lay.addLayout(h)
        return h


class Page(QWidget):
    def __init__(self, win: "Main", title: str, sub: str):
        super().__init__()
        self.win = win
        outer = QVBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        body = QWidget()
        body.setObjectName("content")
        self.lay = QVBoxLayout(body)
        self.lay.setContentsMargins(32, 26, 32, 26)
        self.lay.setSpacing(14)
        self.lay.addWidget(label(title, "pageTitle"))
        if sub:
            self.lay.addWidget(label(sub, "pageSub"))
        scroll.setWidget(body)
        outer.addWidget(scroll)

    def finish(self):
        self.lay.addStretch(1)

    def entered(self):
        """Сторінку відкрили — оновити дані."""


# ───────── сторінки ─────────

class ConnectPage(Page):
    def __init__(self, win):
        super().__init__(win, "Підключення", "Телевізор і комп'ютер мають бути в одній Wi‑Fi мережі. Кабель не потрібен.")
        prep = Card("1. Підготуйте телевізор (один раз)")
        prep.lay.addWidget(label(
            "<ol style='margin-left:-20px'>"
            "<li><b>Налаштування → Налаштування пристрою → Про пристрій</b> — 7 разів натисніть «Збірка», "
            "доки не з'явиться «Ви стали розробником».</li>"
            "<li><b>Налаштування пристрою → Для розробників</b> — увімкніть <b>«Налагодження USB»</b> "
            "(і <b>«Налагодження через мережу»</b> або «ADB по мережі», якщо такий пункт є).</li>"
            "<li>IP-адресу телевізора видно в <b>Налаштування → Мережа та Інтернет →</b> ваша Wi‑Fi.</li>"
            "</ol>", "muted"))
        self.lay.addWidget(prep)

        conn = Card("2. Підключіться")
        self.ip = QLineEdit()
        self.ip.setPlaceholderText("IP телевізора, напр. 192.168.1.45")
        self.ip.setMinimumWidth(260)
        self.ip.returnPressed.connect(self.connect_tv)
        self.port = QSpinBox()
        self.port.setRange(1, 65535)
        self.port.setValue(5555)
        self.port.setFixedWidth(90)
        self.btn_scan = button("Знайти в мережі", slot=self.find)
        self.btn_connect = button("Підключити", "primary", self.connect_tv)
        conn.row(self.ip, label("порт", "muted", False), self.port, self.btn_connect, self.btn_scan)
        self.found = QWidget()
        self.found_lay = QHBoxLayout(self.found)
        self.found_lay.setContentsMargins(0, 0, 0, 0)
        conn.lay.addWidget(self.found)
        self.status = label("", "muted")
        conn.lay.addWidget(self.status)
        self.lay.addWidget(conn)

        pair = Card("Android 11 і новіші: підключення за кодом",
                    "Якщо в «Для розробників» є «Бездротове налагодження»: відкрийте його → «Створити пару за кодом». "
                    "Впишіть порт і код з екрана телевізора, потім підключіться з портом, указаним у «Бездротовому налагодженні».")
        self.pair_port = QSpinBox()
        self.pair_port.setRange(1, 65535)
        self.pair_port.setValue(37000)
        self.pair_port.setFixedWidth(90)
        self.pair_code = QLineEdit()
        self.pair_code.setPlaceholderText("код (6 цифр)")
        self.pair_code.setFixedWidth(130)
        pair.row(label("порт пари", "muted", False), self.pair_port, self.pair_code, button("Створити пару", slot=self.do_pair))
        self.lay.addWidget(pair)

        self.tv_card = Card("Телевізор")
        self.tv_name = label("", "big")
        self.tv_details = label("", "muted")
        self.tv_card.lay.addWidget(self.tv_name)
        self.tv_card.lay.addWidget(self.tv_details)
        self.tv_card.row(button("Відключитися", slot=self.win.disconnect))
        self.tv_card.hide()
        self.lay.addWidget(self.tv_card)
        self.finish()
        self.ip.setText(self.win.settings.value("ip", "") or "")

    def set_status(self, text: str, kind: str = "muted"):
        self.status.setObjectName(kind)
        self.status.setStyleSheet("")
        self.status.setText(text)
        self.status.style().unpolish(self.status)
        self.status.style().polish(self.status)

    def find(self):
        self.btn_scan.setEnabled(False)
        self.set_status("Шукаю телевізори з увімкненим налагодженням у вашій мережі…")

        def ok(hosts):
            self.btn_scan.setEnabled(True)
            while self.found_lay.count():
                w = self.found_lay.takeAt(0).widget()
                if w:
                    w.deleteLater()
            if not hosts:
                self.set_status("Нічого не знайдено. Перевірте, що на телевізорі увімкнено «Налагодження USB» / «через мережу», "
                                "або впишіть IP вручну.", "warn")
                return
            self.set_status("Знайдено — натисніть адресу:", "ok")
            for h in hosts[:8]:
                b = button(h, slot=lambda _=False, h=h: (self.ip.setText(h), self.connect_tv()))
                self.found_lay.addWidget(b)
            self.found_lay.addStretch(1)

        self.win.run(lambda: scan(self.port.value()), ok, lambda e: (self.btn_scan.setEnabled(True), self.set_status(e, "err")))

    def do_pair(self):
        ip = self.ip.text().strip()
        if not ip or not self.pair_code.text().strip():
            self.set_status("Впишіть IP телевізора вгорі та код з екрана телевізора.", "warn")
            return
        self.set_status("Створюю пару…")
        self.win.run(lambda: self.win.ensure_adb().pair(ip, self.pair_port.value(), self.pair_code.text()),
                     lambda _: self.set_status("Пару створено. Тепер впишіть порт з «Бездротового налагодження» і натисніть «Підключити».", "ok"),
                     lambda e: self.set_status(e, "err"))

    def connect_tv(self):
        ip = self.ip.text().strip()
        if not ip:
            self.set_status("Впишіть IP-адресу телевізора або натисніть «Знайти в мережі».", "warn")
            return
        self.win.settings.setValue("ip", ip)
        self.btn_connect.setEnabled(False)
        self.set_status("Підключаюсь…")
        port = self.port.value()

        def work():
            adb = self.win.ensure_adb(lambda d, t: None)
            st = adb.connect(ip, port)
            if st == "unauthorized":
                self.win.sig_status.emit("На телевізорі з'явилось вікно «Дозволити налагодження?» — позначте "
                                         "«Завжди дозволяти з цього комп'ютера» і натисніть OK (пультом).")
                if not adb.wait_authorized(90):
                    raise AdbError("Телевізор не дозволив підключення. Підтвердьте запит на екрані телевізора й спробуйте ще раз.")
            elif st != "device":
                raise AdbError("Телевізор поки недоступний. Спробуйте ще раз за кілька секунд.")
            tv = TV(adb)
            return tv, tv.info()

        def ok(res):
            self.btn_connect.setEnabled(True)
            tv, info = res
            self.win.on_connected(tv, info)

        def fail(e):
            self.btn_connect.setEnabled(True)
            self.set_status(e, "err")

        self.win.run(work, ok, fail)

    def show_tv(self, info: Info | None):
        if not info:
            self.tv_card.hide()
            self.set_status("")
            return
        self.tv_name.setText(info.title)
        self.tv_details.setText(
            f"Android {info.android} · пам'ять: вільно {mb(info.storage_free_kb)} з {mb(info.storage_total_kb)} · "
            f"оперативна: {mb(info.ram_total_kb)}, вільно ≈ {mb(info.ram_free_kb)}")
        self.tv_card.show()
        self.set_status("Підключено ✓ Обирайте, що зробити, у меню ліворуч.", "ok")


class SpeedPage(Page):
    def __init__(self, win):
        super().__init__(win, "Прискорення", "Щоб меню й програми відкривались швидше. Усе можна повернути у «Відновленні».")
        top = Card("Прискорити все одразу", "Швидкі анімації (0,5×), очищення кешу програм і вивантаження фонових програм. "
                                             "Безпечно — нічого не видаляється.")
        self.btn_all = button("Прискорити", "primary", self.boost)
        self.res_all = label("", "ok")
        top.row(self.btn_all, self.res_all)
        self.lay.addWidget(top)

        grid = QGridLayout()
        grid.setSpacing(14)
        anim = Card("Анімації", "Переходи між екранами. «Швидкі» — найкращий баланс: усе виглядає плавно, але відкривається вдвічі швидше.")
        self.anim = QComboBox()
        self.anim.addItems(["Звичайні (1×)", "Швидкі (0,5×) — радимо", "Вимкнені"])
        self.anim.setCurrentIndex(1)
        self.anim_now = label("", "muted")
        anim.row(self.anim, button("Застосувати", slot=self.apply_anim))
        anim.lay.addWidget(self.anim_now)
        grid.addWidget(anim, 0, 0)

        cache = Card("Кеш програм", "Тимчасові файли YouTube, браузера тощо. Звільняє місце; програми самі створять потрібне знову.")
        self.res_cache = label("", "ok")
        cache.row(button("Очистити кеш", slot=self.cache), self.res_cache)
        grid.addWidget(cache, 0, 1)

        bg = Card("Фонові програми", "Закриває програми, що висять у пам'яті. Корисно, якщо телевізор почав гальмувати.")
        self.res_bg = label("", "ok")
        bg.row(button("Вивантажити", slot=self.kill), self.res_bg)
        grid.addWidget(bg, 1, 0)

        deep = Card("Глибока оптимізація", "Перекомпілює програми під процесор телевізора. Триває 5–20 хвилин; "
                                           "у цей час телевізор може пригальмовувати. Достатньо раз на кілька місяців.")
        self.res_deep = label("", "ok")
        self.btn_deep = button("Запустити", slot=self.deep)
        deep.row(self.btn_deep, self.res_deep)
        grid.addWidget(deep, 1, 1)
        rb = Card("Перезапуск", "Телевізор у режимі очікування не вимикається насправді — за тижні пам'ять засмічується. "
                                "Повний перезапуск (≈ 1–2 хв) повертає швидкість. Варто робити раз на тиждень.")
        self.res_rb = label("", "ok")
        rb.row(button("Перезапустити телевізор", slot=self.reboot), self.res_rb)
        grid.addWidget(rb, 2, 0, 1, 2)
        grid.setColumnStretch(0, 1)
        grid.setColumnStretch(1, 1)
        self.lay.addLayout(grid)
        self.finish()

    def entered(self):
        if not self.win.tv:
            return
        self.win.run(self.win.tv.animations, self.show_anim, lambda e: None)

    def show_anim(self, vals: dict):
        s = parse_scale(vals.get("window_animation_scale", "1"))
        now = "вимкнені" if s == 0 else ("швидкі" if s < 1 else "звичайні")
        self.anim_now.setText(f"Зараз: {now} ({str(s).replace('.', ',')}×)")

    def boost(self):
        self.btn_all.setEnabled(False)
        self.res_all.setText("Працюю…")

        def ok(r):
            self.btn_all.setEnabled(True)
            self.res_all.setText(f"Готово ✓ звільнено {mb(r['freed_kb'])}, анімації 0,5×")
            self.entered()

        self.win.run(self.win.actions.boost, ok, lambda e: (self.btn_all.setEnabled(True), self.res_all.setText(""), self.win.error(e)))

    def apply_anim(self):
        v = [1.0, 0.5, 0.0][self.anim.currentIndex()]
        self.win.run(lambda: self.win.actions.set_animations(v), lambda _: (self.entered(), self.win.toast("Анімації змінено ✓")), self.win.error)

    def cache(self):
        self.res_cache.setText("Очищаю…")
        self.win.run(self.win.tv.trim_caches, lambda kb: self.res_cache.setText(f"Звільнено {mb(kb)} ✓"),
                     lambda e: (self.res_cache.setText(""), self.win.error(e)))

    def kill(self):
        self.win.run(self.win.tv.kill_background, lambda _: self.res_bg.setText("Готово ✓"), self.win.error)

    def reboot(self):
        if not self.win.confirm("Перезапустити телевізор?", "Телевізор вимкнеться й увімкнеться знову (1–2 хвилини). "
                                "Після цього підключіться знову на сторінці «Підключення»."):
            return
        self.win.run(self.win.tv.reboot, lambda _: (self.res_rb.setText("Перезапускається… підключіться за 2 хвилини"),
                                                     self.win.disconnect()), self.win.error)

    def deep(self):
        self.btn_deep.setEnabled(False)
        self.res_deep.setText("Триває… не вимикайте телевізор")
        self.win.busy(True)

        def end(text, kind="ok"):
            self.btn_deep.setEnabled(True)
            self.res_deep.setObjectName(kind)
            self.res_deep.setText(text)
            self.win.busy(False)

        self.win.run(self.win.tv.deep_optimize, lambda _: end("Готово ✓"), lambda e: end(e, "err"))


class AppsPage(Page):
    def __init__(self, win):
        super().__init__(win, "Програми", "Вимкнена програма зникає з телевізора й не працює у фоні, але її завжди можна ввімкнути назад. "
                                         "Системне, без чого телевізор не працює, тут не показується.")
        bar = QHBoxLayout()
        self.search = QLineEdit()
        self.search.setPlaceholderText("Пошук програми…")
        self.search.textChanged.connect(self.filter)
        bar.addWidget(self.search, 1)
        bar.addWidget(button("Оновити список", slot=self.entered))
        self.lay.addLayout(bar)

        self.tree = QTreeWidget()
        self.tree.setColumnCount(4)
        self.tree.setHeaderLabels(["Програма", "Стан", "Порада", "Пакет"])
        self.tree.setAlternatingRowColors(True)
        self.tree.setMinimumHeight(380)
        self.tree.setRootIsDecorated(True)
        hdr = self.tree.header()
        hdr.setSectionResizeMode(0, QHeaderView.ResizeMode.Interactive)
        hdr.setSectionResizeMode(1, QHeaderView.ResizeMode.ResizeToContents)
        hdr.setSectionResizeMode(2, QHeaderView.ResizeMode.Stretch)
        hdr.setSectionResizeMode(3, QHeaderView.ResizeMode.Interactive)
        self.tree.setColumnWidth(0, 290)
        self.tree.setColumnWidth(3, 230)
        self.tree.itemChanged.connect(lambda *_: self.count())
        self.lay.addWidget(self.tree, 1)

        row = QHBoxLayout()
        self.btn_off = button("Вимкнути вибрані", "primary", self.disable)
        self.btn_on = button("Увімкнути вибрані", slot=self.enable)
        self.btn_del = button("Видалити вибрані", "danger", self.uninstall)
        self.sel = label("", "muted", False)
        for w in (self.btn_off, self.btn_on, self.btn_del):
            row.addWidget(w)
        row.addSpacing(10)
        row.addWidget(self.sel)
        row.addStretch(1)
        self.lay.addLayout(row)
        self.apps = []

    def entered(self):
        if not self.win.tv:
            return
        self.sel.setText("Завантажую список…")
        self.win.run(self.win.tv.apps, self.fill, self.win.error)

    def fill(self, apps):
        self.apps = apps
        self.tree.blockSignals(True)
        self.tree.clear()
        homes = set(self.win.homes or [])
        groups = {}
        for g in catalog.GROUP_ORDER:
            it = QTreeWidgetItem([catalog.GROUP_TITLES[g], "", "", ""])
            f = it.font(0)
            f.setBold(True)
            it.setFont(0, f)
            it.setFlags(Qt.ItemFlag.ItemIsEnabled)
            groups[g] = it
        for a in apps:
            if a.pkg in homes and a.pkg != catalog.PROJECTIVY:
                continue  # головні екрани — на сторінці «Головний екран»
            k = catalog.classify(a.pkg, a.system)
            if not k:
                continue
            it = QTreeWidgetItem([k.name, "увімкнено" if a.enabled else "вимкнено", k.tip, a.pkg])
            it.setData(0, Qt.ItemDataRole.UserRole, a)
            it.setFlags(Qt.ItemFlag.ItemIsEnabled | Qt.ItemFlag.ItemIsUserCheckable | Qt.ItemFlag.ItemIsSelectable)
            it.setCheckState(0, Qt.CheckState.Checked if (k.preselect and a.enabled) else Qt.CheckState.Unchecked)
            it.setToolTip(0, a.pkg)
            if not a.enabled:
                for c in range(4):
                    it.setForeground(c, QColor("#6B7380"))
            groups[k.group].addChild(it)
        for g in catalog.GROUP_ORDER:
            it = groups[g]
            if it.childCount():
                it.setText(0, f"{catalog.GROUP_TITLES[g]}  ({it.childCount()})")
                self.tree.addTopLevelItem(it)
                it.setExpanded(g != catalog.GROUP_SYSTEM)
        self.tree.blockSignals(False)
        self.filter(self.search.text())
        self.count()

    def items(self):
        for i in range(self.tree.topLevelItemCount()):
            g = self.tree.topLevelItem(i)
            for j in range(g.childCount()):
                yield g.child(j)

    def checked(self):
        return [it.data(0, Qt.ItemDataRole.UserRole) for it in self.items() if it.checkState(0) == Qt.CheckState.Checked]

    def count(self):
        n = len(self.checked())
        self.sel.setText(f"Вибрано: {n}" if n else "Позначте програми галочками")
        for b in (self.btn_off, self.btn_on, self.btn_del):
            b.setEnabled(n > 0)

    def filter(self, text):
        t = text.strip().lower()
        for it in self.items():
            it.setHidden(bool(t) and t not in it.text(0).lower() and t not in it.text(3).lower())

    def disable(self):
        sel = [a.pkg for a in self.checked() if a.enabled]
        if not sel:
            self.win.toast("Вибрані програми вже вимкнено")
            return
        risky = [catalog.pretty(p) for p in sel if (catalog.classify(p, True) or catalog.Known("", "")).group
                 in (catalog.GROUP_CAREFUL, catalog.GROUP_SYSTEM) and p in {a.pkg for a in self.apps if a.system}]
        if risky and not self.win.confirm("Вимкнути системні програми?",
                                          "Серед вибраних є програми, від яких залежать функції телевізора:\n\n• " +
                                          "\n• ".join(risky[:12]) + "\n\nЯкщо щось перестане працювати — увімкніть їх назад тут."):
            return
        self.win.run(lambda: self.win.actions.disable(sel), self.after("Вимкнено"), self.win.error)

    def enable(self):
        sel = [a.pkg for a in self.checked() if not a.enabled]
        if not sel:
            self.win.toast("Вибрані програми й так увімкнені")
            return
        self.win.run(lambda: self.win.actions.enable(sel), self.after("Увімкнено"), self.win.error)

    def uninstall(self):
        sel = [a for a in self.checked()]
        user = [a.pkg for a in sel if not a.system]
        if not user:
            self.win.toast("Видаляти можна лише програми, встановлені вами. Системні — вимикайте.")
            return
        if not self.win.confirm("Видалити програми?", "Ці програми буде видалено з телевізора разом з їхніми даними:\n\n• " +
                                "\n• ".join(catalog.pretty(p) for p in user[:15]) + "\n\nПовернути можна лише встановивши заново з Google Play."):
            return
        self.win.run(lambda: self.win.actions.uninstall(user), self.after("Видалено"), self.win.error)

    def after(self, verb):
        def done(res):
            ok, bad = res
            msg = f"{verb}: {len(ok)}"
            if bad:
                msg += f" · не вдалося: {', '.join(catalog.pretty(p) for p in bad[:5])}"
            self.win.toast(msg)
            self.entered()
        return done


class InstallPage(Page):
    def __init__(self, win):
        super().__init__(win, "Встановити APK", "Будь-які програми з комп'ютера — без Google Play і без флешки. "
                                              "Можна просто перетягнути .apk файли у це вікно.")
        card = Card("Ваші APK", "Оберіть один чи кілька файлів .apk. Якщо програма вже є — оновиться, дані збережуться.")
        self.btn = button("Обрати APK…", "primary", self.pick)
        card.row(self.btn)
        self.state = label("", "muted")
        card.lay.addWidget(self.state)
        self.lay.addWidget(card)
        tips = Card("Де брати APK", "Найбезпечніше — з офіційних сторінок розробників (GitHub-релізи, сайт програми) або з APKMirror, "
                                    "де файли перевіряють на підпис. Програми для телевізора (Android TV) зручніші з пультом. "
                                    "Сховати зайві плитки з головного екрана «Лад» — утримання OK або кнопка Menu на плитці → «Сховати з екрана». "
                                    "Повністю вимкнути програму — сторінка «Програми».")
        self.lay.addWidget(tips)
        self.finish()

    def pick(self):
        if self.win.sync:
            return
        paths, _ = QFileDialog.getOpenFileNames(self, "Оберіть APK", os.path.expanduser("~"), "Програми Android (*.apk)")
        if paths:
            self.install(paths)

    def install(self, paths):
        paths = [p for p in paths if p.lower().endswith(".apk")]
        if not paths:
            return
        if not self.win.tv:
            self.win.error("Спершу підключіться до телевізора.")
            return
        self.btn.setEnabled(False)
        self.state.setText(f"Встановлюю {len(paths)}… (великі програми — до хвилини кожна)")

        def prog(n, total, name):
            self.win.sig_status.emit(f"Встановлюю {n + 1} з {total}: {name}")

        def done(res):
            ok, bad = res
            self.btn.setEnabled(True)
            text = f"Встановлено: {len(ok)}" + (" — " + ", ".join(ok) if ok else "")
            if bad:
                text += "\nНе вдалося:\n" + "\n".join(bad)
            self.state.setText(text)
            self.win.toast(f"Встановлено {len(ok)} з {len(paths)}")

        self.win.run(lambda: self.win.actions.install_apks(paths, prog), done,
                     lambda e: (self.btn.setEnabled(True), self.state.setText(""), self.win.error(e)))


class HomePage(Page):
    def __init__(self, win):
        super().__init__(win, "Головний екран", "Замість стандартного екрана з рекламою й «рекомендаціями» — ваш, охайний і швидкий.")
        cur = Card("Зараз")
        self.cur = label("", "big")
        cur.lay.addWidget(self.cur)
        self.lay.addWidget(cur)

        lad = Card("«Лад» — головний екран у стилі Material",
                   "Великі плитки входів: PlayStation, ПК та інші HDMI — перемикання одним натисканням пульта. "
                   "Ряди «Улюблене» й «Усі програми», годинник, шпалери. Без реклами й без інтернету. "
                   "Що куди підключено, задається довгим натисканням OK на плитці входу.")
        self.lad_state = label("", "muted")
        lad.lay.addWidget(self.lad_state)
        self.btn_lad = button("Встановити «Лад» і зробити головним", "primary", self.install_lad)
        self.btn_lad_home = button("Зробити головним", slot=lambda: self.make_home(catalog.LAD, "«Лад»"))
        lad.row(self.btn_lad, self.btn_lad_home)
        self.lay.addWidget(lad)

        pj = Card("Або Projectivy Launcher", "Готовий безкоштовний головний екран з Google Play: багато налаштувань і власні шпалери.")
        self.pj_state = label("", "muted")
        pj.lay.addWidget(self.pj_state)
        self.btn_store = button("Встановити — відкрити в Google Play на телевізорі", slot=self.store)
        self.btn_pj = button("Зробити головним", slot=lambda: self.make_home(catalog.PROJECTIVY, "Projectivy"))
        pj.row(self.btn_store, self.btn_pj, button("Оновити", slot=self.entered))
        self.lay.addWidget(pj)

        back = Card("Повернути стандартний", "Стандартний головний екран не видаляється — лише вимикається. Ця кнопка повертає все як було.")
        self.btn_back = button("Повернути стандартний головний екран", slot=self.restore)
        back.row(self.btn_back)
        self.lay.addWidget(back)
        self.finish()

    def entered(self):
        if not self.win.tv:
            return

        def work():
            tv = self.win.tv
            return tv.current_home(), tv.home_candidates(), tv.installed(catalog.PROJECTIVY), tv.installed(catalog.LAD)

        self.win.run(work, self.show, self.win.error)

    def show(self, res):
        cur, cands, pj, lad = res
        self.win.homes = cands
        names = {"com.google.android.tvlauncher": "Стандартний Android TV", "com.google.android.leanbacklauncher": "Стандартний Android TV",
                 "com.google.android.apps.tv.launcherx": "Google TV", catalog.PROJECTIVY: "Projectivy Launcher", catalog.LAD: "«Лад»"}
        self.cur.setText(names.get(cur, catalog.pretty(cur)) if cur else "Система питатиме, який обрати")
        has_apk = os.path.exists(resource("lad-home.apk"))
        if cur == catalog.LAD:
            self.lad_state.setText("Встановлено й працює як головний екран ✓  (кнопка нижче оновить до версії з цієї програми)")
        elif lad:
            self.lad_state.setText("Встановлено. Можна зробити головним.")
        else:
            self.lad_state.setText("Ще не встановлено." if has_apk else "У цій збірці немає файлу «Лад».")
        self.btn_lad.setText("Оновити «Лад»" if cur == catalog.LAD else "Встановити «Лад» і зробити головним")
        self.btn_lad.setEnabled(has_apk)
        self.btn_lad_home.setEnabled(lad and cur != catalog.LAD)
        if not pj:
            self.pj_state.setText("Не встановлено.")
        elif cur == catalog.PROJECTIVY:
            self.pj_state.setText("Встановлено й працює як головний екран ✓")
        else:
            self.pj_state.setText("Встановлено.")
        self.btn_pj.setEnabled(pj and cur != catalog.PROJECTIVY)
        self.btn_store.setEnabled(not pj)
        self.btn_back.setEnabled(bool(self.win.history and self.win.history.launchers))

    def install_lad(self):
        apk = resource("lad-home.apk")
        self.lad_state.setText("Встановлюю на телевізор… (до хвилини)")
        self.win.run(lambda: self.win.actions.install_lad(apk),
                     lambda _: (self.win.toast("«Лад» встановлено ✓ Натисніть «Додому» на пульті"), self.entered()),
                     lambda e: (self.win.error(e), self.entered()))

    def make_home(self, pkg, name):
        if not self.win.confirm(f"Зробити {name} головним?",
                                "Стандартний головний екран буде вимкнено (не видалено). Повернути його можна будь-коли "
                                "кнопкою «Повернути стандартний» або у «Відновленні»."):
            return
        self.win.run(lambda: self.win.actions.set_home(pkg),
                     lambda _: (self.win.toast("Готово ✓ Натисніть «Додому» на пульті"), self.entered()), self.win.error)

    def store(self):
        self.win.run(lambda: self.win.tv.open_store(catalog.PROJECTIVY),
                     lambda _: self.win.toast("Подивіться на телевізор: відкрився Google Play ✓"), self.win.error)

    def restore(self):
        self.win.run(self.win.actions.restore_home, lambda _: (self.win.toast("Стандартний головний екран повернуто ✓"), self.entered()),
                     self.win.error)


class YouTubePage(Page):
    def __init__(self, win):
        super().__init__(win, "YouTube без реклами", "SmartTube — безкоштовний YouTube для телевізорів без реклами, з пропуском "
                                                     "спонсорських вставок і роботою на старих Android (від 4.3).")
        st = Card("SmartTube")
        self.state = label("", "muted")
        st.lay.addWidget(self.state)
        self.hide = QComboBox()
        self.hide.addItems(["Стандартний YouTube залишити", "Стандартний YouTube сховати (вимкнути)"])
        self.btn = button("Встановити SmartTube", "primary", self.install)
        st.row(self.btn, self.hide)
        self.progress = QProgressBar()
        self.progress.setRange(0, 100)
        self.progress.setFixedHeight(6)
        self.progress.setTextVisible(False)
        self.progress.hide()
        st.lay.addWidget(self.progress)
        st.lay.addWidget(label("Файл завантажується лише з офіційної сторінки розробника на GitHub "
                               "(github.com/yuliskov/SmartTube). Не ставте SmartTube з APK-сайтів і магазинів — "
                               "там бувають підробки з вірусами.", "muted"))
        self.lay.addWidget(st)
        tips = Card("Після встановлення", "Відкрийте SmartTube на телевізорі й увійдіть у свій акаунт Google (за кодом на "
                                          "youtube.com/activate з телефона) — підписки й історія будуть ті самі. "
                                          "Оновлення SmartTube пропонує сам.")
        self.lay.addWidget(tips)
        self.finish()

    def entered(self):
        if not self.win.tv:
            return

        def work():
            tv = self.win.tv
            return next((p for p in catalog.SMARTTUBE_PKGS if tv.installed(p)), None)

        self.win.run(work, self.show, self.win.error)

    def show(self, pkg):
        self.state.setText("Встановлено ✓ (кнопка оновить до найновішої версії)" if pkg else "Ще не встановлено.")
        self.btn.setText("Оновити SmartTube" if pkg else "Встановити SmartTube")

    def install(self):
        self.btn.setEnabled(False)
        self.progress.setValue(0)
        self.progress.show()
        self.state.setText("Завантажую з GitHub…")
        hide = self.hide.currentIndex() == 1

        def prog(done, total):
            if total:
                self.win.sig_progress.emit(int(done * 100 / total))

        def ok(_):
            self.btn.setEnabled(True)
            self.progress.hide()
            self.win.toast("SmartTube встановлено ✓ Шукайте його серед програм на телевізорі")
            self.entered()

        def fail(e):
            self.btn.setEnabled(True)
            self.progress.hide()
            self.state.setText("")
            self.win.error(e)

        self.win.run(lambda: self.win.actions.install_smarttube(prog, hide), ok, fail)


class DiagPage(Page):
    def __init__(self, win):
        super().__init__(win, "Діагностика", "Що саме гальмує: пам'ять, процесор, місце, знос вбудованої пам'яті й усі програми — в одному файлі.")
        rep = Card("Звіт про телевізор", "Займає хвилину. Файл можна надіслати Claude чи майстру — за ним видно, що вимкнути і чи не "
                                        "зношена пам'ять. Особистих даних у звіті немає: лише технічні показники й назви програм.")
        self.btn = button("Зібрати звіт", "primary", self.collect)
        self.btn_open = button("Відкрити теку зі звітами", slot=self.open_dir)
        rep.row(self.btn, self.btn_open)
        self.path = label("", "ok")
        rep.lay.addWidget(self.path)
        self.preview = QPlainTextEdit()
        self.preview.setReadOnly(True)
        self.preview.setMinimumHeight(260)
        self.preview.setStyleSheet("font-family: Consolas, 'DejaVu Sans Mono', monospace; font-size: 9pt; background: #12151A; "
                                   "border: 1px solid #232934; border-radius: 10px;")
        self.preview.hide()
        rep.lay.addWidget(self.preview)
        self.lay.addWidget(rep)

        shot = Card("Знімок екрана телевізора", "Зберігає, що зараз на екрані телевізора, у PNG — зручно показати Claude чи майстру, як виглядає.")
        self.shot_res = label("", "ok")
        shot.row(button("Зробити знімок", slot=self.screenshot), self.shot_res)
        self.lay.addWidget(shot)

        lst = Card("Застосувати список", "Отримали список програм для вимкнення (.txt — по одній у рядку, або .json)? "
                                        "Системне, без чого телевізор не працює, однаково не вимкнеться. Усе можна повернути у «Відновленні».")
        lst.row(button("Обрати файл зі списком…", slot=self.apply_list))
        self.lay.addWidget(lst)
        self.finish()

    @staticmethod
    def folder() -> str:
        base = QStandardPaths.writableLocation(QStandardPaths.StandardLocation.DocumentsLocation) or os.path.expanduser("~")
        d = os.path.join(base, "TVLad")
        os.makedirs(d, exist_ok=True)
        return d

    def collect(self):
        self.btn.setEnabled(False)
        self.path.setText("Збираю… (до хвилини)")

        def work():
            text = self.win.tv.report()
            import time as _t
            p = os.path.join(self.folder(), f"tv-report-{_t.strftime('%Y%m%d-%H%M')}.txt")
            with open(p, "w", encoding="utf-8") as f:
                f.write(text)
            return p, text

        def ok(res):
            p, text = res
            self.btn.setEnabled(True)
            self.path.setText(f"Збережено: {p}")
            self.preview.setPlainText(text)
            self.preview.show()
            QApplication.clipboard().setText(p)
            self.win.toast("Звіт готовий ✓ шлях до файлу скопійовано")

        self.win.run(work, ok, lambda e: (self.btn.setEnabled(True), self.path.setText(""), self.win.error(e)))

    def screenshot(self):
        import time as _t
        p = os.path.join(self.folder(), f"tv-screen-{_t.strftime('%Y%m%d-%H%M%S')}.png")

        def ok(path):
            self.shot_res.setText(f"Збережено: {path}")
            QApplication.clipboard().setText(path)
            if not self.win.sync:
                QDesktopServices.openUrl(QUrl.fromLocalFile(path))

        self.win.run(lambda: self.win.tv.adb.screencap(p), ok, self.win.error)

    def open_dir(self):
        QDesktopServices.openUrl(QUrl.fromLocalFile(self.folder()))

    def apply_list(self):
        if self.win.sync:
            return
        path, _ = QFileDialog.getOpenFileName(self, "Список програм", self.folder(), "Списки (*.txt *.json);;Усі файли (*)")
        if not path:
            return
        try:
            pkgs = Actions.read_list(path)
        except (OSError, UnicodeDecodeError) as e:
            self.win.error(f"Не вдалося прочитати файл: {e}")
            return
        if not pkgs:
            self.win.error("У файлі немає назв програм.")
            return
        if not self.win.confirm("Вимкнути програми зі списку?", f"Буде вимкнено {len(pkgs)}:\n\n• " +
                                "\n• ".join(catalog.pretty(p) + f"  ({p})" for p in pkgs[:25]) + ("\n…" if len(pkgs) > 25 else "")):
            return

        def done(res):
            ok, bad = res
            msg = f"Вимкнено: {len(ok)}"
            if bad:
                msg += f" · пропущено (захищені або немає на телевізорі): {len(bad)}"
            self.win.toast(msg)

        self.win.run(lambda: self.win.actions.disable(pkgs), done, self.win.error)


class RestorePage(Page):
    def __init__(self, win):
        super().__init__(win, "Відновлення", "Усе, що «ТВ Лад» змінював на цьому телевізорі, записано — і можна повернути одним натисканням.")
        self.summary = Card("Що змінено")
        self.sum_text = label("", "muted", False)
        self.summary.lay.addWidget(self.sum_text)
        self.btn = button("Відновити все як було", "primary", self.restore)
        self.summary.row(self.btn)
        self.lay.addWidget(self.summary)
        self.lay.addWidget(label("Журнал", "cardTitle"))
        self.log = QListWidget()
        self.log.setMinimumHeight(220)
        self.lay.addWidget(self.log, 1)

    def entered(self):
        h = self.win.history
        if not h:
            return
        parts = []
        if h.data.get("animations"):
            parts.append("• анімації змінено")
        if h.disabled:
            names = ", ".join(catalog.pretty(p) for p in h.disabled)
            parts.append("• вимкнено програм: " + str(len(h.disabled)) + " — " + (names if len(names) < 90 else names[:88] + "…"))
        if h.launchers:
            parts.append("• стандартний головний екран вимкнено")
        self.sum_text.setText("\n".join(parts) if parts else "Нічого не змінено — телевізор у початковому стані.")
        self.btn.setEnabled(bool(parts))
        self.log.clear()
        for line in reversed(h.data.get("log") or []):
            self.log.addItem(line)

    def restore(self):
        if not self.win.confirm("Відновити все?", "Повернути анімації, увімкнути всі вимкнені програми й стандартний головний екран?"):
            return
        self.win.run(self.win.actions.restore_all, lambda _: (self.win.toast("Усе повернуто як було ✓"), self.entered()), self.win.error)


# ───────── головне вікно ─────────

class Main(QMainWindow):
    sig_status = pyqtSignal(str)
    sig_progress = pyqtSignal(int)

    def __init__(self, adb=None, sync: bool = False):
        super().__init__()
        self.sync = sync
        self.adb: Adb | None = adb
        self.tv: TV | None = None
        self.info: Info | None = None
        self.history: History | None = None
        self.actions: Actions | None = None
        self.homes: list[str] = []
        self.tasks: set[Task] = set()
        self.settings = QSettings("x44to", "TVLad")
        self.setWindowTitle(f"{APP_NAME} — порядок у вашому Android TV")
        if os.path.exists(resource("icon.png")):
            self.setWindowIcon(QIcon(resource("icon.png")))
        self.resize(1080, 720)
        self.setMinimumSize(900, 600)

        root = QWidget()
        h = QHBoxLayout(root)
        h.setContentsMargins(0, 0, 0, 0)
        h.setSpacing(0)
        side = QFrame()
        side.setObjectName("sidebar")
        side.setFixedWidth(230)
        sl = QVBoxLayout(side)
        sl.setContentsMargins(16, 22, 16, 16)
        sl.setSpacing(4)
        sl.addWidget(label(APP_NAME, "brand", False))
        sl.addWidget(label("для телевізорів KIVI та інших Android TV", "brandSub"))
        sl.addSpacing(18)
        self.stack = QStackedWidget()
        self.pages = [ConnectPage(self), DiagPage(self), SpeedPage(self), AppsPage(self), InstallPage(self), HomePage(self), YouTubePage(self), RestorePage(self)]
        names = ["Підключення", "Діагностика", "Прискорення", "Програми", "Встановити APK", "Головний екран", "YouTube без реклами", "Відновлення"]
        self.nav = QButtonGroup(self)
        self.nav.setExclusive(True)
        self.nav_btns = []
        for i, (p, n) in enumerate(zip(self.pages, names)):
            self.stack.addWidget(p)
            b = QPushButton(n)
            b.setObjectName("nav")
            b.setCheckable(True)
            b.setCursor(Qt.CursorShape.PointingHandCursor)
            b.clicked.connect(lambda _=False, i=i: self.go(i))
            self.nav.addButton(b, i)
            self.nav_btns.append(b)
            sl.addWidget(b)
        sl.addStretch(1)
        self.chip = label("Не підключено", "chip")
        sl.addWidget(self.chip)
        sl.addWidget(label(f"версія {VERSION}", "brandSub"))
        h.addWidget(side)

        right = QVBoxLayout()
        right.setSpacing(0)
        right.addWidget(self.stack, 1)
        self.progress = QProgressBar()
        self.progress.setRange(0, 0)
        self.progress.setTextVisible(False)
        self.progress.setFixedHeight(4)
        self.progress.hide()
        right.addWidget(self.progress)
        self.statusline = label("", "status", False)
        right.addWidget(self.statusline)
        h.addLayout(right, 1)
        self.setCentralWidget(root)
        self.setAcceptDrops(True)
        self.sig_status.connect(self.toast)
        self.sig_progress.connect(lambda v: self.pages[6].progress.setValue(v))
        self.nav_btns[0].setChecked(True)
        self.set_connected(False)

    # ── перетягування .apk у вікно ──
    def dragEnterEvent(self, e):
        if e.mimeData().hasUrls() and any(u.toLocalFile().lower().endswith(".apk") for u in e.mimeData().urls()):
            e.acceptProposedAction()

    def dropEvent(self, e):
        paths = [u.toLocalFile() for u in e.mimeData().urls() if u.toLocalFile().lower().endswith(".apk")]
        if paths:
            self.go(4)
            self.pages[4].install(paths)

    # ── загальне ──
    def run(self, fn, ok, fail):
        if self.sync:
            try:
                res = fn()
            except AdbError as e:
                fail(str(e))
                return
            ok(res)
            return
        t = Task(fn)
        t.done.connect(ok)
        t.failed.connect(fail)
        t.finished.connect(lambda: (self.tasks.discard(t), self.busy(bool(self.tasks))))
        self.tasks.add(t)
        self.busy(True)
        t.start()

    def busy(self, on: bool):
        self.progress.setVisible(on)

    def toast(self, text: str):
        self.statusline.setText(text)
        if not self.sync:
            QTimer.singleShot(9000, lambda: self.statusline.text() == text and self.statusline.setText(""))

    def error(self, text: str):
        if self.sync:
            self.toast("Помилка: " + text)
            return
        QMessageBox.warning(self, APP_NAME, text)

    def confirm(self, title: str, text: str) -> bool:
        if self.sync:
            return True
        return QMessageBox.question(self, title, text) == QMessageBox.StandardButton.Yes

    def ensure_adb(self, progress=None) -> Adb:
        if self.adb:
            return self.adb
        exe = find_adb()
        if not exe:
            self.sig_status.emit("Перший запуск: завантажую adb від Google (≈ 7 МБ)…")
            exe = download_adb(progress)
        self.adb = Adb(exe)
        return self.adb

    def go(self, i: int):
        self.nav_btns[i].setChecked(True)
        self.stack.setCurrentIndex(i)
        self.pages[i].entered()

    def set_connected(self, on: bool):
        for b in self.nav_btns[1:]:
            b.setEnabled(on)
        if on and self.info:
            self.chip.setText(f"● {self.info.title}\n{self.adb.serial if self.adb else ''}")
            self.chip.setStyleSheet(f"color: {ACCENT};")
        else:
            self.chip.setText("○ Не підключено")
            self.chip.setStyleSheet("")

    def on_connected(self, tv: TV, info: Info):
        self.tv = tv
        self.info = info
        self.history = History(info.serial)
        self.actions = Actions(tv, self.history)
        self.set_connected(True)
        self.pages[0].show_tv(info)
        self.toast(f"Підключено до {info.title}")
        try:
            self.homes = tv.home_candidates()
        except AdbError:
            self.homes = []

    def disconnect(self):
        if self.adb:
            self.adb.disconnect()
        self.tv = self.info = self.history = self.actions = None
        self.set_connected(False)
        self.pages[0].show_tv(None)
        self.go(0)


_APP: QApplication | None = None


def make_app(argv) -> QApplication:
    global _APP
    app = QApplication.instance() or QApplication(argv)
    _APP = app  # тримаємо посилання, інакше Python прибере застосунок і вікна впадуть
    app.setApplicationName(APP_NAME)
    app.setStyleSheet(QSS)
    f = QFont("Segoe UI" if sys.platform == "win32" else "Noto Sans")
    f.setPointSize(10)
    app.setFont(f)
    return app


def screenshots(folder: str, history_dir: str) -> list[str]:
    """Знімки всіх сторінок на уявному телевізорі (для перевірки вигляду)."""
    from .fake import FakeAdb

    os.makedirs(folder, exist_ok=True)
    win = Main(adb=FakeAdb(), sync=True)
    tv = TV(win.adb)
    info = tv.info()
    win.on_connected(tv, info)
    win.history = History(info.serial, history_dir)
    win.actions = Actions(tv, win.history)
    win.resize(1080, 720)
    win.show()
    out = []
    names = ["1-connect", "2-diag", "3-speed", "4-apps", "5-install", "6-home", "7-youtube", "8-restore"]
    for i, n in enumerate(names):
        if i == 1:
            win.pages[1].collect()
        if i == 7:
            win.actions.disable(["com.google.android.play.games", "com.google.android.videos"])
            win.actions.set_animations(0.5)
            win.actions.set_home(catalog.PROJECTIVY)
        win.go(i)
        QApplication.processEvents()
        p = os.path.join(folder, n + ".png")
        win.grab().save(p)
        out.append(p)
    win.close()
    return out


def main(argv=None) -> int:
    argv = list(sys.argv if argv is None else argv)
    app = make_app(argv)
    adb = None
    if "--fake" in argv:
        from .fake import FakeAdb
        adb = FakeAdb()
    win = Main(adb=adb)
    win.show()
    return app.exec()
