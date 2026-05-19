package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles booster de venda activation (player right-clicks with booster item in air/block,
 * not clicking a machine block).
 */
public class BoosterListener implements Listener {

    private final SMaquinas plugin;

    public BoosterListener(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBoosterUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Double mult = plugin.getBoosterManager().getBoosterVendaMult(item);
        Long dur = plugin.getBoosterManager().getBoosterVendaDur(item);
        if (mult == null || dur == null) return;

        // Se clicou num bloco que é máquina, deixar o MaquinaInteractListener cuidar
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (plugin.getMaquinaManager().getMaquinaByLocation(event.getClickedBlock().getLocation()) != null) return;
        }

        event.setCancelled(true);

        if (plugin.getBoosterManager().temBooster(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().msg("booster-already"));
            return;
        }

        plugin.getBoosterManager().addBoosterVenda(player.getUniqueId(), mult, dur);
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(plugin.getConfigManager().msg("booster-activated",
                "{bonus}", String.format("%.0f", mult),
                "{tempo}", NumberFormatter.formatTime(dur)));
    }
}
