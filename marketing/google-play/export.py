#!/usr/bin/env python3
"""Render vector store layouts with unchanged Compose capture images.

Requires the system PyGObject/Rsvg and Cairo packages plus Noto fonts.
The output surface is RGB24, so the PNGs have no alpha channel.
"""
import base64
from html import escape
import json
from pathlib import Path
import shutil

import cairo
import gi
gi.require_version("Rsvg", "2.0")
from gi.repository import Rsvg

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
METADATA = REPO / "fastlane/metadata/android"
CAPTIONS = json.loads((ROOT / "captions.json").read_text())
PHONE = ["home", "add", "calendar", "assistant", "templates", "security"]
TABLET = ["home", "calendar", "add", "templates"]


def text(value, x, y, size, fill, *, arabic=False, bold=False, anchor=None):
    # In SVG, the start anchor of RTL text is its right edge.
    attrs = f'direction="rtl" unicode-bidi="embed"' if arabic else 'direction="ltr"'
    family = "Noto Sans Arabic" if arabic else "Noto Sans"
    return (f'<text x="{x}" y="{y}" font-family="{family}" font-size="{size}" '
            f'font-weight="{700 if bold else 400}" fill="{fill}" '
            f'text-anchor="{anchor or "start"}" {attrs}>{escape(value)}</text>')


def artwork(lang, kind, screen, number):
    ar = lang == "ar"
    feature = kind == "feature"
    width, height = (1024, 500) if feature else (1080, 1920)
    dark = feature or screen in ["home", "calendar", "templates"]
    background, ink, secondary = ("#10282c", "#f2f8f3", "#bfd5ca") if dark else ("#edf1e9", "#173e3b", "#526e63")
    glow = "#2b685c" if dark else "#c8ddd3"
    title, subtitle = CAPTIONS[screen][lang]
    if feature:
        title, subtitle = (["تذكيراتك،\nبشكل أوضح.", "تذكيرات · تقويم · مساعد ذكي"] if ar else
                           ["A little less\nto remember.", "Reminders · Calendar · AI assistance"])
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             f'<defs><radialGradient id="glow"><stop stop-color="{glow}"/><stop offset="1" stop-color="{background}"/></radialGradient></defs>',
             f'<rect width="{width}" height="{height}" fill="{background}"/>',
             f'<ellipse cx="{width}" cy="{height*.68}" rx="{width*.8}" ry="{height*.65}" fill="url(#glow)"/>',
             f'<ellipse cx="{width+100}" cy="{height*.83}" rx="{width*.7}" ry="{height*.6}" fill="none" stroke="{secondary}" stroke-opacity=".10"/>']
    brand_y = 70
    if feature and ar:
        parts += [text("DotReminder", 972, brand_y, 26, ink, bold=True, anchor="end"), '<circle cx="780" cy="61" r="6" fill="#6bd8a8"/>']
    else:
        bx = 52 if feature else 70
        parts += [f'<circle cx="{bx+6}" cy="61" r="6" fill="#6bd8a8"/>', text("DotReminder", bx+25, brand_y, 26 if feature else 27, ink, bold=True)]
    if not feature:
        parts.append(text("DOTSUITE", 1010, 68, 20, secondary, anchor="end"))
    x = width-52 if feature and ar else 52 if feature else 1010 if ar else 70
    font = 51 if feature and ar else 49 if feature else 76 if ar else 78
    first_y = 185 if feature else 170 if ar else 175
    step = 71 if feature and ar else 58 if feature else 96 if ar else 88
    for line, value in enumerate(title.splitlines()):
        parts.append(text(value, x, first_y+step*line, font, ink, arabic=ar, bold=True))
    parts.append(text(subtitle, x, 307 if feature else 325, 22 if feature and ar else 21 if feature else 29, secondary, arabic=ar))

    family = "tablet" if kind == "tablet" else "phone"
    image = base64.b64encode((ROOT / "screens" / f"store-{family}-{screen}.png").read_bytes()).decode()
    if feature:
        iw, ih, ix, iy = 300, 300*2235/1080, 48 if ar else 670, 58
    elif kind == "tablet":
        iw, ih, ix, iy = 948, 1264, 66, 473
    else:
        iw, ih, ix, iy = 1500*1080/2235, 1500, (1080-1500*1080/2235)/2, 360
    radius = 19 if feature else 28
    rotate = f'transform="rotate({5 if ar else -5} {ix+iw/2} {iy+ih/2})"' if feature else ""
    parts += [f'<g {rotate}>', f'<rect x="{ix-9}" y="{iy+20}" width="{iw+18}" height="{ih+8}" rx="{radius+9}" fill="#00191a" opacity=".11"/>',
              f'<defs><clipPath id="screen"><rect x="{ix}" y="{iy}" width="{iw}" height="{ih}" rx="{radius}"/></clipPath></defs>',
              f'<image x="{ix}" y="{iy}" width="{iw}" height="{ih}" href="data:image/png;base64,{image}" clip-path="url(#screen)"/>',
              f'<rect x="{ix}" y="{iy}" width="{iw}" height="{ih}" rx="{radius}" fill="none" stroke="{secondary}" stroke-opacity=".5" stroke-width="2"/>', '</g>']
    footer = "#a6c4b7" if dark else "#658579"
    parts.append(text("dotsuite.dev", width-52 if feature and ar else 52 if feature else 70,
                      450 if feature else 1890, 17 if feature else 19, footer, anchor="end" if feature and ar else None))
    if not feature:
        parts.append(text(f'{number:02d} / {4 if kind == "tablet" else 6:02d}', 1010, 1890, 19, footer, anchor="end"))
    parts.append('</svg>')
    return ''.join(parts), width, height


def export(lang, kind, screen, number=1):
    svg, width, height = artwork(lang, kind, screen, number)
    source = ROOT / "vectors" / f"{lang}-{kind}-{screen}.svg"
    source.parent.mkdir(exist_ok=True)
    source.write_text(svg)
    group = "phoneScreenshots" if kind == "phone" else "tenInchScreenshots" if kind == "tablet" else ""
    directory = METADATA / lang / "images" / group
    directory.mkdir(parents=True, exist_ok=True)
    destination = directory / ("featureGraphic.png" if kind == "feature" else f"{number:02d}-{screen}.png")
    handle = Rsvg.Handle.new_from_data(svg.encode())
    surface = cairo.ImageSurface(cairo.FORMAT_RGB24, width, height)
    context = cairo.Context(surface)
    bounds = Rsvg.Rectangle()
    bounds.x = bounds.y = 0
    bounds.width, bounds.height = width, height
    handle.render_document(context, bounds)
    surface.write_to_png(str(destination))
    return {"file":str(destination.relative_to(REPO)), "size":[width,height], "bytes":destination.stat().st_size, "mode":"RGB"}


manifest = []
for language in ["ar", "en-US"]:
    for n, name in enumerate(PHONE, 1):
        manifest.append(export(language, "phone", name, n))
    manifest.append(export(language, "feature", "home"))
    for n, name in enumerate(TABLET, 1):
        record = export(language, "tablet", name, n)
        manifest.append(record)
        # The same responsive tablet layout is used in both store tablet slots.
        source = REPO / record["file"]
        seven = source.parent.parent / "sevenInchScreenshots" / source.name
        seven.parent.mkdir(exist_ok=True)
        shutil.copy2(source, seven)
        manifest.append({**record, "file":str(seven.relative_to(REPO))})
(ROOT / "export-manifest.json").write_text(json.dumps(manifest, indent=2)+'\n')
print(f"Exported {len(manifest)} RGB files (22 distinct images) at exact Google Play dimensions.")

# Review sheet: the same final PNGs arranged on a vector canvas, without altering them.
sheet = ['<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="1660">',
         '<rect width="1800" height="1660" fill="#10282c"/>']
for row, language in enumerate(["ar", "en-US"]):
    y = 70 + row*570
    sheet.append(text("Arabic" if row == 0 else "English", 24, y-22, 25, "#edf1e9", bold=True))
    for col, name in enumerate(PHONE):
        path = METADATA / language / "images/phoneScreenshots" / f"{col+1:02d}-{name}.png"
        encoded = base64.b64encode(path.read_bytes()).decode()
        sheet.append(f'<image x="{24+col*296}" y="{y}" width="272" height="484" href="data:image/png;base64,{encoded}"/>')
sheet.append(text("Feature graphics", 24, 1192, 25, "#edf1e9", bold=True))
for col, language in enumerate(["ar", "en-US"]):
    encoded = base64.b64encode((METADATA/language/"images/featureGraphic.png").read_bytes()).decode()
    sheet.append(f'<image x="{24+col*888}" y="1210" width="864" height="422" href="data:image/png;base64,{encoded}"/>')
sheet.append('</svg>')
handle = Rsvg.Handle.new_from_data(''.join(sheet).encode())
surface = cairo.ImageSurface(cairo.FORMAT_RGB24, 1800, 1660)
bounds = Rsvg.Rectangle()
bounds.width, bounds.height = 1800, 1660
handle.render_document(cairo.Context(surface), bounds)
surface.write_to_png(str(ROOT/"contact-sheet.png"))
