package br.com.skyy.maquinas.hooks;

import br.com.skyy.core.SCore;
import br.com.skyy.core.utils.ColorUtil;
import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Delegates hologram creation/update/removal entirely to sCore's HologramProvider.
 * Supports DecentHolograms → HolographicDisplays → None, all versions 1.8–1.21.
 */
public class HologramManager {

    private final SMaquinas plugin;

    public HologramManager(SMaquinas plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[sMaquinas] Holograma via sCore: "
                + SCore.getHologram().getProviderName());
    }

    // ── API pública ─────────────────────────────────────────────────────────

    public void createHologram(MaquinaColocada maquina) {
        if (!isEnabled()) return;
        MaquinaConfig config = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
        if (config == null || !maquina.isHoloAtivo()) return;
        Location loc = maquina.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        Location holoLoc = loc.clone().add(0.5, config.getHologramaAltura(), 0.5);
        List<String> linhas = buildLinhas(maquina, config);

        // Check if last line is [item] for item display
        Material itemMat = extractItemMaterial(linhas);
        if (itemMat != null) {
            SCore.getHologram().createHologramWithItem(holoId(maquina), holoLoc, textLines(linhas), itemMat);
        } else {
            SCore.getHologram().createHologram(holoId(maquina), holoLoc, linhas);
        }
    }

    public void updateHologram(MaquinaColocada maquina) {
        if (!isEnabled()) return;
        MaquinaConfig config = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
        if (config == null) return;
        List<String> linhas = buildLinhas(maquina, config);
        SCore.getHologram().updateHologram(holoId(maquina), linhas);
    }

    public void removeHologram(String maquinaId) {
        if (!isEnabled()) return;
        SCore.getHologram().removeHologram("smq_" + maquinaId.replace("-", ""));
    }

    public void removeAllHolograms() {
        // sCore keeps its own map — calling removeAll would affect all plugins.
        // Instead, iterate known machines.
        for (MaquinaColocada m : plugin.getMaquinaManager().getMaquinasColocadas().values())
            removeHologram(m.getId());
    }

    public void reloadHolograms() {
        removeAllHolograms();
        for (MaquinaColocada m : plugin.getMaquinaManager().getMaquinasColocadas().values())
            createHologram(m);
    }

    public boolean isEnabled() {
        if (!plugin.getConfigManager().getConfig().getBoolean("Opcoes.Holograma", true)) return false;
        return SCore.getHologram().isAvailable();
    }

    public String getProviderName() {
        return SCore.getHologram().getProviderName();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String holoId(MaquinaColocada maquina) {
        return "smq_" + maquina.getId().replace("-", "");
    }

    /** Extract [item]MATERIAL:data line and return its Material, or null. */
    private Material extractItemMaterial(List<String> linhas) {
        for (String l : linhas) {
            if (l.startsWith("[item]")) {
                String matName = l.substring(6).split(":")[0].trim();
                return SCore.getMaterial().get(matName, "STONE");
            }
        }
        return null;
    }

    /** Returns lines without any [item] entry (text-only). */
    private List<String> textLines(List<String> linhas) {
        List<String> result = new ArrayList<>();
        for (String l : linhas) {
            if (!l.startsWith("[item]")) result.add(l);
        }
        return result;
    }

    private List<String> buildLinhas(MaquinaColocada maquina, MaquinaConfig config) {
        double capTotal    = plugin.getMaquinaManager().getCapacidadeTotal(config, maquina);
        double porcentagem = capTotal > 0 ? (maquina.getCombustivel() / capTotal) * 100 : 0;
        String donoNome    = Bukkit.getOfflinePlayer(maquina.getDono()).getName();
        if (donoNome == null) donoNome = "Desconhecido";

        String status;
        if (maquina.isQuebrada()) {
            status = plugin.getConfigManager().getConfig().getString("Opcoes.QuebradaStatus", "&cQUEBRADA");
        } else if (!maquina.getCombustivelInfinito() && maquina.getCombustivel() <= 0) {
            status = plugin.getConfigManager().getConfig().getString("Opcoes.SemCombustivelStatus", "&cSEM COMBUSTÍVEL");
        } else {
            status = "&aATIVA";
        }

        int    barQtd  = plugin.getConfigManager().getConfig().getInt("Opcoes.Progress bar.Quantia", 10);
        String barSim  = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Cor sim", "&a");
        String barNao  = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Cor nao", "&7");
        String barSimb = plugin.getConfigManager().getConfig().getString("Opcoes.Progress bar.Simbolo", ":");
        int filled = (int) Math.round((porcentagem / 100.0) * barQtd);
        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < barQtd; i++)
            progressBar.append(i < filled ? barSim : barNao).append(barSimb);

        List<String> result = new ArrayList<>();
        for (String linha : config.getHologramaLinhas()) {
            if (linha.startsWith("[item]")) {
                result.add(linha); // passamos raw para extractItemMaterial
                continue;
            }
            linha = linha
                    .replace("{dono}",           donoNome)
                    .replace("{quantia}",         String.valueOf(maquina.getStack()))
                    .replace("{drops}",           String.format("%.0f", maquina.getDrops()))
                    .replace("{combustivel_tem}", String.format("%.0f", maquina.getCombustivel()))
                    .replace("{capacidade}",      String.format("%.0f", capTotal))
                    .replace("{porcentagem}",     String.format("%.0f", porcentagem))
                    .replace("{progressbar}",     progressBar.toString())
                    .replace("{status}",          ColorUtil.colorize(status));
            result.add(linha);
        }
        return result;
    }
}
