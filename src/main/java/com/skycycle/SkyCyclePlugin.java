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

import com.google.inject.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Color;

@PluginDescriptor(
    name = "Divine: Skies (Beta)",
    description = "Configurable day/night sky cycle with smooth transitions, " +
        "local clock sync, phase tints, and automatic underground detection. Works with 117 HD.",
    tags = {"sky", "skybox", "day", "night", "cycle", "117", "hd", "aesthetic",
        "environment", "divine", "tint", "sunset", "sunrise"}
)
public class SkyCyclePlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(SkyCyclePlugin.class);

    @Inject private Client client;
    @Inject private SkyCycleConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private HdBridge hdBridge;
    @Inject private UndergroundDetector undergroundDetector;
    @Inject private SkyCycleOverlay overlay;

    private CycleEngine cycleEngine;
    private boolean currentlyUnderground = false;
    private boolean initialized = false;
    private int tickCounter = 0;
    private Color lastAppliedColor = null;
    private boolean lastWasUnderground = false;

    private static final int UPDATE_INTERVAL_TICKS = 3;

    public CycleEngine getCycleEngine() { return cycleEngine; }
    public boolean isCurrentlyUnderground() { return currentlyUnderground; }

    @Override
    protected void startUp()
    {
        log.info("Divine: Skies starting up");

        cycleEngine = new CycleEngine();
        configureEngine();

        if (!config.syncToLocalClock() && !config.startAtDay())
        {
            cycleEngine.alignToRealTime();
        }

        overlayManager.add(overlay);

        if (hdBridge.isHdActive())
        {
            hdBridge.saveOriginalSettings();
            initialized = true;
            log.info("Divine: Skies initialized with 117 HD");
        }
        else
        {
            log.warn("117 HD not detected — will retry on game state change");
        }
    }

    @Override
    protected void shutDown()
    {
        log.info("Divine: Skies shutting down");
        overlayManager.remove(overlay);

        if (initialized)
        {
            hdBridge.restoreOriginalSettings();
        }

        cycleEngine = null;
        initialized = false;
        lastAppliedColor = null;
    }

    @Provides
    SkyCycleConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SkyCycleConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN && !initialized)
        {
            if (hdBridge.isHdActive())
            {
                hdBridge.saveOriginalSettings();
                initialized = true;
                log.info("117 HD detected after login");
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!initialized || !config.enableCycle()) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;

        cycleEngine.update();

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL_TICKS) return;
        tickCounter = 0;

        boolean underground = config.undergroundEnabled() && undergroundDetector.isUnderground();

        if (underground)
        {
            applyUndergroundSky();
        }
        else
        {
            applyCycleSky();
        }

        currentlyUnderground = underground;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!SkyCycleConfig.CONFIG_GROUP.equals(event.getGroup())) return;

        configureEngine();

        if (!config.syncToLocalClock() && !config.startAtDay())
        {
            cycleEngine.alignToRealTime();
        }

        tickCounter = UPDATE_INTERVAL_TICKS;
    }

    private void configureEngine()
    {
        if (cycleEngine == null) return;

        if (config.syncToLocalClock())
        {
            cycleEngine.configureClock(
                config.clockDayHour(),
                config.clockSunsetHour(),
                config.clockNightHour(),
                config.clockSunriseHour()
            );
        }
        else
        {
            cycleEngine.configure(
                config.dayDuration(),
                config.nightDuration(),
                config.transitionDuration(),
                config.startAtDay()
            );
        }
    }

    private void applyUndergroundSky()
    {
        Color color = config.undergroundColor();

        if (lastWasUnderground && color.equals(lastAppliedColor))
        {
            return;
        }

        // Instant transition: set underground color and reset offsets to base
        hdBridge.applySkyColor(color, true);
        hdBridge.applyVisualOffsets(0, 0);

        lastAppliedColor = color;
        lastWasUnderground = true;
    }

    private void applyCycleSky()
    {
        boolean overrideEnv = config.overrideEnvironments();

        // Build the sky color with tints
        Color dayTint = config.dayTintEnabled() ? config.dayTintColor() : null;
        double dayTintStr = config.dayTintEnabled() ? config.dayTintStrength() / 100.0 : 0;
        Color nightTint = config.nightTintEnabled() ? config.nightTintColor() : null;
        double nightTintStr = config.nightTintEnabled() ? config.nightTintStrength() / 100.0 : 0;

        Color currentColor = cycleEngine.getCurrentColor(
            config.dayColor(), config.nightColor(),
            config.transitionColor(), config.transitionTintStrength() / 100.0,
            dayTint, dayTintStr,
            nightTint, nightTintStr
        );

        // Interpolated brightness/contrast offsets
        int brightnessOffset = getInterpolatedOffset(config.dayBrightnessOffset(), config.nightBrightnessOffset());
        int contrastOffset = getInterpolatedOffset(config.dayContrastOffset(), config.nightContrastOffset());

        boolean colorChanged = lastWasUnderground || lastAppliedColor == null
            || !colorsSimilar(lastAppliedColor, currentColor);

        if (colorChanged)
        {
            hdBridge.applySkyColor(currentColor, overrideEnv);
            lastAppliedColor = currentColor;
        }

        // Always re-apply offsets
        hdBridge.applyVisualOffsets(brightnessOffset, contrastOffset);

        lastWasUnderground = false;
    }

    private int getInterpolatedOffset(int dayOffset, int nightOffset)
    {
        CyclePhase phase = cycleEngine.getCurrentPhase();
        double progress = cycleEngine.getPhaseProgress();

        switch (phase)
        {
            case DAY:
                return dayOffset;
            case NIGHT:
                return nightOffset;
            case SUNSET:
            {
                double t = smoothstep(progress);
                return (int) Math.round(dayOffset + (nightOffset - dayOffset) * t);
            }
            case SUNRISE:
            {
                double t = smoothstep(progress);
                return (int) Math.round(nightOffset + (dayOffset - nightOffset) * t);
            }
            default:
                return dayOffset;
        }
    }

    private static double smoothstep(double t)
    {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static boolean colorsSimilar(Color a, Color b)
    {
        return Math.abs(a.getRed() - b.getRed()) <= 1
            && Math.abs(a.getGreen() - b.getGreen()) <= 1
            && Math.abs(a.getBlue() - b.getBlue()) <= 1;
    }
}
