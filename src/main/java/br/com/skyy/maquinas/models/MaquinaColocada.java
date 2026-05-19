package br.com.skyy.maquinas.models;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaquinaColocada {

    private final String id;
    private final String tipoMaquina;
    private final UUID dono;
    private Location location;
    private double combustivel;
    private double drops;
    private boolean quebrada;
    private boolean combustivelInfinito;
    private double capacidadeExtra;
    private final List<UUID> amigos;
    private boolean ativo;
    private boolean holoAtivo;
    private boolean trusteds;

    // Níveis de upgrade: 0 = sem upgrade
    private int upgradeCombutivel;
    private int upgradeDrops;
    private int upgradeVelocidade;
    private int upgradeDurabilidade;

    // Stack
    private int stack;

    public MaquinaColocada(String id, String tipoMaquina, UUID dono, Location location) {
        this.id = id;
        this.tipoMaquina = tipoMaquina;
        this.dono = dono;
        this.location = location;
        this.combustivel = 0;
        this.drops = 0;
        this.quebrada = false;
        this.combustivelInfinito = false;
        this.capacidadeExtra = 0;
        this.amigos = new ArrayList<>();
        this.upgradeCombutivel = 0;
        this.upgradeDrops = 0;
        this.upgradeVelocidade = 0;
        this.upgradeDurabilidade = 0;
        this.stack = 1;
        this.ativo = true;
        this.holoAtivo = true;
        this.trusteds = false;
    }

    public String getId() { return id; }
    public String getTipoMaquina() { return tipoMaquina; }
    public UUID getDono() { return dono; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public double getCombustivel() { return combustivel; }
    public void setCombustivel(double combustivel) { this.combustivel = combustivel; }
    public double getDrops() { return drops; }
    public void setDrops(double drops) { this.drops = drops; }
    public boolean isQuebrada() { return quebrada; }
    public void setQuebrada(boolean quebrada) { this.quebrada = quebrada; }
    public boolean getCombustivelInfinito() { return combustivelInfinito; }
    public void setCombustivelInfinito(boolean combustivelInfinito) { this.combustivelInfinito = combustivelInfinito; }
    public double getCapacidadeExtra() { return capacidadeExtra; }
    public void setCapacidadeExtra(double capacidadeExtra) { this.capacidadeExtra = capacidadeExtra; }
    public List<UUID> getAmigos() { return amigos; }
    public int getUpgradeCombutivel() { return upgradeCombutivel; }
    public void setUpgradeCombutivel(int v) { this.upgradeCombutivel = v; }
    public int getUpgradeDrops() { return upgradeDrops; }
    public void setUpgradeDrops(int v) { this.upgradeDrops = v; }
    public int getUpgradeVelocidade() { return upgradeVelocidade; }
    public void setUpgradeVelocidade(int v) { this.upgradeVelocidade = v; }
    public int getUpgradeDurabilidade() { return upgradeDurabilidade; }
    public void setUpgradeDurabilidade(int v) { this.upgradeDurabilidade = v; }
    public int getStack() { return stack; }
    public void setStack(int stack) { this.stack = stack; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isHoloAtivo() { return holoAtivo; }
    public void setHoloAtivo(boolean holoAtivo) { this.holoAtivo = holoAtivo; }
    public boolean isTrusteds() { return trusteds; }
    public void setTrusteds(boolean trusteds) { this.trusteds = trusteds; }

    public boolean isAmigo(UUID uuid) {
        return amigos.contains(uuid);
    }

    public boolean temAcesso(UUID uuid) {
        return dono.equals(uuid) || isAmigo(uuid);
    }
}

