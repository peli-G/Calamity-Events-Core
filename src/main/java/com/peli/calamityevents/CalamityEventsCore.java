package com.peli.calamityevents;

import com.peli.calamityevents.commands.CloseEventQueueCommand;
import com.peli.calamityevents.commands.EventQueueAdminCommand;
import com.peli.calamityevents.commands.JoinQueueCommand;
import com.peli.calamityevents.commands.OpenEventQueueCommand;
import com.peli.calamityevents.commands.QueueInfoCommand;
import com.peli.calamityevents.commands.SetQueueSpawnCommand;
import com.peli.calamityevents.data.PlayerDataManager;
import com.peli.calamityevents.data.QueueSpawnManager;
import com.peli.calamityevents.data.SavedPlayerData;
import com.peli.calamityevents.gui.RestoreGUIListener;
import com.peli.calamityevents.listeners.PlayerConnectionListener;
import com.peli.calamityevents.listeners.PlayerDeathListener;
import com.peli.calamityevents.listeners.QueueGateListener;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CalamityEventsCore extends JavaPlugin {

    public static final String QUEUE_OPERATOR_PERMISSION = "queue.join.op";

    private QueueSpawnManager queueSpawnManager;
    private PlayerDataManager playerDataManager;

    private List<String> eventWorlds;
    private String savePermission;
    private boolean debug;

    private boolean queueOpen = false;

    // Players currently looking at the entry restore GUI — quit-save is
    // suppressed for these to avoid clobbering their existing save with lobby gear.
    private final Set<UUID> pendingRestorePlayers = new HashSet<>();

    // In-memory death snapshots: captured on PlayerDeathEvent, consumed by the
    // death restore GUI. Not persisted to disk; the quit-save is the fallback.
    private final Map<UUID, SavedPlayerData> deathSnapshots = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        this.queueSpawnManager = new QueueSpawnManager(this);
        this.playerDataManager = new PlayerDataManager(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new RestoreGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new QueueGateListener(this), this);

        getCommand("setqueuespawn").setExecutor(new SetQueueSpawnCommand(this));
        getCommand("joinqueue").setExecutor(new JoinQueueCommand(this));
        getCommand("openqueue").setExecutor(new OpenEventQueueCommand(this));
        getCommand("closequeue").setExecutor(new CloseEventQueueCommand(this));
        getCommand("queueinfo").setExecutor(new QueueInfoCommand(this));
        getCommand("eventqueue").setExecutor(new EventQueueAdminCommand(this));

        getLogger().info("CalamityEventsCore enabled. Watching " + eventWorlds.size() + " event world(s): " + eventWorlds);
        if (!isLuckPermsAvailable()) {
            getLogger().warning("LuckPerms not found — /openqueue and /closequeue won't be able to modify group permissions.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("CalamityEventsCore disabled.");
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

    /**
     * True if at least one online player who is physically standing in
     * {@code world} currently has {@link #QUEUE_OPERATOR_PERMISSION}.
     * Used to gate queue entry — non-operators can't join the queue unless
     * an operator is already present in the event world.
     */
    public boolean isQueueOperatorPresent(World world) {
        if (world == null) return false;
        for (org.bukkit.entity.Player p : world.getPlayers()) {
            if (p.hasPermission(QUEUE_OPERATOR_PERMISSION)) return true;
        }
        return false;
    }

    public String getSavePermission()                    { return savePermission; }
    public List<String> getEventWorldNames()             { return eventWorlds; }
    public QueueSpawnManager getQueueSpawnManager()      { return queueSpawnManager; }
    public PlayerDataManager getPlayerDataManager()      { return playerDataManager; }
    public Set<UUID> getPendingRestorePlayers()          { return pendingRestorePlayers; }
    public Map<UUID, SavedPlayerData> getDeathSnapshots(){ return deathSnapshots; }
    public boolean isQueueOpen()                         { return queueOpen; }
    public void setQueueOpen(boolean open)               { this.queueOpen = open; }
}
