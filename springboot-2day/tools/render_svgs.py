#!/usr/bin/env python3
"""
Render every images/*.svg under the lesson folders to a PNG next to it (same name, .png).
Uses headless Chrome/Edge, so the PNG looks exactly like the SVG in a browser.
Markdown links the PNG (works in every viewer) and the PNG links to the SVG (sharp, zoomable).

Usage: python tools/render_svgs.py [root folder, default = parent of this tools folder]
"""
import os, re, subprocess, sys, tempfile, pathlib

BROWSERS = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
]
browser = next((b for b in BROWSERS if os.path.exists(b)), None)
if not browser:
    sys.exit("No Chrome/Edge found")

root = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent
tmp = pathlib.Path(tempfile.mkdtemp(prefix="svgrender-"))
ok = fail = 0
for svg in sorted(root.rglob("images/*.svg")):
    text = svg.read_text(encoding="utf-8")
    m = re.search(r'viewBox="0 0 (\d+) (\d+)"', text)
    if not m:
        print("SKIP (no viewBox):", svg); continue
    w, h = int(m.group(1)), int(m.group(2))
    html = tmp / (svg.stem + ".html")
    html.write_text(
        f'<!doctype html><html><head><meta charset="utf-8"><style>html,body{{margin:0;background:#fff}}'
        f'img{{display:block}}</style></head><body><img src="{svg.as_uri()}" width="{w}" height="{h}"></body></html>',
        encoding="utf-8")
    png = svg.with_suffix(".png")
    cmd = [browser, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
           f"--window-size={w},{h}", f"--screenshot={png}", html.as_uri()]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if png.exists() and png.stat().st_size > 1000:
        ok += 1; print("OK  ", png.relative_to(root), f"{w}x{h}")
    else:
        fail += 1; print("FAIL", svg, r.stderr[-300:])
print(f"rendered {ok}, failed {fail}")
sys.exit(1 if fail else 0)
