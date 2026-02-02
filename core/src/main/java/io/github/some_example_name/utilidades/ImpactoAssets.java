package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class ImpactoAssets implements Disposable {

    private final Texture tex;
    public final TextureRegion impacto;

    public ImpactoAssets() {
        Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);

        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        int cx = 16;
        int cy = 16;

        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(cx, cy, 7);

        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(cx, cy, 5);

        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(cx, cy, 3);

        pm.setColor(1f, 1f, 1f, 1f);
        pm.drawCircle(cx, cy, 8);

        pm.setColor(1f, 1f, 1f, 1f);
        drawSpike(pm, cx, cy, 0, 9, 12);
        drawSpike(pm, cx, cy, 45, 8, 11);
        drawSpike(pm, cx, cy, 90, 9, 12);
        drawSpike(pm, cx, cy, 135, 7, 10);
        drawSpike(pm, cx, cy, 180, 9, 12);
        drawSpike(pm, cx, cy, 225, 8, 11);
        drawSpike(pm, cx, cy, 270, 9, 12);
        drawSpike(pm, cx, cy, 315, 7, 10);

        pm.setColor(1f, 1f, 1f, 1f);
        putDot(pm, cx + 11, cy + 2);
        putDot(pm, cx - 10, cy + 3);
        putDot(pm, cx + 4, cy + 11);
        putDot(pm, cx - 3, cy - 11);
        putDot(pm, cx + 9, cy - 6);
        putDot(pm, cx - 9, cy - 5);

        tex = new Texture(pm);
        pm.dispose();

        impacto = new TextureRegion(tex);
    }

    private void drawSpike(Pixmap pm, int cx, int cy, int deg, int r0, int r1) {
        double a = Math.toRadians(deg);

        int x0 = cx + (int) Math.round(Math.cos(a) * r0);
        int y0 = cy + (int) Math.round(Math.sin(a) * r0);

        int x1 = cx + (int) Math.round(Math.cos(a) * r1);
        int y1 = cy + (int) Math.round(Math.sin(a) * r1);

        pm.drawLine(x0, y0, x1, y1);

        int ox = (int) Math.round(Math.cos(a + Math.PI / 2.0) * 1.0);
        int oy = (int) Math.round(Math.sin(a + Math.PI / 2.0) * 1.0);
        pm.drawLine(x0 + ox, y0 + oy, x1 + ox, y1 + oy);
    }

    private void putDot(Pixmap pm, int x, int y) {
        if (x < 0 || x >= pm.getWidth() || y < 0 || y >= pm.getHeight()) return;
        pm.drawPixel(x, y);
    }

    @Override
    public void dispose() {
        tex.dispose();
    }
}
