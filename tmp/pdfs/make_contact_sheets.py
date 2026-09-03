from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parent
render_name = sys.argv[1] if len(sys.argv) > 1 else "bestiary_render"
output_prefix = sys.argv[2] if len(sys.argv) > 2 else "contact"
pages = sorted((root / render_name).glob("page-*.png"))
thumb_w, thumb_h = 260, 368
label_h = 24
cols, rows = 4, 3
per_sheet = cols * rows
font = ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf", 15)

for sheet_index in range((len(pages) + per_sheet - 1) // per_sheet):
    batch = pages[sheet_index * per_sheet : (sheet_index + 1) * per_sheet]
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + label_h)), "#2b2b2b")
    draw = ImageDraw.Draw(sheet)
    for local_index, path in enumerate(batch):
        with Image.open(path) as source:
            page = source.convert("RGB")
            page.thumbnail((thumb_w - 8, thumb_h - 8), Image.Resampling.LANCZOS)
            col = local_index % cols
            row = local_index // cols
            x = col * thumb_w + (thumb_w - page.width) // 2
            y = row * (thumb_h + label_h) + 4
            sheet.paste(page, (x, y))
            page_number = sheet_index * per_sheet + local_index + 1
            label = f"Page {page_number}"
            draw.text((col * thumb_w + 8, row * (thumb_h + label_h) + thumb_h + 2), label, fill="white", font=font)
    sheet.save(root / f"{output_prefix}-{sheet_index + 1}.png")
