package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaConfig;
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

/**
 * Menu de visualização prévia dos drops de uma máquina.
 * Aberto ao clicar com botão DIREITO num item de máquina na loja,
 * permitindo ver o drop sem precisar comprar a máquina.
 */
public class DropPreviewMenu extends BaseMenu {

    private final MaquinaConfig config;
    /** Página de origem na loja (para voltar corretamente) */
    private final int shopPage;

    public DropPreviewMenu(SMaquinas plugin, MaquinaConfig config, int shopPage) {
        super(plugin);
        this.config   = config;
        this.shopPage = shopPage;
    }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuDropPreview();

        // Título: substituir {maquina} pelo nome da máquina
        String titulo = yml.getString("Nome", "{maquina}");
        if (config != null) titulo = titulo.replace("{maquina}", ConfigManager.colorir(config.getNome()));

        int tamanho = yml.getInt("Tamanho", 27);
        inventory = createInventory(tamanho, titulo);


        if (config == null) return;

        // ── Item do drop (Drop slot) ───────────────────────────────────────
        int dropSlot = yml.getInt("Drop slot", 13);
        if (dropSlot >= 0 && dropSlot < tamanho) {
            inventory.setItem(dropSlot, buildDropItem(config));
        }

        // ── Itens decorativos extras ───────────────────────────────────────
        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens != null) {
            for (String key : itens.getKeys(false)) {
                ConfigurationSection sec = itens.getConfigurationSection(key);
                if (sec == null) continue;
                int slot = sec.getInt("Slot", -1);
                if (slot < 0 || slot >= tamanho) continue;
                inventory.setItem(slot, buildExtraItem(sec));
            }
        }

        // ── Botão voltar ──────────────────────────────────────────────────
        int backSlot = yml.getInt("Backslot", 18);
        if (backSlot >= 0 && backSlot < tamanho) {
            ConfigurationSection setaVoltar = plugin.getConfigManager().getConfig()
                    .getConfigurationSection("Setas.Voltar");
            if (setaVoltar != null) {
                Material mat = MaterialCompat.of(setaVoltar.getString("ID", "262"), "ARROW");
                inventory.setItem(backSlot, new ItemBuilder(mat)
                        .nome(setaVoltar.getString("Name", "&cVoltar"))
                        .lore(setaVoltar.getStringList("Lore"))
                        .nbt("smaquinas_action", "voltar").build());
            } else {
                inventory.setItem(backSlot, new ItemBuilder(Material.ARROW)
                        .nome("&cVoltar").nbt("smaquinas_action", "voltar").build());
            }
        }
    }

    // ── Monta o item de drop com informações estáticas ────────────────────

    private ItemStack buildDropItem(MaquinaConfig config) {
        MaquinaConfig.DropConfig drop = config.getDropConfig();

        // Calcular valores com upgrades padrão (sem máquina colocada)
        int nivelDropPadrao = config.getUpgradeDrops().getPadrao();
        double dropsPorRodada = config.getUpgradeDrops()
                .calcularValorEfetivo(nivelDropPadrao, 1);

        double valorTotal = dropsPorRodada * drop.getPreco();
        double bolsa      = 100.0; // placeholder yBolsa

        List<String> rawLore = drop.getIconeLore();
        List<String> lore = new ArrayList<>();

        if (!rawLore.isEmpty()) {
            for (String line : rawLore) {
                line = line
                        .replace("{drops_armazenados}", String.format("%.1f", dropsPorRodada))
                        .replace("{money}",              NumberFormatter.formatStatic(valorTotal))
                        .replace("{bolsa}",              String.format("%.0f", bolsa))
                        .replace("{bonus_raw}",          "0.0")
                        .replace("{bonus}",              "0%");
                lore.add(line);
            }
        } else {
            lore.add("");
            lore.add(" &fDrop: &a" + ConfigManager.colorir(drop.getNome()) + "&f.");
            lore.add(" &fDrops por rodada: &a" + String.format("%.1f", dropsPorRodada) + "&f.");
            lore.add(" &fValor por drop: &a" + NumberFormatter.formatStatic(drop.getPreco()) + " coins&f.");
            lore.add(" &fValor por rodada: &a" + NumberFormatter.formatStatic(valorTotal) + " coins&f.");
            lore.add("");
            if (drop.isVender())   lore.add("&fVenda: &aHabilitada");
            if (drop.isRecolher()) lore.add("&fColeta: &aHabilitada");
            if (drop.isComandoAtivar()) lore.add("&fComando ao coletar: &aHabilitado");
            lore.add("");
        }

        Material mat = MaterialCompat.of(drop.getIconeId(), "IRON_INGOT");
        String name  = drop.getIconeName().isEmpty()
                ? "&f" + ConfigManager.colorir(drop.getNome())
                : drop.getIconeName();

        return new ItemBuilder(mat, drop.getIconeData())
                .nome(name)
                .lore(lore)
                .nbt("smaquinas_drop_preview", config.getId())
                .build();
    }

    private ItemStack buildExtraItem(ConfigurationSection sec) {
        String url   = sec.getString("URL", "");
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", " ");
        List<String> lore = sec.getStringList("Lore");

        Material mat = (!id.equals("AIR") && !id.isEmpty() && !id.equals("0"))
                ? MaterialCompat.of(id, "STONE")
                : MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat, data).nome(name).lore(lore).glow(glow);
        if (!url.isEmpty() && url.startsWith("http")) builder.skullTexture(url);
        return builder.build();
    }

    // ── click ──────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if ("voltar".equals(action)) {
            openLater(plugin, player, () -> new MaquinasShopMenu(plugin, shopPage));
        }
        // Clique no item de drop não faz nada — é apenas preview
    }
}
