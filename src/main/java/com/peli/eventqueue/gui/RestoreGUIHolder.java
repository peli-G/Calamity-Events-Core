package com.peli.eventqueue.gui;

import com.peli.eventqueue.data.RestoreOption;
import com.peli.eventqueue.data.SavedPlayerData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumSet;
import java.util.Set;

/**
 * Custom holder that carries the player's saved data and the current
 * toggle state while the restore GUI is open.
 */
public class RestoreGUIHolder implements InventoryHolder {

    private Inventory inventory;
    private final SavedPlayerData savedData;
    private final Set<RestoreOption> selected;
    private boolean closedByButton = false;

    public RestoreGUIHolder(SavedPlayerData savedData) {
        this.savedData = savedData;
        // Initialise from each option's default checked state
        this.selected = EnumSet.noneOf(RestoreOption.class);
        for (RestoreOption opt : RestoreOption.values()) {
            if (opt.isCheckedByDefault()) selected.add(opt);
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public SavedPlayerData getSavedData()    { return savedData; }
    public Set<RestoreOption> getSelected()  { return selected; }

    public boolean isSelected(RestoreOption opt)   { return selected.contains(opt); }
    public void toggle(RestoreOption opt) {
        if (selected.contains(opt)) selected.remove(opt); else selected.add(opt);
    }

    public boolean isClosedByButton()              { return closedByButton; }
    public void setClosedByButton(boolean v)       { this.closedByButton = v; }
}
