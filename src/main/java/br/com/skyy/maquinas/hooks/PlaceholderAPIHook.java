package br.com.skyy.maquinas.hooks;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.utils.NumberFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final SMaquinas plugin;

    public PlaceholderAPIHook(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "smaquinas"; }

    @Override
    public String getAuthor() { return "Skyy"; }

    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        double limite = plugin.getLimiteManager().getLimite(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "limite":
                return NumberFormatter.formatStatic(limite);
            case "limite_raw":
                return String.valueOf(limite);
            case "maquinas_colocadas":
                return String.valueOf(plugin.getMaquinaManager().getMaquinasColocadasPorJogador(player.getUniqueId()));
            case "limite_total":
                return String.valueOf(plugin.getLimiteManager().getLimiteTotal(player));
            default:
                return null;
        }
    }
}

