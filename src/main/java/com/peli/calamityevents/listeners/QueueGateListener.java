package com.peli.calamityevents.listeners;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Gates entry into event worlds behind queue.join.op presence.
 *
 * PlayerPortalEvent extends PlayerTeleportEvent, so this also catches
 * nether/end portal travel into an event world, not just plugin/command
 * teleports.
 *
 * Queue operators (queue.join.op) and existing participants (event.savedata)
 * are exempt — this only gates players joining as queue actors, since
 * they're the ones the queue.join.event permission is meant for.
 */
public class QueueGateListener implements Listener {

    private final CalamityEventsCore plugin;

    public QueueGateListener(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World to = event.getTo() != null ? event.getTo().getWorld() : null;

        if (!plugin.isEventWorld(to)) return;

        if (player.hasPermission(CalamityEventsCore.QUEUE_OPERATOR_PERMISSION)) return;
        if (player.hasPermission(plugin.getSavePermission())) return; // participants aren't "joining the queue"

        if (!plugin.isQueueOperatorPresent(to)) {
            event.setCancelled(true);
            player.sendMessage("§cThe queue can't be joined right now — no queue operator is in the event world.");
            plugin.debug("Queue gate: blocked " + player.getName() + " from entering '" + to.getName()
                    + "' — no queue operator present.");
        }
    }
}
