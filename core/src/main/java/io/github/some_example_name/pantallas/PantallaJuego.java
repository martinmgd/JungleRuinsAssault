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
import io.github.some_example_name.entidades.PlayerAnimations;
import io.github.some_example_name.utilidades.Constantes;

public class PantallaJuego extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private PlayerAnimations anims;
    private Jugador jugador;

    private float anchoNivel = 50f;
    private float limiteIzq;
    private float limiteDer;

    // Pixels per world unit (ajusta si lo quieres más grande/pequeño)
    private static final float PPU = 32f;

    public PantallaJuego(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new FitViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.apply(true);

        anims = new PlayerAnimations();
        jugador = new Jugador(anims);

        limiteIzq = Constantes.ANCHO_MUNDO / 2f;
        limiteDer = anchoNivel - Constantes.ANCHO_MUNDO / 2f;

        // Coloca cámara y jugador en sitio visible seguro
        camara.position.set(Constantes.ANCHO_MUNDO / 2f, Constantes.ALTO_MUNDO / 2f, 0);
        camara.update();

        jugador.setX(Constantes.ANCHO_MUNDO / 2f);

        Gdx.app.log("PantallaJuego", "show OK");
    }

    @Override
    public void render(float delta) {
        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;

        jugador.moverHorizontal(dir, delta);

        float objetivoX = jugador.getX() + (PlayerAnimations.FRAME_W / PPU) / 2f;
        camara.position.x = Math.max(limiteIzq, Math.min(objetivoX, limiteDer));
        camara.update();

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f); // gris para ver mejor

        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        jugador.draw(juego.batch, PPU);
        juego.batch.end();
    }

    @Override
    public void dispose() {
        anims.dispose();
    }
}
