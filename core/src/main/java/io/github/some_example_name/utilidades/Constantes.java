package io.github.some_example_name.utilidades;

public class Constantes {

    // Base 4:3 para evitar que ExtendViewport meta "más cielo" en ventanas pequeñas
    // Dimensiones lógicas del mundo (unidades de mundo, no píxeles) usadas como referencia por viewports.
    // Al usar una relación 4:3 (16x12), se busca controlar el encuadre y evitar que ExtendViewport
    // muestre más área vertical (por ejemplo, "más cielo") en resoluciones/ventanas con aspect ratio distinto.
    public static final float ANCHO_MUNDO = 16f;
    public static final float ALTO_MUNDO  = 12f;

    // Pixels Per Meter (si no lo usas, no afecta)
    // Factor de conversión habitual en juegos 2D/Box2D para mapear píxeles a unidades físicas (metros).
    // Si el proyecto no lo emplea activamente en cálculos o físicas, mantenerlo no cambia el comportamiento.
    public static final float PPM = 32f;
}
