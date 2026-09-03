#!/usr/bin/env python3
"""Convert the complex lizard helmet bbmodel into a native Minecraft ModelPart layer."""

from __future__ import annotations

import argparse
import json
import math
import re
from pathlib import Path


HEAD_ORIGIN = (0.0, 24.0, 0.0)


def java_float(value: float) -> str:
    if abs(value) < 0.0000005:
        value = 0.0
    text = f"{value:.5f}".rstrip("0").rstrip(".")
    if "." not in text:
        text += ".0"
    return text + "F"


def java_int(value: float) -> str:
    rounded = round(value)
    if abs(value - rounded) > 0.00001:
        raise ValueError(f"Expected integer UV coordinate, got {value}")
    return str(rounded)


def identifier(name: str) -> str:
    clean = re.sub(r"[^a-zA-Z0-9_]", "_", name)
    if not clean or clean[0].isdigit():
        clean = "part_" + clean
    return clean


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


def inverse_box_uv_offset(face_name: str, uv: list[float], width: int, depth: int) -> tuple[int, int]:
    a, b, c, d = map(float, uv)
    if face_name == "east":
        return round(a), round(b - depth)
    if face_name == "north":
        return round(a - depth), round(b - depth)
    if face_name == "west":
        return round(a - depth - width), round(b - depth)
    if face_name == "south":
        return round(a - 2 * depth - width), round(b - depth)
    if face_name == "up":
        return round(c - depth), round(d)
    if face_name == "down":
        return round(c - depth - width), round(b)
    raise ValueError(f"Unknown cube face: {face_name}")


def box_geometry(element: dict, pivot: tuple[float, float, float]) -> tuple[float, float, float, int, int, int]:
    x0, y0, z0 = map(float, element["from"])
    x1, y1, z1 = map(float, element["to"])
    width = round(abs(x1 - x0))
    height = round(abs(y1 - y0))
    depth = round(abs(z1 - z0))
    return (
        min(x0, x1) - pivot[0],
        -(max(y0, y1) - pivot[1]),
        min(z0, z1) - pivot[2],
        width,
        height,
        depth,
    )


def emit_box(builder: str, element: dict, pivot: tuple[float, float, float]) -> list[str]:
    x, y, z, width, height, depth = box_geometry(element, pivot)
    geometry = ", ".join(
        (
            java_float(x),
            java_float(y),
            java_float(z),
            java_float(width),
            java_float(height),
            java_float(depth),
        )
    )
    if element.get("box_uv", True):
        uv_offset = element.get("uv_offset")
        if uv_offset is None:
            uv_offset = inverse_box_uv_offset("north", element["faces"]["north"]["uv"], width, depth)
        u, v = map(java_int, map(float, uv_offset))
        return [
            f"        {builder}.texOffs({u}, {v}).addBox({geometry}, CubeDeformation.NONE);"
        ]

    direction_names = {
        "north": "NORTH",
        "east": "EAST",
        "south": "SOUTH",
        "west": "WEST",
        "up": "UP",
        "down": "DOWN",
    }
    lines: list[str] = []
    for face_name, direction in direction_names.items():
        face = element.get("faces", {}).get(face_name)
        if not face or not face.get("uv"):
            continue
        u, v = inverse_box_uv_offset(face_name, face["uv"], width, depth)
        lines.append(
            f"        {builder}.texOffs({u}, {v}).addBox({geometry}, Set.of(Direction.{direction}));"
        )
    return lines


def generate(model: dict, class_name: str, layer_id: str) -> str:
    resolution = model.get("resolution", {})
    texture_width = int(resolution.get("width", 128))
    texture_height = int(resolution.get("height", 128))
    groups = {group["uuid"]: group for group in model.get("groups", [])}
    chains = element_group_chains(model)
    rotated_groups = {uuid: group for uuid, group in groups.items() if nonzero_rotation(group)}
    buckets: dict[str | None, list[dict]] = {None: []}

    for element in model.get("elements", []):
        rotated_ancestor = next(
            (group_uuid for group_uuid in chains.get(element["uuid"], []) if group_uuid in rotated_groups),
            None,
        )
        buckets.setdefault(rotated_ancestor, []).append(element)

    lines = [
        "package net.cgerwyu.basicrpgclasses.client.model;",
        "",
        "import java.util.Set;",
        "import net.cgerwyu.basicrpgclasses.BasicRPGClasses;",
        "import net.minecraft.client.model.geom.ModelLayerLocation;",
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeDeformation;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "import net.minecraft.core.Direction;",
        "",
        "/** Native runtime geometry generated from the verified 128x128 Blockbench helmet atlas. */",
        f"public final class {class_name} {{",
        "    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(",
        f'            BasicRPGClasses.id("{layer_id}"),',
        '            "main"',
        "    );",
        "",
        f"    private {class_name}() {{",
        "    }",
        "",
        "    public static LayerDefinition createLayer() {",
        "        MeshDefinition mesh = new MeshDefinition();",
        "        PartDefinition root = mesh.getRoot();",
        "        CubeListBuilder helmetCubes = CubeListBuilder.create();",
    ]

    for element in buckets.get(None, []):
        lines.extend(emit_box("helmetCubes", element, HEAD_ORIGIN))
    lines.append('        root.addOrReplaceChild("helmet", helmetCubes, PartPose.ZERO);')

    for group_uuid, elements in buckets.items():
        if group_uuid is None:
            continue
        group = groups[group_uuid]
        name = identifier(group["name"])
        builder = name + "Cubes"
        origin = tuple(map(float, group.get("origin", HEAD_ORIGIN)))
        lines.extend(("", f"        CubeListBuilder {builder} = CubeListBuilder.create();"))
        for element in elements:
            lines.extend(emit_box(builder, element, origin))

        x = origin[0] - HEAD_ORIGIN[0]
        y = -(origin[1] - HEAD_ORIGIN[1])
        z = origin[2] - HEAD_ORIGIN[2]
        rx, ry, rz = map(float, group.get("rotation", (0, 0, 0)))
        # Blockbench Y is upward while ModelPart Y is downward.  Reflection across
        # the XZ plane reverses X/Z rotations and preserves the Y rotation sign.
        rotations = (-math.radians(rx), math.radians(ry), -math.radians(rz))
        lines.extend(
            (
                f'        root.addOrReplaceChild("{name}", {builder}, PartPose.offsetAndRotation(',
                f"                {java_float(x)}, {java_float(y)}, {java_float(z)},",
                f"                {java_float(rotations[0])}, {java_float(rotations[1])}, {java_float(rotations[2])}",
                "        ));",
            )
        )

    lines.extend(
        (
            "",
            f"        return LayerDefinition.create(mesh, {texture_width}, {texture_height});",
            "    }",
            "}",
            "",
        )
    )
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--class-name", default="ComplexLizardHelmetModel")
    parser.add_argument("--layer-id", default="complex_lizard_helmet")
    args = parser.parse_args()

    model = json.loads(args.model.read_text(encoding="utf-8-sig"))
    source = generate(model, args.class_name, args.layer_id)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(source, encoding="utf-8", newline="\n")
    print(f"Generated {args.output} from {len(model.get('elements', []))} cubes")


if __name__ == "__main__":
    main()
