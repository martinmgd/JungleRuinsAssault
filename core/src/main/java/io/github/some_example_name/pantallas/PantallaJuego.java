package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.entidades.Jugador;
import io.github.some_example_name.utilidades.Constantes;

/**
 * Pantalla principal de juego: gestiona cámara, viewport, input (sondeo) y renderizado.
 */
public class PantallaJuego extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private Jugador jugador;

    private float limiteIzquierdoCamara;

    private float limiteDerechoCamara;

    private float anchoNivel;

    public PantallaJuego(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new FitViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.apply(true);

        jugador = new Jugador();

        anchoNivel = 50f;

        // Límites para que la cámara no se salga del nivel
        limiteIzquierdoCamara = Constantes.ANCHO_MUNDO / 2f;
        limiteDerechoCamara = anchoNivel - Constantes.ANCHO_MUNDO / 2f;

        Gdx.app.log("PantallaJuego", "show()");

        camara.position.set(Constantes.ANCHO_MUNDO / 2f, Constantes.ALTO_MUNDO / 2f, 0f);
        camara.update();
    }

    @Override
    public void render(float delta) {
        gestionarEntrada(delta);

        // Seguir al jugador (scroll lateral)
        float objetivoX = jugador.sprite.getX() + jugador.sprite.getWidth() / 2f;

        // La cámara no puede ir más allá de los límites del “nivel”
        camara.position.x = Math.max(limiteIzquierdoCamara, Math.min(objetivoX, limiteDerechoCamara));
        camara.update();

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        jugador.sprite.draw(juego.batch);
        juego.batch.end();
    }

    private void gestionarEntrada(float delta) {
        float direccion = 0f;

        // Teclado (Desktop)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direccion -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direccion += 1f;

        // Táctil simple provisional (mitad izquierda / mitad derecha), como en los apuntes
        if (Gdx.input.isTouched()) {
            if (Gdx.input.getX() <= Gdx.graphics.getWidth() / 2f) direccion = -1f;
            else direccion = 1f;
        }

        jugador.moverHorizontal(direccion, delta);
        // Evitar que el jugador salga por la izquierda
        if (jugador.sprite.getX() < 0f) {
            jugador.sprite.setX(0f);
        }

        // Evitar que el jugador salga por la derecha
        float maxX = anchoNivel - jugador.sprite.getWidth();
        if (jugador.sprite.getX() > maxX) {
            jugador.sprite.setX(maxX);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        Gdx.app.log("PantallaJuego", "resize(" + width + "," + height + ")");
    }

    @Override
    public void dispose() {
        jugador.dispose();
        Gdx.app.log("PantallaJuego", "dispose()");
    }
}
