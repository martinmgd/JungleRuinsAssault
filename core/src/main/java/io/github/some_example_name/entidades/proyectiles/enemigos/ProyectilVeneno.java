package io.github.some_example_name.entidades.proyectiles.enemigos;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class ProyectilVeneno {

    private final TextureRegion region;

    private float x;
    private float y;

    private final float vx;

    private final float w;
    private final float h;

    private final int damage;

    private final Rectangle hitbox = new Rectangle();

    private boolean eliminar = false;

    public ProyectilVeneno(TextureRegion region,
                           float x, float y,
                           float vx,
                           float w, float h,
                           int damage) {
        this.region = region;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.w = w;
        this.h = h;
        this.damage = damage;

        hitbox.set(x, y, w, h);
    }

    public void update(float delta) {
        x += vx * delta;
        hitbox.x = x;
        hitbox.y = y;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(region, x, y, w, h);
    }

    public boolean isOutOfRange(float leftX, float rightX) {
        return (x + w) < leftX || x > rightX;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    public int getDamage() {
        return damage;
    }

    public void marcarEliminar() {
        eliminar = true;
    }

    public boolean isEliminar() {
        return eliminar;
    }
}
