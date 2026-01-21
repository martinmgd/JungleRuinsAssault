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

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
    private com.badlogic.gdx.graphics.Texture texTest;

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
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        anims = new PlayerAnimations();
        jugador = new Jugador(anims);

        texTest = new com.badlogic.gdx.graphics.Texture("libgdx.png");

        limiteIzq = Constantes.ANCHO_MUNDO / 2f;
        limiteDer = anchoNivel - Constantes.ANCHO_MUNDO / 2f;

        // Coloca cámara y jugador en sitio visible seguro
        camara.position.set(Constantes.ANCHO_MUNDO / 2f, Constantes.ALTO_MUNDO / 2f, 0);
        camara.update();

        jugador.setX(Constantes.ANCHO_MUNDO / 2f);

        Gdx.app.log("PantallaJuego", "show OK");

        Gdx.app.log("ASSETS_TEST", "libgdx.png exists=" + Gdx.files.internal("libgdx.png").exists());
        Gdx.app.log("ASSETS_TEST", "idle_sheet exists=" + Gdx.files.internal("sprites/player/idle_sheet.png").exists());
        Gdx.app.log("ASSETS_TEST", "walk_sheet exists=" + Gdx.files.internal("sprites/player/walk_sheet.png").exists());

    }

    @Override
    public void render(float delta) {
        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;

        jugador.moverHorizontal(dir, delta);

        ScreenUtils.clear(1f, 0f, 0f, 1f); // Fondo rojo para confirmar que render se ejecuta

        viewport.apply();

        float objetivoX = jugador.getX() + (PlayerAnimations.FRAME_W / PPU) / 2f;
        camara.position.x = Math.max(limiteIzq, Math.min(objetivoX, limiteDer));
        camara.update();

        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();
        juego.batch.draw(texTest, 1f, 1f, 2f, 2f);
        jugador.draw(juego.batch, PPU);
        juego.batch.end();
    }


    @Override
    public void dispose() {
        anims.dispose();
        if (texTest != null) texTest.dispose();
    }
}
