package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Clase encargada de cargar y almacenar los recursos gráficos utilizados
 * por el enemigo Pajaro.
 *
 * Gestiona las texturas correspondientes a los estados de ataque y muerte
 * del enemigo, así como las regiones de textura utilizadas para renderizarlas.
 */
public class PajaroAssets {

    // Textura utilizada para la animación o sprite del estado de ataque.
    private Texture ataqueTex;

    // Textura utilizada para el sprite del estado de muerte.
    private Texture muertoTex;

    // Región de textura correspondiente al estado de ataque.
    public final TextureRegion ataque;

    // Región de textura correspondiente al estado de muerte.
    public final TextureRegion muerto;

    /**
     * Constructor que carga las texturas desde disco, configura el filtrado
     * y crea las regiones utilizadas para el renderizado.
     */
    public PajaroAssets() {
        ataqueTex = new Texture("sprites/enemigos/pajaro/pajaro_attack.png");
        muertoTex = new Texture("sprites/enemigos/pajaro/pajaro_dead.png");

        // Configuración de filtrado para preservar la nitidez en gráficos pixel-art.
        ataqueTex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        muertoTex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        ataque = new TextureRegion(ataqueTex);
        muerto = new TextureRegion(muertoTex);
    }

    /**
     * Libera las texturas asociadas a este conjunto de recursos.
     */
    public void dispose() {
        ataqueTex.dispose();
        muertoTex.dispose();
    }
}
