package io.github.some_example_name.entidades.jugador;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Jugador {

    private boolean mirandoDerecha = true;

    public enum Estado { IDLE, WALK, CROUCH, JUMP }

    private final PlayerAnimations anims;

    private Estado estado = Estado.IDLE;
    private float stateTime = 0f;

    private float x = 10f;
    private float y = 2f;

    private float velocidadX = 5.5f;

    private float velY = 0f;
    private boolean enSuelo = true;
    private float sueloY = 2f;

    private static final float GRAVEDAD = -30f;
    private static final float VEL_SALTO = 12f;

    private float tiempoDerrape = 0f;
    private int dirDerrape = 0;

    private static final float DURACION_DERRAPE = 0.10f;
    private static final float MULT_VELOCIDAD_DERRAPE = 0.25f;

    private boolean bloqueoMovimientoAgachado = false;

    private final Rectangle hitbox = new Rectangle();

    private int vida = 100;

    private float invulnTimer = 0f;
    private float invulnDuration = 0.70f;

    public Jugador(PlayerAnimations anims) {
        this.anims = anims;
    }

    public void setSueloY(float sueloY) {
        this.sueloY = sueloY;

        if (y < sueloY) {
            y = sueloY;
            velY = 0f;
            enSuelo = true;
        }
    }

    public void aplicarEntrada(float dir, boolean saltar, boolean agacharse, float delta) {

        stateTime += delta;

        if (invulnTimer > 0f) {
            invulnTimer = Math.max(0f, invulnTimer - delta);
        }

        if (!agacharse) {
            bloqueoMovimientoAgachado = false;
        }

        if (dir > 0f) mirandoDerecha = true;
        else if (dir < 0f) mirandoDerecha = false;

        if (saltar && enSuelo) {
            velY = VEL_SALTO;
            enSuelo = false;
            setEstado(Estado.JUMP);
        }

        if (agacharse && enSuelo) {

            if (!bloqueoMovimientoAgachado) {

                if (tiempoDerrape <= 0f && dir != 0f) {
                    tiempoDerrape = DURACION_DERRAPE;
                    dirDerrape = (dir > 0f) ? 1 : -1;
                }

                if (tiempoDerrape > 0f) {
                    x += dirDerrape * velocidadX * MULT_VELOCIDAD_DERRAPE * delta;
                    tiempoDerrape -= delta;

                    if (tiempoDerrape <= 0f) {
                        bloqueoMovimientoAgachado = true;
                        tiempoDerrape = 0f;
                        dirDerrape = 0;
                    }
                }

            }

        } else {
            x += dir * velocidadX * delta;

            tiempoDerrape = 0f;
            dirDerrape = 0;
        }

        if (!enSuelo) {
            velY += GRAVEDAD * delta;
            y += velY * delta;

            if (y <= sueloY) {
                y = sueloY;
                velY = 0f;
                enSuelo = true;
            }
        }

        if (!enSuelo) {
            setEstado(Estado.JUMP);

        } else if (agacharse) {
            setEstado(Estado.CROUCH);

        } else if (dir != 0f) {
            setEstado(Estado.WALK);

        } else {
            setEstado(Estado.IDLE);
        }

        if (estado == Estado.JUMP && enSuelo) {
            if (anims.jump.isAnimationFinished(stateTime)) {
                if (agacharse) setEstado(Estado.CROUCH);
                else if (dir != 0f) setEstado(Estado.WALK);
                else setEstado(Estado.IDLE);
            }
        }
    }

    private void setEstado(Estado nuevoEstado) {
        if (estado == nuevoEstado) return;

        estado = nuevoEstado;
        stateTime = 0f;
    }

    public void draw(SpriteBatch batch, float pixelsPerUnit) {
        Animation<TextureRegion> anim = getAnimacionActual();
        TextureRegion frame = anim.getKeyFrame(stateTime);

        float w = frame.getRegionWidth() / pixelsPerUnit;
        float h = frame.getRegionHeight() / pixelsPerUnit;

        if (mirandoDerecha) {
            batch.draw(frame, x, y, w, h);
        } else {
            batch.draw(frame, x + w, y, -w, h);
        }
    }

    private Animation<TextureRegion> getAnimacionActual() {
        switch (estado) {
            case WALK:
                return anims.walk;
            case CROUCH:
                return anims.crouch;
            case JUMP:
                return anims.jump;
            case IDLE:
            default:
                return anims.idle;
        }
    }

    public Rectangle getHitbox(float pixelsPerUnit) {
        float w = getWidth(pixelsPerUnit);
        float h = getHeight(pixelsPerUnit);

        float hbW = w * 0.55f;
        float hbH = h * 0.80f;

        float hbX = x + (w - hbW) * 0.5f;
        float hbY = y;

        hitbox.set(hbX, hbY, hbW, hbH);
        return hitbox;
    }

    public void recibirDanio(int dmg) {
        if (invulnTimer > 0f) return;

        vida -= dmg;
        if (vida < 0) vida = 0;

        invulnTimer = invulnDuration;
    }

    public int getVida() {
        return vida;
    }

    public boolean isInvulnerable() {
        return invulnTimer > 0f;
    }

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

    public boolean isMirandoDerecha() {
        return mirandoDerecha;
    }

    public float getMuzzleX(float pixelsPerUnit) {
        float w = getWidth(pixelsPerUnit);
        return mirandoDerecha ? (x + w * 0.85f) : (x + w * 0.15f);
    }

    public float getMuzzleY(float pixelsPerUnit) {
        float h = getHeight(pixelsPerUnit);
        return y + h * 0.62f;
    }
}
