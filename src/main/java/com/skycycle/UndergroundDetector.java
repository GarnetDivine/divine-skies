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
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Detects whether the player is underground using OSRS's world coordinate system.
 *
 * In OSRS, the surface world Y coordinates range from roughly 1216 to 6463.
 * Underground/dungeon areas are mapped at Y coordinates above ~8800 (range 8835-11541).
 * This gap makes Y > 8000 a very reliable underground check.
 */
@Singleton
public class UndergroundDetector
{
    private static final int UNDERGROUND_Y_THRESHOLD = 8000;

    private final Client client;

    @Inject
    public UndergroundDetector(Client client)
    {
        this.client = client;
    }

    /**
     * Determine if the player is currently in an underground/dungeon area.
     * Uses the world Y coordinate: surface is Y < 6464, underground is Y > 8000.
     */
    public boolean isUnderground()
    {
        if (client.getLocalPlayer() == null)
        {
            return false;
        }

        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        if (wp == null)
        {
            return false;
        }

        return wp.getY() > UNDERGROUND_Y_THRESHOLD;
    }
}
