package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Representa un elemento de fondo estático (ruinas) que se renderiza
 * con desplazamiento de parallax respecto a la cámara.
 *
 * Gestiona la carga de la textura, su posición en el mundo y el factor
 * de desplazamiento relativo aplicado durante el renderizado.
 */
public class Ruinas implements Disposable {

    // Textura utilizada para representar las ruinas en pantalla.
    private final Texture tex;

    // Posición del objeto en coordenadas del mundo.
    private float worldX;
    private float worldY;

    // Factor de parallax aplicado al desplazamiento horizontal.
    // Valores menores que 1 producen desplazamiento más lento que la cámara.
    private float parallaxFactor = 0.70f;

    /**
     * Constructor que carga la textura de las ruinas y establece su posición inicial.
     *
     * @param path   ruta del archivo de textura
     * @param worldX posición horizontal en el mundo
     * @param worldY posición vertical en el mundo
     */
    public Ruinas(String path, float worldX, float worldY) {
        tex = new Texture(path);

        // Configuración del filtrado para preservar la nitidez en gráficos pixel-art.
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Configuración de wrapping para evitar repetición fuera de los bordes de la textura.
        tex.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

        this.worldX = worldX;
        this.worldY = worldY;
    }

    /**
     * Renderiza el elemento aplicando el desplazamiento de parallax
     * en función de la posición de la cámara.
     *
     * @param batch       SpriteBatch utilizado para el renderizado
     * @param cameraLeftX posición horizontal del borde izquierdo de la cámara
     */
    public void render(SpriteBatch batch, float cameraLeftX) {
        float drawX = worldX - cameraLeftX * parallaxFactor;
        batch.draw(tex, drawX, worldY);
    }

    /**
     * Define el factor de parallax aplicado al desplazamiento.
     *
     * @param factor valor del factor de parallax
     */
    public void setParallaxFactor(float factor) { this.parallaxFactor = factor; }

    // Métodos de acceso a la posición del objeto en el mundo.
    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }
    public void setWorldX(float x) { this.worldX = x; }
    public void setWorldY(float y) { this.worldY = y; }

    /**
     * Libera la textura asociada a este recurso gráfico.
     */
    @Override
    public void dispose() {
        tex.dispose();
    }
}
