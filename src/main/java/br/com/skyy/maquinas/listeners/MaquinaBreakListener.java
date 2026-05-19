package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
public class MaquinaBreakListener implements Listener {

    private final SMaquinas plugin;

    public MaquinaBreakListener(SMaquinas plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();

        MaquinaColocada maquina = plugin.getMaquinaManager().getMaquinaByLocation(event.getBlock().getLocation());
        if (maquina == null) return;

        // Cancelar e tratar manualmente
        event.setCancelled(true);
        event.setDropItems(false);

        MaquinaConfig config = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());

        // Gamemode
        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage(plugin.getConfigManager().msg("machine-gamemode")); return;
        }

        // Verificar dono ou amigo
        if (!maquina.temAcesso(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
            player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
        }

        // Silk touch obrigatório
        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.Silk", true)) {
            ItemStack tool = player.getInventory().getItemInHand();
            if (tool == null || !hasSilkTouch(tool)) {
                player.sendMessage(plugin.getConfigManager().msg("machine-silk-touch")); return;
            }
        }

        // Remover máquina
        int stack = maquina.getStack();
        String tipo = maquina.getTipoMaquina();
        boolean eraCombustivelInfinito = maquina.getCombustivelInfinito();

        plugin.getMaquinaManager().removerMaquina(maquina);
        event.getBlock().setType(Material.AIR);

        // Dropar item da máquina
        ItemStack itemMaquina = plugin.getMaquinaManager().criarItemMaquinaComStack(tipo, stack);

        // QuebrarInfinito: se a máquina tinha combustível infinito, criar item especial
        if (eraCombustivelInfinito
                && plugin.getConfigManager().getConfig().getBoolean("Opcoes.QuebrarInfinito", true)) {
            itemMaquina = plugin.getMaquinaManager().criarItemMaquinaInfinito(tipo, stack);
        }

        if (itemMaquina != null) {
            player.getInventory().addItem(itemMaquina);
        }

        String nomeMaquina = config != null ? ConfigManager.colorir(config.getNome()) : tipo;
        player.sendMessage(plugin.getConfigManager().msg("machine-removed",
                "{quantia}", String.valueOf(stack), "{maquina}", nomeMaquina));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private boolean hasSilkTouch(ItemStack tool) {
        try {
            // 1.21+: Enchantment.SILK_TOUCH was renamed; try registry lookup first
            Enchantment silk;
            try {
                silk = (Enchantment) Enchantment.class.getField("SILK_TOUCH").get(null);
            } catch (NoSuchFieldException e) {
                // Fallback: search by name
                silk = Enchantment.getByName("SILK_TOUCH");
            }
            return silk != null && tool.containsEnchantment(silk);
        } catch (Exception e) {
            return tool.getItemMeta() != null
                    && tool.getItemMeta().hasEnchant(Enchantment.getByName("SILK_TOUCH"));
        }
    }
}
