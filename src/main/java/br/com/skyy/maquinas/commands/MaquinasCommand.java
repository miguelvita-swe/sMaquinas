package br.com.skyy.maquinas.commands;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.MaquinasShopMenu;
import br.com.skyy.maquinas.menus.TopLimiteMenu;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MaquinasCommand implements CommandExecutor, TabCompleter {

    private final SMaquinas plugin;

    public MaquinasCommand(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            // Abrir shop
            if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
            Player player = (Player) sender;
            if (!player.hasPermission("smaquinas.maquina.usar")) {
                player.sendMessage(plugin.getConfigManager().msg("permission"));
                return true;
            }
            new MaquinasShopMenu(plugin, 0).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "top":
                if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
                Player topPlayer = (Player) sender;
                if (!topPlayer.hasPermission("smaquinas.maquina.usar")) {
                    topPlayer.sendMessage(plugin.getConfigManager().msg("permission"));
                    return true;
                }
                new TopLimiteMenu(plugin).open(topPlayer);
                break;

            case "help":
                sendHelp(sender);
                break;

            case "booster":
                if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
                Player bPlayer = (Player) sender;
                if (!bPlayer.hasPermission("smaquinas.maquina.booster")) {
                    bPlayer.sendMessage(plugin.getConfigManager().msg("permission"));
                    return true;
                }
                if (!plugin.getBoosterManager().temBooster(bPlayer.getUniqueId())) {
                    bPlayer.sendMessage(plugin.getConfigManager().msg("booster-none"));
                } else {
                    br.com.skyy.maquinas.models.BoosterData booster = plugin.getBoosterManager().getBooster(bPlayer.getUniqueId());
                    bPlayer.sendMessage(plugin.getConfigManager().msg("booster-using",
                            "{bonus}", String.format("%.0f", booster.getMultiplicador()),
                            "{tempo}", NumberFormatter.formatTime(booster.getTempoRestante())));
                }
                break;

            case "lista":
                if (!sender.hasPermission("smaquinas.maquina.lista")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission"));
                    return true;
                }
                sender.sendMessage(ConfigManager.colorir("&eMáquinas disponíveis:"));
                for (MaquinaConfig config : plugin.getMaquinaManager().getMaquinaConfigs().values()) {
                    sender.sendMessage(ConfigManager.colorir("&7 - &f" + config.getId() + " &8(&7" + config.getNome() + "&8)"));
                }
                break;

            case "give":
                if (!sender.hasPermission("smaquinas.maquina.give")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ConfigManager.colorir("&cUso: /maquinas give <player/all> <tipo> [quantia]")); return true;
                }
                String tipoGive = args[2];
                int quantiaGive = args.length > 3 ? parseInt(args[3], 1) : 1;
                MaquinaConfig mcGive = plugin.getMaquinaManager().getConfig(tipoGive);
                if (mcGive == null) {
                    sender.sendMessage(ConfigManager.colorir("&cMáquina não encontrada: " + tipoGive)); return true;
                }
                ItemStack itemMaquina = plugin.getMaquinaManager().criarItemMaquinaComStack(tipoGive, quantiaGive);
                if (args[1].equalsIgnoreCase("all")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.getInventory().addItem(itemMaquina.clone());
                    }
                    String nome = sender instanceof Player ? ((Player)sender).getName() : "Console";
                    Bukkit.broadcastMessage(plugin.getConfigManager().msg("give-all",
                            "{player}", nome, "{quantia}", String.valueOf(quantiaGive),
                            "{maquina}", ConfigManager.colorir(mcGive.getNome())));
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) { sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return true; }
                    target.getInventory().addItem(itemMaquina);
                    sender.sendMessage(plugin.getConfigManager().msg("give",
                            "{quantia}", String.valueOf(quantiaGive),
                            "{maquina}", ConfigManager.colorir(mcGive.getNome()),
                            "{player}", target.getName()));
                }
                break;

            case "givebooster":
                if (!sender.hasPermission("smaquinas.maquina.givebooster")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ConfigManager.colorir("&cUso: /maquinas givebooster <player> <mult> <segundos>")); return true;
                }
                Player bTarget = Bukkit.getPlayer(args[1]);
                if (bTarget == null) { sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return true; }
                double mult = parseDouble(args[2], 1.0);
                long durMs = parseInt(args[3], 60) * 1000L;
                ItemStack boosterItem = plugin.getBoosterManager().criarItemBoosterVenda(mult, durMs);
                bTarget.getInventory().addItem(boosterItem);
                sender.sendMessage(plugin.getConfigManager().msg("booster-give", "{quantia}", "1", "{player}", bTarget.getName()));
                bTarget.sendMessage(plugin.getConfigManager().msg("booster-received", "{quantia}", "1"));
                break;

            case "giveboosterdrop":
                if (!sender.hasPermission("smaquinas.maquina.giveboosterdrop")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ConfigManager.colorir("&cUso: /maquinas giveboosterdrop <player> <mult> <segundos>")); return true;
                }
                Player bdTarget = Bukkit.getPlayer(args[1]);
                if (bdTarget == null) { sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return true; }
                double multDrop = parseDouble(args[2], 1.0);
                long durDropMs = parseInt(args[3], 60) * 1000L;
                ItemStack boosterDropItem = plugin.getBoosterManager().criarItemBoosterDrop(multDrop, durDropMs);
                bdTarget.getInventory().addItem(boosterDropItem);
                sender.sendMessage(plugin.getConfigManager().msg("booster-drop-give", "{quantia}", "1", "{player}", bdTarget.getName()));
                bdTarget.sendMessage(plugin.getConfigManager().msg("booster-drop-received", "{quantia}", "1"));
                break;

            case "reload":
                if (!sender.hasPermission("smaquinas.maquina.reload")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                plugin.reload();
                sender.sendMessage(ConfigManager.colorir("&asMaquinas recarregado com sucesso!"));
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ConfigManager.colorir("&8=== &6sMaquinas Help &8==="));
        sender.sendMessage(ConfigManager.colorir("&e/maquinas &7- Abre a loja de máquinas."));
        sender.sendMessage(ConfigManager.colorir("&e/maquinas top &7- Abre o menu de top."));
        sender.sendMessage(ConfigManager.colorir("&e/maquinas booster &7- Vê o tempo do booster."));
        if (sender.hasPermission("smaquinas.admin")) {
            sender.sendMessage(ConfigManager.colorir("&e/maquinas lista &7- Lista máquinas disponíveis."));
            sender.sendMessage(ConfigManager.colorir("&e/maquinas give <player> <tipo> [qtd] &7- Dá máquina."));
            sender.sendMessage(ConfigManager.colorir("&e/maquinas givebooster <player> <mult> <seg> &7- Dá booster."));
            sender.sendMessage(ConfigManager.colorir("&e/maquinas reload &7- Recarrega configs."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("top", "help", "booster"));
            if (sender.hasPermission("smaquinas.admin")) subs.addAll(Arrays.asList("lista", "give", "givebooster", "giveboosterdrop", "reload"));
            List<String> result = new ArrayList<>();
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) result.add(s);
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> result = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) result.add(p.getName());
            return result;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> result = new ArrayList<>(plugin.getMaquinaManager().getMaquinaConfigs().keySet());
            result.removeIf(s -> !s.startsWith(args[2].toLowerCase()));
            return result;
        }
        return Collections.emptyList();
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
}

