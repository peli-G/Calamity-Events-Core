package com.peli.eventqueue.gui;

import com.peli.eventqueue.EventQueuePlugin;
import com.peli.eventqueue.data.PlayerDataManager;
import com.peli.eventqueue.data.RestoreOption;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class RestoreGUIListener implements Listener {

    private final EventQueuePlugin plugin;

    public RestoreGUIListener(EventQueuePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RestoreGUIHolder)) return;
        event.setCancelled(true); // never let items be moved in this GUI

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        RestoreGUIHolder holder = (RestoreGUIHolder) event.getInventory().getHolder();

        if (slot == RestoreGUI.SLOT_YES) {
            plugin.debug("RestoreGUI: " + player.getName() + " clicked YES. Options: " + holder.getSelected());
            holder.setClosedByButton(true);
            player.closeInventory();
            plugin.getPendingRestorePlayers().remove(player.getUniqueId());
            plugin.getPlayerDataManager().applyPlayerData(player, holder.getSavedData(), holder.getSelected());
            player.sendMessage("§aRestored your data.");
            return;
        }

        if (slot == RestoreGUI.SLOT_NO) {
            plugin.debug("RestoreGUI: " + player.getName() + " clicked NO — not restoring.");
            holder.setClosedByButton(true);
            player.closeInventory();
            plugin.getPendingRestorePlayers().remove(player.getUniqueId());
            player.sendMessage("§7Skipped restore — starting fresh.");
            return;
        }

        // Toggle buttons
        RestoreOption opt = RestoreOption.forSlot(slot);
        if (opt != null) {
            holder.toggle(opt);
            RestoreGUI.refresh(event.getInventory(), opt, holder.isSelected(opt));
            plugin.debug("RestoreGUI: " + player.getName() + " toggled " + opt.name() + " → " + holder.isSelected(opt));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RestoreGUIHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;

        RestoreGUIHolder holder = (RestoreGUIHolder) event.getInventory().getHolder();
        if (!holder.isClosedByButton()) {
            // Closed with Esc or some other external force — treat as No.
            Player player = (Player) event.getPlayer();
            plugin.debug("RestoreGUI: " + player.getName() + " closed GUI without choosing — treating as No.");
            plugin.getPendingRestorePlayers().remove(player.getUniqueId());
            player.sendMessage("§7Skipped restore — starting fresh.");
        }
    }
}
