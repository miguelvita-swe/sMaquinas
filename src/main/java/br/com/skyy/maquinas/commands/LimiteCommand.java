package br.com.skyy.maquinas.commands;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.TopLimiteMenu;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LimiteCommand implements CommandExecutor, TabCompleter {

    private final SMaquinas plugin;

    public LimiteCommand(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
            Player player = (Player) sender;
            if (!player.hasPermission("smaquinas.limite.usar")) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return true;
            }
            double limite = plugin.getLimiteManager().getLimite(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().msg("limit-player", "{limite}", NumberFormatter.formatStatic(limite)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;

            case "top":
                if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
                if (!sender.hasPermission("smaquinas.limite.usar")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                new TopLimiteMenu(plugin).open((Player) sender);
                break;

            case "add":
                if (!sender.hasPermission("smaquinas.limite.add")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) { sender.sendMessage(ConfigManager.colorir("&cUso: /limite add <player> <quantia>")); return true; }
                handleAddRemove(sender, args, true);
                break;

            case "remove":
                if (!sender.hasPermission("smaquinas.limite.remove")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) { sender.sendMessage(ConfigManager.colorir("&cUso: /limite remove <player> <quantia>")); return true; }
                handleAddRemove(sender, args, false);
                break;

            case "set":
                if (!sender.hasPermission("smaquinas.limite.set")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) { sender.sendMessage(ConfigManager.colorir("&cUso: /limite set <player> <quantia>")); return true; }
                handleSet(sender, args);
                break;

            case "give":
                if (!sender.hasPermission("smaquinas.limite.give")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) { sender.sendMessage(ConfigManager.colorir("&cUso: /limite give <player> <quantia>")); return true; }
                handleGive(sender, args);
                break;

            case "enviar":
                if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
                if (!sender.hasPermission("smaquinas.limite.enviar")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) { sender.sendMessage(ConfigManager.colorir("&cUso: /limite enviar <player> <quantia>")); return true; }
                handleEnviar((Player) sender, args);
                break;

            default:
                if (sender.hasPermission("smaquinas.limite.outros")) {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                    if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                        sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[0])); return true;
                    }
                    double limite = plugin.getLimiteManager().getLimite(target.getUniqueId());
                    sender.sendMessage(plugin.getConfigManager().msg("limit-target",
                            "{player}", target.getName() != null ? target.getName() : args[0],
                            "{limite}", NumberFormatter.formatStatic(limite)));
                } else {
                    sendHelp(sender);
                }
                break;
        }
        return true;
    }

    private void handleAddRemove(CommandSender sender, String[] args, boolean add) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return;
        }
        double quantia = parseDouble(args[2], 0);
        if (quantia <= 0) { sender.sendMessage(ConfigManager.colorir("&cQuantia inválida.")); return; }

        if (add) plugin.getLimiteManager().addLimite(target.getUniqueId(), quantia);
        else plugin.getLimiteManager().removeLimite(target.getUniqueId(), quantia);

        sender.sendMessage(plugin.getConfigManager().msg("limit-changed",
                "{quantia}", NumberFormatter.formatStatic(plugin.getLimiteManager().getLimite(target.getUniqueId())),
                "{player}", target.getName() != null ? target.getName() : args[1]));
    }

    private void handleSet(CommandSender sender, String[] args) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return;
        }
        double quantia = parseDouble(args[2], 0);
        plugin.getLimiteManager().setLimite(target.getUniqueId(), quantia);
        sender.sendMessage(plugin.getConfigManager().msg("limit-changed",
                "{quantia}", NumberFormatter.formatStatic(quantia),
                "{player}", target.getName() != null ? target.getName() : args[1]));
    }

    private void handleGive(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return;
        }
        double quantia = parseDouble(args[2], 0);
        if (quantia <= 0) { sender.sendMessage(ConfigManager.colorir("&cQuantia inválida.")); return; }

        ItemStack item = plugin.getLimiteManager().criarItemLimite(quantia);
        if (target.getInventory().firstEmpty() == -1) {
            sender.sendMessage(plugin.getConfigManager().msg("inv-full")); return;
        }
        target.getInventory().addItem(item);
        sender.sendMessage(plugin.getConfigManager().msg("limit-give",
                "{quantia}", NumberFormatter.formatStatic(quantia),
                "{player}", target.getName()));
    }

    private void handleEnviar(Player sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return;
        }
        if (target.equals(sender)) {
            sender.sendMessage(plugin.getConfigManager().msg("yourself")); return;
        }
        double quantia = parseDouble(args[2], 0);
        if (quantia <= 0) { sender.sendMessage(ConfigManager.colorir("&cQuantia inválida.")); return; }

        if (!plugin.getLimiteManager().temLimiteSuficiente(sender.getUniqueId(), quantia)) {
            sender.sendMessage(plugin.getConfigManager().msg("limit-has",
                    "{limite}", NumberFormatter.formatStatic(quantia))); return;
        }

        // Verificar máximo do target
        int maxLimite = plugin.getConfigManager().getConfig().getInt("Limite.Max", 0);
        if (maxLimite > 0) {
            double targetLimite = plugin.getLimiteManager().getLimite(target.getUniqueId());
            if (targetLimite + quantia > maxLimite) {
                sender.sendMessage(plugin.getConfigManager().msg("limit-target-max",
                        "{limite}", NumberFormatter.formatStatic(maxLimite))); return;
            }
        }

        plugin.getLimiteManager().removeLimite(sender.getUniqueId(), quantia);
        plugin.getLimiteManager().addLimite(target.getUniqueId(), quantia);

        sender.sendMessage(plugin.getConfigManager().msg("limit-sent",
                "{limite}", NumberFormatter.formatStatic(quantia), "{player}", target.getName()));
        target.sendMessage(plugin.getConfigManager().msg("limit-received",
                "{limite}", NumberFormatter.formatStatic(quantia), "{player}", sender.getName()));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ConfigManager.colorir("&8=== &6Limite Help &8==="));
        sender.sendMessage(ConfigManager.colorir("&e/limite &7- Mostra seu limite."));
        sender.sendMessage(ConfigManager.colorir("&e/limite top &7- Abre o menu de top limite."));
        sender.sendMessage(ConfigManager.colorir("&e/limite [player] &7- Mostra o limite de outro jogador."));
        sender.sendMessage(ConfigManager.colorir("&e/limite enviar <player> <qtd> &7- Envia limite."));
        if (sender.hasPermission("smaquinas.admin")) {
            sender.sendMessage(ConfigManager.colorir("&e/limite add <player> <qtd> &7- Adiciona limite."));
            sender.sendMessage(ConfigManager.colorir("&e/limite remove <player> <qtd> &7- Remove limite."));
            sender.sendMessage(ConfigManager.colorir("&e/limite set <player> <qtd> &7- Seta o limite."));
            sender.sendMessage(ConfigManager.colorir("&e/limite give <player> <qtd> &7- Dá item de limite."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("top", "help", "enviar"));
            if (sender.hasPermission("smaquinas.admin")) subs.addAll(Arrays.asList("add", "remove", "set", "give"));
            List<String> result = new ArrayList<>();
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) result.add(s);
            return result;
        }
        if (args.length == 2) {
            List<String> result = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) result.add(p.getName());
            return result;
        }
        return Collections.emptyList();
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
}

