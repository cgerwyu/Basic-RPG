from __future__ import annotations

import html
import re
from dataclasses import dataclass
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "BOSSES_PLAYER_BESTIARY_RU.md"
OUTPUT = ROOT / "output" / "pdf" / "basic_rpg_bosses_player_bestiary_ru.pdf"

PAGE_W, PAGE_H = A4
MARGIN_X = 18 * mm
TOP_Y = PAGE_H - 20 * mm
CONTENT_W = PAGE_W - 2 * MARGIN_X

PAPER = colors.HexColor("#F3EBDD")
INK = colors.HexColor("#242326")
MUTED = colors.HexColor("#6E6864")
NIGHT = colors.HexColor("#111419")
CREAM = colors.HexColor("#F5EBDD")


@dataclass
class Resident:
    name: str
    text: str


@dataclass
class Chapter:
    number: int
    title: str
    image: Path
    biome_title: str
    biome: list[str]
    residents: list[Resident]
    boss_title: str
    boss: list[str]
    accent: colors.Color
    short_biome: str


ACCENTS = [
    colors.HexColor("#6E7F4A"),
    colors.HexColor("#4588B8"),
    colors.HexColor("#4FA9BA"),
    colors.HexColor("#7B4C99"),
    colors.HexColor("#B06B35"),
    colors.HexColor("#A53A2C"),
]


def register_fonts() -> None:
    font_dir = Path(r"C:\Windows\Fonts")
    pdfmetrics.registerFont(TTFont("BestiarySerif", str(font_dir / "georgia.ttf")))
    pdfmetrics.registerFont(TTFont("BestiarySerif-Bold", str(font_dir / "georgiab.ttf")))
    pdfmetrics.registerFont(TTFont("BestiarySerif-Italic", str(font_dir / "georgiai.ttf")))
    pdfmetrics.registerFont(TTFont("BestiarySans", str(font_dir / "arial.ttf")))
    pdfmetrics.registerFont(TTFont("BestiarySans-Bold", str(font_dir / "arialbd.ttf")))


def clean(text: str) -> str:
    return (
        text.replace("—", "-")
        .replace("–", "-")
        .replace("‑", "-")
        .strip()
    )


def paragraphs(text: str) -> list[str]:
    return [clean(item) for item in re.split(r"\n\s*\n", text.strip()) if item.strip()]


def parse_source() -> tuple[list[str], list[str], list[Chapter]]:
    raw = SOURCE.read_text(encoding="utf-8")
    intro_match = re.search(r"^## Предисловие\s*$([\s\S]*?)^## Содержание\s*$", raw, re.M)
    toc_match = re.search(r"^## Содержание\s*$([\s\S]*?)^## 1\.", raw, re.M)
    if not intro_match or not toc_match:
        raise RuntimeError("Не удалось прочитать предисловие или содержание")

    intro = paragraphs(intro_match.group(1))
    toc = [clean(line[3:]) for line in toc_match.group(1).splitlines() if re.match(r"\d+\. ", line)]

    chapters: list[Chapter] = []
    blocks = re.split(r"(?=^## \d+\.)", raw, flags=re.M)[1:]
    for index, block in enumerate(blocks):
        header = re.match(r"^## (\d+)\. (.+)$", block, re.M)
        if not header:
            continue
        number = int(header.group(1))
        title = clean(header.group(2))
        image_match = re.search(r"!\[[^\]]*\]\(([^)]+)\)", block)
        biome_match = re.search(
            r"^### Биом: (.+)$([\s\S]*?)^### Кто здесь живет\s*$",
            block,
            re.M,
        )
        residents_match = re.search(
            r"^### Кто здесь живет\s*$([\s\S]*?)^### Хозяин биома: (.+)$",
            block,
            re.M,
        )
        boss_match = re.search(
            r"^### Хозяин биома: (.+)$([\s\S]*?)(?=^## |\Z)",
            block,
            re.M,
        )
        if not all([image_match, biome_match, residents_match, boss_match]):
            raise RuntimeError(f"Неполная глава: {title}")

        image_path = (SOURCE.parent / image_match.group(1)).resolve()
        biome_title = clean(biome_match.group(1))
        biome_text = re.sub(r"!\[[^\]]*\]\([^)]+\)", "", biome_match.group(2))
        resident_parts = re.split(r"^#### ", residents_match.group(1), flags=re.M)[1:]
        residents: list[Resident] = []
        for resident_part in resident_parts:
            lines = resident_part.strip().splitlines()
            name = clean(lines[0])
            text = " ".join(paragraphs("\n".join(lines[1:])))
            residents.append(Resident(name, text))

        chapters.append(
            Chapter(
                number=number,
                title=title,
                image=image_path,
                biome_title=biome_title,
                biome=paragraphs(biome_text),
                residents=residents,
                boss_title=clean(boss_match.group(1)),
                boss=paragraphs(boss_match.group(2)),
                accent=ACCENTS[index],
                short_biome=biome_title.upper(),
            )
        )
    return intro, toc, chapters


STYLES = {
    "body": ParagraphStyle(
        "Body",
        fontName="BestiarySerif",
        fontSize=10.2,
        leading=15.2,
        textColor=INK,
        spaceAfter=4.2 * mm,
    ),
    "body_small": ParagraphStyle(
        "BodySmall",
        fontName="BestiarySerif",
        fontSize=8.7,
        leading=12.1,
        textColor=INK,
        spaceAfter=2.8 * mm,
    ),
    "resident_name": ParagraphStyle(
        "ResidentName",
        fontName="BestiarySans-Bold",
        fontSize=9.2,
        leading=11.5,
        textColor=INK,
        spaceAfter=1.1 * mm,
    ),
    "toc": ParagraphStyle(
        "Toc",
        fontName="BestiarySerif",
        fontSize=12,
        leading=16,
        textColor=INK,
    ),
    "boss": ParagraphStyle(
        "Boss",
        fontName="BestiarySerif",
        fontSize=10.1,
        leading=15,
        textColor=INK,
        spaceAfter=4.1 * mm,
    ),
}


def draw_paragraph(c: canvas.Canvas, text: str, style: ParagraphStyle, x: float, y: float, width: float) -> float:
    paragraph = Paragraph(html.escape(clean(text)), style)
    _, height = paragraph.wrap(width, PAGE_H)
    paragraph.drawOn(c, x, y - height)
    return y - height - style.spaceAfter


def page_base(c: canvas.Canvas, page_no: int, section: str, accent: colors.Color) -> None:
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    c.setFillColor(accent)
    c.rect(0, PAGE_H - 4 * mm, PAGE_W, 4 * mm, fill=1, stroke=0)
    c.setStrokeColor(colors.HexColor("#D7CCBA"))
    c.setLineWidth(0.35)
    c.line(MARGIN_X, 14 * mm, PAGE_W - MARGIN_X, 14 * mm)
    c.setFillColor(MUTED)
    c.setFont("BestiarySans", 7.2)
    c.drawString(MARGIN_X, 9.4 * mm, section.upper())
    c.drawRightString(PAGE_W - MARGIN_X, 9.4 * mm, str(page_no))


def draw_image(c: canvas.Canvas, path: Path, x: float, y: float, width: float, height: float) -> None:
    if not path.exists():
        raise FileNotFoundError(path)
    c.drawImage(str(path), x, y, width=width, height=height, preserveAspectRatio=True, anchor="c", mask="auto")


def draw_cover(c: canvas.Canvas, tarr: Chapter) -> None:
    c.setFillColor(NIGHT)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    image_h = 140 * mm
    draw_image(c, tarr.image, 0, PAGE_H - 18 * mm - image_h, PAGE_W, image_h)
    c.setFillColor(colors.Color(0.05, 0.06, 0.07, alpha=0.30))
    c.rect(0, PAGE_H - 18 * mm - image_h, PAGE_W, image_h, fill=1, stroke=0)
    c.setFillColor(tarr.accent)
    c.rect(0, PAGE_H - 18 * mm, PAGE_W, 18 * mm, fill=1, stroke=0)
    c.setStrokeColor(colors.Color(1, 1, 1, alpha=0.26))
    c.setLineWidth(0.5)
    c.rect(11 * mm, 11 * mm, PAGE_W - 22 * mm, PAGE_H - 22 * mm, fill=0, stroke=1)

    c.setFillColor(CREAM)
    c.setFont("BestiarySans-Bold", 8.5)
    c.drawString(18 * mm, 78 * mm, "КНИГА ДЛЯ СТРАННИКОВ")
    c.setFont("BestiarySerif-Bold", 30)
    c.drawString(18 * mm, 57 * mm, "БЕСТИАРИЙ")
    c.setFont("BestiarySerif-Bold", 20)
    c.drawString(18 * mm, 44 * mm, "ЗЕМЕЛЬ ПЕРВОКЛЫКА")
    c.setFont("BestiarySerif-Italic", 10.5)
    c.setFillColor(colors.HexColor("#CFC8BB"))
    c.drawString(18 * mm, 28 * mm, "Шесть биомов, шесть древних хозяев")
    c.setFillColor(CREAM)
    c.setFont("BestiarySans", 7.2)
    c.drawRightString(PAGE_W - 18 * mm, 17 * mm, "ТОЛЬКО ЛОР")
    c.showPage()


def draw_intro(c: canvas.Canvas, page_no: int, intro: list[str]) -> None:
    page_base(c, page_no, "Предисловие", ACCENTS[0])
    c.setFillColor(INK)
    c.setFont("BestiarySerif-Bold", 25)
    c.drawString(MARGIN_X, TOP_Y, "Перед дорогой")
    c.setFillColor(ACCENTS[0])
    c.rect(MARGIN_X, TOP_Y - 9 * mm, 33 * mm, 1.2 * mm, fill=1, stroke=0)
    y = TOP_Y - 18 * mm
    for item in intro:
        y = draw_paragraph(c, item, STYLES["body"], MARGIN_X, y, CONTENT_W)

    c.setFillColor(colors.HexColor("#E5D8C4"))
    c.roundRect(MARGIN_X, 35 * mm, CONTENT_W, 34 * mm, 3 * mm, fill=1, stroke=0)
    c.setFillColor(INK)
    c.setFont("BestiarySerif-Italic", 11)
    c.drawString(MARGIN_X + 8 * mm, 55 * mm, "Сначала земля. Затем ее жители. И только потом тот,")
    c.drawString(MARGIN_X + 8 * mm, 46 * mm, "чьему имени эта земля научилась бояться.")
    c.showPage()


def draw_contents(c: canvas.Canvas, page_no: int, toc: list[str]) -> None:
    page_base(c, page_no, "Содержание", colors.HexColor("#7E7467"))
    c.setFillColor(INK)
    c.setFont("BestiarySerif-Bold", 25)
    c.drawString(MARGIN_X, TOP_Y, "Путь через шесть земель")
    y = TOP_Y - 20 * mm
    for index, item in enumerate(toc):
        page = 4 + index * 4
        accent = ACCENTS[index]
        c.setFillColor(accent)
        c.circle(MARGIN_X + 6 * mm, y - 2.5 * mm, 5.3 * mm, fill=1, stroke=0)
        c.setFillColor(colors.white)
        c.setFont("BestiarySans-Bold", 8.5)
        c.drawCentredString(MARGIN_X + 6 * mm, y - 5.2 * mm, str(index + 1))
        c.setFillColor(INK)
        c.setFont("BestiarySerif-Bold", 12)
        c.drawString(MARGIN_X + 17 * mm, y + 1.2 * mm, item.split(":", 1)[0])
        c.setFillColor(MUTED)
        c.setFont("BestiarySerif", 9.2)
        subtitle = item.split(":", 1)[1].strip() if ":" in item else ""
        c.drawString(MARGIN_X + 17 * mm, y - 6.2 * mm, subtitle)
        c.setStrokeColor(colors.HexColor("#CEC2AF"))
        c.setDash(1, 2)
        c.line(MARGIN_X + 17 * mm, y - 10 * mm, PAGE_W - MARGIN_X - 13 * mm, y - 10 * mm)
        c.setDash()
        c.setFillColor(accent)
        c.setFont("BestiarySans-Bold", 8.5)
        c.drawRightString(PAGE_W - MARGIN_X, y - 4 * mm, str(page))
        y -= 32 * mm
    c.showPage()


def draw_chapter_opener(c: canvas.Canvas, page_no: int, chapter: Chapter) -> None:
    c.bookmarkPage(f"chapter-{chapter.number}")
    c.addOutlineEntry(chapter.title, f"chapter-{chapter.number}", level=0, closed=False)
    c.setFillColor(NIGHT)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    image_h = 140 * mm
    image_y = PAGE_H - image_h
    draw_image(c, chapter.image, 0, image_y, PAGE_W, image_h)
    c.setFillColor(colors.Color(0, 0, 0, alpha=0.17))
    c.rect(0, image_y, PAGE_W, image_h, fill=1, stroke=0)
    c.setFillColor(chapter.accent)
    c.rect(0, image_y - 5 * mm, PAGE_W, 5 * mm, fill=1, stroke=0)

    c.setFillColor(chapter.accent)
    c.setFont("BestiarySans-Bold", 9)
    c.drawString(18 * mm, 112 * mm, f"ГЛАВА {chapter.number}")
    c.setFillColor(CREAM)
    c.setFont("BestiarySerif-Bold", 25)
    title_words = chapter.title.split()
    if len(chapter.title) > 27:
        midpoint = max(1, len(title_words) // 2)
        c.drawString(18 * mm, 91 * mm, " ".join(title_words[:midpoint]))
        c.drawString(18 * mm, 76 * mm, " ".join(title_words[midpoint:]))
        sub_y = 57 * mm
    else:
        c.drawString(18 * mm, 88 * mm, chapter.title)
        sub_y = 65 * mm
    c.setFont("BestiarySerif-Italic", 12)
    c.setFillColor(colors.HexColor("#CFC9C0"))
    c.drawString(18 * mm, sub_y, chapter.biome_title)
    c.setFillColor(CREAM)
    c.setFont("BestiarySans", 7.2)
    c.drawRightString(PAGE_W - 18 * mm, 16 * mm, str(page_no))
    c.showPage()


def draw_biome(c: canvas.Canvas, page_no: int, chapter: Chapter) -> None:
    page_base(c, page_no, chapter.biome_title, chapter.accent)
    c.setFillColor(chapter.accent)
    c.setFont("BestiarySans-Bold", 8.5)
    c.drawString(MARGIN_X, TOP_Y, "БИОМ")
    c.setFillColor(INK)
    c.setFont("BestiarySerif-Bold", 23)
    c.drawString(MARGIN_X, TOP_Y - 13 * mm, chapter.biome_title)
    c.setStrokeColor(chapter.accent)
    c.setLineWidth(1.1)
    c.line(MARGIN_X, TOP_Y - 19 * mm, MARGIN_X + 38 * mm, TOP_Y - 19 * mm)
    y = TOP_Y - 29 * mm
    for item in chapter.biome:
        y = draw_paragraph(c, item, STYLES["body"], MARGIN_X, y, CONTENT_W)
    c.showPage()


def resident_column_height(items: list[Resident], width: float) -> float:
    height = 0.0
    for resident in items:
        name = Paragraph(html.escape(resident.name), STYLES["resident_name"])
        _, name_h = name.wrap(width, PAGE_H)
        body = Paragraph(html.escape(resident.text), STYLES["body_small"])
        _, body_h = body.wrap(width, PAGE_H)
        height += name_h + STYLES["resident_name"].spaceAfter + body_h + STYLES["body_small"].spaceAfter + 1.5 * mm
    return height


def split_residents(residents: list[Resident], width: float) -> tuple[list[Resident], list[Resident]]:
    best = (residents[:1], residents[1:])
    best_delta = float("inf")
    for split in range(1, len(residents)):
        left, right = residents[:split], residents[split:]
        delta = abs(resident_column_height(left, width) - resident_column_height(right, width))
        if delta < best_delta:
            best = (left, right)
            best_delta = delta
    return best


def draw_resident_entry(c: canvas.Canvas, resident: Resident, x: float, y: float, width: float, accent: colors.Color) -> float:
    c.setFillColor(accent)
    c.circle(x + 1.6 * mm, y - 2.1 * mm, 1.5 * mm, fill=1, stroke=0)
    name = Paragraph(html.escape(resident.name), STYLES["resident_name"])
    _, name_h = name.wrap(width - 6 * mm, PAGE_H)
    name.drawOn(c, x + 6 * mm, y - name_h)
    y -= name_h + STYLES["resident_name"].spaceAfter
    body = Paragraph(html.escape(resident.text), STYLES["body_small"])
    _, body_h = body.wrap(width, PAGE_H)
    body.drawOn(c, x, y - body_h)
    return y - body_h - STYLES["body_small"].spaceAfter - 1.5 * mm


def draw_residents(c: canvas.Canvas, page_no: int, chapter: Chapter) -> None:
    page_base(c, page_no, "Кто здесь живет", chapter.accent)
    c.setFillColor(chapter.accent)
    c.setFont("BestiarySans-Bold", 8.5)
    c.drawString(MARGIN_X, TOP_Y, chapter.short_biome)
    c.setFillColor(INK)
    c.setFont("BestiarySerif-Bold", 23)
    c.drawString(MARGIN_X, TOP_Y - 13 * mm, "Кто здесь живет")
    column_gap = 11 * mm
    column_w = (CONTENT_W - column_gap) / 2
    left, right = split_residents(chapter.residents, column_w)
    start_y = TOP_Y - 28 * mm
    for items, x in [(left, MARGIN_X), (right, MARGIN_X + column_w + column_gap)]:
        y = start_y
        for resident in items:
            y = draw_resident_entry(c, resident, x, y, column_w, chapter.accent)
        if y < 24 * mm:
            raise RuntimeError(f"Обитатели не помещаются на странице: {chapter.title}")
    c.setStrokeColor(colors.HexColor("#D2C6B4"))
    c.setLineWidth(0.4)
    c.line(PAGE_W / 2, 24 * mm, PAGE_W / 2, start_y)
    c.showPage()


def draw_boss(c: canvas.Canvas, page_no: int, chapter: Chapter) -> None:
    page_base(c, page_no, chapter.boss_title, chapter.accent)
    c.setFillColor(chapter.accent)
    c.setFont("BestiarySans-Bold", 8.5)
    c.drawString(MARGIN_X, TOP_Y, "ХОЗЯИН БИОМА")
    title_style = ParagraphStyle(
        "BossTitle",
        fontName="BestiarySerif-Bold",
        fontSize=20,
        leading=23,
        textColor=INK,
    )
    title_p = Paragraph(html.escape(chapter.boss_title), title_style)
    _, title_h = title_p.wrap(82 * mm, 36 * mm)
    title_p.drawOn(c, MARGIN_X, TOP_Y - 9 * mm - title_h)

    image_w = 78 * mm
    image_h = image_w / 1.5
    image_x = PAGE_W - MARGIN_X - image_w
    image_y = TOP_Y - 60 * mm
    c.setFillColor(colors.HexColor("#D8CCBA"))
    c.rect(image_x - 2 * mm, image_y - 2 * mm, image_w + 4 * mm, image_h + 4 * mm, fill=1, stroke=0)
    draw_image(c, chapter.image, image_x, image_y, image_w, image_h)

    c.setFillColor(colors.HexColor("#E4D7C3"))
    c.roundRect(MARGIN_X, image_y, 78 * mm, 28 * mm, 2.5 * mm, fill=1, stroke=0)
    c.setFillColor(chapter.accent)
    c.setFont("BestiarySerif-Italic", 10.2)
    if chapter.number == 1:
        quote = "Первый великий след на дороге странника."
    else:
        quote = f"Земля помнит имя: {chapter.boss_title}."
    quote_p = Paragraph(html.escape(quote), ParagraphStyle(
        "Quote", fontName="BestiarySerif-Italic", fontSize=10.2, leading=14, textColor=chapter.accent
    ))
    _, quote_h = quote_p.wrap(66 * mm, 26 * mm)
    quote_p.drawOn(c, MARGIN_X + 6 * mm, image_y + 14 * mm - quote_h / 2)

    y = image_y - 11 * mm
    for item in chapter.boss:
        y = draw_paragraph(c, item, STYLES["boss"], MARGIN_X, y, CONTENT_W)
    if y < 23 * mm:
        raise RuntimeError(f"Текст босса не помещается на странице: {chapter.title}")
    c.showPage()


def build() -> None:
    register_fonts()
    intro, toc, chapters = parse_source()
    if len(chapters) != 6:
        raise RuntimeError(f"Ожидалось 6 глав, найдено {len(chapters)}")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4, pageCompression=1)
    c.setTitle("Бестиарий земель Первоклыка")
    c.setAuthor("Basic RPG Classes")
    c.setSubject("Лор биомов, их обитателей и древних хозяев")

    draw_cover(c, chapters[0])
    draw_intro(c, 2, intro)
    draw_contents(c, 3, toc)
    page = 4
    for chapter in chapters:
        draw_chapter_opener(c, page, chapter)
        draw_biome(c, page + 1, chapter)
        draw_residents(c, page + 2, chapter)
        draw_boss(c, page + 3, chapter)
        page += 4
    c.save()
    print(f"Создано: {OUTPUT}")
    print(f"Страниц: {page - 1}")


if __name__ == "__main__":
    build()
