package io.github.some_example_name.entidades.proyectiles.jugador;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class AtaqueEspecial {

    private enum Estado { BUILD, LOOP, END, FIN }

    private final Animation<TextureRegion> buildAnim;
    private final Animation<TextureRegion> loopAnim;
    private final Animation<TextureRegion> endAnim;

    private final TextureRegion[] buildFrames;

    private Estado estado = Estado.BUILD;

    private float stateTime = 0f;

    private float ox, oy;
    private boolean derecha;

    private float len = 0f;
    private float growSpeed = 50f;

    private float beamH;

    private float buildMinFrac = 0.10f;

    private float yOffset = 0.0f;

    private float buildFastFrameTime = 0.030f;
    private float buildSlowFrameTime = 0.060f;
    private int buildFrameIndex = 0;
    private float buildFrameTimer = 0f;

    private final Rectangle hitbox = new Rectangle();
    private int damage = 40;

    public AtaqueEspecial(Animation<TextureRegion> buildAnim,
                          Animation<TextureRegion> loopAnim,
                          Animation<TextureRegion> endAnim,
                          float ox, float oy, boolean derecha,
                          float viewH) {
        this.buildAnim = buildAnim;
        this.loopAnim = loopAnim;
        this.endAnim = endAnim;

        this.buildFrames = buildAnim.getKeyFrames();

        this.ox = ox;
        this.oy = oy;
        this.derecha = derecha;

        this.beamH = Math.max(0.15f, viewH * 0.10f);

        this.yOffset = 0.0f;
    }

    public void update(float delta, float camLeftX, float viewW) {
        if (estado == Estado.FIN) return;

        stateTime += delta;

        float maxLen = computeMaxLenToScreenEdge(camLeftX, viewW);

        if (estado == Estado.BUILD) {
            buildFrameTimer += delta;

            while (buildFrameTimer >= currentBuildStepTime()) {
                buildFrameTimer -= currentBuildStepTime();
                buildFrameIndex++;

                if (buildFrameIndex >= buildFrames.length) {
                    estado = Estado.LOOP;
                    stateTime = 0f;
                    len = 0f;
                    return;
                }
            }
            return;
        }

        if (estado == Estado.LOOP) {
            len += growSpeed * delta;

            if (len >= maxLen) {
                len = maxLen;
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

    private float currentBuildStepTime() {
        return (buildFrameIndex < 6) ? buildFastFrameTime : buildSlowFrameTime;
    }

    private float computeMaxLenToScreenEdge(float camLeftX, float viewW) {
        float rightX = camLeftX + viewW;
        if (derecha) return Math.max(0f, rightX - ox);
        return Math.max(0f, ox - camLeftX);
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        if (estado == Estado.FIN) return;

        float yCenter = oy + yOffset;

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
        return estado == Estado.FIN;
    }

    public void setGrowSpeed(float growSpeed) {
        this.growSpeed = growSpeed;
    }

    public void setBeamHeightFrac(float frac, float viewH) {
        this.beamH = Math.max(0.15f, viewH * frac);
    }

    public void setBuildMinFrac(float frac) {
        this.buildMinFrac = frac;
    }

    public void setYOffset(float yOffset) {
        this.yOffset = yOffset;
    }

    public void setBuildTimings(float fastFrameTime, float slowFrameTime) {
        this.buildFastFrameTime = fastFrameTime;
        this.buildSlowFrameTime = slowFrameTime;
    }

    public Rectangle getHitbox() {
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
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }
}
