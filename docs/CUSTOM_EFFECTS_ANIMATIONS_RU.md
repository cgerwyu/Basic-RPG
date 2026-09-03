# Кастомные 3D-эффекты и анимации

## Что реализовано

Боевые эффекты больше не строятся из сотен ванильных частиц. Сервер отправляет
компактное описание эффекта, а клиент создаёт анимированную fullbright-геометрию:

- яркое белое ядро;
- окрашенный внутренний слой;
- несколько широких полупрозрачных ореолов;
- пересекающиеся плоскости, поэтому эффект выглядит объёмным с разных сторон;
- вращение, расширение, пульсация и плавное исчезновение;
- привязка к игроку или летящему снаряду без пакета частиц каждый тик.

Главные файлы:

- `src/main/java/net/cgerwyu/basicrpgclasses/skill/SkillParticleEffects.java` — серверный запуск VFX;
- `src/main/java/net/cgerwyu/basicrpgclasses/skill/SkillVfxType.java` — набор переиспользуемых форм;
- `src/main/java/net/cgerwyu/basicrpgclasses/network/payload/SkillVfxPayload.java` — синхронизация;
- `src/main/java/net/cgerwyu/basicrpgclasses/client/ClientSkillVfx.java` — клиентский 3D-рендер;
- `src/main/java/net/cgerwyu/basicrpgclasses/client/render/MageFireballRenderer.java` — отдельное светящееся ядро Fireball.

На новый слой переведены активные эффекты мага, воина и охотника: Sky Rays,
Chain Lightning, Meteor, Fireball, Heal, Blink, Magic Shield, Frost Nova,
Whirlwind, Fortify, Provoke, Ground Stun, Shield Bash, Battle Cry, Dash,
Windrun, Camouflage, Multishot, Frost Arrows, Power Shot и Arrow Rain.
Парение, карабканье, вампиризм и попадание ледяной стрелы тоже получили
короткие визуальные сигналы. Постоянные пассивки здоровья/маны намеренно не
рисуют эффект каждую секунду, чтобы не создавать визуальный шум.

## Текстуры

Используемые PNG находятся здесь:

`src/main/resources/assets/basicrpgclasses/textures/vfx/`

- `soft_glow.png` — горячее ядро и мягкий ореол;
- `energy_streak.png` — луч, след, молния и ветер;
- `magic_ring.png` — зона, щит и индикатор каста;
- `slash_arc.png` — режущая дуга Whirlwind.

Все четыре текстуры нейтральные бело-голубые: код окрашивает их под конкретный
навык. Их можно заменить своими PNG без изменения Java-кода. Следует сохранить
имя файла, прозрачный фон и свободный от рисунка отступ по краям. Для итогового
ресурса достаточно 256×256 или 512×512; для длинной ленты подходит 512×128 или
1024×256.

## Как проверить все формы

В мире с разрешёнными командами выполнить:

`/brc_test_vfx`

Команда показывает рядом с игроком щит, вращающиеся разрезы, лечение, лёд,
кастомную молнию, Sky Rays, полёт метеора и сотрясение земли. Бесконечный ресурс
для проверки настоящих навыков по-прежнему включается командой:

`/brc_infinite_mana true`

## Почему это не datapack

Datapack Java Edition не умеет регистрировать собственный renderer, новый
RenderType или fullbright-геометрию. Он может лишь вызывать уже существующие
частицы. Поэтому логика находится в NeoForge-коде, а PNG — в ресурсах мода.
Отдельный datapack для этих эффектов не нужен.

`entityTranslucentEmissive` делает эффект самосветящимся и видимым в темноте.
Несколько альфа-слоёв дают ореол без дополнительных модов. Настоящий экранный
bloom и освещение соседних блоков, как на референсах с шейдерами, являются
постобработкой shader pack. Совместимый shader pack усилит уже подготовленные
яркие области, но эффект остаётся читаемым и без него.

## Анимации игрока из Blockbench

Текстурированный VFX и скелетная анимация игрока — разные системы. Исходные
`.bbmodel` следует хранить в:

`art/blockbench/player_animations/<класс>_<навык>.bbmodel`

Будущие экспортированные runtime-файлы:

`src/main/resources/assets/basicrpgclasses/animations/player/<класс>/<навык>.animation.json`

Имена первых клипов:

- `animation.basicrpgclasses.warrior.whirlwind`
- `animation.basicrpgclasses.warrior.ground_stun`
- `animation.basicrpgclasses.warrior.shield_bash`
- `animation.basicrpgclasses.warrior.battle_cry`
- `animation.basicrpgclasses.mage.cast_staff`
- `animation.basicrpgclasses.mage.meteor`
- `animation.basicrpgclasses.mage.chain_lightning`
- `animation.basicrpgclasses.hunter.dash`
- `animation.basicrpgclasses.hunter.power_shot`
- `animation.basicrpgclasses.hunter.arrow_rain`

Одного JSON недостаточно для проигрывания анимации ванильного игрока. После
выбора совместимого с NeoForge 26.2 animation runtime его нужно подключить к
`ClientSkillVisuals`, `ClientPayloadHandlers` и существующим сетевым событиям.
