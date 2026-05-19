package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.MaquinaInfoMenu;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MaquinaInteractListener implements Listener {

    private final SMaquinas plugin;

    public MaquinaInteractListener(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        MaquinaColocada maquina = plugin.getMaquinaManager().getMaquinaByLocation(event.getClickedBlock().getLocation());
        if (maquina == null) return;

        // If holding a combustível, CombustivelListener handles it – don't open menu
        ItemStack itemMao = event.getItem() != null ? event.getItem() : new ItemStack(Material.AIR);
        if (itemMao.getType() != Material.AIR
                && plugin.getCombustivelManager().getTipoCombustivel(itemMao) != null) {
            return;
        }


        event.setCancelled(true);

        if (itemMao.getType() != Material.AIR) {
            Double mult = plugin.getBoosterManager().getBoosterDropMult(itemMao);
            Long dur = plugin.getBoosterManager().getBoosterDropDur(itemMao);
            if (mult != null && dur != null) {
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner"));
                    return;
                }
                if (plugin.getBoosterManager().temBoosterDrop(maquina.getId())) {
                    player.sendMessage(plugin.getConfigManager().msg("booster-drop-already"));
                    return;
                }
                plugin.getBoosterManager().addBoosterDrop(maquina.getId(), mult, dur);
                itemMao.setAmount(itemMao.getAmount() - 1);
                player.sendMessage(plugin.getConfigManager().msg("booster-drop-activated",
                        "{bonus}", String.format("%.0f", mult),
                        "{tempo}", NumberFormatter.formatTime(dur)));
                return;
            }
        }

        // Verificar acesso para abrir menu
        if (!maquina.temAcesso(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
            player.sendMessage(plugin.getConfigManager().msg("machine-just-owner"));
            return;
        }

        // Abrir menu de informações da máquina
        new MaquinaInfoMenu(plugin, maquina).open(player);
    }
}

