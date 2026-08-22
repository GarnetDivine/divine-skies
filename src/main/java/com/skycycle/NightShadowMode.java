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
 * Controls what happens to 117 HD's dynamic shadows during the NIGHT phase.
 *
 * <p>117 HD exposes shadows as a mode ({@code OFF} / {@code FAST} / {@code DETAILED})
 * rather than an intensity slider, so "reduced" maps to Fast mode (shadows without
 * texture detail) and "removed" maps to Off. The user's own shadow mode is captured
 * as a baseline by {@link HdBridge} and restored at sunrise, underground, in the POH,
 * and on plugin shutdown.</p>
 */
public enum NightShadowMode
{
    /** Leave 117 HD's shadow mode alone at night. */
    UNCHANGED("Unchanged", null),

    /** Drop to 117 HD's Fast shadow mode at night (softer, no texture detail). */
    FAST("Reduced (Fast)", "FAST"),

    /** Turn 117 HD shadows off entirely at night. */
    OFF("No Shadows", "OFF");

    private final String displayName;

    /** The exact value written to 117 HD's {@code shadowMode} config key, or null for baseline. */
    private final String hdValue;

    NightShadowMode(String displayName, String hdValue)
    {
        this.displayName = displayName;
        this.hdValue = hdValue;
    }

    public String getHdValue()
    {
        return hdValue;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
