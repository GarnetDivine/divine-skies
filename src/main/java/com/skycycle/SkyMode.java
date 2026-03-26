package com.skycycle;

/**
 * Maps to 117 HD's DefaultSkyColor enum values.
 * The configValue strings must exactly match the enum constant names
 * in rs117.hd.config.DefaultSkyColor.
 */
public enum SkyMode
{
    HD_BLUE("117 HD Blue", "DEFAULT"),
    RUNELITE_SKYBOX("RuneLite Skybox", "RUNELITE_SKYBOX"),
    OSRS_BLACK("Old School Black", "OLD_SCHOOL_BLACK"),
    CUSTOM("Custom Color", "CUSTOM");

    private final String displayName;

    /**
     * The value to write into 117 HD's config for defaultSkyColor.
     * For CUSTOM, we handle it differently — we set the fogColor directly.
     */
    private final String hdConfigValue;

    SkyMode(String displayName, String hdConfigValue)
    {
        this.displayName = displayName;
        this.hdConfigValue = hdConfigValue;
    }

    public String getDisplayName() { return displayName; }
    public String getHdConfigValue() { return hdConfigValue; }

    @Override
    public String toString()
    {
        return displayName;
    }
}
