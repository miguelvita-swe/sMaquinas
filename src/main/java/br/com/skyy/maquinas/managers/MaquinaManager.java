package br.com.skyy.maquinas.managers;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.models.MaquinaConfig.DropConfig;
import br.com.skyy.maquinas.models.MaquinaConfig.DropCurrency;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.*;

public class MaquinaManager {

    private final SMaquinas plugin;
    private final Map<String, MaquinaConfig> maquinaConfigs = new LinkedHashMap<>();
    private final Map<String, MaquinaColocada> maquinasColocadas = new HashMap<>();
    private final Map<String, Long> lastTick = new HashMap<>();
    private BukkitTask task;

    public MaquinaManager(SMaquinas plugin) {
        this.plugin = plugin;
        loadConfigs();
        loadMaquinas();
    }

    public void loadConfigs() {
        maquinaConfigs.clear();
        ConfigurationSection section = plugin.getConfigManager().getMaquinas().getConfigurationSection("Maquinas");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection cs = section.getConfigurationSection(key);
                if (cs != null) maquinaConfigs.put(key, new MaquinaConfig(key, cs));
            }
        }
        plugin.getLogger().info("Carregadas " + maquinaConfigs.size() + " máquinas.");
    }

    public void loadMaquinas() {
        maquinasColocadas.clear();
        for (MaquinaColocada m : plugin.getDB().loadAllMaquinas()) {
            if (m.getLocation() != null) maquinasColocadas.put(m.getId(), m);
        }
        plugin.getLogger().info("Carregadas " + maquinasColocadas.size() + " máquinas no mundo.");
    }

    public void saveAllMaquinas() {
        for (MaquinaColocada m : maquinasColocadas.values()) plugin.getDB().saveMaquina(m);
    }

    public void reload() {
        if (task != null) task.cancel();
        saveAllMaquinas();
        loadConfigs();
        loadMaquinas();
        startMaquinasTask();
    }

    public void startMaquinasTask() {
        long taskTempo = plugin.getConfigManager().getConfig().getInt("Opcoes.TaskTempo", 5);
        if (taskTempo < 1) taskTempo = 1;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (MaquinaColocada maquina : new ArrayList<>(maquinasColocadas.values())) {
                if (maquina.isQuebrada()) continue;
                if (!maquina.isAtivo()) continue;
                if (maquina.getLocation() == null || maquina.getLocation().getWorld() == null) continue;

                // Não processar máquinas em chunks não carregados
                Location tickLoc = maquina.getLocation();
                if (!tickLoc.getWorld().isChunkLoaded(tickLoc.getBlockX() >> 4, tickLoc.getBlockZ() >> 4)) continue;

                MaquinaConfig config = maquinaConfigs.get(maquina.getTipoMaquina());
                if (config == null) continue;

                // GastarApenasOff: só gastar combustível se o dono estiver offline
                if (config.isGastarApenasOff()) {
                    Player dono = Bukkit.getPlayer(maquina.getDono());
                    if (dono != null && dono.isOnline()) continue;
                }

                // InfinitoDonoOn: máquina com combustível infinito só roda com dono online
                if (maquina.getCombustivelInfinito()
                        && plugin.getConfigManager().getConfig()
                                .getBoolean("Opcoes.InfinitoDonoOn", false)) {
                    Player dono = Bukkit.getPlayer(maquina.getDono());
                    if (dono == null || !dono.isOnline()) continue;
                }

                // Velocidade com upgrade de velocidade
                long velTicks = calcularVelocidadeTicks(config, maquina);
                long velMs = velTicks * 50L;
                long lastTime = lastTick.getOrDefault(maquina.getId(), 0L);
                if (now - lastTime < velMs) continue;
                lastTick.put(maquina.getId(), now);

                // Combustível infinito?
                boolean infinito = maquina.getCombustivelInfinito();
                if (!infinito) {
                    double gasto = config.getCombustivelOnda() * (double) maquina.getStack();
                    if (maquina.getCombustivel() < gasto) continue;
                    maquina.setCombustivel(maquina.getCombustivel() - gasto);
                }

                // Gerar drops
                gerarDrops(maquina, config);

                // Chance de quebrar
                if (config.getQuebrarChance() > 0) {
                    double chanceFinal = config.getQuebrarChance() - calcularReducaoQuebra(config, maquina);
                    if (chanceFinal > 0 && Math.random() * 100 < chanceFinal) {
                        maquina.setQuebrada(true);
                        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.ZerarQuebrar", false)) {
                            maquina.setCombustivel(0);
                        }
                        // FecharQuebrada: fecha inventário de jogadores com esta máquina aberta
                        if (plugin.getConfigManager().getConfig().getBoolean("Opcoes.FecharQuebrada", true)) {
                            final String maqId = maquina.getId();
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                for (Player online : Bukkit.getOnlinePlayers()) {
                                    org.bukkit.inventory.InventoryHolder holder =
                                            online.getOpenInventory().getTopInventory().getHolder();
                                    if (holder instanceof br.com.skyy.maquinas.menus.MaquinaInfoMenu) {
                                        br.com.skyy.maquinas.menus.MaquinaInfoMenu menu =
                                                (br.com.skyy.maquinas.menus.MaquinaInfoMenu) holder;
                                        if (maqId.equals(menu.getMaquinaId())) {
                                            online.closeInventory();
                                        }
                                    }
                                }
                            });
                        }
                        final MaquinaColocada quebradaFinal = maquina;
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDB().saveMaquina(quebradaFinal));
                        plugin.getHologramManager().updateHologram(maquina);
                        continue;
                    }
                }

                // Atualizar holograma
                plugin.getHologramManager().updateHologram(maquina);
                final MaquinaColocada maquinaFinal = maquina;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDB().saveMaquina(maquinaFinal));
            }
        }, taskTempo, taskTempo);
    }

    private long calcularVelocidadeTicks(MaquinaConfig config, MaquinaColocada maquina) {
        int nivelVel = maquina.getUpgradeVelocidade();
        double remover = config.getUpgradeVelocidade().getValorPorLevel() * nivelVel;
        double velFinal = Math.max(1, config.getVelocidade() - remover);
        return (long)(velFinal * 20L);
    }

    private double calcularReducaoQuebra(MaquinaConfig config, MaquinaColocada maquina) {
        int nivelDur = maquina.getUpgradeDurabilidade();
        return config.getUpgradeDurabilidade().getValorPorLevel() * nivelDur;
    }

    private void gerarDrops(MaquinaColocada maquina, MaquinaConfig config) {
        DropConfig drop = config.getDropConfig();

        // Drops por onda = Padrao + nivel * AdicionarPorLevel  (× stack)
        int nivelDrops = maquina.getUpgradeDrops();
        double dropsPorOnda = config.getUpgradeDrops().calcularValorEfetivo(nivelDrops, maquina.getStack());
        if (dropsPorOnda <= 0) dropsPorOnda = 1;

        // Booster de drops
        double boosterMult = plugin.getBoosterManager().getMultiplicadorDrop(maquina.getId());
        // Bônus de venda (bonus.yml) - para venda apenas, não afeta drops gerados
        dropsPorOnda *= boosterMult;

        if (drop.isArmazem()) {
            maquina.setDrops(maquina.getDrops() + dropsPorOnda);
        } else if (drop.isComandoAtivar()) {
            // Executar comandos
            executarComandos(maquina, drop, dropsPorOnda);
        } else {
            // Drop físico no chão
            Location loc = maquina.getLocation().clone().add(0.5, 1, 0.5);
            for (int i = 0; i < (int) dropsPorOnda; i++) {
                ItemStack item = ItemBuilder.getMaterialByName(drop.getIconeId(), drop.getIconeData());
                item.setAmount(1);
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
    }

    private void executarComandos(MaquinaColocada maquina, DropConfig drop, double quantia) {
        String playerName = Bukkit.getOfflinePlayer(maquina.getDono()).getName();
        if (playerName == null) return;
        double totalValor = drop.isMultiplicarQuantiaPreco() ? quantia * drop.getPreco() : quantia;
        String quantiaStr = drop.isFormatarQuantia() ? NumberFormatter.formatStatic(totalValor) : String.format("%.0f", totalValor);

        for (String cmd : drop.getComandos()) {
            String finalCmd = cmd.replace("{player}", playerName).replace("{quantia}", quantiaStr);
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
        }
    }

    // ── Métodos públicos ────────────────────────────────────────────────────

    public MaquinaColocada getMaquinaByLocation(Location loc) {
        if (loc == null) return null;
        for (MaquinaColocada m : maquinasColocadas.values()) {
            Location ml = m.getLocation();
            if (ml != null && ml.getWorld() != null && loc.getWorld() != null
                    && ml.getWorld().getName().equals(loc.getWorld().getName())
                    && ml.getBlockX() == loc.getBlockX()
                    && ml.getBlockY() == loc.getBlockY()
                    && ml.getBlockZ() == loc.getBlockZ()) {
                return m;
            }
        }
        return null;
    }

    public void colocarMaquina(Player player, Location loc, String tipo, int stack) {
        colocarMaquinaComItem(player, loc, tipo, stack, null);
    }

    public void colocarMaquinaComItem(Player player, Location loc, String tipo, int stack, ItemStack itemOriginal) {
        MaquinaConfig config = maquinaConfigs.get(tipo);
        if (config == null) return;

        String id = UUID.randomUUID().toString();
        MaquinaColocada maquina = new MaquinaColocada(id, tipo, player.getUniqueId(), loc);
        maquina.setStack(stack);
        maquina.setCapacidadeExtra(0);

        // Respeitar "Ativar colocar" da config
        boolean ativarAoColocar = plugin.getConfigManager().getConfig()
                .getBoolean("Opcoes.Ativar colocar", false);
        maquina.setAtivo(ativarAoColocar);

        // QuebrarInfinito: restaurar combustível infinito se o item tinha a flag
        if (itemOriginal != null) {
            Integer infinito = br.com.skyy.maquinas.utils.ItemBuilder.getNBTInt(itemOriginal, "smaquinas_infinito");
            if (infinito != null && infinito == 1) {
                maquina.setCombustivelInfinito(true);
            }
        }

        maquinasColocadas.put(id, maquina);
        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().createHologram(maquina);
    }

    public void removerMaquina(MaquinaColocada maquina) {
        maquinasColocadas.remove(maquina.getId());
        plugin.getDB().deleteMaquina(maquina.getId());
        plugin.getHologramManager().removeHologram(maquina.getId());
    }

    /** Calcular capacidade total de combustível com upgrades */
    public double getCapacidadeTotal(MaquinaConfig config, MaquinaColocada maquina) {
        int nivelCap = maquina.getUpgradeCombutivel();
        double val = config.getUpgradeCapacidade().calcularValorEfetivo(nivelCap, maquina.getStack());
        return val;
    }

    public ItemStack criarItemMaquina(String tipo) {
        return criarItemMaquinaComStack(tipo, 1);
    }

    /** Cria item de máquina com flag de combustível infinito (para QuebrarInfinito) */
    public ItemStack criarItemMaquinaInfinito(String tipo, int stack) {
        ItemStack item = criarItemMaquinaComStack(tipo, stack);
        if (item == null) return null;
        br.com.skyy.core.SCore.getNBT().setInt(item, "smaquinas_infinito", 1);
        return item;
    }

    public ItemStack criarItemMaquinaComStack(String tipo, int stack) {
        MaquinaConfig config = maquinaConfigs.get(tipo);
        if (config == null) return null;

        // Usa o stack real para que {stack} na lore mostre o valor correto
        List<String> lore = buildLoreMaquina(config, null, stack);

        org.bukkit.Material mat = config.isCustomSkull()
                ? br.com.skyy.maquinas.utils.MaterialCompat.playerHead()
                : br.com.skyy.maquinas.utils.MaterialCompat.of(config.getItemId(), "IRON_BLOCK");

        ItemBuilder builder = new ItemBuilder(mat, config.getItemData())
                .nome(config.getItemName())
                .lore(lore)
                .quantia(1)             // amount sempre 1; stack lógico no NBT
                .nbt("smaquinas_tipo", tipo)
                .nbt("smaquinas_stack", stack);

        if (config.isCustomSkull() && config.getSkullUrl() != null && !config.getSkullUrl().isEmpty()) {
            builder.skullTexture(config.getSkullUrl());
        }
        return builder.build();
    }

    // sobrecarga que usa maquina.getStack() — mantém compatibilidade
    private List<String> buildLoreMaquina(MaquinaConfig config, MaquinaColocada maquina) {
        int stack = maquina != null ? maquina.getStack() : 1;
        return buildLoreMaquina(config, maquina, stack);
    }

    private List<String> buildLoreMaquina(MaquinaConfig config, MaquinaColocada maquina, int stack) {
        List<String> result = new ArrayList<>();
        int nivelCap  = maquina != null ? maquina.getUpgradeCombutivel()  : 0;
        int nivelDrop = maquina != null ? maquina.getUpgradeDrops()        : 0;
        int nivelVel  = maquina != null ? maquina.getUpgradeVelocidade()   : 0;

        double capTotal   = config.getUpgradeCapacidade().calcularValorEfetivo(nivelCap,  stack);
        double dropsTotal = config.getUpgradeDrops()     .calcularValorEfetivo(nivelDrop, stack);
        double velTotal   = Math.max(1, config.getVelocidade()
                - config.getUpgradeVelocidade().getValorPorLevel() * nivelVel);

        for (String line : config.getItemLore()) {
            line = line
                .replace("{stack}",             String.valueOf(stack))
                .replace("{combustivel_tem}",   maquina != null ? String.format("%.0f", maquina.getCombustivel()) : "0")
                .replace("{drops_armazenados}", maquina != null ? String.format("%.0f", maquina.getDrops())       : "0")
                .replace("{capacidade}",        String.format("%.0f", capTotal))
                .replace("{drops}",             String.format("%.1f", dropsTotal))
                .replace("{velocidade}",        String.format("%.0f", velTotal));
            result.add(line);
        }
        return result;
    }

    public String getTipoMaquina(ItemStack item) {
        return ItemBuilder.getNBTString(item, "smaquinas_tipo");
    }

    public Integer getStackItem(ItemStack item) {
        return ItemBuilder.getNBTInt(item, "smaquinas_stack");
    }

    /**
     * Conta quantas máquinas físicas (blocos) o jogador tem colocadas.
     * Cada MaquinaColocada = 1 bloco, independente do stack lógico.
     * Usado para verificar o limite total de blocos colocados (Player limite max).
     */
    public int getMaquinasColocadasPorJogador(UUID player) {
        int count = 0;
        for (MaquinaColocada m : maquinasColocadas.values()) {
            if (m.getDono().equals(player)) count++;
        }
        return count;
    }

    public int getMaquinasColocadasPorJogadorETipo(UUID player, String tipo) {
        int count = 0;
        for (MaquinaColocada m : maquinasColocadas.values()) {
            if (m.getDono().equals(player) && m.getTipoMaquina().equals(tipo)) count++;
        }
        return count;
    }

    /** Verifica se há máquina de outro tipo num raio horizontal (XZ) definido na config */
    public boolean temMaquinaProxima(Location loc, String tipo) {
        int raio = plugin.getConfigManager().getConfig().getInt("Opcoes.Raio", 5);
        for (MaquinaColocada m : maquinasColocadas.values()) {
            if (m.getTipoMaquina().equals(tipo)) continue;
            Location ml = m.getLocation();
            if (ml == null || !ml.getWorld().getName().equals(loc.getWorld().getName())) continue;
            double dx = ml.getX() - loc.getX();
            double dz = ml.getZ() - loc.getZ();
            if (Math.sqrt(dx * dx + dz * dz) <= raio) return true;
        }
        return false;
    }

    public Map<String, MaquinaConfig> getMaquinaConfigs() { return maquinaConfigs; }
    public Map<String, MaquinaColocada> getMaquinasColocadas() { return maquinasColocadas; }
    public MaquinaConfig getConfig(String tipo) { return maquinaConfigs.get(tipo); }

    /** Top colocadas: soma de stacks por dono */
    public List<Map.Entry<UUID, Double>> getTopColocadas(int limit) {
        Map<UUID, Double> mapa = new HashMap<>();
        for (MaquinaColocada m : maquinasColocadas.values()) {
            mapa.merge(m.getDono(), (double) m.getStack(), Double::sum);
        }
        List<Map.Entry<UUID, Double>> lista = new ArrayList<>(mapa.entrySet());
        lista.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return lista.subList(0, Math.min(limit, lista.size()));
    }

    /** Top valor: soma de (drops * preco) de todas as máquinas do dono */
    public List<Map.Entry<UUID, Double>> getTopValor(int limit) {
        Map<UUID, Double> mapa = new HashMap<>();
        for (MaquinaColocada m : maquinasColocadas.values()) {
            MaquinaConfig cfg = maquinaConfigs.get(m.getTipoMaquina());
            if (cfg == null) continue;
            double valor = m.getDrops() * cfg.getDropConfig().getPreco();
            mapa.merge(m.getDono(), valor, Double::sum);
        }
        List<Map.Entry<UUID, Double>> lista = new ArrayList<>(mapa.entrySet());
        lista.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return lista.subList(0, Math.min(limit, lista.size()));
    }
}

