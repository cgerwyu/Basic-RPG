# Basic RPG Classes — канонический исследовательский отчёт

Дата исследования: 2026-08-25  
Версия проекта: Minecraft/NeoForge 26.2, рабочая копия `basicrpgclasses-26.2`  
Назначение: внутренний источник для русского DOCX-отчёта и последующей реализации.

## 1. Область и критерии

Исследование отвечает на пять вопросов:

1. Почему часть навыков Охотника сейчас отвечает «Сейчас этот навык нельзя применить».
2. Как развести баланс PvE, формальных дуэлей, open-world PvP и массовых войн.
3. Как сделать личное умение важнее уровня, экипировки и безусловных кнопок.
4. Как перераспределить роли Воина, Мага и Охотника и подготовить Priest/Summoner.
5. Как не привязать PvE к конкретным ванильным мобам и поддержать будущий мод существ.

Главные критерии качества: наличие контригры, читаемые окна силы, отсутствие one-button kill, сохранение классовой асимметрии, пригодность для solo/party PvE, защита новичков, измеримость результата.

## 2. Проверенные предположения и ограничения

- «Идеальный» статический баланс недостижим: карта, состав, экипировка, задержка сети и мастерство меняют результат. Цель — управляемый живой процесс с порогами и телеметрией.
- Числа Dota 2, Guild Wars 2, WoW и League of Legends нельзя переносить напрямую в Minecraft. Переносятся принципы и виды ограничителей.
- В проекте пока нет нативного менеджера дуэлей/войн, отдельной стамины, бижутерии и data-driven профилей существ. Scoreboard-префиксы являются временным интеграционным крючком.
- Сильный рост от экипировки совместим с честным PvP только при разных правилах: полноценный рост в PvE, нормализация в арене, компрессия в open-world/GvG.

## 3. Аудит текущего кода

### 3.1. Подтверждённые отказы Охотника

`SkillExecutor.execute` возвращает `false`, после чего `ServerPayloadHandlers` всегда показывает общий ключ `skill_no_valid_effect`.

- `MULTISHOT`, `ARROW_RAIN`, `POWER_SHOT`, `FROST_ARROWS` требуют `HunterBowItem` именно в основной руке. Навык нельзя даже подготовить с луком во второй руке или после временной смены предмета.
- `DASH` возвращает `false`, если горизонтальная проекция взгляда близка к нулю, то есть камера смотрит почти строго вверх или вниз.
- Общая ошибка не сообщает, нужен ли лук, цель, щит или свободное место для телепорта.

### 3.2. Подтверждённые выбросы силы

- Охотник на 15-м ранге получает до `x2.2` скорости натяжения, `x1.6` урона стрел и `x1.225` скорости стрел только из пассивов.
- `DASH` растёт с `1.1` до `1.87` скорости и получает второй заряд; `WINDRUN` длится до 6 секунд и переходит в Speed II; карабканье не учитывает PvP-combat state.
- `MULTISHOT` создаёт до семи самостоятельных стрел без правила «не более одного попадания одной очередью по одной цели». Ограничение PvP-урона на одно попадание не ограничивает сумму одной очереди.
- `ARROW_RAIN` создаёт до 32 наносящих урон стрел и при одной цели снова выбирает её; нет бюджета урона одной способности по игроку.
- Fireball выпускает до трёх полноценных снарядов за одну цену. Meteor наносит до 32 raw damage в радиусе до 7 после 1.2 секунды предупреждения. Sky Rays продолжается 7 секунд и пульсирует каждые 0.4 секунды без настоящего канала.
- Магический щит даёт 5 секунд полной PvE-неуязвимости и одновременно массовое очищение; Fortify раздаёт группе Resistance и до 12 absorption HP.
- Пассив Мага увеличивает пул маны со 150 до 300, то есть удваивает его; пассивы здоровья дают до 25–41% базового HP. Это противоречит цели сделать экипировку главным источником роста.

### 3.3. Архитектурная причина

Текущий `PvpBalance` в основном применяет глобальный множитель `0.72` и cap `7 HP` на одно попадание. Это не ограничивает многострельные очереди, DoT, каналы и повторные AoE-пульсы. Баланс должен учитывать бюджет **одного применения навыка**, а не только одного damage event.

## 4. Реестр утверждений и доказательств

| ID | Утверждение | Доказательство и источник | Уверенность | Проектный вывод |
|---|---|---|---|---|
| C01 | PvE, arena PvP и mass PvP требуют отдельных профилей одного навыка | ArenaNet прямо разделяет ответственность и изменения между PvE, PvP и WvW: [Systems Team](https://www.guildwars2.com/en/news/updates-from-the-systems-team/), [GW2 update 2026-07-14](https://wiki.guildwars2.com/wiki/Game_updates/2026-07-14) | высокая | `CombatProfile`: PVE, DUEL/ARENA, WAR, OPEN_WORLD |
| C02 | Формальная арена должна минимизировать gear gap | GW2 нормализует уровень/экипировку; Blizzard исторически сжимала рост до малого бонуса: [GW2 PvP](https://wiki.guildwars2.com/wiki/SPvP), [WoW Legion gear](https://worldofwarcraft.blizzard.com/en-us/news/20119625/dev-watercooler-gearing-up-for-legion-pvp) | высокая | standard template в дуэлях; компрессия, а не отмена прогресса, в GvG/open world |
| C03 | Новичков защищает система режимов, а не только damage compensation | War Mode является opt-in и сохраняет sanctuary: [Blizzard War Mode](https://worldofwarcraft.blizzard.com/en-us/news/21901729), актуальный [new-player guide](https://worldofwarcraft.blizzard.com/en-us/news/24130393) | высокая | безопасные зоны, opt-in, защита после входа/respawn, anti-camp и diminishing rewards |
| C04 | У боссов нужна видимая шкала устойчивости к контролю | Регенерирующая Defiance bar позволяет координировать CC и открывать окно уязвимости: [ArenaNet Wyvern](https://www.guildwars2.com/en/news/meet-the-wyvern-in-guild-wars-2-heart-of-thorns/) | высокая | boss poise/break bar вместо полного stun immunity |
| C05 | Повторный PvP-контроль должен ослабевать | Blizzard сокращала/объединяла CC и требовала counterplay: [Pruning the Garden of War](https://worldofwarcraft.blizzard.com/en-us/news/13107743), [Dragonflight CC pass](https://worldofwarcraft.blizzard.com/en-us/news/23935248) | высокая для принципа | категорийный DR 100% → 50% → иммунитет, затем reset |
| C06 | Ресурс должен создавать паузы и цену промаха | Riot: мана ограничивает безопасный poke/heal и делает промах значимым; два ресурса нужны только если оба создают разные решения: [Ask Riot](https://www.leagueoflegends.com/en-us/news/dev/ask-riot-manaless-champions/) | высокая | Маг сохраняет ману; Охотник сначала получает одну общую выносливость для offense+mobility, а не дублирующие mana+stamina |
| C07 | Исследовательскую мобильность можно сохранить вне PvP-combat | В GW2 mounts/gliding имеют режимные и combat-state ограничения: [Mount](https://wiki.guildwars2.com/wiki/Mount), [Gliding](https://wiki.guildwars2.com/wiki/Gliding) | высокая для принципа | climbing доступен в PvE/out of combat, запрещён на аренах и после PvP-агрессии |
| C08 | Перегруженный класс обязан иметь явные слабости | Riot описывает strength/weakness budget Akshan, Blizzard — удаление ability bloat: [Riot 7/23](https://www.leagueoflegends.com/en-us/news/dev/quick-gameplay-thoughts-7-23/), [Blizzard pruning](https://worldofwarcraft.blizzard.com/en-us/news/13107743) | средне-высокая | убрать Heal из Мага; каждый класс получает 2–3 сильные оси, но не все сразу |
| C09 | Массовая поддержка требует cap и детерминированного приоритета | GW2 обычно ограничивает supportive effects пятью целями и приоритизирует party/subgroup: [Boons](https://wiki.guildwars2.com/wiki/Boons) | высокая | self → party → raid → прочие; cap 5 в competitive modes |
| C10 | Баланс измеряется отдельно по режиму и mastery bracket | Riot разделяет Average/Skilled/Elite/Pro и отдельно учитывает learning period: [Balance Framework](https://www.leagueoflegends.com/en-us/news/dev/dev-champion-balance-framework/) | высокая | раздельная телеметрия 1v1/GvG/PvE и по опыту класса |
| C11 | Дальник должен терять часть силы после успешного сближения | Marksmanship Drow отключается рядом с врагом; Gust — отдельный peel с КД: [Drow](https://www.dota2.com/hero/drowranger) | высокая | у Hunter штраф точности/draw и отключение части пассивного bonus в ближней зоне; один надёжный peel, не три бесплатных |
| C12 | Милишник получает ограниченный catch и сильное окно после контакта | Sven ловит Storm Hammer, Ursa накапливает Fury Swipes по одной цели: [Sven](https://www.dota2.com/hero/sven), [Ursa](https://www.dota2.com/hero/ursa) | высокая | Warrior не получает свободный teleport; получает точный intercept, короткий anti-mobility и payoff в melee |
| C13 | Большая магия должна быть предупреждаемой и дорогой | Invoker EMP/Meteor/Sun Strike имеют delay и setup requirements: [Invoker data](https://www.dota2.com/datafeed/herodata?hero_id=74&language=english) | высокая | Meteor: 2.5 секунды читаемого каста/телеграфа, прерывание, высокий процент маны |
| C14 | Хилер не обязан иметь почти нулевой урон, но его спасения требуют контригры | Oracle использует рискованные heal/damage/dispels; Healing Ward уничтожается одним ударом: [Oracle](https://www.dota2.com/hero/oracle), [Juggernaut](https://www.dota2.com/hero/juggernaut) | высокая | Priest получает низкий, но достаточный solo-PvE damage; сильный heal имеет cast/object/position counterplay |
| C15 | Саммонер требует жёстких лимитов и цены потери призыва | Chen имеет creature cap, Spirit Bear — leash/backlash/длинный resummon: [Chen](https://www.dota2.com/hero/chen), [Lone Druid](https://www.dota2.com/hero/lonedruid) | высокая | один основной summon, максимум 1–2 временных, leash, owner tradeoff, PvP target/AI limits |
| C16 | Будущих модовых существ нельзя хардкодить по классам Java | NeoForge tags работают для EntityType, data maps являются reloadable registry-object maps: [Tags](https://docs.neoforged.net/docs/resources/server/tags/), [Data Maps](https://docs.neoforged.net/docs/1.21.5/resources/server/datamaps/) | высокая | entity tags для категорий + data map с boss/poise/damage/heal multipliers |
| C17 | Более длинный TTK создаёт больше пространства для skill expression | Riot связывает durability с counterplay, positioning и cooldown management: [Durability Update](https://www.leagueoflegends.com/en-us/news/dev/quick-gameplay-thoughts-5-6/) | высокая | убрать one-combo kills; целевой median duel TTK 14–22 секунды |

## 5. Матрица пробелов второй волны

| Пробел | Проверка | Итог |
|---|---|---|
| Нужны ли Охотнику одновременно mana и stamina? | Riot о конкурирующих secondary bars; анализ текущего UI/ресурса | Нет на первом этапе. Одна выносливость должна оплачивать и побег, и усиленные выстрелы; два независимых пула ослабят выбор |
| Обязан ли Meteor быть неподвижным 3-секундным cast? | Live Invoker data | Источник подтверждает delay/telegraph/cost, но не неподвижный cast. Прерываемый 2.5-секундный cast — собственное решение под Minecraft |
| Можно ли полностью убрать урон Priest? | Oracle/Dazzle/Juggernaut | Нет: это испортит solo PvE. Нужен низкий sustained damage, но почти отсутствующий burst |
| Можно ли дать массовую неуязвимость? | Omniknight/target caps | Только против конкретного типа угрозы и с большим КД. Универсальная AoE invulnerability запрещена |
| Как поддержать будущий mob mod? | NeoForge tags/data maps | Категории и коэффициенты должны загружаться из datapack/data map; неизвестные сущности получают безопасный default |
| Достаточен ли cap одного попадания? | Локальный аудит multi-hit/channel skills | Нет. Нужен per-cast/per-target damage budget и dedup для очередей |

Поиск остановлен после насыщения: новые первичные источники повторяли те же модели и не меняли решения.

## 6. Целевая системная модель

### 6.1. Профили боя

- `PVE_NORMAL`: обычные мобы; полный class fantasy, исследовательская мобильность, AoE без жёсткого player cap.
- `PVE_BOSS`: тот же набор навыков, но hard CC превращается в poise damage, lifesteal/heal имеют boss multiplier.
- `PVP_DUEL`: стандартизированная экипировка/ранги, target cap 1, самые строгие burst и sustain budgets.
- `PVP_WAR`: компрессия экипировки, cap 5 для поддержки, AoE falloff и anti-stack правила.
- `PVP_OPEN_WORLD`: компрессия силы плюс opt-in, sanctuary, novice/spawn protection, anti-camp.

### 6.2. Бюджеты alpha-теста

- Median TTK равных дуэлянтов: 14–22 секунды.
- Instant non-ultimate: до 15% effective max HP; точный telegraphed skillshot: до 22%; полностью попавшая ultimate: до 35%.
- Суммарный урон одного применения по одной PvP-цели за 2 секунды: hard cap 40%, независимо от числа снарядов.
- Hard CC: обычно 0.5–0.9 секунды; DR одной категории `100% → 50% → immune`, reset 8 секунд. Mobility lock — отдельный soft-control, 1.25–1.75 секунды и не складывается.
- Support target cap: 5; приоритет self/party/squad/distance. Повторные источники одного buff используют strongest-only, не складываются.
- Duel gear: standard template, отклонение силы не более 5%. GvG/open-world: `effective bonus = 35%` бонуса сверх базового, caps около `+15% offense / +20% defense`.

### 6.3. Прогрессия

- В PvE экипировка даёт основную долю поздней силы: weapon/armor/jewelry, а не пассивы.
- Навык rank 1→15: примерно `+20–25%` к числовому эффекту, `−10–15%` cost/cooldown; новые механические пороги редки и имеют режимные caps.
- Пассив vitality: не более `+10–20%` базового значения к max rank. Пассив ресурса/regen: не более `+10–15%`; экипировка может дать существенно больше, но с diminishing returns.

## 7. Целевые роли классов

### Warrior

Сила: удержание melee, intercept, краткое окно против ranged pressure, стабильный sustain после контакта.  
Слабость: нет свободного escape/teleport; промах gap closer оставляет окно ответа.

- Shield Bash: узкий targeted intercept; после попадания 1.5 секунды mobility lock, без длинного stun.
- Iron Advance/Fortify: 4 секунды slow/knockback resistance и 40% ranged reduction только самому Warrior; 24–30 секунд КД.
- Ground Stun: читаемый cone; короткий hard CC в PvP, большой poise damage по boss.
- Whirlwind: AoE lifesteal разрешён, но PvP-heal имеет per-tick/per-cast cap.
- Provoke: PvE taunt; PvP Challenge снижает урон цели по остальным, раскрывает stealth, не управляет камерой.

### Mage

Сила: AoE, zone control, telegraphed burst, utility cleanse.  
Слабость: light defense, interruptible cast, ограниченный peel и mana.

- Heal удаляется из дерева Мага после миграции и переезжает в Priest.
- Magic Shield: `0.75–1.0 с` self invulnerability в PvP (`1.5 с` PvE), cleanse self + до 4/5 party targets; offensive lockout и длинный КД. Не 5 секунд.
- Blink: один базовый заряд; если второй оставлен talent/rank milestone, он получает escalating mana cost и общий mobility lockout.
- Fireball: один projectile либо несколько с постоянным total damage budget; никакого утроения полного урона за одну цену.
- Meteor: 2.5 секунды telegraph/cast, отмена от урона/существенного движения, 40–50% базового mana pool, большой КД; PvP damage cap.
- Sky Rays: настоящий channel: отмена при движении/уроне, поворот/сектор ограничен, число pulses и total budget фиксированы.

### Hunter

Сила: лучший repositioning и точный ranged pressure.  
Слабость: штраф в ближней зоне, одна общая выносливость для offense/escape, ограниченный peel.

- Ресурс называется Stamina/Выносливость, не Mana. Dash, Windrun, Climb, Camouflage и усиленные стрелы делят один пул.
- Dash: короче, без i-frame, с одним PvP-зарядом либо дорогим вторым; ошибка вертикального взгляда устранена.
- Windrun: Speed I на короткое окно, не физическая неуязвимость; высокий stamina drain.
- Climbing: полностью работает в PvE/out of combat; запрещён на аренах и в PvP-combat, на стене нельзя стрелять.
- Camouflage: 0.6 секунды входа, breaking on damage/deal/cast, −20% speed, reveal на малой дистанции/от Warrior Challenge.
- Multishot: одна очередь поражает каждую цель не более одного раза; это clear/AoE, не point-blank shotgun.
- Arrow Rain: один PvP-hit budget на цель за cast; PvE boss получает отдельный cap/коэффициент.
- Power Shot: 1.0–1.5 секунды charge, видимый tracer, прерывание; высокий урон только за попадание.

## 8. Новые классы

### Priest — следующий класс

Роль: dedicated healer/cleanser, низкий sustained damage, почти отсутствующий burst. Минимальный solo-PvE цикл обязателен.

Предлагаемый core kit: Smite, Mend (cast heal), Renew (HoT), Purify, Barrier (типовая защита, не универсальная invulnerability), Sanctuary (уничтожаемый/контролируемый объект или зона), долгий combat resurrection только для PvE/war rules. В PvP overheal не превращается в бесконечный shield; repeated AoE heals получают target cap и dampening.

### Summoner — после Priest и boss API

Роль: управление позицией одного ключевого существа. Один основной summon, максимум один временный; leash, command cooldown, reduced PvP damage, AoE resistance без полной immunity, backlash или длинный resummon при смерти. Приручение неизвестных boss/entity types запрещено по умолчанию и управляется data map.

## 9. План реализации

1. Hotfix: убрать ложные bow-in-main-hand preconditions у подготовляемых навыков; сделать Dash устойчивым к вертикальному взгляду; понятные ошибки.
2. Safety pass: same-volley target dedup, Arrow Rain PvP per-cast cap, более короткий Shield, Meteor delay/interruption, ослабление чрезмерных пассивов.
3. `CombatProfile` и единый `BalanceResolver` вместо разбросанных `instanceof ServerPlayer`.
4. Перевести skill values в reloadable datapack registry/data maps; добавить entity categories и boss poise profile.
5. Реализовать Arena/Duel manager с standard template, затем Guild War manager и open-world opt-in.
6. Миграция Heal из Mage в Priest; после этого Summoner.
7. Телеметрия и повторяющийся balance cadence.

## 10. Метрики принятия

- 1v1: matchup win rate при равном шаблоне в коридоре 45–55% после достаточной выборки; отдельно novice и mastered brackets.
- Median TTK 14–22 секунды; доля смертей быстрее 4 секунд — исключение, связанное с подтверждённой ошибкой/полным telegraphed combo.
- Hunter: escape success не выше конкурентов одновременно с top damage; stamina starvation после полного mobility chain.
- Warrior: успешный intercept заметно повышает вероятность выиграть следующие 3–5 секунд, но промах наказывается.
- Mage: доля прерванных больших кастов и hit rate Meteor измеряются; полный hit силён, но не убивает с полного HP.
- GvG: ни один класс не занимает более 45% оптимального состава; поддержка не масштабируется линейно от stack нескольких Priest.
- PvE: solo boss TTK damage-классов отличается не более чем на 15%; Warrior/Priest компенсируют меньший DPS полезностью и выживаемостью; boss break contribution видима.
- Новички: повторные смерти от одного veteran в коротком окне близки к нулю вне добровольного War Mode.

