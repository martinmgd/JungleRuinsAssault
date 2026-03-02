package io.github.some_example_name.entidades.efectos;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class EfectoImpacto {

    // Región de textura que representa visualmente el impacto.
    // Suele provenir de ImpactoAssets y contiene el sprite generado o cargado.
    private final TextureRegion region;

    // Coordenadas del centro del impacto en el mundo.
    // Se usa centro en lugar de esquina para facilitar posicionamiento sobre el punto de colisión.
    private final float cx;
    private final float cy;

    // Dimensiones con las que se renderizará el impacto.
    // Permite reutilizar la misma textura para distintos tamaños.
    private final float w;
    private final float h;

    // Duración total del efecto en segundos.
    // Cuando el tiempo acumulado supera este valor, el efecto se considera terminado.
    private final float duracion;

    // Tiempo acumulado desde que se creó el efecto.
    // Se incrementa en cada update.
    private float t = 0f;

    // Color con el que se renderiza el efecto.
    // Se copia para evitar modificar la referencia externa.
    private final Color color;

    /*
     * Constructor del efecto de impacto.
     *
     * Parámetros:
     * - region: sprite del impacto.
     * - cx, cy: posición central donde aparecerá el efecto.
     * - w, h: tamaño de renderizado.
     * - duracion: tiempo total que el efecto permanecerá visible.
     * - color: color aplicado al dibujar (permite tintes o variaciones).
     */
    public EfectoImpacto(TextureRegion region, float cx, float cy, float w, float h, float duracion, Color color) {
        this.region = region;
        this.cx = cx;
        this.cy = cy;
        this.w = w;
        this.h = h;
        this.duracion = duracion;

        // Se crea una copia del color para evitar que cambios externos afecten a este efecto.
        this.color = new Color(color);
    }

    /*
     * Actualiza el estado interno del efecto.
     * Incrementa el tiempo transcurrido desde su creación.
     *
     * Este método suele llamarse una vez por frame desde el sistema de entidades o efectos.
     */
    public void update(float delta) {
        t += delta;
    }

    /*
     * Dibuja el efecto en pantalla usando el SpriteBatch proporcionado.
     *
     * El efecto se dibuja centrado en (cx, cy), por lo que se calcula la esquina inferior
     * restando la mitad del ancho y alto.
     */
    public void draw(SpriteBatch batch) {

        // Guarda el color actual del batch para restaurarlo después del render.
        Color prev = new Color(batch.getColor());

        // Aplica el color del efecto (puede ser blanco, tintado, etc.).
        batch.setColor(color);

        // Convierte la posición central en coordenadas de esquina inferior izquierda.
        float x = cx - w * 0.5f;
        float y = cy - h * 0.5f;

        // Renderiza la región con el tamaño especificado.
        batch.draw(region, x, y, w, h);

        // Restaura el color previo del batch para no afectar a otros renders.
        batch.setColor(prev);
    }

    /*
     * Indica si el efecto ya ha finalizado.
     * Se usa normalmente para eliminarlo de listas de efectos activos.
     */
    public boolean isFinished() {
        return t >= duracion;
    }
}
