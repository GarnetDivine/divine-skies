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

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

/**
 * Draws a semi-transparent color fill over the entire game viewport.
 * This is the actual "scene tint" — unlike the sky tint which only
 * affects the skybox color, this overlay tints the entire rendered view
 * (terrain, models, everything the player sees).
 *
 * Uses OverlayLayer.ABOVE_SCENE so it renders on top of the 3D world
 * but below UI elements.
 */
public class SceneTintOverlay extends Overlay
{
    private final Client client;
    private final SkyCyclePlugin plugin;
    private final SkyCycleConfig config;

    @Inject
    public SceneTintOverlay(Client client, SkyCyclePlugin plugin, SkyCycleConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.sceneTintEnabled() || !config.enableCycle())
        {
            return null;
        }

        // Don't tint when underground (underground has its own lighting)
        if (plugin.isCurrentlyUnderground())
        {
            return null;
        }

        Color tintColor = getTintColor();
        if (tintColor == null)
        {
            return null;
        }

        int strength = config.sceneTintStrength();
        if (strength <= 0)
        {
            return null;
        }

        // Alpha ranges from 0 (invisible) to 255 (solid)
        // We cap at ~60% opacity even at strength 100 because full solid looks terrible
        int alpha = (int) Math.round(strength / 100.0 * 153);
        alpha = Math.max(0, Math.min(153, alpha));

        Color overlayColor = new Color(
            tintColor.getRed(),
            tintColor.getGreen(),
            tintColor.getBlue(),
            alpha
        );

        // Fill the entire game canvas
        graphics.setColor(overlayColor);
        graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());

        return null;
    }

    /**
     * Determine the current tint color based on the configured mode.
     */
    private Color getTintColor()
    {
        SceneTintMode mode = config.sceneTintMode();

        if (mode == SceneTintMode.FIXED)
        {
            return config.sceneTintFixedColor();
        }

        // CYCLE mode: follow the day/night tint colors
        CycleEngine engine = plugin.getCycleEngine();
        if (engine == null) return null;

        Color dayTint = config.dayTintEnabled() ? config.dayTintColor() : null;
        Color nightTint = config.nightTintEnabled() ? config.nightTintColor() : null;

        // If neither day nor night tint is enabled, fall back to the transition color
        if (dayTint == null && nightTint == null)
        {
            return config.transitionColor();
        }

        // If only one is set, use it for both ends
        if (dayTint == null) dayTint = nightTint;
        if (nightTint == null) nightTint = dayTint;

        CyclePhase phase = engine.getCurrentPhase();
        double progress = engine.getPhaseProgress();

        switch (phase)
        {
            case DAY:
                return dayTint;
            case NIGHT:
                return nightTint;
            case SUNSET:
            {
                double t = progress * progress * (3 - 2 * progress); // smoothstep
                return CycleEngine.lerpColor(dayTint, nightTint, t);
            }
            case SUNRISE:
            {
                double t = progress * progress * (3 - 2 * progress);
                return CycleEngine.lerpColor(nightTint, dayTint, t);
            }
            default:
                return dayTint;
        }
    }
}
