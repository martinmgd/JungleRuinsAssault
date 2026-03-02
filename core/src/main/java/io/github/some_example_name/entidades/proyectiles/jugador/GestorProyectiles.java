package io.github.some_example_name.entidades.proyectiles.jugador;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import io.github.some_example_name.utilidades.DisparoAssets;
import io.github.some_example_name.utilidades.Configuracion;

public class GestorProyectiles {

    // ------------------------------------------------------------------
    // COLECCIONES / ESTADO DE PROYECTILES
    // ------------------------------------------------------------------

    // Lista de proyectiles normales activos (disparo estándar).
    private final Array<Proyectil> normales = new Array<>();

    // Referencia al ataque especial activo (solo puede existir uno a la vez).
    private AtaqueEspecial especial = null;

    // Assets de disparos (animaciones y texturas ya preparadas).
    private final DisparoAssets assets;

    // ------------------------------------------------------------------
    // AUDIO
    // ------------------------------------------------------------------

    // Sonido asociado al disparo normal.
    private Sound sonidoNormal;

    // Sonido asociado al ataque especial.
    private Sound sonidoEspecial;

    // Volúmenes de reproducción (0..1). Se mantienen como configuración interna.
    private float volNormal = 0.20f;
    private float volEspecial = 0.20f;

    // ------------------------------------------------------------------
    // CONFIGURACIÓN DISPARO NORMAL
    // ------------------------------------------------------------------

    // Velocidad del proyectil normal (unidades de mundo por segundo).
    private float velNormal = 18f;

    // Daño aplicado por cada impacto de un proyectil normal.
    private int dmgNormal = 10;

    // Tamaño de render/hitbox del proyectil normal en mundo.
    private float normalW = 0.8f;
    private float normalH = 0.8f;

    // Cooldown mínimo entre disparos normales.
    private float cdNormal = 0.12f;

    // Temporizador de cooldown actual del disparo normal.
    private float tNormal = 0f;

    // ------------------------------------------------------------------
    // CONFIGURACIÓN ATAQUE ESPECIAL
    // ------------------------------------------------------------------

    // Cooldown mínimo entre activaciones del ataque especial.
    private float cdEspecial = 0.60f;

    // Temporizador de cooldown actual del ataque especial.
    private float tEspecial = 0f;

    /*
     * Constructor:
     * - Recibe los assets de disparo (animaciones y texturas preparadas).
     * - Carga los sonidos desde el directorio internal de LibGDX.
     */
    public GestorProyectiles(DisparoAssets assets) {
        this.assets = assets;

        // Carga de sonidos (assets/audio/).
        // Se usan sonidos cortos (Sound) para reproducción inmediata y repetida.
        sonidoNormal = Gdx.audio.newSound(Gdx.files.internal("audio/ataque_normal.mp3"));
        sonidoEspecial = Gdx.audio.newSound(Gdx.files.internal("audio/ataque_especial.mp3"));
    }

    /*
     * Update del gestor:
     * - Actualiza timers de cooldown (normal y especial).
     * - Actualiza proyectiles normales y elimina los que están fuera de rango o marcados.
     * - Actualiza el ataque especial activo y lo limpia cuando termina.
     *
     * camLeftX y viewW se usan para definir el rango horizontal visible (con margen),
     * y así hacer limpieza de entidades fuera de escena.
     */
    public void update(float delta, float camLeftX, float viewW) {
        // Enfriamiento de disparos, clamped a 0 para evitar negativos.
        tNormal = Math.max(0f, tNormal - delta);
        tEspecial = Math.max(0f, tEspecial - delta);

        float rightX = camLeftX + viewW;

        // Actualiza proyectiles normales y elimina los inválidos.
        for (int i = normales.size - 1; i >= 0; i--) {
            Proyectil p = normales.get(i);
            p.update(delta);

            // Se elimina si el propio proyectil lo marca o si sale del rango de cámara con margen.
            if (p.isEliminar() || p.isOutOfRange(camLeftX - 3f, rightX + 3f)) {
                normales.removeIndex(i);
            }
        }

        // Ataque especial: se actualiza mientras exista y se libera cuando termina.
        if (especial != null) {
            especial.update(delta, camLeftX, viewW);
            if (especial.isFinished()) {
                especial = null;
            }
        }
    }

    /*
     * Render de proyectiles:
     * - Dibuja todos los proyectiles normales.
     * - Dibuja el ataque especial si está activo.
     */
    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        for (Proyectil p : normales) p.draw(batch);
        if (especial != null) especial.draw(batch, camLeftX, viewW);
    }

    /*
     * Disparo normal:
     * - Respeta cooldown (tNormal).
     * - Crea un proyectil con la animación normal y lo añade a la lista.
     * - Reproduce sonido si está cargado y el sonido está activado en configuración.
     *
     * Parámetros:
     * - x,y: punto de spawn en mundo.
     * - derecha: dirección del disparo (true -> +X, false -> -X).
     */
    public void shootNormal(float x, float y, boolean derecha) {
        if (tNormal > 0f) return;
        tNormal = cdNormal;

        float vx = derecha ? velNormal : -velNormal;

        normales.add(new Proyectil(
            assets.normalAnim,
            x, y,
            vx,
            derecha,
            normalW, normalH,
            dmgNormal
        ));

        // El sonido se condiciona a configuración para permitir desactivarlo globalmente.
        if (sonidoNormal != null && Configuracion.isSonidoActivado()) sonidoNormal.play(volNormal);
    }

    /*
     * Inicio del ataque especial:
     * - Solo permite uno simultáneo (especial != null).
     * - Respeta cooldown (tEspecial).
     * - Crea AtaqueEspecial con sus tres animaciones (build/loop/end).
     * - Ajusta el daño del especial.
     * - Reproduce sonido si procede.
     *
     * viewH se pasa al ataque especial para que adapte su lógica/alcance a la altura visible.
     */
    public void startEspecial(float x, float y, boolean derecha, float viewH) {
        if (especial != null) return;
        if (tEspecial > 0f) return;
        tEspecial = cdEspecial;

        especial = new AtaqueEspecial(
            assets.specialBuildAnim,
            assets.specialLoopAnim,
            assets.specialEndAnim,
            x, y, derecha, viewH
        );

        // Daño base del ataque especial (se puede parametrizar desde fuera si se decide).
        especial.setDamage(50);

        if (sonidoEspecial != null && Configuracion.isSonidoActivado()) sonidoEspecial.play(volEspecial);
    }

    /*
     * Indica si el ataque especial está actualmente activo.
     * Útil para UI, bloqueo de input o lógica de combate.
     */
    public boolean isEspecialActivo() {
        return especial != null;
    }

    /*
     * Acceso a la lista de proyectiles normales activos.
     * Útil para colisiones externas o debug.
     */
    public Array<Proyectil> getNormales() {
        return normales;
    }

    /*
     * Acceso al ataque especial activo (puede ser null).
     * Útil para colisiones externas, sincronización o debug.
     */
    public AtaqueEspecial getEspecial() {
        return especial;
    }

    // ------------------------------------------------------------------
    // CONFIGURACIÓN EN RUNTIME (OPCIONAL)
    // ------------------------------------------------------------------

    /*
     * Ajusta el volumen del disparo normal en tiempo de ejecución.
     * Se clampa a [0..1] para evitar valores inválidos.
     */
    public void setVolumenNormal(float v) {
        volNormal = Math.max(0f, Math.min(1f, v));
    }

    /*
     * Ajusta el volumen del ataque especial en tiempo de ejecución.
     * Se clampa a [0..1] para evitar valores inválidos.
     */
    public void setVolumenEspecial(float v) {
        volEspecial = Math.max(0f, Math.min(1f, v));
    }

    /*
     * Libera recursos de audio.
     * Debe llamarse cuando el gestor deja de usarse para evitar fugas de memoria nativa.
     */
    public void dispose() {
        if (sonidoNormal != null) {
            sonidoNormal.dispose();
            sonidoNormal = null;
        }
        if (sonidoEspecial != null) {
            sonidoEspecial.dispose();
            sonidoEspecial = null;
        }
    }
}
