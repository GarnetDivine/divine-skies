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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;

/**
 * Bridges SkyCycle to 117 HD by:
 * 1. Setting 117 HD's defaultSkyColor config to "RUNELITE" (reads from RuneLite's skybox)
 * 2. Using client.setSkyboxColor() to set the actual color
 * 3. Applying brightness/contrast offsets relative to the user's base 117 HD settings
 *
 * Baseline brightness/contrast values are persisted in Divine Skies' own config namespace
 * ("skycycle") so they survive client restarts and can't compound/stack.
 */
@Singleton
public class HdBridge
{
    private static final Logger log = LoggerFactory.getLogger(HdBridge.class);

    private static final String HD_GROUP = "hd";
    private static final String KEY_DEFAULT_SKY = "defaultSkyColor";
    private static final String KEY_OVERRIDE_SKY = "overrideSky";
    private static final String KEY_BRIGHTNESS = "screenBrightness";
    private static final String KEY_CONTRAST = "fContrast";

    // Keys in our own config group for persisted baselines
    private static final String DS_GROUP = SkyCycleConfig.CONFIG_GROUP;
    private static final String DS_BASE_BRIGHTNESS = "hdBaseBrightness";
    private static final String DS_BASE_CONTRAST = "hdBaseContrast";
    private static final String DS_ORIG_DEFAULT_SKY = "hdOrigDefaultSky";
    private static final String DS_ORIG_OVERRIDE_SKY = "hdOrigOverrideSky";
    private static final String DS_DIRTY = "hdDirty";

    private final ConfigManager configManager;
    private final Client client;

    // Original settings to restore on shutdown
    private String origDefaultSky;
    private String origOverrideSky;
    private int origSkyboxColor;
    private boolean settingsSaved = false;

    // The user's true base brightness/contrast from 117 HD (before any Divine Skies offsets)
    private int baseBrightness = 100;
    private int baseContrast = 100;

    // Track the last offsets we applied so we can detect external 117HD changes
    private int lastAppliedBrightness = -1;
    private int lastAppliedContrast = -1;

    @Inject
    public HdBridge(ConfigManager configManager, Client client)
    {
        this.configManager = configManager;
        this.client = client;
    }

    public boolean isHdActive()
    {
        return configManager.getConfiguration(HD_GROUP, KEY_DEFAULT_SKY) != null;
    }

    /**
     * Save the user's original 117 HD settings before Divine Skies takes over.
     *
     * The key insight for the stacking fix: if we previously took control and the client
     * was closed without a clean shutdown (crash, force quit, etc.), the 117HD values
     * will still contain our modified values, not the user's real baseline. So we persist
     * the true baseline in our own config namespace and check the "dirty" flag on startup.
     *
     * Flow:
     * 1. Check if DS_DIRTY is "true" — meaning we were actively modifying 117HD when the
     *    client last closed. If so, the current 117HD values are tainted; use our persisted
     *    baselines instead.
     * 2. If DS_DIRTY is not set (clean start, or first ever run), read from 117HD directly
     *    and persist those as the baseline.
     * 3. Set DS_DIRTY = "true" — we're now in control.
     * 4. On clean shutdown (restoreOriginalSettings), clear DS_DIRTY.
     */
    public void saveOriginalSettings()
    {
        if (settingsSaved) return;

        boolean wasDirty = "true".equals(configManager.getConfiguration(DS_GROUP, DS_DIRTY));

        if (wasDirty)
        {
            // Previous session didn't shut down cleanly — use our persisted baselines
            baseBrightness = readOurIntConfig(DS_BASE_BRIGHTNESS, 100);
            baseContrast = readOurIntConfig(DS_BASE_CONTRAST, 100);
            origDefaultSky = configManager.getConfiguration(DS_GROUP, DS_ORIG_DEFAULT_SKY);
            origOverrideSky = configManager.getConfiguration(DS_GROUP, DS_ORIG_OVERRIDE_SKY);

            log.info("Divine: Skies recovered baselines from previous dirty session: " +
                "brightness={}, contrast={}, sky={}, override={}",
                baseBrightness, baseContrast, origDefaultSky, origOverrideSky);

            // Immediately write the correct baselines back to 117HD so it's in a known state
            configManager.setConfiguration(HD_GROUP, KEY_BRIGHTNESS, String.valueOf(baseBrightness));
            configManager.setConfiguration(HD_GROUP, KEY_CONTRAST, String.valueOf(baseContrast));
            if (origDefaultSky != null)
            {
                configManager.setConfiguration(HD_GROUP, KEY_DEFAULT_SKY, origDefaultSky);
            }
            if (origOverrideSky != null)
            {
                configManager.setConfiguration(HD_GROUP, KEY_OVERRIDE_SKY, origOverrideSky);
            }
        }
        else
        {
            // Clean start — read directly from 117 HD
            origDefaultSky = configManager.getConfiguration(HD_GROUP, KEY_DEFAULT_SKY);
            origOverrideSky = configManager.getConfiguration(HD_GROUP, KEY_OVERRIDE_SKY);
            baseBrightness = readIntConfig(KEY_BRIGHTNESS, 100);
            baseContrast = readIntConfig(KEY_CONTRAST, 100);

            // Persist these as our known-good baselines
            configManager.setConfiguration(DS_GROUP, DS_BASE_BRIGHTNESS, String.valueOf(baseBrightness));
            configManager.setConfiguration(DS_GROUP, DS_BASE_CONTRAST, String.valueOf(baseContrast));
            if (origDefaultSky != null)
            {
                configManager.setConfiguration(DS_GROUP, DS_ORIG_DEFAULT_SKY, origDefaultSky);
            }
            if (origOverrideSky != null)
            {
                configManager.setConfiguration(DS_GROUP, DS_ORIG_OVERRIDE_SKY, origOverrideSky);
            }

            log.debug("Saved fresh 117HD baselines: sky={}, override={}, brightness={}, contrast={}",
                origDefaultSky, origOverrideSky, baseBrightness, baseContrast);
        }

        origSkyboxColor = client.getSkyboxColor();

        // Mark ourselves as actively modifying 117HD
        configManager.setConfiguration(DS_GROUP, DS_DIRTY, "true");

        settingsSaved = true;
        lastAppliedBrightness = -1;
        lastAppliedContrast = -1;
    }

    /**
     * Restore the user's original 117 HD settings and clear the dirty flag.
     */
    public void restoreOriginalSettings()
    {
        if (!settingsSaved) return;

        if (origDefaultSky != null)
        {
            configManager.setConfiguration(HD_GROUP, KEY_DEFAULT_SKY, origDefaultSky);
        }
        if (origOverrideSky != null)
        {
            configManager.setConfiguration(HD_GROUP, KEY_OVERRIDE_SKY, origOverrideSky);
        }
        client.setSkyboxColor(origSkyboxColor);

        // Restore original brightness/contrast
        configManager.setConfiguration(HD_GROUP, KEY_BRIGHTNESS, String.valueOf(baseBrightness));
        configManager.setConfiguration(HD_GROUP, KEY_CONTRAST, String.valueOf(baseContrast));

        // Clean shutdown — clear the dirty flag so next startup reads fresh from 117HD
        configManager.setConfiguration(DS_GROUP, DS_DIRTY, "false");

        settingsSaved = false;
        lastAppliedBrightness = -1;
        lastAppliedContrast = -1;
        log.debug("Restored 117HD settings and cleared dirty flag");
    }

    /**
     * Allow the user to manually re-capture their 117HD baseline while Divine Skies is running.
     * Useful if they changed their 117HD brightness/contrast and want Divine Skies to use
     * those new values as the base. Called from a config button or command.
     */
    public void recaptureBaseline()
    {
        if (!settingsSaved) return;

        // Temporarily write back our known baseline so we read the user's intended value
        // Actually — we should read what 117HD currently has, subtract our last offset,
        // to get the user's intended base. Or simpler: just read the current value as the new base.
        // Since the user is explicitly asking to recapture, we trust the current value.
        baseBrightness = readIntConfig(KEY_BRIGHTNESS, 100);
        baseContrast = readIntConfig(KEY_CONTRAST, 100);

        configManager.setConfiguration(DS_GROUP, DS_BASE_BRIGHTNESS, String.valueOf(baseBrightness));
        configManager.setConfiguration(DS_GROUP, DS_BASE_CONTRAST, String.valueOf(baseContrast));

        log.info("Recaptured 117HD baselines: brightness={}, contrast={}", baseBrightness, baseContrast);
    }

    /**
     * Apply a sky color via the RuneLite Client API.
     */
    public void applySkyColor(Color color, boolean overrideEnvironments)
    {
        int rgb = color.getRGB() & 0x00FFFFFF;
        client.setSkyboxColor(rgb);
        configManager.setConfiguration(HD_GROUP, KEY_DEFAULT_SKY, "RUNELITE");
        configManager.setConfiguration(HD_GROUP, KEY_OVERRIDE_SKY, String.valueOf(overrideEnvironments));
    }

    /**
     * Apply brightness and contrast offsets relative to the user's base 117 HD values.
     * An offset of 0 means no change. -50 means 50 less than their setting. +50 means 50 more.
     *
     * @param brightnessOffset offset to add to user's base brightness (-100 to +100)
     * @param contrastOffset   offset to add to user's base contrast (-100 to +100)
     */
    public void applyVisualOffsets(int brightnessOffset, int contrastOffset)
    {
        int newBrightness = Math.max(0, Math.min(200, baseBrightness + brightnessOffset));
        int newContrast = Math.max(0, Math.min(200, baseContrast + contrastOffset));

        // Skip redundant writes
        if (newBrightness == lastAppliedBrightness && newContrast == lastAppliedContrast)
        {
            return;
        }

        log.debug("SkyCycle visual offsets: base bright={}, offset={}, result={}  |  base con={}, offset={}, result={}",
            baseBrightness, brightnessOffset, newBrightness, baseContrast, contrastOffset, newContrast);

        configManager.setConfiguration(HD_GROUP, KEY_BRIGHTNESS, String.valueOf(newBrightness));
        configManager.setConfiguration(HD_GROUP, KEY_CONTRAST, String.valueOf(newContrast));

        lastAppliedBrightness = newBrightness;
        lastAppliedContrast = newContrast;
    }

    /**
     * Get the persisted base brightness (for use by the overlay or other display).
     */
    public int getBaseBrightness()
    {
        return baseBrightness;
    }

    /**
     * Get the persisted base contrast (for use by the overlay or other display).
     */
    public int getBaseContrast()
    {
        return baseContrast;
    }

    private int readIntConfig(String key, int defaultValue)
    {
        String val = configManager.getConfiguration(HD_GROUP, key);
        return parseIntSafe(val, defaultValue);
    }

    private int readOurIntConfig(String key, int defaultValue)
    {
        String val = configManager.getConfiguration(DS_GROUP, key);
        return parseIntSafe(val, defaultValue);
    }

    private static int parseIntSafe(String val, int defaultValue)
    {
        if (val == null) return defaultValue;
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            try
            {
                return Math.round(Float.parseFloat(val));
            }
            catch (NumberFormatException e2)
            {
                return defaultValue;
            }
        }
    }
}
