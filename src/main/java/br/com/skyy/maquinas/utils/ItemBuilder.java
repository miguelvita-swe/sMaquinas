package br.com.skyy.maquinas.utils;

import br.com.skyy.core.SCore;
import br.com.skyy.core.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.List;

/**
 * sMaquinas ItemBuilder — delegates NBT, skull and material resolution to sCore.
 * Includes backward-compatible NBT migration from old "smaquinas" namespace to "score".
 */
public class ItemBuilder {

    // Legacy namespace used before sCore integration (for migration fallback)
    private static final String LEGACY_NS = "smaquinas";

    private ItemStack item;
    private ItemMeta  meta;

    @SuppressWarnings("deprecation")
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material, 1);
        this.meta = item.getItemMeta();
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder(Material material, int data) {
        this.item = new ItemStack(material, 1, (short) data);
        this.meta = item.getItemMeta();
    }

    // ── Chainable setters ──────────────────────────────────────────────────

    public ItemBuilder nome(String nome) {
        if (meta != null) meta.setDisplayName(ColorUtil.colorize(nome));
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null)
            meta.setLore(ColorUtil.colorize(lore));
        return this;
    }

    public ItemBuilder quantia(int quantia) {
        item.setAmount(quantia);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) applyGlow(meta);
        return this;
    }

    public ItemBuilder skullOwner(String playerName) {
        if (playerName != null && meta instanceof SkullMeta)
            SCore.getSkull().applyOwner((SkullMeta) meta, playerName);
        return this;
    }

    public ItemBuilder skullTexture(String url) {
        if (url != null && !url.isEmpty() && meta instanceof SkullMeta)
            SCore.getSkull().applyTexture((SkullMeta) meta, url);
        return this;
    }

    /** Sets a String NBT tag via sCore (namespace "score", all versions). */
    public ItemBuilder nbt(String key, String value) {
        if (key == null) return this;
        item.setItemMeta(meta);
        SCore.getNBT().setString(item, key, value);
        meta = item.getItemMeta();
        return this;
    }

    /** Sets an int NBT tag via sCore. */
    public ItemBuilder nbt(String key, int value) {
        if (key == null) return this;
        item.setItemMeta(meta);
        SCore.getNBT().setInt(item, key, value);
        meta = item.getItemMeta();
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }

    // ── Static readers (with migration fallback) ───────────────────────────

    /**
     * Reads a String NBT tag from an item.
     * Tries sCore namespace "score" first; if null, falls back to old "smaquinas"
     * namespace for items created before the sCore migration.
     */
    public static String getNBTString(ItemStack item, String key) {
        if (item == null || key == null) return null;

        // 1. Try new sCore namespace
        String value = SCore.getNBT().getString(item, key);
        if (value != null) return value;

        // 2. Fallback: old "smaquinas" PDC namespace (migration compat, 1.14+)
        return readLegacyString(item, key);
    }

    /**
     * Reads an int NBT tag from an item.
     * Tries sCore namespace "score" first; falls back to old "smaquinas" namespace.
     */
    public static Integer getNBTInt(ItemStack item, String key) {
        if (item == null || key == null) return null;

        // 1. Try new sCore namespace
        Integer value = SCore.getNBT().getInt(item, key);
        if (value != null) return value;

        // 2. Fallback: old "smaquinas" PDC namespace (migration compat, 1.14+)
        return readLegacyInt(item, key);
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getMaterialByName(String name, int data) {
        try {
            Material mat = SCore.getMaterial().get(name, "STONE");
            return new ItemStack(mat, 1, (short) data);
        } catch (Exception e) {
            return new ItemStack(Material.STONE);
        }
    }

    // ── Legacy namespace migration helpers ─────────────────────────────────

    @SuppressWarnings("deprecation")
    private static String readLegacyString(ItemStack item, String key) {
        if (!item.hasItemMeta()) return null;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return null;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            // Try every possible plugin instance for the old NamespacedKey
            NamespacedKey nk = buildLegacyKey(key);
            if (nk == null) return null;
            return pdc.has(nk, PersistentDataType.STRING)
                    ? pdc.get(nk, PersistentDataType.STRING) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static Integer readLegacyInt(ItemStack item, String key) {
        if (!item.hasItemMeta()) return null;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return null;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey nk = buildLegacyKey(key);
            if (nk == null) return null;
            return pdc.has(nk, PersistentDataType.INTEGER)
                    ? pdc.get(nk, PersistentDataType.INTEGER) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Builds a NamespacedKey with the old "smaquinas" namespace, or null if unavailable. */
    @SuppressWarnings("deprecation")
    private static NamespacedKey buildLegacyKey(String key) {
        try {
            // NamespacedKey only available in 1.14+ (PDC servers) — safe to use here
            return new NamespacedKey(LEGACY_NS, key.toLowerCase().replace(" ", "_"));
        } catch (Exception e) {
            return null;
        }
    }

    // ── Glow ───────────────────────────────────────────────────────────────

    private void applyGlow(ItemMeta meta) {
        // 1.21+: setEnchantmentGlintOverride
        try {
            Method m = meta.getClass().getMethod("setEnchantmentGlintOverride", Boolean.class);
            m.invoke(meta, Boolean.TRUE);
            return;
        } catch (Exception ignored) {}

        // 1.8–1.20 fallback
        try {
            Enchantment ench;
            try {
                ench = (Enchantment) Enchantment.class.getField("DURABILITY").get(null);
            } catch (NoSuchFieldException e) {
                ench = (Enchantment) Enchantment.class.getField("UNBREAKING").get(null);
            }
            meta.addEnchant(ench, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } catch (Exception ignored) {}
    }

    /** @deprecated Use ColorUtil.colorize() directly */
    @Deprecated
    public static String colorir(String text) {
        return ColorUtil.colorize(text);
    }
}
