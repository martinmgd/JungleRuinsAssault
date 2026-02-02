package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class ParallaxBackground implements Disposable {

    private final Texture bgTex;
    private final Texture midTex;
    private final Texture fgTex;

    private final float bgFactor  = 0.25f;
    private final float midFactor = 0.55f;
    private final float fgFactor  = 1.0f;

    private float zoom = 1.25f;

    private float bgDrawW, bgDrawH;
    private float midDrawW, midDrawH;
    private float fgDrawW, fgDrawH;

    private float bgY, midY, fgY;

    private final float midOverlapPx = 48f;

    private float groundFrac = 0.44f;

    private final float actionBottomExtra = 0.25f;

    public ParallaxBackground(String bgPath, String midPath, String fgPath) {
        bgTex = new Texture(bgPath);
        midTex = new Texture(midPath);
        fgTex = new Texture(fgPath);
    }

    public void resize(float worldW, float worldH) {
        float scale = (worldH / bgTex.getHeight()) * zoom;

        bgDrawW = bgTex.getWidth() * scale;
        bgDrawH = bgTex.getHeight() * scale;

        midDrawW = midTex.getWidth() * scale;
        midDrawH = midTex.getHeight() * scale;

        fgDrawW = fgTex.getWidth() * scale;
        fgDrawH = fgTex.getHeight() * scale;

        bgY = (worldH - bgDrawH) * 0.5f;

        float overlapWorld = midOverlapPx * scale;
        midY = (worldH - midDrawH) + 0.2f - overlapWorld;

        fgY = Math.min(0f, worldH - fgDrawH) - actionBottomExtra;
    }

    public void render(SpriteBatch batch, float cameraLeftX, float viewW) {
        drawTiled(batch, bgTex,  bgY,  bgDrawW,  bgDrawH,  cameraLeftX, viewW, bgFactor);
        drawTiled(batch, midTex, midY, midDrawW, midDrawH, cameraLeftX, viewW, midFactor);
        drawTiled(batch, fgTex,  fgY,  fgDrawW,  fgDrawH,  cameraLeftX, viewW, fgFactor);
    }

    private void drawTiled(SpriteBatch batch, Texture tex,
                           float y, float drawW, float drawH,
                           float cameraLeftX, float viewW, float factor) {

        float layerLeft = cameraLeftX * factor;

        float startTile = (float) Math.floor(layerLeft / drawW) * drawW;
        float end = layerLeft + viewW + drawW;

        for (float tileX = startTile; tileX < end; tileX += drawW) {
            float worldX = tileX - layerLeft + cameraLeftX;
            batch.draw(tex, worldX, y, drawW, drawH);
        }
    }

    public float getGroundY() {
        return fgY + fgDrawH * groundFrac;
    }

    public float getGroundFrac() {
        return groundFrac;
    }

    public void setGroundFrac(float frac) {
        this.groundFrac = frac;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    @Override
    public void dispose() {
        bgTex.dispose();
        midTex.dispose();
        fgTex.dispose();
    }
}
