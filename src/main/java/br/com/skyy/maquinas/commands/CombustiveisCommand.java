package br.com.skyy.maquinas.commands;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.CombustiveisShopMenu;
import br.com.skyy.maquinas.models.CombustivelConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CombustiveisCommand implements CommandExecutor, TabCompleter {

    private final SMaquinas plugin;

    public CombustiveisCommand(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("Apenas jogadores!"); return true; }
            Player player = (Player) sender;
            if (!player.hasPermission("smaquinas.combustivel.usar")) {
                player.sendMessage(plugin.getConfigManager().msg("permission")); return true;
            }
            new CombustiveisShopMenu(plugin, 0).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;

            case "lista":
                if (!sender.hasPermission("smaquinas.combustivel.lista")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                sender.sendMessage(ConfigManager.colorir("&eCombustíveis disponíveis:"));
                for (CombustivelConfig config : plugin.getCombustivelManager().getCombustivelConfigs().values()) {
                    sender.sendMessage(ConfigManager.colorir("&7 - &f" + config.getId() + " &8(&7" + config.getNome() + "&8)"));
                }
                break;

            case "give":
                if (!sender.hasPermission("smaquinas.combustivel.give")) {
                    sender.sendMessage(plugin.getConfigManager().msg("permission")); return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ConfigManager.colorir("&cUso: /combustiveis give <player/all> <tipo> [quantia]")); return true;
                }
                String tipo = args[2];
                int quantia = args.length > 3 ? parseInt(args[3], 1) : 1;
                CombustivelConfig cc = plugin.getCombustivelManager().getConfig(tipo);
                if (cc == null) {
                    sender.sendMessage(ConfigManager.colorir("&cCombustível não encontrado: " + tipo)); return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ItemStack item = plugin.getCombustivelManager().criarItemCombustivelComQuantia(tipo, quantia);
                        if (item != null) p.getInventory().addItem(item);
                    }
                    String nome = sender instanceof Player ? ((Player)sender).getName() : "Console";
                    Bukkit.broadcastMessage(plugin.getConfigManager().msg("give-fuel-all",
                            "{player}", nome, "{quantia}", String.valueOf(quantia),
                            "{combustivel}", ConfigManager.colorir(cc.getNome())));
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) { sender.sendMessage(plugin.getConfigManager().msg("target", "{player}", args[1])); return true; }
                    ItemStack item = plugin.getCombustivelManager().criarItemCombustivelComQuantia(tipo, quantia);
                    if (item != null) target.getInventory().addItem(item);
                    sender.sendMessage(plugin.getConfigManager().msg("give-fuel",
                            "{quantia}", String.valueOf(quantia),
                            "{combustivel}", ConfigManager.colorir(cc.getNome()),
                            "{player}", target.getName()));
                }
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ConfigManager.colorir("&8=== &6Combustíveis Help &8==="));
        sender.sendMessage(ConfigManager.colorir("&e/combustiveis &7- Abre a loja de combustíveis."));
        if (sender.hasPermission("smaquinas.admin")) {
            sender.sendMessage(ConfigManager.colorir("&e/combustiveis lista &7- Lista combustíveis disponíveis."));
            sender.sendMessage(ConfigManager.colorir("&e/combustiveis give <player> <tipo> [qtd] &7- Dá combustível."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help"));
            if (sender.hasPermission("smaquinas.admin")) subs.addAll(Arrays.asList("lista", "give"));
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
            List<String> result = new ArrayList<>(plugin.getCombustivelManager().getCombustivelConfigs().keySet());
            result.removeIf(s -> !s.startsWith(args[2].toLowerCase()));
            return result;
        }
        return Collections.emptyList();
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
