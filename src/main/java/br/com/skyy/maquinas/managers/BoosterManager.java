package br.com.skyy.maquinas.managers;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.BoosterData;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoosterManager {

    private final SMaquinas plugin;
    // Booster de VENDA por jogador (UUID do dono)
    private final Map<UUID, BoosterData> boostersVenda = new HashMap<>();
    // Booster de DROP por máquina (ID da máquina)
    private final Map<String, BoosterData> boostersDrop = new HashMap<>();

    public BoosterManager(SMaquinas plugin) {
        this.plugin = plugin;
    }

    public void reload() {}

    // ── Booster de venda ────────────────────────────────────────────────────

    public void addBoosterVenda(UUID player, double mult, long duracaoMs) {
        long exp = System.currentTimeMillis() + duracaoMs;
        BoosterData b = new BoosterData(player, mult, exp);
        boostersVenda.put(player, b);
        plugin.getDB().saveBooster(b);
    }

    public BoosterData getBoosterVenda(UUID player) {
        BoosterData b = boostersVenda.get(player);
        if (b == null) { b = plugin.getDB().loadBooster(player); if (b != null) boostersVenda.put(player, b); }
        return b != null && b.isAtivo() ? b : null;
    }

    public double getMultiplicadorVenda(UUID player) {
        BoosterData b = getBoosterVenda(player);
        return b != null ? b.getMultiplicador() : 1.0;
    }

    public boolean temBooster(UUID player) { return getBoosterVenda(player) != null; }

    public void removeBooster(UUID player) { boostersVenda.remove(player); }

    // ── Booster de drop (por máquina) ────────────────────────────────────────

    public void addBoosterDrop(String maquinaId, double mult, long duracaoMs) {
        long exp = System.currentTimeMillis() + duracaoMs;
        BoosterData data = new BoosterData(null, mult, exp);
        boostersDrop.put(maquinaId, data);
        // Persistir usando um UUID sintético derivado do ID da máquina
        try {
            BoosterData persistData = new BoosterData(java.util.UUID.nameUUIDFromBytes(("drop_" + maquinaId).getBytes()), mult, exp);
            plugin.getDB().saveBooster(persistData);
        } catch (Exception ignored) { /* drop boosters são best-effort */ }
    }

    public double getMultiplicadorDrop(String maquinaId) {
        BoosterData b = boostersDrop.get(maquinaId);
        if (b == null || !b.isAtivo()) { boostersDrop.remove(maquinaId); return 1.0; }
        return b.getMultiplicador();
    }

    public boolean temBoosterDrop(String maquinaId) {
        BoosterData b = boostersDrop.get(maquinaId);
        return b != null && b.isAtivo();
    }

    public long getTempoRestanteDrop(String maquinaId) {
        BoosterData b = boostersDrop.get(maquinaId);
        if (b == null || !b.isAtivo()) return 0;
        return b.getTempoRestante();
    }

    public double getBonusDrop(String maquinaId) {
        BoosterData b = boostersDrop.get(maquinaId);
        if (b == null || !b.isAtivo()) return 0;
        return (b.getMultiplicador() - 1.0) * 100.0;
    }

    // ── Itens de booster ────────────────────────────────────────────────────

    public ItemStack criarItemBoosterVenda(double mult, long duracaoMs) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&aTempo: &e" + NumberFormatter.formatTime(duracaoMs));
        lore.add("&aPorcentagem: &e" + mult + "%");
        return new br.com.skyy.maquinas.utils.ItemBuilder(br.com.skyy.maquinas.utils.MaterialCompat.of("EXP_BOTTLE","EXPERIENCE_BOTTLE"))
                .nome("&aBônus de venda")
                .lore(lore)
                .nbt("smaquinas_booster_venda_mult", (int)(mult * 100))
                .nbt("smaquinas_booster_venda_dur", (int)(duracaoMs / 1000))
                .build();
    }

    public ItemStack criarItemBoosterDrop(double mult, long duracaoMs) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&aTempo: &e" + NumberFormatter.formatTime(duracaoMs));
        lore.add("&aPorcentagem: &e" + mult + "%");
        return new br.com.skyy.maquinas.utils.ItemBuilder(br.com.skyy.maquinas.utils.MaterialCompat.of("EXP_BOTTLE","EXPERIENCE_BOTTLE"))
                .nome("&aBônus de DROP (&7clique na máquina)")
                .lore(lore)
                .nbt("smaquinas_booster_drop_mult", (int)(mult * 100))
                .nbt("smaquinas_booster_drop_dur", (int)(duracaoMs / 1000))
                .build();
    }

    public Double getBoosterVendaMult(ItemStack item) {
        Integer v = br.com.skyy.maquinas.utils.ItemBuilder.getNBTInt(item, "smaquinas_booster_venda_mult");
        return v != null ? v / 100.0 : null;
    }

    public Long getBoosterVendaDur(ItemStack item) {
        Integer v = br.com.skyy.maquinas.utils.ItemBuilder.getNBTInt(item, "smaquinas_booster_venda_dur");
        return v != null ? v * 1000L : null;
    }

    public Double getBoosterDropMult(ItemStack item) {
        Integer v = br.com.skyy.maquinas.utils.ItemBuilder.getNBTInt(item, "smaquinas_booster_drop_mult");
        return v != null ? v / 100.0 : null;
    }

    public Long getBoosterDropDur(ItemStack item) {
        Integer v = br.com.skyy.maquinas.utils.ItemBuilder.getNBTInt(item, "smaquinas_booster_drop_dur");
        return v != null ? v * 1000L : null;
    }

    /** Compat alias used by old code */
    public BoosterData getBooster(UUID player) { return getBoosterVenda(player); }
}

