package br.com.skyy.maquinas.listeners;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.menus.AmigosMenu;
import br.com.skyy.maquinas.menus.CombustiveisShopMenu;
import br.com.skyy.maquinas.menus.CombustiveisShopMenu.PendingCombustivelCompra;
import br.com.skyy.maquinas.menus.DropsMenu;
import br.com.skyy.maquinas.menus.MaquinaInfoMenu;
import br.com.skyy.maquinas.menus.MaquinasShopMenu;
import br.com.skyy.maquinas.menus.MaquinasShopMenu.PendingMaquinaCompra;
import br.com.skyy.maquinas.models.MaquinaColocada;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class ChatListener implements Listener {

    private static final String CANCELAR = "cancelar";
    private static final String MSG_CANCELLED = "cancelled";
    private static final String MSG_NUMBER = "number";

    private final SMaquinas plugin;

    public ChatListener(SMaquinas plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        handleChat(event, player, uuid, event.getMessage().trim());
    }

    private void handleChat(AsyncPlayerChatEvent event, Player player, UUID uuid, String message) {
        // ── Compra de combustível via chat ───────────────────────────────────
        PendingCombustivelCompra pendingComb = PendingCombustivelCompra.pending.get(uuid);
        if (pendingComb != null) {
            event.setCancelled(true);
            PendingCombustivelCompra.pending.remove(uuid);
            if (message.equalsIgnoreCase(CANCELAR)) {
                player.sendMessage(plugin.getConfigManager().msg(MSG_CANCELLED));
                Bukkit.getScheduler().runTask(plugin, () ->
                        new CombustiveisShopMenu(plugin, pendingComb.shopPage).open(player));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    int qtd = Integer.parseInt(message);
                    if (qtd <= 0) throw new NumberFormatException();
                    pendingComb.executar(player, qtd);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().msg(MSG_NUMBER));
                    new CombustiveisShopMenu(plugin, pendingComb.shopPage).open(player);
                }
            });
            return;
        }

        // ── Compra de máquina via chat (escolher quantia) ────────────────────
        PendingMaquinaCompra pendingMaq = MaquinasShopMenu.pendingCompra.get(uuid);
        if (pendingMaq != null) {
            event.setCancelled(true);
            MaquinasShopMenu.pendingCompra.remove(uuid);
            if (message.equalsIgnoreCase(CANCELAR)) {
                player.sendMessage(plugin.getConfigManager().msg(MSG_CANCELLED));
                Bukkit.getScheduler().runTask(plugin, () ->
                        new MaquinasShopMenu(plugin, pendingMaq.shopPage).open(player));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    int qtd = Integer.parseInt(message);
                    if (qtd <= 0) throw new NumberFormatException();
                    pendingMaq.executar(player, qtd);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().msg(MSG_NUMBER));
                    new MaquinasShopMenu(plugin, pendingMaq.shopPage).open(player);
                }
            });
            return;
        }

        // ── Definir multiplicador ────────────────────────────────────────────
        MaquinasShopMenu shopMenu = MaquinasShopMenu.pendingMultiplier.get(uuid);
        if (shopMenu != null) {
            event.setCancelled(true);
            MaquinasShopMenu.pendingMultiplier.remove(uuid);
            if (message.equalsIgnoreCase(CANCELAR)) {
                player.sendMessage(plugin.getConfigManager().msg(MSG_CANCELLED));
                Bukkit.getScheduler().runTask(plugin, () -> shopMenu.open(player));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    int valor = Integer.parseInt(message);
                    shopMenu.setMultiplicador(player, valor);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().msg(MSG_NUMBER));
                    shopMenu.open(player);
                }
            });
            return;
        }

        // ── Coletar drops (digitando quantia no chat) ────────────────────────
        MaquinaColocada maquinaCollect = DropsMenu.getPendingCollect().get(uuid);
        if (maquinaCollect != null) {
            event.setCancelled(true);
            DropsMenu.getPendingCollect().remove(uuid);
            if (message.equalsIgnoreCase(CANCELAR)) {
                player.sendMessage(plugin.getConfigManager().msg(MSG_CANCELLED));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                double quantia;
                if (message.equalsIgnoreCase("tudo")) {
                    quantia = maquinaCollect.getDrops();
                } else {
                    try { quantia = Double.parseDouble(message); }
                    catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().msg(MSG_NUMBER));
                        return;
                    }
                }
                if (quantia <= 0 || quantia > maquinaCollect.getDrops()) {
                    player.sendMessage(plugin.getConfigManager().msg("machine-available-drops"));
                    return;
                }
                DropsMenu menu = new DropsMenu(plugin, maquinaCollect);
                if (plugin.getMaquinaManager().getConfig(maquinaCollect.getTipoMaquina()) != null) {
                    menu.coletarDrops(player,
                            plugin.getMaquinaManager().getConfig(maquinaCollect.getTipoMaquina()).getDropConfig(),
                            quantia);
                }
            });
            return;
        }

        // ── Remove de stack ──────────────────────────────────────────────────
        MaquinaColocada maquinaRemove = MaquinaInfoMenu.getPendingRemove().get(uuid);
        if (maquinaRemove != null) {
            event.setCancelled(true);
            MaquinaInfoMenu.getPendingRemove().remove(uuid);
            if (message.equalsIgnoreCase(CANCELAR)) {
                player.sendMessage(plugin.getConfigManager().msg(MSG_CANCELLED));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    int q = Integer.parseInt(message);
                    new MaquinaInfoMenu(plugin, maquinaRemove).removerMaquinas(player, q);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().msg(MSG_NUMBER));
                }
            });
            return;
        }

        // ── Adicionar amigo ──────────────────────────────────────────────────
        MaquinaColocada maquina = AmigosMenu.getPendingAdd().get(uuid);
        if (maquina == null) return;

        event.setCancelled(true);
        AmigosMenu.getPendingAdd().remove(uuid);

        if (message.equalsIgnoreCase(CANCELAR)) {
            player.sendMessage(plugin.getConfigManager().msg(MSG_CANCELLED));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(message);
            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                player.sendMessage(plugin.getConfigManager().msg("target", "{player}", message));
                return;
            }
            if (target.getUniqueId().equals(uuid)) {
                player.sendMessage(plugin.getConfigManager().msg("yourself")); return;
            }
            if (maquina.getDono().equals(target.getUniqueId())) {
                player.sendMessage(plugin.getConfigManager().msg("machine-add-owner")); return;
            }
            if (maquina.isAmigo(target.getUniqueId())) {
                player.sendMessage(plugin.getConfigManager().msg("machine-friend-already")); return;
            }
            int maxAmigos = plugin.getConfigManager().getConfig().getInt("Opcoes.Maximo amigo", 0);
            if (maxAmigos > 0 && maquina.getAmigos().size() >= maxAmigos) {
                player.sendMessage(plugin.getConfigManager().msg("machine-max-friend")); return;
            }
            maquina.getAmigos().add(target.getUniqueId());
            plugin.getDB().saveMaquina(maquina);
            String nome = target.getName() != null ? target.getName() : message;
            player.sendMessage(plugin.getConfigManager().msg("machine-added", "{player}", nome));
            if (target.isOnline()) {
                ((Player) target).sendMessage(plugin.getConfigManager().msg("machine-added", "{player}", player.getName()));
            }
        });
    }
}
