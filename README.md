# Basic RPG

Basic RPG is a Minecraft mod that adds RPG classes, skills, custom combat, equipment, progression and party gameplay.

The mod is still in development. Some systems are already playable, while others are still being balanced or expanded.

## Contents

- [Requirements](#requirements)
- [Classes](#classes)
  - [Warrior](#warrior)
  - [Mage](#mage)
  - [Hunter](#hunter)
  - [Priest](#priest)
  - [Paladin](#paladin)
- [Skills and Progression](#skills-and-progression)
- [Combat](#combat)
- [Equipment](#equipment)
- [Party System](#party-system)
- [User Interface](#user-interface)
- [Installation](#installation)
- [Building from Source](#building-from-source)
- [Roadmap](#roadmap)
- [Bugs and Suggestions](#bugs-and-suggestions)
- [License](#license)

---

## Requirements

- Minecraft **26.2**
- NeoForge **26.2.0.59**
- Java **25**
- Basic RPG **1.0.0**

The mod is currently made for Minecraft 26.2. Other versions are not supported unless stated otherwise.

---

## Classes

There are currently five playable classes.

### Warrior

A melee class focused on staying close to the target and dealing pressure in close combat.

Current skills include:

- Battle Cry
- Berserk
- Execution
- Ultra Thrust
- Warrior Leap
- Warrior Whirlwind
- Shield Bash
- Warrior Vitality
- Vampirism

Warrior Leap was added to give the class a way to close distance instead of relying only on normal movement against ranged classes.

### Mage

A ranged spellcaster based around mana, burst damage and area control.

Current skills include:

- Fireball
- Blink
- Magic Shield
- Frost Nova
- Chain Lightning
- Meteor
- Mage Vitality
- Mage Glide
- Mana Regeneration

Mage can reposition with Blink and has several AoE abilities, but mana and cooldowns limit how often those skills can be used.

### Hunter

A ranged class built around bows and mobility.

Current skills include:

- Dash
- Windrun
- Camouflage
- Multishot
- Frost Arrows
- Arrow Rain
- Power Shot
- Hunter Vitality
- Fall Training
- Climbing
- Mana Regeneration
- Draw Speed
- Shot Power

Hunter has more movement options than the other classes, so mobility is one of the main areas that still needs PvP testing and balancing.

### Priest

A support class focused mainly on healing and utility.

Current skills include:

- Heal
- Restoration
- Healing Halo
- Cleanse
- Blessing
- Resurrection
- Holy Bolt
- Solar Beam
- Sky Rays
- Holy Storm
- Priest Vitality
- Mana Regeneration

Priest also has offensive holy abilities so the class can still be used outside group content.

### Paladin

A melee support and tank class using shields, defensive skills and holy abilities.

Current skills include:

- Whirlwind
- Ground Stun
- Fortify
- Provoke
- Paladin Heal
- Paladin Blessing
- Divine Bulwark
- Holy Shield
- Divine Slash
- Paladin Vitality
- Armor Training
- Mana Strike

---

## Skills and Progression

Each class has its own skill tree.

Skills can have:

- up to 15 ranks
- class-level requirements
- prerequisite skills
- prerequisite skill ranks
- mana costs
- cooldowns
- passive bonuses

Players spend skill points to unlock and improve skills instead of receiving every ability at once.

The progression system is already implemented, but level requirements, mana costs and cooldown values are still subject to balance changes.

---

## Combat

Basic RPG uses a separate combat system for class abilities.

The current system handles:

- active skills
- passive skills
- mana and other class resources
- cooldowns
- cast and hold abilities
- toggle abilities
- skill slots
- combat mode
- client/server synchronization
- skill effects and VFX

PvP is an important part of the mod.

The aim is to avoid situations where a fight is decided only by one strong ability or by one class having no way to respond to another.

Things such as positioning, aim, timing, movement and cooldown usage should matter.

PvE and PvP values may be different where needed. A skill that works well against mobs may need lower damage, shorter control duration or other changes when used against players.

---

## Equipment

Basic RPG has its own equipment rules and custom items.

Current weapon types include:

- swords
- greatswords
- warhammers
- rapiers
- bows
- staves
- shields

There is also support for:

- custom armor
- armor weight
- rings
- belts
- necklaces
- additional equipment slots
- class-based equipment restrictions

Some items already in the project:

- Iron Greatsword
- Iron Warhammer
- Iron Rapier
- Apprentice Staff
- Simple Shortbow
- Simple Recurve Bow
- Simple Longbow
- Mossfang Shortbow
- Duskstalker Longbow
- Oathkeeper Sword
- Forgeheart Greatsword
- Crimson Dragon Greatsword
- Frozen Serpent Staff
- Stormwing Staff
- Runic Bulwark
- Stormguard Shield
- Moon Eye Ring
- Thicket Heart Ring

There are also custom armor sets, crafting materials and boss-related items.

More equipment will be added later.

---

## Party System

The mod includes a party system for multiplayer.

Party information is stored between sessions and can be used by group abilities and support classes.

This is still being expanded for future group PvE and PvP content.

---

## User Interface

Basic RPG has its own interface for class-related systems.

It currently includes:

- class selection screen
- class progression screen
- RPG HUD
- combat HUD
- skill bar
- cooldown display
- custom equipment interface

English and Russian translations are included.

---

## Installation

1. Install Minecraft **26.2**.
2. Install NeoForge **26.2.0.59**.
3. Use Java **25**.
4. Download the Basic RPG `.jar`.
5. Put it into the Minecraft `mods` folder.
6. Start the game with the NeoForge profile.

For multiplayer, install the same mod version on both the server and all clients.

The Minecraft, NeoForge and Basic RPG versions should match.

---

## Building from Source

Clone the repository:

```bash
git clone https://github.com/cgerwyu/Basic-RPG.git
cd Basic-RPG
```

Run the client on Windows:

```bash
gradlew.bat runClient
```

Linux / macOS:

```bash
./gradlew runClient
```

Run the development server:

```bash
./gradlew runServer
```

Build the mod:

```bash
./gradlew build
```

On Windows:

```bash
gradlew.bat build
```

The compiled file will be created in:

```text
build/libs/
```

Java 25 is required.

---

## Roadmap

The main things that still need work are:

### Class Balance

- finish balance passes for all five classes
- review mana costs and cooldowns
- test skill progression at different levels
- improve weak or overly strong skills
- keep clear strengths and weaknesses for every class

### PvP

- improve melee vs ranged balance
- continue testing Warrior mobility
- balance Hunter mobility and Camouflage
- review Mage Blink and defensive tools
- balance healing and shields
- add better rules for repeated crowd control
- review multi-hit and AoE damage against players
- test 1v1 matchups
- test group PvP
- separate PvE and PvP values where needed

### PvE

- expand boss mechanics
- add more boss drops and materials
- improve boss interaction with crowd control
- add more PvE progression
- add more equipment

### Equipment

- more weapons
- more armor sets
- more accessories
- more unique item effects
- better equipment progression

### Visuals and UI

- improve skill VFX
- add more sounds
- improve cast feedback
- improve skill telegraphs
- improve hit feedback
- continue polishing the HUD and menus

---

## Bugs and Suggestions

If you find a bug or have an idea for the mod, feel free to send it.

This includes:

- bug reports
- balance problems
- skill issues
- multiplayer problems
- UI problems
- suggestions for existing systems
- ideas for new skills
- ideas for new classes
- new weapons or equipment
- PvE or boss ideas
- other feature requests

### GitHub Issues

https://github.com/cgerwyu/Basic-RPG/issues

### Email

**auliumdzhiev@gmail.com**

For bug reports, please include:

- Basic RPG version
- Minecraft version
- NeoForge version
- singleplayer or multiplayer
- class used
- skill or item involved
- steps to reproduce the problem

Logs or crash reports are also useful when available.

For balance feedback, it helps to mention both classes, equipment and the abilities involved.

---

## License

**All Rights Reserved.**

The source code and assets may not be redistributed or republished as an official or modified release of Basic RPG without permission.

---

Basic RPG is an independent Minecraft mod and is not affiliated with Mojang Studios or Microsoft.
