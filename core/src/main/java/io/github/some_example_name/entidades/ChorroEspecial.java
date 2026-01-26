package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ChorroEspecial {

    private enum Fase { START, STREAM, FIN }

    private final Animation<TextureRegion> startAnim;
    private final TextureRegion streamRegion;

    private Fase fase = Fase.FIN;
    private float stateTime = 0f;

    private float originX;
    private float originY;
    private boolean derecha;

    private final float hMin;
    private final float hMax;

    private final float velCrecimiento;
    private final float velAvance;

    private final float yOffset;

    private float longitud = 0f;
    private float avance = 0f;
    private float longitudObjetivo = 0f;

    public ChorroEspecial(Animation<TextureRegion> startAnim,
                          TextureRegion streamRegion,
                          float hMin, float hMax,
                          float velCrecimiento,
                          float velAvance,
                          float yOffset) {
        this.startAnim = startAnim;
        this.streamRegion = streamRegion;
        this.hMin = hMin;
        this.hMax = hMax;
        this.velCrecimiento = velCrecimiento;
        this.velAvance = velAvance;
        this.yOffset = yOffset;
    }

    public void begin(float x, float y, boolean derecha, float cameraLeftX, float viewW) {
        this.originX = x;
        this.originY = y;
        this.derecha = derecha;

        this.fase = Fase.START;
        this.stateTime = 0f;

        this.longitud = 0f;
        this.avance = 0f;

        float edgeX = derecha ? (cameraLeftX + viewW) : cameraLeftX;
        this.longitudObjetivo = Math.abs(edgeX - originX);
    }

    public void setOrigin(float x, float y, boolean derecha) {
        this.originX = x;
        this.originY = y;
        this.derecha = derecha;
    }

    public void update(float delta, float cameraLeftX, float viewW) {
        if (fase == Fase.FIN) return;

        stateTime += delta;

        float edgeX = derecha ? (cameraLeftX + viewW) : cameraLeftX;
        longitudObjetivo = Math.abs(edgeX - originX);

        if (fase == Fase.START) {
            if (startAnim.isAnimationFinished(stateTime)) {
                fase = Fase.STREAM;
                stateTime = 0f;
            }
            return;
        }

        if (fase == Fase.STREAM) {
            longitud = Math.min(longitudObjetivo, longitud + velCrecimiento * delta);
            avance = Math.min(longitudObjetivo, avance + velAvance * delta);

            if (avance >= longitudObjetivo) {
                fase = Fase.FIN;
            }
        }
    }

    public void draw(SpriteBatch batch) {
        if (fase != Fase.STREAM) return;
        if (longitudObjetivo <= 0.001f) return;

        float t = Math.min(1f, longitud / longitudObjetivo);
        float h = hMin + (hMax - hMin) * t;

        float visibleLen = Math.min(avance, longitud);
        if (visibleLen <= 0.001f) return;

        float y = originY + yOffset;

        float segAspect = (float) streamRegion.getRegionWidth() / (float) streamRegion.getRegionHeight();
        float segW = h * segAspect;
        if (segW <= 0.0001f) segW = 0.5f;

        float remaining = visibleLen;

        if (derecha) {
            float x = originX;
            while (remaining > 0f) {
                float w = Math.min(segW, remaining);
                batch.draw(streamRegion, x, y, w, h);
                x += w;
                remaining -= w;
            }
        } else {
            float x = originX;
            while (remaining > 0f) {
                float w = Math.min(segW, remaining);
                batch.draw(streamRegion, x, y, -w, h);
                x -= w;
                remaining -= w;
            }
        }
    }

    public boolean isActive() {
        return fase != Fase.FIN;
    }

    public boolean isInStart() {
        return fase == Fase.START;
    }

    public TextureRegion getStartFrame() {
        return startAnim.getKeyFrame(stateTime, false);
    }
}
