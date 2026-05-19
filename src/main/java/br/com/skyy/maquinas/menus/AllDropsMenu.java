package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Bukkit;
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
 * Menu que exibe os drops de TODAS as máquinas do jogador (e de amigos, se configurado).
 */
public class AllDropsMenu extends BaseMenu {

    private final UUID ownerUUID;
    private int page;

    public AllDropsMenu(SMaquinas plugin, UUID ownerUUID) {
        this(plugin, ownerUUID, 0);
    }

    public AllDropsMenu(SMaquinas plugin, UUID ownerUUID, int page) {
        super(plugin);
        this.ownerUUID = ownerUUID;
        this.page = page;
    }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuAllDrops();

        String titulo = yml.getString("Nome", "&8Máquinas Drops");
        int tamanho   = yml.getInt("Tamanho", 36);

        inventory = createInventory(tamanho, titulo);


        // Coletar máquinas que o jogador tem acesso (próprias + amigo se DropsMostrarAmigo)
        boolean mostrarAmigo = plugin.getConfigManager().getConfig()
                .getBoolean("Opcoes.DropsMostrarAmigo", true);

        List<MaquinaColocada> maquinas = new ArrayList<>();
        for (MaquinaColocada m : plugin.getMaquinaManager().getMaquinasColocadas().values()) {
            if (m.getDono().equals(ownerUUID)) {
                maquinas.add(m);
            } else if (mostrarAmigo && m.isAmigo(ownerUUID)) {
                maquinas.add(m);
            }
        }

        // Slots configurados
        List<Integer> slots = yml.getIntegerList("Slots");
        if (slots.isEmpty()) slots = java.util.Arrays.asList(10, 11, 12, 13, 14, 15, 16);

        int perPage = slots.size();
        int inicio  = page * perPage;

        // Renderizar máquinas na página
        for (int i = 0; i < perPage; i++) {
            int idx = inicio + i;
            if (idx >= maquinas.size()) break;
            int slot = slots.get(i);
            if (slot < 0 || slot >= tamanho) continue;

            MaquinaColocada maq = maquinas.get(idx);
            MaquinaConfig config = plugin.getMaquinaManager().getConfig(maq.getTipoMaquina());
            if (config == null) continue;

            inventory.setItem(slot, buildMaquinaDropItem(maq, config));
        }

        // Navegação – anterior
        int voltarSlot  = yml.getInt("VoltarSlot", 9);
        int proximoSlot = yml.getInt("ProximoSlot", 17);

        if (page > 0) {
            ConfigurationSection setaAnt = plugin.getConfigManager().getConfig()
                    .getConfigurationSection("Setas.Anterior");
            if (setaAnt != null) {
                Material mat = MaterialCompat.of(setaAnt.getString("ID", "262"), "ARROW");
                inventory.setItem(voltarSlot, new ItemBuilder(mat)
                        .nome(setaAnt.getString("Name", "&cAnterior"))
                        .lore(setaAnt.getStringList("Lore"))
                        .nbt("smaquinas_action", "anterior").build());
            } else {
                inventory.setItem(voltarSlot, new ItemBuilder(Material.ARROW)
                        .nome("&cAnterior").nbt("smaquinas_action", "anterior").build());
            }
        } else {
            // Slot vazio (ou vidro já preenchido)
        }

        // Navegação – próximo
        if (maquinas.size() > inicio + perPage) {
            ConfigurationSection setaProx = plugin.getConfigManager().getConfig()
                    .getConfigurationSection("Setas.Proximo");
            if (setaProx != null) {
                Material mat = MaterialCompat.of(setaProx.getString("ID", "262"), "ARROW");
                inventory.setItem(proximoSlot, new ItemBuilder(mat)
                        .nome(setaProx.getString("Name", "&aProximo"))
                        .lore(setaProx.getStringList("Lore"))
                        .nbt("smaquinas_action", "proximo").build());
            } else {
                inventory.setItem(proximoSlot, new ItemBuilder(Material.ARROW)
                        .nome("&aProximo").nbt("smaquinas_action", "proximo").build());
            }
        }

        // Botão Vender Tudo
        ConfigurationSection sellAll = yml.getConfigurationSection("SellAll");
        if (sellAll != null) {
            int sellSlot = sellAll.getInt("Slot", 31);
            if (sellSlot >= 0 && sellSlot < tamanho) {
                String url   = sellAll.getString("URL", "");
                String id    = sellAll.getString("ID", "EMERALD");
                int data     = sellAll.getInt("Data", 0);
                String name  = sellAll.getString("Name", "&aVender Tudo");
                List<String> lore = sellAll.getStringList("Lore");

                Material mat = (!id.equals("AIR") && !id.isEmpty() && !id.equals("0"))
                        ? MaterialCompat.of(id, "EMERALD")
                        : MaterialCompat.playerHead();

                ItemBuilder builder = new ItemBuilder(mat, data)
                        .nome(name).lore(lore)
                        .nbt("smaquinas_action", "sell_all");

                if (!url.isEmpty() && url.startsWith("http")) builder.skullTexture(url);

                inventory.setItem(sellSlot, builder.build());
            }
        }

        // Itens decorativos extras
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
    }

    // ── Monta item de máquina (usando Icone-All da config) ────────────────

    private ItemStack buildMaquinaDropItem(MaquinaColocada maq, MaquinaConfig config) {
        MaquinaConfig.DropConfig drop = config.getDropConfig();
        double drops      = maq.getDrops();
        double valorTotal = drops * drop.getPreco();
        double bonus      = plugin.getBoosterManager().getMultiplicadorVenda(ownerUUID);
        double bonusRaw   = (bonus - 1.0) * 100.0;
        double bolsa      = 100.0; // placeholder yBolsa

        String playerName = Bukkit.getOfflinePlayer(maq.getDono()).getName();
        if (playerName == null) playerName = "Desconhecido";

        // Usar Icone-All se existir na config (lore configurada)
        String iconeName = drop.getIconeName();
        if (iconeName.isEmpty()) iconeName = config.getNome();

        List<String> rawLore = drop.getIconeLore();

        // Se não há lore ou preferir usar o Icone-All do yml de máquinas
        List<String> lore = new ArrayList<>();
        if (!rawLore.isEmpty()) {
            for (String line : rawLore) {
                line = line
                        .replace("{player}",           playerName)
                        .replace("{drops_armazenados}", String.format("%.0f", drops))
                        .replace("{money}",             NumberFormatter.formatStatic(valorTotal))
                        .replace("{bolsa}",             String.format("%.0f", bolsa))
                        .replace("{bonus_raw}",         String.format("%.1f", bonusRaw))
                        .replace("{bonus}",             NumberFormatter.formatStatic(bonusRaw) + "%");
                lore.add(line);
            }
        } else {
            lore.add("&7Dono: &f" + playerName);
            lore.add("");
            lore.add(" &fDrops armazenados: &a" + String.format("%.0f", drops) + "&f.");
            lore.add(" &fValor por drop: &a" + NumberFormatter.formatStatic(drop.getPreco()) + " coins&f.");
            lore.add(" &fValor total: &a" + NumberFormatter.formatStatic(valorTotal) + " coins&f.");
            if (bonusRaw > 0) lore.add(" &fSeu bônus: &b" + String.format("%.1f", bonusRaw) + "%&f.");
            lore.add("");
            if (drop.isVender())   lore.add("&aBotão &fESQUERDO&a para vender.");
            if (drop.isRecolher()) lore.add("&aBotão &fDIREITO&a para recolher.");
        }

        Material mat = MaterialCompat.of(drop.getIconeId(), "IRON_INGOT");
        String maquinaId = maq.getId();

        return new ItemBuilder(mat, drop.getIconeData())
                .nome(iconeName)
                .lore(lore)
                .nbt("smaquinas_alldrops_maquina", maquinaId)
                .build();
    }

    private ItemStack buildExtraItem(ConfigurationSection sec) {
        String url   = sec.getString("URL", "");
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", "");
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
        if (action != null) {
            switch (action) {
                case "anterior":
                    if (page > 0) { page--; build(); player.openInventory(inventory); }
                    return;
                case "proximo":
                    page++; build(); player.openInventory(inventory);
                    return;
                case "sell_all":
                    venderTudo(player);
                    return;
            }
        }

        // Clique em máquina individual
        String maquinaId = ItemBuilder.getNBTString(item, "smaquinas_alldrops_maquina");
        if (maquinaId == null) return;

        MaquinaColocada maq = plugin.getMaquinaManager().getMaquinasColocadas().get(maquinaId);
        if (maq == null) { build(); player.openInventory(inventory); return; }

        MaquinaConfig config = plugin.getMaquinaManager().getConfig(maq.getTipoMaquina());
        if (config == null) return;

        MaquinaConfig.DropConfig drop = config.getDropConfig();

        if (maq.getDrops() <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("machine-available-drops"));
            return;
        }

        String botaoVender = drop.getVenderBotao().toUpperCase();
        boolean isLeft     = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
        boolean clickVender = (botaoVender.equals("LEFT") && isLeft)
                || (botaoVender.equals("RIGHT") && !isLeft);

        if (clickVender) {
            // VENDER
            if (!drop.isVender()) return;
            if (!drop.getVenderPerm().isEmpty() && !player.hasPermission(drop.getVenderPerm())) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return;
            }
            venderMaquina(player, maq, config, drop);
        } else {
            // RECOLHER
            if (!drop.isRecolher()) return;
            if (!drop.getRecolherPerm().isEmpty() && !player.hasPermission(drop.getRecolherPerm())) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return;
            }
            coletarMaquina(player, maq, config, drop, maq.getDrops());
        }
    }

    // ── Vender todos os drops de todas as máquinas ──────────────────────────

    private void venderTudo(Player player) {
        boolean mostrarAmigo = plugin.getConfigManager().getConfig()
                .getBoolean("Opcoes.DropsMostrarAmigo", true);

        double totalGanho = 0;
        for (MaquinaColocada maq : new ArrayList<>(plugin.getMaquinaManager().getMaquinasColocadas().values())) {
            if (!maq.getDono().equals(ownerUUID) && !(mostrarAmigo && maq.isAmigo(ownerUUID))) continue;
            MaquinaConfig config = plugin.getMaquinaManager().getConfig(maq.getTipoMaquina());
            if (config == null || maq.getDrops() <= 0) continue;

            MaquinaConfig.DropConfig drop = config.getDropConfig();
            if (!drop.isVender()) continue;
            if (!drop.getVenderPerm().isEmpty() && !player.hasPermission(drop.getVenderPerm())) continue;

            double drops   = maq.getDrops();
            double bonus   = plugin.getBoosterManager().getMultiplicadorVenda(player.getUniqueId());

            for (MaquinaConfig.DropCurrency curr : drop.getCurrencies()) {
                plugin.getEconomiaAPI().depositar(player, drops * curr.getAmount() * bonus, curr.getProvider());
            }
            totalGanho += drops * drop.getPreco() * bonus;
            maq.setDrops(0);
            plugin.getDB().saveMaquina(maq);
            plugin.getHologramManager().updateHologram(maq);
        }

        player.sendMessage(ConfigManager.colorir(
                "&aVocê vendeu todos os drops por &6" + NumberFormatter.formatStatic(totalGanho) + " coins&a."));
        build();
        player.openInventory(inventory);
    }

    // ── Vender drops de uma máquina específica ─────────────────────────────

    private void venderMaquina(Player player, MaquinaColocada maq, MaquinaConfig config, MaquinaConfig.DropConfig drop) {
        double drops = maq.getDrops();
        double bonus = plugin.getBoosterManager().getMultiplicadorVenda(player.getUniqueId());
        double bonusPct = (bonus - 1.0) * 100.0;
        String bonusStr = bonusPct > 0
                ? ConfigManager.colorir("&8(&a+" + String.format("%.0f%%", bonusPct) + "&8)")
                : "";

        for (MaquinaConfig.DropCurrency curr : drop.getCurrencies()) {
            plugin.getEconomiaAPI().depositar(player, drops * curr.getAmount() * bonus, curr.getProvider());
        }

        String msg = config.getMsgVendeu()
                .replace("{quantia}", String.format("%.0f", drops))
                .replace("{drop}",    ConfigManager.colorir(drop.getNome()))
                .replace("{money}",   NumberFormatter.formatStatic(drops * drop.getPreco()))
                .replace("{bonus}",   bonusStr);
        player.sendMessage(ConfigManager.colorir(msg));

        maq.setDrops(0);
        plugin.getDB().saveMaquina(maq);
        plugin.getHologramManager().updateHologram(maq);
        build();
        player.openInventory(inventory);
    }

    // ── Recolher drops de uma máquina específica ───────────────────────────

    private void coletarMaquina(Player player, MaquinaColocada maq, MaquinaConfig config,
                                 MaquinaConfig.DropConfig drop, double quantia) {
        if (drop.isComandoAtivar()) {
            double totalVal = drop.isMultiplicarQuantiaPreco() ? quantia * drop.getPreco() : quantia;
            String qStr     = drop.isFormatarQuantia()
                    ? NumberFormatter.formatStatic(totalVal)
                    : String.format("%.0f", totalVal);
            final String playerName = player.getName();
            for (String cmd : drop.getComandos()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("{player}", playerName).replace("{quantia}", qStr));
            }
        } else {
            Material mat   = MaterialCompat.of(drop.getIconeId(), "STONE");
            int restante   = (int) quantia;
            while (restante > 0) {
                int stack  = Math.min(64, restante);
                ItemStack di = new ItemStack(mat, stack);
                if (!drop.isInvBypass() && player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(plugin.getConfigManager().msg("inv-full"));
                    break;
                }
                player.getInventory().addItem(di);
                restante -= stack;
            }
        }

        player.sendMessage(plugin.getConfigManager().msg("drop-collected",
                "{quantia}", String.format("%.0f", quantia)));

        maq.setDrops(Math.max(0, maq.getDrops() - quantia));
        plugin.getDB().saveMaquina(maq);
        plugin.getHologramManager().updateHologram(maq);
        build();
        player.openInventory(inventory);
    }
}
