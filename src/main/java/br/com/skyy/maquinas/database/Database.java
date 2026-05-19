package br.com.skyy.maquinas.database;

public interface Database {
    void initialize();
    void close();

    // Máquinas
    void saveMaquina(br.com.skyy.maquinas.models.MaquinaColocada maquina);
    void deleteMaquina(String id);
    java.util.List<br.com.skyy.maquinas.models.MaquinaColocada> loadAllMaquinas();

    // Limites
    double getLimite(java.util.UUID player);
    void setLimite(java.util.UUID player, double limite);
    java.util.Map<java.util.UUID, Double> loadAllLimites();

    // Compras
    int getComprasMaquina(java.util.UUID player, String maquinaId);
    void addCompraMaquina(java.util.UUID player, String maquinaId);
    int getComprasCombustivel(java.util.UUID player, String combustivelId);
    void addCompraCombustivel(java.util.UUID player, String combustivelId);

    // Boosters
    void saveBooster(br.com.skyy.maquinas.models.BoosterData booster);
    br.com.skyy.maquinas.models.BoosterData loadBooster(java.util.UUID player);

    // Top
    java.util.List<java.util.Map.Entry<java.util.UUID, Double>> getTopLimite(int limit);
    java.util.List<java.util.Map.Entry<java.util.UUID, Double>> getTopCompradas(int limit);
}

