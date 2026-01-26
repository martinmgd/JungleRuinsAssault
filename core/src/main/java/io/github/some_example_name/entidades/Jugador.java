package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Jugador {

    private boolean mirandoDerecha = true;

    public enum Estado { IDLE, WALK, CROUCH, JUMP }

    private final PlayerAnimations anims;

    private Estado estado = Estado.IDLE;
    private float stateTime = 0f;

    // Posición en unidades de mundo
    private float x = 10f;
    private float y = 2f;

    private float velocidadX = 4f;

    // Física vertical
    private float velY = 0f;
    private boolean enSuelo = true;
    private float sueloY = 2f;

    private static final float GRAVEDAD = -35f;
    private static final float VEL_SALTO = 10f;

    // Derrape corto estando agachado
    private float tiempoDerrape = 0f;
    private int dirDerrape = 0;

    // Derrape muy corto para no dar sensación de "patinaje"
    private static final float DURACION_DERRAPE = 0.10f;
    private static final float MULT_VELOCIDAD_DERRAPE = 0.25f;

    // Bloqueo de movimiento mientras siga agachado tras derrapar
    private boolean bloqueoMovimientoAgachado = false;

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

        // Tiempo de animación
        stateTime += delta;

        // Si se deja de agachar, se permite moverse normalmente
        if (!agacharse) {
            bloqueoMovimientoAgachado = false;
        }

        // Orientación
        if (dir > 0f) mirandoDerecha = true;
        else if (dir < 0f) mirandoDerecha = false;

        // Inicio de salto solo si está en el suelo
        if (saltar && enSuelo) {
            velY = VEL_SALTO;
            enSuelo = false;
            setEstado(Estado.JUMP);
        }

        // Movimiento horizontal
        if (agacharse && enSuelo) {

            // Si aún no está bloqueado, permitimos iniciar un derrape corto
            if (!bloqueoMovimientoAgachado) {

                if (tiempoDerrape <= 0f && dir != 0f) {
                    tiempoDerrape = DURACION_DERRAPE;
                    dirDerrape = (dir > 0f) ? 1 : -1;
                }

                // Mientras dure el derrape, se mueve un poco
                if (tiempoDerrape > 0f) {
                    x += dirDerrape * velocidadX * MULT_VELOCIDAD_DERRAPE * delta;
                    tiempoDerrape -= delta;

                    // Al terminar el derrape, se bloquea el movimiento hasta que deje de agacharse
                    if (tiempoDerrape <= 0f) {
                        bloqueoMovimientoAgachado = true;
                        tiempoDerrape = 0f;
                        dirDerrape = 0;
                    }
                } else {
                    // Agachado sin derrape: quieto
                }

            } else {
                // Agachado y bloqueado: no se mueve
            }

        } else {
            // Movimiento normal (de pie o en el aire)
            x += dir * velocidadX * delta;

            // Reinicio de derrape al estar fuera del estado agachado
            tiempoDerrape = 0f;
            dirDerrape = 0;
        }

        // Física vertical
        if (!enSuelo) {
            velY += GRAVEDAD * delta;
            y += velY * delta;

            if (y <= sueloY) {
                y = sueloY;
                velY = 0f;
                enSuelo = true;
            }
        }

        // Selección de estado
        if (!enSuelo) {
            setEstado(Estado.JUMP);

        } else if (agacharse) {
            setEstado(Estado.CROUCH);

        } else if (dir != 0f) {
            setEstado(Estado.WALK);

        } else {
            setEstado(Estado.IDLE);
        }

        // El salto no hace loop: cuando termina la animación y ya está en el suelo,
        // vuelve al estado correcto.
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

        // Al cambiar de estado, la animación comienza desde el primer frame
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

    // ✅ AÑADIDO: dirección para disparar a izquierda/derecha
    public boolean isMirandoDerecha() {
        return mirandoDerecha;
    }

    // ✅ AÑADIDO: punto de salida del disparo (aprox a la altura del arma)
    // Ajusta estos multiplicadores si quieres afinar:
    public float getMuzzleX(float pixelsPerUnit) {
        float w = getWidth(pixelsPerUnit);
        return mirandoDerecha ? (x + w * 0.85f) : (x + w * 0.15f);
    }

    public float getMuzzleY(float pixelsPerUnit) {
        float h = getHeight(pixelsPerUnit);
        return y + h * 0.62f;
    }
}
