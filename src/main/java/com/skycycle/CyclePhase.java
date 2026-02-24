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

import java.awt.Color;

public enum CyclePhase
{
    DAY("Day", new Color(255, 220, 80)),
    SUNSET("Sunset", new Color(255, 140, 50)),
    NIGHT("Night", new Color(100, 120, 200)),
    SUNRISE("Sunrise", new Color(255, 180, 100));

    private final String displayName;
    private final Color overlayColor;

    CyclePhase(String displayName, Color overlayColor)
    {
        this.displayName = displayName;
        this.overlayColor = overlayColor;
    }

    public String getDisplayName() { return displayName; }
    public Color getOverlayColor() { return overlayColor; }
}
