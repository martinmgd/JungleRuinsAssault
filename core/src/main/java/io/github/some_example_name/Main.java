package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.pantallas.PantallaJuego;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
/**
 * Clase principal del juego. Gestiona el SpriteBatch y el cambio entre pantallas.
 */
public class Main extends Game {

    /** SpriteBatch compartido por todas las pantallas. */
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new PantallaJuego(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
    }
}

