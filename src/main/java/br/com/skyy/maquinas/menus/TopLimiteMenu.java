package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import br.com.skyy.maquinas.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TopLimiteMenu extends BaseMenu {

    // Tipos de top disponíveis
    public enum TipoTop { LIMITE, COMPRADAS, COLOCADAS, VALOR }

    private TipoTop tipoAtual;
    private int page;

    // Tipos habilitados (na ordem da config)
    private final List<TipoTop> tiposAtivos = new ArrayList<>();

    public TopLimiteMenu(SMaquinas plugin) {
        this(plugin, TipoTop.LIMITE, 0);
    }

    public TopLimiteMenu(SMaquinas plugin, TipoTop tipo, int page) {
        super(plugin);
        this.tipoAtual = tipo;
        this.page = page;
        carregarTiposAtivos();
    }

    private void carregarTiposAtivos() {
        tiposAtivos.clear();
        FileConfiguration yml = plugin.getConfigManager().getMenuTop();
        ConfigurationSection tipos = yml.getConfigurationSection("Tipos");
        if (tipos == null) {
            tiposAtivos.addAll(Arrays.asList(TipoTop.values()));
            return;
        }
        if (tipos.getBoolean("Limite.Ativar", true))    tiposAtivos.add(TipoTop.LIMITE);
        if (tipos.getBoolean("Compradas.Ativar", true)) tiposAtivos.add(TipoTop.COMPRADAS);
        if (tipos.getBoolean("Colocadas.Ativar", true)) tiposAtivos.add(TipoTop.COLOCADAS);
        if (tipos.getBoolean("Valor.Ativar", true))     tiposAtivos.add(TipoTop.VALOR);
    }

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuTop();

        String titulo = yml.getString("Nome", "&8TOP Máquinas");
        int tamanho   = yml.getInt("Tamanho", 36);

        inventory = createInventory(tamanho, titulo);


        // Slots de dados
        List<Integer> slots = yml.getIntegerList("Slots");
        if (slots.isEmpty()) slots = Arrays.asList(10, 11, 12, 13, 14, 15, 16);

        int perPage = slots.size();
        int inicio  = page * perPage;

        // Buscar dados do tipo atual
        List<Map.Entry<UUID, Double>> dados = buscarDados(perPage + 1); // +1 para saber se há próxima pág

        // Renderizar itens de jogadores
        for (int i = 0; i < perPage; i++) {
            int idx = inicio + i;
            if (idx >= dados.size()) break;
            int slot = slots.get(i);
            if (slot < 0 || slot >= tamanho) continue;

            Map.Entry<UUID, Double> entry = dados.get(idx);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String nome = op.getName() != null ? op.getName() : "Desconhecido";
            int pos = idx + 1;

            ItemStack item = buildJogadorItem(yml, nome, pos, entry.getValue());
            inventory.setItem(slot, item);
        }

        // Navegação: anterior
        int anteriorSlot = yml.getInt("AnteriorSlot", 9);
        int proximoSlot  = yml.getInt("ProximoSlot", 17);
        int backSlot     = yml.getInt("BackSlot", 31);

        if (page > 0) {
            ConfigurationSection setaAnterior = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Anterior");
            if (setaAnterior != null) {
                Material mat = MaterialCompat.of(setaAnterior.getString("ID", "262"), "ARROW");
                inventory.setItem(anteriorSlot, new ItemBuilder(mat)
                        .nome(setaAnterior.getString("Name", "&cAnterior"))
                        .lore(setaAnterior.getStringList("Lore"))
                        .nbt("smaquinas_action", "anterior").build());
            } else {
                inventory.setItem(anteriorSlot, new ItemBuilder(Material.ARROW).nome("&cAnterior")
                        .nbt("smaquinas_action", "anterior").build());
            }
        }

        // Navegação: próximo
        if (dados.size() > inicio + perPage) {
            ConfigurationSection setaProximo = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Proximo");
            if (setaProximo != null) {
                Material mat = MaterialCompat.of(setaProximo.getString("ID", "262"), "ARROW");
                inventory.setItem(proximoSlot, new ItemBuilder(mat)
                        .nome(setaProximo.getString("Name", "&aProximo"))
                        .lore(setaProximo.getStringList("Lore"))
                        .nbt("smaquinas_action", "proximo").build());
            } else {
                inventory.setItem(proximoSlot, new ItemBuilder(Material.ARROW).nome("&aProximo")
                        .nbt("smaquinas_action", "proximo").build());
            }
        }

        // Botão voltar
        ConfigurationSection setaVoltar = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Voltar");
        if (setaVoltar != null) {
            Material mat = MaterialCompat.of(setaVoltar.getString("ID", "262"), "ARROW");
            inventory.setItem(backSlot, new ItemBuilder(mat)
                    .nome(setaVoltar.getString("Name", "&cVoltar"))
                    .lore(setaVoltar.getStringList("Lore"))
                    .nbt("smaquinas_action", "fechar").build());
        } else {
            inventory.setItem(backSlot, new ItemBuilder(Material.ARROW).nome("&cFechar")
                    .nbt("smaquinas_action", "fechar").build());
        }

        // Seletor de tipo
        buildSeletor(yml, tamanho);
    }

    private void buildSeletor(FileConfiguration yml, int tamanho) {
        ConfigurationSection selSec = yml.getConfigurationSection("Seletor");
        if (selSec == null) return;
        int selSlot = selSec.getInt("Slot", 32);
        if (selSlot < 0 || selSlot >= tamanho) return;

        String selNome = selSec.getString("Name", "&aSeletor do TOP");

        // Montar lore com os tipos disponíveis
        List<String> lore = new ArrayList<>();
        String fmtVisualizando = yml.getString("Formato.Visualizando", " &f• &a{nome}");
        String fmtSelecionar   = yml.getString("Formato.Selecionar",   " &f• &7{nome}");

        ConfigurationSection tipos = yml.getConfigurationSection("Tipos");
        for (TipoTop t : tiposAtivos) {
            String nomeT = getNomeTipo(yml, t);
            String linha = t == tipoAtual
                    ? fmtVisualizando.replace("{nome}", nomeT)
                    : fmtSelecionar.replace("{nome}", nomeT);
            lore.add(linha);
        }
        lore.add("");
        lore.add("&7Clique para trocar o tipo.");

        String url = selSec.getString("URL", "");
        Material mat = MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat)
                .nome(selNome)
                .lore(lore)
                .nbt("smaquinas_action", "seletor");

        // Se tiver URL, aplicar skull via skullOwner (nome da textura não é suportado em 1.8 sem NMS,
        // mas deixamos preparado para versões novas)
        if (!url.isEmpty() && !url.startsWith("http")) {
            builder.skullOwner(url);
        }

        inventory.setItem(selSlot, builder.build());
    }

    private ItemStack buildJogadorItem(FileConfiguration yml, String playerName, int pos, double valor) {
        String secKey;
        switch (tipoAtual) {
            case COMPRADAS: secKey = "Item compradas"; break;
            case COLOCADAS: secKey = "Item colocadas"; break;
            case VALOR:     secKey = "Item valor";     break;
            default:        secKey = "Item";           break;
        }

        ConfigurationSection sec = yml.getConfigurationSection(secKey);
        if (sec == null) sec = yml.getConfigurationSection("Item");

        String nome = sec != null ? sec.getString("Name", "&f{player}") : "&f{player}";
        nome = nome.replace("{player}", playerName);

        List<String> rawLore = sec != null ? sec.getStringList("Lore") : new ArrayList<>();
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            line = line
                    .replace("{player}", playerName)
                    .replace("{pos}", String.valueOf(pos))
                    .replace("{limite}", NumberFormatter.formatStatic(valor))
                    .replace("{maquinas}", NumberFormatter.formatStatic(valor))
                    .replace("{money}", NumberFormatter.formatStatic(valor));
            lore.add(line);
        }

        Material mat = MaterialCompat.playerHead();
        return new ItemBuilder(mat).nome(nome).lore(lore).skullOwner(playerName).build();
    }

    /** Busca os dados do tipo atual da fonte correta */
    private List<Map.Entry<UUID, Double>> buscarDados(int limit) {
        switch (tipoAtual) {
            case LIMITE:    return plugin.getDB().getTopLimite(limit);
            case COMPRADAS: return plugin.getDB().getTopCompradas(limit);
            case COLOCADAS: return plugin.getMaquinaManager().getTopColocadas(limit);
            case VALOR:     return plugin.getMaquinaManager().getTopValor(limit);
            default:        return Collections.emptyList();
        }
    }

    private String getNomeTipo(FileConfiguration yml, TipoTop tipo) {
        switch (tipo) {
            case LIMITE:    return yml.getString("Tipos.Limite.Nome",    "Limite");
            case COMPRADAS: return yml.getString("Tipos.Compradas.Nome", "Máquinas Compradas");
            case COLOCADAS: return yml.getString("Tipos.Colocadas.Nome", "Máquinas Colocadas");
            case VALOR:     return yml.getString("Tipos.Valor.Nome",     "Valor das Máquinas");
            default:        return tipo.name();
        }
    }

    /** Avança para o próximo tipo ativo */
    private TipoTop proximoTipo() {
        if (tiposAtivos.isEmpty()) return tipoAtual;
        int idx = tiposAtivos.indexOf(tipoAtual);
        return tiposAtivos.get((idx + 1) % tiposAtivos.size());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;
        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if (action == null) return;

        switch (action) {
            case "anterior":
                if (page > 0) { page--; final int p1 = page; org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); }); }
                break;
            case "proximo":
                page++;
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
                break;
            case "seletor":
                tipoAtual = proximoTipo();
                page = 0;
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); });
                break;
            case "fechar":
                org.bukkit.Bukkit.getScheduler().runTask(plugin,
                        () -> new MaquinasShopMenu(plugin, 0).open(player));
                break;
        }
    }
}
