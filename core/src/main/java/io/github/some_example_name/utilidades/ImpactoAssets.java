package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class ImpactoAssets implements Disposable {

    // Textura final generada a partir del Pixmap.
    // Se mantiene como campo para poder liberarla posteriormente en dispose().
    private final Texture tex;

    // Región de textura expuesta públicamente para dibujar el efecto de impacto.
    // Representa la textura completa generada dinámicamente.
    public final TextureRegion impacto;

    /*
     * Constructor:
     * Genera programáticamente una textura de impacto usando un Pixmap.
     * El diseño se crea dibujando círculos, picos radiales y algunos píxeles
     * para dar un efecto visual de explosión o chispa.
     */
    public ImpactoAssets() {

        // Se crea un Pixmap de 32x32 píxeles con formato RGBA.
        // Este Pixmap actúa como un lienzo donde se dibuja el efecto.
        Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);

        // Se limpia el fondo con transparencia total.
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        // Coordenadas del centro del sprite.
        int cx = 16;
        int cy = 16;

        // -----------------------------
        // Núcleo del impacto (círculos)
        // -----------------------------

        // Primer círculo exterior.
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(cx, cy, 7);

        // Círculo interior para reforzar el brillo central.
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(cx, cy, 5);

        // Núcleo más pequeño en el centro.
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(cx, cy, 3);

        // Borde circular para delimitar el núcleo del impacto.
        pm.setColor(1f, 1f, 1f, 1f);
        pm.drawCircle(cx, cy, 8);

        // -----------------------------
        // Picos radiales (destello)
        // -----------------------------
        // Se dibujan líneas en varias direcciones para simular energía o chispas.

        pm.setColor(1f, 1f, 1f, 1f);
        drawSpike(pm, cx, cy, 0, 9, 12);
        drawSpike(pm, cx, cy, 45, 8, 11);
        drawSpike(pm, cx, cy, 90, 9, 12);
        drawSpike(pm, cx, cy, 135, 7, 10);
        drawSpike(pm, cx, cy, 180, 9, 12);
        drawSpike(pm, cx, cy, 225, 8, 11);
        drawSpike(pm, cx, cy, 270, 9, 12);
        drawSpike(pm, cx, cy, 315, 7, 10);

        // -----------------------------
        // Píxeles dispersos (chispas)
        // -----------------------------
        // Se añaden algunos puntos alrededor del núcleo para dar más dinamismo.

        pm.setColor(1f, 1f, 1f, 1f);
        putDot(pm, cx + 11, cy + 2);
        putDot(pm, cx - 10, cy + 3);
        putDot(pm, cx + 4, cy + 11);
        putDot(pm, cx - 3, cy - 11);
        putDot(pm, cx + 9, cy - 6);
        putDot(pm, cx - 9, cy - 5);

        // Conversión del Pixmap a textura GPU.
        tex = new Texture(pm);

        // El Pixmap ya no es necesario una vez creada la textura.
        pm.dispose();

        // Región de textura que representa todo el sprite generado.
        impacto = new TextureRegion(tex);
    }

    /*
     * Dibuja un "pico" o rayo radial desde el centro del impacto.
     *
     * Parámetros:
     * - pm: pixmap donde se dibuja
     * - cx, cy: centro del impacto
     * - deg: ángulo del rayo en grados
     * - r0: radio inicial (desde donde empieza el rayo)
     * - r1: radio final (longitud total del rayo)
     */
    private void drawSpike(Pixmap pm, int cx, int cy, int deg, int r0, int r1) {

        // Conversión del ángulo de grados a radianes.
        double a = Math.toRadians(deg);

        // Punto inicial del rayo.
        int x0 = cx + (int) Math.round(Math.cos(a) * r0);
        int y0 = cy + (int) Math.round(Math.sin(a) * r0);

        // Punto final del rayo.
        int x1 = cx + (int) Math.round(Math.cos(a) * r1);
        int y1 = cy + (int) Math.round(Math.sin(a) * r1);

        // Línea principal del rayo.
        pm.drawLine(x0, y0, x1, y1);

        // Se calcula un pequeño desplazamiento perpendicular
        // para dibujar una segunda línea y dar grosor al rayo.
        int ox = (int) Math.round(Math.cos(a + Math.PI / 2.0) * 1.0);
        int oy = (int) Math.round(Math.sin(a + Math.PI / 2.0) * 1.0);

        pm.drawLine(x0 + ox, y0 + oy, x1 + ox, y1 + oy);
    }

    /*
     * Dibuja un punto individual si la posición está dentro del área del Pixmap.
     * Esto evita errores al intentar escribir fuera de los límites de la imagen.
     */
    private void putDot(Pixmap pm, int x, int y) {
        if (x < 0 || x >= pm.getWidth() || y < 0 || y >= pm.getHeight()) return;
        pm.drawPixel(x, y);
    }

    /*
     * Libera la textura generada.
     * Importante para evitar fugas de memoria en GPU.
     */
    @Override
    public void dispose() {
        tex.dispose();
    }
}
