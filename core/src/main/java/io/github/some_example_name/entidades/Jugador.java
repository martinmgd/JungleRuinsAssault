package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Jugador {

    public enum Estado { IDLE, WALK }

    private final PlayerAnimations anims;

    private Estado estado = Estado.IDLE;
    private float stateTime = 0f;

    // posición en unidades de mundo (las que uses en tu viewport)
    private float x = 10f;
    private float y = 2f;

    private float velocidad = 6f;

    public Jugador(PlayerAnimations anims) {
        this.anims = anims;
    }

    public void moverHorizontal(float dir, float delta) {
        x += dir * velocidad * delta;
        estado = (dir != 0f) ? Estado.WALK : Estado.IDLE;
        stateTime += delta;
    }

    public void draw(SpriteBatch batch, float pixelsPerUnit) {
        Animation<TextureRegion> anim = (estado == Estado.WALK) ? anims.walk : anims.idle;
        TextureRegion frame = anim.getKeyFrame(stateTime);

        // convertimos pixeles->unidades de mundo
        float w = frame.getRegionWidth() / pixelsPerUnit;
        float h = frame.getRegionHeight() / pixelsPerUnit;

        batch.draw(frame, x, y, w, h);
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getWidth(float pixelsPerUnit) { return PlayerAnimations.FRAME_W / pixelsPerUnit; }
}
