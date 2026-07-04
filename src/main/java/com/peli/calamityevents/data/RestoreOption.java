package com.peli.calamityevents.data;

/**
 * The 14 toggleable restore options shown in the restore GUI.
 * slot matches the chest inventory slot used for the toggle button.
 *
 * Two separate default states are tracked: one for the entry GUI (shown
 * when a participant re-enters an event world with existing quit-saved
 * data) and one for the death GUI (shown when they return after dying).
 *
 * HP defaults OFF for the death GUI specifically: a PlayerDeathEvent
 * snapshot captures health at/after the killing blow, which is 0 (or
 * whatever put them below the death threshold). Restoring that value
 * would set the player's health back to 0 and instantly kill them again.
 */
public enum RestoreOption {

    ARMOR      ("Armor",       true,  true,  10),
    INVENTORY  ("Inventory",   true,  true,  11),
    LOCATION   ("Location",    true,  true,  12),
    XP         ("XP",          true,  true,  13),
    MAX_HP     ("Max HP",      true,  true,  14),
    HP         ("HP",          true,  false, 15),  // off by default for death — would instantly kill on restore
    SATURATION ("Saturation",  true,  true,  16),
    HUNGER     ("Hunger",      true,  true,  19),
    EXHAUSTION ("Exhaustion",  true,  true,  20),
    SPAWNPOINT ("Spawnpoint",  true,  true,  21),
    POTIONS    ("Potions",     true,  true,  22),
    GAMEMODE   ("Gamemode",    true,  true,  23),
    ENDER_CHEST("Enderchest",  true,  true,  24),
    ARROWS     ("Arrows",      true,  true,  25);

    private final String displayName;
    private final boolean checkedByDefaultEntry;
    private final boolean checkedByDefaultDeath;
    private final int slot;

    RestoreOption(String displayName, boolean checkedByDefaultEntry, boolean checkedByDefaultDeath, int slot) {
        this.displayName = displayName;
        this.checkedByDefaultEntry = checkedByDefaultEntry;
        this.checkedByDefaultDeath = checkedByDefaultDeath;
        this.slot = slot;
    }

    public String getDisplayName() { return displayName; }
    public int getSlot()           { return slot; }

    /** @param death true for the death-restore GUI, false for the entry-restore GUI */
    public boolean isCheckedByDefault(boolean death) {
        return death ? checkedByDefaultDeath : checkedByDefaultEntry;
    }

    /** Returns the RestoreOption for this slot, or null if none. */
    public static RestoreOption forSlot(int slot) {
        for (RestoreOption opt : values()) {
            if (opt.slot == slot) return opt;
        }
        return null;
    }
}
