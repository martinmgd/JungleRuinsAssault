package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.entidades.Jugador;
import io.github.some_example_name.entidades.PlayerAnimations;
import io.github.some_example_name.entidades.GestorProyectiles;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.ParallaxBackground;
import io.github.some_example_name.utilidades.DisparoAssets;

public class PantallaJuego extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private PlayerAnimations anims;
    private Jugador jugador;

    private float anchoNivel = 50f;
    private float limiteIzq;
    private float limiteDer;

    private static final float PPU = 64f;

    private ParallaxBackground parallax;

    private DisparoAssets disparoAssets;
    private GestorProyectiles gestorProyectiles;

    public PantallaJuego(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        anims = new PlayerAnimations();
        jugador = new Jugador(anims);

        parallax = new ParallaxBackground(
            "sprites/fondos/CapaFondo.png",
            "sprites/fondos/CapaIntermedia3.png",
            "sprites/fondos/fondoAccion2_niveles.png"
        );
        parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

        disparoAssets = new DisparoAssets();
        gestorProyectiles = new GestorProyectiles(disparoAssets);

        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        camara.position.set(viewW / 2f, viewport.getWorldHeight() / 2f, 0f);
        camara.update();

        jugador.setX(viewW / 2f);

        jugador.setSueloY(parallax.getGroundY());
        jugador.setY(parallax.getGroundY());

        Gdx.app.log("PantallaJuego", "show OK");
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        if (parallax != null) {
            parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;
    }

    @Override
    public void render(float delta) {

        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;

        boolean saltar = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        boolean agacharse = Gdx.input.isKeyPressed(Input.Keys.S)
            || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        jugador.setSueloY(parallax.getGroundY());
        jugador.aplicarEntrada(dir, saltar, agacharse, delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();

        float objetivoX = jugador.getX() + (PlayerAnimations.FRAME_W / PPU) / 2f;
        camara.position.x = Math.max(limiteIzq, Math.min(objetivoX, limiteDer));
        camara.update();

        juego.batch.setProjectionMatrix(camara.combined);

        float cameraLeftX = camara.position.x - viewport.getWorldWidth() / 2f;
        float viewW = viewport.getWorldWidth();

        // Disparo normal
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            boolean derecha = jugador.isMirandoDerecha();
            float sx = jugador.getMuzzleX(PPU);
            float sy = jugador.getMuzzleY(PPU) - jugador.getHeight(PPU) * 0.10f;

            gestorProyectiles.shootNormal(sx, sy, derecha);
        }

        // Disparo especial (corriente hasta el borde y termina)
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            boolean derecha = jugador.isMirandoDerecha();
            float sx = jugador.getMuzzleX(PPU);

            float sy = jugador.getMuzzleY(PPU) - jugador.getHeight(PPU) * 0.40f;

            gestorProyectiles.shootEspecial(sx, sy, derecha, cameraLeftX, viewW);
        }

        // Si el especial está activo, su origen sigue al arma
        if (gestorProyectiles.isEspecialActivo()) {
            boolean derecha = jugador.isMirandoDerecha();
            float sx = jugador.getMuzzleX(PPU);
            float sy = jugador.getMuzzleY(PPU) - jugador.getHeight(PPU) * 0.40f;

            gestorProyectiles.setEspecialOrigin(sx, sy, derecha);
        }

        gestorProyectiles.update(delta, cameraLeftX, viewW);

        juego.batch.begin();

        parallax.render(juego.batch, cameraLeftX, viewW);

        gestorProyectiles.draw(juego.batch, cameraLeftX, viewW);

        jugador.draw(juego.batch, PPU);

        juego.batch.end();
    }

    @Override
    public void dispose() {
        if (anims != null) anims.dispose();
        if (parallax != null) parallax.dispose();
        if (disparoAssets != null) disparoAssets.dispose();
    }
}
