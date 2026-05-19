package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

public class MaquinaPlaceListener implements Listener {

    private final SMaquinas plugin;

    public MaquinaPlaceListener(SMaquinas plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        String tipo = plugin.getMaquinaManager().getTipoMaquina(item);
        if (tipo == null) return;

        MaquinaConfig config = plugin.getMaquinaManager().getConfig(tipo);
        if (config == null) { event.setCancelled(true); return; }

        // Apenas survival
        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage(plugin.getConfigManager().msg("machine-gamemode"));
            event.setCancelled(true); return;
        }

        // Permissão
        if (!config.getPermissao().isEmpty() && !player.hasPermission(config.getPermissao())) {
            player.sendMessage(plugin.getConfigManager().msg("machine-permission-place"));
            event.setCancelled(true); return;
        }

        // ── Plot: só pode colocar dentro de plot (PlotSquared) ──────────────
        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.Plot", false)) {
            if (!isInOwnPlot(player, event.getBlockPlaced().getLocation())) {
                player.sendMessage(plugin.getConfigManager().msg("machine-plot"));
                event.setCancelled(true); return;
            }
        }

        // Stack do item na mão
        Integer stackNBT = plugin.getMaquinaManager().getStackItem(item);
        int stack = (stackNBT != null && stackNBT > 0) ? stackNBT : 1;

        // ── MERGE ────────────────────────────────────────────────────────────
        MaquinaColocada existente = encontrarMaquinaAdjacenteParaMerge(
                event.getBlockPlaced().getLocation(), tipo, player);

        if (existente != null) {
            // ChecarInfinito: impede merge em máquina infinita
            if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.ChecarInfinito", true)
                    && existente.getCombustivelInfinito()) {
                player.sendMessage(plugin.getConfigManager().msg("machine-infinite-merge"));
                event.setCancelled(true);
                return;
            }
            int espacoDisponivel = config.getStackMax() - existente.getStack();
            if (espacoDisponivel <= 0) {
                player.sendMessage(plugin.getConfigManager().msg("limit-stack-full",
                        "{limite}", String.valueOf(config.getStackMax())));
                event.setCancelled(true);
                return;
            }
            int mergeando = Math.min(stack, espacoDisponivel);
            int restante  = stack - mergeando;

            event.setCancelled(true);
            existente.setStack(existente.getStack() + mergeando);
            plugin.getDB().saveMaquina(existente);
            plugin.getHologramManager().updateHologram(existente);
            player.sendMessage(plugin.getConfigManager().msg("limit-stack-activated",
                    "{limite}", String.valueOf(existente.getStack())));

            item.setAmount(item.getAmount() - 1);
            if (restante > 0) {
                ItemStack devolver = plugin.getMaquinaManager()
                        .criarItemMaquinaComStack(tipo, restante);
                if (devolver != null) player.getInventory().addItem(devolver);
            }
            return;
        }

        // ── NOVA MÁQUINA ─────────────────────────────────────────────────────

        // Verificar bloco embaixo
        Block below = event.getBlockPlaced().getRelative(0, -1, 0);
        List<String> blacklist = plugin.getConfigManager().getConfig()
                .getStringList("Opcoes.BlockCima blacklist");
        for (String entry : blacklist) {
            String matName = entry.contains(":") ? entry.split(":")[0] : entry;
            if (below.getType().name().equalsIgnoreCase(matName)) {
                player.sendMessage(plugin.getConfigManager().msg("machine-block-below"));
                event.setCancelled(true); return;
            }
        }

        // Verificar máquina próxima (outro tipo)
        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.Verificar perto", true)) {
            if (plugin.getMaquinaManager().temMaquinaProxima(
                    event.getBlockPlaced().getLocation(), tipo)) {
                int raio = plugin.getConfigManager().getConfig().getInt("Opcoes.Raio", 5);
                player.sendMessage(plugin.getConfigManager().msg("machine-nearby",
                        "{raio}", String.valueOf(raio)));
                event.setCancelled(true); return;
            }
        }

        // Plot maquina limite: apenas 1 tipo por plot
        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.Plot maquina limite", false)) {
            if (hasOtherMaquinaTypeInPlot(player, event.getBlockPlaced().getLocation(), tipo)) {
                player.sendMessage(plugin.getConfigManager().msg("machine-plot-type-limit"));
                event.setCancelled(true); return;
            }
        }

        // ApenasUm
        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.ApenasUm", false)) {
            if (plugin.getMaquinaManager()
                    .getMaquinasColocadasPorJogadorETipo(player.getUniqueId(), tipo) > 0) {
                player.sendMessage(plugin.getConfigManager().msg("machine-placed-already"));
                event.setCancelled(true); return;
            }
        }

        // Limite total de máquinas
        if (!plugin.getLimiteManager().podeClocar(player)) {
            int lim   = plugin.getLimiteManager().getLimiteTotal(player);
            int atual = plugin.getLimiteManager().getMaquinasColocadas(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().msg("machine-total-limit",
                    "{atual}", String.valueOf(atual), "{limite}", String.valueOf(lim)));
            event.setCancelled(true); return;
        }

        // Stack máximo: usa o permitido e devolve o restante
        int stackColocar = Math.min(stack, config.getStackMax());
        int stackDevolver = stack - stackColocar;

        // Colocar nova máquina com o stack permitido
        plugin.getMaquinaManager().colocarMaquinaComItem(
                player, event.getBlockPlaced().getLocation(), tipo, stackColocar, item);

        // Devolver as máquinas excedentes ao inventário
        if (stackDevolver > 0) {
            ItemStack devolver = plugin.getMaquinaManager()
                    .criarItemMaquinaComStack(tipo, stackDevolver);
            if (devolver != null) player.getInventory().addItem(devolver);
        }

        player.sendMessage(plugin.getConfigManager().msg("machine-placed",
                "{quantia}", String.valueOf(stackColocar),
                "{maquina}", br.com.skyy.maquinas.utils.ConfigManager.colorir(config.getNome())));
    }

    // ── PlotSquared via reflection (compatível com 1.8–1.21) ─────────────────

    /** Retorna true se o player estiver dentro do próprio plot (ou for dono/membro) */
    private boolean isInOwnPlot(Player player, org.bukkit.Location loc) {
        try {
            Class<?> plotApiClass = null;
            // PS2 moderno (5+): com.plotsquared.core.PlotSquared
            try { plotApiClass = Class.forName("com.plotsquared.core.PlotSquared"); } catch (ClassNotFoundException ignored) {}
            // PS legado: com.intellectualcrafters.plot.PS
            if (plotApiClass == null) {
                try { plotApiClass = Class.forName("com.intellectualcrafters.plot.PS"); } catch (ClassNotFoundException ignored) {}
            }
            if (plotApiClass == null) return true; // PlotSquared não instalado, permitir

            // Obter o plot na localização
            Object locationObj = buildPSLocation(loc);
            if (locationObj == null) return true;

            Method getPlot = plotApiClass.getMethod("getPlotAbs", locationObj.getClass());
            Object plot = getPlot.invoke(null, locationObj);
            if (plot == null) return false; // fora de plot

            // Verificar se o player é dono ou membro
            Method hasOwner = plot.getClass().getMethod("isOwner", java.util.UUID.class);
            if ((boolean) hasOwner.invoke(plot, player.getUniqueId())) return true;

            Method isMember = plot.getClass().getMethod("isAdded", java.util.UUID.class);
            return (boolean) isMember.invoke(plot, player.getUniqueId());
        } catch (Exception e) {
            return true; // em caso de erro, não bloquear
        }
    }

    /** Retorna true se já houver outro tipo de máquina no mesmo plot */
    private boolean hasOtherMaquinaTypeInPlot(Player player, org.bukkit.Location loc, String tipo) {
        try {
            Object locationObj = buildPSLocation(loc);
            if (locationObj == null) return false;

            Class<?> plotApiClass = null;
            try { plotApiClass = Class.forName("com.plotsquared.core.PlotSquared"); } catch (ClassNotFoundException ignored) {}
            if (plotApiClass == null) {
                try { plotApiClass = Class.forName("com.intellectualcrafters.plot.PS"); } catch (ClassNotFoundException ignored) {}
            }
            if (plotApiClass == null) return false;

            Method getPlot = plotApiClass.getMethod("getPlotAbs", locationObj.getClass());
            Object plot = getPlot.invoke(null, locationObj);
            if (plot == null) return false;

            // Verificar máquinas colocadas no mesmo plot
            for (MaquinaColocada m : plugin.getMaquinaManager().getMaquinasColocadas().values()) {
                if (m.getDono().equals(player.getUniqueId())) continue; // ignora próprias
                if (m.getTipoMaquina().equals(tipo)) continue;
                org.bukkit.Location ml = m.getLocation();
                if (ml == null || !ml.getWorld().equals(loc.getWorld())) continue;
                Object mLoc = buildPSLocation(ml);
                if (mLoc == null) continue;
                Object mPlot = getPlot.invoke(null, mLoc);
                if (plot.equals(mPlot)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Constrói um objeto Location do PlotSquared via reflection */
    private Object buildPSLocation(org.bukkit.Location loc) {
        try {
            // PS2 moderno
            try {
                Class<?> locClass = Class.forName("com.plotsquared.core.location.Location");
                Method at = locClass.getMethod("at", String.class, int.class, int.class, int.class);
                return at.invoke(null, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            } catch (Exception ignored) {}
            // PS legado
            Class<?> locClass = Class.forName("com.intellectualcrafters.plot.object.Location");
            return locClass.getConstructor(String.class, int.class, int.class, int.class)
                    .newInstance(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } catch (Exception e) {
            return null;
        }
    }

    // ── Merge helper ─────────────────────────────────────────────────────────

    private MaquinaColocada encontrarMaquinaAdjacenteParaMerge(
            org.bukkit.Location loc, String tipo, Player player) {

        int[][] offsets = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] off : offsets) {
            org.bukkit.Location vizinho = loc.clone().add(off[0], 0, off[1]);
            MaquinaColocada m = plugin.getMaquinaManager().getMaquinaByLocation(vizinho);
            if (m == null || !m.getTipoMaquina().equals(tipo)) continue;
            if (!m.getDono().equals(player.getUniqueId())
                    && !player.hasPermission("smaquinas.admin")) {
                player.sendMessage(plugin.getConfigManager().msg("machine-just-owner"));
                return null;
            }
            return m;
        }
        return null;
    }
}
