package com.peli.calamityevents.data;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Persists the {queue.spawn} location from the original Skript to
 * queuespawn.yml in the plugin's data folder, so it survives restarts.
 */
public class QueueSpawnManager {

    private final CalamityEventsCore plugin;
    private final File file;
    private Location queueSpawn;

    public QueueSpawnManager(CalamityEventsCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "queuespawn.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            queueSpawn = null;
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String worldName = yaml.getString("world");
        if (worldName == null) {
            queueSpawn = null;
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            // World probably just hasn't finished loading yet at startup.
            // It will be picked up correctly the next time setQueueSpawn runs,
            // or you can restart once all Multiverse worlds are loaded.
            plugin.getLogger().warning("Queue spawn world '" + worldName + "' is not currently loaded.");
            queueSpawn = null;
            return;
        }

        double x = yaml.getDouble("x");
        double y = yaml.getDouble("y");
        double z = yaml.getDouble("z");
        float yaw = (float) yaml.getDouble("yaw");
        float pitch = (float) yaml.getDouble("pitch");

        queueSpawn = new Location(world, x, y, z, yaw, pitch);
    }

    public void setQueueSpawn(Location location) {
        this.queueSpawn = location.clone();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", location.getWorld().getName());
        yaml.set("x", location.getX());
        yaml.set("y", location.getY());
        yaml.set("z", location.getZ());
        yaml.set("yaw", (double) location.getYaw());
        yaml.set("pitch", (double) location.getPitch());

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save queue spawn to disk.", e);
        }
    }

    public Location getQueueSpawn() {
        return queueSpawn == null ? null : queueSpawn.clone();
    }

    public boolean hasQueueSpawn() {
        return queueSpawn != null;
    }
}
