package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.some_example_name.pantallas.PantallaMenu;
import io.github.some_example_name.utilidades.Idiomas;

public class Main extends Game {

    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Cargar bundle i18n al iniciar
        Idiomas.cargar();

        // Iniciar en el menú
        setScreen(new PantallaMenu(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
    }
}
