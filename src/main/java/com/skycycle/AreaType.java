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

/**
 * The set of environment classifications Divine: Skies recognises for a region.
 *
 * <p>This enum backs the player-facing manual override system. When automatic
 * detection misclassifies an area, the player can cycle the current region
 * through these states with a hotkey. The override is persisted per region ID
 * and takes precedence over automatic detection (but never over server-authoritative
 * varbit gates such as an active raid).</p>
 *
 * <p>The hotkey cycles in declaration order, then wraps back to "no override":
 * (none) -> UNDERGROUND -> SURFACE -> POH -> (none).</p>
 */
public enum AreaType
{
    /** Treat as overworld — the normal day/night cycle applies. */
    SURFACE("Surface"),

    /** Treat as underground/dungeon — dark sky, no brightness/contrast offsets. */
    UNDERGROUND("Underground"),

    /** Treat as a Player-Owned House — the configured POH sky applies. */
    POH("Player House");

    private final String displayName;

    AreaType(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
