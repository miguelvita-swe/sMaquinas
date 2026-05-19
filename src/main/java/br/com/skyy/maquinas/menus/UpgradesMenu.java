package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.models.MaquinaConfig.UpgradeConfig;
import br.com.skyy.maquinas.models.MaquinaConfig.UpgradePrice;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class UpgradesMenu extends BaseMenu {

    private final MaquinaColocada maquina;
    private final MaquinaConfig config;

    public UpgradesMenu(SMaquinas plugin, MaquinaColocada maquina) {
        super(plugin);
        this.maquina = maquina;
        this.config = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
    }

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuUpgrades();

        // Título do menu com placeholder {maquina}
        String titulo = yml.getString("Nome", "&8Upgrades");
        if (config != null) titulo = titulo.replace("{maquina}", ConfigManager.colorir(config.getNome()));

        int tamanho = yml.getInt("Tamanho", 36);
        inventory = createInventory(tamanho, titulo);


        if (config == null) return;

        // Renderizar todos os itens definidos na seção Itens:
        ConfigurationSection itensSection = yml.getConfigurationSection("Itens");
        if (itensSection != null) {
            for (String key : itensSection.getKeys(false)) {
                ConfigurationSection itemSec = itensSection.getConfigurationSection(key);
                if (itemSec == null) continue;
                int slot = itemSec.getInt("Slot", -1);
                if (slot < 0 || slot >= tamanho) continue;

                ItemStack item = buildItemFromSection(itemSec, key);
                if (item != null) inventory.setItem(slot, item);
            }
        }

        // Botão voltar no slot configurado em Backslot
        int backSlot = yml.getInt("Backslot", 27);
        ConfigurationSection setas = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Voltar");
        if (setas != null) {
            String backName = setas.getString("Name", "&cVoltar");
            List<String> backLore = setas.getStringList("Lore");
            Material backMat = MaterialCompat.of(setas.getString("ID", "262"), "ARROW");
            inventory.setItem(backSlot, new ItemBuilder(backMat)
                    .nome(backName).lore(backLore)
                    .nbt("smaquinas_action", "voltar").build());
        } else {
            inventory.setItem(backSlot, new ItemBuilder(Material.ARROW).nome("&cVoltar")
                    .nbt("smaquinas_action", "voltar").build());
        }
    }

    private ItemStack buildItemFromSection(ConfigurationSection sec, String key) {
        // Determinar qual tipo de upgrade este item representa
        String upgTipo = getUpgradeTipoByKey(key);
        boolean isMaximo = key.endsWith("Maximo") || key.endsWith("Maxximo");

        // Calcular valores dos upgrades
        int nivelCap = maquina.getUpgradeCombutivel();
        int nivelDrop = maquina.getUpgradeDrops();
        int nivelVel = maquina.getUpgradeVelocidade();
        int nivelDur = maquina.getUpgradeDurabilidade();

        UpgradeConfig upg = upgTipo != null ? getUpgradeConfig(upgTipo) : null;
        int nivelAtual = upgTipo != null ? getNivel(upgTipo) : 0;

        // Se for item de "normal" mas o upgrade já está no máximo, ou "maximo" mas não está no máximo → pular
        if (upgTipo != null) {
            boolean noMaximo = nivelAtual >= upg.getMaximo();
            if (isMaximo && !noMaximo) return null;
            if (!isMaximo && noMaximo) return null;
        }

        // Capacidade: calcular atual e máximo
        double capAtual = config.getUpgradeCapacidade().calcularValorEfetivo(nivelCap, maquina.getStack());
        double capMax   = config.getUpgradeCapacidade().calcularValorEfetivo(config.getUpgradeCapacidade().getMaximo(), maquina.getStack());
        double dropsAtual = config.getUpgradeDrops().calcularValorEfetivo(nivelDrop, maquina.getStack());
        double dropsMax   = config.getUpgradeDrops().calcularValorEfetivo(config.getUpgradeDrops().getMaximo(), maquina.getStack());
        double velAtual = config.getUpgradeVelocidade().getValorPorLevel() * nivelVel;
        double velMax   = config.getUpgradeVelocidade().getValorPorLevel() * config.getUpgradeVelocidade().getMaximo();
        double durAtual = config.getUpgradeDurabilidade().getValorPorLevel() * nivelDur;
        double durMax   = config.getUpgradeDurabilidade().getValorPorLevel() * config.getUpgradeDurabilidade().getMaximo();

        // Custo próximo nível
        String money = "0";
        if (upgTipo != null && upg != null && !upg.getPrices().isEmpty()) {
            money = NumberFormatter.formatStatic(upg.getPrices().get(0).getPrice());
        }

        int nivelMaximo = upg != null ? upg.getMaximo() : 0;

        // Processar lore com placeholders
        List<String> rawLore = sec.getStringList("Lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            line = line
                .replace("{nivel_atual}", String.valueOf(nivelAtual))
                .replace("{nivel_maximo}", String.valueOf(nivelMaximo))
                .replace("{capacidade_atual}", String.format("%.0f", capAtual))
                .replace("{capacidade_maximo}", String.format("%.0f", capMax))
                .replace("{drops_atual}", String.format("%.1f", dropsAtual))
                .replace("{drops_maximo}", String.format("%.1f", dropsMax))
                .replace("{velocidade_atual}", String.format("%.0f", velAtual))
                .replace("{velocidade_maximo}", String.format("%.0f", velMax))
                .replace("{durabilidade_atual}", String.format("%.1f", durAtual))
                .replace("{durabilidade_maximo}", String.format("%.1f", durMax))
                .replace("{money}", money);
            lore.add(line);
        }

        String name = sec.getString("Name", "&fUpgrade");
        Material mat = MaterialCompat.of(sec.getString("ID", "STONE"), "STONE");
        int data = sec.getInt("Data", 0);

        ItemBuilder builder = new ItemBuilder(mat, data).nome(name).lore(lore);

        // NBT para identificação do clique
        if (upgTipo != null && !isMaximo) {
            builder.nbt("smaquinas_upg_tipo", upgTipo);
        }

        return builder.build();
    }

    /** Mapeia o nome da chave YAML para o tipo de upgrade interno */
    private String getUpgradeTipoByKey(String key) {
        String lower = key.toLowerCase();
        if (lower.startsWith("capacidade")) return "capacidade";
        if (lower.startsWith("drops")) return "drops";
        if (lower.startsWith("velocidade")) return "velocidade";
        if (lower.startsWith("durabilidade")) return "durabilidade";
        return null;
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if ("voltar".equals(action)) {
            openLater(plugin, player, () -> new MaquinaInfoMenu(plugin, maquina));
            return;
        }

        String upgTipo = ItemBuilder.getNBTString(item, "smaquinas_upg_tipo");
        if (upgTipo != null) fazerUpgrade(player, upgTipo);
    }

    private void fazerUpgrade(Player player, String tipo) {
        if (config == null) return;

        // Apenas o dono pode fazer upgrades
        if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
            player.sendMessage(plugin.getConfigManager().msg("machine-just-owner"));
            return;
        }

        UpgradeConfig upg = getUpgradeConfig(tipo);
        int nivel = getNivel(tipo);

        if (nivel >= upg.getMaximo()) {
            player.sendMessage(plugin.getConfigManager().msg("machine-upgrade-max"));
            return;
        }

        // Verificar saldo para todos os preços
        for (UpgradePrice p : upg.getPrices()) {
            if (!plugin.getEconomiaAPI().temDinheiro(player, p.getPrice(), p.getProvider())) {
                player.sendMessage(plugin.getConfigManager().msg("machine-upgrade-money",
                        "{money}", NumberFormatter.formatStatic(p.getPrice())));
                return;
            }
        }

        // Cobrar todos os preços
        for (UpgradePrice p : upg.getPrices()) {
            plugin.getEconomiaAPI().cobrar(player, p.getPrice(), p.getProvider());
        }

        setNivel(tipo, nivel + 1);
        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().updateHologram(maquina);

        double custo = upg.getPrices().isEmpty() ? 0 : upg.getPrices().get(0).getPrice();
        String msgKey;
        switch (tipo) {
            case "capacidade":   msgKey = "machine-bought-capacity";    break;
            case "drops":        msgKey = "machine-bought-drops";       break;
            case "velocidade":   msgKey = "machine-bought-velocity";    break;
            default:             msgKey = "machine-bought-durability";  break;
        }
        player.sendMessage(plugin.getConfigManager().msg(msgKey, "{money}", NumberFormatter.formatStatic(custo)));

        // Reabrir menu atualizado
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UpgradeConfig getUpgradeConfig(String tipo) {
        switch (tipo) {
            case "capacidade":  return config.getUpgradeCapacidade();
            case "drops":       return config.getUpgradeDrops();
            case "velocidade":  return config.getUpgradeVelocidade();
            default:            return config.getUpgradeDurabilidade();
        }
    }

    private int getNivel(String tipo) {
        switch (tipo) {
            case "capacidade":  return maquina.getUpgradeCombutivel();
            case "drops":       return maquina.getUpgradeDrops();
            case "velocidade":  return maquina.getUpgradeVelocidade();
            default:            return maquina.getUpgradeDurabilidade();
        }
    }

    private void setNivel(String tipo, int nivel) {
        switch (tipo) {
            case "capacidade":  maquina.setUpgradeCombutivel(nivel);  break;
            case "drops":       maquina.setUpgradeDrops(nivel);       break;
            case "velocidade":  maquina.setUpgradeVelocidade(nivel);  break;
            default:            maquina.setUpgradeDurabilidade(nivel); break;
        }
    }
}
