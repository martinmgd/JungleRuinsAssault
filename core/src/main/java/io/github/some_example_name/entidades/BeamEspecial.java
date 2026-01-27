package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BeamEspecial {

    private enum Fase { START, BODY, END, DONE }

    private final Animation<TextureRegion> startAnim;
    private final TextureRegion bodyRegion;
    private final Animation<TextureRegion> endAnim;

    private Fase fase = Fase.START;
    private float stateTime = 0f;

    private float originX;
    private float originY;
    private boolean derecha = true;

    private boolean holdThisFrame = false;
    private float holdGrace = 0.08f; // si sueltas, espera un poco antes de cortar
    private float holdTimer = 0f;

    // Crecimiento
    private float currentLen = 0f;
    private float growSpeed = 80f; // unidades mundo / s (ajusta)
    private float reachAndStopTime = 0.06f;

    private float reachedTimer = 0f;

    // Tamaños (unidades mundo)
    private float beamH;       // 50–70% de pantalla, lo pasamos desde Gestor
    private float startW;
    private float startH;
    private float endW;
    private float endH;

    // Ajuste fino: baja el beam respecto al arma (en unidades mundo)
    private float yOffset = -0.2f;

    public BeamEspecial(Animation<TextureRegion> startAnim,
                        TextureRegion bodyRegion,
                        Animation<TextureRegion> endAnim,
                        float originX, float originY,
                        boolean derecha,
                        float viewH) {
        this.startAnim = startAnim;
        this.bodyRegion = bodyRegion;
        this.endAnim = endAnim;

        this.originX = originX;
        this.originY = originY;
        this.derecha = derecha;

        // Alto entre 50% y 70% del total de pantalla
        this.beamH = viewH * 0.60f;

        // Start y End relativamente grandes, pero no tanto como el body
        this.startH = beamH * 0.60f;
        this.startW = startH * 1.2f;

        this.endH = beamH * 0.80f;
        this.endW = endH * 1.0f;
    }

    public void setYOffset(float yOffset) {
        this.yOffset = yOffset;
    }

    public void setBeamHeightFrac(float frac, float viewH) {
        this.beamH = viewH * frac;
        this.startH = beamH * 0.60f;
        this.startW = startH * 1.2f;
        this.endH = beamH * 0.80f;
        this.endW = endH * 1.0f;
    }

    public void hold(float x, float y, boolean derecha) {
        this.originX = x;
        this.originY = y;
        this.derecha = derecha;
        this.holdThisFrame = true;
    }

    public void update(float delta, float camLeftX, float viewW) {
        stateTime += delta;

        // Hold logic
        if (holdThisFrame) {
            holdTimer = holdGrace;
        } else {
            holdTimer -= delta;
        }
        holdThisFrame = false;

        float screenRight = camLeftX + viewW;

        float maxLen;
        if (derecha) {
            maxLen = Math.max(0f, screenRight - originX);
        } else {
            maxLen = Math.max(0f, originX - camLeftX);
        }

        if (fase == Fase.START) {
            if (startAnim.isAnimationFinished(stateTime)) {
                fase = Fase.BODY;
                stateTime = 0f;
                currentLen = 0f;
            }
            return;
        }

        if (fase == Fase.BODY) {
            // crece rapidísimo hasta el final
            currentLen += growSpeed * delta;
            if (currentLen >= maxLen) {
                currentLen = maxLen;
                reachedTimer += delta;

                // cuando ya llegó al final y pasó un instante, corta
                // o si ya no estás holdeando
                if (reachedTimer >= reachAndStopTime || holdTimer <= 0f) {
                    fase = Fase.END;
                    stateTime = 0f;
                }
            } else {
                // si sueltas antes de llegar, cortamos igual
                if (holdTimer <= 0f) {
                    fase = Fase.END;
                    stateTime = 0f;
                }
            }
            return;
        }

        if (fase == Fase.END) {
            if (endAnim.isAnimationFinished(stateTime)) {
                fase = Fase.DONE;
            }
        }
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        if (fase == Fase.DONE) return;

        float y = originY + yOffset;

        if (fase == Fase.START) {
            TextureRegion fr = startAnim.getKeyFrame(stateTime, false);
            drawFlipped(batch, fr, originX, y, startW, startH, derecha);
            return;
        }

        // BODY + END
        float startX = originX;

        // Dibuja start (frame final del start como “boquilla” fija)
        TextureRegion startFrame = startAnim.getKeyFrame(startAnim.getAnimationDuration(), false);
        drawFlipped(batch, startFrame, startX, y, startW, startH, derecha);

        float bodyStartX = derecha ? (startX + startW * 0.65f) : (startX - startW * 0.65f);
        float len = currentLen;

        // Dibuja cuerpo “tiled”
        float segmentW = beamH * 0.55f; // ancho de cada repetición (ajusta)
        if (segmentW <= 0.01f) segmentW = 0.5f;

        int steps = (int)Math.ceil(len / segmentW);
        if (steps < 1) steps = 1;

        for (int i = 0; i < steps; i++) {
            float dx = Math.min(segmentW, len - i * segmentW);
            if (dx <= 0f) break;

            float px = derecha
                ? bodyStartX + i * segmentW
                : bodyStartX - i * segmentW - dx;

            // body: estiramos/encogemos en X según dx
            drawFlipped(batch, bodyRegion, px, y, dx, beamH, derecha);
        }

        // Punta/end en el borde
        float tipX;
        if (derecha) {
            tipX = Math.min(camLeftX + viewW, originX + len);
        } else {
            tipX = Math.max(camLeftX, originX - len);
        }

        if (fase == Fase.END) {
            TextureRegion endFrame = endAnim.getKeyFrame(stateTime, false);
            float ex = derecha ? (tipX - endW * 0.45f) : (tipX - endW * 0.55f);
            drawFlipped(batch, endFrame, ex, y - (endH - beamH) * 0.2f, endW, endH, derecha);
        } else {
            // si aún está en BODY y ya llegó al borde, puedes mostrar un end “suave” (opcional)
            // aquí lo dejamos sin end hasta que entre en fase END
        }
    }

    private void drawFlipped(SpriteBatch batch, TextureRegion region,
                             float x, float y, float w, float h, boolean derecha) {
        if (derecha) {
            batch.draw(region, x, y, w, h);
        } else {
            batch.draw(region, x + w, y, -w, h);
        }
    }

    public boolean isDone() {
        return fase == Fase.DONE;
    }
}
