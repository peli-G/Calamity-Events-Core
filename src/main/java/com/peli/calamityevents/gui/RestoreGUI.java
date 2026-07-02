package com.peli.calamityevents.gui;

import com.peli.calamityevents.data.RestoreOption;
import com.peli.calamityevents.data.SavedPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

/**
 * Builds and opens the "Load latest inventory?" chest GUI.
 *
 * Layout (4 rows, 36 slots):
 *
 *  Row 0  (0-8):   [border]
 *  Row 1  (9-17):  [b] ARMOR INV LOC XP MAXHP HP SAT [b]
 *  Row 2 (18-26):  [b] HUN  EXH SPN POT GM   ECH ARR [b]
 *  Row 3 (27-35):  [border except slot 29=YES and slot 33=NO]
 */
public class RestoreGUI {

    public static final int SLOT_YES = 29;
    public static final int SLOT_NO  = 33;

    private static final String TITLE_ENTRY = "§6Load latest inventory? §e⚠";
    private static final String TITLE_DEATH = "§cRestore from death? §4☠";

    public static void open(Player player, SavedPlayerData savedData) {
        openInternal(player, savedData, false);
    }

    public static void openForDeath(Player player, SavedPlayerData savedData) {
        openInternal(player, savedData, true);
    }

    private static void openInternal(Player player, SavedPlayerData savedData, boolean death) {
        RestoreGUIHolder holder = new RestoreGUIHolder(savedData, death);
        String title = death ? TITLE_DEATH : TITLE_ENTRY;
        Inventory inv = Bukkit.createInventory(holder, 36, title);
        holder.setInventory(inv);

        // Border — gray glass panes
        ItemStack border = glass(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)  inv.setItem(i, border);          // row 0
        inv.setItem(9, border);   inv.setItem(17, border);              // row 1 sides
        inv.setItem(18, border);  inv.setItem(26, border);              // row 2 sides
        for (int i = 27; i < 36; i++) inv.setItem(i, border);          // row 3

        // Toggle buttons
        for (RestoreOption opt : RestoreOption.values()) {
            inv.setItem(opt.getSlot(), toggleItem(opt, holder.isSelected(opt)));
        }

        // Yes / No
        inv.setItem(SLOT_YES, actionItem(Material.LIME_WOOL, "§a§lYes", "§7Restore selected data."));
        inv.setItem(SLOT_NO,  actionItem(Material.RED_WOOL,  "§c§lNo",  death ? "§7Keep respawn state. Items are gone." : "§7Do not restore. Start fresh."));

        player.openInventory(inv);
    }

    /** Rebuilds a single toggle item after a click. */
    public static void refresh(Inventory inv, RestoreOption opt, boolean selected) {
        inv.setItem(opt.getSlot(), toggleItem(opt, selected));
    }

    // -------------------------------------------------------------------------

    private static ItemStack toggleItem(RestoreOption opt, boolean selected) {
        Material mat = selected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        String prefix = selected ? "§a✔ " : "§7✘ ";
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(prefix + opt.getDisplayName());
        meta.setLore(Collections.singletonList(selected ? "§8Click to deselect" : "§8Click to select"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack actionItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
