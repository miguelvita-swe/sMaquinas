package br.com.skyy.maquinas.managers;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.CombustivelConfig;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CombustivelManager {

    private final SMaquinas plugin;
    private final Map<String, CombustivelConfig> combustivelConfigs = new LinkedHashMap<>();

    public CombustivelManager(SMaquinas plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        combustivelConfigs.clear();
        ConfigurationSection section = plugin.getConfigManager().getCombustiveis().getConfigurationSection("Combustiveis");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection cs = section.getConfigurationSection(key);
                if (cs != null) {
                    combustivelConfigs.put(key, new CombustivelConfig(key, cs));
                }
            }
        }
        plugin.getLogger().info("Carregados " + combustivelConfigs.size() + " combustíveis.");
    }

    public void reload() {
        loadConfigs();
    }

    public ItemStack criarItemCombustivel(String tipoCombustivel) {
        CombustivelConfig config = combustivelConfigs.get(tipoCombustivel);
        if (config == null) return null;

        Material mat = config.isCustomSkull()
                ? MaterialCompat.playerHead()
                : MaterialCompat.of(config.getItemId(), "COAL");

        List<String> lore = new ArrayList<>();
        for (String line : config.getItemLore()) {
            lore.add(line.replace("{litros}", String.format("%.0f", config.getLitros())));
        }
        String nome = config.getItemName().replace("{litros}", String.format("%.0f", config.getLitros()));

        ItemBuilder builder = new ItemBuilder(mat, config.getItemData())
                .nome(nome)
                .lore(lore)
                .nbt("smaquinas_combustivel", tipoCombustivel);

        if (config.isCustomSkull() && config.getSkullUrl() != null && !config.getSkullUrl().isEmpty()) {
            builder.skullTexture(config.getSkullUrl());
        }
        return builder.build();
    }

    public ItemStack criarItemCombustivelComQuantia(String tipoCombustivel, int quantia) {
        ItemStack item = criarItemCombustivel(tipoCombustivel);
        if (item == null) return null;
        item.setAmount(quantia);
        return item;
    }

    public String getTipoCombustivel(ItemStack item) {
        return ItemBuilder.getNBTString(item, "smaquinas_combustivel");
    }

    public Map<String, CombustivelConfig> getCombustivelConfigs() { return combustivelConfigs; }
    public CombustivelConfig getConfig(String tipo) { return combustivelConfigs.get(tipo); }
}

