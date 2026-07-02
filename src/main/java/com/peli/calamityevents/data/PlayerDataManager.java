package com.peli.calamityevents.data;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private final CalamityEventsCore plugin;
    private final File folder;

    public PlayerDataManager(CalamityEventsCore plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) folder.mkdirs();
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    public boolean hasData(UUID uuid) {
        return fileFor(uuid).exists();
    }

    // -------------------------------------------------------------------------
    // SNAPSHOT (in-memory only — used for death restore)
    // -------------------------------------------------------------------------

    public SavedPlayerData snapshotPlayer(Player player) {
        PlayerInventory inv = player.getInventory();

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

        // Respawn location
        Location respawn = player.getRespawnLocation();

        // Potions
        Collection<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());

        // Ender chest
        ItemStack[] ec = new ItemStack[player.getEnderChest().getSize()];
        for (int i = 0; i < ec.length; i++) {
            ec[i] = safeItem(player.getEnderChest().getItem(i));
        }

        return new SavedPlayerData(
                safeArray(inv.getContents()),
                safeItem(inv.getItemInOffHand()),
                safeArray(inv.getArmorContents()),
                maxHealth,
                Math.max(0, Math.min(player.getHealth(), maxHealth)),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getLevel(),
                player.getExp(),
                player.getLocation().clone(),
                respawn != null ? respawn.clone() : null,
                effects,
                player.getGameMode(),
                ec,
                player.getArrowsInBody()
        );
    }

    private ItemStack[] safeArray(ItemStack[] items) {
        ItemStack[] arr = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) arr[i] = safeItem(items[i]);
        return arr;
    }

    public void savePlayerData(Player player) {
        PlayerInventory inv = player.getInventory();
        YamlConfiguration yaml = new YamlConfiguration();

        // Inventory + armor
        yaml.set("inventory", safeList(inv.getContents()));
        yaml.set("armor",     safeList(inv.getArmorContents()));
        yaml.set("offhand",   safeItem(inv.getItemInOffHand()));

        // HP
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        yaml.set("max-health",  maxHealth);
        yaml.set("health",      Math.max(0, Math.min(player.getHealth(), maxHealth)));

        // Hunger / saturation / exhaustion
        yaml.set("food-level",  player.getFoodLevel());
        yaml.set("saturation",  (double) player.getSaturation());
        yaml.set("exhaustion",  (double) player.getExhaustion());

        // XP
        yaml.set("exp-level",    player.getLevel());
        yaml.set("exp-progress", (double) player.getExp());

        // Location
        Location loc = player.getLocation();
        yaml.set("location.world", loc.getWorld().getName());
        yaml.set("location.x",     loc.getX());
        yaml.set("location.y",     loc.getY());
        yaml.set("location.z",     loc.getZ());
        yaml.set("location.yaw",   (double) loc.getYaw());
        yaml.set("location.pitch", (double) loc.getPitch());

        // Respawn location (may be null)
        Location respawn = player.getRespawnLocation();
        if (respawn != null && respawn.getWorld() != null) {
            yaml.set("respawn.world", respawn.getWorld().getName());
            yaml.set("respawn.x",     respawn.getX());
            yaml.set("respawn.y",     respawn.getY());
            yaml.set("respawn.z",     respawn.getZ());
        }

        // Active potion effects
        List<Map<String, Object>> potionData = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            potionData.add(effect.serialize());
        }
        yaml.set("potions", potionData);

        // Gamemode
        yaml.set("gamemode", player.getGameMode().name());

        // Ender chest
        yaml.set("enderchest", safeList(player.getEnderChest().getContents()));

        // Arrows in body
        yaml.set("arrows-in-body", player.getArrowsInBody());

        try {
            yaml.save(fileFor(player.getUniqueId()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getName(), e);
        }
    }

    // -------------------------------------------------------------------------
    // LOAD
    // -------------------------------------------------------------------------

    public SavedPlayerData loadPlayerData(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) return null;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        // Location (required — if the world is gone we bail)
        String worldName = yaml.getString("location.world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Saved location world '" + worldName + "' isn't loaded; cannot restore for " + uuid);
            return null;
        }
        Location location = new Location(world,
                yaml.getDouble("location.x"), yaml.getDouble("location.y"), yaml.getDouble("location.z"),
                (float) yaml.getDouble("location.yaw"), (float) yaml.getDouble("location.pitch"));

        // Respawn location (optional)
        Location respawn = null;
        String respawnWorld = yaml.getString("respawn.world");
        if (respawnWorld != null) {
            World rw = Bukkit.getWorld(respawnWorld);
            if (rw != null) {
                respawn = new Location(rw,
                        yaml.getDouble("respawn.x"), yaml.getDouble("respawn.y"), yaml.getDouble("respawn.z"));
            }
        }

        // Potions
        List<PotionEffect> effects = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("potions")) {
            try {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<?, ?> e : raw.entrySet()) map.put(e.getKey().toString(), e.getValue());
                effects.add(new PotionEffect(map));
            } catch (Exception e) {
                plugin.getLogger().warning("Could not deserialize a potion effect for " + uuid + ": " + e.getMessage());
            }
        }

        // Gamemode
        GameMode gameMode = GameMode.SURVIVAL;
        String gmStr = yaml.getString("gamemode");
        if (gmStr != null) {
            try { gameMode = GameMode.valueOf(gmStr); } catch (IllegalArgumentException ignored) {}
        }

        return new SavedPlayerData(
                listToArray(yaml.getList("inventory")),
                yaml.getItemStack("offhand", new ItemStack(Material.AIR)),
                listToArray(yaml.getList("armor")),
                yaml.getDouble("max-health", 20.0),
                yaml.getDouble("health", 20.0),
                yaml.getInt("food-level", 20),
                (float) yaml.getDouble("saturation", 0.0),
                (float) yaml.getDouble("exhaustion", 0.0),
                yaml.getInt("exp-level", 0),
                (float) yaml.getDouble("exp-progress", 0.0),
                location, respawn,
                effects, gameMode,
                listToArray(yaml.getList("enderchest")),
                yaml.getInt("arrows-in-body", 0)
        );
    }

    // -------------------------------------------------------------------------
    // APPLY (full)
    // -------------------------------------------------------------------------

    public void applyPlayerData(Player player, SavedPlayerData data) {
        applyPlayerData(player, data, EnumSet.allOf(RestoreOption.class));
    }

    // -------------------------------------------------------------------------
    // APPLY (selective — used by the restore GUI)
    // -------------------------------------------------------------------------

    public void applyPlayerData(Player player, SavedPlayerData data, Set<RestoreOption> options) {
        PlayerInventory inv = player.getInventory();

        if (options.contains(RestoreOption.INVENTORY)) {
            inv.setContents(data.getInventoryContents());
            inv.setItemInOffHand(data.getOffHand() != null ? data.getOffHand() : new ItemStack(Material.AIR));
        }

        if (options.contains(RestoreOption.ARMOR)) {
            inv.setArmorContents(data.getArmorContents());
        }

        if (options.contains(RestoreOption.MAX_HP)) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(data.getMaxHealth());
        }

        if (options.contains(RestoreOption.HP)) {
            double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.max(0, Math.min(data.getHealth(), maxHp)));
        }

        if (options.contains(RestoreOption.HUNGER)) {
            player.setFoodLevel(data.getFoodLevel());
        }

        if (options.contains(RestoreOption.SATURATION)) {
            player.setSaturation(data.getSaturation());
        }

        if (options.contains(RestoreOption.EXHAUSTION)) {
            player.setExhaustion(data.getExhaustion());
        }

        if (options.contains(RestoreOption.XP)) {
            player.setLevel(data.getLevel());
            player.setExp(Math.max(0f, Math.min(1f, data.getExp())));
        }

        if (options.contains(RestoreOption.LOCATION)) {
            player.teleport(data.getLocation());
        }

        if (options.contains(RestoreOption.SPAWNPOINT)) {
            player.setRespawnLocation(data.getRespawnLocation(), true);
        }

        if (options.contains(RestoreOption.POTIONS)) {
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);
            for (PotionEffect effect : data.getActivePotionEffects()) {
                player.addPotionEffect(effect);
            }
        }

        if (options.contains(RestoreOption.GAMEMODE)) {
            player.setGameMode(data.getGameMode());
        }

        if (options.contains(RestoreOption.ENDER_CHEST)) {
            ItemStack[] ec = data.getEnderChestContents();
            for (int i = 0; i < Math.min(ec.length, player.getEnderChest().getSize()); i++) {
                player.getEnderChest().setItem(i, ec[i]);
            }
        }

        if (options.contains(RestoreOption.ARROWS)) {
            player.setArrowsInBody(data.getArrowsInBody());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<ItemStack> safeList(ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>(items.length);
        for (ItemStack item : items) list.add(safeItem(item));
        return list;
    }

    private ItemStack safeItem(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item;
    }

    private ItemStack[] listToArray(List<?> list) {
        if (list == null) return new ItemStack[0];
        ItemStack[] arr = new ItemStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            arr[i] = (obj instanceof ItemStack) ? (ItemStack) obj : new ItemStack(Material.AIR);
        }
        return arr;
    }
}
