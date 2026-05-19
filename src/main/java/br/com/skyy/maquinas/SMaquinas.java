package br.com.skyy.maquinas;

import br.com.skyy.maquinas.api.EconomiaAPI;
import br.com.skyy.maquinas.commands.CombustiveisCommand;
import br.com.skyy.maquinas.commands.DropsCommand;
import br.com.skyy.maquinas.commands.LimiteCommand;
import br.com.skyy.maquinas.commands.MaquinasCommand;
import br.com.skyy.maquinas.database.Database;
import br.com.skyy.maquinas.database.DatabaseMySQL;
import br.com.skyy.maquinas.database.DatabaseSQLite;
import br.com.skyy.maquinas.hooks.HologramManager;
import br.com.skyy.maquinas.hooks.PlaceholderAPIHook;
import br.com.skyy.maquinas.listeners.*;
import br.com.skyy.maquinas.managers.*;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SMaquinas extends JavaPlugin {

    private static SMaquinas instance;

    private ConfigManager configManager;
    private MaquinaManager maquinaManager;
    private CombustivelManager combustivelManager;
    private LimiteManager limiteManager;
    private BoosterManager boosterManager;
    private HologramManager hologramManager;
    private EconomiaAPI economiaAPI;
    private Database database;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("mensagens.yml", false);
        saveResource("maquinas.yml", false);
        saveResource("combustiveis.yml", false);
        saveResource("economies.yml", false);
        saveResource("descontos.yml", false);
        saveResource("boosters.yml", false);
        saveResource("bonus.yml", false);
        saveResource("menus/upgrades.yml", false);
        saveResource("menus/top.yml", false);
        saveResource("menus/principal.yml", false);
        saveResource("menus/amigos.yml", false);
        saveResource("menus/drops.yml", false);
        saveResource("menus/all_drops.yml", false);
        saveResource("menus/abastecimento_massa.yml", false);
        saveResource("menus/shop/maquinas.yml", false);
        saveResource("menus/drop_preview.yml", false);
        saveResource("menus/shop/combustiveis.yml", false);
        saveResource("shop/combustiveis.yml", false);
        saveResource("shop/maquinas.yml", false);

        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        setupDatabase();

        this.economiaAPI = new EconomiaAPI(this);
        if (!economiaAPI.setupVault()) {
            getLogger().severe("Vault não encontrado! Desabilitando plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.maquinaManager = new MaquinaManager(this);
        this.combustivelManager = new CombustivelManager(this);
        this.limiteManager = new LimiteManager(this);
        this.boosterManager = new BoosterManager(this);
        this.hologramManager = new HologramManager(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("PlaceholderAPI detectado e integrado!");
        }

        MaquinasCommand    maquinasCmd    = new MaquinasCommand(this);
        CombustiveisCommand combustiveisCmd = new CombustiveisCommand(this);
        LimiteCommand      limiteCmd       = new LimiteCommand(this);
        DropsCommand       dropsCmd        = new DropsCommand(this);

        getCommand("maquinas").setExecutor(maquinasCmd);
        getCommand("maquinas").setTabCompleter(maquinasCmd);
        getCommand("combustiveis").setExecutor(combustiveisCmd);
        getCommand("combustiveis").setTabCompleter(combustiveisCmd);
        getCommand("limite").setExecutor(limiteCmd);
        getCommand("limite").setTabCompleter(limiteCmd);
        getCommand("drops").setExecutor(dropsCmd);

        // Aplicar aliases dinâmicas da config.yml
        registerDynamicAliases();

        getServer().getPluginManager().registerEvents(new MaquinaPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new MaquinaBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new MaquinaInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new CombustivelListener(this), this);
        getServer().getPluginManager().registerEvents(new LimiteItemListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BoosterListener(this), this);

        // login-delay: recarregar dados do jogador com delay ao logar
        int loginDelay = getConfig().getInt("login-delay", 20);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                org.bukkit.entity.Player p = event.getPlayer();
                Bukkit.getScheduler().runTaskLaterAsynchronously(SMaquinas.this, () -> {
                    limiteManager.reloadPlayer(p.getUniqueId());
                }, loginDelay);
            }
        }, this);

        maquinaManager.startMaquinasTask();

        // MaquinaClearTask: limpar máquinas sem bloco físico após 5s
        if (getConfig().getBoolean("Opcoes.MaquinaClearTask", true)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                int removidos = 0;
                for (java.util.Iterator<MaquinaColocada> it =
                     maquinaManager.getMaquinasColocadas().values().iterator(); it.hasNext(); ) {
                    MaquinaColocada m = it.next();
                    org.bukkit.Location loc = m.getLocation();
                    if (loc == null || loc.getWorld() == null) { it.remove(); database.deleteMaquina(m.getId()); removidos++; continue; }
                    if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
                    org.bukkit.Material tipo = loc.getBlock().getType();
                    if (tipo == org.bukkit.Material.AIR) { it.remove(); database.deleteMaquina(m.getId()); removidos++; }
                }
                if (removidos > 0) getLogger().info("[sMaquinas] MaquinaClearTask removeu " + removidos + " máquina(s) fantasma.");
            }, 100L); // 5 segundos
        }

        // HologramaClearTask: recriar hologramas periodicamente
        if (getConfig().getBoolean("Opcoes.HologramaClearTask.Ativar", true)) {
            int delaySegundos = getConfig().getInt("Opcoes.HologramaClearTask.Delay", 30);
            long delayTicks = delaySegundos * 20L;
            Bukkit.getScheduler().runTaskTimer(this, () -> hologramManager.reloadHolograms(), delayTicks, delayTicks);
        }

        getLogger().info("sMaquinas v" + getDescription().getVersion() + " habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (maquinaManager != null) maquinaManager.saveAllMaquinas();
        if (hologramManager != null) hologramManager.removeAllHolograms();
        if (database != null) database.close();
        getLogger().info("sMaquinas desabilitado.");
    }

    /**
     * Lê os aliases da config.yml e os aplica nos PluginCommands em tempo de execução.
     * Também registra no SimpleCommandMap via reflection para que os aliases funcionem
     * mesmo sendo diferentes dos declarados no plugin.yml.
     */
    private void registerDynamicAliases() {
        org.bukkit.command.SimpleCommandMap commandMap = getCommandMap();

        String[][] mapping = {
            { "Comando.Maquina",     "maquinas"     },
            { "Comando.Combustivel", "combustiveis" },
            { "Comando.Limite",      "limite"        },
            { "Comando.Drops",       "drops"         }
        };

        for (String[] entry : mapping) {
            String configPath = entry[0];
            String cmdName    = entry[1];

            java.util.List<String> aliases = getConfig().getStringList(configPath + ".Aliases");
            if (aliases.isEmpty()) continue;

            org.bukkit.command.PluginCommand cmd = getCommand(cmdName);
            if (cmd == null) continue;

            // 1. Atualizar a lista de aliases no PluginCommand
            cmd.setAliases(aliases);

            // 2. Registrar cada alias diretamente no CommandMap via reflection
            if (commandMap != null) {
                for (String alias : aliases) {
                    if (alias == null || alias.isEmpty()) continue;
                    try {
                        commandMap.register(alias, getName().toLowerCase(), cmd);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /** Obtém o SimpleCommandMap do servidor via reflection independente de versão. */
    private org.bukkit.command.SimpleCommandMap getCommandMap() {
        try {
            java.lang.reflect.Field f = Bukkit.getServer().getClass()
                    .getDeclaredField("commandMap");
            f.setAccessible(true);
            return (org.bukkit.command.SimpleCommandMap) f.get(Bukkit.getServer());
        } catch (Exception e) {
            // Tentar na superclasse (CraftServer herda de JavaPlugin em algumas versões)
            try {
                java.lang.reflect.Field f = Bukkit.getServer().getClass()
                        .getSuperclass().getDeclaredField("commandMap");
                f.setAccessible(true);
                return (org.bukkit.command.SimpleCommandMap) f.get(Bukkit.getServer());
            } catch (Exception ex) {
                getLogger().warning("[sMaquinas] Não foi possível acessar o CommandMap: " + ex.getMessage());
                return null;
            }
        }
    }

    private void setupDatabase() {
        String tipo = getConfig().getString("Database.Tipo", "SQLITE").toUpperCase();
        if (tipo.equals("MYSQL")) {
            this.database = new DatabaseMySQL(this);
        } else {
            this.database = new DatabaseSQLite(this);
        }
        this.database.initialize();
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        maquinaManager.reload();
        combustivelManager.reload();
        limiteManager.reload();
        boosterManager.reload();
        hologramManager.reloadHolograms();
        registerDynamicAliases();
    }

    public static SMaquinas getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MaquinaManager getMaquinaManager() { return maquinaManager; }
    public CombustivelManager getCombustivelManager() { return combustivelManager; }
    public LimiteManager getLimiteManager() { return limiteManager; }
    public BoosterManager getBoosterManager() { return boosterManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public EconomiaAPI getEconomiaAPI() { return economiaAPI; }
    public Database getDB() { return database; }
}

