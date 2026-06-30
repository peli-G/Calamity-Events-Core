package com.peli.eventqueue.listeners;

import com.peli.eventqueue.EventQueuePlugin;
import com.peli.eventqueue.data.PlayerDataManager;
import com.peli.eventqueue.data.QueueSpawnManager;
import com.peli.eventqueue.data.SavedPlayerData;
import com.peli.eventqueue.gui.RestoreGUI;
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

    private final EventQueuePlugin plugin;

    public PlayerConnectionListener(EventQueuePlugin plugin) {
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
        boolean isEvent  = plugin.isEventWorld(to);

        plugin.debug("World change: " + player.getName() + " '" + from.getName() + "' → '" + to.getName()
                + "' (wasEvent=" + wasEvent + ", isEvent=" + isEvent + ").");

        if (!isEvent || wasEvent) return;

        handleEnterEventWorld(player, "world change into '" + to.getName() + "'");
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
        } else {
            plugin.debug("Resetting " + player.getName() + " to actor state.");
            resetToActor(player, plugin.getQueueSpawnManager());
        }
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
