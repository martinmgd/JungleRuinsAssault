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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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
import io.github.some_example_name.utilidades.Records;
import io.github.some_example_name.pantallas.PantallaNuevoRecord;
import io.github.some_example_name.utilidades.VenenoAssets;

import io.github.some_example_name.entidades.controles.ControlesTactiles;

public class PantallaJuego extends ScreenAdapter {

    // ------------------------------------------------------------
    // MÚSICA
    // ------------------------------------------------------------
    // Música de fondo del nivel. Se gestiona en función de pausa/muerte y configuración de sonido.
    private Music musicaNivel;

    // Flag para inicializar recursos una única vez cuando la pantalla se muestra por primera vez.
    private boolean inicializado = false;

    // Referencia al juego principal para acceder a batch, cambiar pantallas, etc.
    private final Main juego;

    // Cámara ortográfica del mundo del juego y su viewport asociado (mundo lógico escalado a pantalla).
    private OrthographicCamera camara;
    private Viewport viewport;

    // Control de secuencia de muerte del jugador (animación + transición a pantallas finales).
    private boolean muerteEnCurso = false;
    private float tiempoMuerte = 0f;

    // Duración total prevista de la animación "dead" antes de cambiar de pantalla.
    private static final float DURACION_ANIM_DEAD = 1.2f;

    // Animaciones y entidad del jugador.
    private PlayerAnimations anims;
    private Jugador jugador;

    // Ancho lógico total del nivel (en unidades del mundo).
    private float anchoNivel = 400f;

    // Límites de seguimiento horizontal de la cámara (para no salir del nivel).
    private float limiteIzq;
    private float limiteDer;

    // Pixels-Per-Unit para convertir dimensiones de texturas (px) a mundo (unidades).
    private static final float PPU = 64f;

    // Fondo parallax y sus parámetros de escalado/posición.
    private ParallaxBackground parallax;

    // Assets de disparo del jugador y gestor de proyectiles (normal/especial).
    private DisparoAssets disparoAssets;
    private GestorProyectiles gestorProyectiles;

    // Assets de impactos/efectos y su gestor de reproducción.
    private ImpactoAssets impactoAssets;
    private GestorEfectos gestorEfectos;

    // Gestor centralizado de enemigos y su configuración por tipo.
    private GestorEnemigos gestorEnemigos;

    // Texturas de animaciones enemigas (serpiente).
    private Texture serpienteWalk;
    private Texture serpienteDeath;

    // Texturas de animaciones enemigas (pájaro).
    private Texture pajaroAttak;
    private Texture pajaroDeath;

    // Texturas de animaciones enemigas (golem).
    private Texture golemIdle;
    private Texture golemWalk;
    private Texture golemAttack;
    private Texture golemThrow;
    private Texture golemDeath;

    // Proyectil del golem (roca) y región para dibujado.
    private Texture rocaTex;
    private TextureRegion rocaRegion;

    // Assets relacionados con veneno (ataques de enemigos).
    private VenenoAssets venenoAssets;

    // Color base para impactos normales (chispas/feedback visual al acertar).
    private final Color colorImpactoNormal = new Color(1f, 0.6f, 0.15f, 1f);

    // Ajuste de suelo: fracción del alto de viewport donde se ubica el ground del parallax + offset final.
    private static final float GROUND_FRAC_FINAL = 0.45f;
    private static final float AJUSTE_SUELO_FINAL = -4.7f;

    // Offset vertical aplicado al suelo real para alinear sprites/colisiones.
    private float ajusteSueloY = AJUSTE_SUELO_FINAL;

    // Objetivo de Y del suelo (mantenido estable al redimensionar).
    private float sueloObjetivoY = 0f;

    // Ajuste adicional para alinear entidades por encima del suelo (evitar que "pisen" dentro del ground).
    private static final float SUELO_ENTIDADES_UP_BASE = 0.90f;

    // ------------------------------------------------------------
    // TEMPLO
    // ------------------------------------------------------------
    // Textura y región de la entrada de ruina/templo al final del nivel.
    private Texture temploTex;
    private TextureRegion temploRegion;

    // Dimensiones escaladas del templo en unidades del mundo.
    private float temploW;
    private float temploH;

    // Posición del templo en el mundo.
    private float temploX;
    private float temploY;

    // Control de colocación: cuánto queda fuera por la derecha y fracción del alto del viewport ocupada.
    private static final float TEMPLO_OUT_RIGHT_FRAC = 0.20f;
    private static final float TEMPLO_VIEWPORT_H_FRAC = 0.72f;

    // Rango X aproximado de la puerta (se usa como referencia/limitación).
    private float puertaX0;
    private float puertaX1;

    // Límite de spawn/movimiento de enemigos hacia la derecha (para no invadir la zona del templo).
    private float limiteEnemigosDerecha;

    // Alpha del jugador usado para efecto de "fade" al entrar en el templo (transición de nivel).
    private float alphaJugador = 1f;

    // Trigger borde derecho: posiciones X donde comienza y termina el fade (antes de transición).
    private float fadeStartX;
    private float fadeEndX;

    // ------------------------------------------------------------
    // HUD + TIEMPO + PUNTUACIÓN
    // ------------------------------------------------------------
    // HUD para vida, puntuación, tiempo y mensajes (bonus, etc.).
    private Hud hud;

    // Puntuación acumulada del nivel.
    private int score = 0;

    // Tiempo total de partida en segundos (solo avanza si no está en pausa ni terminado).
    private float tiempoPartida = 0f;

    // Flags de estado del nivel.
    private boolean nivelTerminado = false;
    private boolean enPausa = false;

    // Puntuación base por enemigo.
    private static final int PUNTOS_PAJARO = 50;
    private static final int PUNTOS_SERPIENTE = 100;
    private static final int PUNTOS_GOLEM = 200;

    // Bonus por tiempo: objetivo y máximo bonus posible.
    private static final float TIEMPO_OBJETIVO_SEG = 90f;
    private static final int BONUS_MAX_TIEMPO = 1000;

    // Estado de cálculo del bonus para evitar aplicarlo múltiples veces.
    private boolean bonusTiempoAplicado = false;
    private int bonusTiempo = 0;

    // Hits necesarios para matar cada enemigo (control de resistencia).
    private static final int HITS_PAJARO = 1;
    private static final int HITS_SERPIENTE = 3;
    private static final int HITS_GOLEM = 5;

    // Contadores de impactos por entidad para cada tipo de enemigo.
    private final ObjectIntMap<Pajaro> hitsPajaro = new ObjectIntMap<>();
    private final ObjectIntMap<Serpiente> hitsSerpiente = new ObjectIntMap<>();
    private final ObjectIntMap<Golem> hitsGolem = new ObjectIntMap<>();

    // Conjuntos para asegurar que cada enemigo puntúe una sola vez (evita sumar varias veces por estados/overlaps).
    private final ObjectSet<Pajaro> puntuadoPajaro = new ObjectSet<>();
    private final ObjectSet<Serpiente> puntuadoSerpiente = new ObjectSet<>();
    private final ObjectSet<Golem> puntuadoGolem = new ObjectSet<>();

    // ------------------------------------------------------------
    // DROPS: corazón cada 10 enemigos
    // ------------------------------------------------------------
    // Curación aplicada al recoger un corazón.
    private static final int VIDA_POR_CORAZON = 20;

    // Contador global de enemigos derrotados (para determinar drop cada 10).
    private int enemigosMatados = 0;

    // Región del sprite del corazón y lista de drops activos.
    private TextureRegion heartDropRegion;
    private final Array<HeartDrop> heartDrops = new Array<>();

    // Hitbox temporal reutilizable del jugador para evitar allocs por frame.
    private final Rectangle hbJugadorTmp = new Rectangle();

    // ------------------------------------------------------------
    //  CONTROLES TÁCTILES (SOLO MÓVIL)
    // ------------------------------------------------------------
    // Detección de plataforma móvil para activar controles táctiles.
    private boolean esMovil = false;

    // Controlador de inputs táctiles (joystick/botones) del proyecto.
    private ControlesTactiles controles;

    // Viewport/cámara dedicados a UI táctil en coordenadas de pantalla (siempre visible, independiente del mundo).
    private Viewport controlesViewport;
    private OrthographicCamera controlesCam;

    // Estados previos para implementar "justPressed" en botones táctiles (edge detection).
    private boolean prevSaltarTouch = false;
    private boolean prevDispararTouch = false;
    private boolean prevEspecialTouch = false;
    private boolean prevPausaTouch = false;

    // ------------------------------------------------------------
    //  BOTÓN EXTRA EN MÓVIL (usar boton_saltar.png como “ir a la entrada del boss”)
    // - No toca ControlesTactiles
    // - Se dibuja al lado del botón de pausa (zona superior derecha)
    // - Al pulsarlo: hace lo mismo que NUM_2 (teleport a la entrada de la ruina/boss)
    // ------------------------------------------------------------
    // Textura del botón extra y su hitbox en el viewport de controles.
    private Texture botonIrBossTex;
    private final Rectangle hbBotonIrBoss = new Rectangle();

    // Vector temporal para transformar coordenadas de toque (pantalla -> viewport UI).
    private final Vector3 touchTmp = new Vector3();

    // Tamaño/márgenes en píxeles de pantalla (viewport de controles).
    private static final float IRBOSS_BTN_SIZE_PX = 62f;
    private static final float IRBOSS_BTN_MARGIN_PX = 10f;
    private static final float IRBOSS_BTN_GAP_PX = 10f; // separación respecto al borde superior derecho (y al de pausa)

    // Drop simple con física vertical básica (gravedad) y recolección por hitbox.
    private static class HeartDrop {
        // Posición y velocidad vertical.
        float x, y;
        float vy;

        // Flags de estado (en suelo / marcado para eliminar).
        boolean enSuelo = false;
        boolean eliminar = false;

        // Tamaño del sprite/hitbox en unidades de mundo.
        float w = 0.55f;
        float h = 0.55f;

        // Y del suelo en el que debe detenerse.
        float sueloY;

        HeartDrop(float x, float y, float sueloY) {
            this.x = x;
            this.y = y;
            this.sueloY = sueloY;
            this.vy = 0f;
        }

        // Actualización simple: gravedad constante y colisión con "sueloY".
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

        // Devuelve hitbox en "out" para reutilizar objeto y evitar GC.
        Rectangle getHitbox(Rectangle out) {
            out.set(x - w * 0.5f, y, w, h);
            return out;
        }
    }

    public PantallaJuego(Main juego) {
        // Referencia persistente al juego para renderizar y realizar transiciones entre pantallas.
        this.juego = juego;
    }

    // Configuración de textura para pixel-art: filtrado Nearest y wrap clamp (evita bleeding).
    private void setPixelArt(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    // Y del suelo renderizable (ground del parallax + offset de ajuste).
    private float getSueloY() {
        return parallax.getGroundY() + ajusteSueloY;
    }

    // Ajuste proporcional del "elevado" de entidades según el alto del viewport.
    // Se recalcula para mantener coherencia visual al cambiar resoluciones.
    private float getSueloEntidadesUpWorld() {
        if (viewport == null) return SUELO_ENTIDADES_UP_BASE;
        float wh = viewport.getWorldHeight();
        if (wh <= 0.0001f) return SUELO_ENTIDADES_UP_BASE;
        return SUELO_ENTIDADES_UP_BASE * (Constantes.ALTO_MUNDO / wh);
    }

    // Y efectiva donde deben apoyar entidades (suelo base + separación adicional).
    private float getSueloEntidadesY() {
        return getSueloY() + getSueloEntidadesUpWorld();
    }

    // Mantiene constante el objetivo de suelo al redimensionar: recalcula offset respecto al ground actual del parallax.
    private void recalcularAjusteSueloParaMantenerObjetivo() {
        ajusteSueloY = sueloObjetivoY - parallax.getGroundY();
    }

    // Asegura que el jugador no salga del rango [0, anchoNivel - anchoJugador].
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

    // Configura tamaño y posición del templo en función del viewport, además de definir límites y zonas de fade.
    private void configurarTemplo() {
        if (temploTex == null || jugador == null || viewport == null) return;

        float texW = temploTex.getWidth() / PPU;
        float texH = temploTex.getHeight() / PPU;

        float desiredH = viewport.getWorldHeight() * TEMPLO_VIEWPORT_H_FRAC;
        float scale = desiredH / Math.max(0.0001f, texH);

        temploH = texH * scale;
        temploW = texW * scale;

        // Coloca el templo cerca del final, dejando parte fuera por la derecha.
        temploX = anchoNivel - temploW * (1f - TEMPLO_OUT_RIGHT_FRAC);

        // Anclaje vertical al suelo (ajuste fino para alinear entrada con el ground).
        temploY = getSueloY() - 1.11f;

        // Definición aproximada de zona de puerta (si se necesita para lógica adicional).
        puertaX0 = temploX + temploW * 0.47f;
        puertaX1 = temploX + temploW * 0.63f;

        // Limita a los enemigos para no sobrepasar el templo por la derecha.
        limiteEnemigosDerecha = temploX;
        gestorEnemigos.setLimiteDerecha(limiteEnemigosDerecha);

        // Cálculo de fade: final real coincide con borde derecho útil del nivel (según ancho del jugador).
        float playerW = jugador.getWidth(PPU);
        fadeEndX = anchoNivel - playerW;           // margen derecho real
        fadeStartX = fadeEndX - playerW * 1.2f;    // empieza antes
        if (fadeStartX < 0f) fadeStartX = 0f;
    }

    // Calcula alpha del jugador según su X dentro del intervalo [fadeStartX, fadeEndX].
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

    // Aplica bonus por tiempo una única vez al finalizar el nivel (transición a sala del jefe).
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

    // Lógica de "kill event": incrementa contador y spawnea corazón cada 10 enemigos.
    private void onEnemyKilled(float x, float y) {
        enemigosMatados++;

        if (enemigosMatados % 10 == 0) {
            float suelo = getSueloEntidadesY();
            HeartDrop d = new HeartDrop(x, Math.max(y, suelo + 0.6f), suelo);
            heartDrops.add(d);
        }
    }

    // Curación: se implementa reutilizando recibirDanio con valor negativo (según implementación del Jugador).
    private void curarJugador(int amount) {
        jugador.recibirDanio(-Math.abs(amount));
    }

    // Actualiza drops, aplica gravedad y detecta recolección por solape de hitboxes.
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

                // Vibración breve al recoger un corazón (si está activada y el periférico existe).
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

    // Dibujo de drops activos usando la región del HUD.
    private void drawHeartDrops() {
        if (heartDrops.size == 0 || heartDropRegion == null) return;

        for (int i = 0; i < heartDrops.size; i++) {
            HeartDrop d = heartDrops.get(i);
            float drawX = d.x - d.w * 0.5f;
            float drawY = d.y;
            juego.batch.draw(heartDropRegion, drawX, drawY, d.w, d.h);
        }
    }

    // Detiene y libera la música del nivel (recurso nativo), dejando el campo a null.
    private void pararMusicaNivel() {
        if (musicaNivel != null) {
            musicaNivel.stop();
            musicaNivel.dispose();
            musicaNivel = null;
        }
    }

    // ------------------------------------------------------------
    //  CONTROL AUDIO (SONIDO ON/OFF)
    // ------------------------------------------------------------
    // Sincroniza la música con la configuración: respeta pausa y muerte, crea/reproduce/para según corresponda.
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

    // Setter de estado de pausa usado por pantallas externas (ej. PantallaPausa).
    public void setEnPausa(boolean value) {
        this.enPausa = value;
    }

    // Calcula y actualiza el rectángulo del botón extra "Ir Boss" en el viewport de controles.
    private void recalcularBotonIrBossLayout() {
        if (!esMovil || controlesViewport == null) return;

        float sw = controlesViewport.getWorldWidth();
        float sh = controlesViewport.getWorldHeight();

        // Posición en esquina superior derecha con margen y separación.
        float size = IRBOSS_BTN_SIZE_PX;
        float x = sw - IRBOSS_BTN_MARGIN_PX - size - IRBOSS_BTN_GAP_PX - size;
        float y = sh - IRBOSS_BTN_MARGIN_PX - size;

        if (x < IRBOSS_BTN_MARGIN_PX) x = IRBOSS_BTN_MARGIN_PX;

        hbBotonIrBoss.set(x, y, size, size);
    }

    // Teletransporta al jugador cerca del trigger final, equivalente funcional a NUM_2.
    private void teleportToBossEntrance() {
        if (fadeEndX <= 0f) configurarTemplo();

        float playerW = jugador.getWidth(PPU);
        float x = fadeEndX - playerW * 0.9f;
        if (x < 0f) x = 0f;
        jugador.setX(x);
    }

    // Detecta pulsación puntual sobre el botón extra en móvil (usando justTouched + hitbox UI).
    private boolean botonIrBossJustPressed() {
        if (!esMovil) return false;
        if (controlesViewport == null) return false;
        if (botonIrBossTex == null) return false;

        if (!Gdx.input.justTouched()) return false;

        touchTmp.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        controlesViewport.unproject(touchTmp);

        return hbBotonIrBoss.contains(touchTmp.x, touchTmp.y);
    }

    // Renderiza el botón extra usando el viewport de controles (coordenadas de pantalla).
    private void drawBotonIrBoss() {
        if (!esMovil) return;
        if (botonIrBossTex == null) return;
        if (controlesViewport == null || controlesCam == null) return;

        controlesViewport.apply();
        juego.batch.setProjectionMatrix(controlesCam.combined);

        juego.batch.draw(botonIrBossTex, hbBotonIrBoss.x, hbBotonIrBoss.y, hbBotonIrBoss.width, hbBotonIrBoss.height);
    }

    @Override
    public void show() {

        // Asegura que la música se alinea con la configuración al entrar a la pantalla.
        syncMusicaSegunConfiguracion();

        // Evita reinicializaciones si show() se llama más de una vez.
        if (inicializado) return;
        inicializado = true;

        // Configuración de cámara/viewport del mundo.
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Detectar móvil y activar controles táctiles automáticamente.
        esMovil = (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android)
            || (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS);

        // Viewport de controles en coordenadas de pantalla.
        if (esMovil) {
            controlesCam = new OrthographicCamera();
            controlesViewport = new ScreenViewport(controlesCam);
            controlesViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

            controles = new ControlesTactiles(controlesViewport);
            Gdx.input.setInputProcessor(controles);

            // Reset de estados previos para detección de flancos (justPressed).
            prevSaltarTouch = false;
            prevDispararTouch = false;
            prevEspecialTouch = false;
            prevPausaTouch = false;

            // Botón extra: usar boton_saltar.png para ir directo a la entrada del boss.
            botonIrBossTex = new Texture("sprites/hud/controles/boton_saltar.png");
            setPixelArt(botonIrBossTex);
            recalcularBotonIrBossLayout();
        }

        // Carga/instanciación de animaciones y jugador.
        anims = new PlayerAnimations();
        jugador = new Jugador(anims);

        // Inicialización del fondo parallax y configuración de zoom/suelo.
        parallax = new ParallaxBackground(
            "sprites/fondos/capa_01.png",
            "sprites/fondos/capa_02.png",
            "sprites/fondos/capa_03.png",
            "sprites/fondos/capa_04.png"
        );

        parallax.setZoom(1.0f);
        parallax.setGroundFrac(GROUND_FRAC_FINAL);
        parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());

        // Fija objetivo de suelo para mantenerlo estable con resize.
        sueloObjetivoY = parallax.getGroundY() + AJUSTE_SUELO_FINAL;
        recalcularAjusteSueloParaMantenerObjetivo();

        // Assets y gestor de proyectiles del jugador.
        disparoAssets = new DisparoAssets();
        gestorProyectiles = new GestorProyectiles(disparoAssets);

        // Assets y gestor de efectos visuales (impactos).
        impactoAssets = new ImpactoAssets();
        gestorEfectos = new GestorEfectos(impactoAssets.impacto);
        gestorEfectos.setImpactoConfig(0.55f, 0.55f, 0.14f);

        // Carga de texturas de enemigos (serpiente) y configuración pixel art.
        serpienteWalk = new Texture("sprites/enemigos/serpiente/serpiente_walk.png");
        serpienteDeath = new Texture("sprites/enemigos/serpiente/serpiente_death.png");
        setPixelArt(serpienteWalk);
        setPixelArt(serpienteDeath);

        // Carga de texturas de enemigos (pájaro) y configuración pixel art.
        pajaroAttak = new Texture("sprites/enemigos/pajaro/pajaro_attack.png");
        pajaroDeath = new Texture("sprites/enemigos/pajaro/pajaro_dead.png");
        setPixelArt(pajaroAttak);
        setPixelArt(pajaroDeath);

        // Carga de texturas de enemigos (golem) y configuración pixel art.
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

        // Textura/región de roca para proyectiles del golem.
        rocaTex = new Texture("sprites/proyectiles/enemigo/golem_roca.png");
        setPixelArt(rocaTex);
        rocaRegion = new TextureRegion(rocaTex);

        // Gestor de enemigos con texturas base y configuración específica por tipo.
        gestorEnemigos = new GestorEnemigos(serpienteWalk, serpienteDeath, pajaroAttak, pajaroDeath, PPU);
        gestorEnemigos.setGolemTextures(golemIdle, golemWalk, golemThrow, golemAttack, golemDeath);
        gestorEnemigos.setRocaRegion(rocaRegion);

        // Límites de cámara basados en el ancho visible del viewport.
        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        // Posicionamiento inicial de cámara centrada en el viewport.
        camara.position.set(viewW / 2f, viewport.getWorldHeight() / 2f, 0f);
        camara.update();

        // Posición inicial del jugador en el centro visible y alineado al suelo de entidades.
        jugador.setX(viewW / 2f);
        jugador.setSueloY(getSueloEntidadesY());
        jugador.setY(getSueloEntidadesY());

        // Carga del templo final y configuración pixel art.
        temploTex = new Texture("sprites/fondos/entradaRuina.png");
        setPixelArt(temploTex);
        temploRegion = new TextureRegion(temploTex);

        // Configuración general de enemigos: suelo, animación, stats, y spawn según dificultad.
        gestorEnemigos.setYsuelo(getSueloEntidadesY());
        gestorEnemigos.setAnimacion(128, 80, 0.20f);
        gestorEnemigos.setStats(2.0f, 10);

        Configuracion.Dificultad d = Configuracion.getDificultad();
        if (d == Configuracion.Dificultad.FACIL) {
            gestorEnemigos.setSpawnConfig(3.5f, 3, 2f, anchoNivel - 2f, 3.5f);
        } else {
            gestorEnemigos.setSpawnConfig(2.2f, 6, 2f, anchoNivel - 2f, 2.5f);
        }

        // Ajuste vertical global para spawns/render de enemigos.
        gestorEnemigos.setYOffsetWorld(0.15f);

        // Configuración específica de pájaros: altura top basada en viewport + offsets.
        float yTopPantalla = getSueloEntidadesY() + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(2.4f, 2, yTopPantalla, 0.8f, 12.0f, 12, 0.60f);

        // Assets de veneno y configuración de ataques.
        venenoAssets = new VenenoAssets();
        gestorEnemigos.setVenenoRegion(venenoAssets.veneno);

        gestorEnemigos.setAtaques(12, 0.9f, 8, 1.8f);
        gestorEnemigos.setVenenoConfig(2.2f, 7.5f, 10.0f, 0.35f, 0.35f);

        // Configuración de templo y clamps iniciales de jugador.
        configurarTemplo();
        clampJugadorEnNivel();

        // Inicialización del HUD y sincronización de valores iniciales.
        hud = new Hud();
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hud.setVida(jugador.getVida());
        hud.setScore(score);
        hud.setTiempoSeg(tiempoPartida);

        // Región del corazón reutilizada para drops.
        heartDropRegion = hud.getHeartFullRegion();

        // Reset explícito de estado de partida.
        score = 0;
        tiempoPartida = 0f;
        nivelTerminado = false;
        enPausa = false;

        bonusTiempoAplicado = false;
        bonusTiempo = 0;

        // Limpieza de contadores/sets de hits y puntuación por enemigo.
        hitsPajaro.clear();
        hitsSerpiente.clear();
        hitsGolem.clear();
        puntuadoPajaro.clear();
        puntuadoSerpiente.clear();
        puntuadoGolem.clear();

        // Reset de drops y contador de kills.
        enemigosMatados = 0;
        heartDrops.clear();
    }

    @Override
    public void resize(int width, int height) {
        // Captura de suelo anterior para mantener posición relativa del jugador tras cambios de tamaño.
        float sueloAntes = (parallax != null) ? getSueloEntidadesY() : 0f;

        // Actualiza viewport del mundo y recalcula parallax y ajuste de suelo.
        viewport.update(width, height, true);

        if (parallax != null) {
            parallax.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
            recalcularAjusteSueloParaMantenerObjetivo();
        }

        // Recalcula límites de cámara según nuevo ancho visible.
        float viewW = viewport.getWorldWidth();
        limiteIzq = viewW / 2f;
        limiteDer = anchoNivel - viewW / 2f;

        // Ajusta Y del jugador para conservar separación respecto al suelo.
        float sueloDespues = getSueloEntidadesY();
        float deltaSuelo = sueloDespues - sueloAntes;

        jugador.setSueloY(sueloDespues);
        jugador.setY(jugador.getY() + deltaSuelo);

        // Actualiza suelo de enemigos y su configuración de pájaro dependiente del alto.
        gestorEnemigos.setYsuelo(sueloDespues);

        float yTopPantalla = sueloDespues + viewport.getWorldHeight() + 0.5f;
        gestorEnemigos.setPajaroConfig(2.4f, 2, yTopPantalla, 0.8f, 12.0f, 12, 0.60f);

        // Reconfigura templo y clamps del jugador con el nuevo viewport.
        configurarTemplo();
        clampJugadorEnNivel();

        // Redimensiona HUD.
        if (hud != null) hud.resize(width, height);

        // Actualizar viewport de controles.
        if (esMovil && controlesViewport != null) {
            controlesViewport.update(width, height, true);
        }
        if (esMovil && controles != null) {
            controles.recalcularLayout();
        }

        // Recalcular posición del botón extra.
        if (esMovil) {
            recalcularBotonIrBossLayout();
        }
    }

    @Override
    public void render(float delta) {

        // Mantiene la música en el estado correcto según configuración/pausa/muerte.
        syncMusicaSegunConfiguracion();

        // ------------------------------------------------------------
        //  PAUSA: teclado en PC + botón pausa en móvil
        // ------------------------------------------------------------
        // Detección de pulsación puntual del botón de pausa en móvil (flanco).
        boolean pausaTouchJust = false;
        if (esMovil && controles != null) {
            boolean pausaNow = controles.isPausa();
            pausaTouchJust = pausaNow && !prevPausaTouch;
            prevPausaTouch = pausaNow;
        }

        // Entrada de pausa: ESC/P en teclado o botón táctil.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.P)
            || pausaTouchJust) {
            enPausa = true;
            pauseMusicaNivel();
            juego.setScreen(new PantallaPausa(juego, this));
            return;
        }

        // Móvil: botón “ir al boss” (usa boton_saltar.png) hace lo mismo que NUM_2.
        if (esMovil && botonIrBossJustPressed()) {
            teleportToBossEntrance();
        }

        // Atajo teclado: teletransporta al jugador a la zona de entrada del boss (útil para test).
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            if (fadeEndX <= 0f) configurarTemplo();

            float playerW = jugador.getWidth(PPU);
            float x = fadeEndX - playerW * 0.9f;
            if (x < 0f) x = 0f;
            jugador.setX(x);
        }

        // Gestión de muerte: inicia secuencia, detiene música y transiciona tras animación.
        if (jugador != null && (jugador.isDead() || jugador.getVida() <= 0)) {

            if (!muerteEnCurso) {
                muerteEnCurso = true;
                tiempoMuerte = 0f;

                pararMusicaNivel();
            }

            tiempoMuerte += delta;

            if (tiempoMuerte >= DURACION_ANIM_DEAD) {

                if (Records.qualifies(score)) {
                    juego.setScreen(
                        new PantallaNuevoRecord(
                            juego,
                            score,
                            PantallaNuevoRecord.Destino.MUERTE
                        )
                    );
                } else {
                    juego.setScreen(new PantallaMuerte(juego));
                }

                dispose();
                return;
            }

        } else {
            // Si se hubiera activado muerteEnCurso y el jugador ya no está muerto, se resetea el estado.
            if (muerteEnCurso) {
                muerteEnCurso = false;
                tiempoMuerte = 0f;
            }
        }

        // Contador de tiempo solo activo si no está en pausa y el nivel no terminó.
        if (!enPausa && !nivelTerminado) {
            tiempoPartida += delta;
        }

        // Altura del jugador en mundo (usada para offsets de disparo).
        float h = jugador.getHeight(PPU);

        // ------------------------------------------------------------
        //  INPUT: teclado en PC, táctil en móvil
        // ------------------------------------------------------------
        float dir = 0f;

        // Movimiento horizontal por teclado o joystick táctil.
        if (!esMovil) {
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;
        } else if (controles != null) {
            dir = controles.getDirX();
        }

        // Saltar por teclado (justPressed) en desktop.
        boolean saltar = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        // Agacharse por teclado (pressed) en desktop.
        boolean agacharse = Gdx.input.isKeyPressed(Input.Keys.S)
            || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        // Disparo normal y especial por teclado (justPressed).
        boolean disparaNormal = Gdx.input.isKeyJustPressed(Input.Keys.J);
        boolean disparaEspecial = Gdx.input.isKeyJustPressed(Input.Keys.K);

        // Añadir táctil (justPressed) encima del teclado.
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

            // Se combinan inputs: si cualquiera dispara/salta, se considera true.
            saltar = saltar || saltarJust;
            disparaNormal = disparaNormal || dispararJust;
            disparaEspecial = disparaEspecial || especialJust;

            // Agacharse en móvil usando joystick hacia abajo.
            agacharse = agacharse || (controles.getDirY() < -0.55f);
        }

        // Aplica suelo actual y entrada del jugador (movimiento/salto/agachado).
        jugador.setSueloY(getSueloEntidadesY());
        jugador.aplicarEntrada(dir, saltar, agacharse, delta);

        // Clamps y actualización de alpha según entrada al templo.
        clampJugadorEnNivel();
        actualizarAlphaEntradaTemplo();

        // Condición de fin de nivel: jugador completamente "faded" y situado en el extremo final.
        if (!nivelTerminado && alphaJugador <= 0.01f && jugador.getX() >= fadeEndX - 0.001f) {
            nivelTerminado = true;
            aplicarBonusTiempoSiProcede();

            pararMusicaNivel();

            juego.setScreen(new PantallaSalaJefe(juego, jugador, score));
            return;
        }

        // Mantiene a enemigos alineados con el suelo actual y con límite derecho actualizado.
        gestorEnemigos.setYsuelo(getSueloEntidadesY());
        gestorEnemigos.setLimiteDerecha(limiteEnemigosDerecha);

        // Lógica de disparo: calcula origen y offsets según postura (standing/crouch) y tipo de disparo.
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

        // Limpieza de pantalla.
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Aplica viewport del mundo antes de renderizar escena.
        viewport.apply();

        // Seguimiento horizontal de cámara al jugador, clamped a límites del nivel.
        float objetivoX = jugador.getX() + (PlayerAnimations.FRAME_W / PPU) / 2f;
        camara.position.x = Math.max(limiteIzq, Math.min(objetivoX, limiteDer));
        camara.update();

        juego.batch.setProjectionMatrix(camara.combined);

        // Cálculo de extremos visibles para actualizaciones/dibujo acotados por cámara.
        float cameraLeftX = camara.position.x - viewport.getWorldWidth() / 2f;
        float viewW = viewport.getWorldWidth();
        float rightX = cameraLeftX + viewW;

        // Actualización de proyectiles, enemigos, ataques, efectos y drops.
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

        // SERPIENTE: procesa impactos de proyectiles normales, aplica conteo de hits, muerte, puntuación y efecto.
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

        // PÁJARO: similar, pero con hitbox dependiente de PPU y hits normalmente 1.
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

        // GOLEM: requiere múltiples hits antes de morir y puntuar.
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
        // Ataque especial (rayo): aplica daño por solape con hitbox del rayo y puntúa en transiciones (dying/dead).
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

        // Render del parallax en función de la cámara y el ancho visible.
        parallax.render(juego.batch, cameraLeftX, viewW);

        // Dibujo condicional del templo si entra en el rango visible (culling simple).
        if (temploX + temploW > cameraLeftX - 2f && temploX < rightX + 2f) {
            juego.batch.draw(temploRegion, temploX, temploY, temploW, temploH);
        }

        // Drops de corazón.
        drawHeartDrops();

        // Render de proyectiles, enemigos y efectos.
        gestorProyectiles.draw(juego.batch, cameraLeftX, viewW);
        gestorEnemigos.render(juego.batch, cameraLeftX, viewW);
        gestorEfectos.draw(juego.batch);

        // Preserva color del batch, aplica alpha al jugador y restaura.
        float pr = juego.batch.getColor().r;
        float pg = juego.batch.getColor().g;
        float pb = juego.batch.getColor().b;
        float pa = juego.batch.getColor().a;

        juego.batch.setColor(1f, 1f, 1f, alphaJugador);
        jugador.draw(juego.batch, PPU);
        juego.batch.setColor(pr, pg, pb, pa);

        juego.batch.end();

        // HUD + controles táctiles (en viewport de pantalla) se dibujan al final.
        if (hud != null) {
            juego.batch.begin();
            hud.setVida(jugador.getVida());
            hud.setScore(score);
            hud.setTiempoSeg(tiempoPartida);
            hud.draw(juego.batch);

            // Dibujar controles con su viewport de pantalla.
            if (esMovil && controles != null && controlesViewport != null) {
                controlesViewport.apply();
                juego.batch.setProjectionMatrix(controlesCam.combined);

                // Botón extra (al lado del de pausa).
                drawBotonIrBoss();

                controles.render(juego.batch);
            }

            juego.batch.end();
        }
    }

    @Override
    public void hide() {
        // Al ocultar la pantalla, detiene y libera música (evita que siga sonando en otras pantallas).
        pararMusicaNivel();
    }

    // Método público para detener la música del nivel desde fuera.
    public void stopMusicaNivel() {
        pararMusicaNivel();
    }

    // Pausa la música si está reproduciéndose.
    public void pauseMusicaNivel() {
        if (musicaNivel != null && musicaNivel.isPlaying()) musicaNivel.pause();
    }

    // Reanuda música si corresponde según configuración, recreándola si fuera necesario.
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
        // Liberación de recursos (texturas, assets y HUD) para evitar fugas de memoria nativa.
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

        // Liberar controles táctiles.
        if (controles != null) controles.dispose();

        // Liberar botón extra.
        if (botonIrBossTex != null) botonIrBossTex.dispose();

        // Garantiza que la música queda detenida y liberada.
        pararMusicaNivel();
    }
}
