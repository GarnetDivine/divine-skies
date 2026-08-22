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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Classifies the player's current location as SURFACE, UNDERGROUND or POH.
 *
 * <p>Detection runs through a prioritised chain. Each step short-circuits the rest:</p>
 *
 * <ol>
 *   <li><b>Varbit gates</b> — server-authoritative state (CoX / ToB / ToA / POH build
 *       mode). These cannot be wrong, so they sit above the player override layer.</li>
 *   <li><b>Player overrides</b> — manual per-region classifications set by the player
 *       with the toggle hotkey. Persisted in our own config namespace.</li>
 *   <li><b>POH template regions</b> — the eight verified POH instance template region
 *       IDs. Covers all 9 portal cities and all house sizes (1x1 through 8x8).</li>
 *   <li><b>FORCE_SURFACE_REGIONS</b> — regions always treated as surface.</li>
 *   <li><b>FORCE_UNDERGROUND_REGIONS</b> — regions always underground (no surface
 *       overlap, so the region ID alone is sufficient).</li>
 *   <li><b>INSTANCE_UNDERGROUND_REGIONS</b> — regions that are underground only when
 *       the player is in an instance (they share chunk coordinates with surface areas).</li>
 *   <li><b>Y coordinate fallback</b> — Y &gt;= 6400 = underground. This is the real
 *       OSRS "underground band" boundary (the previous 8000 threshold missed a large
 *       slice of underground content between 6400 and 8000).</li>
 * </ol>
 *
 * <p>All coordinate and region lookups are <b>instance-aware</b>: in instanced areas
 * (raids, POH, Gauntlet, etc.) the raw world location is a meaningless pseudo-coordinate,
 * so we resolve the canonical template coordinate with
 * {@link WorldPoint#fromLocalInstance(Client, LocalPoint)} first. This is the same
 * approach RuneLite's own GroundMarker and Discord plugins use.</p>
 *
 * <p>To find new region IDs: enable RuneLite's "Developer Tools" plugin, or turn on
 * Divine: Skies' own Developer Mode in the overlay. Then either add the ID to one of
 * the sets below, or simply use the in-game toggle hotkey to set a persistent override.</p>
 */
@Singleton
public class UndergroundDetector
{
    private static final Logger log = LoggerFactory.getLogger(UndergroundDetector.class);

    /**
     * The OSRS underground "band" begins at Y = 6400. Almost all dungeon/cave content
     * lives at or above this line. (The previous threshold of 8000 left a gap.)
     *
     * NOTE: a handful of areas above this line are visually outdoors — Zanaris (~4400),
     * Puro-Puro (~4300) are actually below it, but POH templates (~5200-5700) and some
     * instance pseudo-coords can land above it. Those are caught by earlier layers
     * (POH region match, FORCE_SURFACE_REGIONS) before the fallback ever runs.
     */
    private static final int UNDERGROUND_Y_THRESHOLD = 6400;

    // --- Varbit IDs (server-authoritative; integer IDs are stable across RL versions) ---
    private static final int VARBIT_IN_RAID = 5432;        // Chambers of Xeric: 1 while in a raid
    private static final int VARBIT_THEATRE_OF_BLOOD = 6440; // ToB: >=2 inside/spectating
    private static final int VARBIT_TOA_RAID_LEVEL = 14380;  // ToA: >0 invocation level when inside
    private static final int VARBIT_POH_BUILD_MODE = 2176;   // 1 while in POH build mode

    /**
     * The eight POH instance template region IDs. Once the player is INSIDE a POH
     * instance, the portal city is irrelevant — the instance always uses these
     * template regions regardless of which of the 9 portals was used or the house size.
     *
     * These are the same IDs RuneLite hardcodes internally as REGION_POH.
     */
    private static final Set<Integer> POH_TEMPLATE_REGIONS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            7513, 7514, 7769, 7770, 8025, 8026, 8281, 8282
        )));

    /**
     * Regions that are ALWAYS underground. These have chunk coordinates that don't
     * overlap with any surface-world area, so the region ID alone is sufficient.
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
        12127,  // Ancient Guthixian Temple / Gauntlet lobby
        12192,  // Camdozaal / Rogues' Den
        12933,  // Zalcano
        13654   // Leviathan Path
    ));

    /**
     * Regions that are underground ONLY when the player is in an instance. These share
     * chunk coordinates with surface-world areas, so the region ID alone would cause
     * false positives.
     *
     * NB: several entries from earlier builds were flagged during research as
     * questionable region IDs. They are kept here (so behaviour doesn't silently change
     * for existing users) but marked. Verify against the live cache / Developer Mode
     * before trusting them, or let player overrides correct them in the field.
     */
    private static final Set<Integer> INSTANCE_UNDERGROUND_REGIONS = new HashSet<>(Arrays.asList(
        9781,   // Tree Gnome Village Maze Cave
        11050,  // Ape Atoll Cooking Cave
        11058,  // (flagged) reported as Lassar Undercity — verify; may be surface Brimhaven
        12132,  // Duke Sucellus / Lassar Undercity area
        12598,  // (flagged) reported as Burthorpe Games Room — verify region ID
        13107,  // Abandoned Mine 2nd floor
        14385   // (flagged) reported as Phantom Muspah — RL uses 11330 for Muspah; verify
    ));

    /**
     * Regions that should ALWAYS be treated as surface/overworld, even if a later layer
     * (e.g. the Y fallback) would otherwise flag them as underground.
     */
    private static final Set<Integer> FORCE_SURFACE_REGIONS = new HashSet<>(Arrays.asList(
        10041   // Reported: needs overworld sky (north Fremennik coastline area)
    ));

    /** Config key under which the serialised override map is persisted. */
    private static final String OVERRIDE_KEY = "regionOverrides";

    private final Client client;
    private final ConfigManager configManager;
    private final SkyCycleConfig config;
    private final Gson gson;

    /** region ID -> forced AreaType. Loaded from config on startup. */
    private final Map<Integer, AreaType> overrides = new HashMap<>();

    @Inject
    public UndergroundDetector(Client client, ConfigManager configManager, SkyCycleConfig config, Gson gson)
    {
        this.client = client;
        this.configManager = configManager;
        this.config = config;
        this.gson = gson;
        loadOverrides();
    }

    // =====================================================================
    // Public API (unchanged signatures so the plugin needs minimal edits)
    // =====================================================================

    /**
     * @return true if the player should currently see an underground (dark) sky.
     */
    public boolean isUnderground()
    {
        return classify() == AreaType.UNDERGROUND;
    }

    /**
     * @return true if the player is in a Player-Owned House.
     */
    public boolean isInPlayerHouse()
    {
        return classify() == AreaType.POH;
    }

    /**
     * Full classification of the current location.
     *
     * @return SURFACE, UNDERGROUND or POH; defaults to SURFACE if state is unknown.
     */
    public AreaType classify()
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return AreaType.SURFACE;
        }

        // 1. Server-authoritative varbit gates. A confirmed raid / build session
        //    cannot be overridden by region heuristics OR by player overrides.
        if (safeVarbit(VARBIT_IN_RAID) == 1) return AreaType.UNDERGROUND;          // CoX
        if (safeVarbit(VARBIT_THEATRE_OF_BLOOD) >= 2) return AreaType.UNDERGROUND; // ToB
        if (safeVarbit(VARBIT_TOA_RAID_LEVEL) > 0) return AreaType.UNDERGROUND;    // ToA
        if (safeVarbit(VARBIT_POH_BUILD_MODE) == 1) return AreaType.POH;           // POH build

        int region = resolveRegionId();

        // 2. Player override layer — wins over all region-based heuristics below.
        if (region >= 0)
        {
            AreaType override = overrides.get(region);
            if (override != null)
            {
                return override;
            }
        }

        // 3. POH template regions.
        if (region >= 0 && POH_TEMPLATE_REGIONS.contains(region))
        {
            return AreaType.POH;
        }

        // 4. Force-surface.
        if (region >= 0 && FORCE_SURFACE_REGIONS.contains(region))
        {
            return AreaType.SURFACE;
        }

        // 5. Force-underground (no surface overlap).
        if (region >= 0 && FORCE_UNDERGROUND_REGIONS.contains(region))
        {
            return AreaType.UNDERGROUND;
        }

        // 6. Instance-only underground regions.
        if (region >= 0 && client.isInInstancedRegion()
            && INSTANCE_UNDERGROUND_REGIONS.contains(region))
        {
            return AreaType.UNDERGROUND;
        }

        // 7. Y coordinate fallback.
        WorldPoint wp = resolveWorldPoint();
        if (wp != null && wp.getY() >= UNDERGROUND_Y_THRESHOLD)
        {
            return AreaType.UNDERGROUND;
        }

        return AreaType.SURFACE;
    }

    // =====================================================================
    // Player override system
    // =====================================================================

    /**
     * Cycle the override for the player's CURRENT region through:
     * (none) -> UNDERGROUND -> SURFACE -> POH -> (none).
     *
     * <p>Must be called on the client thread (region/coordinate reads are not
     * thread-safe).</p>
     *
     * @return a human-readable description of the new state, or null if no region
     *         could be resolved (e.g. not logged in).
     */
    public String cycleOverrideAtCurrentLocation()
    {
        if (!config.allowManualOverrides())
        {
            return null;
        }

        int region = resolveRegionId();
        if (region < 0)
        {
            return null;
        }

        AreaType current = overrides.get(region);
        AreaType next;
        if (current == null)
        {
            next = AreaType.UNDERGROUND;
        }
        else
        {
            switch (current)
            {
                case UNDERGROUND:
                    next = AreaType.SURFACE;
                    break;
                case SURFACE:
                    next = AreaType.POH;
                    break;
                case POH:
                default:
                    next = null; // clear -> back to automatic detection
                    break;
            }
        }

        if (next == null)
        {
            overrides.remove(region);
        }
        else
        {
            overrides.put(region, next);
        }
        saveOverrides();

        return next == null
            ? "Region " + region + " override cleared (auto-detect)"
            : "Region " + region + " set to " + next.getDisplayName();
    }

    /**
     * Remove every saved override. Returns the number cleared.
     */
    public int clearAllOverrides()
    {
        int n = overrides.size();
        overrides.clear();
        saveOverrides();
        return n;
    }

    /**
     * @return the override for the current region, or null if none / not resolvable.
     */
    public AreaType getCurrentOverride()
    {
        int region = resolveRegionId();
        return region < 0 ? null : overrides.get(region);
    }

    public int getOverrideCount()
    {
        return overrides.size();
    }

    private void loadOverrides()
    {
        overrides.clear();
        String json = configManager.getConfiguration(SkyCycleConfig.CONFIG_GROUP, OVERRIDE_KEY);
        if (json == null || json.isEmpty())
        {
            return;
        }
        try
        {
            Type type = new TypeToken<Map<Integer, AreaType>>() {}.getType();
            Map<Integer, AreaType> saved = gson.fromJson(json, type);
            if (saved != null)
            {
                overrides.putAll(saved);
                log.debug("Loaded {} region override(s)", overrides.size());
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to parse saved region overrides; starting fresh", e);
        }
    }

    private void saveOverrides()
    {
        try
        {
            String json = gson.toJson(overrides);
            configManager.setConfiguration(SkyCycleConfig.CONFIG_GROUP, OVERRIDE_KEY, json);
        }
        catch (Exception e)
        {
            log.warn("Failed to persist region overrides", e);
        }
    }

    // =====================================================================
    // Instance-aware coordinate / region resolution
    // =====================================================================

    /**
     * Resolve the player's true region ID, accounting for instances.
     *
     * <p>In an instanced area the raw world location is a pseudo-coordinate that does
     * not correspond to the real template region, so we decode the template chunk via
     * {@link WorldPoint#fromLocalInstance}. This call can throw
     * ArrayIndexOutOfBoundsException if invoked mid-load (see RL issues #18681/#1667),
     * so it is guarded.</p>
     *
     * @return region ID, or -1 if it could not be resolved.
     */
    public int resolveRegionId()
    {
        WorldPoint wp = resolveWorldPoint();
        return wp == null ? -1 : wp.getRegionID();
    }

    /**
     * Resolve the player's canonical WorldPoint, instance-aware and crash-guarded.
     */
    private WorldPoint resolveWorldPoint()
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return null;
        }

        if (!client.isInInstancedRegion())
        {
            return local.getWorldLocation();
        }

        // Instanced: decode the template coordinate. Guard against transient
        // mid-load states that can index out of bounds.
        try
        {
            LocalPoint lp = local.getLocalLocation();
            if (lp == null)
            {
                return local.getWorldLocation();
            }
            return WorldPoint.fromLocalInstance(client, lp);
        }
        catch (Exception e)
        {
            log.debug("fromLocalInstance failed (transient load state?); falling back", e);
            return local.getWorldLocation();
        }
    }

    private int safeVarbit(int id)
    {
        try
        {
            return client.getVarbitValue(id);
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    // =====================================================================
    // Debug / overlay helpers
    // =====================================================================

    /** Raw loaded regions (for the dev overlay). */
    public int[] getCurrentRegions()
    {
        return client.getMapRegions();
    }

    /** Instance-aware resolved region (for the dev overlay). */
    public int getResolvedRegion()
    {
        return resolveRegionId();
    }

    public boolean isInInstance()
    {
        return client.isInInstancedRegion();
    }
}
