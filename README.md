# Divine: Skies

A RuneLite plugin that adds a configurable day/night sky cycle to Old School RuneScape.

## Features

**Day/Night Cycle** — Smooth skybox color transitions through day, sunset, night, and sunrise phases. Supports both a configurable timer mode and real local clock sync.

**117 HD Integration** — Applies brightness and contrast offsets relative to your base 117 HD settings during each phase. Works without 117 HD installed (sky color changes only, no brightness/contrast).

**Scene Tint Overlay** — Optional fullscreen color filter that tints the entire game view during each phase. Supports both cycle-following and fixed color modes.

**Underground Detection** — Automatically switches to a configurable underground sky color when entering caves and dungeons. Covers 20+ known underground areas using region ID matching, instance detection, and Y-coordinate fallback.

**Player-Owned House Skybox** — Dedicated sky color and lighting settings for POH instances, independent of the main day/night cycle.

**On-Screen Overlay** — Displays current phase, time remaining, and progress. Supports a condensed single-line mode and shows local time in clock mode.

**Developer Mode** — Debug overlay showing current region IDs, instance status, and plane for community members to report areas that need detection adjustments.

## Configuration

| Section | What it controls |
|---|---|
| Cycle Timing | Enable/disable, clock sync vs timer mode, day/night durations, transition speed |
| Sky Colors | Day, night, sunrise, sunset, and underground skybox colors |
| Phase Tints | Optional per-phase color filters over the game viewport |
| Brightness & Contrast | Per-phase offsets applied to 117 HD base values |
| Player-Owned House | Independent sky color, brightness, and contrast for POH |
| Overlay | Position, style, condensed mode |
| Advanced | Override 117 HD environments, developer mode |

## Installation

Search for **Divine: Skies** in the RuneLite Plugin Hub, or find it under the "skycycle" tag.

## Compatibility

- Works standalone for sky color changes
- Integrates with [117 HD](https://runelite.net/plugin-hub/show/117hd) for brightness, contrast, and environment overrides
- Does not require 117 HD to be active, only installed

## Support

Found a bug or a location where underground/POH detection is wrong? Open an issue on [GitHub](https://github.com/GarnetDivine/divine-skies/issues) and include the region ID from developer mode if possible.

## Credits

**Author:** Garnet Divine
**Studio:** O3 Studios

Licensed under the BSD 2-Clause License.