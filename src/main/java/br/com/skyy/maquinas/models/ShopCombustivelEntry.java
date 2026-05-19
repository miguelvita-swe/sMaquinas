package br.com.skyy.maquinas.models;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma entrada na loja de combustíveis (shop/combustiveis.yml).
 */
public class ShopCombustivelEntry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm");

    private final String id;
    private final String combustivelTipo;
    private final String permissao;
    private final LocalDateTime liberaEm;

    // Item display
    private final boolean customSkull;
    private final String skullUrl;
    private final String itemId;
    private final int itemData;
    private final boolean glow;
    private final String itemName;
    private final List<String> itemLore;

    // Custos
    private final List<ShopCusto> custos;

    public ShopCombustivelEntry(String id, ConfigurationSection s) {
        this.id              = id;
        this.combustivelTipo = s.getString("Combustivel", "");
        this.permissao       = s.getString("Permissao", "");

        // Data de liberação
        String dataStr = s.getString("Libera em", "");
        LocalDateTime dt = null;
        if (!dataStr.isEmpty()) {
            try {
                dt = LocalDateTime.parse(dataStr, FMT);
            } catch (Exception ignored) {
                // Intentionally ignored: if the configured date is invalid we keep liberaEm == null
                // so the shop entry is considered immediately available. This avoids startup errors
                // due to misconfigured dates while keeping the plugin robust.
            }
        }
        this.liberaEm = dt;

        // Item
        ConfigurationSection item = s.getConfigurationSection("Item");
        if (item != null) {
            customSkull = item.getBoolean("CustomSkull", false);
            skullUrl    = item.getString("URL", "");
            itemId      = item.getString("ID", "COAL");
            itemData    = item.getInt("Data", 0);
            glow        = item.getBoolean("Glow", false);
            itemName    = item.getString("Name", "&fCombustível");
            itemLore    = item.getStringList("Lore");
        } else {
            customSkull = false; skullUrl = ""; itemId = "COAL";
            itemData = 0; glow = false; itemName = "&fCombustível"; itemLore = new ArrayList<>();
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
    public String getId()              { return id; }
    public String getCombustivelTipo() { return combustivelTipo; }
    public String getPermissao()       { return permissao; }
    public boolean isCustomSkull()     { return customSkull; }
    public String getSkullUrl()        { return skullUrl; }
    public String getItemId()          { return itemId; }
    public int getItemData()           { return itemData; }
    public boolean isGlow()            { return glow; }
    public String getItemName()        { return itemName; }
    public List<String> getItemLore()  { return itemLore; }
    public List<ShopCusto> getCustos() { return custos; }

    // ── Inner: custo ───────────────────────────────────────────────────────
    public static class ShopCusto {
        private final String display;
        private final String tipo;
        private final double custo;

        public ShopCusto(String display, String tipo, double custo) {
            this.display = display;
            this.tipo    = tipo;
            this.custo   = custo;
        }

        public String getDisplay() { return display; }
        public String getTipo()    { return tipo; }
        public double getCusto()   { return custo; }
    }
}
