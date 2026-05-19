package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.BaseMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {

    private final SMaquinas plugin;

    public MenuListener(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTopInventory().getHolder() instanceof BaseMenu) {
            event.setCancelled(true);

            // Only handle clicks inside the menu (top inventory), not the player's own inventory
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlot() < 0 || event.getRawSlot() >= topSize) return;

            BaseMenu menu = (BaseMenu) event.getView().getTopInventory().getHolder();
            if (event.getCurrentItem() != null) {
                menu.onClick(player, event.getRawSlot(), event.getCurrentItem(), event.getClick());
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getInventory().getHolder() instanceof BaseMenu) {
            BaseMenu menu = (BaseMenu) event.getView().getTopInventory().getHolder();
            menu.onClose((Player) event.getPlayer());
        }
    }
}

