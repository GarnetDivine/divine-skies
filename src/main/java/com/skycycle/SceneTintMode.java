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
 * Determines how the fullscreen scene tint overlay color is chosen.
 */
public enum SceneTintMode
{
    /**
     * Tint color follows the day/night cycle — uses the day tint color during day,
     * night tint color during night, and interpolates during transitions.
     */
    CYCLE("Follow Cycle"),

    /**
     * Tint color is a single fixed color chosen by the user.
     */
    FIXED("Fixed Color");

    private final String displayName;

    SceneTintMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
