# Basic RPG

Basic RPG is a Minecraft mod that adds RPG classes, skills, custom combat, equipment, progression and multiplayer party gameplay.

The mod is still in active development. Most of the core systems are already implemented, but class balance, progression, PvP and some equipment systems are still being adjusted and expanded.

## Contents

- [Requirements](#requirements)
- [Classes](#classes)
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

The mod is currently developed for Minecraft 26.2. Other Minecraft or NeoForge versions are not officially supported unless stated otherwise.

---

## Classes

Basic RPG currently has five playable classes: Warrior, Mage, Hunter, Priest and Paladin. Each class has its own abilities, passives, progression and combat role.

### Warrior

Warrior is a melee class focused on close-range pressure and survivability. Its current skill set includes abilities such as Battle Cry, Berserk, Execution, Ultra Thrust, Warrior Leap, Whirlwind and Shield Bash, together with passive bonuses such as Vitality and Vampirism.

Warrior Leap gives the class a way to close distance against ranged opponents instead of relying entirely on normal movement.

### Mage

Mage is a ranged spellcaster based around mana, burst damage and area control. The class can use Fireball, Blink, Magic Shield, Frost Nova, Chain Lightning and Meteor, as well as passive effects such as Glide, Vitality and Mana Regeneration.

Mage has strong ranged tools and repositioning, but its stronger abilities are limited by mana costs and cooldowns.

### Hunter

Hunter is a ranged class built around bows, movement and positioning. Its abilities include Dash, Windrun, Camouflage, Multishot, Frost Arrows, Arrow Rain and Power Shot. The class also has several passive bonuses related to climbing, fall resistance, bow draw speed, shot power and resource regeneration.

Because Hunter has access to more movement options than most other classes, its mobility is one of the main areas that still needs PvP testing.

### Priest

Priest is primarily a support class with healing, cleansing and utility abilities. Heal, Restoration, Healing Halo, Blessing, Cleanse and Resurrection form the support side of the class, while Holy Bolt, Solar Beam, Sky Rays and Holy Storm provide offensive options.

The class is intended to remain useful outside of party content, so Priest is not limited to healing alone.

### Paladin

Paladin combines melee combat, defensive abilities and support. The class can use Fortify, Provoke, Ground Stun, Divine Bulwark, Holy Shield, Divine Slash and several healing or blessing abilities.

Paladin is intended to stay in close combat while protecting itself and supporting nearby players.

---

## Skills and Progression

Each class has its own progression tree. Skills can be unlocked and upgraded using skill points, and individual abilities can currently have up to 15 ranks.

Skills may also require a certain class level, another skill or a specific rank in a prerequisite skill before they become available. Active abilities use mana or other resources and can have cooldowns, while passive skills improve the character without requiring direct activation.

The basic progression system is already working, but level requirements, cooldowns, mana costs and scaling values are still subject to balance changes.

---

## Combat

Basic RPG adds its own combat layer for class abilities instead of relying only on vanilla attacks.

The system handles active and passive skills, mana and class resources, cooldowns, casting, toggle abilities, skill slots, combat mode and synchronization between the client and server. Many abilities also have their own visual effects and animations.

PvP is an important part of the mod. The aim is to avoid fights where one class wins simply because it has a single ability that the opponent cannot answer.

Positioning, aim, timing, movement and cooldown management should matter alongside equipment and character progression. Classes are meant to have different strengths and weaknesses, but each matchup should still provide ways for both players to react and counter each other.

Because abilities that work well in PvE can easily become too strong against players, PvP values may be balanced separately where necessary. Damage, crowd control, healing, shields and mobility are all systems that still require further testing.

---

## Equipment

Basic RPG has its own equipment system with class restrictions, additional equipment slots and custom weapon rules.

The mod already contains several weapon types, including swords, greatswords, warhammers, rapiers, bows, staves and shields. Weapons can use different attack profiles and handling rules instead of behaving exactly like vanilla items.

Custom armor and accessories are also supported. The equipment system includes armor weight and additional slots for items such as rings, belts and necklaces.

There are already several custom weapons and items in the project, including the Iron Greatsword, Iron Warhammer, Apprentice Staff, Mossfang Shortbow, Duskstalker Longbow, Oathkeeper Sword, Forgeheart Greatsword, Frozen Serpent Staff, Stormwing Staff, Runic Bulwark and Stormguard Shield.

More weapons, armor sets, accessories, crafting materials and progression tiers will be added as development continues.

---

## Party System

Basic RPG includes a multiplayer party system with persistent party data.

The system is intended to support group gameplay and abilities that interact with allies, especially for classes such as Priest and Paladin. It will also be used more heavily as group PvE and PvP content is expanded.

---

## User Interface

The mod has its own interface for class-related systems. Players can select a class, manage progression and use a custom RPG HUD during gameplay.

The HUD displays class-related information such as skills and cooldowns, while separate screens are used for class selection, progression and equipment.

English and Russian localization are currently included.

---

## Installation

Install Minecraft **26.2** together with NeoForge **26.2.0.59** and make sure the game is using Java **25**.

Download or build the Basic RPG `.jar`, place it inside the Minecraft `mods` folder and start the game using the NeoForge profile.

For multiplayer, the same version of Basic RPG should be installed on the server and on every connecting client. Minecraft, NeoForge and mod versions should match.

---

## Building from Source

Clone the repository:

```bash
git clone https://github.com/cgerwyu/Basic-RPG.git
cd Basic-RPG
```

On Windows, start the development client with:

```bash
gradlew.bat runClient
```

On Linux or macOS:

```bash
./gradlew runClient
```

A development server can be started with:

```bash
./gradlew runServer
```

To build the mod:

```bash
./gradlew build
```

On Windows:

```bash
gradlew.bat build
```

The compiled `.jar` will be created in `build/libs/`.

Java 25 is required for development.

---

## Roadmap

The main focus for the next stages of development is class and combat balance.

All five classes still need additional testing at different progression levels. Mana costs, cooldowns, damage scaling and passive bonuses will continue to change as more real combat situations are tested.

PvP requires particular attention. Melee classes need reliable ways to deal with ranged mobility, while ranged classes still need enough movement to maintain their identity without being able to avoid close combat indefinitely. Healing, shields, crowd control, multi-hit abilities and AoE damage also need separate PvP testing.

PvE development will continue alongside this. Boss mechanics, boss drops, equipment progression and interaction between crowd control and stronger enemies are planned to receive more work.

The equipment system will also continue to grow with additional weapons, armor sets, accessories and unique item effects.

Visual polish is another ongoing area. Skill effects, sounds, cast feedback, hit feedback, HUD elements and ability telegraphs will gradually be improved as the combat systems become more stable.

---

## Bugs and Suggestions

Bug reports, balance feedback and feature suggestions are welcome.

If you find a problem with a skill, item, multiplayer system, UI element or another part of the mod, you can open an issue on GitHub:

https://github.com/cgerwyu/Basic-RPG/issues

You can also contact me directly by email:

**auliumdzhiev@gmail.com**

For bug reports, it is useful to include the Basic RPG version, Minecraft version, NeoForge version and a short description of how to reproduce the problem. Logs or crash reports are helpful if the issue caused an error or crash.

For balance feedback, please mention which classes, equipment and abilities were involved. Suggestions for new skills, classes, equipment, bosses or other systems are also welcome.

---

## License

**All Rights Reserved.**

The source code and assets may not be redistributed or republished as an official or modified release of Basic RPG without permission.

Basic RPG is an independent Minecraft mod and is not affiliated with Mojang Studios or Microsoft.
