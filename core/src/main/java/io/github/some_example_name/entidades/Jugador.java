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

    private float velocidadX = 12f;

    // Física vertical
    private float velY = 0f;
    private boolean enSuelo = true;
    private float sueloY = 2f;

    private static final float GRAVEDAD = -35f;
    private static final float VEL_SALTO = 10f;

    // Derrape corto estando agachado
    private float tiempoDerrape = 0f;
    private int dirDerrape = 0;

    private static final float DURACION_DERRAPE = 0.18f;
    private static final float MULT_VELOCIDAD_DERRAPE = 0.35f;

    // Cima del salto (mantiene el frame central un instante)
    private boolean enCima = false;
    private float tiempoCima = 0f;
    private static final float DURACION_CIMA = 0.10f;

    public Jugador(PlayerAnimations anims) {
        this.anims = anims;
    }

    public void setSueloY(float sueloY) {
        this.sueloY = sueloY;

        if (y < sueloY) {
            y = sueloY;
            velY = 0f;
            enSuelo = true;
            enCima = false;
            tiempoCima = 0f;
        }
    }

    public void aplicarEntrada(float dir, boolean saltar, boolean agacharse, float delta) {

        // Tiempo de animación
        stateTime += delta;

        // Orientación
        if (dir > 0f) mirandoDerecha = true;
        else if (dir < 0f) mirandoDerecha = false;

        // Inicio de salto solo si está en el suelo
        if (saltar && enSuelo) {
            velY = VEL_SALTO;
            enSuelo = false;
            enCima = false;
            tiempoCima = 0f;
        }

        // Movimiento horizontal
        if (agacharse && enSuelo) {

            // Si se inicia dirección estando agachado, se aplica un derrape corto
            if (tiempoDerrape <= 0f && dir != 0f) {
                tiempoDerrape = DURACION_DERRAPE;
                dirDerrape = (dir > 0f) ? 1 : -1;
            }

            // Mientras dure el derrape, se mueve un poco y luego se detiene
            if (tiempoDerrape > 0f) {
                x += dirDerrape * velocidadX * MULT_VELOCIDAD_DERRAPE * delta;
                tiempoDerrape -= delta;
            }

        } else {
            // Movimiento normal
            x += dir * velocidadX * delta;

            // Al salir de agachado se reinicia el derrape
            tiempoDerrape = 0f;
            dirDerrape = 0;
        }

        // Física vertical
        if (!enSuelo) {
            float velYAnterior = velY;

            velY += GRAVEDAD * delta;
            y += velY * delta;

            // Detectar punto más alto: al pasar de positivo a cero/negativo
            if (!enCima && velYAnterior > 0f && velY <= 0f) {
                enCima = true;
                tiempoCima = DURACION_CIMA;
            }

            // Aterrizaje
            if (y <= sueloY) {
                y = sueloY;
                velY = 0f;
                enSuelo = true;
                enCima = false;
                tiempoCima = 0f;
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

        // Temporizador de cima (solo mientras está en el aire)
        if (estado == Estado.JUMP && enCima) {
            tiempoCima -= delta;
            if (tiempoCima <= 0f) {
                enCima = false;
                tiempoCima = 0f;
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
                if (enCima) return anims.jumpTop;
                if (velY > 0f) return anims.jumpUp;
                return anims.jumpDown;
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
}
