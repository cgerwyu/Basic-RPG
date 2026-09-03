import base64
import io
import json
import shutil
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


MODEL = Path(r"C:\Users\cdald\Desktop\references\BOSSES\Tarr-Gorak the Firstfang.bbmodel")
BASE_TEXTURE = Path(r"C:\Users\cdald\Desktop\references\BOSSES\texture.png")
EFFECT_TEXTURE = Path(r"C:\Users\cdald\Desktop\references\BOSSES\crystal_effect_emissive.png")
MODEL_BACKUP = Path(r"C:\Users\cdald\Desktop\references\BOSSES\Tarr-Gorak the Firstfang.before-crystal-aura.bbmodel")
TEXTURE_BACKUP = Path(r"C:\Users\cdald\Desktop\references\BOSSES\texture.before-crystal-aura.png")


def uid():
    return str(uuid.uuid4())


def outliner_find(nodes, wanted_uuid):
    for node in nodes:
        if isinstance(node, dict):
            if node.get("uuid") == wanted_uuid:
                return node
            result = outliner_find(node.get("children", []), wanted_uuid)
            if result:
                return result
    return None


def outliner_without(nodes, removed):
    result = []
    for node in nodes:
        if isinstance(node, dict):
            if node.get("uuid") in removed:
                continue
            node["children"] = outliner_without(node.get("children", []), removed)
        result.append(node)
    return result


def make_group(name, origin, color):
    return {
        "name": name,
        "uuid": uid(),
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "_static": {"properties": {}, "temp_data": {}},
        "origin": origin,
        "rotation": [0, 0, 0],
        "color": color,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
        "primary_selected": False,
    }


def make_cube(name, start, end, origin, uv, color):
    cube_uuid = uid()
    faces = {
        face: {"uv": list(uv), "texture": 1}
        for face in ("north", "east", "south", "west", "up", "down")
    }
    return {
        "name": name,
        "box_uv": False,
        "render_order": "default",
        "locked": False,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": start,
        "to": end,
        "autouv": 0,
        "color": color,
        "origin": origin,
        "uv_offset": [uv[0], uv[1]],
        "faces": faces,
        "type": "cube",
        "uuid": cube_uuid,
    }


def keyframe(channel, time, values):
    return {
        "channel": channel,
        "data_points": [{"x": str(values[0]), "y": str(values[1]), "z": str(values[2])}],
        "uuid": uid(),
        "time": time,
        "color": -1,
        "interpolation": "catmullrom",
    }


with MODEL.open("r", encoding="utf-8") as handle:
    model = json.load(handle)

if not MODEL_BACKUP.exists():
    shutil.copy2(MODEL, MODEL_BACKUP)
if not TEXTURE_BACKUP.exists():
    shutil.copy2(BASE_TEXTURE, TEXTURE_BACKUP)

# Idempotently remove an older version of this effect.
old_group_ids = {
    group["uuid"] for group in model.get("groups", [])
    if group.get("name", "").startswith("crystal_fx_")
}
old_element_ids = {
    element["uuid"] for element in model.get("elements", [])
    if element.get("name", "").startswith("crystal_fx_")
}
model["groups"] = [g for g in model.get("groups", []) if g.get("uuid") not in old_group_ids]
model["elements"] = [e for e in model.get("elements", []) if e.get("uuid") not in old_element_ids]
model["outliner"] = outliner_without(model.get("outliner", []), old_group_ids | old_element_ids)
model["textures"] = [t for t in model.get("textures", []) if t.get("name") != EFFECT_TEXTURE.name]
model["animations"] = [a for a in model.get("animations", []) if a.get("name") != "crystal_aura"]

# Create a full atlas copy so the original gem UVs can be reused exactly.
effect = Image.open(BASE_TEXTURE).convert("RGBA")
pixels = effect.load()
for y in range(effect.height):
    for x in range(effect.width):
        r, g, b, a = pixels[x, y]
        if a and b > 65 and r > 45 and b > g * 1.35 and r > g * 1.15:
            pixels[x, y] = (min(255, int(r * 1.35 + 35)), min(105, int(g * 0.65 + 10)), 255, 172)

# The rectangle x=105..152, y=0..47 is unused in the base atlas.
draw = ImageDraw.Draw(effect, "RGBA")
swatches = {
    "void": (105, 0, 113, 8, (12, 2, 22, 205)),
    "deep": (113, 0, 121, 8, (48, 5, 78, 178)),
    "violet": (121, 0, 129, 8, (92, 12, 145, 154)),
    "purple": (129, 0, 137, 8, (150, 24, 220, 126)),
    "glow": (137, 0, 145, 8, (235, 55, 255, 92)),
}
for _, (x0, y0, x1, y1, color) in swatches.items():
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=color)
    draw.rectangle((x0 + 1, y0 + 1, x1 - 2, y1 - 2), fill=(color[0], color[1], color[2], max(40, color[3] - 35)))

effect.save(EFFECT_TEXTURE, optimize=True)
buffer = io.BytesIO()
effect.save(buffer, format="PNG", optimize=True)
effect_source = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")

template_texture = dict(model["textures"][0])
template_texture.update({
    "name": EFFECT_TEXTURE.name,
    "relative_path": EFFECT_TEXTURE.name,
    "id": "1",
    "render_mode": "emissive",
    "render_sides": "double",
    "use_as_default": False,
    "internal": True,
    "saved": True,
    "visible": True,
    "uuid": uid(),
    "source": effect_source,
})
model["textures"].append(template_texture)

# Reassign only the crystal itself to the emissive atlas copy.
group_by_name = {group["name"]: group for group in model["groups"]}
gem_group = group_by_name["Gem"]
gem_node = outliner_find(model["outliner"], gem_group["uuid"])
element_by_uuid = {element["uuid"]: element for element in model["elements"]}
gem_cube_count = 0
for child in gem_node.get("children", []):
    if isinstance(child, str) and child in element_by_uuid:
        for face in element_by_uuid[child].get("faces", {}).values():
            if "texture" in face:
                face["texture"] = 1
        gem_cube_count += 1

# Add a parent FX bone and three separately animated smoke wisps.
fx_root = make_group("crystal_fx_aura", [-0.5, 75.0, -70.5], 5)
wisp_a = make_group("crystal_fx_wisp_left", [-1.45, 76.0, -71.0], 5)
wisp_b = make_group("crystal_fx_wisp_right", [0.55, 76.15, -70.3], 6)
wisp_c = make_group("crystal_fx_wisp_high", [-0.15, 77.5, -71.4], 7)
model["groups"].extend([fx_root, wisp_a, wisp_b, wisp_c])

uv = {key: values[:4] for key, values in swatches.items()}
cubes = [
    make_cube("crystal_fx_glow_top", [-3.15, 75.26, -74.45], [2.15, 75.42, -67.05], [-0.5, 75.0, -70.5], uv["glow"], 5),
    make_cube("crystal_fx_glow_left", [-3.18, 73.15, -74.3], [-3.02, 75.3, -67.1], [-0.5, 75.0, -70.5], uv["purple"], 5),
    make_cube("crystal_fx_glow_right", [2.02, 73.15, -74.3], [2.18, 75.3, -67.1], [-0.5, 75.0, -70.5], uv["purple"], 5),
    make_cube("crystal_fx_mote_front", [-0.25, 75.55, -75.0], [0.25, 76.05, -74.5], [-0.5, 75.0, -70.5], uv["glow"], 5),
    make_cube("crystal_fx_mote_back", [-1.45, 75.25, -66.9], [-0.9, 75.8, -66.35], [-0.5, 75.0, -70.5], uv["violet"], 6),
    make_cube("crystal_fx_left_low", [-2.35, 75.3, -72.0], [-1.15, 76.15, -70.55], wisp_a["origin"], uv["void"], 5),
    make_cube("crystal_fx_left_high", [-1.85, 76.0, -71.75], [-0.75, 77.2, -70.65], wisp_a["origin"], uv["deep"], 5),
    make_cube("crystal_fx_right_low", [0.0, 75.45, -71.15], [1.35, 76.45, -69.85], wisp_b["origin"], uv["deep"], 6),
    make_cube("crystal_fx_right_high", [-0.15, 76.25, -70.95], [0.85, 77.55, -69.95], wisp_b["origin"], uv["violet"], 6),
    make_cube("crystal_fx_high_low", [-0.9, 76.95, -72.35], [0.3, 77.9, -71.1], wisp_c["origin"], uv["violet"], 7),
    make_cube("crystal_fx_high_tip", [-0.3, 77.75, -72.05], [0.65, 79.2, -71.15], wisp_c["origin"], uv["purple"], 7),
]
model["elements"].extend(cubes)

fx_root_elements = [cube["uuid"] for cube in cubes[:5]]
wisp_a_elements = [cube["uuid"] for cube in cubes[5:7]]
wisp_b_elements = [cube["uuid"] for cube in cubes[7:9]]
wisp_c_elements = [cube["uuid"] for cube in cubes[9:11]]
fx_root_node = {
    "uuid": fx_root["uuid"],
    "isOpen": True,
    "children": fx_root_elements + [
        {"uuid": wisp_a["uuid"], "isOpen": True, "children": wisp_a_elements},
        {"uuid": wisp_b["uuid"], "isOpen": True, "children": wisp_b_elements},
        {"uuid": wisp_c["uuid"], "isOpen": True, "children": wisp_c_elements},
    ],
}
gem_node["children"].append(fx_root_node)

animators = {}
motion_specs = [
    (wisp_a, [(0, 0, 0), (0.45, 0.85, -0.2), (-0.3, 1.65, 0.25), (0.2, 0.75, 0.05), (0, 0, 0)], -12),
    (wisp_b, [(0, 0, 0), (-0.35, 1.1, 0.2), (0.4, 1.85, -0.25), (-0.15, 0.9, 0.1), (0, 0, 0)], 10),
    (wisp_c, [(0, 0, 0), (0.25, 0.75, -0.15), (-0.25, 1.45, 0.2), (0.15, 0.7, -0.1), (0, 0, 0)], -7),
]
times = [0.0, 0.6, 1.2, 1.8, 2.4]
for group, positions, tilt in motion_specs:
    frames = []
    for time, position in zip(times, positions):
        frames.append(keyframe("position", time, position))
    rotations = [(0, 0, 0), (0, 90, tilt), (0, 180, -tilt), (0, 270, tilt), (0, 360, 0)]
    for time, rotation in zip(times, rotations):
        frames.append(keyframe("rotation", time, rotation))
    frames.sort(key=lambda item: (item["time"], item["channel"]))
    animators[group["uuid"]] = {
        "name": group["name"],
        "type": "bone",
        "rotation_global": False,
        "quaternion_interpolation": False,
        "keyframes": frames,
    }

model["animations"].append({
    "uuid": uid(),
    "name": "crystal_aura",
    "loop": "loop",
    "override": False,
    "length": 2.4,
    "snapping": 20,
    "selected": False,
    "group_name": "effects",
    "scope": 0,
    "anim_time_update": "",
    "blend_weight": "",
    "start_delay": "",
    "loop_delay": "",
    "animators": animators,
})

with MODEL.open("w", encoding="utf-8", newline="\n") as handle:
    json.dump(model, handle, ensure_ascii=False, indent=2)
    handle.write("\n")

print(f"gem_cubes_emissive={gem_cube_count}")
print(f"fx_groups=4")
print(f"fx_cubes={len(cubes)}")
print(f"animations={len(model['animations'])}")
print(f"effect_texture={EFFECT_TEXTURE}")
print(f"model_backup={MODEL_BACKUP}")
