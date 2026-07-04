package com.peli.calamityevents.data;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Persists per-world warps to warps.yml, structured as:
 *
 * worlds:
 *   <world name>:
 *     <warp name>:
 *       x: ...
 *       y: ...
 *       z: ...
 *       yaw: ...
 *       pitch: ...
 *
 * Warps are scoped to the world they were set in — /warp and /listwarps
 * only look at the current world's own warps. A warp named "spawn" in
 * "build" is entirely independent from a warp named "spawn" in "world".
 */
public class WarpManager {

    private final CalamityEventsCore plugin;
    private final File file;
    private YamlConfiguration yaml;

    public WarpManager(CalamityEventsCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        load();
    }

    private void load() {
        yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save warps.yml", e);
        }
    }

    private String path(World world, String warpName) {
        return "worlds." + world.getName() + "." + warpName.toLowerCase();
    }

    public void setWarp(World world, String warpName, Location location) {
        String base = path(world, warpName);
        yaml.set(base + ".x", location.getX());
        yaml.set(base + ".y", location.getY());
        yaml.set(base + ".z", location.getZ());
        yaml.set(base + ".yaw", (double) location.getYaw());
        yaml.set(base + ".pitch", (double) location.getPitch());
        save();
    }

    public boolean hasWarp(World world, String warpName) {
        return yaml.contains(path(world, warpName));
    }

    public Location getWarp(World world, String warpName) {
        String base = path(world, warpName);
        if (!yaml.contains(base)) return null;

        double x = yaml.getDouble(base + ".x");
        double y = yaml.getDouble(base + ".y");
        double z = yaml.getDouble(base + ".z");
        float yaw = (float) yaml.getDouble(base + ".yaw");
        float pitch = (float) yaml.getDouble(base + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Returns true if a warp was actually removed. */
    public boolean deleteWarp(World world, String warpName) {
        String base = path(world, warpName);
        if (!yaml.contains(base)) return false;
        yaml.set(base, null);
        save();
        return true;
    }

    /** Returns the warp names configured for this world, sorted alphabetically. */
    public Set<String> listWarps(World world) {
        ConfigurationSection section = yaml.getConfigurationSection("worlds." + world.getName());
        if (section == null) return Collections.emptySet();
        return new TreeSet<>(section.getKeys(false));
    }
}
