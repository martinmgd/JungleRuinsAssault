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
import io.github.some_example_name.utilidades.SensorLuz;
import io.github.some_example_name.pantallas.PantallaVictoria;
import io.github.some_example_name.utilidades.Records;
import io.github.some_example_name.pantallas.PantallaNuevoRecord;

// Controles táctiles (ruta según tu proyecto)
import io.github.some_example_name.entidades.controles.ControlesTactiles;

import java.util.ArrayList;

public class PantallaSalaJefe implements Screen {

    // Píxeles por unidad (PPU) para convertir tamaños de sprites a unidades de mundo según entidad.
    private static final float PPU_PLAYER = 64f;
    private static final float PPU_BOSS = 128f;

    // Fracción de la altura de la sala usada como posición del suelo (en coordenadas de mundo).
    private static final float SUELO_FRAC_SALA = 0.28f;

    // BAJAMOS el boss para que no quede a la altura de la cabeza
    // Offset vertical aplicado al jefe respecto al suelo para ajustar su colocación.
    private static final float BOSS_Y_OFFSET = -1.70f;

    // Retraso para ver animación de muerte antes de cambiar de pantalla
    // Controla la transición tras muerte del jugador (espera para que se vea la animación).
    private boolean muerteIniciada = false;
    private float timerMuerte = 0f;
    private static final float RETARDO_MUERTE = 1.20f;

    // Referencia al juego principal y al jugador (se reutiliza el mismo objeto Jugador entre pantallas).
    private final Main game;
    private final Jugador jugador;

    // Cámara y viewport del mundo (coordenadas del nivel/sala).
    private OrthographicCamera camara;
    private Viewport viewport;

    // Texturas del fondo de sala y del jefe (dormido/despierto), y del meteoro enemigo.
    private Texture fondoSala;
    private Texture bossDormidoTex;
    private Texture bossDespiertoTex;
    private Texture meteoroTex;

    // Lógica del jefe y lista de proyectiles meteoros que genera.
    private Jefe jefe;
    private final ArrayList<ProyectilMeteoro> meteoros = new ArrayList<>();

    // Coordenada Y del suelo en la sala (en unidades de mundo).
    private float sueloY;

    // HUD (vida/tiempo/puntuación) y variables asociadas al estado de partida en esta pantalla.
    private Hud hud;
    private float tiempo = 0f;
    private int score = 0;

    // Assets para disparo del jugador y gestor de proyectiles del jugador (normales y especial).
    private DisparoAssets disparoAssets;
    private GestorProyectiles gestorProyectiles;

    // Música nivel 2
    private Music musicaJefe;

    // NUEVO: para que no se re-arranque música mientras estás en la pantalla de pausa
    // Bandera que evita reiniciar/reanudar la música durante el estado de pausa.
    private boolean enPausa = false;

    // ------------------------------------------------------------
    // CONTROLES TÁCTILES (SOLO MÓVIL)
    // - Saltar: joystick arriba (ControlesTactiles.isSaltar())
    // - Agacharse: joystick abajo (dirY < umbral)
    // - Disparo / Especial / Pausa: botones
    // ------------------------------------------------------------
    // Detección de plataforma y referencia al sistema de controles táctiles.
    private boolean esMovil = false;
    private ControlesTactiles controles;

    // Viewport UI para controles (para que se vean siempre y no dependan de la cámara del mundo)
    // Cámara/viewport dedicados a UI para que el HUD/controles sean independientes del mundo.
    private OrthographicCamera camUI;
    private Viewport viewportUI;

    // Para detectar "justPressed" en botones táctiles
    // Estados previos para emular "just pressed" en entradas táctiles.
    private boolean prevSaltarTouch = false;
    private boolean prevDispararTouch = false;
    private boolean prevEspecialTouch = false;
    private boolean prevPausaTouch = false;

    // Umbral para agacharse con joystick hacia abajo
    private static final float JOY_CROUCH_THRESHOLD = -0.55f;

    // ------------------------------------------------------------
    // COLISIONES Y DAÑO (1 corazón por golpe)
    // ------------------------------------------------------------
    // Daño por impacto en unidades del sistema de vida del jugador (aquí: 20 = un corazón completo).
    private static final int DANIO_CORAZON_COMPLETO = 20;

    // Ventana de invulnerabilidad tras recibir daño, para evitar hits múltiples en un solo contacto.
    private static final float INVULN_SEG = 0.85f;
    private float invulnTimer = 0f;

    // Rectángulos temporales reutilizables para evitar GC en chequeos de colisión.
    private final Rectangle hbJugadorTmp = new Rectangle();
    private final Rectangle hbBossTmp = new Rectangle();
    private final Rectangle hbMeteoroTmp = new Rectangle();

    // ------------------------------------------------------------
    // SENSOR DE LUZ (hardware real) + iluminación global de sala con meteoros
    // ------------------------------------------------------------
    // Textura blanca 1x1 para dibujar overlays (oscuridad) escalando sobre toda la sala.
    private Texture whitePixel;

    // Fallback si no hay sensor o no hay lectura
    // Oscuridad base mínima (overlay) aplicada siempre para mantener estética de sala "oscura".
    private static final float OSCURIDAD_BASE = 0.60f; // sala SIEMPRE “algo oscura” aunque no haya sensor

    // Mapeo LUX -> OSCURIDAD (ALPHA). Más alpha = más oscuro.
    // Como lo quieres:
    // - Lux BAJO (poca luz real): pantalla MÁS CLARA => alpha MÁS BAJO (pero NO transparente)
    // - Lux ALTO (mucha luz real): pantalla MÁS OSCURA => alpha MÁS ALTO
    private static final float LUX_MIN = 0f;
    private static final float LUX_MAX = 1500f;

    // Rango realista y visible (esto hace que SIEMPRE se note la oscuridad)
    private static final float ALPHA_POCALUZ = 0.40f; // lux bajo => menos overlay (más claro)
    private static final float ALPHA_MUCHALUZ = 0.82f; // lux alto => más overlay (más oscuro)

    //  Suavizado para que "se actualice" sin saltos
    // Constantes para interpolación temporal y respuesta perceptual del cambio (curva de contraste).
    private static final float SUAVIZADO_LUZ = 10f;
    private static final float CURVA_CONTRASTE = 0.70f;

    // Alpha base suavizada (la que realmente se usa)
    // Valores actuales/objetivo para un cambio suave de la oscuridad base.
    private float alphaBaseActual = OSCURIDAD_BASE;
    private float alphaBaseObjetivo = OSCURIDAD_BASE;

    //  ILUMINACIÓN GLOBAL POR METEOROS (TODA LA SALA)
    // Número máximo de meteoros a considerar para el cálculo de aclarado sostenido.
    private static final int ILUM_MAX_METEOROS = 6;

    //  Aclarado sostenido (mientras hay meteoros vivos)
    // Cantidad máxima de reducción de oscuridad aportada por meteoros vivos en pantalla.
    private static final float ILUM_SOSTENIDA_MAX_REDUCE = 0.48f;

    //  FLASH GLOBAL MUY NOTORIO cuando se crean meteoros
    // Flash breve al spawnear meteoros para enfatizar el ataque del jefe.
    private static final float FLASH_REDUCE_OSCURIDAD = 0.65f;
    private static final float FLASH_DURACION = 0.35f;
    private float flashTimer = 0f;

    //  Luz global extra que aguanta un poco después del disparo
    // Persistencia de aclarado global tras el disparo/spawn de meteoros (decae en el tiempo).
    private static final float LUZ_SALA_REDUCE_MAX = 0.40f;
    private static final float LUZ_SALA_DURACION = 0.85f;
    private float luzSalaTimer = 0f;

    // Para detectar disparo: si sube el contador de meteoros
    // Variable auxiliar para comparar cambios en la cantidad de meteoros.
    private int meteorosPrevCount = 0;

    // ------------------------------------------------------------
    //  HITBOX BOSS (AJUSTADO) + BARRA VIDA BOSS (UI)
    // ------------------------------------------------------------
    // Ajustes para encoger/desplazar hitbox del jefe respecto a su sprite, mejorando sensación de colisión.
    private static final float BOSS_HB_INSET_X_FRAC = 0.18f;   // 18% a cada lado
    private static final float BOSS_HB_INSET_Y_FRAC = 0.10f;   // 10% arriba/abajo
    private static final float BOSS_HB_SHIFT_Y_FRAC = -0.06f;  // baja un poco el hitbox

    // Parámetros de la barra de vida del jefe en UI (coordenadas de pantalla/viewport UI).
    private static final float BOSS_BAR_W_FRAC = 0.42f;        // 42% ancho pantalla (no llega de score a reloj)
    private static final float BOSS_BAR_H_PX = 18f;            // altura visible
    private static final float BOSS_BAR_TOP_MARGIN_PX = 10f;   // margen superior
    private static final float BOSS_BAR_BORDER_PX = 3f;        // borde

    // ------------------------------------------------------------
    //  DAÑO AL JEFE POR DISPAROS DEL JUGADOR
    // ------------------------------------------------------------
    // Daño por proyectil normal y por ticks del ataque especial.
    private static final int DANIO_DISPARO_NORMAL = 10;
    private static final int DANIO_TICK_ESPECIAL = 8;

    // Cadencia de aplicación de daño del ataque especial (tick damage) cuando solapa con el jefe.
    private static final float ESPECIAL_TICK_CD = 0.12f;
    private float especialTickTimer = 0f;

    public PantallaSalaJefe(Main game, Jugador jugador, int scoreInicial) {
        // Recibe referencias necesarias para la sala: juego, jugador persistente y puntuación acumulada.
        this.game = game;
        this.jugador = jugador;
        this.score = scoreInicial;
    }

    public Jugador getJugador() {
        // Exposición del jugador para otras pantallas (p.ej. pausa) sin duplicar estado.
        return jugador;
    }

    public int getScore() {
        // Devuelve la puntuación actual para uso en transiciones (victoria/muerte/records).
        return score;
    }

    // ------------------------------------------------------------
    // Métodos para que PantallaPausaJefe pueda controlar la música
    // ------------------------------------------------------------
    public void pauseMusica() {
        // Pausa música si existe y está sonando.
        if (musicaJefe != null && musicaJefe.isPlaying()) {
            musicaJefe.pause();
        }
    }

    public void resumeMusica() {
        //  respeta SONIDO ON/OFF y evita arrancar en pausa
        // Reanuda música sólo si no se está en pausa y la configuración permite sonido.
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
        // Detiene y libera recursos de la música (Music ocupa recursos nativos).
        if (musicaJefe != null) {
            musicaJefe.stop();
            musicaJefe.dispose();
            musicaJefe = null;
        }
    }
    // ------------------------------------------------------------

    private void syncMusicaSegunConfiguracion() {
        // Sincroniza estado de música con la configuración y el estado de la pantalla (pausa/muerte).
        if (muerteIniciada) return;

        if (enPausa) {
            pauseMusica();
            return;
        }

        boolean sonidoOn = Configuracion.isSonidoActivado();

        if (!sonidoOn) {
            stopMusica();
            return;
        }

        // Lazy init: crea Music sólo cuando es necesaria, evitando cargarla si el usuario desactiva sonido.
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
        // Configura filtros/wrap para estilo pixel-art y evita bleeding al samplear en bordes.
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    private Texture crearWhitePixel() {
        // Crea una textura 1x1 blanca para overlays y primitivas rectangulares.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    @Override
    public void show() {

        // Estado inicial: no en pausa y música sincronizada según configuración.
        enPausa = false;
        syncMusicaSegunConfiguracion();

        // Reinicia control de muerte/transición.
        muerteIniciada = false;
        timerMuerte = 0f;

        // Reinicia timers de invulnerabilidad y efectos de iluminación.
        invulnTimer = 0f;
        flashTimer = 0f;
        luzSalaTimer = 0f;
        meteorosPrevCount = 0;
        especialTickTimer = 0f;

        // Inicializa oscuridad base (suavizada) al valor base.
        alphaBaseActual = OSCURIDAD_BASE;
        alphaBaseObjetivo = OSCURIDAD_BASE;

        // Configuración del mundo: cámara ortográfica y viewport extendido.
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Detecta si el dispositivo es móvil para activar controles táctiles.
        esMovil = (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android)
            || (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS);

        // Configuración de UI: cámara/viewport de pantalla completa para HUD/controles.
        camUI = new OrthographicCamera();
        viewportUI = new ScreenViewport(camUI);
        viewportUI.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        if (esMovil) {
            // Inicializa controles táctiles vinculados al viewport de UI y los establece como input processor.
            controles = new ControlesTactiles(viewportUI);
            Gdx.input.setInputProcessor(controles);

            // Reset de estados previos para detección de justPressed táctil.
            prevSaltarTouch = false;
            prevDispararTouch = false;
            prevEspecialTouch = false;
            prevPausaTouch = false;
        }

        // Carga de texturas principales del nivel/sala del jefe.
        fondoSala = new Texture("sprites/fondos/salaBoss2.png");
        bossDormidoTex = new Texture("sprites/enemigos/jefe/BossFinalDormido.png");
        bossDespiertoTex = new Texture("sprites/enemigos/jefe/BossFinal.png");
        meteoroTex = new Texture("sprites/proyectiles/enemigo/TresProyectile.png");

        // Configura todas las texturas para estilo pixel-art y wrapping correcto.
        setPixelArt(fondoSala);
        setPixelArt(bossDormidoTex);
        setPixelArt(bossDespiertoTex);
        setPixelArt(meteoroTex);

        // Inyecta textura y PPU a la clase de meteoros (configuración estática compartida).
        ProyectilMeteoro.setTextura(meteoroTex);
        ProyectilMeteoro.setPPU(300f);

        //  overlay para oscuridad
        // Crea el pixel blanco usado para overlays de oscuridad y lo configura para pixel-art.
        whitePixel = crearWhitePixel();
        setPixelArt(whitePixel);

        // Calcula posición del suelo en la sala.
        sueloY = viewport.getWorldHeight() * SUELO_FRAC_SALA;

        // Inicializa al jugador en el suelo y cerca del lado izquierdo de la sala.
        jugador.setSueloY(sueloY);
        jugador.setY(sueloY);
        jugador.setX(viewport.getWorldWidth() * 0.10f);

        // Calcula posición del jefe: al lado derecho, dejando margen, y ajustando su altura con offset.
        float bossW = bossDormidoTex.getWidth() / PPU_BOSS;
        float bossX = viewport.getWorldWidth() - bossW - 0.8f;
        if (bossX < 0.8f) bossX = 0.8f;

        float bossY = sueloY + BOSS_Y_OFFSET;

        // Parámetros base de comportamiento del jefe (cadencia de disparo y cooldown de salto).
        long cadenciaDisparoMs = 1400L;
        long cooldownSaltoMs = 2600L;

        // Ajuste de dificultad: en fácil, el jefe ataca/salta con menor frecuencia.
        Configuracion.Dificultad d = Configuracion.getDificultad();
        if (d == Configuracion.Dificultad.FACIL) {
            cadenciaDisparoMs = 2200L;
            cooldownSaltoMs = 3600L;
        }

        // Construye el jefe con parámetros de movimiento, vida, cadencias y escalas (PPU).
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

        // Inicializa assets y gestor de proyectiles del jugador.
        disparoAssets = new DisparoAssets();
        gestorProyectiles = new GestorProyectiles(disparoAssets);

        // Inicializa HUD y ajusta al tamaño actual de pantalla.
        hud = new Hud();
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Centra la cámara del mundo en la sala.
        camara.position.set(
            viewport.getWorldWidth() / 2f,
            viewport.getWorldHeight() / 2f,
            0f
        );
        camara.update();

        // Estado previo de meteoros para detectar nuevos spawns en el primer update.
        meteorosPrevCount = meteoros.size();
    }

    @Override
    public void render(float dt) {

        // Mantiene música sincronizada con configuración incluso si se cambia en runtime.
        syncMusicaSegunConfiguracion();

        // Detección de pausa táctil con "just pressed" manual.
        boolean pausaTouchJust = false;
        if (esMovil && controles != null) {
            boolean pausaNow = controles.isPausa();
            pausaTouchJust = pausaNow && !prevPausaTouch;
            prevPausaTouch = pausaNow;
        }

        // Entrada a pantalla de pausa (teclas o botón táctil).
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P) ||
            pausaTouchJust) {

            enPausa = true;
            pauseMusica();
            game.setScreen(new PantallaPausaJefe(game, this));
            return;
        }

        // Lógica de muerte del jugador: espera antes de cambiar de pantalla para respetar animación.
        if (jugador.isDead() || jugador.getVida() <= 0) {

            if (!muerteIniciada) {
                muerteIniciada = true;
                timerMuerte = 0f;
                stopMusica();
            }

            // Mantiene al jugador actualizándose sin entrada (para animación/estado interno).
            jugador.aplicarEntrada(0f, false, false, dt);

            timerMuerte += dt;
            if (timerMuerte >= RETARDO_MUERTE) {

                // Si el score entra en TOP10, ir a pantalla de guardar record (Destino MUERTE)
                if (Records.qualifies(score)) {
                    game.setScreen(
                        new PantallaNuevoRecord(
                            game,
                            score,
                            PantallaNuevoRecord.Destino.MUERTE
                        )
                    );
                } else {
                    game.setScreen(new PantallaMuerte(game));
                }

                return;
            }

            // Renderiza escena durante el retardo para mostrar la muerte/estado final.
            dibujarEscena();
            return;
        }

        // Acumula tiempo para HUD.
        tiempo += dt;

        // Update principal (inputs, IA jefe, proyectiles, colisiones, iluminación, etc.).
        update(dt);

        // Victoria: si el jefe muere, transiciona a record o victoria y libera recursos.
        if (jefe != null && !jefe.isVivo()) {

            stopMusica();

            if (Records.qualifies(score)) {
                game.setScreen(
                    new PantallaNuevoRecord(
                        game,
                        score,
                        PantallaNuevoRecord.Destino.VICTORIA
                    )
                );
            } else {
                game.setScreen(new PantallaVictoria(game, score));
            }

            dispose();
            return;
        }

        // Render normal.
        dibujarEscena();
    }

    private float clamp01(float v) {
        // Clamp básico a [0,1] para valores interpolados/normalizados.
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private float calcularAlphaOscuridadBaseDesdeSensor() {
        // Si no hay sensor, aplica el fallback de oscuridad base.
        if (!SensorLuz.isDisponible()) {
            return OSCURIDAD_BASE;
        }

        // Lee lux del sensor (valor físico de iluminación).
        float lux = SensorLuz.getLux();

        // Limita lux al rango definido para evitar valores extremos.
        if (lux < LUX_MIN) lux = LUX_MIN;
        if (lux > LUX_MAX) lux = LUX_MAX;

        // Normaliza lux a [0,1] dentro del rango.
        float t = (lux - LUX_MIN) / Math.max(0.0001f, (LUX_MAX - LUX_MIN));
        t = clamp01(t);

        // Aplica curva perceptual para ajustar contraste de respuesta.
        t = (float) Math.pow(t, CURVA_CONTRASTE);

        // lux bajo => alpha bajo (más claro)
        // lux alto => alpha alto (más oscuro)
        return ALPHA_POCALUZ + (ALPHA_MUCHALUZ - ALPHA_POCALUZ) * t;
    }

    private void dibujarOscuridadYFlash() {
        // Dibuja el overlay de oscuridad sobre TODA la sala (mundo), combinando:
        // - oscuridad base (sensor o fallback)
        // - aclarado por meteoros vivos
        // - flash al crear meteoros
        // - aclarado temporal post-disparo
        if (whitePixel == null) return;

        float alphaBase = alphaBaseActual;

        // contar meteoros vivos
        int vivos = 0;
        for (int i = 0; i < meteoros.size(); i++) {
            if (meteoros.get(i) != null && meteoros.get(i).isVivo()) vivos++;
        }
        if (vivos > ILUM_MAX_METEOROS) vivos = ILUM_MAX_METEOROS;

        // iluminación sostenida global mientras haya meteoros
        float ratio = vivos / (float) ILUM_MAX_METEOROS;
        float reduceSostenido = ILUM_SOSTENIDA_MAX_REDUCE * ratio;

        // flash global
        float flashT = 0f;
        if (flashTimer > 0f) {
            flashT = flashTimer / FLASH_DURACION;
            if (flashT > 1f) flashT = 1f;
            if (flashT < 0f) flashT = 0f;
        }
        float reduceFlash = FLASH_REDUCE_OSCURIDAD * flashT;

        // luz de sala global después del disparo
        float salaT = 0f;
        if (luzSalaTimer > 0f) {
            salaT = luzSalaTimer / LUZ_SALA_DURACION;
            if (salaT > 1f) salaT = 1f;
            if (salaT < 0f) salaT = 0f;
        }
        float reduceSala = LUZ_SALA_REDUCE_MAX * (salaT * salaT);

        // Alpha final: base menos reducciones por iluminación/flash.
        float alpha = alphaBase - reduceSostenido - reduceFlash - reduceSala;

        // límites: base visible + cambios visibles
        if (alpha < 0.10f) alpha = 0.10f;
        if (alpha > 0.92f) alpha = 0.92f;

        // Guarda color actual del batch para restaurarlo después del overlay.
        float pr = game.batch.getColor().r;
        float pg = game.batch.getColor().g;
        float pb = game.batch.getColor().b;
        float pa = game.batch.getColor().a;

        // Dibuja overlay negro con alpha calculado cubriendo todo el mundo.
        game.batch.setColor(0f, 0f, 0f, alpha);
        game.batch.draw(whitePixel, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Restaura color previo del batch.
        game.batch.setColor(pr, pg, pb, pa);
    }

    private void drawBossHealthBarUI() {
        // Renderiza barra de vida del jefe en coordenadas de UI (pantalla), sólo si:
        // - jefe existe, está vivo y despierto
        // - whitePixel y viewport/cámara UI están disponibles
        if (jefe == null || whitePixel == null || viewportUI == null || camUI == null) return;
        if (!jefe.isVivo()) return;
        if (jefe.isDormido()) return;

        int vida = jefe.getVida();
        int vidaMax = jefe.getVidaMax();
        if (vidaMax <= 0) return;

        // Ratio de vida (clamp a [0,1]).
        float ratio = vida / (float) vidaMax;
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;

        float screenW = viewportUI.getWorldWidth();
        float screenH = viewportUI.getWorldHeight();

        // Dimensiones y posición de la barra centrada arriba.
        float barW = screenW * BOSS_BAR_W_FRAC;
        float barH = BOSS_BAR_H_PX;

        float x = (screenW - barW) * 0.5f;
        float y = screenH - BOSS_BAR_TOP_MARGIN_PX - barH;

        // Guarda color actual para restauración posterior.
        float pr = game.batch.getColor().r;
        float pg = game.batch.getColor().g;
        float pb = game.batch.getColor().b;
        float pa = game.batch.getColor().a;

        // Fondo/contorno oscuro (caja) con borde.
        game.batch.setColor(0f, 0f, 0f, 0.65f);
        game.batch.draw(
            whitePixel,
            x - BOSS_BAR_BORDER_PX,
            y - BOSS_BAR_BORDER_PX,
            barW + BOSS_BAR_BORDER_PX * 2f,
            barH + BOSS_BAR_BORDER_PX * 2f
        );

        // Fondo de la barra (rojo oscuro).
        game.batch.setColor(0.30f, 0.05f, 0.05f, 0.95f);
        game.batch.draw(whitePixel, x, y, barW, barH);

        // Relleno de vida restante (rojo más brillante).
        game.batch.setColor(0.90f, 0.10f, 0.10f, 0.98f);
        game.batch.draw(whitePixel, x, y, barW * ratio, barH);

        // Brillo superior sutil para dar volumen.
        game.batch.setColor(1f, 1f, 1f, 0.12f);
        game.batch.draw(whitePixel, x, y + barH * 0.55f, barW, barH * 0.45f);

        // Restaura color original del batch.
        game.batch.setColor(pr, pg, pb, pa);
    }

    private void dibujarEscena() {
        // Limpia la pantalla antes de renderizar el frame.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render del mundo (sala).
        viewport.apply();
        game.batch.setProjectionMatrix(camara.combined);

        game.batch.begin();

        // Fondo de la sala, estirado a tamaño de mundo.
        game.batch.draw(fondoSala, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Dibuja jefe, meteoros y proyectiles del jugador, y al jugador.
        if (jefe != null) jefe.draw(game.batch);

        for (ProyectilMeteoro m : meteoros) m.draw(game.batch);

        gestorProyectiles.draw(game.batch, 0f, viewport.getWorldWidth());

        jugador.draw(game.batch, PPU_PLAYER);

        //  Overlay oscuro al final (CONTROL GLOBAL DE LA SALA)
        // Se dibuja después de entidades para afectar a toda la escena.
        dibujarOscuridadYFlash();

        game.batch.end();

        // Render de HUD y UI (en espacio de pantalla).
        game.batch.begin();
        hud.setVida(jugador.getVida());
        hud.setScore(score);
        hud.setTiempoSeg(tiempo);
        hud.draw(game.batch);

        // Barra de vida del jefe en UI (requiere proyección de cámara UI).
        if (viewportUI != null && camUI != null) {
            viewportUI.apply();
            game.batch.setProjectionMatrix(camUI.combined);
            drawBossHealthBarUI();
        }

        // Controles táctiles por encima de todo el UI si es móvil.
        if (esMovil && controles != null && viewportUI != null && camUI != null) {
            viewportUI.apply();
            game.batch.setProjectionMatrix(camUI.combined);
            controles.render(game.batch);
        }

        game.batch.end();
    }

    private Rectangle getHitboxJugador(Rectangle out) {
        // Construye el hitbox del jugador en unidades de mundo usando su posición y tamaño a PPU_PLAYER.
        float jx = jugador.getX();
        float jy = jugador.getY();
        float jw = jugador.getWidth(PPU_PLAYER);
        float jh = jugador.getHeight(PPU_PLAYER);
        out.set(jx, jy, jw, jh);
        return out;
    }

    private Rectangle getHitboxBoss(Rectangle out) {
        // Construye hitbox ajustado del jefe (inset + shift) para un feeling más justo.
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
        // Meteoro se considera centrado en (x,y), por eso se ajusta a esquina inferior izquierda con -0.5*w/h.
        float w = m.getAncho();
        float h = m.getAlto();
        out.set(m.getX() - w * 0.5f, m.getY() - h * 0.5f, w, h);
        return out;
    }

    private void aplicarDanioJugador() {
        // Aplica daño al jugador respetando invulnerabilidad temporal y vibración si está habilitada.
        if (invulnTimer > 0f) return;

        jugador.recibirDanio(DANIO_CORAZON_COMPLETO);

        if (Configuracion.isVibracionActivada()
            && Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator)) {
            Gdx.input.vibrate(45);
        }

        invulnTimer = INVULN_SEG;
    }

    private void comprobarColisionesBossYProyectiles() {
        // Colisiones que dañan al jugador:
        // - contacto con jefe mientras está saltando
        // - impacto con meteoros vivos
        Rectangle hbJ = getHitboxJugador(hbJugadorTmp);

        if (jefe != null && jefe.isSaltando()) {
            Rectangle hbB = getHitboxBoss(hbBossTmp);
            if (hbB.overlaps(hbJ)) {
                aplicarDanioJugador();
            }
        }

        for (int i = meteoros.size() - 1; i >= 0; i--) {
            ProyectilMeteoro m = meteoros.get(i);
            if (!m.isVivo()) continue;

            Rectangle hbM = getHitboxMeteoro(m, hbMeteoroTmp);
            if (hbM.overlaps(hbJ)) {
                aplicarDanioJugador();
                meteoros.remove(i);
            }
        }
    }

    private void comprobarColisionesDisparosContraJefe(float dt) {
        // Colisiones de los proyectiles del jugador contra el jefe:
        // - disparos normales: daño instantáneo
        // - ataque especial: daño por ticks con cooldown
        if (jefe == null || !jefe.isVivo()) return;

        // Mientras está dormido no recibe ticks de especial (y se resetea el temporizador).
        if (jefe.isDormido()) {
            especialTickTimer = 0f;
            return;
        }

        Rectangle hbB = getHitboxBoss(hbBossTmp);

        // Disparos normales: si overlap con hitbox del jefe, aplica daño y marca proyectil para eliminar.
        for (int i = gestorProyectiles.getNormales().size - 1; i >= 0; i--) {
            Proyectil p = gestorProyectiles.getNormales().get(i);
            if (p.isEliminar()) continue;

            if (hbB.overlaps(p.getHitbox())) {
                jefe.recibirDanio(DANIO_DISPARO_NORMAL);
                p.marcarEliminar();
            }
        }

        // Ataque especial: si existe y su hitbox solapa con jefe, aplica daño a intervalos.
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
            // Si deja de solapar, se reinicia para que el tick no "arrastre" tiempo previo.
            especialTickTimer = 0f;
        }
    }

    private void update(float dt) {

        // Actualiza temporizadores de invulnerabilidad y efectos de iluminación.
        if (invulnTimer > 0f) invulnTimer -= dt;
        if (invulnTimer < 0f) invulnTimer = 0f;

        if (flashTimer > 0f) flashTimer -= dt;
        if (flashTimer < 0f) flashTimer = 0f;

        if (luzSalaTimer > 0f) luzSalaTimer -= dt;
        if (luzSalaTimer < 0f) luzSalaTimer = 0f;

        //  LUZ (HARDWARE): recalcular objetivo CADA FRAME y suavizar
        // Calcula el objetivo desde el sensor y suaviza hacia él (filtro exponencial).
        alphaBaseObjetivo = calcularAlphaOscuridadBaseDesdeSensor();
        float lerp = 1f - (float) Math.exp(-SUAVIZADO_LUZ * dt);
        alphaBaseActual += (alphaBaseObjetivo - alphaBaseActual) * lerp;

        // Clamps adicionales para asegurar visibilidad mínima y evitar saturación total.
        if (alphaBaseActual < 0.15f) alphaBaseActual = 0.15f;
        if (alphaBaseActual > 0.95f) alphaBaseActual = 0.95f;

        // INPUT
        // Dirección horizontal del jugador: teclado en PC, joystick táctil en móvil.
        float dir = 0f;

        if (!esMovil) {
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;
        } else if (controles != null) {
            dir = controles.getDirX();
        }

        // Entrada base por teclado (PC).
        boolean saltar =
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        boolean agacharse =
            Gdx.input.isKeyPressed(Input.Keys.S) ||
                Gdx.input.isKeyPressed(Input.Keys.DOWN);

        boolean disparaNormal = Gdx.input.isKeyJustPressed(Input.Keys.J);
        boolean disparaEspecial = Gdx.input.isKeyJustPressed(Input.Keys.K);

        // Entrada adicional por controles táctiles (móvil) con detección de "just pressed".
        if (esMovil && controles != null) {

            boolean saltarNow = controles.isSaltar();
            boolean saltarJust = saltarNow && !prevSaltarTouch;
            prevSaltarTouch = saltarNow;

            boolean dispararNow = controles.isDisparar();
            boolean especialNow = controles.isEspecial();

            boolean dispararJust = dispararNow && !prevDispararTouch;
            boolean especialJust = especialNow && !prevEspecialTouch;

            prevDispararTouch = dispararNow;
            prevEspecialTouch = especialNow;

            // Combina entradas de teclado (si existieran) con las táctiles.
            saltar = saltar || saltarJust;
            disparaNormal = disparaNormal || dispararJust;
            disparaEspecial = disparaEspecial || especialJust;

            // Agacharse por joystick hacia abajo (umbral).
            float dy = controles.getDirY();
            agacharse = agacharse || (dy < JOY_CROUCH_THRESHOLD);
        }

        // Aplica entrada al jugador y fija su referencia de suelo.
        jugador.setSueloY(sueloY);
        jugador.aplicarEntrada(dir, saltar, agacharse, dt);

        // Clamps de movimiento horizontal del jugador dentro de los límites de la sala.
        float playerW = jugador.getWidth(PPU_PLAYER);
        float minX = 0f;
        float maxX = viewport.getWorldWidth() - playerW;

        float x = jugador.getX();
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;
        jugador.setX(x);

        // Gestión de disparos: calcula origen (muzzle) y offsets según postura (de pie/agachado).
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

        // Actualiza proyectiles del jugador y elimina/limita fuera de bounds.
        gestorProyectiles.update(dt, 0f, viewport.getWorldWidth());

        //  Detectar disparo del boss para iluminación GLOBAL de la sala
        // Se compara tamaño de la lista antes/después del update del jefe para detectar spawns.
        int before = meteoros.size();

        if (jefe != null) {
            jefe.update(dt, jugador, 0, 0, meteoros);
        }

        int after = meteoros.size();

        //  Si han aparecido meteoros nuevos: flash + luz de sala
        if (after > before) {
            flashTimer = FLASH_DURACION;
            luzSalaTimer = LUZ_SALA_DURACION;
        }

        meteorosPrevCount = after;

        // Actualiza meteoros y elimina los que ya no están vivos.
        for (int i = meteoros.size() - 1; i >= 0; i--) {
            ProyectilMeteoro m = meteoros.get(i);
            m.update(dt);
            if (!m.isVivo()) meteoros.remove(i);
        }

        // Chequeos de colisión: meteoros/jefe -> jugador y disparos jugador -> jefe.
        comprobarColisionesBossYProyectiles();
        comprobarColisionesDisparosContraJefe(dt);
    }

    @Override
    public void resize(int width, int height) {
        // Actualiza viewport del mundo y HUD al cambiar tamaño (rotación/ventana).
        viewport.update(width, height, true);
        hud.resize(width, height);

        // Actualiza viewport UI y recalcula layout de controles táctiles.
        if (viewportUI != null) viewportUI.update(width, height, true);
        if (esMovil && controles != null) controles.recalcularLayout();

        // Recalcula suelo y ajusta al jugador para que no quede por debajo tras el resize.
        sueloY = viewport.getWorldHeight() * SUELO_FRAC_SALA;
        jugador.setSueloY(sueloY);
        if (jugador.getY() < sueloY) jugador.setY(sueloY);
    }

    @Override public void pause() {}

    @Override public void resume() {
        // Al volver a esta pantalla (p.ej. tras pausa), se marca como no pausada y se reanuda música si procede.
        enPausa = false;
        resumeMusica();
    }

    @Override
    public void hide() {
        // No hacemos stop aquí a propósito.
        // Se delega la decisión de parar música/recursos al flujo de pantallas (pausa/victoria/muerte).
    }

    @Override
    public void dispose() {
        // Libera música (incluye dispose nativo) y recursos de texturas/objetos auxiliares.
        stopMusica();

        if (fondoSala != null) fondoSala.dispose();
        if (bossDormidoTex != null) bossDormidoTex.dispose();
        if (bossDespiertoTex != null) bossDespiertoTex.dispose();
        if (meteoroTex != null) meteoroTex.dispose();
        if (hud != null) hud.dispose();
        if (disparoAssets != null) disparoAssets.dispose();

        if (whitePixel != null) whitePixel.dispose();

        if (controles != null) controles.dispose();
    }
}
