package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Proyectil {

    private final Animation<TextureRegion> anim;

    private float x, y;
    private float vx;
    private float stateTime = 0f;

    private final boolean haciaDerecha;

    private final float w;
    private final float h;

    private final int damage;

    public Proyectil(Animation<TextureRegion> anim,
                     float x, float y, float vx,
                     boolean haciaDerecha,
                     float w, float h,
                     int damage) {
        this.anim = anim;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.haciaDerecha = haciaDerecha;
        this.w = w;
        this.h = h;
        this.damage = damage;
    }

    public void update(float delta) {
        stateTime += delta;
        x += vx * delta;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = anim.getKeyFrame(stateTime, true);

        if (haciaDerecha) {
            batch.draw(frame, x, y, w, h);
        } else {
            batch.draw(frame, x + w, y, -w, h);
        }
    }

    public boolean isOutOfRange(float leftX, float rightX) {
        return (x + w) < leftX || x > rightX;
    }

    public int getDamage() { return damage; }
}
