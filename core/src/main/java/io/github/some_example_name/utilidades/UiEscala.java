package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;

/**
 * Escalado unificado de UI (fuentes, iconos, etc.) para que en móvil no se vea gigante.
 *
 * Idea general del sistema de escalado:
 * - La escala se basa exclusivamente en la ALTURA real de pantalla en píxeles.
 * - Se evita usar density (dpi) porque en muchos móviles genera escalados excesivos.
 * - Se aplican límites mínimos y máximos para mantener la UI legible en pantallas
 *   extremadamente pequeñas o muy grandes.
 *
 * Este enfoque permite que todos los elementos de interfaz (fuentes, iconos, paneles)
 * mantengan proporciones consistentes entre distintas resoluciones.
 */
public final class UiEscala {

    // Altura de referencia usada durante el diseño de la UI.
    // Si la pantalla mide exactamente 720 píxeles de alto, la escala resultante será 1.0.
    // Este valor se eligió porque 720p ofrece un buen punto medio entre móviles y escritorio.
    private static final float REF_HEIGHT = 720f;

    // Límite inferior de la escala.
    // Evita que en pantallas pequeñas la UI se reduzca demasiado y pierda legibilidad.
    private static final float MIN_SCALE = 0.65f;

    // Límite superior de la escala.
    // Evita que en pantallas grandes (ej: tablets o monitores 1440p/4K)
    // la UI se vuelva exageradamente grande.
    private static final float MAX_SCALE = 1.65f;

    /*
     * Constructor privado para impedir instanciación.
     * Esta clase funciona como utilitario estático.
     */
    private UiEscala() { }

    /**
     * Devuelve un multiplicador de escala para UI basado en la altura real de pantalla.
     * NO usa density.
     *
     * Funcionamiento:
     * 1. Obtiene la altura actual de la ventana o pantalla.
     * 2. La compara con la altura de referencia (720 px).
     * 3. Calcula un factor proporcional.
     * 4. Aplica un clamp para evitar valores extremos.
     *
     * Ejemplos aproximados:
     *  - 720 px  -> escala 1.0
     *  - 1080 px -> escala ~1.5
     *  - 1440 px -> escala ~2.0 (pero limitada por MAX_SCALE)
     *
     * @return factor de escala final limitado por MIN_SCALE y MAX_SCALE.
     */
    public static float uiScale() {

        // Obtiene la altura actual de la pantalla.
        // Math.max evita división por cero en casos extremos.
        float h = Math.max(1f, (float) Gdx.graphics.getHeight());

        // Escala proporcional respecto a la referencia de diseño.
        float s = h / REF_HEIGHT;

        // Aplicación de límites para evitar valores fuera de rango razonable.
        if (s < MIN_SCALE) s = MIN_SCALE;
        if (s > MAX_SCALE) s = MAX_SCALE;

        return s;
    }

    /**
     * Escala final para fuentes basada en una escala base.
     *
     * La idea es que cada pantalla defina su "baseScale" según el tamaño visual
     * que se desea en la resolución de referencia. Luego se multiplica por la
     * escala global de UI para adaptarse automáticamente al dispositivo actual.
     *
     * Ejemplo de uso:
     *   font.getData().setScale(UiEscala.fontScale(1.4f));
     *
     * @param baseScale escala pensada para la resolución de referencia
     *                  (ejemplo: 1.4f para texto normal, 2.0f para títulos).
     * @return escala final adaptada al tamaño real de pantalla.
     */
    public static float fontScale(float baseScale) {
        return baseScale * uiScale();
    }
}
