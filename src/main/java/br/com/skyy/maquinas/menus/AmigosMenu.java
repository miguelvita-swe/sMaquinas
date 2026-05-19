package br.com.skyy.maquinas.menus;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.models.MaquinaConfig;
import br.com.skyy.maquinas.utils.ConfigManager;
import br.com.skyy.maquinas.utils.ItemBuilder;
import br.com.skyy.maquinas.utils.MaterialCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AmigosMenu extends BaseMenu {

    private final MaquinaColocada maquina;
    private final MaquinaConfig config;
    private int page;

    // Pending add: UUID → máquina
    private static final Map<UUID, MaquinaColocada> pendingAdd = new HashMap<>();

    public AmigosMenu(SMaquinas plugin, MaquinaColocada maquina) {
        this(plugin, maquina, 0);
    }

    public AmigosMenu(SMaquinas plugin, MaquinaColocada maquina, int page) {
        super(plugin);
        this.maquina = maquina;
        this.config  = plugin.getMaquinaManager().getConfig(maquina.getTipoMaquina());
        this.page    = page;
    }

    public static Map<UUID, MaquinaColocada> getPendingAdd() { return pendingAdd; }

    // ── build ─────────────────────────────────────────────────────────────

    @Override
    public void build() {
        FileConfiguration yml = plugin.getConfigManager().getMenuAmigos();

        // Título
        String titulo = yml.getString("Nome", "&8Amigos");
        if (config != null)
            titulo = titulo.replace("{maquina}", ConfigManager.colorir(config.getNome()));

        int tamanho = yml.getInt("Tamanho", 54);
        inventory = createInventory(tamanho, titulo);


        // Slots disponíveis para amigos
        List<Integer> slots = yml.getIntegerList("Slots");
        if (slots.isEmpty()) {
            // Fallback: slots 10-43 excluindo bordas
            slots = new ArrayList<>();
            for (int r = 0; r < 3; r++)
                for (int c = 1; c <= 7; c++)
                    slots.add((r + 1) * 9 + c);
        }

        int perPage = slots.size();
        int inicio  = page * perPage;
        List<UUID> amigos = maquina.getAmigos();

        // Renderizar amigos
        for (int i = 0; i < perPage; i++) {
            int idx = inicio + i;
            if (idx >= amigos.size()) break;
            int slot = slots.get(i);
            if (slot < 0 || slot >= tamanho) continue;

            UUID uuid = amigos.get(idx);
            inventory.setItem(slot, buildAmigoItem(yml, uuid));
        }

        // Paginação – anterior
        int anteriorSlot = yml.getInt("Backslot", 18);
        int voltarSlot   = yml.getInt("VoltarSlot", 9);
        int proximoSlot  = yml.getInt("ProximoSlot", 17);

        if (page > 0) {
            ConfigurationSection setaAnt = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Anterior");
            if (setaAnt != null) {
                Material m = MaterialCompat.of(setaAnt.getString("ID", "262"), "ARROW");
                inventory.setItem(anteriorSlot, new ItemBuilder(m)
                        .nome(setaAnt.getString("Name", "&cAnterior"))
                        .lore(setaAnt.getStringList("Lore"))
                        .nbt("smaquinas_action", "anterior").build());
            } else {
                inventory.setItem(anteriorSlot, new ItemBuilder(Material.ARROW)
                        .nome("&cAnterior").nbt("smaquinas_action", "anterior").build());
            }
        }

        // Paginação – próximo
        if (amigos.size() > inicio + perPage) {
            ConfigurationSection setaProx = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Proximo");
            if (setaProx != null) {
                Material m = MaterialCompat.of(setaProx.getString("ID", "262"), "ARROW");
                inventory.setItem(proximoSlot, new ItemBuilder(m)
                        .nome(setaProx.getString("Name", "&aProximo"))
                        .lore(setaProx.getStringList("Lore"))
                        .nbt("smaquinas_action", "proximo").build());
            } else {
                inventory.setItem(proximoSlot, new ItemBuilder(Material.ARROW)
                        .nome("&aProximo").nbt("smaquinas_action", "proximo").build());
            }
        }

        // Botão voltar (ao menu principal)
        ConfigurationSection setaVolt = plugin.getConfigManager().getConfig().getConfigurationSection("Setas.Voltar");
        if (setaVolt != null) {
            Material m = MaterialCompat.of(setaVolt.getString("ID", "262"), "ARROW");
            inventory.setItem(voltarSlot, new ItemBuilder(m)
                    .nome(setaVolt.getString("Name", "&cVoltar"))
                    .lore(setaVolt.getStringList("Lore"))
                    .nbt("smaquinas_action", "voltar").build());
        } else {
            inventory.setItem(voltarSlot, new ItemBuilder(Material.ARROW)
                    .nome("&cVoltar").nbt("smaquinas_action", "voltar").build());
        }

        // Itens extras da seção Itens: (Adicionar, decoração, etc.)
        ConfigurationSection itens = yml.getConfigurationSection("Itens");
        if (itens != null) {
            for (String key : itens.getKeys(false)) {
                ConfigurationSection sec = itens.getConfigurationSection(key);
                if (sec == null) continue;
                int slot = sec.getInt("Slot", -1);
                if (slot < 0 || slot >= tamanho) continue;
                inventory.setItem(slot, buildExtraItem(sec, key));
            }
        }
    }

    // ── Item de amigo com skull ────────────────────────────────────────────

    private ItemStack buildAmigoItem(FileConfiguration yml, UUID uuid) {
        ConfigurationSection sec = yml.getConfigurationSection("Amigo");
        OfflinePlayer op   = Bukkit.getOfflinePlayer(uuid);
        String nome        = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);

        String name        = (sec != null ? sec.getString("Name", "&a{player}") : "&a{player}")
                                .replace("{player}", nome);
        boolean glow       = sec != null && sec.getBoolean("Glow", true);

        List<String> rawLore = sec != null ? sec.getStringList("Lore") : new ArrayList<>();
        List<String> lore  = new ArrayList<>();
        for (String line : rawLore) lore.add(line.replace("{player}", nome));

        if (lore.isEmpty()) {
            lore.add("");
            lore.add("&aBotão &fDIREITO&a para deletar.");
            lore.add("");
        }

        return new ItemBuilder(MaterialCompat.playerHead())
                .nome(name)
                .lore(lore)
                .glow(glow)
                .skullOwner(nome)
                .nbt("smaquinas_amigo_uuid", uuid.toString())
                .build();
    }

    // ── Item extra (Adicionar, decoração) ─────────────────────────────────

    private ItemStack buildExtraItem(ConfigurationSection sec, String key) {
        String url   = sec.getString("URL", "");
        String id    = sec.getString("ID", "STONE");
        int data     = sec.getInt("Data", 0);
        boolean glow = sec.getBoolean("Glow", false);
        String name  = sec.getString("Name", "&f" + key);
        List<String> lore = sec.getStringList("Lore");

        Material mat = (!id.equals("AIR") && !id.isEmpty() && !id.equals("0"))
                ? MaterialCompat.of(id, "STONE")
                : MaterialCompat.playerHead();

        ItemBuilder builder = new ItemBuilder(mat, data).nome(name).lore(lore).glow(glow)
                .nbt("smaquinas_amigos_key", key);

        if (!url.isEmpty() && url.startsWith("http")) builder.skullTexture(url);

        return builder.build();
    }

    // ── click ─────────────────────────────────────────────────────────────

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType click) {
        if (item == null) return;

        // Ações de navegação
        String action = ItemBuilder.getNBTString(item, "smaquinas_action");
        if (action != null) {
            switch (action) {
                case "voltar":
                    openLater(plugin, player, () -> new MaquinaInfoMenu(plugin, maquina)); return;
                case "anterior":
                    if (page > 0) { page--; final int p = page; org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); }); } return;
                case "proximo":
                    page++; org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { build(); player.openInventory(inventory); }); return;
            }
        }

        // Clique em item extra (ex: Adicionar)
        String menuKey = ItemBuilder.getNBTString(item, "smaquinas_amigos_key");
        if ("Adicionar".equals(menuKey)) {
            if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
            }
            // Verificar limite máximo de amigos
            int maxAmigo = plugin.getConfigManager().getConfig().getInt("Opcoes.Maximo amigo", 0);
            if (maxAmigo > 0 && maquina.getAmigos().size() >= maxAmigo) {
                player.sendMessage(plugin.getConfigManager().msg("machine-max-friend")); return;
            }
            player.closeInventory();
            pendingAdd.put(player.getUniqueId(), maquina);
            player.sendMessage(plugin.getConfigManager().msg("digit-add"));
            return;
        }

        // Clique em amigo → remover com botão DIREITO
        String amigoUUIDStr = ItemBuilder.getNBTString(item, "smaquinas_amigo_uuid");
        if (amigoUUIDStr != null) {
            if (!maquina.getDono().equals(player.getUniqueId()) && !player.hasPermission("smaquinas.admin")) {
                player.sendMessage(plugin.getConfigManager().msg("machine-just-owner")); return;
            }
            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                try {
                    UUID uuid = UUID.fromString(amigoUUIDStr);
                    maquina.getAmigos().remove(uuid);
                    plugin.getDB().saveMaquina(maquina);
                    OfflinePlayer op    = Bukkit.getOfflinePlayer(uuid);
                    String nomeRemovido = op.getName() != null ? op.getName() : amigoUUIDStr.substring(0, 8);
                    player.sendMessage(ConfigManager.colorir(
                            "&aVocê removeu o jogador &7" + nomeRemovido + "&a da lista de amigos."));
                    // Se a página ficar vazia, volta uma página
                    FileConfiguration yml = plugin.getConfigManager().getMenuAmigos();
                    int perPage = yml.getIntegerList("Slots").size();
                    if (perPage == 0) perPage = 21;
                    if (page > 0 && maquina.getAmigos().size() <= page * perPage) page--;
                    build();
                    player.openInventory(inventory);
                } catch (Exception ignored) {}
            }
        }
    }

    // ── Adicionar amigo (chamado pelo ChatListener) ────────────────────────

    public static void adicionarAmigo(SMaquinas plugin, Player player, MaquinaColocada maquina, String nomeAlvo) {
        // Verificar máximo
        int maxAmigo = plugin.getConfigManager().getConfig().getInt("Opcoes.Maximo amigo", 0);
        if (maxAmigo > 0 && maquina.getAmigos().size() >= maxAmigo) {
            player.sendMessage(plugin.getConfigManager().msg("machine-max-friend")); return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer alvo = Bukkit.getOfflinePlayer(nomeAlvo);
        if (alvo == null || (!alvo.isOnline() && !alvo.hasPlayedBefore())) {
            player.sendMessage(plugin.getConfigManager().msg("target", "{player}", nomeAlvo)); return;
        }
        if (alvo.getUniqueId().equals(maquina.getDono())) {
            player.sendMessage(plugin.getConfigManager().msg("machine-add-owner")); return;
        }
        if (maquina.getAmigos().contains(alvo.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().msg("machine-friend-already")); return;
        }

        maquina.getAmigos().add(alvo.getUniqueId());
        plugin.getDB().saveMaquina(maquina);
        player.sendMessage(plugin.getConfigManager().msg("machine-added",
                "{player}", alvo.getName() != null ? alvo.getName() : nomeAlvo));

        // Reabrir menu
        new AmigosMenu(plugin, maquina).open(player);
    }
}
