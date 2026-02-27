package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.entidades.enemigos.Jefe;
import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilMeteoro;
import io.github.some_example_name.entidades.proyectiles.jugador.AtaqueEspecial;
import io.github.some_example_name.entidades.proyectiles.jugador.GestorProyectiles;
import io.github.some_example_name.entidades.proyectiles.jugador.Proyectil;
import io.github.some_example_name.utilidades.Configuracion;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.DisparoAssets;
import io.github.some_example_name.utilidades.Hud;

// ✅ Controles táctiles (ruta según tu proyecto)
import io.github.some_example_name.entidades.entidades.controles.ControlesTactiles;

import java.util.ArrayList;

public class PantallaSalaJefe implements Screen {

    private static final float PPU_PLAYER = 64f;
    private static final float PPU_BOSS = 128f;

    private static final float SUELO_FRAC_SALA = 0.28f;

    // BAJAMOS el boss para que no quede a la altura de la cabeza
    private static final float BOSS_Y_OFFSET = -1.70f;

    // ✅ Retraso para ver animación de muerte antes de cambiar de pantalla
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

    // ✅ NUEVO: para que no se re-arranque música mientras estás en la pantalla de pausa
    private boolean enPausa = false;

    // ------------------------------------------------------------
    // ✅ CONTROLES TÁCTILES (SOLO MÓVIL)
    // - Saltar: joystick arriba (ControlesTactiles.isSaltar())
    // - Agacharse: joystick abajo (dirY < umbral)
    // - Disparo / Especial / Pausa: botones
    // ------------------------------------------------------------
    private boolean esMovil = false;
    private ControlesTactiles controles;

    // Viewport UI para controles (para que se vean siempre y no dependan de la cámara del mundo)
    private OrthographicCamera camUI;
    private Viewport viewportUI;

    // Para detectar "justPressed" en botones táctiles
    private boolean prevSaltarTouch = false;
    private boolean prevDispararTouch = false;
    private boolean prevEspecialTouch = false;
    private boolean prevPausaTouch = false;

    // Umbral para agacharse con joystick hacia abajo
    private static final float JOY_CROUCH_THRESHOLD = -0.55f;

    // ------------------------------------------------------------
    // ✅ COLISIONES Y DAÑO (1 corazón por golpe)
    // ------------------------------------------------------------
    private static final int DANIO_CORAZON_COMPLETO = 20;
    private static final float INVULN_SEG = 0.85f;
    private float invulnTimer = 0f;

    private final Rectangle hbJugadorTmp = new Rectangle();
    private final Rectangle hbBossTmp = new Rectangle();
    private final Rectangle hbMeteoroTmp = new Rectangle();

    // ------------------------------------------------------------
    // ✅ SENSOR DE LUZ (oscuro base + flash cuando dispara meteoros)
    // ------------------------------------------------------------
    private Texture whitePixel;

    // Oscuridad "un poco" (ajusta a gusto)
    private static final float OSCURIDAD_BASE = 0.28f;

    // Flash leve al disparar (reduce la oscuridad temporalmente)
    private static final float FLASH_REDUCE_OSCURIDAD = 0.14f;
    private static final float FLASH_DURACION = 0.22f;
    private float flashTimer = 0f;

    // Para detectar disparo: si sube el contador de meteoros
    private int meteorosPrevCount = 0;

    // ------------------------------------------------------------
    // ✅ HITBOX BOSS (AJUSTADO) + BARRA VIDA BOSS (UI)
    // ------------------------------------------------------------
    private static final float BOSS_HB_INSET_X_FRAC = 0.18f;   // 18% a cada lado
    private static final float BOSS_HB_INSET_Y_FRAC = 0.10f;   // 10% arriba/abajo
    private static final float BOSS_HB_SHIFT_Y_FRAC = -0.06f;  // baja un poco el hitbox

    private static final float BOSS_BAR_W_FRAC = 0.42f;        // 42% ancho pantalla (no llega de score a reloj)
    private static final float BOSS_BAR_H_PX = 18f;            // altura visible
    private static final float BOSS_BAR_TOP_MARGIN_PX = 10f;   // margen superior
    private static final float BOSS_BAR_BORDER_PX = 3f;        // borde

    // ------------------------------------------------------------
    // ✅ DAÑO AL JEFE POR DISPAROS DEL JUGADOR
    // ------------------------------------------------------------
    private static final int DANIO_DISPARO_NORMAL = 10;   // 30 disparos matan si vida=300
    private static final int DANIO_TICK_ESPECIAL = 8;     // daño por tick del rayo
    private static final float ESPECIAL_TICK_CD = 0.12f;  // cada 0.12s aplica daño si está tocando
    private float especialTickTimer = 0f;

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
        // ✅ respeta SONIDO ON/OFF y evita arrancar en pausa
        if (enPausa) return;
        if (!Configuracion.isSonidoActivado()) {
            stopMusica();
            return;
        }
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

    // ------------------------------------------------------------
    // ✅ NUEVO: sincroniza música con Configuracion (sonido ON/OFF)
    // - OFF => stop + dispose (no queda sonando)
    // - ON  => crea/reanuda si no estás en pausa ni en muerte
    // ------------------------------------------------------------
    private void syncMusicaSegunConfiguracion() {
        // Si ya estás en muerte, no vuelvas a arrancar música
        if (muerteIniciada) return;

        // Si estás en pausa, asegúrate de que no se reanuda aquí
        if (enPausa) {
            pauseMusica();
            return;
        }

        boolean sonidoOn = Configuracion.isSonidoActivado();

        if (!sonidoOn) {
            stopMusica();
            return;
        }

        // ON: crear/reanudar
        if (musicaJefe == null) {
            musicaJefe = Gdx.audio.newMusic(Gdx.files.internal("audio/nivel2.mp3"));
            musicaJefe.setLooping(true);
            musicaJefe.setVolume(0.7f);
            musicaJefe.play();
        } else if (!musicaJefe.isPlaying()) {
            musicaJefe.play();
        }
    }

    private void setPixelArt(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    private Texture crearWhitePixel() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    @Override
    public void show() {

        // ✅ Música: respeta configuración al entrar
        enPausa = false;
        syncMusicaSegunConfiguracion();

        // Reset muerte
        muerteIniciada = false;
        timerMuerte = 0f;

        // Reset colisiones/luz
        invulnTimer = 0f;
        flashTimer = 0f;
        meteorosPrevCount = 0;
        especialTickTimer = 0f;

        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // ------------------------------------------------------------
        // ✅ Detectar móvil y activar controles táctiles automáticamente
        // (Usamos viewportUI para que los botones se vean y sean fijos en pantalla)
        // ------------------------------------------------------------
        esMovil = (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android)
            || (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS);

        camUI = new OrthographicCamera();
        viewportUI = new ScreenViewport(camUI);
        viewportUI.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        if (esMovil) {
            controles = new ControlesTactiles(viewportUI);
            Gdx.input.setInputProcessor(controles);

            prevSaltarTouch = false;
            prevDispararTouch = false;
            prevEspecialTouch = false;
            prevPausaTouch = false;
        }

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

        // ✅ overlay para oscuridad
        whitePixel = crearWhitePixel();
        setPixelArt(whitePixel);

        sueloY = viewport.getWorldHeight() * SUELO_FRAC_SALA;

        jugador.setSueloY(sueloY);
        jugador.setY(sueloY);
        jugador.setX(viewport.getWorldWidth() * 0.10f);

        float bossW = bossDormidoTex.getWidth() / PPU_BOSS;
        float bossX = viewport.getWorldWidth() - bossW - 0.8f;
        if (bossX < 0.8f) bossX = 0.8f;

        float bossY = sueloY + BOSS_Y_OFFSET;

        // ------------------------------------------------------------
        // ✅ DIFICULTAD
        // - Como está: difícil
        // - Fácil: más cooldown a salto y disparo
        // ------------------------------------------------------------
        long cadenciaDisparoMs = 1400L; // difícil (actual)
        long cooldownSaltoMs = 2600L;   // difícil (actual)

        Configuracion.Dificultad d = Configuracion.getDificultad();
        if (d == Configuracion.Dificultad.FACIL) {
            cadenciaDisparoMs = 2200L;
            cooldownSaltoMs = 3600L;
        }

        jefe = new Jefe(
            bossDormidoTex,
            bossDespiertoTex,
            bossX,
            bossY,
            6.0f,
            300,
            cadenciaDisparoMs,
            cooldownSaltoMs,
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

        meteorosPrevCount = meteoros.size();
    }

    @Override
    public void render(float dt) {

        // ✅ CLAVE: sincroniza CADA FRAME para obedecer SONIDO ON/OFF inmediatamente
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P) ||
            pausaTouchJust) {

            // Al entrar en pausa: pausar música (no stop)
            enPausa = true;
            pauseMusica();
            game.setScreen(new PantallaPausaJefe(game, this));
            return;
        }

        // Si está muerto: esperar animación y luego ir a PantallaMuerte
        if (jugador.isDead() || jugador.getVida() <= 0) {

            if (!muerteIniciada) {
                muerteIniciada = true;
                timerMuerte = 0f;
                // Importante: cortar música ya al empezar la muerte
                stopMusica();
            }

            // Avanzar animación del jugador (dead) sin input real
            jugador.aplicarEntrada(0f, false, false, dt);

            timerMuerte += dt;
            if (timerMuerte >= RETARDO_MUERTE) {
                // OJO: PantallaMuerte (según tu proyecto) solo recibe Main
                game.setScreen(new PantallaMuerte(game));
                return;
            }

            // Dibujar escena para ver la animación
            dibujarEscena();
            return;
        }

        tiempo += dt;

        update(dt);

        dibujarEscena();
    }

    private void dibujarOscuridadYFlash() {
        if (whitePixel == null) return;

        float flashT = 0f;
        if (flashTimer > 0f) {
            flashT = flashTimer / FLASH_DURACION;
            if (flashT > 1f) flashT = 1f;
            if (flashT < 0f) flashT = 0f;
        }

        float reduce = FLASH_REDUCE_OSCURIDAD * flashT;
        float alpha = OSCURIDAD_BASE - reduce;

        if (alpha < 0.06f) alpha = 0.06f;
        if (alpha > 0.80f) alpha = 0.80f;

        float pr = game.batch.getColor().r;
        float pg = game.batch.getColor().g;
        float pb = game.batch.getColor().b;
        float pa = game.batch.getColor().a;

        game.batch.setColor(0f, 0f, 0f, alpha);
        game.batch.draw(whitePixel, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());

        game.batch.setColor(pr, pg, pb, pa);
    }

    private void drawBossHealthBarUI() {
        if (jefe == null || whitePixel == null || viewportUI == null || camUI == null) return;
        if (!jefe.isVivo()) return;

        // ✅ Solo se muestra cuando el jefe ya NO está dormido (BossFinal)
        if (jefe.isDormido()) return;

        int vida = jefe.getVida();
        int vidaMax = jefe.getVidaMax();
        if (vidaMax <= 0) return;

        float ratio = vida / (float) vidaMax;
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;

        float screenW = viewportUI.getWorldWidth();
        float screenH = viewportUI.getWorldHeight();

        float barW = screenW * BOSS_BAR_W_FRAC;
        float barH = BOSS_BAR_H_PX;

        float x = (screenW - barW) * 0.5f;
        float y = screenH - BOSS_BAR_TOP_MARGIN_PX - barH;

        float pr = game.batch.getColor().r;
        float pg = game.batch.getColor().g;
        float pb = game.batch.getColor().b;
        float pa = game.batch.getColor().a;

        // Fondo (negro semitransparente + borde)
        game.batch.setColor(0f, 0f, 0f, 0.65f);
        game.batch.draw(
            whitePixel,
            x - BOSS_BAR_BORDER_PX,
            y - BOSS_BAR_BORDER_PX,
            barW + BOSS_BAR_BORDER_PX * 2f,
            barH + BOSS_BAR_BORDER_PX * 2f
        );

        // Barra vacía (rojo oscuro)
        game.batch.setColor(0.30f, 0.05f, 0.05f, 0.95f);
        game.batch.draw(whitePixel, x, y, barW, barH);

        // Barra llena (rojo vivo)
        game.batch.setColor(0.90f, 0.10f, 0.10f, 0.98f);
        game.batch.draw(whitePixel, x, y, barW * ratio, barH);

        // Brillo superior leve
        game.batch.setColor(1f, 1f, 1f, 0.12f);
        game.batch.draw(whitePixel, x, y + barH * 0.55f, barW, barH * 0.45f);

        game.batch.setColor(pr, pg, pb, pa);
    }

    private void dibujarEscena() {
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

        // ✅ Sensor de luz: oscurece y suaviza al disparar fuego
        dibujarOscuridadYFlash();

        game.batch.end();

        // HUD + CONTROLES
        game.batch.begin();
        hud.setVida(jugador.getVida());
        hud.setScore(score);
        hud.setTiempoSeg(tiempo);
        hud.draw(game.batch);

        // ✅ Barra vida jefe grande entre puntuación y reloj (UI)
        if (viewportUI != null && camUI != null) {
            viewportUI.apply();
            game.batch.setProjectionMatrix(camUI.combined);
            drawBossHealthBarUI();
        }

        // ✅ DIBUJAR CONTROLES TÁCTILES ENCIMA DEL HUD (solo móvil)
        if (esMovil && controles != null && viewportUI != null && camUI != null) {
            viewportUI.apply();
            game.batch.setProjectionMatrix(camUI.combined);
            controles.render(game.batch);
        }

        game.batch.end();
    }

    private Rectangle getHitboxJugador(Rectangle out) {
        float jx = jugador.getX();
        float jy = jugador.getY();
        float jw = jugador.getWidth(PPU_PLAYER);
        float jh = jugador.getHeight(PPU_PLAYER);
        out.set(jx, jy, jw, jh);
        return out;
    }

    private Rectangle getHitboxBoss(Rectangle out) {
        if (jefe == null) {
            out.set(0f, 0f, 0f, 0f);
            return out;
        }

        float bx = jefe.getX();
        float by = jefe.getY();
        float bw = jefe.getAncho();
        float bh = jefe.getAlto();

        float insetX = bw * BOSS_HB_INSET_X_FRAC;
        float insetY = bh * BOSS_HB_INSET_Y_FRAC;

        float hbX = bx + insetX;
        float hbW = bw - insetX * 2f;

        float hbY = by + insetY + (bh * BOSS_HB_SHIFT_Y_FRAC);
        float hbH = bh - insetY * 2f;

        if (hbW < 0f) hbW = 0f;
        if (hbH < 0f) hbH = 0f;

        out.set(hbX, hbY, hbW, hbH);
        return out;
    }

    private Rectangle getHitboxMeteoro(ProyectilMeteoro m, Rectangle out) {
        // En ProyectilMeteoro el draw está centrado (x - ancho/2, y - alto/2),
        // así que la hitbox también debe ser centrada.
        float w = m.getAncho();
        float h = m.getAlto();
        out.set(m.getX() - w * 0.5f, m.getY() - h * 0.5f, w, h);
        return out;
    }

    private void aplicarDanioJugador() {
        if (invulnTimer > 0f) return;

        jugador.recibirDanio(DANIO_CORAZON_COMPLETO);

        // Vibración corta al recibir golpe
        if (Configuracion.isVibracionActivada()
            && Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator)) {
            Gdx.input.vibrate(45);
        }

        invulnTimer = INVULN_SEG;
    }

    private void comprobarColisionesBossYProyectiles() {
        Rectangle hbJ = getHitboxJugador(hbJugadorTmp);

        // Boss solo hace daño por contacto si está saltando y te cae encima
        if (jefe != null && jefe.isSaltando()) {
            Rectangle hbB = getHitboxBoss(hbBossTmp);
            if (hbB.overlaps(hbJ)) {
                aplicarDanioJugador();
            }
        }

        // Meteoros: si te tocan, 1 corazón y se eliminan (vivo=false)
        for (int i = meteoros.size() - 1; i >= 0; i--) {
            ProyectilMeteoro m = meteoros.get(i);
            if (!m.isVivo()) continue;

            Rectangle hbM = getHitboxMeteoro(m, hbMeteoroTmp);
            if (hbM.overlaps(hbJ)) {
                aplicarDanioJugador();

                // ✅ eliminar el meteoro al impactar (sin método público -> lo retiramos de la lista)
                // Como isVivo depende de una flag privada, lo más directo es quitarlo aquí.
                meteoros.remove(i);
            }
        }
    }

    private void comprobarColisionesDisparosContraJefe(float dt) {
        if (jefe == null || !jefe.isVivo()) return;

        // ✅ Solo recibe daño cuando ya NO está dormido (BossFinal)
        if (jefe.isDormido()) {
            especialTickTimer = 0f;
            return;
        }

        Rectangle hbB = getHitboxBoss(hbBossTmp);

        // -------------------------
        // DISPAROS NORMALES
        // -------------------------
        for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
            Proyectil p = gestorProyectiles.getNormales().get(i);
            if (p.isEliminar()) continue;

            if (hbB.overlaps(p.getHitbox())) {
                jefe.recibirDanio(DANIO_DISPARO_NORMAL);
                p.marcarEliminar();
            }
        }

        // -------------------------
        // ESPECIAL (rayo): daño por ticks
        // -------------------------
        AtaqueEspecial esp = gestorProyectiles.getEspecial();
        if (esp == null) {
            especialTickTimer = 0f;
            return;
        }

        Rectangle hbRayo = esp.getHitbox();
        if (hbRayo == null) {
            especialTickTimer = 0f;
            return;
        }

        if (hbRayo.overlaps(hbB)) {
            especialTickTimer -= dt;
            if (especialTickTimer <= 0f) {
                jefe.recibirDanio(DANIO_TICK_ESPECIAL);
                especialTickTimer = ESPECIAL_TICK_CD;
            }
        } else {
            especialTickTimer = 0f;
        }
    }

    private void update(float dt) {

        // Timers colisión/luz
        if (invulnTimer > 0f) invulnTimer -= dt;
        if (invulnTimer < 0f) invulnTimer = 0f;

        if (flashTimer > 0f) flashTimer -= dt;
        if (flashTimer < 0f) flashTimer = 0f;

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

        boolean saltar =
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        boolean agacharse =
            Gdx.input.isKeyPressed(Input.Keys.S) ||
                Gdx.input.isKeyPressed(Input.Keys.DOWN);

        boolean disparaNormal = Gdx.input.isKeyJustPressed(Input.Keys.J);
        boolean disparaEspecial = Gdx.input.isKeyJustPressed(Input.Keys.K);

        // ✅ Añadir táctil (jump con joystick arriba + crouch con joystick abajo + botones)
        if (esMovil && controles != null) {

            // Saltar: joystick arriba (lo tratamos como justPressed)
            boolean saltarNow = controles.isSaltar();
            boolean saltarJust = saltarNow && !prevSaltarTouch;
            prevSaltarTouch = saltarNow;

            // Disparo / especial: justPressed
            boolean dispararNow = controles.isDisparar();
            boolean especialNow = controles.isEspecial();

            boolean dispararJust = dispararNow && !prevDispararTouch;
            boolean especialJust = especialNow && !prevEspecialTouch;

            prevDispararTouch = dispararNow;
            prevEspecialTouch = especialNow;

            saltar = saltar || saltarJust;
            disparaNormal = disparaNormal || dispararJust;
            disparaEspecial = disparaEspecial || especialJust;

            // Agacharse: joystick abajo (mientras lo mantienes)
            float dy = controles.getDirY();
            agacharse = agacharse || (dy < JOY_CROUCH_THRESHOLD);
        }

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

        // Detectar disparo del boss para flash de luz
        int before = meteoros.size();

        if (jefe != null) {
            jefe.update(dt, jugador, 0, 0, meteoros);
        }

        int after = meteoros.size();
        if (after > before) {
            flashTimer = FLASH_DURACION;
        }
        meteorosPrevCount = after;

        for (int i = meteoros.size() - 1; i >= 0; i--) {
            ProyectilMeteoro m = meteoros.get(i);
            m.update(dt);
            if (!m.isVivo()) meteoros.remove(i);
        }

        // ✅ colisiones (boss saltando + meteoros)
        comprobarColisionesBossYProyectiles();

        // ✅ colisiones disparos del jugador contra el jefe (solo BossFinal, no dormido)
        comprobarColisionesDisparosContraJefe(dt);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height);

        if (viewportUI != null) viewportUI.update(width, height, true);
        if (esMovil && controles != null) controles.recalcularLayout();

        sueloY = viewport.getWorldHeight() * SUELO_FRAC_SALA;
        jugador.setSueloY(sueloY);
        if (jugador.getY() < sueloY) jugador.setY(sueloY);
    }

    @Override public void pause() {}

    @Override public void resume() {
        // Si vuelves del pause, reanuda música (solo si SONIDO está ON)
        enPausa = false;
        resumeMusica();
    }

    @Override
    public void hide() {
        // IMPORTANTE:
        // No hacemos stop aquí, porque al ir a PantallaPausaJefe también se llama hide().
        // El stop se hace explícitamente al morir (arriba) o desde el menú de pausa al salir.
    }

    @Override
    public void dispose() {
        stopMusica();

        if (fondoSala != null) fondoSala.dispose();
        if (bossDormidoTex != null) bossDormidoTex.dispose();
        if (bossDespiertoTex != null) bossDespiertoTex.dispose();
        if (meteoroTex != null) meteoroTex.dispose();
        if (hud != null) hud.dispose();
        if (disparoAssets != null) disparoAssets.dispose();

        if (whitePixel != null) whitePixel.dispose();

        // ✅ liberar controles táctiles
        if (controles != null) controles.dispose();
    }
}
