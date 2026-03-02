package io.github.some_example_name.entidades.proyectiles.jugador;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class AtaqueEspecial {

    // Estados internos del ciclo de vida del ataque: carga, emisión, cierre y finalización.
    private enum Estado { BUILD, LOOP, END, FIN }

    // Animaciones utilizadas en cada fase del ataque.
    private final Animation<TextureRegion> buildAnim;
    private final Animation<TextureRegion> loopAnim;
    private final Animation<TextureRegion> endAnim;

    // Frames precalculados de la animación de construcción para control manual del avance.
    private final TextureRegion[] buildFrames;

    // Estado actual del ataque.
    private Estado estado = Estado.BUILD;

    // Tiempo acumulado del estado actual (segundos).
    private float stateTime = 0f;

    // Origen del ataque en coordenadas de mundo y dirección de disparo.
    private float ox, oy;
    private boolean derecha;

    // Longitud actual del haz y velocidad de crecimiento (unidades de mundo por segundo).
    private float len = 0f;
    private float growSpeed = 50f;

    // Altura del haz en unidades de mundo.
    private float beamH;

    // Altura mínima relativa durante la fase de construcción.
    private float buildMinFrac = 0.10f;

    // Desplazamiento vertical adicional aplicado al centro del haz.
    private float yOffset = 0.0f;

    // Tiempos de avance por frame durante la construcción (dos tramos de velocidad).
    private float buildFastFrameTime = 0.030f;
    private float buildSlowFrameTime = 0.060f;
    private int buildFrameIndex = 0;
    private float buildFrameTimer = 0f;

    // Hitbox reutilizable para colisiones durante las fases activas.
    private final Rectangle hitbox = new Rectangle();

    // Daño aplicado por el ataque especial.
    private int damage = 40;

    public AtaqueEspecial(Animation<TextureRegion> buildAnim,
                          Animation<TextureRegion> loopAnim,
                          Animation<TextureRegion> endAnim,
                          float ox, float oy, boolean derecha,
                          float viewH) {
        // Animaciones asociadas a cada fase del ataque.
        this.buildAnim = buildAnim;
        this.loopAnim = loopAnim;
        this.endAnim = endAnim;

        // Captura directa de frames de la animación de construcción.
        this.buildFrames = buildAnim.getKeyFrames();

        // Configuración del origen y dirección.
        this.ox = ox;
        this.oy = oy;
        this.derecha = derecha;

        // Altura inicial del haz basada en la altura del viewport con un mínimo absoluto.
        this.beamH = Math.max(0.15f, viewH * 0.10f);

        // Offset inicial vertical (permite ajustes externos posteriores).
        this.yOffset = 0.0f;
    }

    public void update(float delta, float camLeftX, float viewW) {
        // Si el ataque ha finalizado, no actualiza estado.
        if (estado == Estado.FIN) return;

        stateTime += delta;

        // Longitud máxima permitida según borde de pantalla (en coordenadas de cámara).
        float maxLen = computeMaxLenToScreenEdge(camLeftX, viewW);

        // Fase BUILD: avance manual de frames según tiempos configurados.
        if (estado == Estado.BUILD) {
            buildFrameTimer += delta;

            while (buildFrameTimer >= currentBuildStepTime()) {
                buildFrameTimer -= currentBuildStepTime();
                buildFrameIndex++;

                // Al completar la construcción, pasa a la fase LOOP y reinicia longitud y timers.
                if (buildFrameIndex >= buildFrames.length) {
                    estado = Estado.LOOP;
                    stateTime = 0f;
                    len = 0f;
                    return;
                }
            }
            return;
        }

        // Fase LOOP: crecimiento progresivo del haz hasta alcanzar el límite visible.
        if (estado == Estado.LOOP) {
            len += growSpeed * delta;

            // Al alcanzar la longitud máxima, transiciona a fase END.
            if (len >= maxLen) {
                len = maxLen;
                estado = Estado.END;
                stateTime = 0f;
            }
            return;
        }

        // Fase END: reproducción de animación de cierre hasta finalizar.
        if (estado == Estado.END) {
            if (endAnim.isAnimationFinished(stateTime)) {
                estado = Estado.FIN;
            }
        }
    }

    private float currentBuildStepTime() {
        // Selecciona el tiempo por frame según el tramo inicial o final de la construcción.
        return (buildFrameIndex < 6) ? buildFastFrameTime : buildSlowFrameTime;
    }

    private float computeMaxLenToScreenEdge(float camLeftX, float viewW) {
        // Calcula la longitud máxima del haz para que termine exactamente en el borde visible.
        float rightX = camLeftX + viewW;
        if (derecha) return Math.max(0f, rightX - ox);
        return Math.max(0f, ox - camLeftX);
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        // No dibuja si el ataque ha finalizado.
        if (estado == Estado.FIN) return;

        float yCenter = oy + yOffset;

        // En BUILD se dibuja el frame actual con interpolación de altura progresiva.
        if (estado == Estado.BUILD) {
            int idx = Math.min(buildFrameIndex, buildFrames.length - 1);
            TextureRegion fr = buildFrames[idx];

            float tt = (buildFrames.length <= 1) ? 1f : (idx / (float) (buildFrames.length - 1));

            float minH = beamH * buildMinFrac;
            float h = minH + (beamH - minH) * tt;

            float aspect = fr.getRegionWidth() / (float) fr.getRegionHeight();
            float w = h * aspect;

            float x = derecha ? ox : (ox - w);
            float y = yCenter - h * 0.5f;

            if (derecha) batch.draw(fr, x, y, w, h);
            else batch.draw(fr, x + w, y, -w, h);

            return;
        }

        // En LOOP/END se renderiza el frame de la animación correspondiente.
        TextureRegion fr = (estado == Estado.LOOP)
            ? loopAnim.getKeyFrame(stateTime, true)
            : endAnim.getKeyFrame(stateTime, false);

        float w = len;
        float h = beamH;

        float x = derecha ? ox : (ox - w);
        float y = yCenter - h * 0.5f;

        if (derecha) batch.draw(fr, x, y, w, h);
        else batch.draw(fr, x + w, y, -w, h);
    }

    public boolean isFinished() {
        // Indica si el ataque ha completado todas sus fases.
        return estado == Estado.FIN;
    }

    public void setGrowSpeed(float growSpeed) {
        // Ajusta la velocidad de crecimiento del haz.
        this.growSpeed = growSpeed;
    }

    public void setBeamHeightFrac(float frac, float viewH) {
        // Ajusta la altura del haz como fracción de la altura visible con un mínimo absoluto.
        this.beamH = Math.max(0.15f, viewH * frac);
    }

    public void setBuildMinFrac(float frac) {
        // Ajusta la altura mínima relativa durante la fase BUILD.
        this.buildMinFrac = frac;
    }

    public void setYOffset(float yOffset) {
        // Ajusta el desplazamiento vertical aplicado al centro del haz.
        this.yOffset = yOffset;
    }

    public void setBuildTimings(float fastFrameTime, float slowFrameTime) {
        // Ajusta la temporización de avance de frames durante la fase BUILD.
        this.buildFastFrameTime = fastFrameTime;
        this.buildSlowFrameTime = slowFrameTime;
    }

    public Rectangle getHitbox() {
        // La hitbox solo es válida durante las fases activas (LOOP y END).
        if (estado != Estado.LOOP && estado != Estado.END) return null;

        float yCenter = oy + yOffset;

        float w = len;
        float h = beamH;

        float x = derecha ? ox : (ox - w);
        float y = yCenter - h * 0.5f;

        hitbox.set(x, y, w, h);
        return hitbox;
    }

    public int getDamage() {
        // Devuelve el daño configurado del ataque especial.
        return damage;
    }

    public void setDamage(int damage) {
        // Ajusta el daño aplicado por el ataque especial.
        this.damage = damage;
    }
}
