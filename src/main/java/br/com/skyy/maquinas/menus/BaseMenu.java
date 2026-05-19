package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class BaseMenu implements InventoryHolder {

    protected final SMaquinas plugin;
    protected Inventory inventory;

    public BaseMenu(SMaquinas plugin) {
        this.plugin = plugin;
    }

    public abstract void build();

    public void open(Player player) {
        build();
        player.openInventory(inventory);
    }

    /** Schedules opening a new menu on the next server tick (safe inside InventoryClickEvent) */
    protected void openLater(SMaquinas pl, Player player, java.util.function.Supplier<BaseMenu> supplier) {
        org.bukkit.Bukkit.getScheduler().runTask(pl, () -> supplier.get().open(player));
    }

    public abstract void onClick(Player player, int slot, org.bukkit.inventory.ItemStack item, ClickType click);

    public void onClose(Player player) {}

    @Override
    public Inventory getInventory() { return inventory; }

    protected Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(this, size, br.com.skyy.maquinas.utils.ConfigManager.colorir(title));
    }
}

