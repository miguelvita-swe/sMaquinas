package br.com.skyy.maquinas.utils;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.core.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final SMaquinas plugin;

    private FileConfiguration config;
    private FileConfiguration mensagens;
    private FileConfiguration maquinas;
    private FileConfiguration combustiveis;
    private FileConfiguration economies;
    private FileConfiguration descontos;
    private FileConfiguration boosters;
    private FileConfiguration bonus;
    private FileConfiguration menuUpgrades;
    private FileConfiguration menuTop;
    private FileConfiguration menuPrincipal;
    private FileConfiguration menuDrops;
    private FileConfiguration menuAmigos;
    private FileConfiguration menuAllDrops;
    private FileConfiguration menuAbastecimentoMassa;
    private FileConfiguration menuShopMaquinas;
    private FileConfiguration menuDropPreview;
    private FileConfiguration menuShopCombustiveis;
    private FileConfiguration shopCombustiveis;
    private FileConfiguration shopMaquinas;

    public ConfigManager(SMaquinas plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        this.config       = plugin.getConfig();
        this.mensagens    = loadConfig("mensagens.yml");
        this.maquinas     = loadConfig("maquinas.yml");
        this.combustiveis = loadConfig("combustiveis.yml");
        this.economies    = loadConfig("economies.yml");
        this.descontos    = loadConfig("descontos.yml");
        this.boosters     = loadConfig("boosters.yml");
        this.bonus        = loadConfig("bonus.yml");
        this.menuUpgrades  = loadConfig("menus/upgrades.yml");
        this.menuTop       = loadConfig("menus/top.yml");
        this.menuPrincipal = loadConfig("menus/principal.yml");
        this.menuDrops     = loadConfig("menus/drops.yml");
        this.menuAmigos    = loadConfig("menus/amigos.yml");
        this.menuAllDrops           = loadConfig("menus/all_drops.yml");
        this.menuAbastecimentoMassa = loadConfig("menus/abastecimento_massa.yml");
        this.menuShopMaquinas = loadConfig("menus/shop/maquinas.yml");
        this.menuDropPreview  = loadConfig("menus/drop_preview.yml");
        this.menuShopCombustiveis = loadConfig("menus/shop/combustiveis.yml");
        this.shopCombustiveis     = loadConfig("shop/combustiveis.yml");
        this.shopMaquinas         = loadConfig("shop/maquinas.yml");
    }

    private FileConfiguration loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig()       { return config; }
    public FileConfiguration getMensagens()    { return mensagens; }
    public FileConfiguration getMaquinas()     { return maquinas; }
    public FileConfiguration getCombustiveis() { return combustiveis; }
    public FileConfiguration getEconomies()    { return economies; }
    public FileConfiguration getDescontos()    { return descontos; }
    public FileConfiguration getBoosters()     { return boosters; }
    public FileConfiguration getBonus()        { return bonus; }
    public FileConfiguration getMenuUpgrades()  { return menuUpgrades; }
    public FileConfiguration getMenuTop()        { return menuTop; }
    public FileConfiguration getMenuPrincipal()  { return menuPrincipal; }
    public FileConfiguration getMenuDrops()      { return menuDrops; }
    public FileConfiguration getMenuAmigos()     { return menuAmigos; }
    public FileConfiguration getMenuAllDrops()             { return menuAllDrops; }
    public FileConfiguration getMenuAbastecimentoMassa()   { return menuAbastecimentoMassa; }
    public FileConfiguration getMenuShopMaquinas()          { return menuShopMaquinas; }
    public FileConfiguration getMenuDropPreview()            { return menuDropPreview; }
    public FileConfiguration getMenuShopCombustiveis()       { return menuShopCombustiveis; }
    public FileConfiguration getShopCombustiveis()            { return shopCombustiveis; }
    public FileConfiguration getShopMaquinas()                { return shopMaquinas; }

    /** Retorna mensagem colorida de chat.<key> */
    public String msg(String key) {
        String raw = mensagens.getString("chat." + key, "&cMensagem não encontrada: chat." + key);
        return colorir(raw);
    }

    /** Retorna mensagem com substituições par a par: key, {placeholder}, valor, ... */
    public String msg(String key, String... replacements) {
        String m = msg(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            m = m.replace(replacements[i], replacements[i + 1]);
        }
        return m;
    }

    public String getPrefix() {
        return colorir(config.getString("prefix", ""));
    }

    public static String colorir(String texto) {
        return ColorUtil.colorize(texto);
    }
}

