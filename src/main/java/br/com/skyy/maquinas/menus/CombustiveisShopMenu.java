package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.ShopCombustivelEntry;
import br.com.skyy.maquinas.models.ShopCombustivelEntry.ShopCusto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CombustiveisShopMenu extends BaseMenu {

    private final int page;
    /** Entradas da loja carregadas do shop/combustiveis.yml */
    private final List<ShopCombustivelEntry> entries;

    public CombustiveisShopMenu(SMaquinas plugin, int page) {
        super(plugin);
        this.page    = page;
        this.entries = loadEntries();
    }

    // ── Carregar entradas do shop/combustiveis.yml ────────────────────────

    private List<ShopCombustivelEntry> loadEntries() {
        List<ShopCombustivelEntry> list = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfigManager()
                .getShopCombustiveis().getConfigurationSection("Combustiveis");
        if (sec == null) return list;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(key);
            if (cs != null) list.add(new ShopCombustivelEntry(key, cs));
        }
        return list;
    }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuShopCombustiveis();
        String titulo = yml.getString("Nome", "&8Loja de Combustíveis");
        int tamanho   = yml.getInt("Tamanho", 54);

        inventory = createInventory(tamanho, titulo);


        // Slots de conteúdo
        List<Integer> slots = yml.getIntegerList("Slots");
        if (slots.isEmpty()) {
            slots = new ArrayList<>();
            for (int i = 10; i <= 34; i++) {
                int col = i % 9;
                if (col >= 1 && col <= 7) slots.add(i);
            }
        }

        int inicio = page * slots.size();
        for (int i = 0; i < slots.size(); i++) {
            int idx = inicio + i;
            if (idx >= entries.size()) break;
            int slot = slots.get(i);
            if (slot >= 0 && slot < tamanho)
                inventory.setItem(slot, buildEntryItem(entries.get(idx), null));
        }

        // Paginação – anterior
        int anteriorSlot = yml.getInt("AnteriorSlot", 45);
        if (page > 0) {
            ConfigurationSection sa = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Anterior");
            Material ma = sa != null ? MaterialCompat.of(sa.getString("ID","262"), "ARROW") : Material.ARROW;
            String   na = sa != null ? sa.getString("Name","&cAnterior") : "&cAnterior";
            inventory.setItem(anteriorSlot, new ItemBuilder(ma)
                    .nome(na).lore(sa != null ? sa.getStringList("Lore") : new ArrayList<>())
                    .nbt("smaquinas_action","anterior").build());
        }

        // Paginação – próximo
        int proximoSlot = yml.getInt("ProximoSlot", 53);
        if ((page + 1) * slots.size() < entries.size()) {
            ConfigurationSection sp = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Proximo");
            Material mp = sp != null ? MaterialCompat.of(sp.getString("ID","262"), "ARROW") : Material.ARROW;
            String   np = sp != null ? sp.getString("Name","&aProximo") : "&aProximo";
            inventory.setItem(proximoSlot, new ItemBuilder(mp)
                    .nome(np).lore(sp != null ? sp.getStringList("Lore") : new ArrayList<>())
                    .nbt("smaquinas_action","proximo").build());
        }

        // Itens fixos (sem player)
        buildFixedItens(yml, tamanho, null);
    }

    /** Versão com player para preencher placeholders */
    public void build(Player player) {
        build();
        FileConfiguration yml = plugin.getConfigManager().getMenuShopCombustiveis();
        int tamanho = yml.getInt("Tamanho", 54);

        // Substituir itens de entrada com dados do player (permissão + data)
        List<Integer> slots = yml.getIntegerList("Slots");
        if (slots.isEmpty()) {
            slots = new ArrayList<>();
            for (int i = 10; i <= 34; i++) {
                int col = i % 9;
                if (col >= 1 && col <= 7) slots.add(i);
            }
        }
        int inicio = page * slots.size();
        for (int i = 0; i < slots.size(); i++) {
            int idx = inicio + i;
            if (idx >= entries.size()) break;
            int slot = slots.get(i);
            if (slot >= 0 && slot < tamanho)
                inventory.setItem(slot, buildEntryItem(entries.get(idx), player));
        }

        // Itens fixos com player
        buildFixedItens(yml, tamanho, player);
    }

    @Override
    public void open(Player player) {
        build(player);
        player.openInventory(inventory);
    }

    // ── Item de entrada do shop ────────────────────────────────────────────

    private ItemStack buildEntryItem(ShopCombustivelEntry entry, Player player) {
        // Sem permissão?
        if (player != null && !entry.getPermissao().isEmpty()
                && !player.hasPermission(entry.getPermissao())) {
            return buildLockedItem("Permissao combustivel", entry, player);
        }

        // Data bloqueada?
        if (entry.isLocked()) {
            return buildLockedItem("Nao pode combustivel", entry, player);
        }

        String name     = entry.getItemName();
        List<String> lore = new ArrayList<>(entry.getItemLore());

        Material mat = (!entry.getItemId().equals("AIR") && !entry.getItemId().isEmpty()
                && !entry.getItemId().equals("0"))
                ? MaterialCompat.of(entry.getItemId(), "COAL")
                : MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat, entry.getItemData())
                .nome(name).lore(lore).glow(entry.isGlow())
                .nbt("smaquinas_combustivel_shop_entry", entry.getId());

        if (entry.isCustomSkull() && !entry.getSkullUrl().isEmpty())
            builder.skullTexture(entry.getSkullUrl());

        return builder.build();
    }

    /** Item de bloqueio por data ou permissão */
    private ItemStack buildLockedItem(String configKey, ShopCombustivelEntry entry, Player player) {
        ConfigurationSection sec = plugin.getConfigManager().getConfig()
                .getConfigurationSection("Items." + configKey);

        String name = sec != null ? sec.getString("Name", "{combustivel}") : "{combustivel}";
        List<String> rawLore = sec != null ? sec.getStringList("Lore") : new ArrayList<>();

        name = name.replace("{combustivel}", ConfigManager.colorir(entry.getItemName()));
        List<String> lore = new ArrayList<>();
        for (String l : rawLore) {
            l = l.replace("{combustivel}",    ConfigManager.colorir(entry.getItemName()))
                 .replace("{data}",           entry.getDataFormatada())
                 .replace("{hora}",           entry.getHoraFormatada())
                 .replace("{time_formatted}", entry.getTempoRestante());
            lore.add(l);
        }

        return new ItemBuilder(Material.BARRIER).nome(name).lore(lore)
                .nbt("smaquinas_combustivel_shop_locked", "true").build();
    }

    // ── Itens fixos do menu (Informacoes, etc.) ───────────────────────────

    private void buildFixedItens(FileConfiguration yml, int tamanho, Player player) {
        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens == null) return;
        for (String key : itens.getKeys(false)) {
            ConfigurationSection sec = itens.getConfigurationSection(key);
            if (sec == null) continue;
            int slot = sec.getInt("Slot", -1);
            if (slot < 0 || slot >= tamanho) continue;
            inventory.setItem(slot, buildFixedItem(sec, player));
        }
    }

    private ItemStack buildFixedItem(ConfigurationSection sec, Player player) {
        String url   = sec.getString("URL", "");
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", " ");
        List<String> rawLore = new ArrayList<>(sec.getStringList("Lore"));

        if (player != null) {
            name = applyPlayerPlaceholders(name, player);
            List<String> pl = new ArrayList<>();
            for (String l : rawLore) pl.add(applyPlayerPlaceholders(l, player));
            rawLore = pl;
        }

        Material mat = (!id.equals("AIR") && !id.isEmpty() && !id.equals("0"))
                ? MaterialCompat.of(id, "STONE")
                : MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat, data)
                .nome(name).lore(rawLore).glow(glow)
                .nbt("smaquinas_combustivel_fixed", "true");

        if (!url.isEmpty() && url.startsWith("http")) builder.skullTexture(url);
        return builder.build();
    }

    private String applyPlayerPlaceholders(String text, Player player) {
        double money    = plugin.getEconomiaAPI().getSaldo(player);
        double desconto = getDesconto(player);
        String grupo    = getGrupoDisplay(player);
        return text
                .replace("{money}",    NumberFormatter.formatStatic(money))
                .replace("{desconto}", String.format("%.0f", desconto))
                .replace("{grupo}",    ConfigManager.colorir(grupo));
    }

    // ── click ──────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        // Paginação
        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if ("anterior".equals(action)) {
            if (page > 0) org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new CombustiveisShopMenu(plugin, page - 1).open(player));
            return;
        }
        if ("proximo".equals(action)) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new CombustiveisShopMenu(plugin, page + 1).open(player));
            return;
        }

        // Item fixo ou bloqueado → sem ação
        if (ItemBuilder.getNBTString(item, "smaquinas_combustivel_fixed") != null) return;
        if (ItemBuilder.getNBTString(item, "smaquinas_combustivel_shop_locked") != null) return;

        // Item de combustível do shop → comprar
        String entryId = ItemBuilder.getNBTString(item, "smaquinas_combustivel_shop_entry");
        if (entryId != null) {
            ShopCombustivelEntry entry = entries.stream()
                    .filter(e -> e.getId().equals(entryId)).findFirst().orElse(null);
            if (entry != null) comprar(player, entry, click);
        }
    }

    // ── Lógica de compra ───────────────────────────────────────────────────

    private void comprar(Player player, ShopCombustivelEntry entry, ClickType click) {
        // Verificar permissão
        if (!entry.getPermissao().isEmpty() && !player.hasPermission(entry.getPermissao())) {
            player.sendMessage(plugin.getConfigManager().msg("permission")); return;
        }
        // Verificar data
        if (entry.isLocked()) {
            player.sendMessage(plugin.getConfigManager().msg("permission")); return;
        }

        // Determinar quantia
        boolean isShift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
        boolean isLeft  = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;

        // Botão direito → comprar 1; botão esquerdo → pedir quantia no chat
        if (isLeft && !isShift) {
            // Pedir quantia no chat
            player.closeInventory();
            PendingCombustivelCompra.pending.put(player.getUniqueId(),
                    new PendingCombustivelCompra(plugin, entry, page));
            player.sendMessage(plugin.getConfigManager().msg("digit-fuel"));
            return;
        }

        // Botão direito → 1 unidade (ou shift → multiplicador)
        int mult    = MaquinasShopMenu.getMultiplicadorAtivo(player);
        int quantia = isShift ? mult : 1;

        executarCompra(player, entry, quantia);
    }

    public void executarCompra(Player player, ShopCombustivelEntry entry, int quantia) {
        if (quantia <= 0) return;

        // Verificar e cobrar cada custo
        for (ShopCusto custo : entry.getCustos()) {
            double total = custo.getCusto() * quantia;
            if (!plugin.getEconomiaAPI().temDinheiro(player, total, custo.getTipo())) {
                player.sendMessage(plugin.getConfigManager().msg("no-balance",
                        "{provider_display}",   custo.getDisplay(),
                        "{provider_balance}",   NumberFormatter.formatStatic(
                                plugin.getEconomiaAPI().getSaldo(player))));
                return;
            }
        }
        for (ShopCusto custo : entry.getCustos()) {
            plugin.getEconomiaAPI().cobrar(player, custo.getCusto() * quantia, custo.getTipo());
        }

        // Dar combustível
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getConfigManager().msg("inv-full")); return;
        }

        String tipo = entry.getCombustivelTipo();
        ItemStack itemComb = plugin.getCombustivelManager()
                .criarItemCombustivelComQuantia(tipo, quantia);
        if (itemComb != null) player.getInventory().addItem(itemComb);

        // Registrar compra
        plugin.getDB().addCompraCombustivel(player.getUniqueId(), tipo);

        // Mensagem
        String nomeComb = ConfigManager.colorir(entry.getItemName());
        player.sendMessage(plugin.getConfigManager().msg("bought-fuel",
                "{combustivel}", nomeComb,
                "{quantia}",     String.valueOf(quantia)));

        // Reabrir menu
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new CombustiveisShopMenu(plugin, page).open(player));
    }

    // ── Desconto / Grupo ───────────────────────────────────────────────────

    private double getDesconto(Player player) {
        ConfigurationSection descontos = plugin.getConfigManager().getDescontos()
                .getConfigurationSection("Descontos");
        if (descontos == null) return 0;
        boolean acumular = plugin.getConfigManager().getConfig().getBoolean("Opcoes.Acumular bonus", false);
        double melhor = 0, acumulado = 0;
        for (String k : descontos.getKeys(false)) {
            ConfigurationSection d = descontos.getConfigurationSection(k);
            if (d == null) continue;
            String perm = d.getString("Permissao", "");
            if (!perm.isEmpty() && player.hasPermission(perm)) {
                double val = d.getDouble("Desconto", 0);
                if (acumular) acumulado += val; else if (val > melhor) melhor = val;
            }
        }
        return acumular ? acumulado : melhor;
    }

    private String getGrupoDisplay(Player player) {
        ConfigurationSection descontos = plugin.getConfigManager().getDescontos()
                .getConfigurationSection("Descontos");
        if (descontos == null) return "&7Nenhum";
        String display = "&7Nenhum"; int ordem = Integer.MAX_VALUE;
        for (String k : descontos.getKeys(false)) {
            ConfigurationSection d = descontos.getConfigurationSection(k);
            if (d == null) continue;
            String perm = d.getString("Permissao", "");
            if (!perm.isEmpty() && player.hasPermission(perm)) {
                int o = d.getInt("Ordem", 999);
                if (o < ordem) { ordem = o; display = d.getString("Display", "&7Nenhum"); }
            }
        }
        return display;
    }

    // ── Inner: pending de compra via chat ─────────────────────────────────

    public static class PendingCombustivelCompra {
        public static final Map<java.util.UUID, PendingCombustivelCompra> pending = new LinkedHashMap<>();

        private final SMaquinas plugin;
        public final ShopCombustivelEntry entry;
        public final int shopPage;

        public PendingCombustivelCompra(SMaquinas plugin, ShopCombustivelEntry entry, int shopPage) {
            this.plugin   = plugin;
            this.entry    = entry;
            this.shopPage = shopPage;
        }

        public void executar(Player player, int quantia) {
            new CombustiveisShopMenu(plugin, shopPage).executarCompra(player, entry, quantia);
        }
    }
}
