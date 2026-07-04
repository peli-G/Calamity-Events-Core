package com.peli.calamityevents.listeners;

import com.peli.calamityevents.CalamityEventsCore;
import com.peli.calamityevents.commands.QueueInfoCommand;
import com.peli.calamityevents.data.PlayerDataManager;
import com.peli.calamityevents.data.QueueSpawnManager;
import com.peli.calamityevents.data.SavedPlayerData;
import com.peli.calamityevents.gui.RestoreGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerConnectionListener implements Listener {

    private static final ItemStack[] EMPTY_ARMOR = {
        new ItemStack(Material.AIR), new ItemStack(Material.AIR),
        new ItemStack(Material.AIR), new ItemStack(Material.AIR)
    };

    private final CalamityEventsCore plugin;

    public PlayerConnectionListener(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // QUIT — save data if eligible; skip if the player has the restore GUI open
    // (they'd have lobby gear, not event gear, which would corrupt the save).
    // -------------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!plugin.isEventWorld(world)) {
            plugin.debug("Quit: " + player.getName() + " was in '" + world.getName() + "' (not an event world, ignoring).");
            return;
        }

        if (plugin.getPendingRestorePlayers().contains(player.getUniqueId())) {
            plugin.debug("Quit: " + player.getName() + " quit while restore GUI was open — not saving to preserve existing save.");
            plugin.getPendingRestorePlayers().remove(player.getUniqueId());
            return;
        }

        if (player.hasPermission(plugin.getSavePermission())) {
            plugin.debug("Quit: " + player.getName() + " left event world '" + world.getName() + "' with " + plugin.getSavePermission() + " — saving.");
            plugin.getPlayerDataManager().savePlayerData(player);
        } else {
            plugin.debug("Quit: " + player.getName() + " left '" + world.getName() + "' without " + plugin.getSavePermission() + " — nothing to save.");
        }
    }

    // -------------------------------------------------------------------------
    // WORLD CHANGE — fires when an admin/event-start teleports a player in.
    // -------------------------------------------------------------------------

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World from = event.getFrom();
        World to = player.getWorld();

        boolean wasEvent = plugin.isEventWorld(from);
        boolean isEvent = plugin.isEventWorld(to);
        String fromGroup = canonicalWorldGroup(from);
        String toGroup = canonicalWorldGroup(to);
        boolean sameGroup = fromGroup.equalsIgnoreCase(toGroup);

        plugin.debug("World change: " + player.getName() + " '" + from.getName() + "' → '" + to.getName()
                + "' (wasEvent=" + wasEvent + ", isEvent=" + isEvent + ", fromGroup=" + fromGroup + ", toGroup=" + toGroup + ").");

        // Leaving the event world for good (not just a nether/end hop within
        // it) — save immediately rather than waiting for a disconnect. This
        // covers moving back to the lobby, or into any other world entirely
        // (staff testing/building areas, etc.) — any destination outside the
        // event world's own group counts, not just a fixed list of names.
        if (wasEvent && !sameGroup) {
            if (plugin.getPendingRestorePlayers().contains(player.getUniqueId())) {
                plugin.debug("World change: " + player.getName() + " left '" + from.getName()
                        + "' while a restore GUI was open — not saving to preserve existing save.");
            } else if (player.hasPermission(plugin.getSavePermission())) {
                plugin.debug("World change: " + player.getName() + " left event world '" + from.getName()
                        + "' for '" + to.getName() + "' — saving player data.");
                plugin.getPlayerDataManager().savePlayerData(player);
            }
        }

        if (!isEvent) return;

        if (sameGroup) {
            // e.g. world -> world_nether -> world, or world -> world_the_end -> world.
            // Same underlying event, just a dimension hop — don't re-run entry logic
            // (restore GUI, actor reset, queue-operator queueinfo readout, etc).
            plugin.debug("Same event group ('" + toGroup + "') — skipping re-entry logic.");
            return;
        }

        handleEnterEventWorld(player, "world change into '" + to.getName() + "'");
    }

    /**
     * Returns the "base" name of a world for grouping dimension pairs together:
     * world_nether -> world, world_the_end -> world, world -> world.
     * This lets nether/end travel within an event world's own dimensions avoid
     * re-triggering entry logic, without requiring _nether/_the_end to be
     * separately listed in config.yml's event-worlds.
     */
    private String canonicalWorldGroup(World world) {
        if (world == null) return "";
        String lower = world.getName().toLowerCase();
        if (lower.endsWith("_nether")) return lower.substring(0, lower.length() - "_nether".length());
        if (lower.endsWith("_the_end")) return lower.substring(0, lower.length() - "_the_end".length());
        return lower;
    }

    // -------------------------------------------------------------------------
    // JOIN at MONITOR priority — reads the player's final world AFTER Essentials
    // (and every other plugin) has already acted on spawn-on-join. This handles
    // the case where a player's session starts directly inside an event world
    // (e.g. an op who's exempt from spawn-on-join) without any world-change event.
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        plugin.debug("Join (MONITOR): " + player.getName() + " final world = '" + world.getName() + "'.");

        if (!plugin.isEventWorld(world)) {
            plugin.debug("Join: '" + world.getName() + "' is not an event world, ignoring.");
            return;
        }

        handleEnterEventWorld(player, "join into '" + world.getName() + "'");
    }

    // -------------------------------------------------------------------------

    private void handleEnterEventWorld(Player player, String reason) {
        boolean isOperator = player.hasPermission(CalamityEventsCore.QUEUE_OPERATOR_PERMISSION);

        // Queue operators get an automatic status readout every time they
        // step into the event world, regardless of savedata/actor status.
        if (isOperator) {
            plugin.debug("Enter event world (" + reason + "): " + player.getName() + " is a queue operator — sending /queueinfo.");
            QueueInfoCommand.send(player, plugin);
        }

        // A pending death snapshot takes priority over the normal savedata
        // restore GUI: the player is coming back after dying in this event
        // world specifically to be offered their death loot back. The GUI is
        // deferred until they're actually back inside the event world (not
        // shown right at respawn, since respawn usually drops them in the
        // lobby) — see PlayerDeathListener for why the snapshot itself is
        // captured at death time rather than here.
        SavedPlayerData deathSnapshot = plugin.getDeathSnapshots().get(player.getUniqueId());
        if (deathSnapshot != null) {
            plugin.debug("Enter event world (" + reason + "): " + player.getName()
                    + " has a pending death snapshot — showing death restore GUI.");
            plugin.getPendingRestorePlayers().add(player.getUniqueId());
            RestoreGUI.openForDeath(player, deathSnapshot);
            return;
        }

        boolean hasPermission = player.hasPermission(plugin.getSavePermission());
        plugin.debug("Enter event world (" + reason + "): " + player.getName()
                + " — hasPermission=" + hasPermission);

        if (hasPermission) {
            SavedPlayerData saved = plugin.getPlayerDataManager().loadPlayerData(player.getUniqueId());
            if (saved != null) {
                plugin.debug("Showing restore GUI to " + player.getName() + ".");
                plugin.getPendingRestorePlayers().add(player.getUniqueId());
                RestoreGUI.open(player, saved);
            } else {
                plugin.debug("No saved data for " + player.getName() + " — leaving as-is.");
            }
            return;
        }

        // Queue operators are staff, not queue actors — this branch used to
        // fall through to resetToActor() below for any operator who didn't
        // also separately hold event.savedata, silently stripping their
        // inventory and teleporting them to the queue spawn every time they
        // walked in. Operators are never treated as actors, full stop.
        if (isOperator) {
            plugin.debug("Enter event world (" + reason + "): " + player.getName()
                    + " is a queue operator without savedata — leaving them as-is (no actor reset).");
            return;
        }

        // Actor path (queue joiner). The QueueGateListener normally stops these
        // players before they ever arrive via PlayerTeleportEvent, but a player
        // logging in with their session starting directly inside the event world
        // never fires a teleport event, so this is the backup check for that case.
        if (!plugin.isQueueOperatorPresent(player.getWorld())) {
            plugin.debug("Enter event world (" + reason + "): " + player.getName()
                    + " has no operator present — bouncing out instead of resetting to actor.");
            player.sendMessage("§cThe queue can't be joined right now — no queue operator is in the event world.");
            Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.teleport(fallback);
            return;
        }

        plugin.debug("Resetting " + player.getName() + " to actor state.");
        resetToActor(player, plugin.getQueueSpawnManager());
    }

    private void resetToActor(Player player, QueueSpawnManager queueSpawnManager) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(EMPTY_ARMOR.clone());
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(20.0, maxHealth));
        player.setFoodLevel(20);
        player.setSaturation(5f);
        player.setLevel(0);
        player.setExp(0f);
        player.getActivePotionEffects().stream()
                .map(pe -> pe.getType())
                .forEach(player::removePotionEffect);

        if (queueSpawnManager.hasQueueSpawn()) {
            plugin.debug("Teleporting " + player.getName() + " to queue spawn.");
            player.teleport(queueSpawnManager.getQueueSpawn());
        } else {
            plugin.debug("No queue spawn set — " + player.getName() + " was reset but not teleported. Use /setqueuespawn.");
        }
    }
}
