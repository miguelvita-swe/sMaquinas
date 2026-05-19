package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LimiteItemListener implements Listener {

    private final SMaquinas plugin;

    public LimiteItemListener(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        // ── Limite de compra ─────────────────────────────────────────────────
        Double quantia = plugin.getLimiteManager().getItemLimiteQuantia(item);
        if (quantia != null) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                compactarLimites(player);
                return;
            }
            int maxLimite = plugin.getConfigManager().getConfig().getInt("Limite.Max", 0);
            double limiteAtual = plugin.getLimiteManager().getLimite(player.getUniqueId());
            if (maxLimite > 0 && limiteAtual >= maxLimite) {
                player.sendMessage(plugin.getConfigManager().msg("limit-max"));
                return;
            }
            double total = quantia * item.getAmount();
            if (maxLimite > 0) total = Math.min(total, maxLimite - limiteAtual);
            plugin.getLimiteManager().addLimite(player.getUniqueId(), total);
            item.setAmount(0);
            player.sendMessage(plugin.getConfigManager().msg("limit-activated",
                    "{quantia}", String.format("%.0f", total)));
            return;
        }

        // ── Shift+direito: compactar combustíveis ────────────────────────────
        if (player.isSneaking()) {
            String tipoComb = plugin.getCombustivelManager().getTipoCombustivel(item);
            if (tipoComb != null) {
                // Não compactar quando clicando em cima de uma máquina (CombustivelListener trata)
                boolean clicouNaMaquina = event.getAction() == Action.RIGHT_CLICK_BLOCK
                        && event.getClickedBlock() != null
                        && plugin.getMaquinaManager().getMaquinaByLocation(
                                event.getClickedBlock().getLocation()) != null;
                if (!clicouNaMaquina
                        && plugin.getConfigManager().getConfig()
                                .getBoolean("Opcoes.CompactarCombustivel", true)) {
                    event.setCancelled(true);
                    compactarCombustivel(player, tipoComb);
                    return;
                }
            }

            // ── Shift+direito: compactar máquinas ────────────────────────────
            String tipoMaq = plugin.getMaquinaManager().getTipoMaquina(item);
            if (tipoMaq != null
                    && plugin.getConfigManager().getConfig()
                            .getBoolean("Opcoes.CompactarMaquina", false)) {
                event.setCancelled(true);
                compactarMaquina(player, tipoMaq);
            }
        }
    }

    // ── Compactar limites ─────────────────────────────────────────────────

    private void compactarLimites(Player player) {
        double total = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            Double q = plugin.getLimiteManager().getItemLimiteQuantia(it);
            if (q == null) continue;
            total += q * it.getAmount();
            player.getInventory().setItem(i, null);
        }
        if (total <= 0) return;
        ItemStack compactado = plugin.getLimiteManager().criarItemLimite(total);
        player.getInventory().addItem(compactado);
        player.sendMessage(plugin.getConfigManager().msg("limit-converted"));
    }

    // ── Compactar combustíveis do mesmo tipo ──────────────────────────────

    private void compactarCombustivel(Player player, String tipo) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            if (!tipo.equals(plugin.getCombustivelManager().getTipoCombustivel(it))) continue;
            total += it.getAmount();
            player.getInventory().setItem(i, null);
        }
        if (total <= 0) return;
        int restante = total;
        while (restante > 0) {
            int qtd = Math.min(64, restante);
            ItemStack stack = plugin.getCombustivelManager().criarItemCombustivelComQuantia(tipo, qtd);
            if (stack != null) player.getInventory().addItem(stack);
            restante -= qtd;
        }
        player.sendMessage(plugin.getConfigManager().msg("limit-converted"));
    }

    // ── Compactar máquinas do mesmo tipo ──────────────────────────────────

    private void compactarMaquina(Player player, String tipo) {
        int totalStack = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            if (!tipo.equals(plugin.getMaquinaManager().getTipoMaquina(it))) continue;
            Integer st = plugin.getMaquinaManager().getStackItem(it);
            int stackItem = (st != null && st > 0) ? st : 1;
            totalStack += stackItem * it.getAmount();
            player.getInventory().setItem(i, null);
        }
        if (totalStack <= 0) return;
        ItemStack compactado = plugin.getMaquinaManager().criarItemMaquinaComStack(tipo, totalStack);
        if (compactado != null) player.getInventory().addItem(compactado);
        player.sendMessage(plugin.getConfigManager().msg("limit-converted"));
    }
}
