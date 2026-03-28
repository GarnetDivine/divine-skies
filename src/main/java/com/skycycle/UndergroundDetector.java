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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects whether the player is underground, in Death's Office, or in a Player-Owned House.
 *
 * Detection uses three layers for underground:
 * 1. FORCE_SURFACE_REGIONS - always treated as surface, even if Y > 8000.
 * 2. FORCE_UNDERGROUND_REGIONS - always underground (no surface overlap).
 * 3. INSTANCE_UNDERGROUND_REGIONS - underground ONLY when instanced or non-zero plane.
 * 4. Y coordinate fallback - Y > 8000 = underground.
 *
 * POH detection uses chunk coordinate range matching since POH regions cluster
 * in a specific area that doesn't overlap with the surface world.
 */
@Singleton
public class UndergroundDetector
{
    private static final Logger log = LoggerFactory.getLogger(UndergroundDetector.class);
    private static final int UNDERGROUND_Y_THRESHOLD = 8000;

    /**
     * Regions that are ALWAYS underground. These have chunk coordinates that don't
     * overlap with any surface-world area, so region ID alone is sufficient.
     */
    private static final Set<Integer> FORCE_UNDERGROUND_REGIONS = new HashSet<>(Arrays.asList(
        6222,   // Death's Office / Death's Domain
        6992,   // Giant Mole Lair
        7505,   // Stronghold of Security: Floor 1
        7506,   // Stronghold of Security: Floor 2
        7507,   // Stronghold of Security: Floor 3
        7508,   // Stronghold of Security: Floor 4
        7509,   // Stronghold of Security: Floor 5
        8023,   // Demonic Gorilla Cave
        9551,   // Mor Ul Rek (TzHaar city)
        11836,  // Blast Furnace
        11844,  // Corporeal Beast Cave
        12126,  // Lithkren Vault
        12127,  // Ancient Guthixian Temple
        12192,  // Camdozaal / Rogues' Den
        12933,  // Zalcano
        13654   // Leviathan Path
    ));

    /**
     * Regions that are underground ONLY when the player is in an instance or on a
     * non-ground plane. These share chunk coordinates with surface-world areas.
     */
    private static final Set<Integer> INSTANCE_UNDERGROUND_REGIONS = new HashSet<>(Arrays.asList(
        9781,   // Tree Gnome Village Maze Cave
        11050,  // Ape Atoll Cooking Cave
        11058,  // Lassar Undercity
        12132,  // Duke Sucellus
        12598,  // Burthorpe Games Room
        13107,  // Abandoned Mine 2nd floor
        14385   // Phantom Muspah Arena
    ));

    /**
     * Regions that should ALWAYS be treated as surface/overworld, even if the
     * Y coordinate would otherwise flag them as underground.
     */
    private static final Set<Integer> FORCE_SURFACE_REGIONS = new HashSet<>(Arrays.asList(
        10041   // Reported: needs overworld sky
    ));

    private final Client client;

    @Inject
    public UndergroundDetector(Client client)
    {
        this.client = client;
    }

    /**
     * Check if the player is in a Player-Owned House instance.
     * POH regions cluster in chunk coordinates X=29-30, Y=85-115, which are
     * well outside the surface world range. We match any instanced region
     * in that coordinate space. This works for your own house and other
     * players' houses regardless of location or size.
     */
    public boolean isInPlayerHouse()
    {
        if (!client.isInInstancedRegion()) return false;

        int[] regions = client.getMapRegions();
        if (regions == null) return false;

        for (int region : regions)
        {
            int chunkX = region >> 8;
            int chunkY = region & 0xFF;

            // POH chunks fall in X=29-30, Y=85-115
            if (chunkX >= 29 && chunkX <= 30 && chunkY >= 85 && chunkY <= 115)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the player is currently in an underground/dungeon area.
     *
     * Priority:
     * 1. POH check is handled separately by the caller
     * 2. Force-surface override - NOT underground
     * 3. Force-underground (safe regions) - underground
     * 4. Instance-underground (ambiguous regions + instance/plane check) - underground
     * 5. Y coordinate fallback - Y > 8000 = underground
     */
    public boolean isUnderground()
    {
        if (client.getLocalPlayer() == null) return false;

        int[] regions = client.getMapRegions();

        if (regions != null)
        {
            // 1. Force-surface takes highest priority
            for (int region : regions)
            {
                if (FORCE_SURFACE_REGIONS.contains(region))
                {
                    return false;
                }
            }

            // 2. Safe force-underground - no surface overlap
            for (int region : regions)
            {
                if (FORCE_UNDERGROUND_REGIONS.contains(region))
                {
                    return true;
                }
            }

			// 3. Ambiguous regions - only count as underground if instanced
            boolean isInstance = client.isInInstancedRegion();

            if (isInstance)
            {
                for (int region : regions)
                {
                    if (INSTANCE_UNDERGROUND_REGIONS.contains(region))
                    {
                        return true;
                    }
                }
            }
        }

        // 4. Fallback: standard Y coordinate check
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        if (wp == null) return false;

        return wp.getY() > UNDERGROUND_Y_THRESHOLD;
    }

    /**
     * Get the current region IDs for debug/logging purposes.
     */
    public int[] getCurrentRegions()
    {
        return client.getMapRegions();
    }

    /**
     * Check if the current area is an instance.
     */
    public boolean isInInstance()
    {
        return client.isInInstancedRegion();
    }
}