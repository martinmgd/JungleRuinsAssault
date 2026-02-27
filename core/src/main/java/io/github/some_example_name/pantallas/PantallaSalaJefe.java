package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.entidades.enemigos.Jefe;
import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilMeteoro;
import io.github.some_example_name.entidades.proyectiles.jugador.GestorProyectiles;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.DisparoAssets;
import io.github.some_example_name.utilidades.Hud;

import java.util.ArrayList;

public class PantallaSalaJefe implements Screen {

    private static final float PPU_PLAYER = 64f;
    private static final float PPU_BOSS = 128f;

    private static final float SUELO_FRAC_SALA = 0.28f;

    // BAJAMOS el boss para que no quede a la altura de la cabeza
    private static final float BOSS_Y_OFFSET = -1.70f;

    // ✅ NUEVO: retraso para ver animación de muerte antes de cambiar de pantalla
    private boolean muerteIniciada = false;
    private float timerMuerte = 0f;
    private static final float RETARDO_MUERTE = 1.20f;

    private final Main game;
    private final Jugador jugador;

    private OrthographicCamera camara;
    private Viewport viewport;

    private Texture fondoSala;
    private Texture bossDormidoTex;
    private Texture bossDespiertoTex;
    private Texture meteoroTex;

    private Jefe jefe;
    private final ArrayList<ProyectilMeteoro> meteoros = new ArrayList<>();

    private float sueloY;

    private Hud hud;
    private float tiempo = 0f;
    private int score = 0;

    private DisparoAssets disparoAssets;
    private GestorProyectiles gestorProyectiles;

    // Música nivel 2
    private Music musicaJefe;

    public PantallaSalaJefe(Main game, Jugador jugador) {
        this.game = game;
        this.jugador = jugador;
    }

    public Jugador getJugador() {
        return jugador;
    }

    // ------------------------------------------------------------
    // Métodos para que PantallaPausaJefe pueda controlar la música
    // ------------------------------------------------------------
    public void pauseMusica() {
        if (musicaJefe != null && musicaJefe.isPlaying()) {
            musicaJefe.pause();
        }
    }

    public void resumeMusica() {
        if (musicaJefe != null && !musicaJefe.isPlaying()) {
            musicaJefe.play();
        }
    }

    public void stopMusica() {
        if (musicaJefe != null) {
            musicaJefe.stop();
            musicaJefe.dispose();
            musicaJefe = null;
        }
    }
    // ------------------------------------------------------------

    private void setPixelArt(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    @Override
    public void show() {

        // Iniciar música del jefe
        if (musicaJefe == null) {
            musicaJefe = Gdx.audio.newMusic(Gdx.files.internal("audio/nivel2.mp3"));
            musicaJefe.setLooping(true);
            musicaJefe.setVolume(0.7f);
            musicaJefe.play();
        } else if (!musicaJefe.isPlaying()) {
            musicaJefe.play();
        }

        // ✅ Reset muerte
        muerteIniciada = false;
        timerMuerte = 0f;

        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        fondoSala = new Texture("sprites/fondos/salaBoss2.png");
        bossDormidoTex = new Texture("sprites/enemigos/jefe/BossFinalDormido.png");
        bossDespiertoTex = new Texture("sprites/enemigos/jefe/BossFinal.png");
        meteoroTex = new Texture("sprites/proyectiles/enemigo/TresProyectile.png");

        setPixelArt(fondoSala);
        setPixelArt(bossDormidoTex);
        setPixelArt(bossDespiertoTex);
        setPixelArt(meteoroTex);

        ProyectilMeteoro.setTextura(meteoroTex);
        ProyectilMeteoro.setPPU(300f);

        sueloY = viewport.getWorldHeight() * SUELO_FRAC_SALA;

        jugador.setSueloY(sueloY);
        jugador.setY(sueloY);
        jugador.setX(viewport.getWorldWidth() * 0.10f);

        float bossW = bossDormidoTex.getWidth() / PPU_BOSS;
        float bossX = viewport.getWorldWidth() - bossW - 0.8f;
        if (bossX < 0.8f) bossX = 0.8f;

        float bossY = sueloY + BOSS_Y_OFFSET;

        jefe = new Jefe(
            bossDormidoTex,
            bossDespiertoTex,
            bossX,
            bossY,
            6.0f,
            300,
            1400L,
            2600L,
            3.0f,
            0.45f,
            1.6f,
            PPU_BOSS,
            PPU_PLAYER
        );

        disparoAssets = new DisparoAssets();
        gestorProyectiles = new GestorProyectiles(disparoAssets);

        hud = new Hud();
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        camara.position.set(
            viewport.getWorldWidth() / 2f,
            viewport.getWorldHeight() / 2f,
            0f
        );
        camara.update();
    }

    @Override
    public void render(float dt) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)) {

            // Al entrar en pausa: pausar música (no stop)
            pauseMusica();

            game.setScreen(new PantallaPausaJefe(game, this));
            return;
        }

        // ✅ Si está muerto, no seguimos update de gameplay: esperamos y saltamos
        if (jugador.isDead() || jugador.getVida() <= 0) {

            if (!muerteIniciada) {
                muerteIniciada = true;
                timerMuerte = 0f;
            }

            // Aun así dejamos que el jugador avance animación (aplicarEntrada ya la avanza con stateTime)
            jugador.aplicarEntrada(0f, false, false, dt);

            timerMuerte += dt;
            if (timerMuerte >= RETARDO_MUERTE) {
                game.setScreen(new PantallaMuerte(game));
            }

            // Dibujamos la escena para ver la animación
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            viewport.apply();
            game.batch.setProjectionMatrix(camara.combined);

            game.batch.begin();
            game.batch.draw(fondoSala, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
            if (jefe != null) jefe.draw(game.batch);
            for (ProyectilMeteoro m : meteoros) m.draw(game.batch);
            gestorProyectiles.draw(game.batch, 0f, viewport.getWorldWidth());
            jugador.draw(game.batch, PPU_PLAYER);
            game.batch.end();

            game.batch.begin();
            hud.setVida(jugador.getVida());
            hud.setScore(score);
            hud.setTiempoSeg(tiempo);
            hud.draw(game.batch);
            game.batch.end();

            return;
        }

        tiempo += dt;

        update(dt);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.batch.setProjectionMatrix(camara.combined);

        game.batch.begin();

        game.batch.draw(fondoSala, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());

        if (jefe != null) jefe.draw(game.batch);

        for (ProyectilMeteoro m : meteoros) m.draw(game.batch);

        gestorProyectiles.draw(game.batch, 0f, viewport.getWorldWidth());

        jugador.draw(game.batch, PPU_PLAYER);

        game.batch.end();

        game.batch.begin();
        hud.setVida(jugador.getVida());
        hud.setScore(score);
        hud.setTiempoSeg(tiempo);
        hud.draw(game.batch);
        game.batch.end();
    }

    private void update(float dt) {

        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;

        boolean saltar =
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        boolean agacharse =
            Gdx.input.isKeyPressed(Input.Keys.S) ||
                Gdx.input.isKeyPressed(Input.Keys.DOWN);

        boolean disparaNormal = Gdx.input.isKeyJustPressed(Input.Keys.J);
        boolean disparaEspecial = Gdx.input.isKeyJustPressed(Input.Keys.K);

        jugador.setSueloY(sueloY);
        jugador.aplicarEntrada(dir, saltar, agacharse, dt);

        float playerW = jugador.getWidth(PPU_PLAYER);
        float minX = 0f;
        float maxX = viewport.getWorldWidth() - playerW;

        float x = jugador.getX();
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;
        jugador.setX(x);

        if (disparaNormal || disparaEspecial) {

            boolean derechaDisparo = jugador.isMirandoDerecha();

            float sx = jugador.getMuzzleX(PPU_PLAYER);
            float sy = jugador.getMuzzleY(PPU_PLAYER);

            float h = jugador.getHeight(PPU_PLAYER);

            float NORMAL_STAND = h * 0.10f;
            float NORMAL_CROUCH = h * 0.29f;

            float SPECIAL_STAND = h * 0.05f;
            float SPECIAL_CROUCH = h * 0.29f;

            if (disparaNormal) {
                float syNormal = sy - NORMAL_STAND;
                if (agacharse) syNormal -= NORMAL_CROUCH;
                gestorProyectiles.shootNormal(sx, syNormal, derechaDisparo);
            }

            if (disparaEspecial) {
                float viewH = viewport.getWorldHeight();

                float syEspecial = sy + SPECIAL_STAND;
                if (agacharse) syEspecial -= SPECIAL_CROUCH;

                gestorProyectiles.startEspecial(sx, syEspecial, derechaDisparo, viewH);
            }
        }

        gestorProyectiles.update(dt, 0f, viewport.getWorldWidth());

        if (jefe != null) {
            jefe.update(dt, jugador, 0, 0, meteoros);
        }

        for (int i = meteoros.size() - 1; i >= 0; i--) {
            ProyectilMeteoro m = meteoros.get(i);
            m.update(dt);
            if (!m.isVivo()) meteoros.remove(i);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height);

        sueloY = viewport.getWorldHeight() * SUELO_FRAC_SALA;
        jugador.setSueloY(sueloY);
        if (jugador.getY() < sueloY) jugador.setY(sueloY);
    }

    @Override public void pause() {}

    @Override public void resume() {
        // Por seguridad, si vuelves al boss y no está sonando, reanuda
        resumeMusica();
    }

    @Override
    public void hide() {
        // Si sales del boss por cualquier vía (menú, muerte, victoria, etc)
        stopMusica();
    }

    @Override public void dispose() {
        stopMusica();

        if (fondoSala != null) fondoSala.dispose();
        if (bossDormidoTex != null) bossDormidoTex.dispose();
        if (bossDespiertoTex != null) bossDespiertoTex.dispose();
        if (meteoroTex != null) meteoroTex.dispose();
        if (hud != null) hud.dispose();
        if (disparoAssets != null) disparoAssets.dispose();
    }
}
