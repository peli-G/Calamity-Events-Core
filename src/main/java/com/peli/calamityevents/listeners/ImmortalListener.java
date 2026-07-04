package com.peli.calamityevents.listeners;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.PlayerInventory;

public class ImmortalListener implements Listener {

    private final CalamityEventsCore plugin;

    public ImmortalListener(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs after other plugins/vanilla mechanics have finished adjusting
     * damage (armor, potions, enchantments), so getFinalDamage() reflects
     * what would actually be taken — but before the server applies it to
     * health, so the clamp still lands in the same tick as the hit.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (!plugin.isImmortal(player)) return;

        // A totem in either hand should behave exactly like vanilla when this
        // hit would otherwise be lethal — don't interfere, let it pop normally.
        if (hasTotem(player)) return;

        double finalDamage = event.getFinalDamage();
        double resultingHealth = player.getHealth() - finalDamage;

        if (resultingHealth < CalamityEventsCore.IMMORTAL_HEALTH_FLOOR) {
            // Clamp the (base) damage so health lands at exactly the floor —
            // never lower, never zero, never dead.
            double allowedDamage = Math.max(0.0, player.getHealth() - CalamityEventsCore.IMMORTAL_HEALTH_FLOOR);
            event.setDamage(allowedDamage);
            plugin.debug("Immortal: clamped damage for " + player.getName()
                    + " (would have gone to " + resultingHealth + " health, capped at " + CalamityEventsCore.IMMORTAL_HEALTH_FLOOR + ").");
        }
    }

    private boolean hasTotem(Player player) {
        PlayerInventory inv = player.getInventory();
        return inv.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || inv.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }
}
