package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.entidades.efectos.GestorEfectos;
import io.github.some_example_name.entidades.enemigos.GestorEnemigos;
import io.github.some_example_name.entidades.enemigos.Golem;
import io.github.some_example_name.entidades.enemigos.Pajaro;
import io.github.some_example_name.entidades.enemigos.Serpiente;
import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.jugador.PlayerAnimations;
import io.github.some_example_name.entidades.proyectiles.jugador.AtaqueEspecial;
import io.github.some_example_name.entidades.proyectiles.jugador.GestorProyectiles;
import io.github.some_example_name.entidades.proyectiles.jugador.Proyectil;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.DisparoAssets;
import io.github.some_example_name.utilidades.ImpactoAssets;
import io.github.some_example_name.utilidades.ParallaxBackground;
import io.github.some_example_name.utilidades.VenenoAssets;

public class PantallaJuego extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private PlayerAnimations anims;
    private Jugador jugador;

    // Nivel más largo para dar sensación de recorrido
    // (el fondo tilea y con esto no “se acaba” el mundo)
    private float anchoNivel = 400f;

    private float limiteIzq;
    private float limiteDer;

    private static final float PPU = 64f;

    private ParallaxBackground parallax;

    private DisparoAssets disparoAssets;
    private GestorProyectiles gestorProyectiles;

    private ImpactoAssets impactoAssets;
    private GestorEfectos gestorEfectos;

    private GestorEnemigos gestorEnemigos;

    private Texture serpienteWalk;
    private Texture serpienteDeath;

    private Texture pajaroAttak;
    private Texture pajaroDeath;

    private Texture golemIdle;
    private Texture golemWalk;
    private Texture golemAttack;
    private Texture golemThrow;
    private Texture golemDeath;

    private Texture rocaTex;
    private TextureRegion rocaRegion;

    private VenenoAssets venenoAssets;

    private final Color colorImpactoNormal = new Color(1f, 0.6f, 0.15f, 1f);

    // Valores finales que calibraste
    private static final float GROUND_FRAC_FINAL = 0.45f;
    private static final float AJUSTE_SUELO_FINAL = -4.7f;

    // Offset que se aplica al suelo (se recalcula en resize para mantener el suelo objetivo)
    private float ajusteSueloY = AJUSTE_SUELO_FINAL;

    // Suelo objetivo en coordenadas de mundo (se fija una vez y luego se mantiene)
    private float sueloObjetivoY = 0f;

    // ------------------------------------------------------------
    // TEMPLO / RUINA (FIN DE NIVEL)
    // ------------------------------------------------------------
    private Texture temploTex;
    private TextureRegion temploRegion;
    private float temploWWorld = 0f;
    private float temploHWorld = 0f;

    // Ajustes pedidos:
    // - Bajar en Y el 90% del tamaño del personaje
    private static final float TEMPLO_BAJA_Y_FRAC_JUGADOR = 0.90f;

    // - Que el lado derecho quede fuera de pantalla un 20%
    private static final float TEMPLO_RIGHT_OVERFLOW_FRAC = 0.20f;

    public PantallaJuego(Main juego) {
        this.juego = juego;
    }

    private void setPixelArt(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    private float getSueloY() {
        return parallax.getGroundY() + ajusteSueloY;
    }

    // Recalcula ajusteSueloY para mantener el mismo sueloObjetivoY aunque cambie parallax.getGroundY() tras resize
    private void recalcularAjusteSueloParaMantenerObjetivo() {
        ajusteSueloY = sueloObjetivoY - parallax.getGroundY();
    }

    // Evita que el jugador se salga del nivel (izquierda/derecha)
    private void clampJugadorEnNivel() {
        float playerW = jugador.getWidth(PPU);

        float minX = 0f;
        float maxX = anchoNivel - playerW;

        if (maxX < minX) {
            // Por si algún día anchoNivel fuese más pequeño que el jugador
            maxX = minX;
        }

        float x = jugador.getX();
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;

        jugador.setX(x);
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        anims = new PlayerAnimations();
        jugador = new Jugador(anims);

        parallax = new ParallaxBackground(
            "sprites/fondos/capa_01.png",
            "sprites/fondos/capa_02.png",
            "sprites/fondos/capa_03.png",
            "sprites/fondos/capa_04.png"
        );

        parallax.setZoom(1.0f);
        parallax.setGroundFrac(GROUND_FRAC_FINAL);
        parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

        // Fijamos una vez el suelo objetivo usando tu calibración final
        sueloObjetivoY = parallax.getGroundY() + AJUSTE_SUELO_FINAL;
        recalcularAjusteSueloParaMantenerObjetivo();

        disparoAssets = new DisparoAssets();
        gestorProyectiles = new GestorProyectiles(disparoAssets);

        impactoAssets = new ImpactoAssets();
        gestorEfectos = new GestorEfectos(impactoAssets.impacto);
        gestorEfectos.setImpactoConfig(0.55f, 0.55f, 0.14f);

        // Serpiente
        serpienteWalk = new Texture("sprites/enemigos/serpiente/serpiente_walk.png");
        serpienteDeath = new Texture("sprites/enemigos/serpiente/serpiente_death.png");
        setPixelArt(serpienteWalk);
        setPixelArt(serpienteDeath);

        // Pájaro
        pajaroAttak = new Texture("sprites/enemigos/pajaro/pajaro_attack.png");
        pajaroDeath = new Texture("sprites/enemigos/pajaro/pajaro_dead.png");
        setPixelArt(pajaroAttak);
        setPixelArt(pajaroDeath);

        // Golem
        golemIdle   = new Texture("sprites/enemigos/golem/idle_sheet.png");
        golemWalk   = new Texture("sprites/enemigos/golem/walk_sheet.png");
        golemThrow  = new Texture("sprites/enemigos/golem/throw_sheet.png");
        golemAttack = new Texture("sprites/enemigos/golem/attack_sheet.png");
        golemDeath  = new Texture("sprites/enemigos/golem/death_sheet.png");

        setPixelArt(golemIdle);
        setPixelArt(golemWalk);
        setPixelArt(golemThrow);
        setPixelArt(golemAttack);
        setPixelArt(golemDeath);

        // Roca
        rocaTex = new Texture("sprites/proyectiles/enemigo/golem_roca.png");
        setPixelArt(rocaTex);
        rocaRegion = new TextureRegion(rocaTex);

        // ------------------------------------------------------------
        // TEMPLO / RUINA
        // ------------------------------------------------------------
        temploTex = new Texture("sprites/fondos/entradaRuina.png");
        setPixelArt(temploTex);
        temploRegion = new TextureRegion(temploTex);
        temploWWorld = temploTex.getWidth() / PPU;
        temploHWorld = temploTex.getHeight() / PPU;

        gestorEnemigos = new GestorEnemigos(serpienteWalk, serpienteDeath, pajaroAttak, pajaroDeath, PPU);

        gestorEnemigos.setGolemTextures(
            golemIdle,
            golemWalk,
            golemThrow,
            golemAttack,
            golemDeath
        );
        gestorEnemigos.setRocaRegion(rocaRegion);

        gestorEnemigos.setGolemSpawnConfig(
            4.0f,
            2,
            10f,
            anchoNivel - 10f
        );

        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        camara.position.set(viewW / 2f, viewport.getWorldHeight() / 2f, 0f);
        camara.update();

        // Colocación inicial ya con suelo fijo
        jugador.setX(viewW / 2f);
        jugador.setSueloY(getSueloY());
        jugador.setY(getSueloY());

        clampJugadorEnNivel();

        gestorEnemigos.setYsuelo(getSueloY());
        gestorEnemigos.setAnimacion(128, 80, 0.20f);
        gestorEnemigos.setStats(2.0f, 10);
        gestorEnemigos.setSpawnConfig(
            2.2f,
            6,
            10f,
            anchoNivel - 10f,
            2.5f
        );
        gestorEnemigos.setYOffsetWorld(0.15f);

        float yTopPantalla = getSueloY() + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(
            2.4f,
            2,
            yTopPantalla,
            0.8f,
            12.0f,
            12,
            0.60f
        );

        venenoAssets = new VenenoAssets();
        gestorEnemigos.setVenenoRegion(venenoAssets.veneno);

        gestorEnemigos.setAtaques(12, 0.9f, 8, 1.8f);
        gestorEnemigos.setVenenoConfig(2.2f, 7.5f, 10.0f, 0.35f, 0.35f);
    }

    @Override
    public void resize(int width, int height) {
        // Guardamos el suelo actual antes de recalcular, para recolocar al jugador sin “caída”
        float sueloAntes = (parallax != null) ? getSueloY() : 0f;

        viewport.update(width, height, true);

        if (parallax != null) {
            parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

            // Mantenemos el MISMO suelo objetivo tras el resize
            recalcularAjusteSueloParaMantenerObjetivo();
        }

        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        // Recoloca jugador al nuevo suelo manteniendo estabilidad
        float sueloDespues = getSueloY();
        float deltaSuelo = sueloDespues - sueloAntes;

        jugador.setSueloY(sueloDespues);
        jugador.setY(jugador.getY() + deltaSuelo);

        clampJugadorEnNivel();

        gestorEnemigos.setYsuelo(sueloDespues);

        float yTopPantalla = sueloDespues + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(
            2.4f,
            2,
            yTopPantalla,
            0.8f,
            12.0f,
            12,
            0.60f
        );
    }

    @Override
    public void render(float delta) {

        float h = jugador.getHeight(PPU);

        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;

        boolean saltar = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        boolean agacharse = Gdx.input.isKeyPressed(Input.Keys.S)
            || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        boolean disparaNormal = Gdx.input.isKeyJustPressed(Input.Keys.J);
        boolean disparaEspecial = Gdx.input.isKeyJustPressed(Input.Keys.K);

        jugador.setSueloY(getSueloY());
        jugador.aplicarEntrada(dir, saltar, agacharse, delta);

        // Importante: después de mover, cerramos al nivel
        clampJugadorEnNivel();

        gestorEnemigos.setYsuelo(getSueloY());

        if (disparaNormal || disparaEspecial) {

            boolean derechaDisparo = jugador.isMirandoDerecha();

            float sx = jugador.getMuzzleX(PPU);
            float sy = jugador.getMuzzleY(PPU);

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

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();

        float objetivoX = jugador.getX() + (PlayerAnimations.FRAME_W / PPU) / 2f;
        camara.position.x = Math.max(limiteIzq, Math.min(objetivoX, limiteDer));
        camara.update();

        juego.batch.setProjectionMatrix(camara.combined);

        float cameraLeftX = camara.position.x - viewport.getWorldWidth() / 2f;
        float viewW = viewport.getWorldWidth();

        gestorProyectiles.update(delta, cameraLeftX, viewW);

        gestorEnemigos.update(delta);

        float yTopPantalla = getSueloY() + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(
            2.4f,
            2,
            yTopPantalla,
            0.8f,
            12.0f,
            12,
            0.60f
        );

        gestorEnemigos.updateAtaques(delta, jugador, PPU, cameraLeftX, viewW);

        gestorEfectos.update(delta);

        for (Serpiente s : gestorEnemigos.getSerpientes()) {
            if (s.isDead()) continue;

            for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
                Proyectil p = gestorProyectiles.getNormales().get(i);
                if (p.isEliminar()) continue;

                if (s.getHitbox().overlaps(p.getHitbox())) {
                    s.recibirDanio(p.getDamage());

                    Rectangle hb = p.getHitbox();
                    float cy = hb.y + hb.height * 0.5f;

                    float frenteX = (p.getVx() >= 0f) ? (hb.x + hb.width) : hb.x;
                    float avance = 0.10f;
                    float shift = (p.getVx() >= 0f) ? avance : -avance;

                    gestorEfectos.spawnImpacto(frenteX + shift, cy, colorImpactoNormal);

                    p.marcarEliminar();
                }
            }
        }

        for (Pajaro b : gestorEnemigos.getPajaros()) {
            if (b.isDead()) continue;

            for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
                Proyectil p = gestorProyectiles.getNormales().get(i);
                if (p.isEliminar()) continue;

                if (b.getHitbox(PPU).overlaps(p.getHitbox())) {
                    b.recibirDanio(p.getDamage());

                    Rectangle hb = p.getHitbox();
                    float cy = hb.y + hb.height * 0.5f;

                    float frenteX = (p.getVx() >= 0f) ? (hb.x + hb.width) : hb.x;
                    float avance = 0.10f;
                    float shift = (p.getVx() >= 0f) ? avance : -avance;

                    gestorEfectos.spawnImpacto(frenteX + shift, cy, colorImpactoNormal);

                    p.marcarEliminar();
                }
            }
        }

        for (Golem g : gestorEnemigos.getGolems()) {
            if (g.isDead()) continue;

            for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
                Proyectil p = gestorProyectiles.getNormales().get(i);
                if (p.isEliminar()) continue;

                if (g.getHitbox().overlaps(p.getHitbox())) {
                    g.recibirDanio(p.getDamage());

                    Rectangle hb = p.getHitbox();
                    float cy = hb.y + hb.height * 0.5f;

                    float frenteX = (p.getVx() >= 0f) ? (hb.x + hb.width) : hb.x;
                    float avance = 0.10f;
                    float shift = (p.getVx() >= 0f) ? avance : -avance;

                    gestorEfectos.spawnImpacto(frenteX + shift, cy, colorImpactoNormal);

                    p.marcarEliminar();
                }
            }
        }

        AtaqueEspecial esp = gestorProyectiles.getEspecial();
        if (esp != null) {
            Rectangle hbRayo = esp.getHitbox();
            if (hbRayo != null) {
                for (Serpiente s : gestorEnemigos.getSerpientes()) {
                    if (s.isDead()) continue;
                    if (s.getHitbox().overlaps(hbRayo)) s.recibirDanio(esp.getDamage());
                }

                for (Pajaro b : gestorEnemigos.getPajaros()) {
                    if (b.isDead()) continue;
                    if (b.getHitbox(PPU).overlaps(hbRayo)) b.recibirDanio(esp.getDamage());
                }

                for (Golem g : gestorEnemigos.getGolems()) {
                    if (g.isDead()) continue;
                    if (g.getHitbox().overlaps(hbRayo)) g.recibirDanio(esp.getDamage());
                }
            }
        }

        juego.batch.begin();

        parallax.render(juego.batch, cameraLeftX, viewW);
        gestorProyectiles.draw(juego.batch, cameraLeftX, viewW);
        gestorEnemigos.render(juego.batch, cameraLeftX, viewW);
        gestorEfectos.draw(juego.batch);

        // TEMPLO: delante del fondo, antes del jugador (jugador queda delante)
        if (temploRegion != null) {
            float temploX = anchoNivel - (temploWWorld * (1f - TEMPLO_RIGHT_OVERFLOW_FRAC));
            float temploY = getSueloY() - (jugador.getHeight(PPU) * TEMPLO_BAJA_Y_FRAC_JUGADOR);

            juego.batch.draw(temploRegion, temploX, temploY, temploWWorld, temploHWorld);
        }

        jugador.draw(juego.batch, PPU);

        juego.batch.end();
    }

    @Override
    public void dispose() {
        if (anims != null) anims.dispose();
        if (parallax != null) parallax.dispose();
        if (disparoAssets != null) disparoAssets.dispose();
        if (impactoAssets != null) impactoAssets.dispose();

        if (serpienteWalk != null) serpienteWalk.dispose();
        if (serpienteDeath != null) serpienteDeath.dispose();

        if (pajaroAttak != null) pajaroAttak.dispose();
        if (pajaroDeath != null) pajaroDeath.dispose();

        if (golemIdle != null) golemIdle.dispose();
        if (golemWalk != null) golemWalk.dispose();
        if (golemThrow != null) golemThrow.dispose();
        if (golemAttack != null) golemAttack.dispose();
        if (golemDeath != null) golemDeath.dispose();

        if (rocaTex != null) rocaTex.dispose();

        if (venenoAssets != null) venenoAssets.dispose();

        if (temploTex != null) temploTex.dispose();
    }
}
