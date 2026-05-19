package br.com.skyy.maquinas.utils;

import br.com.skyy.core.SCore;
import org.bukkit.Material;

/**
 * Thin wrapper around sCore's MaterialProvider.
 * Keeps the original sMaquinas API intact while delegating resolution to sCore
 * (which handles legacy data values for 1.8–1.12 transparently).
 */
public class MaterialCompat {

    public static Material of(String name, String fallback) {
        return SCore.getMaterial().get(name, fallback);
    }

    public static Material of(String... names) {
        for (String name : names) {
            Material m = SCore.getMaterial().get(name, null);
            if (m != null) return m;
        }
        return Material.STONE;
    }

    public static Material playerHead() {
        return SCore.getMaterial().get("PLAYER_HEAD", "SKULL_ITEM");
    }

    public static Material glassPaneGray() {
        return SCore.getMaterial().get("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
    }

    public static Material glassPaneBlack() {
        return SCore.getMaterial().get("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
    }

    public static Material glassPane() {
        return SCore.getMaterial().get("GLASS_PANE", "THIN_GLASS");
    }

    public static Material barrier() {
        return SCore.getMaterial().get("BARRIER", "STONE");
    }

    public static Material anvil() {
        return SCore.getMaterial().get("ANVIL", "STONE");
    }

    @SuppressWarnings("deprecation")
    public static ItemBuilder grayGlassPane() {
        short data = SCore.getMaterial().getData("GRAY_STAINED_GLASS_PANE");
        return new ItemBuilder(glassPaneGray(), data);
    }

    @SuppressWarnings("deprecation")
    public static ItemBuilder blackGlassPane() {
        short data = SCore.getMaterial().getData("BLACK_STAINED_GLASS_PANE");
        return new ItemBuilder(glassPaneBlack(), data);
    }

    @SuppressWarnings("deprecation")
    public static ItemBuilder glassPane(int data) {
        return new ItemBuilder(glassPaneGray(), data);
    }
}
