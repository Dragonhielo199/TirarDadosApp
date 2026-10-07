package com.example.tirardadosapp;

// Tipos de dados disponibles en la aplicación.
public enum TipoDado {
    D4(4), D6(6), D8(8), D10(10), D12(12), D20(20), D100(100);

    private final int caras;

    TipoDado(int caras) {
        this.caras = caras;
    }

    public int getCaras() {
        return caras;
    }
}
