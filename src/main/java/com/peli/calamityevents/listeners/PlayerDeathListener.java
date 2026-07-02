package com.peli.calamityevents.listeners;

import com.peli.calamityevents.CalamityEventsCore;
import com.peli.calamityevents.data.SavedPlayerData;
import com.peli.calamityevents.gui.RestoreGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerDeathListener implements Listener {

    private final CalamityEventsCore plugin;

    public PlayerDeathListener(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    /**
     * On death: snapshot the player's current state, then cancel item and XP
     * drops so nothing hits the ground. If the player picks No (or closes the
     * GUI), they simply keep whatever vanilla respawn gives them — items are
     * already gone from the world, which is intentional for event play.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!plugin.isEventWorld(player.getWorld())) return;
        if (!player.hasPermission(plugin.getSavePermission())) return;

        plugin.debug("Death: " + player.getName() + " died in event world '" + player.getWorld().getName()
                + "' — snapshotting and cancelling drops.");

        // Snapshot before any other plugin modifies the inventory
        SavedPlayerData snapshot = plugin.getPlayerDataManager().snapshotPlayer(player);
        plugin.getDeathSnapshots().put(player.getUniqueId(), snapshot);

        // Cancel drops and XP so nothing litters the event world
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(false); // ensure vanilla still clears inventory on respawn
    }

    /**
     * On respawn: open the death restore GUI 1 tick later.
     * The 1-tick delay is intentional here — the client needs to finish
     * loading at the respawn point before an inventory GUI can open cleanly.
     * This is a real engine constraint, not a Skript workaround.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getDeathSnapshots().containsKey(player.getUniqueId())) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                plugin.getDeathSnapshots().remove(player.getUniqueId());
                return;
            }
            SavedPlayerData snapshot = plugin.getDeathSnapshots().get(player.getUniqueId());
            if (snapshot == null) return; // already handled

            plugin.debug("Respawn: showing death restore GUI to " + player.getName() + ".");
            RestoreGUI.openForDeath(player, snapshot);
        }, 1L);
    }
}
