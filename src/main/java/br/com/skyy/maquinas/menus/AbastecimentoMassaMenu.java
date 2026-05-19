package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.CombustivelConfig;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menu de Abastecimento em Massa.
 *
 * Ativado por SHIFT + clique direito com combustível na mão sobre uma máquina.
 * Exibe todas as máquinas do jogador compatíveis com aquele combustível,
 * mostrando o status de cada uma, e permite confirmar ou cancelar.
 */
public class AbastecimentoMassaMenu extends BaseMenu {

    private final UUID ownerUUID;
    private final String tipoCombustivel;
    private final CombustivelConfig combustivelCfg;
    /** Litros totais disponíveis (litros/item × quantidade na mão) */
    private final double litrosDisponiveis;
    /** Item na mão do jogador — consumido ao confirmar */
    private final ItemStack itemMao;
    private int page;

    /** Slots de conteúdo: linhas 2, 3 e 4 sem bordas (cols 1-7) */
    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34
    };

    public AbastecimentoMassaMenu(SMaquinas plugin,
                                   UUID ownerUUID,
                                   String tipoCombustivel,
                                   ItemStack itemMao) {
        super(plugin);
        this.ownerUUID        = ownerUUID;
        this.tipoCombustivel  = tipoCombustivel;
        this.combustivelCfg   = plugin.getCombustivelManager().getConfig(tipoCombustivel);
        this.litrosDisponiveis = (combustivelCfg != null ? combustivelCfg.getLitros() : 0)
                                  * (itemMao != null ? itemMao.getAmount() : 0);
        this.itemMao           = itemMao;
        this.page              = 0;
    }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuAbastecimentoMassa();

        String nomeCombus = combustivelCfg != null
                ? ConfigManager.colorir(combustivelCfg.getNome())
                : tipoCombustivel;

        String titulo = yml.getString("Nome", "&6Abastecimento em Massa")
                .replace("{combustivel}", nomeCombus);
        int tamanho = yml.getInt("Tamanho", 54);

        inventory = createInventory(tamanho, titulo);

        // Fundo vidro cinza
        ItemStack glass = MaterialCompat.grayGlassPane().nome(" ").build();
        for (int i = 0; i < tamanho; i++) inventory.setItem(i, glass);

        // ── Máquinas compatíveis ──────────────────────────────────────────
        List<MaquinaColocada> maquinas = getMaquinasCompativeis();
        int perPage = CONTENT_SLOTS.length;
        int inicio  = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = inicio + i;
            if (idx >= maquinas.size()) break;
            inventory.setItem(CONTENT_SLOTS[i], buildMaquinaItem(yml, maquinas.get(idx)));
        }

        // ── Paginação ─────────────────────────────────────────────────────
        if (page > 0) {
            ConfigurationSection sa = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Anterior");
            Material ma = sa != null ? MaterialCompat.of(sa.getString("ID","262"), "ARROW") : Material.ARROW;
            String   na = sa != null ? sa.getString("Name","&cAnterior") : "&cAnterior";
            inventory.setItem(0, new ItemBuilder(ma).nome(na).nbt("smaquinas_action","anterior").build());
        }
        if (maquinas.size() > inicio + perPage) {
            ConfigurationSection sp = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Proximo");
            Material mp = sp != null ? MaterialCompat.of(sp.getString("ID","262"), "ARROW") : Material.ARROW;
            String   np = sp != null ? sp.getString("Name","&aProximo") : "&aProximo";
            inventory.setItem(8, new ItemBuilder(mp).nome(np).nbt("smaquinas_action","proximo").build());
        }

        // ── Item informativo de combustível ───────────────────────────────
        buildFixedItem(yml, "Combustivel", tamanho,
                "{combustivel}", nomeCombus,
                "{quantidade}",  String.format("%.0f", litrosDisponiveis));

        // ── Confirmar / Cancelar ──────────────────────────────────────────
        buildFixedItem(yml, "Confirmar", tamanho);
        buildFixedItem(yml, "Cancelar",  tamanho);

        // ── Itens decorativos extras ──────────────────────────────────────
        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens != null) {
            for (String key : itens.getKeys(false)) {
                if (key.equals("Maquina") || key.equals("Combustivel")
                        || key.equals("Confirmar") || key.equals("Cancelar")) continue;
                ConfigurationSection sec = itens.getConfigurationSection(key);
                if (sec == null) continue;
                int slot = sec.getInt("Slot", -1);
                if (slot < 0 || slot >= tamanho) continue;
                inventory.setItem(slot, buildExtraItem(sec));
            }
        }
    }

    // ── Item de cada máquina ───────────────────────────────────────────────

    private ItemStack buildMaquinaItem(FileConfiguration yml, MaquinaColocada maq) {
        MaquinaConfig config = plugin.getMaquinaManager().getConfig(maq.getTipoMaquina());
        if (config == null) return MaterialCompat.grayGlassPane().nome(" ").build();

        double cap     = plugin.getMaquinaManager().getCapacidadeTotal(config, maq);
        double atual   = maq.getCombustivel();
        double precisa = Math.max(0, cap - atual);
        double pct     = cap > 0 ? (atual / cap) * 100.0 : 0;
        String bar     = buildProgressBar(pct);
        String nomeMaq = ConfigManager.colorir(config.getNome());

        ConfigurationSection sec = yml.getConfigurationSection("Itens.Maquina");
        String name     = sec != null ? sec.getString("Name", "&6{maquina} &7[{stack}]")
                                       : "&6{maquina} &7[{stack}]";
        List<String> rawLore = sec != null ? sec.getStringList("Lore") : new ArrayList<>();

        // Prefixo visual conforme status
        String prefixo;
        if (maq.getCombustivelInfinito()) {
            prefixo = "&b♾ ";
        } else if (precisa <= 0) {
            prefixo = "&a✔ ";
        } else {
            prefixo = "&e";
        }

        name = (prefixo + name)
                .replace("{maquina}", nomeMaq)
                .replace("{stack}",   String.valueOf(maq.getStack()));

        List<String> lore = new ArrayList<>();
        if (!rawLore.isEmpty()) {
            for (String line : rawLore) {
                lore.add(line
                        .replace("{maquina}",        nomeMaq)
                        .replace("{stack}",           String.valueOf(maq.getStack()))
                        .replace("{combustivel_tem}", String.format("%.0f", atual))
                        .replace("{capacidade}",      String.format("%.0f", cap))
                        .replace("{precisa}",         String.format("%.0f", precisa))
                        .replace("{progressbar}",     ConfigManager.colorir(bar))
                        .replace("{porcentagem}",     String.format("%.0f", pct)));
            }
        } else {
            lore.add("");
            lore.add("§fCombustível: §e" + String.format("%.0f", atual));
            lore.add("§fCapacidade:  §e" + String.format("%.0f", cap));
            lore.add("§fPrecisa:     §e" + String.format("%.0f", precisa));
            lore.add(ConfigManager.colorir(bar));
            lore.add("");
        }

        Material mat = MaterialCompat.of(config.getItemId(), "IRON_BLOCK");
        return new ItemBuilder(mat, config.getItemData())
                .nome(name)
                .lore(lore)
                .nbt("smaquinas_abast_maqid", maq.getId())
                .build();
    }

    // ── Item fixo (Combustivel / Confirmar / Cancelar) ─────────────────────

    private void buildFixedItem(FileConfiguration yml, String key, int tamanho, String... replacements) {
        ConfigurationSection sec = yml.getConfigurationSection("Itens." + key);
        if (sec == null) return;
        int slot = sec.getInt("Slot", -1);
        if (slot < 0 || slot >= tamanho) return;

        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", "&f" + key);
        List<String> lore = new ArrayList<>(sec.getStringList("Lore"));

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            final String from = replacements[i], to = replacements[i + 1];
            name = name.replace(from, to);
            List<String> tmp = new ArrayList<>();
            for (String l : lore) tmp.add(l.replace(from, to));
            lore = tmp;
        }

        Material mat = MaterialCompat.of(id, "STONE");
        inventory.setItem(slot, new ItemBuilder(mat, data)
                .nome(name).lore(lore).glow(glow)
                .nbt("smaquinas_abast_key", key)
                .build());
    }

    private ItemStack buildExtraItem(ConfigurationSection sec) {
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", " ");
        List<String> lore = sec.getStringList("Lore");
        String url   = sec.getString("URL", "");
        Material mat = MaterialCompat.of(id, "STONE");
        ItemBuilder builder = new ItemBuilder(mat, data).nome(name).lore(lore).glow(glow);
        if (!url.isEmpty() && url.startsWith("http")) builder.skullTexture(url);
        return builder.build();
    }

    // ── click ──────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if ("anterior".equals(action)) {
            if (page > 0) { page--; build(); player.openInventory(inventory); } return;
        }
        if ("proximo".equals(action)) {
            page++; build(); player.openInventory(inventory); return;
        }

        String key = ItemBuilder.getNBTString(item, "smaquinas_abast_key");
        if ("Confirmar".equals(key)) { confirmarAbastecimento(player); return; }
        if ("Cancelar".equals(key))  {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("cancelled"));
        }
    }

    // ── Confirmar abastecimento ────────────────────────────────────────────

    private void confirmarAbastecimento(Player player) {
        if (combustivelCfg == null) { player.closeInventory(); return; }

        List<MaquinaColocada> maquinas = getMaquinasCompativeis();
        if (maquinas.isEmpty()) {
            player.sendMessage(ConfigManager.colorir("&cNenhuma máquina compatível para abastecer."));
            player.closeInventory();
            return;
        }

        double litrosPorItem   = combustivelCfg.getLitros();
        int    itensNaMao      = itemMao != null ? itemMao.getAmount() : 0;
        double litrosRestantes = litrosPorItem * itensNaMao;
        int    abastecidas     = 0;

        for (MaquinaColocada maq : maquinas) {
            if (litrosRestantes <= 0) break;

            MaquinaConfig config = plugin.getMaquinaManager().getConfig(maq.getTipoMaquina());
            if (config == null) continue;

            // Combustível que deixa infinito
            if (combustivelCfg.isInfinito()) {
                maq.setCombustivelInfinito(true);
                litrosRestantes -= litrosPorItem;
                abastecidas++;
                plugin.getDB().saveMaquina(maq);
                plugin.getHologramManager().updateHologram(maq);
                continue;
            }

            // Combustível que preenche completamente sem ficar infinito
            if (combustivelCfg.isCombustivelInfinito()) {
                double cap = plugin.getMaquinaManager().getCapacidadeTotal(config, maq);
                maq.setCombustivel(cap);
                litrosRestantes -= litrosPorItem;
                abastecidas++;
                plugin.getDB().saveMaquina(maq);
                plugin.getHologramManager().updateHologram(maq);
                continue;
            }

            double cap     = plugin.getMaquinaManager().getCapacidadeTotal(config, maq);
            double precisa = cap - maq.getCombustivel();
            if (precisa <= 0) continue; // já cheia

            double adicionar = Math.min(precisa, litrosRestantes);
            maq.setCombustivel(maq.getCombustivel() + adicionar);
            litrosRestantes -= adicionar;

            plugin.getDB().saveMaquina(maq);
            plugin.getHologramManager().updateHologram(maq);
            abastecidas++;
        }

        // Consumir itens da mão
        if (combustivelCfg.isConsumir() && itemMao != null) {
            double litrosUsados = (litrosPorItem * itensNaMao) - litrosRestantes;
            int itensUsados = (int) Math.ceil(litrosUsados / litrosPorItem);
            itensUsados = Math.min(itensUsados, itensNaMao);
            itemMao.setAmount(Math.max(0, itensNaMao - itensUsados));
        }

        player.sendMessage(ConfigManager.colorir(
                "&aAbastecimento concluído! &f" + abastecidas + " &amáquina(s) abastecida(s)."));
        if (litrosRestantes <= 0 && abastecidas < maquinas.size()) {
            player.sendMessage(ConfigManager.colorir(
                    "&eCombustível insuficiente para abastecer todas as máquinas."));
        }
        player.closeInventory();
    }

    // ── Utilitários ────────────────────────────────────────────────────────

    private List<MaquinaColocada> getMaquinasCompativeis() {
        List<MaquinaColocada> result = new ArrayList<>();
        for (MaquinaColocada maq : plugin.getMaquinaManager().getMaquinasColocadas().values()) {
            if (!maq.getDono().equals(ownerUUID)) continue;
            if (maq.isQuebrada()) continue;
            MaquinaConfig config = plugin.getMaquinaManager().getConfig(maq.getTipoMaquina());
            if (config == null) continue;
            if (!config.getCombustiveisAceitos().contains(tipoCombustivel)) continue;
            // Se já for infinita e o combustível não for do tipo infinito → pular
            if (maq.getCombustivelInfinito() && combustivelCfg != null && !combustivelCfg.isInfinito()) continue;
            result.add(maq);
        }
        return result;
    }

    private String buildProgressBar(double pct) {
        int barQtd = plugin.getConfigManager().getConfig().getInt("Opcoes.Progress bar.Quantia", 10);
        String sim  = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Cor sim", "&a");
        String nao  = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Cor nao", "&7");
        String simb = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Simbolo", ":");
        int filled  = (int) Math.round((pct / 100.0) * barQtd);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barQtd; i++) sb.append(i < filled ? sim : nao).append(simb);
        return sb.toString();
    }
}
