package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.AbastecimentoMassaMenu;
import br.com.skyy.maquinas.models.CombustivelConfig;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CombustivelListener implements Listener {

    private final SMaquinas plugin;

    public CombustivelListener(SMaquinas plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack itemMao = event.getItem();
        if (itemMao == null || itemMao.getType().name().equals("AIR")) return;

        // Verificar se é combustível
        String tipoCombustivel = plugin.getCombustivelManager().getTipoCombustivel(itemMao);
        if (tipoCombustivel == null) return;

        // Verificar se o bloco clicado é uma máquina
        MaquinaColocada maquina = plugin.getMaquinaManager().getMaquinaByLocation(event.getClickedBlock().getLocation());
        if (maquina == null) return;

        event.setCancelled(true);

        // ── SHIFT + clique direito → Abastecimento em Massa ───────────────
        if (player.isSneaking()) {
            // Apenas dono pode abrir o abastecimento em massa
            if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                player.sendMessage(plugin.getConfigManager().msg("machine-just-owner"));
                return;
            }
            new AbastecimentoMassaMenu(plugin, player.getUniqueId(), tipoCombustivel, itemMao).open(player);
            return;
        }

        MaquinaConfig config = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
        if (config == null) return;

        CombustivelConfig combustivel = plugin.getCombustivelManager().getConfig(tipoCombustivel);
        if (combustivel == null) return;

        // Verificar acesso
        if (!maquina.temAcesso(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
            player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
        }

        // Somente dono do combustível
        if (combustivel.isSomenteDono() && !maquina.getDono().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().msg("machine-fuel-only-owner")); return;
        }

        // Verificar compatibilidade
        if (!config.getCombustiveisAceitos().contains(tipoCombustivel)) {
            player.sendMessage(plugin.getConfigManager().msg("machine-fuel-compatibility",
                    "{combustivel}", ConfigManager.colorir(combustivel.getNome()))); return;
        }

        // Verificar se já tem combustível infinito
        if (maquina.getCombustivelInfinito()) {
            player.sendMessage(plugin.getConfigManager().msg("machine-infinity-fuel")); return;
        }

        double capacidade = plugin.getMaquinaManager().getCapacidadeTotal(config, maquina);

        // Combustível que deixa a máquina infinita
        if (combustivel.isInfinito()) {
            maquina.setCombustivelInfinito(true);
            if (combustivel.isConsumir()) {
                itemMao.setAmount(itemMao.getAmount() - 1);
            }
            plugin.getDB().saveMaquina(maquina);
            plugin.getHologramManager().updateHologram(maquina);
            player.sendMessage(plugin.getConfigManager().msg("machine-filled",
                    "{combustivel}", ConfigManager.colorir(combustivel.getNome())));
            return;
        }

        // Combustível que preenche completamente sem ficar infinito
        if (combustivel.isCombustivelInfinito()) {
            maquina.setCombustivel(capacidade);
            if (combustivel.isConsumir()) {
                itemMao.setAmount(itemMao.getAmount() - 1);
            }
            plugin.getDB().saveMaquina(maquina);
            plugin.getHologramManager().updateHologram(maquina);
            player.sendMessage(plugin.getConfigManager().msg("machine-filled",
                    "{combustivel}", ConfigManager.colorir(combustivel.getNome())));
            return;
        }

        // Verificar se já está cheio
        if (maquina.getCombustivel() >= capacidade) {
            player.sendMessage(plugin.getConfigManager().msg("machine-full")); return;
        }

        // Adicionar litros
        double litros = combustivel.getLitros();
        double novoTotal = Math.min(capacidade, maquina.getCombustivel() + litros);
        maquina.setCombustivel(novoTotal);

        if (combustivel.isConsumir()) {
            itemMao.setAmount(itemMao.getAmount() - 1);
        }

        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().updateHologram(maquina);
        player.sendMessage(plugin.getConfigManager().msg("machine-filled",
                "{combustivel}", ConfigManager.colorir(combustivel.getNome())));
    }
}
