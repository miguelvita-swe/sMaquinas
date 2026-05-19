package br.com.skyy.maquinas.managers;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LimiteManager {

    private final SMaquinas plugin;
    private final Map<UUID, Double> limiteCache = new HashMap<>();

    public LimiteManager(SMaquinas plugin) {
        this.plugin = plugin;
        loadLimites();
    }

    public void loadLimites() {
        limiteCache.clear();
        limiteCache.putAll(plugin.getDB().loadAllLimites());
    }

    public void reload() { loadLimites(); }

    public void reloadPlayer(java.util.UUID uuid) {
        double val = plugin.getDB().getLimite(uuid);
        limiteCache.put(uuid, val);
    }

    public double getLimite(UUID player) { return limiteCache.getOrDefault(player, 0.0); }

    public void setLimite(UUID player, double limite) {
        limiteCache.put(player, limite);
        plugin.getDB().setLimite(player, limite);
    }

    public void addLimite(UUID player, double quantia) { setLimite(player, getLimite(player) + quantia); }

    public void removeLimite(UUID player, double quantia) {
        setLimite(player, Math.max(0, getLimite(player) - quantia));
    }

    public boolean temLimiteSuficiente(UUID player, double quantia) { return getLimite(player) >= quantia; }

    /** Limite total de máquinas via permissão smaquinas.limitetotal.N */
    public int getLimiteTotal(Player player) {
        if (player.hasPermission("smaquinas.place.bypass")) return Integer.MAX_VALUE;
        int max = plugin.getConfigManager().getConfig().getInt("Opcoes.Player limite max", 0);
        for (int i = (max > 0 ? max : 1000000); i >= 1; i--) {
            if (player.hasPermission("smaquinas.limitetotal." + i)) return i;
        }
        return max;
    }

    public int getMaquinasColocadas(UUID player) {
        return plugin.getMaquinaManager().getMaquinasColocadasPorJogador(player);
    }

    public boolean podeClocar(Player player) {
        if (player.hasPermission("smaquinas.place.bypass")) return true;
        int limite = getLimiteTotal(player);
        if (limite == 0) return true; // sem limite configurado
        return getMaquinasColocadas(player.getUniqueId()) < limite;
    }

    public ItemStack criarItemLimite(double quantia) {
        List<String> lore = new ArrayList<>();
        for (String l : plugin.getConfigManager().getConfig().getStringList("Limite.Item.Lore")) {
            lore.add(l.replace("{quantia}", NumberFormatter.formatStatic(quantia)));
        }
        String nome = plugin.getConfigManager().getConfig().getString("Limite.Item.Name", "&bLimite de compra");
        nome = nome.replace("{quantia}", NumberFormatter.formatStatic(quantia));
        boolean glow       = plugin.getConfigManager().getConfig().getBoolean("Limite.Item.Glow", true);
        boolean customSkull = plugin.getConfigManager().getConfig().getBoolean("Limite.Item.CustomSkull", false);
        String url          = plugin.getConfigManager().getConfig().getString("Limite.Item.URL", "");

        ItemBuilder builder = new ItemBuilder(MaterialCompat.playerHead())
                .nome(nome)
                .lore(lore)
                .glow(glow)
                .nbt("smaquinas_limite", (int) quantia);

        if (customSkull && url != null && !url.isEmpty()) {
            builder.skullTexture(url);
        }
        return builder.build();
    }

    public Double getItemLimiteQuantia(ItemStack item) {
        Integer val = ItemBuilder.getNBTInt(item, "smaquinas_limite");
        return val != null ? (double) val : null;
    }

    public List<Map.Entry<UUID, Double>> getTopLimite(int limit) {
        return plugin.getDB().getTopLimite(limit);
    }
}
