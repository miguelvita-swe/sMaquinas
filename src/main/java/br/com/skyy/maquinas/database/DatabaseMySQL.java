package br.com.skyy.maquinas.database;

import br.com.skyy.maquinas.SMaquinas;
import br.com.skyy.maquinas.models.BoosterData;
import br.com.skyy.maquinas.models.MaquinaColocada;
import br.com.skyy.maquinas.utils.LocationSerializer;

import java.sql.*;
import java.util.*;

public class DatabaseMySQL implements Database {

    private final SMaquinas plugin;
    private Connection connection;

    public DatabaseMySQL(SMaquinas plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        String host = plugin.getConfig().getString("Database.IP", "localhost:3306");
        String[] hostParts = host.split(":");
        String ip = hostParts[0];
        int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 3306;
        String db = plugin.getConfig().getString("Database.DB", "smaquinas");
        String user = plugin.getConfig().getString("Database.User", "root");
        String pass = plugin.getConfig().getString("Database.Pass", "");

        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver"); // legacy fallback
            }
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + ip + ":" + port + "/" + db
                            + "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8",
                    user, pass);
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao conectar ao MySQL: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS maquinas (" +
                    "id VARCHAR(64) PRIMARY KEY," +
                    "tipo VARCHAR(64) NOT NULL," +
                    "dono VARCHAR(36) NOT NULL," +
                    "location TEXT NOT NULL," +
                    "combustivel DOUBLE DEFAULT 0," +
                    "drops DOUBLE DEFAULT 0," +
                    "quebrada TINYINT DEFAULT 0," +
                    "combustivel_infinito TINYINT DEFAULT 0," +
                    "capacidade_extra DOUBLE DEFAULT 0," +
                    "amigos TEXT DEFAULT ''," +
                    "upgrade_combustivel INT DEFAULT 0," +
                    "upgrade_drops INT DEFAULT 0," +
                    "upgrade_velocidade INT DEFAULT 0," +
                    "upgrade_durabilidade INT DEFAULT 0," +
                    "stack INT DEFAULT 1," +
                    "ativo TINYINT DEFAULT 1," +
                    "holo_ativo TINYINT DEFAULT 1," +
                    "trusteds TINYINT DEFAULT 0" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS limites (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "limite DOUBLE DEFAULT 0" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS compras_maquinas (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "maquina_id VARCHAR(64) NOT NULL," +
                    "quantia INT DEFAULT 0," +
                    "PRIMARY KEY(uuid, maquina_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS compras_combustiveis (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "combustivel_id VARCHAR(64) NOT NULL," +
                    "quantia INT DEFAULT 0," +
                    "PRIMARY KEY(uuid, combustivel_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS boosters (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "multiplicador DOUBLE DEFAULT 1," +
                    "expiracao BIGINT DEFAULT 0" +
                    ")");

            // Migração de colunas novas
            try { stmt.execute("ALTER TABLE maquinas ADD COLUMN ativo TINYINT DEFAULT 1"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE maquinas ADD COLUMN holo_ativo TINYINT DEFAULT 1"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE maquinas ADD COLUMN trusteds TINYINT DEFAULT 0"); } catch (Exception ignored) {}
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao fechar MySQL: " + e.getMessage());
        }
    }

    @Override
    public void saveMaquina(MaquinaColocada maquina) {
        String sql = "INSERT INTO maquinas (id, tipo, dono, location, combustivel, drops, quebrada, combustivel_infinito, capacidade_extra, amigos, upgrade_combustivel, upgrade_drops, upgrade_velocidade, upgrade_durabilidade, stack, ativo, holo_ativo, trusteds) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "combustivel=VALUES(combustivel), drops=VALUES(drops), quebrada=VALUES(quebrada), combustivel_infinito=VALUES(combustivel_infinito), " +
                "capacidade_extra=VALUES(capacidade_extra), amigos=VALUES(amigos), upgrade_combustivel=VALUES(upgrade_combustivel), upgrade_drops=VALUES(upgrade_drops), " +
                "upgrade_velocidade=VALUES(upgrade_velocidade), upgrade_durabilidade=VALUES(upgrade_durabilidade), stack=VALUES(stack), " +
                "ativo=VALUES(ativo), holo_ativo=VALUES(holo_ativo), trusteds=VALUES(trusteds)";
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
            plugin.getLogger().severe("Erro ao salvar máquina MySQL: " + e.getMessage());
        }
    }

    @Override
    public void deleteMaquina(String id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM maquinas WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao deletar máquina MySQL: " + e.getMessage());
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
                    plugin.getLogger().warning("Erro ao carregar máquina MySQL: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar máquinas MySQL: " + e.getMessage());
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
            plugin.getLogger().severe("Erro ao buscar limite MySQL: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void setLimite(UUID player, double limite) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO limites (uuid, limite) VALUES (?, ?) ON DUPLICATE KEY UPDATE limite=VALUES(limite)")) {
            ps.setString(1, player.toString());
            ps.setDouble(2, limite);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar limite MySQL: " + e.getMessage());
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
            plugin.getLogger().severe("Erro ao carregar limites MySQL: " + e.getMessage());
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
            plugin.getLogger().severe("Erro ao buscar compras MySQL: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void addCompraMaquina(UUID player, String maquinaId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO compras_maquinas (uuid, maquina_id, quantia) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE quantia = quantia + 1")) {
            ps.setString(1, player.toString());
            ps.setString(2, maquinaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao adicionar compra MySQL: " + e.getMessage());
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
            plugin.getLogger().severe("Erro ao buscar compras combustível MySQL: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void addCompraCombustivel(UUID player, String combustivelId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO compras_combustiveis (uuid, combustivel_id, quantia) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE quantia = quantia + 1")) {
            ps.setString(1, player.toString());
            ps.setString(2, combustivelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao adicionar compra combustível MySQL: " + e.getMessage());
        }
    }

    @Override
    public void saveBooster(BoosterData booster) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO boosters (uuid, multiplicador, expiracao) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE multiplicador=VALUES(multiplicador), expiracao=VALUES(expiracao)")) {
            ps.setString(1, booster.getPlayer().toString());
            ps.setDouble(2, booster.getMultiplicador());
            ps.setLong(3, booster.getExpiracao());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar booster MySQL: " + e.getMessage());
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
            plugin.getLogger().severe("Erro ao carregar booster MySQL: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Map.Entry<UUID, Double>> getTopLimite(int limit) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, limite FROM limites ORDER BY limite DESC LIMIT ?")) {
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
            plugin.getLogger().severe("Erro ao buscar top limite MySQL: " + e.getMessage());
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
            plugin.getLogger().severe("Erro ao buscar top compradas MySQL: " + e.getMessage());
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

