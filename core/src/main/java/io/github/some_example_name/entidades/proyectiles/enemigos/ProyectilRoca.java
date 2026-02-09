package io.github.some_example_name.entidades.proyectiles.enemigos;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class ProyectilRoca {

    private final TextureRegion region;

    private float x;
    private float y;

    private float vx;
    private float vy;

    private float gravity;

    private float w;
    private float h;

    private final int damage;

    private final Rectangle hitbox = new Rectangle();

    private boolean eliminar = false;

    public ProyectilRoca(TextureRegion region,
                         float x, float y,
                         float vx, float vy,
                         float gravity,
                         float w, float h,
                         int damage) {
        this.region = region;

        this.x = x;
        this.y = y;

        this.vx = vx;
        this.vy = vy;

        this.gravity = gravity;

        this.w = w;
        this.h = h;

        this.damage = damage;

        hitbox.set(x, y, w, h);
    }

    public void update(float delta) {
        if (eliminar) return;

        vy += gravity * delta;

        x += vx * delta;
        y += vy * delta;

        hitbox.x = x;
        hitbox.y = y;
    }

    public void draw(SpriteBatch batch) {
        if (eliminar) return;
        batch.draw(region, x, y, w, h);
    }

    public boolean isOutOfRange(float leftX, float rightX) {
        return (x + w) < leftX || x > rightX;
    }

    public boolean isBelow(float sueloY) {
        return (y + h) < sueloY;
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

    public float getX() { return x; }
    public float getY() { return y; }

    public float getVx() { return vx; }
    public float getVy() { return vy; }

    public void setVel(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void setSize(float w, float h) {
        this.w = w;
        this.h = h;
        hitbox.width = w;
        hitbox.height = h;
    }
}
