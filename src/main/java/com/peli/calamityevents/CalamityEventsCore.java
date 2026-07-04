package com.peli.calamityevents;

import com.peli.calamityevents.commands.CloseEventQueueCommand;
import com.peli.calamityevents.commands.DelWarpCommand;
import com.peli.calamityevents.commands.EventQueueAdminCommand;
import com.peli.calamityevents.commands.ImmortalCommand;
import com.peli.calamityevents.commands.JoinQueueCommand;
import com.peli.calamityevents.commands.ListWarpsCommand;
import com.peli.calamityevents.commands.OpenEventQueueCommand;
import com.peli.calamityevents.commands.QueueInfoCommand;
import com.peli.calamityevents.commands.SetQueueSpawnCommand;
import com.peli.calamityevents.commands.SetWarpCommand;
import com.peli.calamityevents.commands.WarpCommand;
import com.peli.calamityevents.data.PlayerDataManager;
import com.peli.calamityevents.data.QueueSpawnManager;
import com.peli.calamityevents.data.SavedPlayerData;
import com.peli.calamityevents.data.WarpManager;
import com.peli.calamityevents.gui.RestoreGUIListener;
import com.peli.calamityevents.listeners.ImmortalListener;
import com.peli.calamityevents.listeners.PlayerConnectionListener;
import com.peli.calamityevents.listeners.PlayerDeathListener;
import com.peli.calamityevents.listeners.QueueGateListener;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CalamityEventsCore extends JavaPlugin {

    public static final String QUEUE_OPERATOR_PERMISSION = "queue.join.op";
    public static final String QUEUE_TRUSTED_PERMISSION = "queue.join.trusted";

    // Half a heart, in Bukkit's health units (1.0 = half a heart, 2.0 = one full heart).
    // Immortal players are never allowed to drop below this via normal damage.
    public static final double IMMORTAL_HEALTH_FLOOR = 1.0;

    private QueueSpawnManager queueSpawnManager;
    private PlayerDataManager playerDataManager;
    private WarpManager warpManager;

    private List<String> eventWorlds;
    private List<String> warpWorlds;
    private String savePermission;
    private boolean debug;

    private boolean queueOpen = false;
    // True when the queue was opened via "/openqueue trusted" — only players
    // with QUEUE_TRUSTED_PERMISSION can join while this is active, regardless
    // of whether the default group holds queue.join.event.
    private boolean trustedOnlyMode = false;

    // Players currently looking at the entry restore GUI — quit-save is
    // suppressed for these to avoid clobbering their existing save with lobby gear.
    private final Set<UUID> pendingRestorePlayers = new HashSet<>();

    // In-memory death snapshots: captured on PlayerDeathEvent, consumed by the
    // death restore GUI. Not persisted to disk; the quit-save is the fallback.
    private final Map<UUID, SavedPlayerData> deathSnapshots = new HashMap<>();

    // Players with /immortal toggled on. In-memory only, resets on restart —
    // this is meant as a per-session recording-safety toggle, not a
    // persistent player attribute.
    private final Set<UUID> immortalPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        this.queueSpawnManager = new QueueSpawnManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.warpManager = new WarpManager(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new RestoreGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new QueueGateListener(this), this);
        getServer().getPluginManager().registerEvents(new ImmortalListener(this), this);

        SetQueueSpawnCommand setQueueSpawnCommand = new SetQueueSpawnCommand(this);
        getCommand("setqueuespawn").setExecutor(setQueueSpawnCommand);
        getCommand("setqueuespawn").setTabCompleter(setQueueSpawnCommand);

        JoinQueueCommand joinQueueCommand = new JoinQueueCommand(this);
        getCommand("joinqueue").setExecutor(joinQueueCommand);
        getCommand("joinqueue").setTabCompleter(joinQueueCommand);

        OpenEventQueueCommand openEventQueueCommand = new OpenEventQueueCommand(this);
        getCommand("openqueue").setExecutor(openEventQueueCommand);
        getCommand("openqueue").setTabCompleter(openEventQueueCommand);

        CloseEventQueueCommand closeEventQueueCommand = new CloseEventQueueCommand(this);
        getCommand("closequeue").setExecutor(closeEventQueueCommand);
        getCommand("closequeue").setTabCompleter(closeEventQueueCommand);

        QueueInfoCommand queueInfoCommand = new QueueInfoCommand(this);
        getCommand("queueinfo").setExecutor(queueInfoCommand);
        getCommand("queueinfo").setTabCompleter(queueInfoCommand);

        EventQueueAdminCommand eventQueueAdminCommand = new EventQueueAdminCommand(this);
        getCommand("calamityeventscore").setExecutor(eventQueueAdminCommand);
        getCommand("calamityeventscore").setTabCompleter(eventQueueAdminCommand);

        SetWarpCommand setWarpCommand = new SetWarpCommand(this);
        getCommand("setwarp").setExecutor(setWarpCommand);
        getCommand("setwarp").setTabCompleter(setWarpCommand);

        WarpCommand warpCommand = new WarpCommand(this);
        getCommand("warp").setExecutor(warpCommand);
        getCommand("warp").setTabCompleter(warpCommand);

        DelWarpCommand delWarpCommand = new DelWarpCommand(this);
        getCommand("delwarp").setExecutor(delWarpCommand);
        getCommand("delwarp").setTabCompleter(delWarpCommand);

        ListWarpsCommand listWarpsCommand = new ListWarpsCommand(this);
        getCommand("listwarps").setExecutor(listWarpsCommand);
        getCommand("listwarps").setTabCompleter(listWarpsCommand);

        ImmortalCommand immortalCommand = new ImmortalCommand(this);
        getCommand("immortal").setExecutor(immortalCommand);
        getCommand("immortal").setTabCompleter(immortalCommand);

        getLogger().info("CalamityEventsCore enabled. Watching " + eventWorlds.size() + " event world(s): " + eventWorlds);
        getLogger().info("Warps active in " + warpWorlds.size() + " world(s): " + warpWorlds);
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
        this.warpWorlds = getConfig().getStringList("warp-worlds");
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

    public boolean isWarpWorld(World world) {
        if (world == null) return false;
        for (String name : warpWorlds) {
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
    public WarpManager getWarpManager()                  { return warpManager; }
    public Set<UUID> getPendingRestorePlayers()          { return pendingRestorePlayers; }
    public Map<UUID, SavedPlayerData> getDeathSnapshots(){ return deathSnapshots; }
    public boolean isQueueOpen()                         { return queueOpen; }
    public void setQueueOpen(boolean open)               { this.queueOpen = open; }
    public boolean isTrustedOnlyMode()                   { return trustedOnlyMode; }
    public void setTrustedOnlyMode(boolean trustedOnly)  { this.trustedOnlyMode = trustedOnly; }

    public boolean isImmortal(Player player) {
        return immortalPlayers.contains(player.getUniqueId());
    }

    /** @return the new state (true if now immortal, false if now off) */
    public boolean toggleImmortal(Player player) {
        UUID uuid = player.getUniqueId();
        if (immortalPlayers.remove(uuid)) {
            return false;
        }
        immortalPlayers.add(uuid);
        return true;
    }
}
