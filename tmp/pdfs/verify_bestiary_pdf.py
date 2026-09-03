from pathlib import Path

import pdfplumber
from pypdf import PdfReader

pdf_path = Path(__file__).resolve().parents[2] / "output" / "pdf" / "basic_rpg_bosses_bestiary_volume_1_ru.pdf"
reader = PdfReader(str(pdf_path))
texts = []
short_pages = []

with pdfplumber.open(str(pdf_path)) as document:
    for page_number, page in enumerate(document.pages, 1):
        text = page.extract_text() or ""
        texts.append(text)
        if len(text.strip()) < 20:
            short_pages.append(page_number)

all_text = "\n".join(texts)
required = [
    "Любой состав группы должен быть жизнеспособен",
    "Универсальная механика — шкала заземления",
    "рейд из одних мили-классов",
    "группой только из Воинов",
    "Пирокровное сердце дракона",
    "Кхарры и Тарр-Горак Первозуб",
    "Пращуритовое сердце Первозуба",
    "Пыль хищного нефрита",
    "Добыча босса без конфликтов и лишнего гринда",
    "восемь пращуритовых сердец, а не одно",
    "Целевой обязательный гринд равен трём убийствам",
    "Утверждённая тяжёлая броня кхарров",
    "Резной клык вожака",
    "Клеймо охоты",
    "Контракт восьмикадровой анимации",
    "Алмазная и незеритовая экипировка не являются эндгеймом",
]

print(f"pages={len(reader.pages)}")
print(f"title={reader.metadata.title}")
print(f"extracted_chars={len(all_text)}")
print(f"short_pages={short_pages}")
print(f"replacement_chars={all_text.count(chr(0xFFFD))}")
print(f"forbidden[фацет]={'фацет' in all_text.lower()}")
print(f"forbidden[суверенный гем]={'суверенный гем' in all_text.lower()}")
for phrase in required:
    print(f"required[{phrase}]={phrase in all_text}")
