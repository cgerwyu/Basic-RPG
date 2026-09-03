#!/usr/bin/env python3
"""Generate a pixel-perfect texture for the complex lizard helmet UV atlas.

The Blockbench template is used as the authoritative alpha mask.  Colors and
small-scale patterns are painted per UV face using the element's semantic group
and face direction from the bbmodel file.  The source model and source texture
are never overwritten.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import io
import json
from pathlib import Path

from PIL import Image


RGBA = tuple[int, int, int, int]


LEATHER: list[RGBA] = [
    (7, 20, 14, 255),       # silhouette/recess
    (18, 38, 23, 255),      # forest shadow
    (31, 57, 29, 255),      # moss shadow
    (49, 76, 35, 255),      # olive green
    (70, 94, 40, 255),      # lizard-skin midtone
    (94, 112, 49, 255),     # dry olive highlight
    (127, 132, 65, 255),    # restrained yellow-green shine
]

OCHRE: list[RGBA] = [
    (57, 43, 25, 255),
    (83, 62, 31, 255),
    (111, 84, 39, 255),
    (139, 111, 56, 255),
]

BONE: list[RGBA] = [
    (63, 43, 25, 255),
    (99, 70, 39, 255),
    (143, 108, 65, 255),
    (187, 153, 99, 255),
    (220, 194, 143, 255),
    (239, 220, 174, 255),
]

EMERALD: list[RGBA] = [
    (3, 20, 15, 255),
    (6, 42, 27, 255),
    (10, 67, 39, 255),
    (19, 103, 55, 255),
    (47, 144, 75, 255),
    (112, 192, 112, 255),
]

AMETHYST: list[RGBA] = [
    (28, 6, 39, 255),
    (54, 10, 74, 255),
    (82, 18, 111, 255),
    (119, 29, 157, 255),
    (163, 56, 201, 255),
    (207, 112, 232, 255),
    (239, 197, 247, 255),
]

BRIGHT_GREEN_CRYSTAL: list[RGBA] = [
    (2, 35, 16, 255),
    (3, 67, 26, 255),
    (6, 108, 37, 255),
    (14, 157, 51, 255),
    (36, 205, 69, 255),
    (91, 238, 113, 255),
    (190, 255, 199, 255),
]

FACE_SHADE = {
    "up": 2,
    "north": 1,
    "east": 0,
    "west": -1,
    "south": -1,
    "down": -2,
}


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def noise_byte(key: str, x: int, y: int) -> int:
    digest = hashlib.blake2b(
        f"{key}:{x}:{y}".encode("utf-8"), digest_size=2
    ).digest()
    return digest[0]


def group_paths(model: dict) -> dict[str, str]:
    names = {group["uuid"]: group["name"] for group in model.get("groups", [])}
    result: dict[str, str] = {}

    def walk(node, parent: str) -> None:
        if isinstance(node, str):
            result[node] = parent
            return
        name = names.get(node.get("uuid"), "")
        current = "/".join(filter(None, (parent, name)))
        for child in node.get("children", []):
            walk(child, current)

    outliner = model.get("outliner", [])
    if isinstance(outliner, dict):
        walk(outliner, "")
    else:
        for root in outliner:
            walk(root, "")
    return result


def horn_ranges(model: dict, paths: dict[str, str]) -> dict[str, tuple[float, float]]:
    centers: dict[str, list[float]] = {}
    for element in model.get("elements", []):
        path = paths.get(element["uuid"], "")
        if "/horns/" not in path.lower():
            continue
        horn_name = next(
            (
                part.lower().replace(" ", "_")
                for part in path.split("/")
                if part.lower().replace(" ", "_").startswith(("left_horn", "right_horn"))
            ),
            "horn",
        )
        center_y = (float(element["from"][1]) + float(element["to"][1])) / 2.0
        centers.setdefault(horn_name, []).append(center_y)
    return {
        name: (min(values), max(values))
        for name, values in centers.items()
    }


def material_for(path: str) -> str:
    normalized = path.lower().replace(" ", "_")
    if "/horns/" in normalized:
        return "horn"
    if normalized.endswith("/gem") or "/gem/" in normalized:
        return "gem"
    if normalized.endswith("/gem_slot") or "/gem_slot/" in normalized:
        return "leather"
    return "leather"


def leather_pixel(
    key: str,
    face: str,
    x: int,
    y: int,
    width: int,
    height: int,
) -> RGBA:
    shade = FACE_SHADE.get(face, 0)
    index = 4 + shade

    # Small overlapping-scale rhythm: a quiet highlighted crown and a dark seam.
    scale_row = y % 3
    scale_col = (x + (y // 3) * 2) % 5
    if scale_row == 0 and scale_col in (1, 2):
        index += 1
    elif scale_row == 2 and scale_col in (0, 1, 2, 3):
        index -= 1

    # Make readable constructed edges without black-framing one-pixel strips.
    if width >= 3 and height >= 3:
        if x == 0 or x == width - 1 or y == height - 1:
            index -= 1
        elif y == 0:
            index += 1

    variation = noise_byte(key, x, y)
    if variation < 18:
        index -= 1
    elif variation > 242:
        index += 1

    # Sparse dry ochre scales connect the old green set to the lizard reference.
    if variation in range(90, 101) and width > 1 and height > 1:
        ochre_index = clamp(2 + shade // 2, 0, len(OCHRE) - 1)
        return OCHRE[ochre_index]

    return LEATHER[clamp(index, 0, len(LEATHER) - 1)]


def horn_pixel(
    key: str,
    face: str,
    x: int,
    y: int,
    width: int,
    height: int,
    segment_t: float,
) -> RGBA:
    shade = FACE_SHADE.get(face, 0)
    # Beige through the body, with darker roots and distinctly brown tips.
    if segment_t < 0.18:
        index = 2
    elif segment_t > 0.78:
        index = clamp(4 - round((segment_t - 0.78) * 12), 0, 4)
    else:
        index = 4
    index += shade // 2

    # Thin natural growth bands.
    if ((y + round(segment_t * 11)) % 5 == 4) or noise_byte(key, x, y) < 12:
        index -= 1
    if width >= 3 and height >= 3 and (x == 0 or x == width - 1 or y == height - 1):
        index -= 1
    if y == 0 and height >= 2:
        index += 1
    return BONE[clamp(index, 0, len(BONE) - 1)]


def socket_pixel(face: str, x: int, y: int, width: int, height: int) -> RGBA:
    shade = FACE_SHADE.get(face, 0)
    index = 2 + shade // 2
    if width >= 3 and height >= 3:
        if x == 0 or y == height - 1 or x == width - 1:
            index -= 1
        elif y == 0:
            index += 1
    return EMERALD[clamp(index, 0, len(EMERALD) - 1)]


def gem_pixel(face: str, x: int, y: int, width: int, height: int) -> RGBA:
    shade = FACE_SHADE.get(face, 0)
    index = 4 + shade // 2

    # Dark cut facets around the perimeter and a bright upper-left crystal face.
    if width >= 3 and height >= 3:
        if x == 0 or y == height - 1 or x == width - 1:
            index -= 2
        elif y == 0:
            index += 1
    if x <= max(0, width // 3) and y <= max(0, height // 3) and (x + y) % 2 == 0:
        return (219, 244, 207, 255)
    if (x - y) % 5 == 0:
        index += 1
    return BRIGHT_GREEN_CRYSTAL[clamp(index, 0, len(BRIGHT_GREEN_CRYSTAL) - 1)]


def paint_texture(model: dict, template: Image.Image) -> Image.Image:
    resolution = model.get("resolution", {})
    expected_size = (int(resolution.get("width", 128)), int(resolution.get("height", 128)))
    if template.size != expected_size:
        raise ValueError(f"Expected {expected_size} UV template, got {template.size}")
    template = template.convert("RGBA")
    output = Image.new("RGBA", template.size, (0, 0, 0, 0))
    template_alpha = template.getchannel("A")
    paths = group_paths(model)
    ranges = horn_ranges(model, paths)

    for element in model.get("elements", []):
        element_path = paths.get(element["uuid"], "")
        material = material_for(element_path)
        center_y = (float(element["from"][1]) + float(element["to"][1])) / 2.0
        horn_name = next(
            (
                part.lower().replace(" ", "_")
                for part in element_path.split("/")
                if part.lower().replace(" ", "_").startswith(("left_horn", "right_horn"))
            ),
            "horn",
        )
        low_y, high_y = ranges.get(horn_name, (center_y, center_y + 1.0))
        segment_t = (center_y - low_y) / max(1e-6, high_y - low_y)

        for face_name, face in element.get("faces", {}).items():
            uv = face.get("uv")
            if not uv:
                continue
            x0, x1 = sorted((round(float(uv[0])), round(float(uv[2]))))
            y0, y1 = sorted((round(float(uv[1])), round(float(uv[3]))))
            width, height = x1 - x0, y1 - y0
            if width <= 0 or height <= 0:
                continue
            key = f"{element['uuid']}:{face_name}:{element_path}"
            for local_y, atlas_y in enumerate(range(y0, y1)):
                for local_x, atlas_x in enumerate(range(x0, x1)):
                    if template_alpha.getpixel((atlas_x, atlas_y)) == 0:
                        continue
                    if material == "horn":
                        color = horn_pixel(
                            key, face_name, local_x, local_y, width, height, segment_t
                        )
                    elif material == "gem":
                        color = gem_pixel(face_name, local_x, local_y, width, height)
                    elif material == "socket":
                        color = socket_pixel(face_name, local_x, local_y, width, height)
                    else:
                        color = leather_pixel(
                            key, face_name, local_x, local_y, width, height
                        )
                    output.putpixel((atlas_x, atlas_y), color)

    # Enforce the template as an exact immutable alpha mask.
    output.putalpha(template_alpha)
    return output


def save_preview(texture: Image.Image, destination: Path) -> None:
    scale = 8
    checker = Image.new("RGBA", texture.size, (28, 30, 29, 255))
    for y in range(texture.height):
        for x in range(texture.width):
            if ((x // 4) + (y // 4)) % 2:
                checker.putpixel((x, y), (42, 45, 43, 255))
    checker.alpha_composite(texture)
    checker.resize((texture.width * scale, texture.height * scale), Image.Resampling.NEAREST).save(destination)


def embed_texture(model: dict, texture_path: Path, model_path: Path) -> None:
    payload = texture_path.read_bytes()
    with Image.open(texture_path) as texture_image:
        width, height = texture_image.size
    data_url = "data:image/png;base64," + base64.b64encode(payload).decode("ascii")
    textures = model.setdefault("textures", [])
    if not textures:
        textures.append({})
    texture = textures[0]
    texture.update(
        {
            "name": texture_path.name,
            "relative_path": texture_path.name,
            "source": data_url,
            "width": width,
            "height": height,
            "uv_width": width,
            "uv_height": height,
            "internal": True,
            "saved": True,
        }
    )
    model_path.write_text(
        json.dumps(model, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--template", required=True, type=Path)
    parser.add_argument("--texture-out", required=True, type=Path)
    parser.add_argument("--model-out", required=True, type=Path)
    parser.add_argument("--preview-out", required=True, type=Path)
    args = parser.parse_args()

    model = json.loads(args.model.read_text(encoding="utf-8-sig"))
    template = Image.open(args.template)
    texture = paint_texture(model, template)

    for path in (args.texture_out, args.model_out, args.preview_out):
        path.parent.mkdir(parents=True, exist_ok=True)
    texture.save(args.texture_out, format="PNG", optimize=True)
    save_preview(texture, args.preview_out)
    embed_texture(model, args.texture_out, args.model_out)

    with Image.open(args.texture_out).convert("RGBA") as saved:
        template_alpha = template.convert("RGBA").getchannel("A")
        saved_alpha = saved.getchannel("A")
        if saved_alpha.tobytes() != template_alpha.tobytes():
            raise RuntimeError("Output alpha mask differs from the Blockbench template")

    print(f"Texture: {args.texture_out}")
    print(f"Model: {args.model_out}")
    print(f"Preview: {args.preview_out}")


if __name__ == "__main__":
    main()
