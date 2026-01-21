package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Jugador {

    private boolean mirandoDerecha = true;

    public enum Estado { IDLE, WALK }

    private final PlayerAnimations anims;

    private Estado estado = Estado.IDLE;
    private float stateTime = 0f;

    // posición en unidades de mundo (las que uses en tu viewport)
    private float x = 10f;
    private float y = 2f;

    private float velocidad = 12f;

    public Jugador(PlayerAnimations anims) {
        this.anims = anims;
    }

    public void moverHorizontal(float dir, float delta) {
        x += dir * velocidad * delta;

        estado = (dir != 0f) ? Estado.WALK : Estado.IDLE;

        if (dir > 0f) mirandoDerecha = true;
        else if (dir < 0f) mirandoDerecha = false;

        stateTime += delta;
    }

    public void draw(SpriteBatch batch, float pixelsPerUnit) {
        Animation<TextureRegion> anim = (estado == Estado.WALK) ? anims.walk : anims.idle;
        TextureRegion frame = anim.getKeyFrame(stateTime);

        // IMPORTANTE:
        // No flipes el TextureRegion original (se reutiliza). Dibuja con ancho negativo.
        float w = frame.getRegionWidth() / pixelsPerUnit;
        float h = frame.getRegionHeight() / pixelsPerUnit;

        if (mirandoDerecha) {
            batch.draw(frame, x, y, w, h);
        } else {
            // Dibuja "espejado": ancho negativo y desplaza X para que el pie no cambie
            batch.draw(frame, x + w, y, -w, h);
        }
    }

    // --- getters / setters de posición ---
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getWidth(float pixelsPerUnit) {
        return PlayerAnimations.FRAME_W / pixelsPerUnit;
    }

    public float getHeight(float pixelsPerUnit) {
        return PlayerAnimations.FRAME_H / pixelsPerUnit;
    }
}
