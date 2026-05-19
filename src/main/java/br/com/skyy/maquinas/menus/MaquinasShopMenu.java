package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.models.ShopCombustivelEntry.ShopCusto;
import br.com.skyy.maquinas.models.ShopMaquinaEntry;
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

import java.util.*;

public class MaquinasShopMenu extends BaseMenu {

    private final int page;
    private final List<ShopMaquinaEntry> entries;

    private static final Map<UUID, Integer> multiplicadores = new HashMap<>();
    private static final Map<UUID, Long> delayQCooldown = new HashMap<>();

    /** Jogadores aguardando input do multiplicador no chat */
    public static final Map<UUID, MaquinasShopMenu> pendingMultiplier = new HashMap<>();

    /** Jogadores aguardando quantia de compra no chat */
    public static final Map<UUID, PendingMaquinaCompra> pendingCompra = new HashMap<>();

    public MaquinasShopMenu(SMaquinas plugin, int page) {
        super(plugin);
        this.page    = page;
        this.entries = loadEntries();
    }

    // ── Carregar entradas do shop/maquinas.yml ─────────────────────────────

    private List<ShopMaquinaEntry> loadEntries() {
        List<ShopMaquinaEntry> list = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfigManager()
                .getShopMaquinas().getConfigurationSection("Maquinas");
        if (sec == null) return list;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(key);
            if (cs != null) list.add(new ShopMaquinaEntry(key, cs));
        }
        return list;
    }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuShopMaquinas();
        String titulo = yml.getString("Nome", "&8Loja de Máquinas");
        int tamanho   = yml.getInt("Tamanho", 54);

        inventory = createInventory(tamanho, titulo);


        List<Integer> slots = getSlots(yml);

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

        // Itens fixos
        buildFixedItens(yml, tamanho, null);
    }

    public void build(Player player) {
        build();
        FileConfiguration yml = plugin.getConfigManager().getMenuShopMaquinas();
        int tamanho = yml.getInt("Tamanho", 54);
        List<Integer> slots = getSlots(yml);

        int inicio = page * slots.size();
        for (int i = 0; i < slots.size(); i++) {
            int idx = inicio + i;
            if (idx >= entries.size()) break;
            int slot = slots.get(i);
            if (slot >= 0 && slot < tamanho)
                inventory.setItem(slot, buildEntryItem(entries.get(idx), player));
        }

        buildFixedItens(yml, tamanho, player);
    }

    @Override
    public void open(Player player) {
        build(player);
        player.openInventory(inventory);
    }

    private List<Integer> getSlots(FileConfiguration yml) {
        List<Integer> slots = yml.getIntegerList("Slots");
        if (slots.isEmpty()) {
            slots = new ArrayList<>();
            for (int i = 10; i <= 34; i++) {
                int col = i % 9;
                if (col >= 1 && col <= 7) slots.add(i);
            }
        }
        return slots;
    }

    // ── Item de entrada do shop ────────────────────────────────────────────

    private ItemStack buildEntryItem(ShopMaquinaEntry entry, Player player) {
        if (entry.isLocked()) return buildLockedItem(entry);

        String name       = ConfigManager.colorir(entry.getItemName());
        List<String> lore = new ArrayList<>();
        for (String l : entry.getItemLore()) lore.add(ConfigManager.colorir(l));

        Material mat = MaterialCompat.of(entry.getItemId(), "IRON_BLOCK");
        ItemBuilder builder = new ItemBuilder(mat, entry.getItemData())
                .nome(name).lore(lore).glow(entry.isGlow())
                .nbt("smaquinas_shop_maquina_entry", entry.getId());

        if (entry.isCustomSkull() && !entry.getSkullUrl().isEmpty())
            builder.skullTexture(entry.getSkullUrl());

        return builder.build();
    }

    private ItemStack buildLockedItem(ShopMaquinaEntry entry) {
        ConfigurationSection sec = plugin.getConfigManager().getConfig()
                .getConfigurationSection("Items.Nao pode");

        String name      = sec != null ? sec.getString("Name", "&cIndisponível") : "&cIndisponível";
        List<String> raw = sec != null ? sec.getStringList("Lore") : new ArrayList<>();

        String itemName = ConfigManager.colorir(entry.getItemName());
        name = ConfigManager.colorir(name)
                .replace("{maquina}", itemName)
                .replace("{rank}",    ConfigManager.colorir(entry.getRank()));

        List<String> lore = new ArrayList<>();
        for (String l : raw) {
            lore.add(ConfigManager.colorir(l)
                    .replace("{maquina}",        itemName)
                    .replace("{rank}",           ConfigManager.colorir(entry.getRank()))
                    .replace("{data}",           entry.getDataFormatada())
                    .replace("{hora}",           entry.getHoraFormatada())
                    .replace("{time_formatted}", entry.getTempoRestante()));
        }

        return new ItemBuilder(Material.BARRIER).nome(name).lore(lore)
                .nbt("smaquinas_shop_maquina_locked", "true").build();
    }

    // ── Itens fixos ────────────────────────────────────────────────────────

    private void buildFixedItens(FileConfiguration yml, int tamanho, Player player) {
        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens == null) return;
        for (String key : itens.getKeys(false)) {
            ConfigurationSection sec = itens.getConfigurationSection(key);
            if (sec == null) continue;
            int slot = sec.getInt("Slot", -1);
            if (slot < 0 || slot >= tamanho) continue;
            inventory.setItem(slot, buildFixedItem(sec, key, player));
        }
    }

    private ItemStack buildFixedItem(ConfigurationSection sec, String key, Player player) {
        String url   = sec.getString("URL", "");
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", "&f" + key);
        List<String> rawLore = new ArrayList<>(sec.getStringList("Lore"));

        if (player != null) {
            name = applyPlayerPlaceholders(name, key, player);
            List<String> pl = new ArrayList<>();
            for (String l : rawLore) pl.add(applyPlayerPlaceholders(l, key, player));
            rawLore = pl;
        }

        Material mat = (!id.equals("AIR") && !id.isEmpty() && !id.equals("0"))
                ? MaterialCompat.of(id, "STONE")
                : MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat, data)
                .nome(name).lore(rawLore).glow(glow)
                .nbt("smaquinas_shop_key", key);

        if (!url.isEmpty() && url.startsWith("http")) builder.skullTexture(url);
        return builder.build();
    }

    private String applyPlayerPlaceholders(String text, String key, Player player) {
        switch (key) {
            case "Informacoes": {
                double money    = plugin.getEconomiaAPI().getSaldo(player);
                double desconto = getDesconto(player);
                String grupo    = getGrupoDisplay(player);
                double limite   = plugin.getLimiteManager().getLimite(player.getUniqueId());
                int maquinas    = plugin.getDB().getTopCompradas(Integer.MAX_VALUE).stream()
                        .filter(e -> e.getKey().equals(player.getUniqueId()))
                        .mapToInt(e -> e.getValue().intValue()).sum();
                text = text
                        .replace("{money}",    NumberFormatter.formatStatic(money))
                        .replace("{desconto}", String.format("%.0f", desconto))
                        .replace("{grupo}",    ConfigManager.colorir(grupo))
                        .replace("{limite}",   NumberFormatter.formatStatic(limite))
                        .replace("{maquinas}", String.valueOf(maquinas));
                break;
            }
            case "Multiplicador": {
                int ativo  = getMultiplicadorAtivo(player);
                int maximo = getMultiplicadorMax(player);
                text = text
                        .replace("{ativo}",  String.valueOf(ativo))
                        .replace("{maximo}", String.valueOf(maximo));
                break;
            }
        }
        return text;
    }

    // ── click ──────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        // Paginação
        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if ("anterior".equals(action)) {
            if (page > 0) org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new MaquinasShopMenu(plugin, page - 1).open(player));
            return;
        }
        if ("proximo".equals(action)) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new MaquinasShopMenu(plugin, page + 1).open(player));
            return;
        }

        // Item bloqueado → sem ação
        if (ItemBuilder.getNBTString(item, "smaquinas_shop_maquina_locked") != null) return;

        // Itens fixos
        String shopKey = ItemBuilder.getNBTString(item, "smaquinas_shop_key");
        if (shopKey != null) {
            switch (shopKey) {
                case "Top":
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new TopLimiteMenu(plugin).open(player));
                    return;
                case "Multiplicador":
                    player.closeInventory();
                    pendingMultiplier.put(player.getUniqueId(), new MaquinasShopMenu(plugin, page));
                    int atual  = getMultiplicadorAtivo(player);
                    int maximo = getMultiplicadorMax(player);
                    player.sendMessage(plugin.getConfigManager().msg("digit-multiplier",
                            "{atual}",  String.valueOf(atual),
                            "{maximo}", String.valueOf(maximo)));
                    return;
            }
        }

        // Item de entrada do shop de máquinas
        String entryId = ItemBuilder.getNBTString(item, "smaquinas_shop_maquina_entry");
        if (entryId == null) return;

        ShopMaquinaEntry entry = entries.stream()
                .filter(e -> e.getId().equals(entryId)).findFirst().orElse(null);
        if (entry == null) return;

        if (click == ClickType.SHIFT_RIGHT) {
            // Shift+direito → preview do drop
            MaquinaConfig config = plugin.getMaquinaManager().getConfig(entry.getMaquinaTipo());
            if (config != null) {
                final MaquinaConfig finalConfig = config;
                openLater(plugin, player, () -> new DropPreviewMenu(plugin, finalConfig, page));
            }
        } else if (click == ClickType.DROP) {
            // Q → comprar limite disponível (se habilitado na config)
            boolean compraQ = plugin.getConfigManager().getConfig().getBoolean("Opcoes.Compra Q", true);
            if (!compraQ) return;
            boolean limiteAtivo = plugin.getConfigManager().getConfig().getBoolean("Limite.Ativar", true);
            if (!limiteAtivo) return;

            // DelayQ cooldown
            int delayQ = plugin.getConfigManager().getConfig().getInt("Opcoes.DelayQ", 10);
            if (delayQ > 0) {
                long agora = System.currentTimeMillis();
                long ultimo = delayQCooldown.getOrDefault(player.getUniqueId(), 0L);
                long restanteMs = (ultimo + delayQ * 1000L) - agora;
                if (restanteMs > 0) {
                    player.sendMessage(plugin.getConfigManager().msg("delay-q",
                            "{tempo}", String.valueOf((int) Math.ceil(restanteMs / 1000.0))));
                    return;
                }
                delayQCooldown.put(player.getUniqueId(), agora);
            }

            double limiteDisponivel = plugin.getLimiteManager().getLimite(player.getUniqueId());
            int qtd = (int) Math.max(1, limiteDisponivel);
            executarCompra(player, entry, qtd);
        } else if (click == ClickType.RIGHT) {
            // Direito → comprar 1 × multiplicador
            executarCompra(player, entry, 1);
        } else if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
            // Esquerdo → escolher quantia no chat (se habilitado) ou comprar 1
            boolean compraChat = plugin.getConfigManager().getConfig().getBoolean("Opcoes.Compra chat", true);
            if (!compraChat) {
                executarCompra(player, entry, 1);
                return;
            }
            player.closeInventory();
            pendingCompra.put(player.getUniqueId(), new PendingMaquinaCompra(plugin, entry, page));
            int mult = getMultiplicadorAtivo(player);
            player.sendMessage(plugin.getConfigManager().msg("digit-amount",
                    "{multiplicador}", String.valueOf(mult)));
        }
    }

    // ── Lógica de compra ───────────────────────────────────────────────────

    public void executarCompra(Player player, ShopMaquinaEntry entry, int quantia) {
        if (quantia <= 0) return;

        // Aplicar multiplicador: quantia inserida × multiplicador ativo
        int multiplicador = getMultiplicadorAtivo(player);
        int totalQuantia  = quantia * multiplicador;

        if (entry.isLocked()) {
            player.sendMessage(plugin.getConfigManager().msg("shop-locked",
                    "{data}", entry.getDataFormatada(),
                    "{hora}", entry.getHoraFormatada()));
            return;
        }

        // Verificar saldo para todos os custos
        for (ShopCusto custo : entry.getCustos()) {
            double total = custo.getCusto() * totalQuantia;
            if (!plugin.getEconomiaAPI().temDinheiro(player, total, custo.getTipo())) {
                player.sendMessage(plugin.getConfigManager().msg("no-balance",
                        "{provider_display}",   custo.getDisplay(),
                        "{provider_balance}",   NumberFormatter.formatStatic(
                                plugin.getEconomiaAPI().getSaldo(player))));
                return;
            }
        }

        // Verificar limite de compra
        boolean gastarLimite = plugin.getConfigManager().getConfig()
                .getBoolean("Opcoes.GastarLimite", true);
        boolean limiteAtivo = plugin.getConfigManager().getConfig()
                .getBoolean("Limite.Ativar", true);

        if (gastarLimite && limiteAtivo) {
            double limiteDisponivel = plugin.getLimiteManager().getLimite(player.getUniqueId());
            if (limiteDisponivel < totalQuantia) {
                player.sendMessage(plugin.getConfigManager().msg("limit-need",
                        "{limite}", NumberFormatter.formatStatic(limiteDisponivel)));
                return;
            }
        }

        // Inventário cheio?
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getConfigManager().msg("inv-full"));
            return;
        }

        // Cobrar todos os custos
        for (ShopCusto custo : entry.getCustos()) {
            plugin.getEconomiaAPI().cobrar(player, custo.getCusto() * totalQuantia, custo.getTipo());
        }

        // Gastar limite
        if (gastarLimite && limiteAtivo) {
            plugin.getLimiteManager().removeLimite(player.getUniqueId(), totalQuantia);
        }

        // Dar a máquina (1 item com stack = totalQuantia)
        String tipo = entry.getMaquinaTipo();
        ItemStack itemMaquina = plugin.getMaquinaManager().criarItemMaquinaComStack(tipo, totalQuantia);
        if (itemMaquina != null) player.getInventory().addItem(itemMaquina);

        // Registrar compra
        for (int i = 0; i < totalQuantia; i++) plugin.getDB().addCompraMaquina(player.getUniqueId(), tipo);

        // Mensagem
        MaquinaConfig config = plugin.getMaquinaManager().getConfig(tipo);
        String nomeMaquina = config != null ? ConfigManager.colorir(config.getNome())
                : ConfigManager.colorir(entry.getItemName());
        player.sendMessage(plugin.getConfigManager().msg("bought",
                "{maquina}",      nomeMaquina,
                "{quantia}",      String.valueOf(totalQuantia),
                "{multiplicador}", String.valueOf(multiplicador)));

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new MaquinasShopMenu(plugin, page).open(player));
    }

    // ── Definir multiplicador (chamado pelo ChatListener) ───────────────────

    public void setMultiplicador(Player player, int valor) {
        int maximo = getMultiplicadorMax(player);
        if (valor > maximo) {
            player.sendMessage(plugin.getConfigManager().msg("max-multiplier",
                    "{maximo}", String.valueOf(maximo)));
            valor = maximo;
        }
        if (valor < 1) valor = 1;
        multiplicadores.put(player.getUniqueId(), valor);
        player.sendMessage(plugin.getConfigManager().msg("multiplier-defined",
                "{multiplicador}", String.valueOf(valor)));
        this.open(player);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public static int getMultiplicadorAtivo(Player player) {
        return multiplicadores.getOrDefault(player.getUniqueId(), 1);
    }

    private int getMultiplicadorMax(Player player) {
        int max = plugin.getConfigManager().getConfig().getInt("Opcoes.Multiplicador max", 10);
        for (int i = max; i >= 1; i--) {
            if (player.hasPermission("smaquinas.multiplicador." + i)) return i;
        }
        return 1;
    }

    private double getDesconto(Player player) {
        ConfigurationSection descontos = plugin.getConfigManager().getDescontos()
                .getConfigurationSection("Descontos");
        if (descontos == null) return 0;
        double melhor = 0;
        boolean acumular = plugin.getConfigManager().getConfig().getBoolean("Opcoes.Acumular bonus", false);
        double acumulado = 0;
        for (String key : descontos.getKeys(false)) {
            ConfigurationSection d = descontos.getConfigurationSection(key);
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
        for (String key : descontos.getKeys(false)) {
            ConfigurationSection d = descontos.getConfigurationSection(key);
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

    public static class PendingMaquinaCompra {
        private final SMaquinas plugin;
        public final ShopMaquinaEntry entry;
        public final int shopPage;

        public PendingMaquinaCompra(SMaquinas plugin, ShopMaquinaEntry entry, int shopPage) {
            this.plugin   = plugin;
            this.entry    = entry;
            this.shopPage = shopPage;
        }

        public void executar(Player player, int quantia) {
            new MaquinasShopMenu(plugin, shopPage).executarCompra(player, entry, quantia);
        }
    }
}
