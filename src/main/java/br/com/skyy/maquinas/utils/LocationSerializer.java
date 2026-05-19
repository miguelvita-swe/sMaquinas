package br.com.skyy.maquinas.utils;

import org.bukkit.Location;

public class LocationSerializer {

    public static String serialize(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public static Location deserialize(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            String[] parts = str.split(";");
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
}

