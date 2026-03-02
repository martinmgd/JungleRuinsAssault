package io.github.some_example_name.entidades.efectos;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * Clase responsable de gestionar los efectos visuales de impacto.
 *
 * Mantiene una colección de instancias activas de {@link EfectoImpacto},
 * permite generarlas, actualizarlas en cada frame y renderizarlas.
 */
public class GestorEfectos {

    // Lista de efectos de impacto activos.
    private final Array<EfectoImpacto> impactos = new Array<>();

    // Región de textura utilizada por todos los efectos de impacto.
    private final TextureRegion impactoRegion;

    // Tamaño del efecto de impacto en unidades del mundo.
    private float impactoW = 0.55f;
    private float impactoH = 0.55f;

    // Duración del efecto en segundos.
    private float impactoDuracion = 0.14f;

    /**
     * Constructor que recibe la región de textura utilizada para los impactos.
     *
     * @param impactoRegion región de textura que representa el efecto visual
     */
    public GestorEfectos(TextureRegion impactoRegion) {
        this.impactoRegion = impactoRegion;
    }

    /**
     * Permite configurar el tamaño y duración de los efectos de impacto.
     *
     * @param w        ancho del efecto
     * @param h        alto del efecto
     * @param duracion duración del efecto en segundos
     */
    public void setImpactoConfig(float w, float h, float duracion) {
        this.impactoW = w;
        this.impactoH = h;
        this.impactoDuracion = duracion;
    }

    /**
     * Crea una nueva instancia de efecto de impacto en una posición concreta.
     *
     * @param cx    coordenada X del centro del impacto
     * @param cy    coordenada Y del centro del impacto
     * @param color color aplicado al efecto
     */
    public void spawnImpacto(float cx, float cy, Color color) {
        impactos.add(new EfectoImpacto(
            impactoRegion,
            cx, cy,
            impactoW, impactoH,
            impactoDuracion,
            color
        ));
    }

    /**
     * Actualiza todos los efectos activos y elimina aquellos que han finalizado.
     *
     * @param delta tiempo transcurrido desde el último frame
     */
    public void update(float delta) {
        for (int i = impactos.size - 1; i >= 0; i--) {
            EfectoImpacto e = impactos.get(i);
            e.update(delta);
            if (e.isFinished()) impactos.removeIndex(i);
        }
    }

    /**
     * Renderiza todos los efectos de impacto activos.
     *
     * @param batch SpriteBatch utilizado para el renderizado
     */
    public void draw(SpriteBatch batch) {
        Color prev = new Color(batch.getColor());

        for (EfectoImpacto e : impactos) {
            e.draw(batch);
        }

        batch.setColor(prev);
    }
}
