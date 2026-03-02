package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class ParallaxBackground implements Disposable {

    // Capas del parallax en orden de fondo -> primer plano (última capa suele ser el FG).
    private final Texture[] layers;

    // Factores de desplazamiento por capa: valores menores mueven más lento (más "lejos"),
    // valores mayores mueven más rápido (más "cerca"). El FG normalmente usa 1.0f.
    private final float[] factors;

    // Zoom aplicado al escalado final del parallax (multiplica el tamaño renderizado).
    private float zoom = 1.0f;

    // Ancho/alto de dibujo calculados para cada capa, tras aplicar escalado.
    private final float[] drawW;
    private final float[] drawH;

    // Posición Y (vertical) calculada para cada capa en el mundo.
    private final float[] layerY;

    // Ajuste de "suelo" dentro del FG
    // Fracción de la altura del primer plano usada como referencia para la línea del suelo.
    private float groundFrac = 0.44f;

    // Se mantiene para no alterar demasiado tu lógica previa
    // Ajuste adicional hacia abajo para el FG (compensa composición/acción visible).
    private final float actionBottomExtra = 0.25f;

    // Altura de referencia para el escalado (mantiene el mismo tamaño en PC y móvil)
    // En lugar de escalar con el worldH real del viewport (que puede cambiar con ExtendViewport),
    // se usa una altura fija de referencia para mantener consistencia visual.
    private float referenceWorldH = Constantes.ALTO_MUNDO;

    public ParallaxBackground(String bgPath, String midPath, String fgPath) {
        // Construye un parallax de 3 capas (fondo, medio, primer plano) cargando texturas desde rutas.
        this.layers = new Texture[] {
            new Texture(bgPath),
            new Texture(midPath),
            new Texture(fgPath)
        };

        // Factores por defecto para 3 capas: fondo lento, medio intermedio, FG a velocidad completa.
        this.factors = new float[] { 0.25f, 0.55f, 1.0f };

        // Arrays auxiliares del mismo tamaño que layers para almacenar métricas precalculadas.
        this.drawW = new float[layers.length];
        this.drawH = new float[layers.length];
        this.layerY = new float[layers.length];
    }

    public ParallaxBackground(String bgPath, String midPath, String nearPath, String fgPath) {
        // Construye un parallax de 4 capas (fondo, medio, cercano, primer plano).
        this.layers = new Texture[] {
            new Texture(bgPath),
            new Texture(midPath),
            new Texture(nearPath),
            new Texture(fgPath)
        };

        // Factores por defecto para 4 capas, incrementando la velocidad a medida que se acerca a cámara.
        this.factors = new float[] { 0.18f, 0.40f, 0.70f, 1.0f };

        // Arrays auxiliares para dimensiones y posiciones precalculadas.
        this.drawW = new float[layers.length];
        this.drawH = new float[layers.length];
        this.layerY = new float[layers.length];
    }

    // Si algún día quieres probar otra referencia sin tocar Constantes
    public void setReferenceWorldHeight(float h) {
        // Define la altura de referencia usada para escalar las capas (evita valores demasiado pequeños).
        this.referenceWorldH = Math.max(0.01f, h);
    }

    public void resize(float worldW, float worldH) {
        // Recalcula tamaños de dibujo y posiciones verticales en función del mundo visible.
        // Debe llamarse cuando cambia el viewport/tamaño para mantener el encuadre correcto.
        if (layers.length == 0) return;

        // Importante: NO usamos worldH real del viewport para escalar,
        // porque con ExtendViewport puede variar entre PC y móvil.
        // Usamos una altura fija de referencia para mantener el tamaño "de antes".
        float scale = (referenceWorldH / layers[0].getHeight()) * zoom;

        // Calcula drawW/drawH para cada capa aplicando el mismo factor de escala.
        for (int i = 0; i < layers.length; i++) {
            drawW[i] = layers[i].getWidth() * scale;
            drawH[i] = layers[i].getHeight() * scale;
        }

        // Calcula la posición Y por capa. El objetivo es que las capas encajen visualmente
        // (centrado en algunas capas y "pegado" abajo en otras), con ajustes específicos para FG.
        if (layers.length == 3) {
            // Fondo: centrado vertical
            layerY[0] = (worldH - drawH[0]) * 0.5f;

            // Medio: alineación hacia arriba con pequeño offset
            layerY[1] = (worldH - drawH[1]) + 0.2f;

            // FG: alineación inferior con ajuste extra para mantener el área de acción
            layerY[2] = Math.min(0f, worldH - drawH[2]) - actionBottomExtra;
        } else {
            // En el caso de 4 capas, las tres primeras se alinean al borde superior.
            layerY[0] = worldH - drawH[0];
            layerY[1] = worldH - drawH[1];
            layerY[2] = worldH - drawH[2];

            // El FG se alinea al borde inferior con el mismo ajuste extra.
            layerY[3] = Math.min(0f, worldH - drawH[3]) - actionBottomExtra;
        }
    }

    public void render(SpriteBatch batch, float cameraLeftX, float viewW) {
        // Dibuja todas las capas de forma "tiled" horizontalmente para cubrir el ancho visible.
        // cameraLeftX: coordenada X del borde izquierdo de la cámara en el mundo.
        // viewW: ancho visible del mundo.
        for (int i = 0; i < layers.length; i++) {
            drawTiled(batch, layers[i], layerY[i], drawW[i], drawH[i], cameraLeftX, viewW, factors[i]);
        }
    }

    private void drawTiled(SpriteBatch batch, Texture tex,
                           float y, float drawW, float drawH,
                           float cameraLeftX, float viewW, float factor) {

        // Calcula el desplazamiento horizontal de esta capa en función del factor de parallax.
        // A mayor factor, más se mueve la capa con la cámara (más cercana).
        float layerLeft = cameraLeftX * factor;

        // Determina el primer tile a dibujar alineando a múltiplos de drawW hacia la izquierda.
        float startTile = (float) Math.floor(layerLeft / drawW) * drawW;

        // Determina hasta dónde dibujar: borde izquierdo + ancho visible + un tile extra por seguridad.
        float end = layerLeft + viewW + drawW;

        // Dibuja la textura repetida en horizontal cubriendo toda el área visible.
        for (float tileX = startTile; tileX < end; tileX += drawW) {
            // Convierte coordenadas del "espacio de capa" al "espacio de mundo" visible.
            float worldX = tileX - layerLeft + cameraLeftX;
            batch.draw(tex, worldX, y, drawW, drawH);
        }
    }

    public float getGroundY() {
        // Devuelve la coordenada Y del "suelo" basada en el primer plano (última capa).
        // Se calcula como el Y de la capa + una fracción de su altura.
        int fgIndex = layers.length - 1;
        return layerY[fgIndex] + drawH[fgIndex] * groundFrac;
    }

    public float getGroundFrac() {
        // Devuelve la fracción de altura del FG usada para calcular el suelo.
        return groundFrac;
    }

    public void setGroundFrac(float frac) {
        // Permite ajustar dinámicamente la fracción de suelo (sin clamping explícito).
        this.groundFrac = frac;
    }

    public void setZoom(float zoom) {
        // Ajusta el zoom del parallax (afecta al scale calculado en resize()).
        this.zoom = zoom;
    }

    public void setFactors(float... newFactors) {
        // Permite sobrescribir factores de parallax si coinciden en longitud con el array actual.
        if (newFactors == null || newFactors.length != factors.length) return;
        System.arraycopy(newFactors, 0, factors, 0, factors.length);
    }

    @Override
    public void dispose() {
        // Libera todas las texturas asociadas al parallax.
        for (Texture t : layers) {
            if (t != null) t.dispose();
        }
    }
}
