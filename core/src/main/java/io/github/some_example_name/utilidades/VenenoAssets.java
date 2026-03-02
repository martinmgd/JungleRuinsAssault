package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Clase encargada de generar y almacenar el recurso gráfico utilizado
 * para representar visualmente el veneno en el juego.
 *
 * La textura se crea de forma procedimental mediante un Pixmap en lugar
 * de cargarse desde un archivo externo.
 */
public class VenenoAssets implements Disposable {

    // Textura generada a partir del Pixmap.
    private final Texture tex;

    // Región de textura utilizada para renderizar el veneno.
    public final TextureRegion veneno;

    /**
     * Constructor que genera el gráfico del veneno mediante operaciones
     * de dibujo sobre un Pixmap y posteriormente crea la textura asociada.
     */
    public VenenoAssets() {

        // Creación de un Pixmap de 24x24 píxeles con canal alfa.
        Pixmap pm = new Pixmap(24, 24, Pixmap.Format.RGBA8888);

        // Limpieza inicial del Pixmap estableciendo un fondo completamente transparente.
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        // Círculo principal que representa el núcleo del proyectil de veneno.
        pm.setColor(0.25f, 1.0f, 0.25f, 1.0f);
        pm.fillCircle(12, 12, 8);

        // Contorno del círculo para aumentar el contraste visual.
        pm.setColor(0.10f, 0.60f, 0.10f, 1.0f);
        pm.drawCircle(12, 12, 8);

        // Detalle interno que simula brillo o reflejo en el veneno.
        pm.setColor(0.70f, 1.0f, 0.70f, 0.9f);
        pm.fillCircle(9, 15, 2);

        // Creación de la textura a partir del Pixmap generado.
        tex = new Texture(pm);

        // Liberación del Pixmap una vez creada la textura.
        pm.dispose();

        // Creación de la región de textura utilizada para el renderizado.
        veneno = new TextureRegion(tex);
    }

    /**
     * Libera la textura asociada al recurso gráfico.
     */
    @Override
    public void dispose() {
        tex.dispose();
    }
}
