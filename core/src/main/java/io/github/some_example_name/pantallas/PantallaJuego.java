package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// ✅ NUEVO (solo para controles táctiles visibles)
import com.badlogic.gdx.utils.viewport.ScreenViewport;

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
import io.github.some_example_name.utilidades.Configuracion;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.DisparoAssets;
import io.github.some_example_name.utilidades.Hud;
import io.github.some_example_name.utilidades.ImpactoAssets;
import io.github.some_example_name.utilidades.ParallaxBackground;
import io.github.some_example_name.utilidades.VenenoAssets;

// ✅ Controles táctiles (ruta según tu proyecto)
import io.github.some_example_name.entidades.entidades.controles.ControlesTactiles;

public class PantallaJuego extends ScreenAdapter {

    // ------------------------------------------------------------
    // MÚSICA
    // ------------------------------------------------------------
    private Music musicaNivel;

    private boolean inicializado = false;
    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private boolean muerteEnCurso = false;
    private float tiempoMuerte = 0f;

    // Ajusta esto al tiempo real de tu animación dead (ej: 1.0s, 1.2s, etc.)
    private static final float DURACION_ANIM_DEAD = 1.2f;

    private PlayerAnimations anims;
    private Jugador jugador;

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

    private static final float GROUND_FRAC_FINAL = 0.45f;
    private static final float AJUSTE_SUELO_FINAL = -4.7f;

    private float ajusteSueloY = AJUSTE_SUELO_FINAL;
    private float sueloObjetivoY = 0f;

    private static final float SUELO_ENTIDADES_UP_BASE = 0.90f;

    // ------------------------------------------------------------
    // TEMPLO
    // ------------------------------------------------------------
    private Texture temploTex;
    private TextureRegion temploRegion;

    private float temploW;
    private float temploH;

    private float temploX;
    private float temploY;

    private static final float TEMPLO_OUT_RIGHT_FRAC = 0.20f;
    private static final float TEMPLO_VIEWPORT_H_FRAC = 0.72f;

    private float puertaX0;
    private float puertaX1;

    private float limiteEnemigosDerecha;

    private float alphaJugador = 1f;

    // Trigger borde derecho (fade antes, transición al final)
    private float fadeStartX;
    private float fadeEndX;

    // ------------------------------------------------------------
    // HUD + TIEMPO + PUNTUACIÓN
    // ------------------------------------------------------------
    private Hud hud;
    private int score = 0;

    private float tiempoPartida = 0f;
    private boolean nivelTerminado = false;
    private boolean enPausa = false;

    private static final int PUNTOS_PAJARO = 50;
    private static final int PUNTOS_SERPIENTE = 100;
    private static final int PUNTOS_GOLEM = 200;

    // Bonus por tiempo
    private static final float TIEMPO_OBJETIVO_SEG = 90f;
    private static final int BONUS_MAX_TIEMPO = 1000;

    private boolean bonusTiempoAplicado = false;
    private int bonusTiempo = 0;

    // Hits para matar
    private static final int HITS_PAJARO = 1;
    private static final int HITS_SERPIENTE = 3;
    private static final int HITS_GOLEM = 5;

    private final ObjectIntMap<Pajaro> hitsPajaro = new ObjectIntMap<>();
    private final ObjectIntMap<Serpiente> hitsSerpiente = new ObjectIntMap<>();
    private final ObjectIntMap<Golem> hitsGolem = new ObjectIntMap<>();

    private final ObjectSet<Pajaro> puntuadoPajaro = new ObjectSet<>();
    private final ObjectSet<Serpiente> puntuadoSerpiente = new ObjectSet<>();
    private final ObjectSet<Golem> puntuadoGolem = new ObjectSet<>();

    // ------------------------------------------------------------
    // DROPS: corazón cada 10 enemigos
    // ------------------------------------------------------------
    private static final int VIDA_POR_CORAZON = 20;
    private int enemigosMatados = 0;

    private TextureRegion heartDropRegion;
    private final Array<HeartDrop> heartDrops = new Array<>();
    private final Rectangle hbJugadorTmp = new Rectangle();

    // ------------------------------------------------------------
    // ✅ CONTROLES TÁCTILES (SOLO MÓVIL)
    // ------------------------------------------------------------
    private boolean esMovil = false;
    private ControlesTactiles controles;

    // ✅ NUEVO: viewport de controles en coordenadas de pantalla (para que se vean siempre)
    private Viewport controlesViewport;
    private OrthographicCamera controlesCam;

    // Para detectar "justPressed" en botones táctiles
    private boolean prevSaltarTouch = false;
    private boolean prevDispararTouch = false;
    private boolean prevEspecialTouch = false;
    private boolean prevPausaTouch = false;

    private static class HeartDrop {
        float x, y;
        float vy;
        boolean enSuelo = false;
        boolean eliminar = false;

        float w = 0.55f;
        float h = 0.55f;

        float sueloY;

        HeartDrop(float x, float y, float sueloY) {
            this.x = x;
            this.y = y;
            this.sueloY = sueloY;
            this.vy = 0f;
        }

        void update(float delta) {
            if (eliminar) return;

            if (!enSuelo) {
                vy += (-18f) * delta;
                y += vy * delta;

                if (y <= sueloY) {
                    y = sueloY;
                    vy = 0f;
                    enSuelo = true;
                }
            }
        }

        Rectangle getHitbox(Rectangle out) {
            out.set(x - w * 0.5f, y, w, h);
            return out;
        }
    }

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

    private float getSueloEntidadesUpWorld() {
        if (viewport == null) return SUELO_ENTIDADES_UP_BASE;
        float wh = viewport.getWorldHeight();
        if (wh <= 0.0001f) return SUELO_ENTIDADES_UP_BASE;
        return SUELO_ENTIDADES_UP_BASE * (Constantes.ALTO_MUNDO / wh);
    }

    private float getSueloEntidadesY() {
        return getSueloY() + getSueloEntidadesUpWorld();
    }

    private void recalcularAjusteSueloParaMantenerObjetivo() {
        ajusteSueloY = sueloObjetivoY - parallax.getGroundY();
    }

    private void clampJugadorEnNivel() {
        float playerW = jugador.getWidth(PPU);

        float minX = 0f;
        float maxX = anchoNivel - playerW;

        if (maxX < minX) maxX = minX;

        float x = jugador.getX();
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;

        jugador.setX(x);
    }

    private void configurarTemplo() {
        if (temploTex == null || jugador == null || viewport == null) return;

        float texW = temploTex.getWidth() / PPU;
        float texH = temploTex.getHeight() / PPU;

        float desiredH = viewport.getWorldHeight() * TEMPLO_VIEWPORT_H_FRAC;
        float scale = desiredH / Math.max(0.0001f, texH);

        temploH = texH * scale;
        temploW = texW * scale;

        temploX = anchoNivel - temploW * (1f - TEMPLO_OUT_RIGHT_FRAC);

        // ✅ VALOR “BUENO” (el que ya tenías bien): anclado al suelo, NO alto
        temploY = getSueloY() - 1.11f;

        puertaX0 = temploX + temploW * 0.47f;
        puertaX1 = temploX + temploW * 0.63f;

        limiteEnemigosDerecha = temploX;
        gestorEnemigos.setLimiteDerecha(limiteEnemigosDerecha);

        // Fade al borde derecho real
        float playerW = jugador.getWidth(PPU);
        fadeEndX = anchoNivel - playerW;           // margen derecho real
        fadeStartX = fadeEndX - playerW * 1.2f;    // empieza antes
        if (fadeStartX < 0f) fadeStartX = 0f;
    }

    private void actualizarAlphaEntradaTemplo() {
        float x = jugador.getX();

        if (x <= fadeStartX) {
            alphaJugador = 1f;
            return;
        }
        if (x >= fadeEndX) {
            alphaJugador = 0f;
            return;
        }

        float t = (x - fadeStartX) / Math.max(0.0001f, (fadeEndX - fadeStartX));
        t = Math.max(0f, Math.min(1f, t));
        alphaJugador = 1f - t;
    }

    private void aplicarBonusTiempoSiProcede() {
        if (bonusTiempoAplicado) return;

        float ratio = (TIEMPO_OBJETIVO_SEG - tiempoPartida) / Math.max(0.0001f, TIEMPO_OBJETIVO_SEG);
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;

        bonusTiempo = Math.round(ratio * BONUS_MAX_TIEMPO);
        score += bonusTiempo;

        bonusTiempoAplicado = true;

        if (hud != null) hud.showBonusTiempo(bonusTiempo);
    }

    private void onEnemyKilled(float x, float y) {
        enemigosMatados++;

        if (enemigosMatados % 10 == 0) {
            float suelo = getSueloEntidadesY();
            HeartDrop d = new HeartDrop(x, Math.max(y, suelo + 0.6f), suelo);
            heartDrops.add(d);
        }
    }

    private void curarJugador(int amount) {
        jugador.recibirDanio(-Math.abs(amount));
    }

    private void updateHeartDrops(float delta) {
        if (heartDrops.size == 0) return;

        float jx = jugador.getX();
        float jy = jugador.getY();
        float jw = jugador.getWidth(PPU);
        float jh = jugador.getHeight(PPU);
        hbJugadorTmp.set(jx, jy, jw, jh);

        Rectangle hbDropTmp = new Rectangle();

        for (int i = heartDrops.size - 1; i >= 0; i--) {
            HeartDrop d = heartDrops.get(i);
            d.update(delta);

            if (d.eliminar) {
                heartDrops.removeIndex(i);
                continue;
            }

            Rectangle hbD = d.getHitbox(hbDropTmp);
            if (hbD.overlaps(hbJugadorTmp)) {

                // VIBRACIÓN CORTA AL RECOGER DROP
                if (Configuracion.isVibracionActivada() &&
                    Gdx.input.isPeripheralAvailable(com.badlogic.gdx.Input.Peripheral.Vibrator)) {
                    Gdx.input.vibrate(30);
                }

                d.eliminar = true;
                curarJugador(VIDA_POR_CORAZON);
                heartDrops.removeIndex(i);
            }
        }
    }

    private void drawHeartDrops() {
        if (heartDrops.size == 0 || heartDropRegion == null) return;

        for (int i = 0; i < heartDrops.size; i++) {
            HeartDrop d = heartDrops.get(i);
            float drawX = d.x - d.w * 0.5f;
            float drawY = d.y;
            juego.batch.draw(heartDropRegion, drawX, drawY, d.w, d.h);
        }
    }

    private void pararMusicaNivel() {
        if (musicaNivel != null) {
            musicaNivel.stop();
            musicaNivel.dispose();
            musicaNivel = null;
        }
    }

    // ------------------------------------------------------------
    // ✅ CONTROL AUDIO (SONIDO ON/OFF)
    // ------------------------------------------------------------
    public void syncMusicaSegunConfiguracion() {
        if (muerteEnCurso) return;

        if (enPausa) {
            if (musicaNivel != null && musicaNivel.isPlaying()) {
                musicaNivel.pause();
            }
            return;
        }

        boolean sonidoOn = Configuracion.isSonidoActivado();

        if (!sonidoOn) {
            pararMusicaNivel();
            return;
        }

        if (musicaNivel == null) {
            musicaNivel = Gdx.audio.newMusic(Gdx.files.internal("audio/nivel1.mp3"));
            musicaNivel.setLooping(true);
            musicaNivel.setVolume(0.3f);
            musicaNivel.play();
        } else if (!musicaNivel.isPlaying()) {
            musicaNivel.play();
        }
    }

    public void setEnPausa(boolean value) {
        this.enPausa = value;
    }

    @Override
    public void show() {

        syncMusicaSegunConfiguracion();

        if (inicializado) return;
        inicializado = true;

        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // ------------------------------------------------------------
        // ✅ Detectar móvil y activar controles táctiles automáticamente
        // ------------------------------------------------------------
        esMovil = (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android)
            || (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS);

        // ✅ NUEVO: viewport de controles en coordenadas de pantalla
        if (esMovil) {
            controlesCam = new OrthographicCamera();
            controlesViewport = new ScreenViewport(controlesCam);
            controlesViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

            controles = new ControlesTactiles(controlesViewport);
            Gdx.input.setInputProcessor(controles);

            prevSaltarTouch = false;
            prevDispararTouch = false;
            prevEspecialTouch = false;
            prevPausaTouch = false;
        }

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

        sueloObjetivoY = parallax.getGroundY() + AJUSTE_SUELO_FINAL;
        recalcularAjusteSueloParaMantenerObjetivo();

        disparoAssets = new DisparoAssets();
        gestorProyectiles = new GestorProyectiles(disparoAssets);

        impactoAssets = new ImpactoAssets();
        gestorEfectos = new GestorEfectos(impactoAssets.impacto);
        gestorEfectos.setImpactoConfig(0.55f, 0.55f, 0.14f);

        serpienteWalk = new Texture("sprites/enemigos/serpiente/serpiente_walk.png");
        serpienteDeath = new Texture("sprites/enemigos/serpiente/serpiente_death.png");
        setPixelArt(serpienteWalk);
        setPixelArt(serpienteDeath);

        pajaroAttak = new Texture("sprites/enemigos/pajaro/pajaro_attack.png");
        pajaroDeath = new Texture("sprites/enemigos/pajaro/pajaro_dead.png");
        setPixelArt(pajaroAttak);
        setPixelArt(pajaroDeath);

        golemIdle = new Texture("sprites/enemigos/golem/idle_sheet.png");
        golemWalk = new Texture("sprites/enemigos/golem/walk_sheet.png");
        golemThrow = new Texture("sprites/enemigos/golem/throw_sheet.png");
        golemAttack = new Texture("sprites/enemigos/golem/attack_sheet.png");
        golemDeath = new Texture("sprites/enemigos/golem/death_sheet.png");

        setPixelArt(golemIdle);
        setPixelArt(golemWalk);
        setPixelArt(golemThrow);
        setPixelArt(golemAttack);
        setPixelArt(golemDeath);

        rocaTex = new Texture("sprites/proyectiles/enemigo/golem_roca.png");
        setPixelArt(rocaTex);
        rocaRegion = new TextureRegion(rocaTex);

        gestorEnemigos = new GestorEnemigos(serpienteWalk, serpienteDeath, pajaroAttak, pajaroDeath, PPU);
        gestorEnemigos.setGolemTextures(golemIdle, golemWalk, golemThrow, golemAttack, golemDeath);
        gestorEnemigos.setRocaRegion(rocaRegion);

        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        camara.position.set(viewW / 2f, viewport.getWorldHeight() / 2f, 0f);
        camara.update();

        jugador.setX(viewW / 2f);
        jugador.setSueloY(getSueloEntidadesY());
        jugador.setY(getSueloEntidadesY());

        temploTex = new Texture("sprites/fondos/entradaRuina.png");
        setPixelArt(temploTex);
        temploRegion = new TextureRegion(temploTex);

        gestorEnemigos.setYsuelo(getSueloEntidadesY());
        gestorEnemigos.setAnimacion(128, 80, 0.20f);
        gestorEnemigos.setStats(2.0f, 10);

        Configuracion.Dificultad d = Configuracion.getDificultad();
        if (d == Configuracion.Dificultad.FACIL) {
            gestorEnemigos.setSpawnConfig(3.5f, 3, 2f, anchoNivel - 2f, 3.5f);
        } else {
            gestorEnemigos.setSpawnConfig(2.2f, 6, 2f, anchoNivel - 2f, 2.5f);
        }

        gestorEnemigos.setYOffsetWorld(0.15f);

        float yTopPantalla = getSueloEntidadesY() + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(2.4f, 2, yTopPantalla, 0.8f, 12.0f, 12, 0.60f);

        venenoAssets = new VenenoAssets();
        gestorEnemigos.setVenenoRegion(venenoAssets.veneno);

        gestorEnemigos.setAtaques(12, 0.9f, 8, 1.8f);
        gestorEnemigos.setVenenoConfig(2.2f, 7.5f, 10.0f, 0.35f, 0.35f);

        configurarTemplo();
        clampJugadorEnNivel();

        hud = new Hud();
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hud.setVida(jugador.getVida());
        hud.setScore(score);
        hud.setTiempoSeg(tiempoPartida);

        heartDropRegion = hud.getHeartFullRegion();

        score = 0;
        tiempoPartida = 0f;
        nivelTerminado = false;
        enPausa = false;

        bonusTiempoAplicado = false;
        bonusTiempo = 0;

        hitsPajaro.clear();
        hitsSerpiente.clear();
        hitsGolem.clear();
        puntuadoPajaro.clear();
        puntuadoSerpiente.clear();
        puntuadoGolem.clear();

        enemigosMatados = 0;
        heartDrops.clear();
    }

    @Override
    public void resize(int width, int height) {
        float sueloAntes = (parallax != null) ? getSueloEntidadesY() : 0f;

        viewport.update(width, height, true);

        if (parallax != null) {
            parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
            recalcularAjusteSueloParaMantenerObjetivo();
        }

        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        float sueloDespues = getSueloEntidadesY();
        float deltaSuelo = sueloDespues - sueloAntes;

        jugador.setSueloY(sueloDespues);
        jugador.setY(jugador.getY() + deltaSuelo);

        gestorEnemigos.setYsuelo(sueloDespues);

        float yTopPantalla = sueloDespues + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(2.4f, 2, yTopPantalla, 0.8f, 12.0f, 12, 0.60f);

        configurarTemplo();
        clampJugadorEnNivel();

        if (hud != null) hud.resize(width, height);

        // ✅ NUEVO: actualizar viewport de controles
        if (esMovil && controlesViewport != null) {
            controlesViewport.update(width, height, true);
        }
        if (esMovil && controles != null) {
            controles.recalcularLayout();
        }
    }

    @Override
    public void render(float delta) {

        syncMusicaSegunConfiguracion();

        // ------------------------------------------------------------
        // ✅ PAUSA: teclado en PC + botón pausa en móvil
        // ------------------------------------------------------------
        boolean pausaTouchJust = false;
        if (esMovil && controles != null) {
            boolean pausaNow = controles.isPausa();
            pausaTouchJust = pausaNow && !prevPausaTouch;
            prevPausaTouch = pausaNow;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.P)
            || pausaTouchJust) {
            enPausa = true;
            pauseMusicaNivel();
            juego.setScreen(new PantallaPausa(juego, this));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            if (fadeEndX <= 0f) configurarTemplo();

            float playerW = jugador.getWidth(PPU);
            float x = fadeEndX - playerW * 0.9f;
            if (x < 0f) x = 0f;
            jugador.setX(x);
        }

        if (jugador != null && (jugador.isDead() || jugador.getVida() <= 0)) {

            if (!muerteEnCurso) {
                muerteEnCurso = true;
                tiempoMuerte = 0f;

                pararMusicaNivel();
            }

            tiempoMuerte += delta;

            if (tiempoMuerte >= DURACION_ANIM_DEAD) {
                juego.setScreen(new PantallaMuerte(juego));
                return;
            }
        }

        if (!enPausa && !nivelTerminado) {
            tiempoPartida += delta;
        }

        float h = jugador.getHeight(PPU);

        // ------------------------------------------------------------
        // ✅ INPUT: teclado en PC, táctil en móvil
        // ------------------------------------------------------------
        float dir = 0f;

        if (!esMovil) {
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;
        } else if (controles != null) {
            dir = controles.getDirX();
        }

        boolean saltar = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        boolean agacharse = Gdx.input.isKeyPressed(Input.Keys.S)
            || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        boolean disparaNormal = Gdx.input.isKeyJustPressed(Input.Keys.J);
        boolean disparaEspecial = Gdx.input.isKeyJustPressed(Input.Keys.K);

        // ✅ Añadir táctil (justPressed) encima del teclado
        if (esMovil && controles != null) {
            boolean saltarNow = controles.isSaltar();
            boolean dispararNow = controles.isDisparar();
            boolean especialNow = controles.isEspecial();

            boolean saltarJust = saltarNow && !prevSaltarTouch;
            boolean dispararJust = dispararNow && !prevDispararTouch;
            boolean especialJust = especialNow && !prevEspecialTouch;

            prevSaltarTouch = saltarNow;
            prevDispararTouch = dispararNow;
            prevEspecialTouch = especialNow;

            saltar = saltar || saltarJust;
            disparaNormal = disparaNormal || dispararJust;
            disparaEspecial = disparaEspecial || especialJust;

            // ✅ FIX: AGACHARSE EN MÓVIL usando joystick hacia abajo
            agacharse = agacharse || (controles.getDirY() < -0.55f);
        }

        jugador.setSueloY(getSueloEntidadesY());
        jugador.aplicarEntrada(dir, saltar, agacharse, delta);

        clampJugadorEnNivel();
        actualizarAlphaEntradaTemplo();

        if (!nivelTerminado && alphaJugador <= 0.01f && jugador.getX() >= fadeEndX - 0.001f) {
            nivelTerminado = true;
            aplicarBonusTiempoSiProcede();

            pararMusicaNivel();

            juego.setScreen(new PantallaSalaJefe(juego, jugador));
            return;
        }

        gestorEnemigos.setYsuelo(getSueloEntidadesY());
        gestorEnemigos.setLimiteDerecha(limiteEnemigosDerecha);

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
        float rightX = cameraLeftX + viewW;

        gestorProyectiles.update(delta, cameraLeftX, viewW);
        gestorEnemigos.update(delta);

        float yTopPantalla = getSueloEntidadesY() + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(2.4f, 2, yTopPantalla, 0.8f, 12.0f, 12, 0.60f);

        gestorEnemigos.updateAtaques(delta, jugador, PPU, cameraLeftX, viewW);
        gestorEfectos.update(delta);

        updateHeartDrops(delta);

        // ------------------------------------------------------------
        // COLISIONES DISPAROS NORMALES + SCORE + CHISPITA
        // ------------------------------------------------------------

        // SERPIENTE
        for (Serpiente s : gestorEnemigos.getSerpientes()) {
            if (s.isDead()) continue;
            if (s.isDying()) continue;

            for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
                Proyectil p = gestorProyectiles.getNormales().get(i);
                if (p.isEliminar()) continue;

                if (s.getHitbox().overlaps(p.getHitbox())) {

                    int hs = hitsSerpiente.get(s, 0) + 1;
                    hitsSerpiente.put(s, hs);

                    if (hs >= HITS_SERPIENTE) {
                        if (!puntuadoSerpiente.contains(s)) {
                            score += PUNTOS_SERPIENTE;
                            puntuadoSerpiente.add(s);

                            Rectangle hbS = s.getHitbox();
                            onEnemyKilled(hbS.x + hbS.width * 0.5f, hbS.y);
                        }
                        s.recibirDanio(999999);
                    }

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

        // PÁJARO
        for (Pajaro b : gestorEnemigos.getPajaros()) {
            if (b.isDead()) continue;

            for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
                Proyectil p = gestorProyectiles.getNormales().get(i);
                if (p.isEliminar()) continue;

                if (b.getHitbox(PPU).overlaps(p.getHitbox())) {

                    int hbHits = hitsPajaro.get(b, 0) + 1;
                    hitsPajaro.put(b, hbHits);

                    if (hbHits >= HITS_PAJARO) {
                        b.recibirDanio(999999);
                        if (!puntuadoPajaro.contains(b) && b.isDead()) {
                            score += PUNTOS_PAJARO;
                            puntuadoPajaro.add(b);

                            Rectangle hbB = b.getHitbox(PPU);
                            onEnemyKilled(hbB.x + hbB.width * 0.5f, hbB.y);
                        }
                    }

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

        // GOLEM
        for (Golem g : gestorEnemigos.getGolems()) {
            if (g.isDead()) continue;

            for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
                Proyectil p = gestorProyectiles.getNormales().get(i);
                if (p.isEliminar()) continue;

                if (g.getHitbox().overlaps(p.getHitbox())) {

                    int hg = hitsGolem.get(g, 0) + 1;
                    hitsGolem.put(g, hg);

                    if (hg >= HITS_GOLEM) {
                        g.recibirDanio(999999);
                        if (!puntuadoGolem.contains(g) && g.isDead()) {
                            score += PUNTOS_GOLEM;
                            puntuadoGolem.add(g);

                            Rectangle hbG = g.getHitbox();
                            onEnemyKilled(hbG.x + hbG.width * 0.5f, hbG.y);
                        }
                    }

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

        // ------------------------------------------------------------
        // ATAQUE ESPECIAL: DAÑO + PUNTOS + KILLS
        // ------------------------------------------------------------
        AtaqueEspecial esp = gestorProyectiles.getEspecial();
        if (esp != null) {
            Rectangle hbRayo = esp.getHitbox();
            if (hbRayo != null) {

                for (Serpiente s : gestorEnemigos.getSerpientes()) {
                    if (s.isDead()) continue;
                    if (s.isDying()) continue;

                    if (s.getHitbox().overlaps(hbRayo)) {
                        s.recibirDanio(esp.getDamage());
                        if (!puntuadoSerpiente.contains(s) && s.isDying()) {
                            score += PUNTOS_SERPIENTE;
                            puntuadoSerpiente.add(s);

                            Rectangle hbS = s.getHitbox();
                            onEnemyKilled(hbS.x + hbS.width * 0.5f, hbS.y);
                        }
                    }
                }

                for (Pajaro b : gestorEnemigos.getPajaros()) {
                    if (b.isDead()) continue;

                    if (b.getHitbox(PPU).overlaps(hbRayo)) {
                        b.recibirDanio(esp.getDamage());
                        if (!puntuadoPajaro.contains(b) && b.isDead()) {
                            score += PUNTOS_PAJARO;
                            puntuadoPajaro.add(b);

                            Rectangle hbB = b.getHitbox(PPU);
                            onEnemyKilled(hbB.x + hbB.width * 0.5f, hbB.y);
                        }
                    }
                }

                for (Golem g : gestorEnemigos.getGolems()) {
                    if (g.isDead()) continue;

                    if (g.getHitbox().overlaps(hbRayo)) {
                        g.recibirDanio(esp.getDamage());
                        if (!puntuadoGolem.contains(g) && g.isDead()) {
                            score += PUNTOS_GOLEM;
                            puntuadoGolem.add(g);

                            Rectangle hbG = g.getHitbox();
                            onEnemyKilled(hbG.x + hbG.width * 0.5f, hbG.y);
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------
        // DIBUJO
        // ------------------------------------------------------------
        juego.batch.begin();

        parallax.render(juego.batch, cameraLeftX, viewW);

        if (temploX + temploW > cameraLeftX - 2f && temploX < rightX + 2f) {
            juego.batch.draw(temploRegion, temploX, temploY, temploW, temploH);
        }

        drawHeartDrops();

        gestorProyectiles.draw(juego.batch, cameraLeftX, viewW);
        gestorEnemigos.render(juego.batch, cameraLeftX, viewW);
        gestorEfectos.draw(juego.batch);

        float pr = juego.batch.getColor().r;
        float pg = juego.batch.getColor().g;
        float pb = juego.batch.getColor().b;
        float pa = juego.batch.getColor().a;

        juego.batch.setColor(1f, 1f, 1f, alphaJugador);
        jugador.draw(juego.batch, PPU);
        juego.batch.setColor(pr, pg, pb, pa);

        juego.batch.end();

        if (hud != null) {
            juego.batch.begin();
            hud.setVida(jugador.getVida());
            hud.setScore(score);
            hud.setTiempoSeg(tiempoPartida);
            hud.draw(juego.batch);

            // ✅ FIX REAL: dibujar controles con SU viewport de pantalla
            if (esMovil && controles != null && controlesViewport != null) {
                controlesViewport.apply();
                juego.batch.setProjectionMatrix(controlesCam.combined);
                controles.render(juego.batch);
            }

            juego.batch.end();
        }
    }

    @Override
    public void hide() {
        pararMusicaNivel();
    }

    public void stopMusicaNivel() {
        pararMusicaNivel();
    }

    public void pauseMusicaNivel() {
        if (musicaNivel != null && musicaNivel.isPlaying()) musicaNivel.pause();
    }

    public void resumeMusicaNivel() {
        if (!Configuracion.isSonidoActivado()) {
            pararMusicaNivel();
            return;
        }
        if (musicaNivel != null && !musicaNivel.isPlaying()) musicaNivel.play();
        else if (musicaNivel == null) syncMusicaSegunConfiguracion();
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

        if (hud != null) hud.dispose();

        // ✅ liberar controles táctiles
        if (controles != null) controles.dispose();

        pararMusicaNivel();
    }
}
