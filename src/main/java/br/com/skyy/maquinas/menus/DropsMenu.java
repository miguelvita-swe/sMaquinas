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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DropsMenu extends BaseMenu {

    private final MaquinaColocada maquina;
    private final MaquinaConfig config;

    // Pending collect: UUID → máquina (aguardando digitação no chat)
    private static final Map<UUID, MaquinaColocada> pendingCollect = new HashMap<>();

    public DropsMenu(SMaquinas plugin, MaquinaColocada maquina) {
        super(plugin);
        this.maquina = maquina;
        this.config  = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
    }

    public static Map<UUID, MaquinaColocada> getPendingCollect() { return pendingCollect; }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuDrops();

        // Título
        String titulo = yml.getString("Nome", "&8Drops");
        if (config != null)
            titulo = titulo.replace("{maquina}", ConfigManager.colorir(config.getNome()));

        int tamanho = yml.getInt("Tamanho", 27);
        inventory = createInventory(tamanho, titulo);


        if (config == null) return;

        MaquinaConfig.DropConfig drop = config.getDropConfig();
        double bonus = plugin.getBoosterManager().getMultiplicadorVenda(maquina.getDono());
        double bonusDrop = plugin.getBoosterManager().getMultiplicadorDrop(maquina.getId());
        double bonusRaw  = (bonus - 1.0) * 100.0;

        // ── Item principal do drop (Drop slot) ─────────────────────────
        int dropSlot = yml.getInt("Drop slot", 13);
        if (dropSlot >= 0 && dropSlot < tamanho) {
            inventory.setItem(dropSlot, buildDropItem(drop, bonus, bonusRaw));
        }

        // ── Itens decorativos extras configurados em Itens: ─────────────
        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens != null) {
            for (String key : itens.getKeys(false)) {
                ConfigurationSection sec = itens.getConfigurationSection(key);
                if (sec == null) continue;
                int slot = sec.getInt("Slot", -1);
                if (slot < 0 || slot >= tamanho) continue;
                inventory.setItem(slot, buildExtraItem(sec, key));
            }
        }

        // ── Botão voltar ───────────────────────────────────────────────
        int backSlot = yml.getInt("Backslot", 18);
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

    // ── Monta o item principal do drop ────────────────────────────────────

    private ItemStack buildDropItem(MaquinaConfig.DropConfig drop, double bonus, double bonusRaw) {
        double drops     = maquina.getDrops();
        double valorTotal = drops * drop.getPreco();
        double bolsa     = 100.0; // placeholder — implementar yBolsa se disponível

        List<String> rawLore = drop.getIconeLore();
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            line = line
                .replace("{drops_armazenados}", String.format("%.0f", drops))
                .replace("{money}",              NumberFormatter.formatStatic(valorTotal))
                .replace("{bolsa}",              String.format("%.0f", bolsa))
                .replace("{bonus_raw}",          String.format("%.1f", bonusRaw))
                .replace("{bonus}",              NumberFormatter.formatStatic(bonusRaw) + "%");
            lore.add(line);
        }

        // Se lore não estiver configurada, usar padrão
        if (lore.isEmpty()) {
            lore.add("");
            lore.add(" &fDrops armazenados: &a" + String.format("%.0f", drops) + "&f.");
            lore.add(" &fValor por drop: &a" + NumberFormatter.formatStatic(drop.getPreco()) + " coins&f.");
            lore.add(" &fValor total: &a" + NumberFormatter.formatStatic(valorTotal) + " coins&f.");
            if (bonusRaw > 0) lore.add(" &fSeu bônus: &b" + String.format("%.1f", bonusRaw) + "%&f.");
            lore.add("");
            if (drop.isVender())   lore.add("&aBotão &f" + drop.getVenderBotao()   + "&a para vender.");
            if (drop.isRecolher()) lore.add("&aBotão &f" + drop.getRecolherBotao() + "&a para recolher.");
        }

        Material mat = MaterialCompat.of(drop.getIconeId(), "IRON_INGOT");
        String name  = drop.getIconeName().isEmpty() ? "&f" + drop.getNome() : drop.getIconeName();

        return new ItemBuilder(mat, drop.getIconeData())
                .nome(name)
                .lore(lore)
                .nbt("smaquinas_drops_action", "drop")
                .build();
    }

    // ── Monta itens decorativos extras ────────────────────────────────────

    private ItemStack buildExtraItem(ConfigurationSection sec, String key) {
        String url   = sec.getString("URL", "");
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", "&f" + key);
        List<String> lore = sec.getStringList("Lore");

        Material mat = (!id.equals("AIR") && !id.isEmpty())
                ? MaterialCompat.of(id, "STONE")
                : MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat, data).nome(name).lore(lore).glow(glow);

        if (!url.isEmpty() && url.startsWith("http")) {
            builder.skullTexture(url);
        }
        return builder.build();
    }

    // ── click ─────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        // Botão voltar
        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if ("voltar".equals(action)) {
            openLater(plugin, player, () -> new MaquinaInfoMenu(plugin, maquina));
            return;
        }

        // Item de drop
        String dropsAction = ItemBuilder.getNBTString(item, "smaquinas_drops_action");
        if (!"drop".equals(dropsAction)) return;

        if (!maquina.temAcesso(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
            player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
        }

        if (maquina.getDrops() <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("machine-available-drops")); return;
        }

        MaquinaConfig.DropConfig drop = config.getDropConfig();
        String botaoVender    = drop.getVenderBotao().toUpperCase();
        boolean isLeft        = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
        boolean clickedVender = (botaoVender.equals("LEFT") && isLeft) || (botaoVender.equals("RIGHT") && !isLeft);

        if (clickedVender) {
            // ── VENDER ──
            if (!drop.isVender()) return;
            if (!drop.getVenderPerm().isEmpty() && !player.hasPermission(drop.getVenderPerm())) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return;
            }
            venderDrops(player, drop);

        } else {
            // ── RECOLHER ──
            if (!drop.isRecolher()) return;
            if (!drop.getRecolherPerm().isEmpty() && !player.hasPermission(drop.getRecolherPerm())) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return;
            }

            if (drop.isColetarChatBypass() || drop.isComandoAtivar()) {
                // Coletar direto sem digitar no chat
                coletarDrops(player, drop, maquina.getDrops());
            } else {
                // Pedir quantia no chat
                player.closeInventory();
                pendingCollect.put(player.getUniqueId(), maquina);
                player.sendMessage(plugin.getConfigManager().msg("digit-collect",
                        "{quantia}", String.format("%.0f", maquina.getDrops())));
            }
        }
    }

    // ── Vender todos os drops ─────────────────────────────────────────────

    private void venderDrops(Player player, MaquinaConfig.DropConfig drop) {
        double total = maquina.getDrops();
        double bonus = plugin.getBoosterManager().getMultiplicadorVenda(player.getUniqueId());

        for (MaquinaConfig.DropCurrency curr : drop.getCurrencies()) {
            plugin.getEconomiaAPI().depositar(player, total * curr.getAmount() * bonus, curr.getProvider());
        }

        double bonusPct = (bonus - 1.0) * 100.0;
        String bonusStr = bonusPct > 0
                ? ConfigManager.colorir("&8(&a+" + String.format("%.0f%%", bonusPct) + "&8)")
                : "";

        String msg = config.getMsgVendeu()
                .replace("{quantia}", String.format("%.0f", total))
                .replace("{drop}",    ConfigManager.colorir(drop.getNome()))
                .replace("{money}",   NumberFormatter.formatStatic(total * drop.getPreco()))
                .replace("{bonus}",   bonusStr);
        player.sendMessage(ConfigManager.colorir(msg));

        maquina.setDrops(0);
        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().updateHologram(maquina);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
    }

    // ── Coletar drops (direto ou com quantia do chat) ─────────────────────

    public void coletarDrops(Player player, MaquinaConfig.DropConfig drop, double quantia) {
        if (quantia <= 0 || quantia > maquina.getDrops()) {
            quantia = maquina.getDrops();
        }

        if (drop.isComandoAtivar()) {
            // Executar comandos configurados — deve rodar na thread principal
            double totalVal = drop.isMultiplicarQuantiaPreco() ? quantia * drop.getPreco() : quantia;
            String qStr     = drop.isFormatarQuantia()
                    ? NumberFormatter.formatStatic(totalVal)
                    : String.format("%.0f", totalVal);
            final String playerName = player.getName();
            for (String cmd : drop.getComandos()) {
                final String finalCmd = cmd.replace("{player}", playerName).replace("{quantia}", qStr);
                if (org.bukkit.Bukkit.isPrimaryThread()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                } else {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
                }
            }
        } else {
            // Dropar itens no inventário
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

        maquina.setDrops(Math.max(0, maquina.getDrops() - quantia));
        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().updateHologram(maquina);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
    }
}
