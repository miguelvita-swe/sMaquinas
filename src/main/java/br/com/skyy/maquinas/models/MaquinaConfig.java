package br.com.skyy.maquinas.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class MaquinaConfig {

    private final String id;
    private final String nome;
    private final String permissao;
    private final int stackMax;
    private final int limiteStackPadrao;
    private final int velocidade;
    private final double quebrarChance;
    private final double consertarPreco;
    private final int combustivelOnda;
    private final boolean gastarApenasOff;
    private final List<String> combustiveisAceitos;
    private final String combustivelInfinito;
    private final boolean customSkull;
    private final String skullUrl;
    private final String itemId;
    private final int itemData;
    private final String itemName;
    private final List<String> itemLore;
    private final double hologramaAltura;
    private final List<String> hologramaLinhas;
    private final UpgradeConfig upgradeCapacidade;
    private final UpgradeConfig upgradeDrops;
    private final UpgradeConfig upgradeVelocidade;
    private final UpgradeConfig upgradeDurabilidade;
    private final DropConfig dropConfig;
    private final String msgVendeu;

    public MaquinaConfig(String id, ConfigurationSection s) {
        this.id = id;
        this.nome = s.getString("Nome", "&fMáquina");
        this.permissao = s.getString("Permissao", "");
        this.stackMax = s.getInt("StackMax", 1);
        this.limiteStackPadrao = s.getInt("LimiteStackPadrao", 0);
        this.velocidade = s.getInt("Velocidade", 10);
        this.quebrarChance = s.getDouble("QuebrarChance", 0.0);
        this.consertarPreco = s.getDouble("ConsertarPreco", 1000.0);
        this.combustivelOnda = s.getInt("CombustivelOnda", 10);
        this.gastarApenasOff = s.getBoolean("GastarApenasOff", false);
        this.combustiveisAceitos = s.getStringList("Combustiveis");
        this.combustivelInfinito = s.getString("CombustivelInfinito", "");

        ConfigurationSection item = s.getConfigurationSection("Item");
        if (item != null) {
            customSkull = item.getBoolean("CustomSkull", false);
            skullUrl = item.getString("URL", "");
            itemId = item.getString("ID", "IRON_BLOCK");
            itemData = item.getInt("Data", 0);
            itemName = item.getString("Name", nome);
            itemLore = item.getStringList("Lore");
        } else {
            customSkull = false; skullUrl = ""; itemId = "IRON_BLOCK"; itemData = 0; itemName = nome; itemLore = new ArrayList<>();
        }

        ConfigurationSection holo = s.getConfigurationSection("Holograma");
        if (holo != null) {
            hologramaAltura = holo.getDouble("Altura", 3.5);
            hologramaLinhas = holo.getStringList("Holograma");
        } else { hologramaAltura = 3.5; hologramaLinhas = new ArrayList<>(); }

        ConfigurationSection upgs = s.getConfigurationSection("Upgrades");
        upgradeCapacidade   = new UpgradeConfig(upgs != null ? upgs.getConfigurationSection("Capacidade")   : null, "add");
        upgradeDrops        = new UpgradeConfig(upgs != null ? upgs.getConfigurationSection("Drops")        : null, "add");
        upgradeVelocidade   = new UpgradeConfig(upgs != null ? upgs.getConfigurationSection("Velocidade")   : null, "remove");
        upgradeDurabilidade = new UpgradeConfig(upgs != null ? upgs.getConfigurationSection("Durabilidade") : null, "remove");

        dropConfig = new DropConfig(s.getConfigurationSection("Drop"));
        msgVendeu = s.getString("Mensagens.Vendeu", "&aVocê vendeu &7{quantia}x {drop}&a por &6{money} coins&a.");
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getPermissao() { return permissao; }
    public int getStackMax() { return stackMax; }
    public int getLimiteStackPadrao() { return limiteStackPadrao; }
    public int getVelocidade() { return velocidade; }
    public double getQuebrarChance() { return quebrarChance; }
    public double getConsertarPreco() { return consertarPreco; }
    public int getCombustivelOnda() { return combustivelOnda; }
    public boolean isGastarApenasOff() { return gastarApenasOff; }
    public List<String> getCombustiveisAceitos() { return combustiveisAceitos; }
    public String getCombustivelInfinito() { return combustivelInfinito; }
    public boolean isCustomSkull() { return customSkull; }
    public String getSkullUrl() { return skullUrl; }
    public String getItemId() { return itemId; }
    public int getItemData() { return itemData; }
    public String getItemName() { return itemName; }
    public List<String> getItemLore() { return itemLore; }
    public double getHologramaAltura() { return hologramaAltura; }
    public List<String> getHologramaLinhas() { return hologramaLinhas; }
    public UpgradeConfig getUpgradeCapacidade() { return upgradeCapacidade; }
    public UpgradeConfig getUpgradeDrops() { return upgradeDrops; }
    public UpgradeConfig getUpgradeVelocidade() { return upgradeVelocidade; }
    public UpgradeConfig getUpgradeDurabilidade() { return upgradeDurabilidade; }
    public DropConfig getDropConfig() { return dropConfig; }
    public String getMsgVendeu() { return msgVendeu; }

    // ── Upgrade ───────────────────────────────────────────────────────────────
    public static class UpgradeConfig {
        private final double precoPorLevel;
        private final int maximo;
        private final int padrao;
        private final double valorPorLevel;
        private final boolean multiplicarStack;
        private final List<UpgradePrice> prices;
        private final String tipo;

        public UpgradeConfig(ConfigurationSection s, String tipo) {
            this.tipo = tipo;
            if (s == null) {
                precoPorLevel = 0; maximo = 0; padrao = 0; valorPorLevel = 0;
                multiplicarStack = false; prices = new ArrayList<>(); return;
            }
            precoPorLevel = s.getDouble("PrecoPorLevel", 10.0);
            maximo = s.getInt("Maximo", 5);
            padrao = s.getInt("Padrao", 0);
            multiplicarStack = s.getBoolean("MultiplicarStack", false);
            valorPorLevel = tipo.equals("add") ? s.getDouble("AdicionarPorLevel", 1) : s.getDouble("RemoverPorLevel", 1);
            prices = new ArrayList<>();
            ConfigurationSection ps = s.getConfigurationSection("Prices-PerLevel");
            if (ps != null) {
                for (String key : ps.getKeys(false)) {
                    ConfigurationSection p = ps.getConfigurationSection(key);
                    if (p != null) prices.add(new UpgradePrice(p.getString("provider","Money"), p.getDouble("price",0)));
                }
            }
        }

        public double getPrecoPorLevel() { return precoPorLevel; }
        public int getMaximo() { return maximo; }
        public int getPadrao() { return padrao; }
        public double getValorPorLevel() { return valorPorLevel; }
        public boolean isMultiplicarStack() { return multiplicarStack; }
        public List<UpgradePrice> getPrices() { return prices; }
        public String getTipo() { return tipo; }

        /** Valor efetivo calculado: base + nivel * valorPorLevel (para remove, subtrai da velocidade base) */
        public double calcularValorEfetivo(int nivel, int stack) {
            double base = padrao + (long) nivel * valorPorLevel;
            if (multiplicarStack) base *= stack;
            return base;
        }
    }

    public static class UpgradePrice {
        private final String provider;
        private final double price;
        public UpgradePrice(String provider, double price) { this.provider = provider; this.price = price; }
        public String getProvider() { return provider; }
        public double getPrice() { return price; }
    }

    // ── Drop ─────────────────────────────────────────────────────────────────
    public static class DropConfig {
        private final String nome;
        private final boolean armazem;
        private final boolean recolher;
        private final boolean vender;
        private final String venderBotao;
        private final String recolherBotao;
        private final String recolherPerm;
        private final String venderPerm;
        private final double preco;
        private final List<DropCurrency> currencies;
        private final boolean comandoAtivar;
        private final boolean usarQuantia;
        private final boolean multiplicarQuantiaPreco;
        private final boolean invBypass;
        private final boolean coletarChatBypass;
        private final boolean formatarQuantia;
        private final List<String> comandos;
        private final String iconeId;
        private final int iconeData;
        private final String iconeName;
        private final List<String> iconeLore;

        public DropConfig(ConfigurationSection s) {
            if (s == null) {
                nome = "Drop"; armazem = true; recolher = true; vender = true;
                venderBotao = "LEFT"; recolherBotao = "RIGHT"; recolherPerm = ""; venderPerm = "";
                preco = 100; currencies = new ArrayList<>(); comandoAtivar = false;
                usarQuantia = true; multiplicarQuantiaPreco = true; invBypass = true;
                coletarChatBypass = true; formatarQuantia = true; comandos = new ArrayList<>();
                iconeId = "IRON_INGOT"; iconeData = 0; iconeName = "Drop"; iconeLore = new ArrayList<>(); return;
            }
            nome = s.getString("Nome", "Drop");
            armazem = s.getBoolean("Armazem", true);
            recolher = s.getBoolean("Recolher", true);
            vender = s.getBoolean("Vender", true);
            venderBotao = s.getString("Vender botao", "LEFT");
            recolherBotao = s.getString("Recolher botao", "RIGHT");
            recolherPerm = s.getString("Recolher perm", "");
            venderPerm = s.getString("Vender perm", "");
            preco = s.getDouble("Preco", 100);
            currencies = new ArrayList<>();
            ConfigurationSection curr = s.getConfigurationSection("Currencies");
            if (curr != null) {
                for (String key : curr.getKeys(false)) {
                    ConfigurationSection cs = curr.getConfigurationSection(key);
                    if (cs != null) currencies.add(new DropCurrency(cs.getString("Provider","Money"), cs.getDouble("Amount",0)));
                }
            }
            ConfigurationSection cmd = s.getConfigurationSection("Comando");
            if (cmd != null) {
                comandoAtivar = cmd.getBoolean("Ativar", false);
                usarQuantia = cmd.getBoolean("UsarQuantia", true);
                multiplicarQuantiaPreco = cmd.getBoolean("MultiplicarQuantiaPreco", true);
                invBypass = cmd.getBoolean("InvBypass", true);
                coletarChatBypass = cmd.getBoolean("ColetarChatBypass", true);
                formatarQuantia = cmd.getBoolean("FormatarQuantia", true);
                comandos = cmd.getStringList("Comandos");
            } else {
                comandoAtivar = false; usarQuantia = true; multiplicarQuantiaPreco = true;
                invBypass = true; coletarChatBypass = true; formatarQuantia = true; comandos = new ArrayList<>();
            }
            ConfigurationSection icone = s.getConfigurationSection("Icone");
            if (icone != null) {
                iconeId = icone.getString("ID", "IRON_INGOT"); iconeData = icone.getInt("Data", 0);
                iconeName = icone.getString("Name", nome); iconeLore = icone.getStringList("Lore");
            } else { iconeId = "IRON_INGOT"; iconeData = 0; iconeName = nome; iconeLore = new ArrayList<>(); }
        }

        public String getNome() { return nome; }
        public boolean isArmazem() { return armazem; }
        public boolean isRecolher() { return recolher; }
        public boolean isVender() { return vender; }
        public String getVenderBotao() { return venderBotao; }
        public String getRecolherBotao() { return recolherBotao; }
        public String getRecolherPerm() { return recolherPerm; }
        public String getVenderPerm() { return venderPerm; }
        public double getPreco() { return preco; }
        public List<DropCurrency> getCurrencies() { return currencies; }
        public boolean isComandoAtivar() { return comandoAtivar; }
        public boolean isUsarQuantia() { return usarQuantia; }
        public boolean isMultiplicarQuantiaPreco() { return multiplicarQuantiaPreco; }
        public boolean isInvBypass() { return invBypass; }
        public boolean isColetarChatBypass() { return coletarChatBypass; }
        public boolean isFormatarQuantia() { return formatarQuantia; }
        public List<String> getComandos() { return comandos; }
        public String getIconeId() { return iconeId; }
        public int getIconeData() { return iconeData; }
        public String getIconeName() { return iconeName; }
        public List<String> getIconeLore() { return iconeLore; }
    }

    public static class DropCurrency {
        private final String provider;
        private final double amount;
        public DropCurrency(String provider, double amount) { this.provider = provider; this.amount = amount; }
        public String getProvider() { return provider; }
        public double getAmount() { return amount; }
    }
}

