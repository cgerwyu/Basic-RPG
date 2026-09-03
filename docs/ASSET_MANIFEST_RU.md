# Basic RPG Classes — точный манифест графики

Этот файл является рабочим списком графики для первой версии мода классов. Все имена уже совпадают с ID предметов в коде. Используйте только нижний регистр, английские буквы, цифры и `_`.

## Куда сохранять

- Исходники Blockbench (`.bbmodel`): `art/blockbench/`. Эту папку можно создать в корне проекта.
- Готовые модели предметов: `src/main/resources/assets/basicrpgclasses/models/item/`.
- Готовые модели блоков: `src/main/resources/assets/basicrpgclasses/models/block/`.
- Текстуры предметов: `src/main/resources/assets/basicrpgclasses/textures/item/`.
- Текстуры блоков: `src/main/resources/assets/basicrpgclasses/textures/block/`.
- Интерфейс: `src/main/resources/assets/basicrpgclasses/textures/gui/`.
- Иконки навыков: `src/main/resources/assets/basicrpgclasses/textures/gui/skills/<class>/`.

В проекте уже лежат временные модели, использующие ванильную графику. Поэтому мод запускается без ваших PNG. Экспортированные из Blockbench JSON нужно класть поверх одноимённых файлов.

## Настройки Blockbench

Для оружия и предметов выбирайте формат `Java Block/Item`. Не используйте формат Bedrock Entity: он не подходит обычной модели предмета без отдельного рендера.

- Базовый масштаб текстур оружия: `32x32` или `64x64`. Для аккуратного пиксель-арта предпочтительнее `32x32`.
- Пиксели должны оставаться резкими: без сглаживания и полупрозрачной каймы.
- Центр рукояти располагайте около стандартной точки удержания Minecraft.
- Проверьте отображение `Third Person Right Hand`, `First Person Right Hand`, `GUI`, `Ground` и `Fixed`.
- Геометрия может выходить за условный куб 16x16, но не делайте оружие чрезмерно большим в GUI.
- Для двуручного оружия пока достаточно одной предметной модели. Анимация второй руки и мувсеты будут сделаны кодом позже.

## Обязательные 3D-модели оружия

### Маг

1. `models/item/apprentice_staff.json`
2. `textures/item/apprentice_staff.png`
3. Исходник: `art/blockbench/apprentice_staff.bbmodel`

Внешность: простой деревянный посох длиной примерно 1.4–1.6 блока. Тёмно-дубовая рукоять, железное кольцо или скоба в верхней части, небольшой необработанный аметист. Это дешёвый учебный предмет, поэтому без золота, сложной филиграни и огромного кристалла.

### Воин

1. `models/item/iron_rapier.json`
2. `textures/item/iron_rapier.png`
3. Исходник: `art/blockbench/iron_rapier.bbmodel`

Внешность: длинный очень узкий прямой клинок, острый кончик, чашевидная или крестовая гарда, тонкая рукоять. Силуэт должен сразу читаться как колющее оружие, а не короткий меч.

1. `models/item/iron_greatsword.json`
2. `textures/item/iron_greatsword.png`
3. Исходник: `art/blockbench/iron_greatsword.bbmodel`

Внешность: широкий двуручный клинок примерно в 1.5 раза массивнее ванильного меча, длинная рукоять под две руки, простая железная гарда. Это базовый железный меч, не фэнтезийный артефакт.

1. `models/item/iron_warhammer.json`
2. `textures/item/iron_warhammer.png`
3. Исходник: `art/blockbench/iron_warhammer.bbmodel`

Внешность: длинное древко, тяжёлая прямоугольная ударная головка, на обратной стороне небольшой клюв. Масса должна быть визуально сосредоточена в головке. Не рисуйте огромный каменный молот размером с игрока.

### Охотник

1. `models/item/hunting_knife.json`
2. `textures/item/hunting_knife.png`
3. Исходник: `art/blockbench/hunting_knife.bbmodel`

Внешность: короткий однолезвийный клинок, кожаная рукоять, маленькая гарда. Он должен быть заметно короче рапиры и ванильного меча.

## Луки: обязательные состояния натяжения

Каждому луку нужны четыре модели и четыре текстуры. Геометрия и тетива должны последовательно показывать натяжение. Имена менять нельзя: файл `items/<bow>.json` уже переключает эти состояния во время удержания ПКМ.

### Короткий лук

- `models/item/simple_shortbow.json`
- `models/item/simple_shortbow_pulling_0.json`
- `models/item/simple_shortbow_pulling_1.json`
- `models/item/simple_shortbow_pulling_2.json`
- `textures/item/simple_shortbow.png`
- `textures/item/simple_shortbow_pulling_0.png`
- `textures/item/simple_shortbow_pulling_1.png`
- `textures/item/simple_shortbow_pulling_2.png`
- Исходник: `art/blockbench/simple_shortbow.bbmodel`

Внешность: короткие плечи, простое светлое дерево, минимум металлических деталей. Силуэт компактный. Это самый мобильный и быстро натягиваемый лук, поэтому он не должен выглядеть тяжёлым.

### Рекурсивный лук

- `models/item/simple_recurve_bow.json`
- `models/item/simple_recurve_bow_pulling_0.json`
- `models/item/simple_recurve_bow_pulling_1.json`
- `models/item/simple_recurve_bow_pulling_2.json`
- `textures/item/simple_recurve_bow.png`
- `textures/item/simple_recurve_bow_pulling_0.png`
- `textures/item/simple_recurve_bow_pulling_1.png`
- `textures/item/simple_recurve_bow_pulling_2.png`
- Исходник: `art/blockbench/simple_recurve_bow.bbmodel`

Внешность: концы плеч заметно выгнуты вперёд, дерево темнее, на рукояти небольшие железные усиления. Это сбалансированный средний вариант: средняя скорость натяжения, дальность и точность.

### Длинный лук

- `models/item/simple_longbow.json`
- `models/item/simple_longbow_pulling_0.json`
- `models/item/simple_longbow_pulling_1.json`
- `models/item/simple_longbow_pulling_2.json`
- `textures/item/simple_longbow.png`
- `textures/item/simple_longbow_pulling_0.png`
- `textures/item/simple_longbow_pulling_1.png`
- `textures/item/simple_longbow_pulling_2.png`
- Исходник: `art/blockbench/simple_longbow.bbmodel`

Внешность: почти в рост игрока, длинная плавная дуга без сложного изгиба, толстая центральная рукоять. Визуально самый мощный, точный и медленный из трёх.

Если удобнее, четыре состояния одного лука можно держать в одном `.bbmodel` как отдельные группы, а при экспорте сохранять под указанными именами.

## Классовый верстак

Модель:

- `models/block/class_workbench.json`
- Исходник: `art/blockbench/class_workbench.bbmodel`

Текстуры:

- `textures/block/class_workbench_top.png`
- `textures/block/class_workbench_front.png`
- `textures/block/class_workbench_side.png`
- `textures/block/class_workbench_bottom.png`

Каждая текстура `16x16` или `32x32`, но все четыре одного разрешения.

Внешность: усиленный ремесленный стол в ванильном стиле. Сверху — сетка или три рабочих секции с маленькими символами меча, посоха и лука. Спереди — железная скоба и выдвижной ящик. Сбоку — ремни, инструменты или держатели. Не рисуйте магический алтарь: этот блок должен подходить всем трём классам.

Экспортированная модель должна использовать именно перечисленные пути текстур. Предметная форма блока уже ссылается на `models/block/class_workbench.json`, отдельная модель в `models/item/` не нужна.

## Плоские иконки предметов

Эти предметы лучше рисовать как обычные пиксельные иконки `16x16` или `32x32`, с прозрачным фоном. Blockbench для них необязателен. Пока код использует ванильные заглушки; после добавления PNG модели будут переключены на эти текстуры.

### Универсальные заготовки

- `textures/item/amethyst_lens.png` — круглая огранённая фиолетовая линза в тонкой железной оправе; компонент магических предметов.
- `textures/item/defenders_mark.png` — небольшая железная эмблема щита с бронзовой окантовкой; знак воина-защитника.
- `textures/item/quiver_clasp.png` — металлическая застёжка с кожаным ремешком и маленьким символом стрелы; компонент охотника.
- `textures/item/wind_feather.png` — светлое перо с бирюзовым воздушным следом; предмет мобильности охотника.
- `textures/item/ender_charm.png` — маленький тёмный амулет с зелёно-фиолетовой сердцевиной; компонент телепортации мага.

### Командные крафты классов

- `textures/item/armor_reinforcement_kit.png` — сложенные железные пластины, заклёпки и короткий кожаный ремень. Крафт воина для усиления брони союзников.
- `textures/item/sharpening_stone.png` — серый точильный брусок с короткой искрой на краю. Крафт воина для временного усиления оружия.
- `textures/item/simple_poison.png` — маленький зелёный флакон с пробкой и символом капли. Крафт охотника, передаваемый союзникам.
- `textures/item/healing_crystal.png` — светло-бирюзовый кристалл с белым бликом или маленьким крестом. Крафт мага для поддержки команды.
- `textures/item/conjured_ration.png` — свёрток еды в синеватой магической оболочке. Созданная магом пища, не роскошный обычный хлеб.

## Интерфейс: вторая очередь

Текущий HUD рисуется кодом и работает без PNG. Эти файлы понадобятся для финального пиксельного MMORPG-оформления, но их можно рисовать уже сейчас.

- `textures/gui/hud/player_frame.png` — рамка `160x48`, прозрачный центр, место `36x36` слева под голову игрока.
- `textures/gui/hud/target_frame.png` — рамка `176x36` для имени и HP цели.
- `textures/gui/hud/skill_slot.png` — обычный слот `32x32`; код выводит его примерно в размере `30x30`.
- `textures/gui/hud/skill_slot_selected.png` — выбранный слот `32x32`, яркая золотистая рамка.
- `textures/gui/hud/skill_slot_cooldown.png` — необязательная белая полупрозрачная маска `32x32`. Текущие радар, цифры и вращающаяся стрелка рисуются кодом и работают без неё.
- `textures/gui/hud/health_bar.png` — красная заполняемая полоска `128x8`.
- `textures/gui/hud/mana_bar.png` — синяя заполняемая полоска `128x8`.
- `textures/gui/classes/warrior.png` — иконка `32x32`: меч перед щитом, красно-бронзовая гамма.
- `textures/gui/classes/mage.png` — иконка `32x32`: посох или кристалл со звездой, сине-фиолетовая гамма.
- `textures/gui/classes/hunter.png` — иконка `32x32`: лук со стрелой, зелёно-коричневая гамма.

Рамки должны напоминать интерфейс Minecraft: прямые пиксельные границы, тёмный полупрозрачный фон, без гладких мобильных градиентов.

## Иконки навыков

Первый набор навыков уже подключён к игровым эффектам. Пока HUD показывает цвет и короткое буквенное обозначение, поэтому PNG остаются необязательными. Все иконки рисуются `32x32` с прозрачным фоном, без текста и цифр: стоимость маны, клавишу и cooldown поверх добавляет код.

### Уже используемые точные имена

- `textures/gui/skills/warrior/whirlwind.png` — круговой след тяжёлого клинка вокруг центра.
- `textures/gui/skills/warrior/fortify.png` — щит или нагрудник с золотистым защитным свечением.
- `textures/gui/skills/warrior/provoke.png` — шлем и расходящиеся красно-оранжевые волны крика.
- `textures/gui/skills/warrior/ground_stun.png` — удар оружия в землю; от центра вперёд расходится широкий конус трещин и каменной пыли.
- `textures/gui/skills/warrior/shield_bash.png` — щит в момент сильного удара, перед ним короткая белая вспышка и две звезды оглушения.
- `textures/gui/skills/warrior/battle_cry.png` — красный силуэт воина с поднятым оружием и расходящимися волнами боевого крика.
- `textures/gui/skills/warrior/warrior_vitality.png` — красное сердце перед тяжёлым стальным щитом, вокруг короткие всполохи ярости.
- `textures/gui/skills/warrior/warrior_vampirism.png` — бордовая капля крови поверх лезвия, за ней небольшое красное сердце; без изображения клыков.
- `textures/gui/skills/mage/fireball.png` — яркая огненная сфера с коротким хвостом.
- `textures/gui/skills/mage/heal.png` — сердце в зелёно-белом магическом свете.
- `textures/gui/skills/mage/blink.png` — две фиолетовые точки, соединённые быстрым следом.
- `textures/gui/skills/mage/magic_shield.png` — сине-фиолетовая прозрачная сфера вокруг белого силуэта мага; по краю сферы несколько ярких рун.
- `textures/gui/skills/mage/mage_vitality.png` — большое синее ядро маны внутри светлого сердца с фиолетовой каймой.
- `textures/gui/skills/mage/mage_glide.png` — силуэт мага над землёй с двумя направленными назад фиолетовыми потоками энергии; не рисуйте крылья или элитры.
- `textures/gui/skills/mage/mage_mana_regen.png` — синяя капля маны, вокруг которой по кругу движутся две бирюзовые дуги.
- `textures/gui/skills/mage/frost_nova.png` — голубая ледяная вспышка, расходящаяся кругом от белого центра.
- `textures/gui/skills/mage/meteor.png` — раскалённый метеорит, падающий по диагонали с огненным хвостом.
- `textures/gui/skills/mage/sky_rays.png` — три голубых вертикальных луча, сходящих с облака на тёмные силуэты целей.
- `textures/gui/skills/mage/chain_lightning.png` — синяя молния, последовательно соединяющая три маленькие точки-цели.
- `textures/gui/skills/hunter/dash.png` — сапог или силуэт с бирюзовым скоростным следом.
- `textures/gui/skills/hunter/windrun.png` — бегущий силуэт, окружённый зелёными потоками воздуха.
- `textures/gui/skills/hunter/camouflage.png` — полупрозрачный капюшон, растворяющийся в зелёном дыме.
- `textures/gui/skills/hunter/hunter_vitality.png` — зелёное сердце с голубой каплей маны и маленьким пером сбоку.
- `textures/gui/skills/hunter/hunter_fall_training.png` — охотничий сапог, мягко приземляющийся на изогнутую воздушную волну.
- `textures/gui/skills/hunter/hunter_climbing.png` — перчатка, цепляющаяся за каменный уступ, с маленькой стрелкой вверх.
- `textures/gui/skills/hunter/multishot.png` — пять стрел, расходящихся широким веером.
- `textures/gui/skills/hunter/arrow_rain.png` — несколько стрел, вертикально падающих из светлого облака в круг на земле.
- `textures/gui/skills/hunter/power_shot.png` — крупная стрела внутри яркого голубого пробивающего потока.
- `textures/gui/skills/hunter/frost_arrows.png` — наконечник стрелы, покрытый голубым льдом и снежинками.
- `textures/gui/skills/hunter/hunter_mana_regen.png` — голубая капля маны внутри зелёного охотничьего круга с двумя короткими вращающимися дугами.

Сам летящий `mage_fireball` пока использует ванильную объёмную модель огненного заряда с увеличенным масштабом и не требует отдельной текстуры. Позже её можно заменить собственным renderer/texture, не меняя механику навыка.

### Остальные будущие иконки Воина — `textures/gui/skills/warrior/`

- `forward_stun.png` — меч или молот, ударяющий цель спереди.
- `cleansing_shout.png` — белая звуковая волна, разбивающая фиолетовые капли дебаффа.
- `fortitude.png` — сердце за щитом.
- `heavy_weapon_training.png` — скрещённые двуручный меч и молот.
- `attack_speed_training.png` — три последовательных следа клинка.

### Остальные будущие иконки Мага — `textures/gui/skills/mage/`

- `frostbolt.png` — ледяной снаряд.
- `arcane_bolt.png` — фиолетовая магическая стрела.
- `group_cleanse.png` — белое кольцо, очищающее фиолетовые капли.
- `resurrection.png` — светлый силуэт, поднимающийся вверх.
- `conjure_food.png` — хлеб в голубом магическом круге.
- `staff_light.png` — наконечник посоха, излучающий свет.
- `mana_barrier.png` — синяя полусфера перед персонажем.
- `mana_surge.png` — переполненный синий кристалл.
- `healing_mastery.png` — посох и бело-зелёная руна.

### Остальные будущие иконки Охотника — `textures/gui/skills/hunter/`

- `side_step.png` — две стрелки в стороны вокруг силуэта.
- `double_jump.png` — два крыла или две направленные вверх стрелки.
- `explosive_arrow.png` — стрела с оранжевым взрывом.
- `fire_arrow.png` — горящий наконечник.
- `poison_arrow.png` — зелёная капля на наконечнике.
- `bleeding_blades.png` — два ножа и красная капля.
- `evasion.png` — силуэт, уходящий от белого следа удара.
- `scavenger.png` — открытый мешок с добычей и маленькой стрелой вверх.

## Сейчас не рисовать

- Катану: она будет оружием отдельного класса «Самурай» и потребует ножен, состояний `sheathed/drawn` и своих анимаций.
- Броню и оружие боссов: это отдельный будущий мод/модуль боссов, чтобы `basicrpgclasses` не зависел от биомов и существ.
- Модели самих боссов, строений и биомов.
- Анимации игрока: текущая архитектура мувсетов сначала должна определить кости и способ рендера рук.

## Минимальная первая поставка от художника

Чтобы быстро заменить главные заглушки, достаточно сначала подготовить:

1. `apprentice_staff` — JSON + PNG.
2. `iron_rapier` — JSON + PNG.
3. `iron_greatsword` — JSON + PNG.
4. `iron_warhammer` — JSON + PNG.
5. `hunting_knife` — JSON + PNG.
6. Один полный лук из четырёх состояний — лучше `simple_recurve_bow`.
7. Четыре текстуры и JSON классового верстака.

После этой поставки можно проверить масштаб, положение в руке и общий стиль, а затем рисовать остальные луки и плоские иконки без риска переделывать весь набор.
