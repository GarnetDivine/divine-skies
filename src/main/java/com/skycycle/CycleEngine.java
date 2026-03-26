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

import java.awt.Color;
import java.time.LocalTime;

/**
 * Handles the day/night cycle timing, phase tracking, and color interpolation.
 * Supports timer mode and local clock mode.
 * Handles phase tints (day/night) and transition tints (sunrise/sunset).
 */
public class CycleEngine
{
    private static final Logger log = LoggerFactory.getLogger(CycleEngine.class);

    private CyclePhase currentPhase = CyclePhase.DAY;
    private double phaseProgress = 0.0;

    // Timer mode
    private long cycleStartTimeMs;
    private long totalCycleDurationMs;
    private long dayDurationMs;
    private long sunsetDurationMs;
    private long nightDurationMs;
    private long sunriseDurationMs;
    private long dayEndMs;
    private long sunsetEndMs;
    private long nightEndMs;

    // Clock mode
    private boolean clockMode = false;
    private int dayStartHour = 7;
    private int sunsetStartHour = 18;
    private int nightStartHour = 19;
    private int sunriseStartHour = 6;

    public CyclePhase getCurrentPhase() { return currentPhase; }
    public double getPhaseProgress() { return phaseProgress; }

    public CycleEngine()
    {
        this.cycleStartTimeMs = System.currentTimeMillis();
    }

    public void configure(int dayMinutes, int nightMinutes, int transitionSeconds, boolean startAtDay)
    {
        this.clockMode = false;
        this.dayDurationMs = dayMinutes * 60_000L;
        this.nightDurationMs = nightMinutes * 60_000L;
        this.sunsetDurationMs = transitionSeconds * 1000L;
        this.sunriseDurationMs = transitionSeconds * 1000L;
        this.totalCycleDurationMs = dayDurationMs + sunsetDurationMs + nightDurationMs + sunriseDurationMs;
        this.dayEndMs = dayDurationMs;
        this.sunsetEndMs = dayEndMs + sunsetDurationMs;
        this.nightEndMs = sunsetEndMs + nightDurationMs;

        if (startAtDay)
        {
            this.cycleStartTimeMs = System.currentTimeMillis();
        }
    }

    public void configureClock(int dayStart, int sunsetStart, int nightStart, int sunriseStart)
    {
        this.clockMode = true;
        this.dayStartHour = dayStart;
        this.sunsetStartHour = sunsetStart;
        this.nightStartHour = nightStart;
        this.sunriseStartHour = sunriseStart;
    }

    public void alignToRealTime()
    {
        if (totalCycleDurationMs <= 0) return;
        long now = System.currentTimeMillis();
        long offset = now % totalCycleDurationMs;
        this.cycleStartTimeMs = now - offset;
    }

    public void update()
    {
        if (clockMode) updateClockMode();
        else updateTimerMode();
    }

    private void updateTimerMode()
    {
        if (totalCycleDurationMs <= 0) return;
        long now = System.currentTimeMillis();
        long elapsed = (now - cycleStartTimeMs) % totalCycleDurationMs;
        if (elapsed < 0) elapsed += totalCycleDurationMs;

        if (elapsed < dayEndMs)
        {
            currentPhase = CyclePhase.DAY;
            phaseProgress = (double) elapsed / dayDurationMs;
        }
        else if (elapsed < sunsetEndMs)
        {
            currentPhase = CyclePhase.SUNSET;
            phaseProgress = (double) (elapsed - dayEndMs) / sunsetDurationMs;
        }
        else if (elapsed < nightEndMs)
        {
            currentPhase = CyclePhase.NIGHT;
            phaseProgress = (double) (elapsed - sunsetEndMs) / nightDurationMs;
        }
        else
        {
            currentPhase = CyclePhase.SUNRISE;
            phaseProgress = (double) (elapsed - nightEndMs) / sunriseDurationMs;
        }
    }

    private void updateClockMode()
    {
        LocalTime now = LocalTime.now();
        int minuteOfDay = now.getHour() * 60 + now.getMinute();
        int sunriseStart = sunriseStartHour * 60;
        int dayStart = dayStartHour * 60;
        int sunsetStart = sunsetStartHour * 60;
        int nightStart = nightStartHour * 60;

        if (isInRange(minuteOfDay, sunriseStart, dayStart))
        {
            currentPhase = CyclePhase.SUNRISE;
            phaseProgress = rangeProgress(minuteOfDay, sunriseStart, dayStart);
        }
        else if (isInRange(minuteOfDay, dayStart, sunsetStart))
        {
            currentPhase = CyclePhase.DAY;
            phaseProgress = rangeProgress(minuteOfDay, dayStart, sunsetStart);
        }
        else if (isInRange(minuteOfDay, sunsetStart, nightStart))
        {
            currentPhase = CyclePhase.SUNSET;
            phaseProgress = rangeProgress(minuteOfDay, sunsetStart, nightStart);
        }
        else
        {
            currentPhase = CyclePhase.NIGHT;
            phaseProgress = rangeProgress(minuteOfDay, nightStart, sunriseStart);
        }
    }

    /**
     * Get the final sky color, including phase tints and transition tints.
     *
     * @param dayColor           Base sky color for day
     * @param nightColor         Base sky color for night
     * @param transitionTintColor Tint color during sunrise/sunset
     * @param transitionTintStr  Transition tint strength (0.0-1.0)
     * @param dayTintColor       Optional day tint color (null if disabled)
     * @param dayTintStr         Day tint strength (0.0-1.0)
     * @param nightTintColor     Optional night tint color (null if disabled)
     * @param nightTintStr       Night tint strength (0.0-1.0)
     */
    public Color getCurrentColor(
        Color dayColor, Color nightColor,
        Color transitionTintColor, double transitionTintStr,
        Color dayTintColor, double dayTintStr,
        Color nightTintColor, double nightTintStr)
    {
        // First, apply phase tints to the base colors
        Color tintedDay = dayColor;
        if (dayTintColor != null && dayTintStr > 0)
        {
            tintedDay = lerpColor(dayColor, dayTintColor, dayTintStr);
        }

        Color tintedNight = nightColor;
        if (nightTintColor != null && nightTintStr > 0)
        {
            tintedNight = lerpColor(nightColor, nightTintColor, nightTintStr);
        }

        switch (currentPhase)
        {
            case DAY:
                return tintedDay;
            case NIGHT:
                return tintedNight;
            case SUNSET:
            {
                double t = smoothstep(phaseProgress);
                Color midpoint = lerpColor(tintedDay, tintedNight, t);
                double tintAmount = transitionTintStr * bellCurve(t);
                return lerpColor(midpoint, transitionTintColor, tintAmount);
            }
            case SUNRISE:
            {
                double t = smoothstep(phaseProgress);
                Color midpoint = lerpColor(tintedNight, tintedDay, t);
                double tintAmount = transitionTintStr * bellCurve(t);
                return lerpColor(midpoint, transitionTintColor, tintAmount);
            }
            default:
                return tintedDay;
        }
    }

    public int getPhaseRemainingSeconds()
    {
        if (clockMode) return getClockPhaseRemainingSeconds();
        if (totalCycleDurationMs <= 0) return 0;

        long phaseDuration;
        switch (currentPhase)
        {
            case DAY: phaseDuration = dayDurationMs; break;
            case SUNSET: phaseDuration = sunsetDurationMs; break;
            case NIGHT: phaseDuration = nightDurationMs; break;
            case SUNRISE: phaseDuration = sunriseDurationMs; break;
            default: phaseDuration = 0;
        }
        return (int) ((1.0 - phaseProgress) * phaseDuration / 1000);
    }

    private int getClockPhaseRemainingSeconds()
    {
        LocalTime now = LocalTime.now();
        int minuteOfDay = now.getHour() * 60 + now.getMinute();
        int secondOfMinute = now.getSecond();

        int phaseEnd;
        switch (currentPhase)
        {
            case SUNRISE: phaseEnd = dayStartHour * 60; break;
            case DAY: phaseEnd = sunsetStartHour * 60; break;
            case SUNSET: phaseEnd = nightStartHour * 60; break;
            case NIGHT: phaseEnd = sunriseStartHour * 60; break;
            default: return 0;
        }

        int remainingMinutes = (phaseEnd > minuteOfDay)
            ? (phaseEnd - minuteOfDay)
            : (1440 - minuteOfDay + phaseEnd);

        return remainingMinutes * 60 - secondOfMinute;
    }

    // --- Utilities ---

    private static boolean isInRange(int value, int start, int end)
    {
        return (start <= end) ? (value >= start && value < end) : (value >= start || value < end);
    }

    private static double rangeProgress(int value, int start, int end)
    {
        int duration, elapsed;
        if (start <= end)
        {
            duration = end - start;
            elapsed = value - start;
        }
        else
        {
            duration = (1440 - start) + end;
            elapsed = (value >= start) ? (value - start) : (1440 - start + value);
        }
        if (duration <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (double) elapsed / duration));
    }

    private static double bellCurve(double t) { return 4.0 * t * (1.0 - t); }

    private static double smoothstep(double t)
    {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    static Color lerpColor(Color a, Color b, double t)
    {
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, bl))
        );
    }
}
