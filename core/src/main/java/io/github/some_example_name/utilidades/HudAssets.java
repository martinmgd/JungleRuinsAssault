package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Clase responsable de cargar y mantener los recursos gráficos utilizados
 * por el HUD relacionados con los iconos de vida.
 *
 * Carga las texturas de corazón lleno y corazón vacío, crea sus regiones
 * correspondientes y gestiona su liberación de memoria.
 */
public class HudAssets implements Disposable {

    // Rutas de los archivos de textura utilizados por el HUD.
    private static final String HEART_FULL_PATH = "sprites/hud/corazon_lleno.png";
    private static final String HEART_EMPTY_PATH = "sprites/hud/corazon_vacio.png";

    // Texturas originales cargadas desde disco.
    private Texture texFull;
    private Texture texEmpty;

    // Regiones utilizadas para dibujar los iconos en el HUD.
    public TextureRegion heartFull;
    public TextureRegion heartEmpty;

    /**
     * Constructor que carga las texturas requeridas y crea sus regiones.
     */
    public HudAssets() {
        texFull = loadTexMust(HEART_FULL_PATH);
        texEmpty = loadTexMust(HEART_EMPTY_PATH);

        heartFull = new TextureRegion(texFull);
        heartEmpty = new TextureRegion(texEmpty);
    }

    /**
     * Carga una textura desde el sistema de archivos interno del juego.
     * Si el archivo no existe, se lanza una excepción para evitar que el
     * juego continúe sin un recurso obligatorio.
     *
     * @param path ruta interna del archivo de textura
     * @return textura cargada con filtrado configurado
     */
    private Texture loadTexMust(String path) {
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            throw new RuntimeException("NO EXISTE asset HUD: " + path);
        }
        Texture t = new Texture(fh);

        // Configuración de filtrado para mantener la apariencia pixel-art.
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        return t;
    }

    /**
     * Libera las texturas asociadas a los iconos del HUD.
     */
    @Override
    public void dispose() {
        if (texFull != null) texFull.dispose();
        if (texEmpty != null) texEmpty.dispose();
    }
}
