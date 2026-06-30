package com.peli.eventqueue;

import com.peli.eventqueue.commands.CloseEventQueueCommand;
import com.peli.eventqueue.commands.EventQueueAdminCommand;
import com.peli.eventqueue.commands.OpenEventQueueCommand;
import com.peli.eventqueue.commands.QueueInfoCommand;
import com.peli.eventqueue.commands.SetQueueSpawnCommand;
import com.peli.eventqueue.data.PlayerDataManager;
import com.peli.eventqueue.data.QueueSpawnManager;
import com.peli.eventqueue.gui.RestoreGUIListener;
import com.peli.eventqueue.listeners.PlayerConnectionListener;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EventQueuePlugin extends JavaPlugin {

    private QueueSpawnManager queueSpawnManager;
    private PlayerDataManager playerDataManager;

    private List<String> eventWorlds;
    private String savePermission;
    private boolean debug;

    // Queue open/close state (from /openeventqueue and /closeeventqueue)
    private boolean queueOpen = false;

    // Players who currently have the restore GUI open.
    // Quit-save is suppressed for these players to avoid clobbering old saved data
    // with whatever lobby gear they had when they entered the event world.
    private final Set<UUID> pendingRestorePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        this.queueSpawnManager = new QueueSpawnManager(this);
        this.playerDataManager = new PlayerDataManager(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new RestoreGUIListener(this), this);

        getCommand("setqueuespawn").setExecutor(new SetQueueSpawnCommand(this));
        getCommand("openeventqueue").setExecutor(new OpenEventQueueCommand(this));
        getCommand("closeeventqueue").setExecutor(new CloseEventQueueCommand(this));
        getCommand("queueinfo").setExecutor(new QueueInfoCommand(this));
        getCommand("eventqueue").setExecutor(new EventQueueAdminCommand(this));

        getLogger().info("EventQueueSystem enabled. Watching " + eventWorlds.size() + " event world(s): " + eventWorlds);
        if (!isLuckPermsAvailable()) {
            getLogger().warning("LuckPerms not found — /openeventqueue and /closeeventqueue won't be able to modify group permissions.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("EventQueueSystem disabled.");
    }

    public void loadSettings() {
        reloadConfig();
        this.eventWorlds = getConfig().getStringList("event-worlds");
        this.savePermission = getConfig().getString("save-permission", "event.savedata");
        this.debug = getConfig().getBoolean("debug", true);
    }

    public void debug(String message) {
        if (debug) getLogger().info("[debug] " + message);
    }

    public boolean isEventWorld(World world) {
        if (world == null) return false;
        for (String name : eventWorlds) {
            if (name.equalsIgnoreCase(world.getName())) return true;
        }
        return false;
    }

    public boolean isLuckPermsAvailable() {
        return getServer().getPluginManager().getPlugin("LuckPerms") != null;
    }

    // ---- Getters / setters ----

    public String getSavePermission()                { return savePermission; }
    public List<String> getEventWorldNames()         { return eventWorlds; }
    public QueueSpawnManager getQueueSpawnManager()  { return queueSpawnManager; }
    public PlayerDataManager getPlayerDataManager()  { return playerDataManager; }
    public Set<UUID> getPendingRestorePlayers()      { return pendingRestorePlayers; }
    public boolean isQueueOpen()                     { return queueOpen; }
    public void setQueueOpen(boolean open)           { this.queueOpen = open; }
}
