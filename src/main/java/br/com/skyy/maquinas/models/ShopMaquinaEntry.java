package br.com.skyy.maquinas.models;

import br.com.skyy.maquinas.models.ShopCombustivelEntry.ShopCusto;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma entrada na loja de máquinas (shop/maquinas.yml).
 */
public class ShopMaquinaEntry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm");

    private final String id;
    private final String maquinaTipo;
    private final String rank;
    private final LocalDateTime liberaEm;

    // Item display
    private final boolean customSkull;
    private final String skullUrl;
    private final String itemId;
    private final int itemData;
    private final boolean glow;
    private final String itemName;
    private final List<String> itemLore;

    // Drop preview item
    private final boolean dropCustomSkull;
    private final String dropSkullUrl;
    private final String dropItemId;
    private final int dropItemData;
    private final String dropItemName;
    private final List<String> dropItemLore;

    // Custos
    private final List<ShopCusto> custos;

    public ShopMaquinaEntry(String id, ConfigurationSection s) {
        this.id          = id;
        this.maquinaTipo = s.getString("Maquina", "");
        this.rank        = s.getString("Rank", "");

        // Data de liberação
        String dataStr = s.getString("Libera em", "");
        LocalDateTime dt = null;
        if (!dataStr.isEmpty()) {
            try {
                dt = LocalDateTime.parse(dataStr, FMT);
            } catch (Exception ignored) {
                // Data inválida no config — considera liberado imediatamente
            }
        }
        this.liberaEm = dt;

        // Item
        ConfigurationSection item = s.getConfigurationSection("Item");
        if (item != null) {
            customSkull = item.getBoolean("CustomSkull", false);
            skullUrl    = item.getString("URL", "");
            itemId      = item.getString("ID", "IRON_BLOCK");
            itemData    = item.getInt("Data", 0);
            glow        = item.getBoolean("Glow", false);
            itemName    = item.getString("Name", "&fMáquina");
            itemLore    = item.getStringList("Lore");
        } else {
            customSkull = false; skullUrl = ""; itemId = "IRON_BLOCK";
            itemData = 0; glow = false; itemName = "&fMáquina"; itemLore = new ArrayList<>();
        }

        // Drop preview
        ConfigurationSection drop = s.getConfigurationSection("Drop");
        if (drop != null) {
            dropCustomSkull = drop.getBoolean("CustomSkull", false);
            dropSkullUrl    = drop.getString("URL", "");
            dropItemId      = drop.getString("ID", "STONE");
            dropItemData    = drop.getInt("Data", 0);
            dropItemName    = drop.getString("Name", "&fDrop");
            dropItemLore    = drop.getStringList("Lore");
        } else {
            dropCustomSkull = false; dropSkullUrl = ""; dropItemId = "STONE";
            dropItemData = 0; dropItemName = "&fDrop"; dropItemLore = new ArrayList<>();
        }

        // Custos
        custos = new ArrayList<>();
        ConfigurationSection comprar = s.getConfigurationSection("Comprar");
        if (comprar != null) {
            ConfigurationSection custosSec = comprar.getConfigurationSection("Custos");
            if (custosSec != null) {
                for (String key : custosSec.getKeys(false)) {
                    ConfigurationSection cs = custosSec.getConfigurationSection(key);
                    if (cs != null) {
                        custos.add(new ShopCusto(
                                cs.getString("Display", "Money"),
                                cs.getString("Tipo", "Money"),
                                cs.getDouble("Custo", 0)));
                    }
                }
            }
        }
    }

    /** Retorna true se o item ainda não foi liberado (data futura) */
    public boolean isLocked() {
        return liberaEm != null && LocalDateTime.now().isBefore(liberaEm);
    }

    /** Tempo restante formatado até liberação (ex: "2d 3h 10m") */
    public String getTempoRestante() {
        if (!isLocked()) return "";
        java.time.Duration d = java.time.Duration.between(LocalDateTime.now(), liberaEm);
        long dias    = d.toDays();
        long horas   = d.toHours() % 24;
        long minutos = d.toMinutes() % 60;
        if (dias > 0)   return dias + "d " + horas + "h " + minutos + "m";
        if (horas > 0)  return horas + "h " + minutos + "m";
        return minutos + "m";
    }

    /** Data formatada dd/MM/yyyy */
    public String getDataFormatada() {
        return liberaEm != null ? liberaEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    /** Hora formatada HH:mm */
    public String getHoraFormatada() {
        return liberaEm != null ? liberaEm.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getId()            { return id; }
    public String getMaquinaTipo()   { return maquinaTipo; }
    public String getRank()          { return rank; }
    public boolean isCustomSkull()   { return customSkull; }
    public String getSkullUrl()      { return skullUrl; }
    public String getItemId()        { return itemId; }
    public int getItemData()         { return itemData; }
    public boolean isGlow()          { return glow; }
    public String getItemName()      { return itemName; }
    public List<String> getItemLore(){ return itemLore; }

    public boolean isDropCustomSkull()   { return dropCustomSkull; }
    public String getDropSkullUrl()      { return dropSkullUrl; }
    public String getDropItemId()        { return dropItemId; }
    public int getDropItemData()         { return dropItemData; }
    public String getDropItemName()      { return dropItemName; }
    public List<String> getDropItemLore(){ return dropItemLore; }

    public List<ShopCusto> getCustos()   { return custos; }
}
