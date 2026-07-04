package com.peli.calamityevents.listeners;

import com.peli.calamityevents.CalamityEventsCore;
import com.peli.calamityevents.data.SavedPlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final CalamityEventsCore plugin;

    public PlayerDeathListener(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    /**
     * On death: snapshot the player's current state, then cancel item and XP
     * drops so nothing hits the ground.
     *
     * Note the snapshot's HP is captured at/after the killing blow, so it's
     * 0 (or whatever put them below the death threshold) — that's why HP
     * defaults OFF in the death restore GUI (see RestoreOption); restoring
     * it as-is would instantly kill the player again.
     *
     * The restore GUI itself is NOT shown here or on respawn — respawn
     * usually drops the player in the lobby (bed/anchor spawn, or vanilla
     * world spawn), not back in the event world. Showing a "restore your
     * death loot" GUI while standing in the lobby doesn't make sense and
     * previously caused HP to be restored as 0 before the player had even
     * left the lobby, instantly killing them there.
     *
     * Instead, the snapshot is stored in plugin.getDeathSnapshots() and
     * consumed by PlayerConnectionListener.handleEnterEventWorld() the next
     * time this player actually re-enters the event world.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!plugin.isEventWorld(player.getWorld())) return;
        if (!player.hasPermission(plugin.getSavePermission())) return;

        plugin.debug("Death: " + player.getName() + " died in event world '" + player.getWorld().getName()
                + "' — snapshotting and cancelling drops. Restore GUI deferred until they re-enter the event world.");

        // Snapshot before any other plugin modifies the inventory
        SavedPlayerData snapshot = plugin.getPlayerDataManager().snapshotPlayer(player);
        plugin.getDeathSnapshots().put(player.getUniqueId(), snapshot);

        // Cancel drops and XP so nothing litters the event world
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(false); // ensure vanilla still clears inventory on respawn
    }
}
