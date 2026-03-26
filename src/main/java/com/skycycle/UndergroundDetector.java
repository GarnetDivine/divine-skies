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
 * Detection uses three layers:
 * 1. FORCE_SURFACE_REGIONS — always treated as surface, even if Y > 8000.
 * 2. FORCE_UNDERGROUND_REGIONS — always underground (region IDs that don't overlap surface chunks).
 * 3. INSTANCE_UNDERGROUND_REGIONS — underground ONLY when the player is in an instanced region
 *    or on a non-zero plane. These region IDs share chunk coordinates with surface-world areas,
 *    so we can't use the region ID alone without false positives.
 * 4. Y coordinate fallback — if no region override matches, Y > 8000 = underground.
 *
 * To find new region IDs: enable RuneLite's "Developer Tools" plugin.
 * The region overlay shows the ID on screen. Add safe IDs to FORCE_UNDERGROUND_REGIONS.
 * If the region overlaps with a surface area, add it to INSTANCE_UNDERGROUND_REGIONS instead.
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
     * non-ground plane. These share chunk coordinates with surface-world areas,
     * so region ID alone would cause false positives (e.g. Burthorpe Games Room
     * region overlaps with the Burthorpe/Varrock surface area).
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
        10041,   // Reported: needs overworld sky, gets flashbanged at dawn otherwise
    12850,  // Brutus Arena (instanced but visually outdoors)
    12851,  // Brutus Arena
    12852,  // Brutus Arena
    13106,  // Brutus Arena
    13108   // Brutus Arena
    ));

    // Player-Owned House instance regions
    private static final Set<Integer> POH_REGIONS = new HashSet<>(Arrays.asList(
        7513,
        7769
    ));

    private final Client client;

    @Inject
    public UndergroundDetector(Client client)
    {
        this.client = client;
    }

    /**
     * Determine if the player is currently in an underground/dungeon area.
     *
     * Priority:
     * 1. POH check is handled separately by the caller
     * 2. Force-surface override → NOT underground
     * 3. Force-underground (safe regions) → underground
     * 4. Instance-underground (ambiguous regions + instance/plane check) → underground
     * 5. Y coordinate fallback → Y > 8000 = underground
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

            // 2. Safe force-underground — no surface overlap, region ID is sufficient
            for (int region : regions)
            {
                if (FORCE_UNDERGROUND_REGIONS.contains(region))
                {
                    return true;
                }
            }

            // 3. Ambiguous regions — only count as underground if instanced or non-ground plane
            boolean isInstance = client.isInInstancedRegion();
            int plane = client.getPlane();

            if (isInstance || plane > 0)
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
     * Check if the player is in a Player-Owned House instance.
     */
    public boolean isInPlayerHouse()
    {
        int[] regions = client.getMapRegions();
        if (regions == null) return false;

        for (int region : regions)
        {
            if (POH_REGIONS.contains(region)) return true;
        }
        return false;
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
