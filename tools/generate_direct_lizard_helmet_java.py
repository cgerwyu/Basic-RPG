#!/usr/bin/env python3
"""Generate exact fractional ModelPart geometry with per-face Blockbench UVs."""

from __future__ import annotations

import argparse
import json
import math
import re
from pathlib import Path


HEAD_ORIGIN = (0.0, 24.0, 0.0)
FACES_PER_METHOD = 100


def java_float(value: float) -> str:
    if abs(value) < 0.0000005:
        value = 0.0
    text = f"{value:.5f}".rstrip("0").rstrip(".")
    if "." not in text:
        text += ".0"
    return text + "F"


def identifier(value: str) -> str:
    result = re.sub(r"[^A-Za-z0-9_]", "", value)
    if not result or result[0].isdigit():
        raise ValueError(f"Invalid Java class name: {value}")
    return result


def element_group_chains(model: dict) -> dict[str, list[str]]:
    chains: dict[str, list[str]] = {}

    def walk(node, parents: list[str]) -> None:
        if isinstance(node, str):
            chains[node] = parents
            return
        group_uuid = node.get("uuid")
        current = parents + ([group_uuid] if group_uuid else [])
        for child in node.get("children", []):
            walk(child, current)

    outliner = model.get("outliner", [])
    if isinstance(outliner, dict):
        walk(outliner, [])
    else:
        for root in outliner:
            walk(root, [])
    return chains


def nonzero_rotation(group: dict) -> bool:
    return any(abs(float(angle)) > 0.00001 for angle in group.get("rotation", (0, 0, 0)))


def cube_geometry(
    element: dict,
    pivot: tuple[float, float, float],
) -> tuple[float, float, float, float, float, float]:
    x0, y0, z0 = map(float, element["from"])
    x1, y1, z1 = map(float, element["to"])
    return (
        min(x0, x1) - pivot[0],
        -(max(y0, y1) - pivot[1]),
        min(z0, z1) - pivot[2],
        abs(x1 - x0),
        abs(y1 - y0),
        abs(z1 - z0),
    )


def generate(model: dict, class_name: str) -> str:
    class_name = identifier(class_name)
    resolution = model.get("resolution", {})
    texture_width = float(resolution.get("width", 64))
    texture_height = float(resolution.get("height", 64))
    direction_names = {
        "north": "NORTH",
        "east": "EAST",
        "south": "SOUTH",
        "west": "WEST",
        "up": "UP",
        "down": "DOWN",
    }
    groups = {group["uuid"]: group for group in model.get("groups", [])}
    chains = element_group_chains(model)
    rotated_groups = {
        uuid: group for uuid, group in groups.items() if nonzero_rotation(group)
    }
    buckets: dict[str | None, list[dict]] = {None: []}
    for element in model.get("elements", []):
        rotated_ancestor = next(
            (
                group_uuid
                for group_uuid in chains.get(element["uuid"], [])
                if group_uuid in rotated_groups
            ),
            None,
        )
        buckets.setdefault(rotated_ancestor, []).append(element)

    bucket_calls: dict[str | None, list[str]] = {}
    for group_uuid, elements in buckets.items():
        pivot = HEAD_ORIGIN
        if group_uuid is not None:
            pivot = tuple(map(float, groups[group_uuid].get("origin", HEAD_ORIGIN)))
        calls: list[str] = []
        for element in elements:
            geometry = ", ".join(
                java_float(value) for value in cube_geometry(element, pivot)
            )
            for face_name, direction in direction_names.items():
                face = element.get("faces", {}).get(face_name)
                uv = face.get("uv") if face else None
                if not uv:
                    continue
                u0, v0, u1, v1 = map(float, uv)
                if abs(u1 - u0) < 0.000001 or abs(v1 - v0) < 0.000001:
                    continue
                calls.append(
                    "        addFace(cubes, Direction."
                    + direction
                    + ", "
                    + geometry
                    + ", "
                    + ", ".join(java_float(value) for value in (u0, v0, u1, v1))
                    + ");"
                )
        bucket_calls[group_uuid] = calls

    bucket_chunks = {
        group_uuid: [
            calls[index:index + FACES_PER_METHOD]
            for index in range(0, len(calls), FACES_PER_METHOD)
        ]
        for group_uuid, calls in bucket_calls.items()
    }
    rotated_group_uuids = [uuid for uuid in buckets if uuid is not None]
    lines = [
        "package net.cgerwyu.basicrpgclasses.client.model;",
        "",
        "import java.util.ArrayList;",
        "import java.util.HashMap;",
        "import java.util.List;",
        "import java.util.Map;",
        "import java.util.Set;",
        "import net.minecraft.client.model.geom.ModelPart;",
        "import net.minecraft.core.Direction;",
        "",
        "/** Exact runtime geometry generated from the fractional Blockbench model. */",
        f"public final class {class_name} {{",
        f"    private static final float TEXTURE_WIDTH = {java_float(texture_width)};",
        f"    private static final float TEXTURE_HEIGHT = {java_float(texture_height)};",
        "",
        f"    private {class_name}() {{",
        "    }",
        "",
        "    public static ModelPart createModel() {",
        "        List<ModelPart.Cube> rootCubes = new ArrayList<>();",
    ]
    for index in range(len(bucket_chunks.get(None, []))):
        lines.append(f"        addRootFaces{index}(rootCubes);")
    lines.append("        Map<String, ModelPart> children = new HashMap<>();")
    for part_index, group_uuid in enumerate(rotated_group_uuids):
        group = groups[group_uuid]
        origin = tuple(map(float, group.get("origin", HEAD_ORIGIN)))
        x = origin[0] - HEAD_ORIGIN[0]
        y = -(origin[1] - HEAD_ORIGIN[1])
        z = origin[2] - HEAD_ORIGIN[2]
        rx, ry, rz = map(float, group.get("rotation", (0, 0, 0)))
        rotations = (-math.radians(rx), math.radians(ry), -math.radians(rz))
        lines.append(f"        List<ModelPart.Cube> part{part_index}Cubes = new ArrayList<>();")
        for chunk_index in range(len(bucket_chunks[group_uuid])):
            lines.append(
                f"        addPart{part_index}Faces{chunk_index}(part{part_index}Cubes);"
            )
        lines.extend(
            (
                f"        ModelPart part{part_index} = new ModelPart(part{part_index}Cubes, Map.of());",
                f"        part{part_index}.setPos({java_float(x)}, {java_float(y)}, {java_float(z)});",
                f"        part{part_index}.setRotation({java_float(rotations[0])}, {java_float(rotations[1])}, {java_float(rotations[2])});",
                f'        children.put("rotated_part_{part_index}", part{part_index});',
            )
        )
    lines.extend(("        return new ModelPart(rootCubes, children);", "    }"))

    for index, chunk in enumerate(bucket_chunks.get(None, [])):
        lines.extend(
            (
                "",
                f"    private static void addRootFaces{index}(List<ModelPart.Cube> cubes) {{",
                *chunk,
                "    }",
            )
        )
    for part_index, group_uuid in enumerate(rotated_group_uuids):
        for chunk_index, chunk in enumerate(bucket_chunks[group_uuid]):
            lines.extend(
                (
                    "",
                    f"    private static void addPart{part_index}Faces{chunk_index}(List<ModelPart.Cube> cubes) {{",
                    *chunk,
                    "    }",
                )
            )

    lines.extend((
        "",
        "    private static void addFace(",
        "            List<ModelPart.Cube> cubes,",
        "            Direction direction,",
        "            float x, float y, float z,",
        "            float width, float height, float depth,",
        "            float u0, float v0, float u1, float v1",
        "    ) {",
        "        ModelPart.Cube cube = new ModelPart.Cube(",
        "                0, 0, x, y, z, width, height, depth,",
        "                0.0F, 0.0F, 0.0F, false,",
        "                TEXTURE_WIDTH, TEXTURE_HEIGHT, Set.of(direction)",
        "        );",
        "        ModelPart.Polygon polygon = cube.polygons[0];",
        "        ModelPart.Vertex[] source = polygon.vertices();",
        "        ModelPart.Vertex[] remapped = new ModelPart.Vertex[]{",
        "                source[0].remap(u1 / TEXTURE_WIDTH, v0 / TEXTURE_HEIGHT),",
        "                source[1].remap(u0 / TEXTURE_WIDTH, v0 / TEXTURE_HEIGHT),",
        "                source[2].remap(u0 / TEXTURE_WIDTH, v1 / TEXTURE_HEIGHT),",
        "                source[3].remap(u1 / TEXTURE_WIDTH, v1 / TEXTURE_HEIGHT)",
        "        };",
        "        cube.polygons[0] = new ModelPart.Polygon(remapped, polygon.normal());",
        "        cubes.add(cube);",
        "    }",
        "}",
        "",
    ))
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--class-name", default="ComplexLizardHelmetModel")
    args = parser.parse_args()

    model = json.loads(args.model.read_text(encoding="utf-8-sig"))
    output = generate(model, args.class_name)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(output, encoding="utf-8", newline="\n")
    print(f"Generated {args.output} from {len(model.get('elements', []))} cubes")


if __name__ == "__main__":
    main()
