package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.utilidades.Constantes;

/**
 * Representa al jugador. Usa Sprite para mantener estado interno (posición, tamaño, etc.).
 */
public class Jugador implements Disposable {

    private final Texture textura;
    public final Sprite sprite;

    public Jugador() {
        textura = new Texture("libgdx.png"); // placeholder temporal
        sprite = new Sprite(textura);
        sprite.setSize(Constantes.JUGADOR_ANCHO, Constantes.JUGADOR_ALTO);
        sprite.setPosition(1f, 1f);
    }

    /**
     * Mueve al jugador en el eje X usando delta time.
     * @param direccion -1 izquierda, 0 quieto, +1 derecha
     * @param delta tiempo transcurrido entre frames
     */
    public void moverHorizontal(float direccion, float delta) {
        sprite.translateX(direccion * Constantes.JUGADOR_VELOCIDAD * delta);
    }

    @Override
    public void dispose() {
        textura.dispose();
    }
}
