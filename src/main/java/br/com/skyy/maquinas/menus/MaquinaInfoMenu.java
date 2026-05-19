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

import java.util.*;

public class MaquinaInfoMenu extends BaseMenu {

    private final MaquinaColocada maquina;
    private final MaquinaConfig config;

    // Pending remove actions: UUID jogador → máquina
    private static final Map<UUID, MaquinaColocada> pendingRemove = new HashMap<>();

    public MaquinaInfoMenu(SMaquinas plugin, MaquinaColocada maquina) {
        super(plugin);
        this.maquina = maquina;
        this.config  = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
    }

    public static Map<UUID, MaquinaColocada> getPendingRemove() { return pendingRemove; }

    public String getMaquinaId() { return maquina.getId(); }

    // ── build ──────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuPrincipal();

        String titulo = yml.getString("Nome", "&8Máquina");
        if (config != null)
            titulo = titulo.replace("{maquina}", ConfigManager.colorir(config.getNome()));

        int tamanho = yml.getInt("Tamanho", 36);
        inventory = createInventory(tamanho, titulo);


        if (config == null) return;

        // Placeholders globais
        PlaceholderContext ctx = new PlaceholderContext();

        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens == null) return;

        for (String key : itens.getKeys(false)) {
            ConfigurationSection sec = itens.getConfigurationSection(key);
            if (sec == null) continue;

            // Itens condicionais — só renderiza o que for pertinente
            if (key.equals("Funcional") && maquina.isQuebrada()) continue;
            if (key.equals("Quebrada")  && !maquina.isQuebrada()) continue;
            if (key.equals("Drops")     && maquina.getDrops() <= 0) continue;
            if (key.equals("Drops-Nao") && maquina.getDrops() > 0) continue;
            if (key.equals("InfinitoRemover")    && !maquina.getCombustivelInfinito()) continue;
            if (key.equals("InfinitoRemoverTem") && maquina.getCombustivelInfinito()) continue;
            if (key.equals("BoosterSim") && !plugin.getBoosterManager().temBoosterDrop(maquina.getId())) continue;
            if (key.equals("BoosterNao") && plugin.getBoosterManager().temBoosterDrop(maquina.getId())) continue;

            int slot = sec.getInt("Slot", -1);
            if (slot < 0 || slot >= tamanho) continue;

            ItemStack item = buildItem(sec, key, ctx);
            if (item != null) inventory.setItem(slot, item);
        }
    }

    // ── item builder ───────────────────────────────────────────────────────

    private ItemStack buildItem(ConfigurationSection sec, String key, PlaceholderContext ctx) {
        String url  = sec.getString("URL", "");
        String id   = sec.getString("ID", "STONE");
        int data    = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name = sec.getString("Name", "&f" + key);
        List<String> lore = sec.getStringList("Lore");

        // Substituir placeholders no nome e lore
        name = applyPlaceholders(name, ctx, key);
        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            processedLore.add(applyPlaceholders(line, ctx, key));
        }

        // Determinar material
        Material mat;
        if (url.equals("{maquina}")) {
            // Usar o material da própria máquina
            mat = MaterialCompat.of(config.getItemId(), "IRON_BLOCK");
            url = null;
        } else if (!id.equals("AIR") && !id.isEmpty()) {
            mat = MaterialCompat.of(id, "STONE");
        } else {
            mat = MaterialCompat.playerHead();
        }

        ItemBuilder builder = new ItemBuilder(mat, data)
                .nome(name)
                .lore(processedLore)
                .glow(glow)
                .nbt("smaquinas_menu_key", key);

        // Skull com URL de textura
        if (url != null && !url.isEmpty() && url.startsWith("http")) {
            builder.skullTexture(url);
        }

        return builder.build();
    }

    // ── placeholders ───────────────────────────────────────────────────────

    private String applyPlaceholders(String text, PlaceholderContext ctx, String key) {
        if (config == null) return ConfigManager.colorir(text);

        double capTotal   = plugin.getMaquinaManager().getCapacidadeTotal(config, maquina);
        int nivelVel      = maquina.getUpgradeVelocidade();
        double velBase    = config.getVelocidade();
        double velReduc   = config.getUpgradeVelocidade().getValorPorLevel() * nivelVel;
        double velFinal   = Math.max(1, velBase - velReduc);

        int nivelDrop     = maquina.getUpgradeDrops();
        double dropsRodada = config.getUpgradeDrops().calcularValorEfetivo(nivelDrop, maquina.getStack());

        // Barra de progresso
        double pct = capTotal > 0 ? (maquina.getCombustivel() / capTotal) * 100 : 0;
        String progressBar = buildProgressBar(pct);

        // Status
        String status;
        if (maquina.isQuebrada())
            status = plugin.getConfigManager().getConfig().getString("Opcoes.QuebradaStatus", "&cQUEBRADA");
        else if (!maquina.isAtivo())
            status = "&ePausada";
        else if (!maquina.getCombustivelInfinito() && maquina.getCombustivel() <= 0)
            status = plugin.getConfigManager().getConfig().getString("Opcoes.SemCombustivelStatus", "&cSEM COMBUSTÍVEL");
        else
            status = "&aAtiva";

        // Holograma status
        String holoStatus = maquina.isHoloAtivo() ? "&aAtivado" : "&cDesativado";

        // Trusteds status
        String trustedsStatus = maquina.isTrusteds() ? "&aAtivado" : "&cDesativado";

        // Dono
        String donoNome = Bukkit.getOfflinePlayer(maquina.getDono()).getName();
        if (donoNome == null) donoNome = "Desconhecido";

        // Infinito
        String infinito = maquina.getCombustivelInfinito() ? "&aSim" : "&cNão";

        // Booster
        double boosterBonus = plugin.getBoosterManager().getBonusDrop(maquina.getId());
        long boosterTempoMs = plugin.getBoosterManager().getTempoRestanteDrop(maquina.getId());
        String boosterTempo = NumberFormatter.formatTime(boosterTempoMs);

        // Conserto
        String conserto = NumberFormatter.formatStatic(config.getConsertarPreco());

        text = text
            .replace("{dono}",             donoNome)
            .replace("{stack}",            String.valueOf(maquina.getStack()))
            .replace("{status}",           ConfigManager.colorir(status))
            .replace("{holograma_status}", ConfigManager.colorir(holoStatus))
            .replace("{trusteds_status}",  ConfigManager.colorir(trustedsStatus))
            .replace("{drops_armazenados}",String.format("%.0f", maquina.getDrops()))
            .replace("{combustivel_tem}",  String.format("%.0f", maquina.getCombustivel()))
            .replace("{capacidade}",       String.format("%.0f", capTotal))
            .replace("{progressbar}",      ConfigManager.colorir(progressBar))
            .replace("{porcentagem}",      String.format("%.0f", pct))
            .replace("{infinito}",         ConfigManager.colorir(infinito))
            .replace("{drops_rodada}",     String.format("%.1f", dropsRodada))
            .replace("{velocidade_upgrade}", String.format("%.0f", velFinal))
            .replace("{conserto}",         conserto)
            .replace("{bonus}",            String.format("%.0f", boosterBonus))
            .replace("{tempo}",            boosterTempo);

        return ConfigManager.colorir(text);
    }

    private String buildProgressBar(double pct) {
        int barQtd   = plugin.getConfigManager().getConfig().getInt("Opcoes.Progress bar.Quantia", 10);
        String sim   = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Cor sim", "&a");
        String nao   = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Cor nao", "&7");
        String simb  = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Simbolo", ":");
        int filled   = (int) Math.round((pct / 100.0) * barQtd);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barQtd; i++) sb.append(i < filled ? sim : nao).append(simb);
        return sb.toString();
    }

    // ── click ──────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;
        String key = ItemBuilder.getNBTString(item, "smaquinas_menu_key");
        if (key == null) return;

        switch (key) {
            case "Upgrades":
                openLater(plugin, player, () -> new UpgradesMenu(plugin, maquina));
                break;

            case "Amigos":
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
                }
                openLater(plugin, player, () -> new AmigosMenu(plugin, maquina));
                break;

            case "Drops":
                if (maquina.getDrops() <= 0) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-available-drops")); return;
                }
                openLater(plugin, player, () -> new DropsMenu(plugin, maquina));
                break;

            case "Quebrada":
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
                }
                consertar(player);
                break;

            case "Opcoes":
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
                }
                if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
                    maquina.setAtivo(!maquina.isAtivo());
                    plugin.getDB().saveMaquina(maquina);
                    plugin.getHologramManager().updateHologram(maquina);
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
                } else {
                    maquina.setHoloAtivo(!maquina.isHoloAtivo());
                    plugin.getDB().saveMaquina(maquina);
                    if (maquina.isHoloAtivo())
                        plugin.getHologramManager().createHologram(maquina);
                    else
                        plugin.getHologramManager().removeHologram(maquina.getId());
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
                }
                break;

            case "Trusteds":
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
                }
                maquina.setTrusteds(!maquina.isTrusteds());
                plugin.getDB().saveMaquina(maquina);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
                break;

            case "InfinitoRemover":
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
                }
                if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
                    maquina.setCombustivelInfinito(false);
                    maquina.setCombustivel(0);
                    plugin.getDB().saveMaquina(maquina);
                    plugin.getHologramManager().updateHologram(maquina);
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
                }
                break;

            case "Remover":
                if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
                }
                if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
                    removerMaquinas(player, maquina.getStack());
                } else {
                    player.closeInventory();
                    pendingRemove.put(player.getUniqueId(), maquina);
                    player.sendMessage(plugin.getConfigManager().msg("digit-remove"));
                }
                break;

            default:
                break;
        }
    }

    // ── lógica de venda/coleta de drops ────────────────────────────────────

    private void coletarOuVender(Player player, ClickType click) {
        MaquinaConfig.DropConfig drop = config.getDropConfig();
        String botaoVender = drop.getVenderBotao().toUpperCase();
        boolean isLeft = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
        boolean vender = (botaoVender.equals("LEFT") && isLeft) || (botaoVender.equals("RIGHT") && !isLeft);

        if (vender && drop.isVender()) {
            if (!drop.getVenderPerm().isEmpty() && !player.hasPermission(drop.getVenderPerm())) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return;
            }
            double total = maquina.getDrops();
            double bonus = plugin.getBoosterManager().getMultiplicadorVenda(player.getUniqueId());
            for (MaquinaConfig.DropCurrency curr : drop.getCurrencies()) {
                plugin.getEconomiaAPI().depositar(player, total * curr.getAmount() * bonus, curr.getProvider());
            }
            String msg = config.getMsgVendeu()
                    .replace("{quantia}", String.format("%.0f", total))
                    .replace("{drop}",    ConfigManager.colorir(drop.getNome()))
                    .replace("{money}",   String.format("%.0f", total * drop.getPreco()))
                    .replace("{bonus}",   bonus > 1 ? "&a+" + String.format("%.0f%%", (bonus-1)*100) : "");
            player.sendMessage(ConfigManager.colorir(msg));
            maquina.setDrops(0);

        } else if (!vender && drop.isRecolher()) {
            if (!drop.getRecolherPerm().isEmpty() && !player.hasPermission(drop.getRecolherPerm())) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return;
            }
            if (drop.isComandoAtivar()) {
                double quantia  = maquina.getDrops();
                double totalVal = drop.isMultiplicarQuantiaPreco() ? quantia * drop.getPreco() : quantia;
                String qStr     = drop.isFormatarQuantia() ? NumberFormatter.formatStatic(totalVal) : String.format("%.0f", totalVal);
                for (String cmd : drop.getComandos()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            cmd.replace("{player}", player.getName()).replace("{quantia}", qStr));
                }
            } else {
                Material mat    = MaterialCompat.of(drop.getIconeId(), "STONE");
                int quantidade  = (int) maquina.getDrops();
                while (quantidade > 0) {
                    int stack   = Math.min(64, quantidade);
                    ItemStack di = new ItemStack(mat, stack);
                    if (!drop.isInvBypass() && player.getInventory().firstEmpty() == -1) {
                        player.sendMessage(plugin.getConfigManager().msg("inv-full")); break;
                    }
                    player.getInventory().addItem(di);
                    quantidade -= stack;
                }
            }
            player.sendMessage(plugin.getConfigManager().msg("drop-collected",
                    "{quantia}", String.format("%.0f", maquina.getDrops())));
            maquina.setDrops(0);
        }

        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().updateHologram(maquina);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
    }

    // ── consertar ──────────────────────────────────────────────────────────

    private void consertar(Player player) {
        double preco = config.getConsertarPreco();
        if (!plugin.getEconomiaAPI().temDinheiro(player, preco, "Money")) {
            player.sendMessage(plugin.getConfigManager().msg("machine-repair",
                    "{money}", NumberFormatter.formatStatic(preco))); return;
        }
        plugin.getEconomiaAPI().cobrar(player, preco, "Money");
        maquina.setQuebrada(false);
        plugin.getDB().saveMaquina(maquina);
        plugin.getHologramManager().updateHologram(maquina);
        player.sendMessage(plugin.getConfigManager().msg("machine-repaired",
                "{money}", NumberFormatter.formatStatic(preco)));
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
    }

    // ── remover máquinas do stack ──────────────────────────────────────────

    public void removerMaquinas(Player player, int quantia) {
        if (quantia <= 0) return;
        if (quantia > maquina.getStack()) {
            player.sendMessage(plugin.getConfigManager().msg("machine-collect-available",
                    "{stack}", String.valueOf(maquina.getStack())));
            return;
        }

        int novoStack = maquina.getStack() - quantia;
        String nomeMaquina = ConfigManager.colorir(config.getNome());

        if (novoStack <= 0) {
            // Remover completamente
            plugin.getMaquinaManager().removerMaquina(maquina);
            if (maquina.getLocation() != null && maquina.getLocation().getBlock() != null) {
                maquina.getLocation().getBlock().setType(Material.AIR);
            }
            player.closeInventory();
        } else {
            maquina.setStack(novoStack);
            plugin.getDB().saveMaquina(maquina);
            plugin.getHologramManager().updateHologram(maquina);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
        }

        // Devolver itens ao inventário
        ItemStack devolver = plugin.getMaquinaManager().criarItemMaquinaComStack(maquina.getTipoMaquina(), quantia);
        if (devolver != null) player.getInventory().addItem(devolver);

        player.sendMessage(plugin.getConfigManager().msg("machine-removed",
                "{quantia}", String.valueOf(quantia),
                "{maquina}", nomeMaquina));
    }

    // ── Holder (ctx vazio, só estrutural) ─────────────────────────────────
    private static class PlaceholderContext {}
}
