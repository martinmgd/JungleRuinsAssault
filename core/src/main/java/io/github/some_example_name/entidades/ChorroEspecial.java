package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ChorroEspecial {

    private enum Estado { START, BODY, END, FIN }

    private final Animation<TextureRegion> startAnim;
    private final TextureRegion bodyRegion;
    private final Animation<TextureRegion> endAnim;

    private Estado estado = Estado.START;

    private float stateTime = 0f;

    private float ox, oy;
    private boolean derecha = true;

    private float bodyLen = 0f;

    private float beamH = 6.0f;
    private float startH = 0f;
    private float startW = 0f;

    private float endH = 0f;
    private float endW = 0f;

    private float tileW = 1.2f;

    private float growSpeed = 50f;

    private float yOffset = -0.2f;

    private boolean holding = true;

    public ChorroEspecial(Animation<TextureRegion> startAnim,
                          TextureRegion bodyRegion,
                          Animation<TextureRegion> endAnim,
                          float ox, float oy, boolean derecha) {
        this.startAnim = startAnim;
        this.bodyRegion = bodyRegion;
        this.endAnim = endAnim;
        this.ox = ox;
        this.oy = oy;
        this.derecha = derecha;

        recomputeSizes();
    }

    public void setOrigin(float x, float y, boolean derecha) {
        this.ox = x;
        this.oy = y;
        this.derecha = derecha;
    }

    public void setHolding(boolean holding) {
        this.holding = holding;
        if (!holding && estado != Estado.END && estado != Estado.FIN) {
            estado = Estado.END;
            stateTime = 0f;
        }
    }

    public boolean isFinished() {
        return estado == Estado.FIN;
    }

    public void update(float delta, float camLeftX, float viewW) {
        if (estado == Estado.FIN) return;

        stateTime += delta;

        float maxLen = computeMaxLenToScreenEdge(camLeftX, viewW);

        if (estado == Estado.START) {
            if (startAnim.isAnimationFinished(stateTime)) {
                estado = Estado.BODY;
                stateTime = 0f;
                bodyLen = 0f;
            }
            return;
        }

        if (estado == Estado.BODY) {
            bodyLen += growSpeed * delta;

            if (bodyLen >= maxLen) {
                bodyLen = maxLen;
                estado = Estado.END;
                stateTime = 0f;
            }

            if (!holding) {
                estado = Estado.END;
                stateTime = 0f;
            }
            return;
        }

        if (estado == Estado.END) {
            if (endAnim.isAnimationFinished(stateTime)) {
                estado = Estado.FIN;
            }
        }
    }

    private float computeMaxLenToScreenEdge(float camLeftX, float viewW) {
        float rightX = camLeftX + viewW;
        if (derecha) return Math.max(0f, rightX - ox);
        return Math.max(0f, ox - camLeftX);
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        if (estado == Estado.FIN) return;

        float yCenter = oy + yOffset;
        float yBeam = yCenter - beamH * 0.5f;

        if (estado == Estado.START) {
            TextureRegion fr = startAnim.getKeyFrame(stateTime, false);
            float yStart = yCenter - startH * 0.5f;
            drawFlipped(batch, fr, ox, yStart, startW, startH);
            return;
        }

        float maxLen = computeMaxLenToScreenEdge(camLeftX, viewW);
        float len = (estado == Estado.END) ? maxLen : bodyLen;

        TextureRegion startFrame = startAnim.getKeyFrames()[startAnim.getKeyFrames().length - 1];
        float yStart = yCenter - startH * 0.5f;
        drawFlipped(batch, startFrame, ox, yStart, startW, startH);

        float tipX = derecha ? (ox + len) : (ox - len);

        float bodyFrom = derecha ? (ox + startW * 0.35f) : (ox - startW * 0.35f);
        float bodyTo = derecha ? (tipX - endW * 0.35f) : (tipX + endW * 0.35f);

        float bodyLenVisible = derecha ? (bodyTo - bodyFrom) : (bodyFrom - bodyTo);
        if (bodyLenVisible < 0f) bodyLenVisible = 0f;

        int tiles = (int) Math.ceil(bodyLenVisible / tileW);

        for (int i = 0; i < tiles; i++) {
            float bx = derecha ? (bodyFrom + i * tileW) : (bodyFrom - i * tileW - tileW);
            float w = tileW;

            if (derecha) {
                float endX = bx + w;
                if (endX > bodyTo) {
                    w = bodyTo - bx;
                    if (w <= 0f) continue;
                }
                batch.draw(bodyRegion, bx, yBeam, w, beamH);
            } else {
                if (bx < bodyTo) {
                    float over = bodyTo - bx;
                    w = tileW - over;
                    if (w <= 0f) continue;
                    bx = bodyTo;
                }
                batch.draw(bodyRegion, bx + w, yBeam, -w, beamH);
            }
        }

        TextureRegion endFrame = (estado == Estado.END)
            ? endAnim.getKeyFrame(stateTime, false)
            : endAnim.getKeyFrames()[0];

        float ex = derecha ? (tipX - endW * 0.10f) : (tipX - endW * 0.90f);
        float ey = yCenter - endH * 0.5f;

        drawFlipped(batch, endFrame, ex, ey, endW, endH);
    }

    private void drawFlipped(SpriteBatch batch, TextureRegion region, float x, float y, float w, float h) {
        if (derecha) {
            batch.draw(region, x, y, w, h);
        } else {
            batch.draw(region, x + w, y, -w, h);
        }
    }

    private void recomputeSizes() {
        startH = beamH * 0.45f;
        startW = startH * 1.20f;

        endH = beamH * 0.65f;
        endW = endH * 1.10f;

        tileW = beamH * 1.10f;
    }

    public void setBeamHeight(float h) {
        this.beamH = h;
        recomputeSizes();
    }

    public void setGrowSpeed(float speed) {
        this.growSpeed = speed;
    }

    public void setYOffset(float yOffset) {
        this.yOffset = yOffset;
    }
}
