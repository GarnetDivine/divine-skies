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

        // Don't tint underground or in the POH — those areas use their own
        // lighting / custom skybox, and a day/night tint there looks wrong.
        if (plugin.isCurrentlyUnderground() || plugin.isCurrentlyInPoh())
        {
            return null;
        }

        int strength = config.sceneTintStrength();
        if (strength <= 0)
        {
            return null;
        }

        // Master alpha: 0 (invisible) to ~60% opacity at strength 100,
        // because a fully solid fill looks terrible.
        int masterAlpha = clampAlpha((int) Math.round(strength / 100.0 * 153));

        Color overlayColor = getTintColor(masterAlpha);
        if (overlayColor == null || overlayColor.getAlpha() <= 0)
        {
            return null;
        }

        // Fill the entire game canvas (UI widgets render above this layer)
        graphics.setColor(overlayColor);
        graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());

        return null;
    }

    /**
     * Determine the current tint color AND alpha based on the configured mode.
     *
     * <p>In CYCLE mode each phase has its own effective alpha: a disabled phase tint
     * means alpha 0 for that phase (previously a disabled day tint would incorrectly
     * borrow the night tint at full strength, tinting the daytime scene). During
     * sunrise/sunset both the color and the alpha are smoothly interpolated, so a
     * night-only tint now fades in over the sunset instead of popping.</p>
     *
     * @param masterAlpha the alpha corresponding to the configured tint strength
     * @return the premixed overlay color with alpha, or null for no overlay
     */
    private Color getTintColor(int masterAlpha)
    {
        SceneTintMode mode = config.sceneTintMode();

        if (mode == SceneTintMode.FIXED)
        {
            Color fixed = config.sceneTintFixedColor();
            return withAlpha(fixed, masterAlpha);
        }

        // CYCLE mode: follow the day/night tint colors
        CycleEngine engine = plugin.getCycleEngine();
        if (engine == null)
        {
            return null;
        }

        Color dayTint = config.dayTintEnabled() ? config.dayTintColor() : null;
        Color nightTint = config.nightTintEnabled() ? config.nightTintColor() : null;

        // Neither tint enabled: nothing to draw. (Previously this fell back to the
        // transition color, permanently orange-tinting the scene — surprising.)
        if (dayTint == null && nightTint == null)
        {
            return null;
        }

        int dayAlpha = dayTint != null ? masterAlpha : 0;
        int nightAlpha = nightTint != null ? masterAlpha : 0;

        // For color interpolation continuity, a disabled end borrows the other end's
        // hue — but its alpha stays 0, so it never actually shows during that phase.
        Color dayColor = dayTint != null ? dayTint : nightTint;
        Color nightColor = nightTint != null ? nightTint : dayTint;

        CyclePhase phase = engine.getCurrentPhase();
        double progress = engine.getPhaseProgress();

        switch (phase)
        {
            case DAY:
                return dayAlpha > 0 ? withAlpha(dayColor, dayAlpha) : null;
            case NIGHT:
                return nightAlpha > 0 ? withAlpha(nightColor, nightAlpha) : null;
            case SUNSET:
            {
                double t = smoothstep(progress);
                Color c = CycleEngine.lerpColor(dayColor, nightColor, t);
                int a = (int) Math.round(dayAlpha + (nightAlpha - dayAlpha) * t);
                return withAlpha(c, clampAlpha(a));
            }
            case SUNRISE:
            {
                double t = smoothstep(progress);
                Color c = CycleEngine.lerpColor(nightColor, dayColor, t);
                int a = (int) Math.round(nightAlpha + (dayAlpha - nightAlpha) * t);
                return withAlpha(c, clampAlpha(a));
            }
            default:
                return dayAlpha > 0 ? withAlpha(dayColor, dayAlpha) : null;
        }
    }

    private static double smoothstep(double t)
    {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static int clampAlpha(int a)
    {
        return Math.max(0, Math.min(153, a));
    }

    private static Color withAlpha(Color c, int alpha)
    {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
