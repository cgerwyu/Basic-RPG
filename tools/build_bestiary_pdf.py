from __future__ import annotations

import html
import re
import sys
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    HRFlowable,
    Image,
    KeepTogether,
    ListFlowable,
    ListItem,
    LongTable,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "BOSSES_BESTIARY_VOLUME_1_RU.md"
OUTPUT = ROOT / "output" / "pdf" / "basic_rpg_bosses_bestiary_volume_1_ru.pdf"
REFERENCE_IMAGES = [
    Path(r"C:\Users\cdald\Desktop\pixellab-Ultra-rare-fantasy-magic-gem-s-1788115506244.png"),
    Path(r"C:\Users\cdald\Desktop\pixellab-Ultra-rare-fantasy-magic-gem-s-1788115999773.png"),
]
LIZARD_ARMOR_DIR = Path(r"C:\Users\cdald\Desktop\references\ARMOR\lizards")
LIZARD_BASIC_ARMOR = [
    ("Шлем", LIZARD_ARMOR_DIR / "basic helmet.png"),
    ("Нагрудник", LIZARD_ARMOR_DIR / "basic chestplate.png"),
    ("Поножи", LIZARD_ARMOR_DIR / "basic leggings.png"),
    ("Ботинки", LIZARD_ARMOR_DIR / "basic boots.png"),
]
LIZARD_BOSS_ARMOR = [
    ("Шлем Первозуба", LIZARD_ARMOR_DIR / "pixellab-The-green-gem-embedded-in-the--1788182730378.png"),
    ("Нагрудник Первозуба", LIZARD_ARMOR_DIR / "Boss chestplate animated.png"),
    ("Поножи Первозуба", LIZARD_ARMOR_DIR / "Boss leggings animated.png"),
    ("Ботинки Первозуба", LIZARD_ARMOR_DIR / "Boss boots animated.png"),
]

PAGE_W, PAGE_H = A4
MARGIN_X = 18 * mm
MARGIN_TOP = 19 * mm
MARGIN_BOTTOM = 18 * mm
CONTENT_W = PAGE_W - 2 * MARGIN_X

INK = colors.HexColor("#252328")
MUTED = colors.HexColor("#6D6664")
PAPER = colors.HexColor("#F7F2E8")
PAPER_DARK = colors.HexColor("#EAE1D2")
NIGHT = colors.HexColor("#11131A")
BRONZE = colors.HexColor("#A9763F")
CRIMSON = colors.HexColor("#7D2728")
STORM = colors.HexColor("#2F7FA3")
FROST = colors.HexColor("#3B91A3")
SILK = colors.HexColor("#6C3B82")
FORGE = colors.HexColor("#A4552A")
DRAGON = colors.HexColor("#8F2A22")


def register_fonts() -> None:
    font_dir = Path(r"C:\Windows\Fonts")
    pdfmetrics.registerFont(TTFont("BookSerif", str(font_dir / "georgia.ttf")))
    pdfmetrics.registerFont(TTFont("BookSerif-Bold", str(font_dir / "georgiab.ttf")))
    pdfmetrics.registerFont(TTFont("BookSerif-Italic", str(font_dir / "georgiai.ttf")))
    pdfmetrics.registerFont(TTFont("BookSerif-BoldItalic", str(font_dir / "georgiaz.ttf")))
    pdfmetrics.registerFont(TTFont("BookSans", str(font_dir / "arial.ttf")))
    pdfmetrics.registerFont(TTFont("BookSans-Bold", str(font_dir / "arialbd.ttf")))
    pdfmetrics.registerFont(TTFont("BookCode", str(font_dir / "consola.ttf")))
    pdfmetrics.registerFontFamily(
        "BookSerif",
        normal="BookSerif",
        bold="BookSerif-Bold",
        italic="BookSerif-Italic",
        boldItalic="BookSerif-BoldItalic",
    )


def gem_path(canvas, cx: float, cy: float, radius: float, sides: int = 6) -> None:
    import math

    path = canvas.beginPath()
    for index in range(sides):
        angle = -math.pi / 2 + index * 2 * math.pi / sides
        x = cx + math.cos(angle) * radius
        y = cy + math.sin(angle) * radius
        if index == 0:
            path.moveTo(x, y)
        else:
            path.lineTo(x, y)
    path.close()
    canvas.drawPath(path, fill=1, stroke=1)


def draw_cover(canvas, doc) -> None:
    canvas.saveState()
    canvas.setFillColor(NIGHT)
    canvas.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    canvas.setStrokeColor(BRONZE)
    canvas.setLineWidth(0.8)
    canvas.rect(10 * mm, 10 * mm, PAGE_W - 20 * mm, PAGE_H - 20 * mm, fill=0, stroke=1)
    canvas.setLineWidth(0.25)
    canvas.rect(13 * mm, 13 * mm, PAGE_W - 26 * mm, PAGE_H - 26 * mm, fill=0, stroke=1)

    gem_colors = [STORM, FROST, SILK, FORGE, DRAGON]
    xs = [45, 75, 105, 135, 165]
    for index, (x_mm, color) in enumerate(zip(xs, gem_colors)):
        canvas.setFillColor(color)
        canvas.setStrokeColor(colors.HexColor("#E9D9B8"))
        canvas.setLineWidth(0.7)
        gem_path(canvas, x_mm * mm, 63 * mm, (8 + (index == 2) * 2) * mm, 6 if index != 4 else 5)
        canvas.setFillColor(colors.Color(1, 1, 1, alpha=0.33))
        canvas.circle((x_mm - 2) * mm, 66 * mm, 1.4 * mm, fill=1, stroke=0)

    canvas.setStrokeColor(colors.Color(0.85, 0.69, 0.43, alpha=0.35))
    canvas.line(31 * mm, 84 * mm, 179 * mm, 84 * mm)
    canvas.line(31 * mm, PAGE_H - 70 * mm, 179 * mm, PAGE_H - 70 * mm)
    canvas.restoreState()


def draw_content_page(canvas, doc) -> None:
    canvas.saveState()
    canvas.setFillColor(PAPER)
    canvas.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    canvas.setStrokeColor(PAPER_DARK)
    canvas.setLineWidth(0.35)
    canvas.line(MARGIN_X, PAGE_H - 14 * mm, PAGE_W - MARGIN_X, PAGE_H - 14 * mm)
    canvas.line(MARGIN_X, 13 * mm, PAGE_W - MARGIN_X, 13 * mm)

    canvas.setFillColor(MUTED)
    canvas.setFont("BookSans", 7.3)
    canvas.drawString(MARGIN_X, PAGE_H - 11 * mm, "BASIC RPG BOSSES  /  БЕСТИАРИЙ, ТОМ I")
    canvas.drawRightString(PAGE_W - MARGIN_X, 9.5 * mm, f"{doc.page}")

    accent = [STORM, FROST, SILK, FORGE, DRAGON][(max(doc.page, 2) - 2) % 5]
    canvas.setFillColor(accent)
    canvas.rect(MARGIN_X, PAGE_H - 14.6 * mm, 23 * mm, 0.8 * mm, fill=1, stroke=0)
    canvas.restoreState()


class BestiaryDocTemplate(BaseDocTemplate):
    def __init__(self, filename: str, **kwargs):
        super().__init__(filename, **kwargs)
        cover_frame = Frame(18 * mm, 18 * mm, PAGE_W - 36 * mm, PAGE_H - 36 * mm, id="cover")
        content_frame = Frame(
            MARGIN_X,
            MARGIN_BOTTOM,
            CONTENT_W,
            PAGE_H - MARGIN_TOP - MARGIN_BOTTOM,
            id="content",
            leftPadding=0,
            rightPadding=0,
            topPadding=0,
            bottomPadding=0,
        )
        self.addPageTemplates(
            [
                PageTemplate(id="Cover", frames=[cover_frame], onPage=draw_cover),
                PageTemplate(id="Content", frames=[content_frame], onPage=draw_content_page),
            ]
        )

    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph):
            return
        style_name = flowable.style.name
        if style_name not in {"BookH2", "BookH3"}:
            return
        level = 0 if style_name == "BookH2" else 1
        text = flowable.getPlainText()
        key = f"bookmark-{level}-{self.seq.nextf('bookmark')}"
        self.canv.bookmarkPage(key)
        if level == 0:
            self.canv.addOutlineEntry(text, key, level=0, closed=False)
        self.notify("TOCEntry", (level, text, self.page, key))


def make_styles():
    base = getSampleStyleSheet()
    styles = {}
    styles["cover_kicker"] = ParagraphStyle(
        "CoverKicker",
        parent=base["Normal"],
        fontName="BookSans-Bold",
        fontSize=9,
        leading=11,
        textColor=BRONZE,
        alignment=TA_CENTER,
        spaceAfter=10 * mm,
        tracking=1.5,
    )
    styles["cover_title"] = ParagraphStyle(
        "CoverTitle",
        parent=base["Title"],
        fontName="BookSerif-Bold",
        fontSize=31,
        leading=35,
        textColor=colors.HexColor("#F4E9D4"),
        alignment=TA_CENTER,
        spaceAfter=8 * mm,
    )
    styles["cover_subtitle"] = ParagraphStyle(
        "CoverSubtitle",
        parent=base["Normal"],
        fontName="BookSerif-Italic",
        fontSize=12,
        leading=17,
        textColor=colors.HexColor("#D0C5B2"),
        alignment=TA_CENTER,
    )
    styles["body"] = ParagraphStyle(
        "BookBody",
        parent=base["BodyText"],
        fontName="BookSerif",
        fontSize=8.7,
        leading=12.4,
        textColor=INK,
        alignment=TA_LEFT,
        spaceAfter=2.2 * mm,
        allowWidows=0,
        allowOrphans=0,
    )
    styles["h2"] = ParagraphStyle(
        "BookH2",
        parent=base["Heading1"],
        fontName="BookSerif-Bold",
        fontSize=21,
        leading=25,
        textColor=CRIMSON,
        spaceBefore=2 * mm,
        spaceAfter=5 * mm,
        keepWithNext=True,
    )
    styles["h3"] = ParagraphStyle(
        "BookH3",
        parent=base["Heading2"],
        fontName="BookSerif-Bold",
        fontSize=13.5,
        leading=17,
        textColor=colors.HexColor("#573A2E"),
        spaceBefore=4.2 * mm,
        spaceAfter=2.2 * mm,
        keepWithNext=True,
    )
    styles["h4"] = ParagraphStyle(
        "BookH4",
        parent=base["Heading3"],
        fontName="BookSans-Bold",
        fontSize=9.5,
        leading=12,
        textColor=colors.HexColor("#4B555D"),
        spaceBefore=3.4 * mm,
        spaceAfter=1.5 * mm,
        keepWithNext=True,
    )
    styles["bullet"] = ParagraphStyle(
        "BookBullet",
        parent=styles["body"],
        fontSize=8.4,
        leading=11.5,
        leftIndent=0,
        firstLineIndent=0,
        spaceAfter=0.9 * mm,
    )
    styles["quote"] = ParagraphStyle(
        "BookQuote",
        parent=styles["body"],
        fontName="BookSerif-Italic",
        fontSize=9.2,
        leading=13.2,
        textColor=colors.HexColor("#5F4850"),
        leftIndent=5 * mm,
        rightIndent=5 * mm,
        borderColor=SILK,
        borderWidth=0,
        borderPadding=3 * mm,
        backColor=colors.HexColor("#EEE6EE"),
        spaceBefore=2 * mm,
        spaceAfter=3 * mm,
    )
    styles["code"] = ParagraphStyle(
        "BookCodeBlock",
        parent=base["Code"],
        fontName="BookCode",
        fontSize=7.1,
        leading=9.2,
        textColor=colors.HexColor("#E8E3D9"),
        backColor=colors.HexColor("#24262D"),
        borderPadding=3 * mm,
        leftIndent=2 * mm,
        rightIndent=2 * mm,
        spaceBefore=1.5 * mm,
        spaceAfter=3 * mm,
    )
    styles["table"] = ParagraphStyle(
        "BookTable",
        parent=styles["body"],
        fontName="BookSans",
        fontSize=7.0,
        leading=9.1,
        spaceAfter=0,
        splitLongWords=True,
    )
    styles["table_header"] = ParagraphStyle(
        "BookTableHeader",
        parent=styles["table"],
        fontName="BookSans-Bold",
        textColor=colors.white,
        alignment=TA_LEFT,
    )
    styles["caption"] = ParagraphStyle(
        "BookCaption",
        parent=styles["body"],
        fontName="BookSans",
        fontSize=7.2,
        leading=9,
        textColor=MUTED,
        alignment=TA_CENTER,
        spaceBefore=1.5 * mm,
    )
    styles["toc_title"] = ParagraphStyle(
        "TocTitle",
        parent=styles["h2"],
        fontSize=24,
        alignment=TA_CENTER,
        spaceAfter=5 * mm,
    )
    return styles


INLINE_CODE = re.compile(r"`([^`]+)`")
BOLD = re.compile(r"\*\*(.+?)\*\*")
ITALIC = re.compile(r"(?<!\*)\*([^*]+?)\*(?!\*)")


def inline_markup(text: str) -> str:
    escaped = html.escape(text, quote=False)
    escaped = INLINE_CODE.sub(r'<font name="BookCode" color="#7A3E2D">\1</font>', escaped)
    escaped = BOLD.sub(r"<b>\1</b>", escaped)
    escaped = ITALIC.sub(r"<i>\1</i>", escaped)
    return escaped


def table_widths(column_count: int):
    ratios = {
        2: [0.28, 0.72],
        3: [0.21, 0.37, 0.42],
        4: [0.18, 0.22, 0.31, 0.29],
        5: [0.15, 0.17, 0.23, 0.23, 0.22],
    }.get(column_count)
    if ratios is None:
        ratios = [1 / column_count] * column_count
    return [CONTENT_W * ratio for ratio in ratios]


def parse_table(lines: list[str], styles) -> LongTable:
    raw_rows = []
    for line in lines:
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        raw_rows.append(cells)
    if len(raw_rows) > 1 and all(re.fullmatch(r":?-{3,}:?", cell or "") for cell in raw_rows[1]):
        del raw_rows[1]
    width = max(len(row) for row in raw_rows)
    for row in raw_rows:
        row.extend([""] * (width - len(row)))
    data = []
    for row_index, row in enumerate(raw_rows):
        style = styles["table_header"] if row_index == 0 else styles["table"]
        data.append([Paragraph(inline_markup(cell), style) for cell in row])
    table = LongTable(data, colWidths=table_widths(width), repeatRows=1, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#51433E")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBBEAA")),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 3.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5),
    ]
    for row_index in range(1, len(data)):
        commands.append(
            ("BACKGROUND", (0, row_index), (-1, row_index), PAPER if row_index % 2 else colors.HexColor("#F0E8DB"))
        )
    table.setStyle(TableStyle(commands))
    table.spaceBefore = 1.5 * mm
    table.spaceAfter = 3 * mm
    return table


def build_reference_page(styles):
    flowables = [
        PageBreak(),
        Paragraph("Визуальные референсы самоцветов", styles["h2"]),
        Paragraph(
            "Предоставленные листы используются как направление для пиксель-арта: тёмный контур, яркий центр и уникальный силуэт. Финальные самоцветы бестиария не копируют отдельные значки и строятся по описанным в книге формам.",
            styles["body"],
        ),
    ]
    cells = []
    for image_path in REFERENCE_IMAGES:
        if image_path.exists():
            image = Image(str(image_path), width=72 * mm, height=72 * mm)
            image.hAlign = "CENTER"
            cells.append(image)
    if cells:
        if len(cells) == 1:
            cells.append(Spacer(72 * mm, 72 * mm))
        table = Table([cells], colWidths=[CONTENT_W / 2, CONTENT_W / 2])
        table.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("ALIGN", (0, 0), (-1, -1), "CENTER")]))
        flowables.extend([Spacer(1, 4 * mm), table, Paragraph("Референсные листы редких магических камней", styles["caption"])])
    return flowables


def build_lizard_armor_reference_page(styles):
    flowables = [
        PageBreak(),
        Paragraph("Утверждённая тяжёлая броня кхарров", styles["h2"]),
        Paragraph(
            "Авторские спрайты ниже являются каноном Воина и Паладина. Верхний ряд — обычный комплект Каменного Клыка из трофеев мобов. Большие листы — тот же комплект после возвышения самоцветом Первозуба. Лёгкие комплекты Мага, Охотника и Жреца требуют отдельных рисунков.",
            styles["body"],
        ),
        Paragraph("Обычный тяжёлый комплект · статические иконки 32 × 32", styles["h3"]),
    ]

    basic_cells = []
    for label, image_path in LIZARD_BASIC_ARMOR:
        if not image_path.exists():
            continue
        icon = Image(str(image_path), width=24 * mm, height=24 * mm)
        icon.hAlign = "CENTER"
        basic_cells.append([icon, Spacer(1, 1.5 * mm), Paragraph(label, styles["caption"])])
    if basic_cells:
        basic_table = Table([basic_cells], colWidths=[CONTENT_W / max(len(basic_cells), 1)] * len(basic_cells))
        basic_table.setStyle(
            TableStyle(
                [
                    ("VALIGN", (0, 0), (-1, -1), "TOP"),
                    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                    ("BACKGROUND", (0, 0), (-1, -1), PAPER_DARK),
                    ("BOX", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBBEAA")),
                    ("INNERGRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#D8CCBA")),
                    ("TOPPADDING", (0, 0), (-1, -1), 5),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ]
            )
        )
        flowables.extend([basic_table, Spacer(1, 4 * mm)])

    flowables.extend(
        [
            Paragraph("Возвышенный тяжёлый комплект · авторские листы 3 × 3", styles["h3"]),
            Paragraph(
                "Каждый лист имеет размер 192 × 192 и состоит из ячеек 64 × 64. Первая ячейка хранит спокойное состояние; оставшиеся восемь образуют цикл свечения в инвентаре и на предметной модели в руке. Цельный самоцвет находится в нагруднике, суверенные осколки — в шлеме, поножах и ботинках.",
                styles["body"],
            ),
        ]
    )

    boss_cells = []
    for label, image_path in LIZARD_BOSS_ARMOR:
        if not image_path.exists():
            continue
        sheet = Image(str(image_path), width=54 * mm, height=54 * mm)
        sheet.hAlign = "CENTER"
        boss_cells.append([sheet, Spacer(1, 1.5 * mm), Paragraph(label, styles["caption"])])
    if boss_cells:
        while len(boss_cells) < 4:
            boss_cells.append(Spacer(1, 54 * mm))
        boss_table = Table(
            [boss_cells[:2], boss_cells[2:4]],
            colWidths=[CONTENT_W / 2, CONTENT_W / 2],
            rowHeights=[64 * mm, 64 * mm],
        )
        boss_table.setStyle(
            TableStyle(
                [
                    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                    ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#EEE6D9")),
                    ("BOX", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBBEAA")),
                    ("INNERGRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#D8CCBA")),
                    ("TOPPADDING", (0, 0), (-1, -1), 4),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ]
            )
        )
        flowables.append(boss_table)
    return flowables


def markdown_to_story(markdown: str, styles):
    lines = markdown.splitlines()
    story = []
    index = 0
    first_h2 = True

    while index < len(lines):
        line = lines[index].rstrip()
        stripped = line.strip()

        if not stripped:
            index += 1
            continue
        if stripped.startswith("# "):
            index += 1
            continue
        if stripped == "---":
            story.append(HRFlowable(width="100%", thickness=0.6, color=BRONZE, spaceBefore=4 * mm, spaceAfter=4 * mm))
            index += 1
            continue
        if stripped.startswith("## "):
            if not first_h2:
                story.append(PageBreak())
            first_h2 = False
            story.append(Paragraph(inline_markup(stripped[3:]), styles["h2"]))
            story.append(HRFlowable(width="100%", thickness=1.1, color=BRONZE, spaceAfter=4 * mm))
            index += 1
            continue
        if stripped.startswith("### "):
            story.append(Paragraph(inline_markup(stripped[4:]), styles["h3"]))
            index += 1
            continue
        if stripped.startswith("#### "):
            story.append(Paragraph(inline_markup(stripped[5:]), styles["h4"]))
            index += 1
            continue
        if stripped.startswith("```"):
            code_lines = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(lines[index])
                index += 1
            index += 1
            story.append(Preformatted("\n".join(code_lines), styles["code"], maxLineLength=96))
            continue
        if stripped.startswith("|"):
            table_lines = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index].strip())
                index += 1
            story.append(parse_table(table_lines, styles))
            continue
        if stripped.startswith(">"):
            quote_lines = []
            while index < len(lines) and lines[index].strip().startswith(">"):
                quote_lines.append(lines[index].strip()[1:].strip())
                index += 1
            story.append(Paragraph(inline_markup(" ".join(quote_lines)), styles["quote"]))
            continue
        if re.match(r"^-\s+", stripped):
            items = []
            while index < len(lines) and re.match(r"^-\s+", lines[index].strip()):
                item_text = re.sub(r"^-\s+", "", lines[index].strip())
                items.append(ListItem(Paragraph(inline_markup(item_text), styles["bullet"]), leftIndent=4 * mm))
                index += 1
            story.append(
                ListFlowable(
                    items,
                    bulletType="bullet",
                    start="circle",
                    leftIndent=5 * mm,
                    bulletFontName="BookSans",
                    bulletFontSize=6.5,
                    bulletColor=CRIMSON,
                    spaceAfter=2 * mm,
                )
            )
            continue
        if re.match(r"^\d+\.\s+", stripped):
            items = []
            while index < len(lines) and re.match(r"^\d+\.\s+", lines[index].strip()):
                item_text = re.sub(r"^\d+\.\s+", "", lines[index].strip())
                items.append(ListItem(Paragraph(inline_markup(item_text), styles["bullet"]), leftIndent=5 * mm))
                index += 1
            story.append(
                ListFlowable(
                    items,
                    bulletType="1",
                    leftIndent=7 * mm,
                    bulletFontName="BookSans-Bold",
                    bulletFontSize=7,
                    bulletColor=CRIMSON,
                    spaceAfter=2 * mm,
                )
            )
            continue

        paragraph_lines = [stripped]
        index += 1
        while index < len(lines):
            nxt = lines[index].strip()
            if not nxt:
                break
            if (
                nxt.startswith("#")
                or nxt.startswith("|")
                or nxt.startswith(">")
                or nxt.startswith("```")
                or nxt == "---"
                or re.match(r"^-\s+", nxt)
                or re.match(r"^\d+\.\s+", nxt)
            ):
                break
            paragraph_lines.append(nxt)
            index += 1
        story.append(Paragraph(inline_markup(" ".join(paragraph_lines)), styles["body"]))

    return story


def create_pdf() -> Path:
    register_fonts()
    styles = make_styles()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    source_text = SOURCE.read_text(encoding="utf-8")

    document = BestiaryDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        title="Basic RPG Bosses — Бестиарий, том I",
        author="Basic RPG Classes project",
        subject="Боссы, биомы, логова, самоцветы и экипировка",
        creator="Codex / ReportLab",
        leftMargin=MARGIN_X,
        rightMargin=MARGIN_X,
        topMargin=MARGIN_TOP,
        bottomMargin=MARGIN_BOTTOM,
        allowSplitting=True,
    )

    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(
            "TOCLevel0",
            fontName="BookSerif-Bold",
            fontSize=9.2,
            leading=11.2,
            leftIndent=0,
            firstLineIndent=0,
            textColor=CRIMSON,
            spaceBefore=1.2 * mm,
        ),
        ParagraphStyle(
            "TOCLevel1",
            fontName="BookSerif",
            fontSize=7.6,
            leading=9.1,
            leftIndent=6 * mm,
            firstLineIndent=0,
            textColor=INK,
            spaceBefore=0.15 * mm,
        ),
    ]

    story = [
        Spacer(1, 46 * mm),
        Paragraph("BASIC RPG BOSSES", styles["cover_kicker"]),
        Paragraph("Бестиарий боссов", styles["cover_title"]),
        Paragraph("Том I", styles["cover_title"]),
        Spacer(1, 5 * mm),
        Paragraph(
            "Биомы · существа · логова · фазы боя<br/>суверенные самоцветы · доспехи · оружие",
            styles["cover_subtitle"],
        ),
        NextPageTemplate("Content"),
        PageBreak(),
        Paragraph("Содержание", styles["toc_title"]),
        toc,
    ]
    story.extend(build_reference_page(styles))
    story.extend(build_lizard_armor_reference_page(styles))
    story.append(PageBreak())
    story.extend(markdown_to_story(source_text, styles))

    document.multiBuild(story)
    return OUTPUT


if __name__ == "__main__":
    try:
        result = create_pdf()
        print(result)
    except Exception as exc:
        print(f"PDF build failed: {exc}", file=sys.stderr)
        raise
