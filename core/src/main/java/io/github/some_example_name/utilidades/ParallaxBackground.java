package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class ParallaxBackground implements Disposable {

    private final Texture bgTex;
    private final Texture midTex;
    private final Texture fgTex;

    // Velocidad relativa de cada capa
    private final float bgFactor  = 0.25f;
    private final float midFactor = 0.55f;
    private final float fgFactor  = 1.0f;

    // Zoom global del fondo (hace las capas "más grandes")
    private float zoom = 1.25f;

    // Tamaños dibujados (unidades de mundo)
    private float bgDrawW, bgDrawH;
    private float midDrawW, midDrawH;
    private float fgDrawW, fgDrawH;

    // Posición Y de cada capa (unidades mundo)
    private float bgY, midY, fgY;

    // Solape para tapar el halo blanco de la intermedia
    private final float midOverlapPx = 48f;

    // Posición del camino dentro de la capa de acción (fracción del alto)
    // ↓ ESTE ES EL VALOR CLAVE ↓
    private float groundFrac = 0.44f;

    // Margen inferior para que la acción tape siempre el borde
    private final float actionBottomExtra = 0.25f;

    public ParallaxBackground(String bgPath, String midPath, String fgPath) {
        bgTex = new Texture(bgPath);
        midTex = new Texture(midPath);
        fgTex = new Texture(fgPath);
    }

    /** Recalcula escalas y posiciones cuando cambia el viewport */
    public void resize(float worldW, float worldH) {
        // Escala tomando el fondo como referencia
        float scale = (worldH / bgTex.getHeight()) * zoom;

        bgDrawW = bgTex.getWidth()  * scale;
        bgDrawH = bgTex.getHeight() * scale;

        midDrawW = midTex.getWidth()  * scale;
        midDrawH = midTex.getHeight() * scale;

        fgDrawW = fgTex.getWidth()  * scale;
        fgDrawH = fgTex.getHeight() * scale;

        // Fondo: ligeramente centrado para subir montañas
        bgY = (worldH - bgDrawH) * 0.5f;

        // Intermedia: arriba + solape para eliminar halos
        float overlapWorld = midOverlapPx * scale;
        midY = (worldH - midDrawH) + 0.2f - overlapWorld;

        // Acción: anclada abajo, sin huecos
        fgY = Math.min(0f, worldH - fgDrawH) - actionBottomExtra;
    }

    /** Dibuja las 3 capas con parallax */
    public void render(SpriteBatch batch, float cameraLeftX, float viewW) {
        drawTiled(batch, bgTex,  bgY,  bgDrawW,  bgDrawH,  cameraLeftX, viewW, bgFactor);
        drawTiled(batch, midTex, midY, midDrawW, midDrawH, cameraLeftX, viewW, midFactor);
        drawTiled(batch, fgTex,  fgY,  fgDrawW,  fgDrawH,  cameraLeftX, viewW, fgFactor);
    }

    /** Dibujo infinito en X */
    private void drawTiled(SpriteBatch batch, Texture tex,
                           float y, float drawW, float drawH,
                           float cameraLeftX, float viewW, float factor) {

        float x = -(cameraLeftX * factor);

        float startX = x % drawW;
        if (startX > 0) startX -= drawW;

        for (float dx = startX; dx < startX + viewW + drawW; dx += drawW) {
            batch.draw(tex, dx, y, drawW, drawH);
        }
    }

    /** Y del camino de la capa de acción (suelo real) */
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
