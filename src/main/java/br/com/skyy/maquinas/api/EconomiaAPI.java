package br.com.skyy.maquinas.api;

import br.com.skyy.core.SCore;
import br.com.skyy.maquinas.SMaquinas;
import org.bukkit.entity.Player;

/**
 * sMaquinas economy facade — delegates entirely to sCore's EconomyManager.
 * Keeps the original public API so no other class needs to change.
 */
public class EconomiaAPI {

    private final SMaquinas plugin;

    public EconomiaAPI(SMaquinas plugin) {
        this.plugin = plugin;
    }

    /**
     * Called during plugin enable to verify Vault is available.
     * sCore already set up all providers in onLoad/onEnable,
     * so we just check whether "money" provider is registered.
     */
    public boolean setupVault() {
        if (SCore.getEconomy().get("money") == null) {
            plugin.getLogger().severe("[sMaquinas] Vault/economia não encontrada via sCore!");
            return false;
        }
        plugin.getLogger().info("[sMaquinas] Economia integrada via sCore (" +
                SCore.getEconomy().get("money").getName() + ")");
        return true;
    }

    public boolean temDinheiro(Player player, double value, String provider) {
        return SCore.getEconomy().has(player, value, provider);
    }

    public boolean cobrar(Player player, double value, String provider) {
        return SCore.getEconomy().withdraw(player, value, provider);
    }

    public boolean depositar(Player player, double value, String provider) {
        return SCore.getEconomy().deposit(player, value, provider);
    }

    public double getSaldo(Player player) {
        return SCore.getEconomy().getBalance(player, "money");
    }

    public double getSaldo(Player player, String provider) {
        return SCore.getEconomy().getBalance(player, provider);
    }
}
