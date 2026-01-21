package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.entidades.Jugador;
import io.github.some_example_name.entidades.PlayerAnimations;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.ParallaxBackground;

public class PantallaJuego extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private PlayerAnimations anims;
    private Jugador jugador;

    private float anchoNivel = 50f;
    private float limiteIzq;
    private float limiteDer;

    // Pixels per world unit
    private static final float PPU = 64f;

    // TEST
    private Texture texTest;

    // Parallax
    private ParallaxBackground parallax;

    public PantallaJuego(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new com.badlogic.gdx.utils.viewport.ExtendViewport(
            Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara
        );

        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        anims = new PlayerAnimations();
        jugador = new Jugador(anims);

        texTest = new Texture("libgdx.png");

        // ✅ Parallax con tus rutas reales
        parallax = new ParallaxBackground(
            "sprites/fondos/CapaFondo.png",
            "sprites/fondos/CapaIntermedia3.png",
            "sprites/fondos/fondoAccion2 niveles.png"
        );
        parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

        // Limites de cámara (nivel finito)
        limiteIzq = Constantes.ANCHO_MUNDO / 2f;
        limiteDer = anchoNivel - Constantes.ANCHO_MUNDO / 2f;

        // Posición inicial de cámara y jugador
        camara.position.set(Constantes.ANCHO_MUNDO / 2f, Constantes.ALTO_MUNDO / 2f, 0);
        camara.update();

        jugador.setX(Constantes.ANCHO_MUNDO / 2f);

        // Logs útiles
        Gdx.app.log("PantallaJuego", "show OK");
        Gdx.app.log("ASSETS_TEST", "fondo exists=" +
            Gdx.files.internal("sprites/fondos/CapaFondo.png").exists());
        Gdx.app.log("ASSETS_TEST", "intermedia exists=" +
            Gdx.files.internal("sprites/fondos/CapaIntermedia3.png").exists());
        Gdx.app.log("ASSETS_TEST", "accion exists=" +
            Gdx.files.internal("sprites/fondos/fondoAccion2 niveles.png").exists());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (parallax != null) {
            parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
        }
    }

    @Override
    public void render(float delta) {
        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;

        //Para que te ponga la altura a la que está el personaje en la pantalla .Ajuste fino del suelo (DEBUG)
//        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
//            parallax.setGroundFrac(parallax.getGroundFrac() + 0.01f);
//            Gdx.app.log("GROUND", "groundFrac=" + parallax.getGroundFrac());
//        }
//        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
//            parallax.setGroundFrac(parallax.getGroundFrac() - 0.01f);
//            Gdx.app.log("GROUND", "groundFrac=" + parallax.getGroundFrac());
//        }


        jugador.moverHorizontal(dir, delta);

        ScreenUtils.clear(1f, 0f, 0f, 1f);

        viewport.apply();

        // Cámara sigue al jugador
        float objetivoX = jugador.getX() + (PlayerAnimations.FRAME_W / PPU) / 2f;
        camara.position.x = Math.max(limiteIzq, Math.min(objetivoX, limiteDer));
        camara.update();

        juego.batch.setProjectionMatrix(camara.combined);

        float cameraLeftX = camara.position.x - viewport.getWorldWidth() / 2f;

        juego.batch.begin();

        // 1) Parallax
        parallax.render(juego.batch, cameraLeftX, viewport.getWorldWidth());

        float groundY = parallax.getGroundY();
        jugador.setY(groundY);


        // test
        juego.batch.draw(texTest, 1f, 1f, 2f, 2f);

        // 2) Jugador encima
        jugador.draw(juego.batch, PPU);

        juego.batch.end();
    }

    @Override
    public void dispose() {
        anims.dispose();
        if (texTest != null) texTest.dispose();
        if (parallax != null) parallax.dispose();
    }
}
