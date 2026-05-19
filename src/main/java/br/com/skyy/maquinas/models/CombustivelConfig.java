package br.com.skyy.maquinas.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class CombustivelConfig {

    private final String id;
    private final String nome;
    private final boolean infinito;
    private final boolean consumir;
    private final boolean combustivelInfinito;
    private final boolean somenteDono;
    private final double litros;
    private final boolean customSkull;
    private final String skullUrl;
    private final String itemId;
    private final int itemData;
    private final String itemName;
    private final List<String> itemLore;

    public CombustivelConfig(String id, ConfigurationSection s) {
        this.id = id;
        this.nome = s.getString("Nome", "&fCombustível");
        this.infinito = s.getBoolean("Infinito", false);
        this.consumir = s.getBoolean("Consumir", true);
        this.combustivelInfinito = s.getBoolean("CombustivelInfinito", false);
        this.somenteDono = s.getBoolean("SomenteDono", false);
        this.litros = s.getDouble("Litros", 50);

        ConfigurationSection item = s.getConfigurationSection("Item");
        if (item != null) {
            customSkull = item.getBoolean("CustomSkull", false);
            skullUrl = item.getString("URL", "");
            itemId = item.getString("ID", "COAL");
            itemData = item.getInt("Data", 0);
            itemName = item.getString("Name", nome);
            itemLore = item.getStringList("Lore");
        } else {
            customSkull = false; skullUrl = ""; itemId = "COAL"; itemData = 0;
            itemName = nome; itemLore = new ArrayList<>();
        }
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public boolean isInfinito() { return infinito; }
    public boolean isConsumir() { return consumir; }
    public boolean isCombustivelInfinito() { return combustivelInfinito; }
    public boolean isSomenteDono() { return somenteDono; }
    public double getLitros() { return litros; }
    public boolean isCustomSkull() { return customSkull; }
    public String getSkullUrl() { return skullUrl; }
    public String getItemId() { return itemId; }
    public int getItemData() { return itemData; }
    public String getItemName() { return itemName; }
    public List<String> getItemLore() { return itemLore; }
}

