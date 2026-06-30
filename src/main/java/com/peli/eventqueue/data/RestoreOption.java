package com.peli.eventqueue.data;

/**
 * The 14 toggleable restore options shown in the restore GUI.
 * slot matches the chest inventory slot used for the toggle button.
 * Default checked state mirrors the screenshot (Location and Spawnpoint are off).
 */
public enum RestoreOption {

    ARMOR      ("Armor",       true,  10),
    INVENTORY  ("Inventory",   true,  11),
    LOCATION   ("Location",    true,  12),  // checked by default
    XP         ("XP",          true,  13),
    MAX_HP     ("Max HP",      true,  14),
    HP         ("HP",          true,  15),
    SATURATION ("Saturation",  true,  16),
    HUNGER     ("Hunger",      true,  19),
    EXHAUSTION ("Exhaustion",  true,  20),
    SPAWNPOINT ("Spawnpoint",  true,  21),  // checked by default
    POTIONS    ("Potions",     true,  22),
    GAMEMODE   ("Gamemode",    true,  23),
    ENDER_CHEST("Enderchest",  true,  24),
    ARROWS     ("Arrows",      true,  25);

    private final String displayName;
    private final boolean checkedByDefault;
    private final int slot;

    RestoreOption(String displayName, boolean checkedByDefault, int slot) {
        this.displayName = displayName;
        this.checkedByDefault = checkedByDefault;
        this.slot = slot;
    }

    public String getDisplayName()    { return displayName; }
    public boolean isCheckedByDefault(){ return checkedByDefault; }
    public int getSlot()              { return slot; }

    /** Returns the RestoreOption for this slot, or null if none. */
    public static RestoreOption forSlot(int slot) {
        for (RestoreOption opt : values()) {
            if (opt.slot == slot) return opt;
        }
        return null;
    }
}
