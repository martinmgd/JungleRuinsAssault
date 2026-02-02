package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class VenenoAssets implements Disposable {

    private final Texture tex;
    public final TextureRegion veneno;

    public VenenoAssets() {
        Pixmap pm = new Pixmap(24, 24, Pixmap.Format.RGBA8888);

        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        pm.setColor(0.25f, 1.0f, 0.25f, 1.0f);
        pm.fillCircle(12, 12, 8);

        pm.setColor(0.10f, 0.60f, 0.10f, 1.0f);
        pm.drawCircle(12, 12, 8);

        pm.setColor(0.70f, 1.0f, 0.70f, 0.9f);
        pm.fillCircle(9, 15, 2);

        tex = new Texture(pm);
        pm.dispose();

        veneno = new TextureRegion(tex);
    }

    @Override
    public void dispose() {
        tex.dispose();
    }
}
