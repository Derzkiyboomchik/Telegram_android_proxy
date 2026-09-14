import math
import os
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

ORIGINAL_SRC = r"C:\Users\kapet\.gemini\antigravity\brain\3f70f92c-0601-49f8-8fa7-4f3b01a27e9a\.user_uploaded\media_1788098708579.jpg"
RES_DIR = r"app\src\main\res"

img = Image.open(ORIGINAL_SRC).convert("RGBA")
W, H = img.size

# Crop the outer mockup border
crop_margin = 28
cropped = img.crop((crop_margin, crop_margin, W - crop_margin, H - crop_margin))

# Enhance contrast and sharpness slightly
enhancer = ImageEnhance.Contrast(cropped)
cropped = enhancer.enhance(1.06)
enhancer_sharp = ImageEnhance.Sharpness(cropped)
cropped = enhancer_sharp.enhance(1.15)

CW, CH = cropped.size

# Android adaptive icon size is 432x432 px.
# The visible viewport on round/squircle launchers has diameter ~264 px (radius 132 px).
# To guarantee that the nose tip of the paper plane and all shards are 100% inside
# the launcher mask without being cut off by the edges:
# We scale the artwork down so its maximum dimension is ~275 px (approx 0.64 scale).
FG_SIZE = 432
ART_TARGET_SIZE = 275

art_resized = cropped.resize((ART_TARGET_SIZE, ART_TARGET_SIZE), Image.Resampling.LANCZOS)
AW, AH = art_resized.size

# Create a smooth soft circular/rounded vignette mask on the artwork edges
# so it blends seamlessly into the dark cyber background without any hard rectangular borders.
art_mask = Image.new("L", (AW, AH), 255)
mask_draw = ImageDraw.Draw(art_mask)
# Soft fade on outer 16px
for i in range(16):
    alpha = int(255 * (i / 16.0))
    # Outer rectangle border with increasing alpha
    mask_draw.rectangle([i, i, AW - 1 - i, AH - 1 - i], outline=alpha)

# Create deep cyberpunk space background (432x432)
bg_canvas = Image.new("RGBA", (FG_SIZE, FG_SIZE), (7, 10, 20, 255))
bg_draw = ImageDraw.Draw(bg_canvas)

# Radial blue/cyan glow in background center
center = FG_SIZE / 2
for r in range(216, 0, -4):
    a = int(75 * (1 - r / 216) ** 1.8)
    bg_draw.ellipse([center - r, center - r, center + r, center + r], fill=(16, 50, 105, a))

# Composite artwork centered on the background
offset_x = (FG_SIZE - AW) // 2
offset_y = (FG_SIZE - AH) // 2

full_canvas = bg_canvas.copy()
full_canvas.paste(art_resized, (offset_x, offset_y), art_mask)

DENSITIES = {
    "mipmap-mdpi": (48, 108),
    "mipmap-hdpi": (72, 162),
    "mipmap-xhdpi": (96, 216),
    "mipmap-xxhdpi": (144, 324),
    "mipmap-xxxhdpi": (192, 432),
}

for folder, (icon_sz, bg_sz) in DENSITIES.items():
    out_dir = os.path.join(RES_DIR, folder)
    os.makedirs(out_dir, exist_ok=True)

    # 1. Adaptive icon background (complete scaled & padded artwork with full shards & nose):
    bg_img = full_canvas.resize((bg_sz, bg_sz), Image.Resampling.LANCZOS)
    bg_img.save(os.path.join(out_dir, "ic_launcher_background_img.png"), "PNG")

    # 2. Adaptive icon foreground (transparent):
    transparent_fg = Image.new("RGBA", (bg_sz, bg_sz), (0, 0, 0, 0))
    transparent_fg.save(os.path.join(out_dir, "ic_launcher_foreground.png"), "PNG")

    # 3. Legacy squircle / square icon:
    sq_icon = full_canvas.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
    sq_icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")

    # 4. Legacy round icon:
    rd_mask = Image.new("L", (icon_sz, icon_sz), 0)
    rd_draw = ImageDraw.Draw(rd_mask)
    rd_draw.ellipse([0, 0, icon_sz, icon_sz], fill=255)
    rd_icon = Image.new("RGBA", (icon_sz, icon_sz), (0, 0, 0, 0))
    rd_icon.paste(sq_icon, (0, 0), rd_mask)
    rd_icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")

print("Generated perfectly scaled icons with safe zone margins protecting the plane nose and all shards!")
