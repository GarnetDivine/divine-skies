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

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SkyCycleOverlay extends Overlay
{
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final SkyCyclePlugin plugin;
    private final SkyCycleConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public SkyCycleOverlay(SkyCyclePlugin plugin, SkyCycleConfig config)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay()) return null;

        CycleEngine engine = plugin.getCycleEngine();
        if (engine == null) return null;

        panelComponent.getChildren().clear();

        CyclePhase phase = engine.getCurrentPhase();
        boolean isUnderground = plugin.isCurrentlyUnderground();
        String phaseText = isUnderground ? "Underground" : phase.getDisplayName();
        Color phaseColor = isUnderground ? new Color(120, 120, 120) : phase.getOverlayColor();
        int remainingSeconds = engine.getPhaseRemainingSeconds();
        String timeStr = formatTime(remainingSeconds);

        if (config.condensedOverlay())
        {
            String condensedText;
            if (config.syncToLocalClock())
            {
                condensedText = phaseText + " (" + LocalTime.now().format(TIME_FORMAT) + ")";
            }
            else
            {
                condensedText = phaseText + ": " + (isUnderground ? "--:--" : timeStr);
            }

            panelComponent.getChildren().add(TitleComponent.builder()
                .text(condensedText)
                .color(phaseColor)
                .build());
            panelComponent.setPreferredSize(new Dimension(130, 0));
        }
        else
        {
            panelComponent.getChildren().add(TitleComponent.builder()
                .text("Divine: Skies")
                .color(phaseColor)
                .build());

            panelComponent.getChildren().add(LineComponent.builder()
                .left("Phase:")
                .right(phaseText)
                .rightColor(phaseColor)
                .build());

            if (!isUnderground)
            {
                if (config.syncToLocalClock())
                {
                    panelComponent.getChildren().add(LineComponent.builder()
                        .left("Time:")
                        .right(LocalTime.now().format(TIME_FORMAT))
                        .build());

                    panelComponent.getChildren().add(LineComponent.builder()
                        .left("Next in:")
                        .right(timeStr)
                        .build());
                }
                else
                {
                    panelComponent.getChildren().add(LineComponent.builder()
                        .left("Remaining:")
                        .right(timeStr)
                        .build());

                    double progress = engine.getPhaseProgress();
                    String bar = buildProgressBar(progress, 10);
                    panelComponent.getChildren().add(LineComponent.builder()
                        .left("")
                        .right(bar)
                        .rightColor(phaseColor)
                        .build());
                }
            }

            panelComponent.setPreferredSize(new Dimension(140, 0));
        }

        return panelComponent.render(graphics);
    }

    private static String formatTime(int totalSeconds)
    {
        if (totalSeconds >= 3600)
        {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            return String.format("%d:%02d:%02d", hours, minutes, totalSeconds % 60);
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String buildProgressBar(double progress, int length)
    {
        int filled = (int) Math.round(progress * length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            sb.append(i < filled ? '\u2588' : '\u2591');
        }
        return sb.toString();
    }
}
