package com.peli.calamityevents.gui;

import com.peli.calamityevents.CalamityEventsCore;
import com.peli.calamityevents.data.RestoreOption;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class RestoreGUIListener implements Listener {

    private final CalamityEventsCore plugin;

    public RestoreGUIListener(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RestoreGUIHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        RestoreGUIHolder holder = (RestoreGUIHolder) event.getInventory().getHolder();

        if (slot == RestoreGUI.SLOT_YES) {
            plugin.debug("RestoreGUI (" + (holder.isDeath() ? "death" : "entry") + "): " + player.getName()
                    + " clicked YES. Options: " + holder.getSelected());
            holder.setClosedByButton(true);
            player.closeInventory();
            if (holder.isDeath()) {
                plugin.getDeathSnapshots().remove(player.getUniqueId());
            } else {
                plugin.getPendingRestorePlayers().remove(player.getUniqueId());
            }
            plugin.getPlayerDataManager().applyPlayerData(player, holder.getSavedData(), holder.getSelected());
            player.sendMessage("§aRestored your data.");
            return;
        }

        if (slot == RestoreGUI.SLOT_NO) {
            plugin.debug("RestoreGUI (" + (holder.isDeath() ? "death" : "entry") + "): " + player.getName()
                    + " clicked NO — not restoring.");
            holder.setClosedByButton(true);
            player.closeInventory();
            if (holder.isDeath()) {
                plugin.getDeathSnapshots().remove(player.getUniqueId());
                player.sendMessage("§7Skipped restore — you keep your respawn state.");
            } else {
                plugin.getPendingRestorePlayers().remove(player.getUniqueId());
                player.sendMessage("§7Skipped restore — starting fresh.");
            }
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
        Player player = (Player) event.getPlayer();

        if (!holder.isClosedByButton()) {
            plugin.debug("RestoreGUI (" + (holder.isDeath() ? "death" : "entry") + "): "
                    + player.getName() + " closed GUI without choosing — treating as No.");
            if (holder.isDeath()) {
                plugin.getDeathSnapshots().remove(player.getUniqueId());
                player.sendMessage("§7Skipped restore — you keep your respawn state.");
            } else {
                plugin.getPendingRestorePlayers().remove(player.getUniqueId());
                player.sendMessage("§7Skipped restore — starting fresh.");
            }
        }
    }
}
