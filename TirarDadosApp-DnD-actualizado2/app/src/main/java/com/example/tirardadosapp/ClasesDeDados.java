package com.example.tirardadosapp;

import java.util.Random;

// Lógica reutilizable para realizar tiradas.
public class ClasesDeDados {

    public static String tirarDados(int caras, int cantidad) {
        if (caras <= 0 || cantidad <= 0) {
            return "No se puede realizar la tirada.";
        }

        Random random = new Random();
        int total = 0;
        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < cantidad; i++) {
            int valor = random.nextInt(caras) + 1;
            resultado.append("Dado ")
                    .append(i + 1)
                    .append(": ")
                    .append(valor)
                    .append("\n");
            total += valor;
        }

        resultado.append("\nTotal: ").append(total);
        return resultado.toString();
    }
}
