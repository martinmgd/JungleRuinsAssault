package io.github.some_example_name.entidades.efectos;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class EfectoImpacto {

    private final TextureRegion region;

    private final float cx;
    private final float cy;

    private final float w;
    private final float h;

    private final float duracion;

    private float t = 0f;

    private final Color color;

    public EfectoImpacto(TextureRegion region, float cx, float cy, float w, float h, float duracion, Color color) {
        this.region = region;
        this.cx = cx;
        this.cy = cy;
        this.w = w;
        this.h = h;
        this.duracion = duracion;
        this.color = new Color(color);
    }

    public void update(float delta) {
        t += delta;
    }

    public void draw(SpriteBatch batch) {
        Color prev = new Color(batch.getColor());

        batch.setColor(color);

        float x = cx - w * 0.5f;
        float y = cy - h * 0.5f;
        batch.draw(region, x, y, w, h);

        batch.setColor(prev);
    }

    public boolean isFinished() {
        return t >= duracion;
    }
}
