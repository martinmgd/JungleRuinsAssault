package io.github.some_example_name.entidades.proyectiles.jugador;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Proyectil {

    private final Animation<TextureRegion> anim;

    private float x, y;
    private float vx;
    private float stateTime = 0f;

    private final boolean haciaDerecha;

    private final float w;
    private final float h;

    private final int damage;

    private boolean eliminar = false;

    private final Rectangle hitbox = new Rectangle();

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

        hitbox.set(x, y, w, h);
    }

    public void update(float delta) {
        stateTime += delta;
        x += vx * delta;
        hitbox.x = x;
        hitbox.y = y;
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

    public Rectangle getHitbox() {
        return hitbox;
    }

    public void marcarEliminar() {
        eliminar = true;
    }

    public boolean isEliminar() {
        return eliminar;
    }

    public float getVx() {
        return vx;
    }
}
