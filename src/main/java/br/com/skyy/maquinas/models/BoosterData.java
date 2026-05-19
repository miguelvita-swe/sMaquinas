package br.com.skyy.maquinas.models;

import java.util.UUID;

public class BoosterData {

    private final UUID player;
    private double multiplicador;
    private long expiracao; // timestamp em milissegundos

    public BoosterData(UUID player, double multiplicador, long expiracao) {
        this.player = player;
        this.multiplicador = multiplicador;
        this.expiracao = expiracao;
    }

    public UUID getPlayer() { return player; }
    public double getMultiplicador() { return multiplicador; }
    public void setMultiplicador(double multiplicador) { this.multiplicador = multiplicador; }
    public long getExpiracao() { return expiracao; }
    public void setExpiracao(long expiracao) { this.expiracao = expiracao; }

    public boolean isAtivo() {
        return System.currentTimeMillis() < expiracao;
    }

    public long getTempoRestante() {
        return Math.max(0, expiracao - System.currentTimeMillis());
    }
}

