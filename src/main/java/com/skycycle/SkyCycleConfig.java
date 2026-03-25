/*
 * Divine: Skies - A day/night sky cycle plugin for RuneLite
 * Copyright (c) 2025, O3 Studios / Garnet Divine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the above copyright notice
 * and this permission notice appear in all copies or substantial portions
 * of the software.
 *
 * @author Garnet Divine
 * @organization O3 Studios
 */

package com.skycycle;

import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup(SkyCycleConfig.CONFIG_GROUP)
public interface SkyCycleConfig extends Config
{
    String CONFIG_GROUP = "skycycle";

    // =============================================
    // Cycle Timing
    // =============================================

    @ConfigSection(
        name = "Cycle Timing",
        description = "Configure the day/night cycle durations.",
        position = 0
    )
    String timingSection = "timingSection";

    @ConfigItem(
        keyName = "enableCycle",
        name = "Enable Day/Night Cycle",
        description = "Toggle the automatic day/night sky cycle on or off.",
        section = timingSection,
        position = 0
    )
    default boolean enableCycle()
    {
        return true;
    }

    @ConfigItem(
        keyName = "syncToLocalClock",
        name = "Sync to Local Clock",
        description = "Day and night follow your real local time instead of the timer.",
        section = timingSection,
        position = 1
    )
    default boolean syncToLocalClock()
    {
        return false;
    }

    // --- Timer mode ---

    @Range(min = 1, max = 120)
    @ConfigItem(
        keyName = "dayDuration",
        name = "Day Duration (minutes)",
        description = "How long the day phase lasts. (Timer mode only)",
        section = timingSection,
        position = 2
    )
    default int dayDuration()
    {
        return 30;
    }

    @Range(min = 1, max = 120)
    @ConfigItem(
        keyName = "nightDuration",
        name = "Night Duration (minutes)",
        description = "How long the night phase lasts. (Timer mode only)",
        section = timingSection,
        position = 3
    )
    default int nightDuration()
    {
        return 15;
    }

    @Range(min = 1, max = 60)
    @ConfigItem(
        keyName = "transitionDuration",
        name = "Transition Duration (seconds)",
        description = "How long sunrise/sunset takes. (Timer mode only)",
        section = timingSection,
        position = 4
    )
    default int transitionDuration()
    {
        return 30;
    }

    @ConfigItem(
        keyName = "startAtDay",
        name = "Start at Day",
        description = "Cycle starts at daytime on plugin load. (Timer mode only)",
        section = timingSection,
        position = 5
    )
    default boolean startAtDay()
    {
        return false;
    }

    // --- Clock mode ---

    @Range(min = 0, max = 23)
    @ConfigItem(
        keyName = "clockSunriseHour",
        name = "Sunrise Start Hour",
        description = "When sunrise transition begins (24h). (Clock mode only)",
        section = timingSection,
        position = 6
    )
    default int clockSunriseHour()
    {
        return 6;
    }

    @Range(min = 0, max = 23)
    @ConfigItem(
        keyName = "clockDayHour",
        name = "Day Start Hour",
        description = "When full daytime begins. (Clock mode only)",
        section = timingSection,
        position = 7
    )
    default int clockDayHour()
    {
        return 7;
    }

    @Range(min = 0, max = 23)
    @ConfigItem(
        keyName = "clockSunsetHour",
        name = "Sunset Start Hour",
        description = "When sunset transition begins. (Clock mode only)",
        section = timingSection,
        position = 8
    )
    default int clockSunsetHour()
    {
        return 18;
    }

    @Range(min = 0, max = 23)
    @ConfigItem(
        keyName = "clockNightHour",
        name = "Night Start Hour",
        description = "When full nighttime begins. (Clock mode only)",
        section = timingSection,
        position = 9
    )
    default int clockNightHour()
    {
        return 19;
    }

    // =============================================
    // Day Sky
    // =============================================

    @ConfigSection(
        name = "Day Sky",
        description = "Configure the daytime sky.",
        position = 1
    )
    String daySection = "daySection";

    @ConfigItem(
        keyName = "dayColor",
        name = "Day Sky Color",
        description = "The sky color during daytime.",
        section = daySection,
        position = 0
    )
    default Color dayColor()
    {
        return new Color(0x61, 0x9E, 0xCC);
    }

    @Range(min = -100, max = 100)
    @ConfigItem(
        keyName = "dayBrightnessOffset",
        name = "Day Brightness Offset",
        description = "Offset to 117 HD brightness during day. 0 = no change.",
        section = daySection,
        position = 1
    )
    default int dayBrightnessOffset()
    {
        return 0;
    }

    @Range(min = -100, max = 100)
    @ConfigItem(
        keyName = "dayContrastOffset",
        name = "Day Contrast Offset",
        description = "Offset to 117 HD contrast during day. 0 = no change.",
        section = daySection,
        position = 2
    )
    default int dayContrastOffset()
    {
        return 0;
    }

    @ConfigItem(
        keyName = "dayTintEnabled",
        name = "Day Tint",
        description = "Apply a color tint to the sky during daytime.",
        section = daySection,
        position = 3
    )
    default boolean dayTintEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "dayTintColor",
        name = "Day Tint Color",
        description = "The tint color blended into the sky during daytime.",
        section = daySection,
        position = 4
    )
    default Color dayTintColor()
    {
        return new Color(0xFF, 0xE8, 0xA0); // warm golden
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "dayTintStrength",
        name = "Day Tint Strength",
        description = "How strongly the tint is applied (0 = none, 100 = full).",
        section = daySection,
        position = 5
    )
    default int dayTintStrength()
    {
        return 20;
    }

    // =============================================
    // Night Sky
    // =============================================

    @ConfigSection(
        name = "Night Sky",
        description = "Configure the nighttime sky.",
        position = 2
    )
    String nightSection = "nightSection";

    @ConfigItem(
        keyName = "nightColor",
        name = "Night Sky Color",
        description = "The sky color during nighttime.",
        section = nightSection,
        position = 0
    )
    default Color nightColor()
    {
        return new Color(0x08, 0x0C, 0x20);
    }

    @Range(min = -100, max = 100)
    @ConfigItem(
        keyName = "nightBrightnessOffset",
        name = "Night Brightness Offset",
        description = "Offset to 117 HD brightness during night. 0 = no change.",
        section = nightSection,
        position = 1
    )
    default int nightBrightnessOffset()
    {
        return -20;
    }

    @Range(min = -100, max = 100)
    @ConfigItem(
        keyName = "nightContrastOffset",
        name = "Night Contrast Offset",
        description = "Offset to 117 HD contrast during night. 0 = no change.",
        section = nightSection,
        position = 2
    )
    default int nightContrastOffset()
    {
        return 0;
    }

    @ConfigItem(
        keyName = "nightTintEnabled",
        name = "Night Tint",
        description = "Apply a color tint to the sky during nighttime.",
        section = nightSection,
        position = 3
    )
    default boolean nightTintEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "nightTintColor",
        name = "Night Tint Color",
        description = "The tint color blended into the sky during nighttime.",
        section = nightSection,
        position = 4
    )
    default Color nightTintColor()
    {
        return new Color(0x30, 0x40, 0x80); // cool blue
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "nightTintStrength",
        name = "Night Tint Strength",
        description = "How strongly the tint is applied (0 = none, 100 = full).",
        section = nightSection,
        position = 5
    )
    default int nightTintStrength()
    {
        return 20;
    }

    // =============================================
    // Transition
    // =============================================

    @ConfigSection(
        name = "Transition",
        description = "Configure the sunrise/sunset transition appearance.",
        position = 3
    )
    String transitionSection = "transitionSection";

    @ConfigItem(
        keyName = "transitionColor",
        name = "Transition Tint Color",
        description = "The hue that eases in/out during sunrise and sunset.",
        section = transitionSection,
        position = 0
    )
    default Color transitionColor()
    {
        return new Color(0xFF, 0x8C, 0x32);
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "transitionTintStrength",
        name = "Tint Strength",
        description = "How strongly the transition color tints the sky (0 = none, 100 = full).",
        section = transitionSection,
        position = 1
    )
    default int transitionTintStrength()
    {
        return 40;
    }

    // =============================================
    // Underground
    // =============================================

    @ConfigSection(
        name = "Underground",
        description = "Configure the sky for underground/dungeon areas. Transitions are always instant.",
        position = 4
    )
    String undergroundSection = "undergroundSection";

    @ConfigItem(
        keyName = "undergroundEnabled",
        name = "Force Underground Sky",
        description = "Automatically switch to a dark sky when underground.",
        section = undergroundSection,
        position = 0
    )
    default boolean undergroundEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "undergroundColor",
        name = "Underground Sky Color",
        description = "The sky color used when underground.",
        section = undergroundSection,
        position = 1
    )
    default Color undergroundColor()
    {
        return new Color(0x00, 0x00, 0x00);
    }

    // =============================================
    // Overlay
    // =============================================

    @ConfigSection(
        name = "Overlay",
        description = "Configure the on-screen time overlay.",
        position = 5
    )
    String overlaySection = "overlaySection";

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Time Overlay",
        description = "Display the current cycle phase and time remaining on screen.",
        section = overlaySection,
        position = 0
    )
    default boolean showOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "condensedOverlay",
        name = "Condensed Overlay",
        description = "Show a compact single-line overlay instead of the full panel.",
        section = overlaySection,
        position = 1
    )
    default boolean condensedOverlay()
    {
        return false;
    }

    // Internal: persisted 117HD baseline (hidden from UI)
    String KEY_SAVED_HD_BRIGHTNESS = "savedHdBrightness";
    String KEY_SAVED_HD_CONTRAST   = "savedHdContrast";

    @ConfigItem(keyName = KEY_SAVED_HD_BRIGHTNESS, name = "", description = "", hidden = true)
    default int savedHdBrightness() { return -1; }

    @ConfigItem(keyName = KEY_SAVED_HD_BRIGHTNESS, name = "", description = "", hidden = true)
    void savedHdBrightness(int value);

    @ConfigItem(keyName = KEY_SAVED_HD_CONTRAST, name = "", description = "", hidden = true)
    default int savedHdContrast() { return -1; }

    @ConfigItem(keyName = KEY_SAVED_HD_CONTRAST, name = "", description = "", hidden = true)
    void savedHdContrast(int value);

    // =============================================
    // Advanced
    // =============================================

    @ConfigSection(
        name = "Advanced",
        description = "Advanced configuration options.",
        position = 6,
        closedByDefault = true
    )
    String advancedSection = "advancedSection";

    @ConfigItem(
        keyName = "overrideEnvironments",
        name = "Override 117 HD Environments",
        description = "Override 117 HD's per-area sky colors. Disable for areas like the Wilderness.",
        section = advancedSection,
        position = 0
    )
    default boolean overrideEnvironments()
    {
        return true;
    }
}
