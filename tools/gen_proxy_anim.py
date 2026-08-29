# Pre-rendered loop animation behind the proxy start button.
# Scene: concentric dashed orbit rings rotating smoothly,
# paper planes completing a full 360-degree circuit nose-first,
# slow twinkling orbital particles, and soft breathing radial glow.
#
# Guaranteed 100% mathematically seamless periodic loop.
#
# Usage: py tools/gen_proxy_anim.py
import math
import random
from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
CENTER = SIZE / 2
SCALE = 1.5
HI_SIZE = int(SIZE * SCALE)
HI_CENTER = HI_SIZE / 2

# Telegram celestial color palette
BLUE = (42, 171, 238)         # #2AABEE telegram blue
BLUE_LIGHT = (125, 205, 248)  # #7DCDF8 light blue
BLUE_DEEP = (24, 124, 196)    # #187CC4 deep blue
CYAN = (0, 195, 255)          # bright cyan

OUT_DIR = "app/src/main/res/raw"

FRAMES = 75       # 75 frames at 60ms = 4.5s loop (smooth & calm)
FRAME_MS = 60


def make_base_glow():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    steps = 32
    max_r = 230
    for i in range(steps, 0, -1):
        r = max_r * i / steps
        a = int(115 * (1 - i / steps) ** 2)
        d.ellipse([CENTER - r, CENTER - r, CENTER + r, CENTER + r],
                  fill=BLUE + (a,))
    return img.filter(ImageFilter.GaussianBlur(8))


BASE_GLOW = make_base_glow()


def draw_dashed_ring(d, radius, dash_deg, gap_deg, rotation_deg, width, color, alpha):
    step = dash_deg + gap_deg
    a = 0.0
    r = radius * SCALE
    w = max(1, int(width * SCALE))
    fill_col = color + (int(alpha),)
    bbox = [HI_CENTER - r, HI_CENTER - r, HI_CENTER + r, HI_CENTER + r]
    while a < 360.0:
        start = (a + rotation_deg) % 360.0
        end = start + dash_deg
        d.arc(bbox, start=start, end=end, fill=fill_col, width=w)
        a += step


PLANE = [(16, 0), (-9, 7), (-4, 0), (-9, -7)]


def draw_paper_plane(d, radius_orbit, angle_deg, color, alpha, scale=1.0, forward_ccw=True):
    a = math.radians(angle_deg)
    r = radius_orbit * SCALE
    x = HI_CENTER + r * math.cos(a)
    y = HI_CENTER + r * math.sin(a)

    # Tangent angle in screen coords (X right, Y down)
    if forward_ccw:
        tang = a + math.pi / 2
    else:
        tang = a - math.pi / 2

    s = scale * SCALE
    pts = []
    for px, py in PLANE:
        rx = (px * math.cos(tang) - py * math.sin(tang)) * s + x
        ry = (px * math.sin(tang) + py * math.cos(tang)) * s + y
        pts.append((rx, ry))

    d.polygon(pts, fill=color + (int(alpha),))


# Generate calm, slow-moving particles (2x slower drift & subtle twinkling)
_rng = random.Random(2026)
PARTICLES = []
for _ in range(36):
    ang = _rng.uniform(0, 2 * math.pi)
    rad = _rng.uniform(160, 245)
    # Slow drift: mostly floating (0) or calm single turn (1, -1) over 4.5s
    k_rot = _rng.choice([0, 0, 1, -1, 0, 1, -1])
    tw_freq = _rng.choice([1, 2])
    PARTICLES.append({
        "rad": rad,
        "ang": ang,
        "r": _rng.uniform(1.4, 2.8),
        "phase": _rng.uniform(0, 2 * math.pi),
        "k_rot": k_rot,
        "tw_freq": tw_freq,
        "alpha": _rng.uniform(90, 210),
    })


def draw_particles(d, t):
    for p in PARTICLES:
        cur_ang = p["ang"] + t * 2 * math.pi * p["k_rot"]
        cur_r = p["rad"] * SCALE
        x = HI_CENTER + cur_r * math.cos(cur_ang)
        y = HI_CENTER + cur_r * math.sin(cur_ang)

        tw = 0.5 + 0.5 * math.sin(p["phase"] + t * 2 * math.pi * p["tw_freq"])
        a = int(p["alpha"] * (0.55 + 0.45 * tw))
        pr = p["r"] * SCALE
        d.ellipse([x - pr, y - pr, x + pr, y + pr],
                  fill=BLUE_LIGHT + (a,))


def render_orbit():
    out = []
    print(f"Rendering {FRAMES} frames ({FRAMES * FRAME_MS / 1000.0:.1f}s seamless loop)...")

    for f in range(FRAMES):
        t = f / FRAMES
        hi_frame = Image.new("RGBA", (HI_SIZE, HI_SIZE), (0, 0, 0, 0))
        d = ImageDraw.Draw(hi_frame)

        # 1. Inner orbit (Radius 165 px, period 36 deg -> exactly 2 pattern periods per loop)
        draw_dashed_ring(d, 165, dash_deg=20, gap_deg=16, rotation_deg=t * 72.0,
                         width=3.5, color=BLUE, alpha=160)

        # 2. Mid orbit 1 (Radius 195 px, period 30 deg -> exactly -2 pattern periods per loop)
        draw_dashed_ring(d, 195, dash_deg=14, gap_deg=16, rotation_deg=-t * 60.0,
                         width=3.0, color=BLUE_LIGHT, alpha=140)

        # 3. Mid orbit 2 (Radius 220 px, period 40 deg -> exactly 1 pattern period per loop)
        draw_dashed_ring(d, 220, dash_deg=22, gap_deg=18, rotation_deg=t * 40.0,
                         width=2.5, color=CYAN, alpha=130)

        # 4. Outer orbit (Radius 242 px, period 36 deg -> exactly -1 pattern period per loop)
        draw_dashed_ring(d, 242, dash_deg=16, gap_deg=20, rotation_deg=-t * 36.0,
                         width=2.5, color=BLUE_DEEP, alpha=110)

        # 5. Paper plane 1: Inner orbit (r=165), flies counter-clockwise completing 1 full 360 deg circle
        plane1_ang = (t * 360.0 + 60.0) % 360.0
        draw_paper_plane(d, 165, angle_deg=plane1_ang, color=BLUE, alpha=250,
                         scale=0.92, forward_ccw=True)

        # 6. Paper plane 2: Outer orbit (r=242), flies clockwise completing 1 full 360 deg circle
        plane2_ang = (-t * 360.0 + 240.0) % 360.0
        draw_paper_plane(d, 242, angle_deg=plane2_ang, color=CYAN, alpha=240,
                         scale=0.82, forward_ccw=False)

        # 7. Orbiting particles & stars (2x slower, calm ambient drift)
        draw_particles(d, t)

        # Downsample to final size with Bicubic
        frame = hi_frame.resize((SIZE, SIZE), Image.Resampling.BICUBIC)

        # 8. Composite breathing glow behind orbits (1 breathing cycle per loop)
        breathe = 0.7 + 0.3 * math.sin(t * 2 * math.pi)
        glow_frame = Image.eval(BASE_GLOW.getchannel("A"), lambda a: int(a * breathe))
        glow = BASE_GLOW.copy()
        glow.putalpha(glow_frame)

        final_frame = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        final_frame.alpha_composite(glow)
        final_frame.alpha_composite(frame)

        out.append(final_frame)

    out[0].save(f"{OUT_DIR}/proxy_orbit.webp", save_all=True,
                append_images=out[1:], duration=FRAME_MS, loop=0,
                method=4, quality=82)
    print(f"SUCCESS: proxy_orbit.webp generated ({FRAMES} frames x {FRAME_MS}ms, {SIZE}x{SIZE})")


if __name__ == "__main__":
    render_orbit()
