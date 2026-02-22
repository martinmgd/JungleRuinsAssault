package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class ParallaxBackground implements Disposable {

    private final Texture[] layers;
    private final float[] factors;

    private float zoom = 1.0f;

    private final float[] drawW;
    private final float[] drawH;
    private final float[] layerY;

    // Ajuste de "suelo" dentro del FG
    private float groundFrac = 0.44f;

    // Se mantiene para no alterar demasiado tu lógica previa
    private final float actionBottomExtra = 0.25f;

    // Altura de referencia para el escalado (mantiene el mismo tamaño en PC y móvil)
    private float referenceWorldH = Constantes.ALTO_MUNDO;

    public ParallaxBackground(String bgPath, String midPath, String fgPath) {
        this.layers = new Texture[] {
            new Texture(bgPath),
            new Texture(midPath),
            new Texture(fgPath)
        };

        this.factors = new float[] { 0.25f, 0.55f, 1.0f };

        this.drawW = new float[layers.length];
        this.drawH = new float[layers.length];
        this.layerY = new float[layers.length];
    }

    public ParallaxBackground(String bgPath, String midPath, String nearPath, String fgPath) {
        this.layers = new Texture[] {
            new Texture(bgPath),
            new Texture(midPath),
            new Texture(nearPath),
            new Texture(fgPath)
        };

        this.factors = new float[] { 0.18f, 0.40f, 0.70f, 1.0f };

        this.drawW = new float[layers.length];
        this.drawH = new float[layers.length];
        this.layerY = new float[layers.length];
    }

    // Si algún día quieres probar otra referencia sin tocar Constantes
    public void setReferenceWorldHeight(float h) {
        this.referenceWorldH = Math.max(0.01f, h);
    }

    public void resize(float worldW, float worldH) {
        if (layers.length == 0) return;

        // Importante: NO usamos worldH real del viewport para escalar,
        // porque con ExtendViewport puede variar entre PC y móvil.
        // Usamos una altura fija de referencia para mantener el tamaño "de antes".
        float scale = (referenceWorldH / layers[0].getHeight()) * zoom;

        for (int i = 0; i < layers.length; i++) {
            drawW[i] = layers[i].getWidth() * scale;
            drawH[i] = layers[i].getHeight() * scale;
        }

        if (layers.length == 3) {
            layerY[0] = (worldH - drawH[0]) * 0.5f;
            layerY[1] = (worldH - drawH[1]) + 0.2f;
            layerY[2] = Math.min(0f, worldH - drawH[2]) - actionBottomExtra;
        } else {
            layerY[0] = worldH - drawH[0];
            layerY[1] = worldH - drawH[1];
            layerY[2] = worldH - drawH[2];

            layerY[3] = Math.min(0f, worldH - drawH[3]) - actionBottomExtra;
        }
    }

    public void render(SpriteBatch batch, float cameraLeftX, float viewW) {
        for (int i = 0; i < layers.length; i++) {
            drawTiled(batch, layers[i], layerY[i], drawW[i], drawH[i], cameraLeftX, viewW, factors[i]);
        }
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
        int fgIndex = layers.length - 1;
        return layerY[fgIndex] + drawH[fgIndex] * groundFrac;
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

    public void setFactors(float... newFactors) {
        if (newFactors == null || newFactors.length != factors.length) return;
        System.arraycopy(newFactors, 0, factors, 0, factors.length);
    }

    @Override
    public void dispose() {
        for (Texture t : layers) {
            if (t != null) t.dispose();
        }
    }
}
