#!/usr/bin/env python3
"""Transfer the ordinary lizard helmet texture onto the boss helmet UV atlas.

Unchanged faces are copied by UUID. New horn faces reuse the closest matching
ordinary horn face, while only the newly added Gem group is painted anew.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image

from generate_complex_lizard_helmet_texture import embed_texture, gem_pixel, group_paths, save_preview


def uv_box(uv: list[float]) -> tuple[int, int, int, int]:
    x0, x1 = sorted((round(float(uv[0])), round(float(uv[2]))))
    y0, y1 = sorted((round(float(uv[1])), round(float(uv[3]))))
    return x0, y0, x1, y1


def extract_face(texture: Image.Image, uv: list[float]) -> Image.Image:
    face = texture.crop(uv_box(uv))
    if float(uv[2]) < float(uv[0]):
        face = face.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    if float(uv[3]) < float(uv[1]):
        face = face.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    return face


def place_face(
    output: Image.Image,
    template_alpha: Image.Image,
    face: Image.Image,
    uv: list[float],
) -> None:
    x0, y0, x1, y1 = uv_box(uv)
    width, height = x1 - x0, y1 - y0
    if width <= 0 or height <= 0:
        return
    if face.size != (width, height):
        face = face.resize((width, height), Image.Resampling.NEAREST)
    if float(uv[2]) < float(uv[0]):
        face = face.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    if float(uv[3]) < float(uv[1]):
        face = face.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    output.paste(face, (x0, y0), template_alpha.crop((x0, y0, x1, y1)))


def dimensions(element: dict) -> tuple[float, float, float]:
    return tuple(
        abs(float(element["to"][axis]) - float(element["from"][axis]))
        for axis in range(3)
    )


def center_y(element: dict) -> float:
    return (float(element["from"][1]) + float(element["to"][1])) / 2.0


def horn_role(path: str) -> str | None:
    for part in path.lower().split("/"):
        if part.startswith(("left_horn_top", "left_horn_bottom", "right_horn_top", "right_horn_bottom")):
            return part.removesuffix("_boss")
    return None


def normalized_positions(elements: list[dict]) -> dict[str, float]:
    values = [center_y(element) for element in elements]
    low, high = min(values), max(values)
    span = max(1e-6, high - low)
    return {element["uuid"]: (center_y(element) - low) / span for element in elements}


def closest_horn_source(
    target: dict,
    target_t: float,
    candidates: list[dict],
    source_positions: dict[str, float],
) -> dict:
    target_dimensions = dimensions(target)

    def score(candidate: dict) -> tuple[float, float]:
        candidate_dimensions = dimensions(candidate)
        size_difference = sum(
            abs(target_dimensions[axis] - candidate_dimensions[axis])
            for axis in range(3)
        )
        position_difference = abs(target_t - source_positions[candidate["uuid"]])
        return size_difference, position_difference

    return min(candidates, key=score)


def paint_generated_gem(
    output: Image.Image,
    template_alpha: Image.Image,
    element: dict,
) -> None:
    for face_name, face_data in element.get("faces", {}).items():
        uv = face_data.get("uv")
        if not uv:
            continue
        x0, y0, x1, y1 = uv_box(uv)
        width, height = x1 - x0, y1 - y0
        generated = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        for y in range(height):
            for x in range(width):
                generated.putpixel((x, y), gem_pixel(face_name, x, y, width, height))
        place_face(output, template_alpha, generated, uv)


def transfer_texture(
    base_model: dict,
    base_texture: Image.Image,
    boss_model: dict,
    boss_template: Image.Image,
) -> tuple[Image.Image, dict[str, int]]:
    base_texture = base_texture.convert("RGBA")
    boss_template = boss_template.convert("RGBA")
    template_alpha = boss_template.getchannel("A")
    output = Image.new("RGBA", boss_template.size, (0, 0, 0, 0))

    base_elements = {element["uuid"]: element for element in base_model.get("elements", [])}
    base_paths = group_paths(base_model)
    boss_paths = group_paths(boss_model)

    base_horns: dict[str, list[dict]] = {}
    for element in base_model.get("elements", []):
        role = horn_role(base_paths.get(element["uuid"], ""))
        if role:
            base_horns.setdefault(role, []).append(element)
    base_horn_positions = {
        role: normalized_positions(elements)
        for role, elements in base_horns.items()
    }

    boss_horns: dict[str, list[dict]] = {}
    for element in boss_model.get("elements", []):
        role = horn_role(boss_paths.get(element["uuid"], ""))
        if role:
            boss_horns.setdefault(role, []).append(element)
    boss_horn_positions = {
        role: normalized_positions(elements)
        for role, elements in boss_horns.items()
    }

    counts = {"ordinary_faces": 0, "horn_faces": 0, "gem_faces": 0}
    for element in boss_model.get("elements", []):
        path = boss_paths.get(element["uuid"], "")
        source = base_elements.get(element["uuid"])
        if source is not None:
            for face_name, target_face in element.get("faces", {}).items():
                target_uv = target_face.get("uv")
                source_uv = source.get("faces", {}).get(face_name, {}).get("uv")
                if not target_uv or not source_uv:
                    continue
                place_face(output, template_alpha, extract_face(base_texture, source_uv), target_uv)
                counts["ordinary_faces"] += 1
            continue

        role = horn_role(path)
        if role:
            source = closest_horn_source(
                element,
                boss_horn_positions[role][element["uuid"]],
                base_horns[role],
                base_horn_positions[role],
            )
            for face_name, target_face in element.get("faces", {}).items():
                target_uv = target_face.get("uv")
                source_uv = source.get("faces", {}).get(face_name, {}).get("uv")
                if not target_uv or not source_uv:
                    continue
                place_face(output, template_alpha, extract_face(base_texture, source_uv), target_uv)
                counts["horn_faces"] += 1
            continue

        if path.lower().endswith("/gem"):
            paint_generated_gem(output, template_alpha, element)
            counts["gem_faces"] += len(element.get("faces", {}))
            continue

        raise ValueError(f"Unmatched non-horn, non-gem element: {element['uuid']} at {path}")

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
    print(f"Transferred horn faces: {counts['horn_faces']}")
    print(f"Generated gem faces: {counts['gem_faces']}")


if __name__ == "__main__":
    main()
