#!/usr/bin/env python3
"""Build the elevated helmet atlas from the ordinary helmet by element UUID.

Every unchanged face is copied pixel-for-pixel from the ordinary helmet.  The
single changed cube in the Gem slot is painted as a bright-green crystal, and
only genuinely new cubes in the authored Horns groups receive the matching
beige horn material.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image

from generate_complex_lizard_helmet_texture import (
    embed_texture,
    gem_pixel,
    group_paths,
    horn_pixel,
    horn_ranges,
    save_preview,
)
from transfer_boss_lizard_helmet_texture import extract_face, place_face, uv_box


def geometry(element: dict) -> tuple[float, ...]:
    return tuple(map(float, (*element["from"], *element["to"])))


def normalized_path(path: str) -> str:
    return path.lower().replace(" ", "_")


def find_gem_uuid(
    base_elements: dict[str, dict],
    boss_elements: dict[str, dict],
    boss_paths: dict[str, str],
) -> str:
    changed_common = [
        uuid
        for uuid, boss_element in boss_elements.items()
        if uuid in base_elements and geometry(base_elements[uuid]) != geometry(boss_element)
    ]
    candidates = [
        uuid
        for uuid in changed_common
        if "/gem_slot" in normalized_path(boss_paths.get(uuid, ""))
    ]
    if len(candidates) != 1:
        raise ValueError(
            "Expected exactly one changed common cube inside Gem slot, got "
            f"{len(candidates)} from changed UUIDs {changed_common}"
        )
    if changed_common != candidates:
        raise ValueError(
            "Unexpected changed ordinary cubes besides the boss gem: "
            f"{[uuid for uuid in changed_common if uuid not in candidates]}"
        )
    return candidates[0]


def paint_gem_face(
    output: Image.Image,
    template_alpha: Image.Image,
    face_name: str,
    uv: list[float],
) -> None:
    x0, y0, x1, y1 = uv_box(uv)
    width, height = x1 - x0, y1 - y0
    generated = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for y in range(height):
        for x in range(width):
            generated.putpixel((x, y), gem_pixel(face_name, x, y, width, height))
    place_face(output, template_alpha, generated, uv)


def paint_horn_face(
    output: Image.Image,
    template_alpha: Image.Image,
    element: dict,
    element_path: str,
    face_name: str,
    uv: list[float],
    ranges: dict[str, tuple[float, float]],
) -> None:
    x0, y0, x1, y1 = uv_box(uv)
    width, height = x1 - x0, y1 - y0
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
    key = f"{element['uuid']}:{face_name}:{element_path}"
    generated = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for y in range(height):
        for x in range(width):
            generated.putpixel(
                (x, y),
                horn_pixel(key, face_name, x, y, width, height, segment_t),
            )
    place_face(output, template_alpha, generated, uv)


def transfer_texture(
    base_model: dict,
    base_texture: Image.Image,
    boss_model: dict,
    boss_template: Image.Image,
) -> tuple[Image.Image, dict[str, int | str]]:
    base_texture = base_texture.convert("RGBA")
    boss_template = boss_template.convert("RGBA")
    expected_size = (
        int(boss_model.get("resolution", {}).get("width", 128)),
        int(boss_model.get("resolution", {}).get("height", 128)),
    )
    if boss_template.size != expected_size:
        raise ValueError(f"Expected {expected_size} boss template, got {boss_template.size}")
    if base_texture.size != expected_size:
        raise ValueError(f"Expected {expected_size} base texture, got {base_texture.size}")

    template_alpha = boss_template.getchannel("A")
    output = Image.new("RGBA", boss_template.size, (0, 0, 0, 0))
    base_elements = {element["uuid"]: element for element in base_model.get("elements", [])}
    boss_elements = {element["uuid"]: element for element in boss_model.get("elements", [])}
    boss_paths = group_paths(boss_model)
    ranges = horn_ranges(boss_model, boss_paths)
    gem_uuid = find_gem_uuid(base_elements, boss_elements, boss_paths)

    counts: dict[str, int | str] = {
        "ordinary_faces": 0,
        "resized_ordinary_faces": 0,
        "horn_faces": 0,
        "gem_faces": 0,
        "gem_uuid": gem_uuid,
    }
    for element in boss_model.get("elements", []):
        uuid = element["uuid"]
        element_path = boss_paths.get(uuid, "")

        if uuid == gem_uuid:
            for face_name, target_face in element.get("faces", {}).items():
                uv = target_face.get("uv")
                if uv:
                    paint_gem_face(output, template_alpha, face_name, uv)
                    counts["gem_faces"] += 1
            continue

        source = base_elements.get(uuid)
        if source is not None:
            if geometry(source) != geometry(element):
                raise ValueError(f"Changed non-gem cube {uuid} at {element_path}")
            for face_name, target_face in element.get("faces", {}).items():
                target_uv = target_face.get("uv")
                source_uv = source.get("faces", {}).get(face_name, {}).get("uv")
                if not target_uv or not source_uv:
                    continue
                source_box = uv_box(source_uv)
                target_box = uv_box(target_uv)
                source_size = (source_box[2] - source_box[0], source_box[3] - source_box[1])
                target_size = (target_box[2] - target_box[0], target_box[3] - target_box[1])
                if source_size != target_size:
                    counts["resized_ordinary_faces"] += 1
                place_face(
                    output,
                    template_alpha,
                    extract_face(base_texture, source_uv),
                    target_uv,
                )
                counts["ordinary_faces"] += 1
            continue

        if "/horns/" not in normalized_path(element_path):
            raise ValueError(f"New non-horn cube {uuid} at {element_path}")
        for face_name, target_face in element.get("faces", {}).items():
            uv = target_face.get("uv")
            if uv:
                paint_horn_face(
                    output,
                    template_alpha,
                    element,
                    element_path,
                    face_name,
                    uv,
                    ranges,
                )
                counts["horn_faces"] += 1

    output.putalpha(template_alpha)
    return output, counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-model", required=True, type=Path)
    parser.add_argument("--base-texture", required=True, type=Path)
    parser.add_argument("--boss-model", required=True, type=Path)
    parser.add_argument("--boss-template", required=True, type=Path)
    parser.add_argument("--texture-out", required=True, type=Path)
    parser.add_argument("--model-out", required=True, type=Path)
    parser.add_argument("--preview-out", required=True, type=Path)
    args = parser.parse_args()

    base_model = json.loads(args.base_model.read_text(encoding="utf-8-sig"))
    boss_model = json.loads(args.boss_model.read_text(encoding="utf-8-sig"))
    base_texture = Image.open(args.base_texture)
    boss_template = Image.open(args.boss_template)
    texture, counts = transfer_texture(base_model, base_texture, boss_model, boss_template)

    for destination in (args.texture_out, args.model_out, args.preview_out):
        destination.parent.mkdir(parents=True, exist_ok=True)
    texture.save(args.texture_out, format="PNG", optimize=True)
    save_preview(texture, args.preview_out)
    embed_texture(boss_model, args.texture_out, args.model_out)

    if texture.getchannel("A").tobytes() != boss_template.convert("RGBA").getchannel("A").tobytes():
        raise RuntimeError("Output alpha mask differs from the boss UV template")

    print(f"Texture: {args.texture_out}")
    print(f"Model: {args.model_out}")
    print(f"Preview: {args.preview_out}")
    print(f"Transferred ordinary faces: {counts['ordinary_faces']}")
    print(f"Nearest-neighbor resized ordinary faces: {counts['resized_ordinary_faces']}")
    print(f"Generated new horn faces: {counts['horn_faces']}")
    print(f"Generated bright-green gem faces: {counts['gem_faces']}")
    print(f"Boss gem UUID: {counts['gem_uuid']}")


if __name__ == "__main__":
    main()
