package io.github.some_example_name.utilidades;

/** Constantes globales del juego (unidades de mundo, velocidades, etc.). */
public final class Constantes {

    /** Tamaño del mundo en unidades lógicas (como en los apuntes). */
    public static final float ANCHO_MUNDO = 10.8f;
    public static final float ALTO_MUNDO  = 7.2f;

    /** Tamaño aproximado del jugador en unidades del mundo. */
    public static final float JUGADOR_ANCHO = 1.0f;
    public static final float JUGADOR_ALTO  = 1.6f;

    /** Velocidad horizontal del jugador (unidades del mundo/segundo). */
    public static final float JUGADOR_VELOCIDAD = 4.0f;

    private Constantes() {}
}
