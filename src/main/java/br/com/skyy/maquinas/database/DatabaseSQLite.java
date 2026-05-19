package br.com.skyy.maquinas.database;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.BoosterData;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.utils.LocationSerializer;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseSQLite implements Database {

    private final SMaquinas plugin;
    private Connection connection;

    public DatabaseSQLite(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao conectar ao SQLite: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS maquinas (" +
                    "id TEXT PRIMARY KEY," +
                    "tipo TEXT NOT NULL," +
                    "dono TEXT NOT NULL," +
                    "location TEXT NOT NULL," +
                    "combustivel REAL DEFAULT 0," +
                    "drops REAL DEFAULT 0," +
                    "quebrada INTEGER DEFAULT 0," +
                    "combustivel_infinito INTEGER DEFAULT 0," +
                    "capacidade_extra REAL DEFAULT 0," +
                    "amigos TEXT DEFAULT ''," +
                    "upgrade_combustivel INTEGER DEFAULT 0," +
                    "upgrade_drops INTEGER DEFAULT 0," +
                    "upgrade_velocidade INTEGER DEFAULT 0," +
                    "upgrade_durabilidade INTEGER DEFAULT 0," +
                    "stack INTEGER DEFAULT 1," +
                    "ativo INTEGER DEFAULT 1," +
                    "holo_ativo INTEGER DEFAULT 1," +
                    "trusteds INTEGER DEFAULT 0" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS limites (" +
                    "uuid TEXT PRIMARY KEY," +
                    "limite REAL DEFAULT 0" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS compras_maquinas (" +
                    "uuid TEXT NOT NULL," +
                    "maquina_id TEXT NOT NULL," +
                    "quantia INTEGER DEFAULT 0," +
                    "PRIMARY KEY(uuid, maquina_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS compras_combustiveis (" +
                    "uuid TEXT NOT NULL," +
                    "combustivel_id TEXT NOT NULL," +
                    "quantia INTEGER DEFAULT 0," +
                    "PRIMARY KEY(uuid, combustivel_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS boosters (" +
                    "uuid TEXT PRIMARY KEY," +
                    "multiplicador REAL DEFAULT 1," +
                    "expiracao INTEGER DEFAULT 0" +
                    ")");

            // Migração de colunas novas (caso tabela já existia)
            try { stmt.execute("ALTER TABLE maquinas ADD COLUMN ativo INTEGER DEFAULT 1"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE maquinas ADD COLUMN holo_ativo INTEGER DEFAULT 1"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE maquinas ADD COLUMN trusteds INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao fechar SQLite: " + e.getMessage());
        }
    }

    @Override
    public void saveMaquina(MaquinaColocada maquina) {
        String sql = "INSERT OR REPLACE INTO maquinas (id, tipo, dono, location, combustivel, drops, quebrada, combustivel_infinito, capacidade_extra, amigos, upgrade_combustivel, upgrade_drops, upgrade_velocidade, upgrade_durabilidade, stack, ativo, holo_ativo, trusteds) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maquina.getId());
            ps.setString(2, maquina.getTipoMaquina());
            ps.setString(3, maquina.getDono().toString());
            ps.setString(4, LocationSerializer.serialize(maquina.getLocation()));
            ps.setDouble(5, maquina.getCombustivel());
            ps.setDouble(6, maquina.getDrops());
            ps.setInt(7, maquina.isQuebrada() ? 1 : 0);
            ps.setInt(8, maquina.getCombustivelInfinito() ? 1 : 0);
            ps.setDouble(9, maquina.getCapacidadeExtra());
            ps.setString(10, serializeUUIDs(maquina.getAmigos()));
            ps.setInt(11, maquina.getUpgradeCombutivel());
            ps.setInt(12, maquina.getUpgradeDrops());
            ps.setInt(13, maquina.getUpgradeVelocidade());
            ps.setInt(14, maquina.getUpgradeDurabilidade());
            ps.setInt(15, maquina.getStack());
            ps.setInt(16, maquina.isAtivo() ? 1 : 0);
            ps.setInt(17, maquina.isHoloAtivo() ? 1 : 0);
            ps.setInt(18, maquina.isTrusteds() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar máquina: " + e.getMessage());
        }
    }

    @Override
    public void deleteMaquina(String id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM maquinas WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao deletar máquina: " + e.getMessage());
        }
    }

    @Override
    public List<MaquinaColocada> loadAllMaquinas() {
        List<MaquinaColocada> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM maquinas")) {
            while (rs.next()) {
                try {
                    MaquinaColocada m = new MaquinaColocada(
                            rs.getString("id"),
                            rs.getString("tipo"),
                            UUID.fromString(rs.getString("dono")),
                            LocationSerializer.deserialize(rs.getString("location"))
                    );
                    m.setCombustivel(rs.getDouble("combustivel"));
                    m.setDrops(rs.getDouble("drops"));
                    m.setQuebrada(rs.getInt("quebrada") == 1);
                    m.setCombustivelInfinito(rs.getInt("combustivel_infinito") == 1);
                    m.setCapacidadeExtra(rs.getDouble("capacidade_extra"));
                    m.setUpgradeCombutivel(rs.getInt("upgrade_combustivel"));
                    m.setUpgradeDrops(rs.getInt("upgrade_drops"));
                    m.setUpgradeVelocidade(rs.getInt("upgrade_velocidade"));
                    m.setUpgradeDurabilidade(rs.getInt("upgrade_durabilidade"));
                    m.setStack(rs.getInt("stack"));
                    m.setAtivo(rs.getInt("ativo") == 1);
                    m.setHoloAtivo(rs.getInt("holo_ativo") == 1);
                    m.setTrusteds(rs.getInt("trusteds") == 1);

                    String amigosStr = rs.getString("amigos");
                    if (amigosStr != null && !amigosStr.isEmpty()) {
                        for (String uuidStr : amigosStr.split(",")) {
                            if (!uuidStr.isEmpty()) {
                                try { m.getAmigos().add(UUID.fromString(uuidStr)); } catch (Exception ignored) {}
                            }
                        }
                    }
                    list.add(m);
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao carregar máquina: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar máquinas: " + e.getMessage());
        }
        return list;
    }

    @Override
    public double getLimite(UUID player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT limite FROM limites WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("limite");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao buscar limite: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void setLimite(UUID player, double limite) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO limites (uuid, limite) VALUES (?, ?)")) {
            ps.setString(1, player.toString());
            ps.setDouble(2, limite);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar limite: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllLimites() {
        Map<UUID, Double> map = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM limites")) {
            while (rs.next()) {
                try {
                    map.put(UUID.fromString(rs.getString("uuid")), rs.getDouble("limite"));
                } catch (Exception ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar limites: " + e.getMessage());
        }
        return map;
    }

    @Override
    public int getComprasMaquina(UUID player, String maquinaId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT quantia FROM compras_maquinas WHERE uuid = ? AND maquina_id = ?")) {
            ps.setString(1, player.toString());
            ps.setString(2, maquinaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("quantia");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao buscar compras: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void addCompraMaquina(UUID player, String maquinaId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO compras_maquinas (uuid, maquina_id, quantia) VALUES (?, ?, 1) " +
                "ON CONFLICT(uuid, maquina_id) DO UPDATE SET quantia = quantia + 1")) {
            ps.setString(1, player.toString());
            ps.setString(2, maquinaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao adicionar compra: " + e.getMessage());
        }
    }

    @Override
    public int getComprasCombustivel(UUID player, String combustivelId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT quantia FROM compras_combustiveis WHERE uuid = ? AND combustivel_id = ?")) {
            ps.setString(1, player.toString());
            ps.setString(2, combustivelId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("quantia");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao buscar compras combustível: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void addCompraCombustivel(UUID player, String combustivelId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO compras_combustiveis (uuid, combustivel_id, quantia) VALUES (?, ?, 1) " +
                "ON CONFLICT(uuid, combustivel_id) DO UPDATE SET quantia = quantia + 1")) {
            ps.setString(1, player.toString());
            ps.setString(2, combustivelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao adicionar compra combustível: " + e.getMessage());
        }
    }

    @Override
    public void saveBooster(BoosterData booster) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO boosters (uuid, multiplicador, expiracao) VALUES (?, ?, ?)")) {
            ps.setString(1, booster.getPlayer().toString());
            ps.setDouble(2, booster.getMultiplicador());
            ps.setLong(3, booster.getExpiracao());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar booster: " + e.getMessage());
        }
    }

    @Override
    public BoosterData loadBooster(UUID player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM boosters WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new BoosterData(player, rs.getDouble("multiplicador"), rs.getLong("expiracao"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar booster: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Map.Entry<UUID, Double>> getTopLimite(int limit) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, limite FROM limites WHERE limite > 0 ORDER BY limite DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    double limite = rs.getDouble("limite");
                    list.add(new AbstractMap.SimpleEntry<>(uuid, limite));
                } catch (Exception ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao buscar top limite: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Map.Entry<UUID, Double>> getTopCompradas(int limit) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, SUM(quantia) as total FROM compras_maquinas GROUP BY uuid ORDER BY total DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    double total = rs.getDouble("total");
                    list.add(new AbstractMap.SimpleEntry<>(uuid, total));
                } catch (Exception ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao buscar top compradas: " + e.getMessage());
        }
        return list;
    }

    private String serializeUUIDs(List<UUID> uuids) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : uuids) {
            if (sb.length() > 0) sb.append(",");
            sb.append(uuid.toString());
        }
        return sb.toString();
    }
}

