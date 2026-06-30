package com.peli.eventqueue.data;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

/**
 * Snapshot of a player's full state in an event world.
 * Fields match the restore GUI checkboxes exactly.
 */
public class SavedPlayerData {

    // INVENTORY + ARMOR
    private final ItemStack[] inventoryContents;
    private final ItemStack offHand;
    private final ItemStack[] armorContents;

    // HP
    private final double maxHealth;
    private final double health;

    // HUNGER / SATURATION / EXHAUSTION
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;

    // XP
    private final int level;
    private final float exp;

    // LOCATION
    private final Location location;

    // SPAWNPOINT (bed/anchor respawn, may be null)
    private final Location respawnLocation;

    // POTIONS (active effects)
    private final Collection<PotionEffect> activePotionEffects;

    // GAMEMODE
    private final GameMode gameMode;

    // ENDER CHEST
    private final ItemStack[] enderChestContents;

    // ARROWS (stuck in body, cosmetic)
    private final int arrowsInBody;

    public SavedPlayerData(
            ItemStack[] inventoryContents, ItemStack offHand, ItemStack[] armorContents,
            double maxHealth, double health,
            int foodLevel, float saturation, float exhaustion,
            int level, float exp,
            Location location, Location respawnLocation,
            Collection<PotionEffect> activePotionEffects,
            GameMode gameMode, ItemStack[] enderChestContents, int arrowsInBody) {
        this.inventoryContents = inventoryContents;
        this.offHand = offHand;
        this.armorContents = armorContents;
        this.maxHealth = maxHealth;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.exp = exp;
        this.location = location;
        this.respawnLocation = respawnLocation;
        this.activePotionEffects = activePotionEffects;
        this.gameMode = gameMode;
        this.enderChestContents = enderChestContents;
        this.arrowsInBody = arrowsInBody;
    }

    public ItemStack[] getInventoryContents()          { return inventoryContents; }
    public ItemStack getOffHand()                      { return offHand; }
    public ItemStack[] getArmorContents()              { return armorContents; }
    public double getMaxHealth()                       { return maxHealth; }
    public double getHealth()                          { return health; }
    public int getFoodLevel()                          { return foodLevel; }
    public float getSaturation()                       { return saturation; }
    public float getExhaustion()                       { return exhaustion; }
    public int getLevel()                              { return level; }
    public float getExp()                              { return exp; }
    public Location getLocation()                      { return location; }
    public Location getRespawnLocation()               { return respawnLocation; }
    public Collection<PotionEffect> getActivePotionEffects() { return activePotionEffects; }
    public GameMode getGameMode()                      { return gameMode; }
    public ItemStack[] getEnderChestContents()         { return enderChestContents; }
    public int getArrowsInBody()                       { return arrowsInBody; }
}
