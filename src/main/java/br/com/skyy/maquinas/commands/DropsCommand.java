package br.com.skyy.maquinas.commands;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.AllDropsMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DropsCommand implements CommandExecutor {

    private final SMaquinas plugin;

    public DropsCommand(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores!");
            return true;
        }
        Player player = (Player) sender;
        new AllDropsMenu(plugin, player.getUniqueId()).open(player);
        return true;
    }
}
