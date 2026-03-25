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

    private final ConfigManager configManager;
    private final Client client;

    // Original settings to restore on shutdown
    private String origDefaultSky;
    private String origOverrideSky;
    private int origSkyboxColor;
    private boolean settingsSaved = false;

    // The user's base brightness/contrast from 117 HD (captured at startup)
    private int baseBrightness = 100;
    private int baseContrast = 100;

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

    public void saveOriginalSettings()
    {
        if (settingsSaved) return;

        origDefaultSky = configManager.getConfiguration(HD_GROUP, KEY_DEFAULT_SKY);
        origOverrideSky = configManager.getConfiguration(HD_GROUP, KEY_OVERRIDE_SKY);
        origSkyboxColor = client.getSkyboxColor();

        // Capture the user's base brightness/contrast from 117 HD
        int previouslySavedBrightness = readOwnIntConfig(SkyCycleConfig.KEY_SAVED_HD_BRIGHTNESS, -1);
        int previouslySavedContrast   = readOwnIntConfig(SkyCycleConfig.KEY_SAVED_HD_CONTRAST, -1);

        if (previouslySavedBrightness != -1)
        {
            baseBrightness = previouslySavedBrightness;
        }
        else
        {
            baseBrightness = readIntConfig(KEY_BRIGHTNESS, 100);
            configManager.setConfiguration(SkyCycleConfig.CONFIG_GROUP,
                SkyCycleConfig.KEY_SAVED_HD_BRIGHTNESS, String.valueOf(baseBrightness));
        }

        if (previouslySavedContrast != -1)
        {
            baseContrast = previouslySavedContrast;
        }
        else
        {
            baseContrast = readIntConfig(KEY_CONTRAST, 100);
            configManager.setConfiguration(SkyCycleConfig.CONFIG_GROUP,
                SkyCycleConfig.KEY_SAVED_HD_CONTRAST, String.valueOf(baseContrast));
        }

        settingsSaved = true;
        log.debug("Saved 117HD settings: sky={}, override={}, brightness={}, contrast={}",
            origDefaultSky, origOverrideSky, baseBrightness, baseContrast);
    }

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

        // Clear the persisted baseline now that we've cleanly restored
        configManager.setConfiguration(SkyCycleConfig.CONFIG_GROUP,
            SkyCycleConfig.KEY_SAVED_HD_BRIGHTNESS, "-1");
        configManager.setConfiguration(SkyCycleConfig.CONFIG_GROUP,
            SkyCycleConfig.KEY_SAVED_HD_CONTRAST, "-1");

        settingsSaved = false;
        log.debug("Restored 117HD settings");
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

        log.debug("SkyCycle visual offsets: base bright={}, offset={}, result={}  |  base con={}, offset={}, result={}",
            baseBrightness, brightnessOffset, newBrightness, baseContrast, contrastOffset, newContrast);

        configManager.setConfiguration(HD_GROUP, KEY_BRIGHTNESS, String.valueOf(newBrightness));
        configManager.setConfiguration(HD_GROUP, KEY_CONTRAST, String.valueOf(newContrast));
    }

    private int readOwnIntConfig(String key, int defaultValue)
    {
        String val = configManager.getConfiguration(SkyCycleConfig.CONFIG_GROUP, key);
        if (val == null) return defaultValue;
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    private int readIntConfig(String key, int defaultValue)
    {
        String val = configManager.getConfiguration(HD_GROUP, key);
        if (val == null) return defaultValue;
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            // Might be a float like "100.0"
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
