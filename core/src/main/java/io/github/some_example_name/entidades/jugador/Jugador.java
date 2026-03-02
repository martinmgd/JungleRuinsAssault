package io.github.some_example_name.entidades.jugador;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import io.github.some_example_name.utilidades.Configuracion;

public class Jugador {

    // Indica la orientación actual del jugador para dibujar el sprite (flip horizontal) y orientar disparos.
    private boolean mirandoDerecha = true;

    // Estados principales del jugador, usados para decidir animación y lógica de control/movimiento.
    public enum Estado { IDLE, WALK, CROUCH, JUMP, DEAD }

    // Contenedor de animaciones del jugador (idle/walk/crouch/jump/dead fases).
    private final PlayerAnimations anims;

    // Estado actual y tiempo acumulado dentro del estado/animación (state time típico de libGDX).
    private Estado estado = Estado.IDLE;
    private float stateTime = 0f;

    // Posición en el mundo (unidades de mundo, no píxeles).
    private float x = 10f;
    private float y = 2f;

    // Velocidad horizontal base del movimiento.
    private float velocidadX = 5.5f;

    // Velocidad vertical y estado de contacto con el suelo.
    private float velY = 0f;
    private boolean enSuelo = true;
    private float sueloY = 2f;

    // Constantes físicas simplificadas: gravedad y velocidad inicial del salto.
    private static final float GRAVEDAD = -30f;
    private static final float VEL_SALTO = 12f;

    // Variables para el “derrape”/deslizamiento al agacharse con dirección.
    private float tiempoDerrape = 0f;
    private int dirDerrape = 0;

    // Parámetros del derrape: duración y multiplicador de velocidad durante el deslizamiento.
    private static final float DURACION_DERRAPE = 0.10f;
    private static final float MULT_VELOCIDAD_DERRAPE = 0.25f;

    // Bloquea el movimiento lateral mientras se mantiene agachado tras completar el derrape.
    private boolean bloqueoMovimientoAgachado = false;

    // Hitbox reutilizable para evitar allocations en cada consulta.
    private final Rectangle hitbox = new Rectangle();

    // Vida del jugador en puntos (0..100 típicamente).
    private int vida = 100;

    // Temporizador de invulnerabilidad tras recibir daño.
    private float invulnTimer = 0f;
    private float invulnDuration = 0.70f;

    // NUEVO: fase de muerte (1 -> 2 -> 3)
    // Controla la progresión entre varias animaciones de muerte encadenadas.
    private int deadFase = 1;

    public Jugador(PlayerAnimations anims) {
        // Inyección de animaciones para desacoplar carga de assets de la lógica del jugador.
        this.anims = anims;
    }

    public boolean isDead() {
        // El jugador se considera muerto si el estado actual es DEAD.
        return estado == Estado.DEAD;
    }

    public void setSueloY(float sueloY) {
        // Actualiza la altura del suelo (útil si cambia el nivel/escenario) y corrige la posición si está por debajo.
        this.sueloY = sueloY;

        if (y < sueloY) {
            y = sueloY;
            velY = 0f;
            enSuelo = true;
        }
    }

    public void aplicarEntrada(float dir, boolean saltar, boolean agacharse, float delta) {

        // Acumula tiempo para animaciones (y para detectar fin de animación en algunos estados).
        stateTime += delta;

        // Si está muerto: no hay control, solo animación y quedarse en el suelo
        // En estado DEAD se ignora la entrada y se ejecuta únicamente una física vertical mínima + encadenado de animaciones.
        if (estado == Estado.DEAD) {

            // Física vertical mínima por si muriese en el aire
            if (!enSuelo) {
                velY += GRAVEDAD * delta;
                y += velY * delta;

                if (y <= sueloY) {
                    y = sueloY;
                    velY = 0f;
                    enSuelo = true;
                }
            } else {
                // Asegura que, si está en suelo, la Y no quede por debajo por acumulación numérica.
                if (y < sueloY) y = sueloY;
            }

            // Encadenar dead1 -> dead2 -> dead3
            // Se avanza a la siguiente fase cuando la animación actual termina.
            if (deadFase == 1) {
                if (anims.dead1.isAnimationFinished(stateTime)) {
                    deadFase = 2;
                    stateTime = 0f;
                }
            } else if (deadFase == 2) {
                if (anims.dead2.isAnimationFinished(stateTime)) {
                    deadFase = 3;
                    stateTime = 0f;
                }
            } else {
                // deadFase == 3: se queda en último frame (no hacemos nada)
                // La última fase actúa como “pose final” de muerte.
            }

            return;
        }

        // Reduce invulnerabilidad si está activa.
        if (invulnTimer > 0f) {
            invulnTimer = Math.max(0f, invulnTimer - delta);
        }

        // Si deja de agacharse, se permite volver a mover (se desbloquea el bloqueo post-derrape).
        if (!agacharse) {
            bloqueoMovimientoAgachado = false;
        }

        // Actualiza orientación en función de la dirección de entrada.
        if (dir > 0f) mirandoDerecha = true;
        else if (dir < 0f) mirandoDerecha = false;

        // Salto: solo si se solicita y el jugador está en el suelo.
        if (saltar && enSuelo) {
            velY = VEL_SALTO;
            enSuelo = false;
            setEstado(Estado.JUMP);
        }

        // Agacharse: si está en suelo, se puede iniciar/continuar derrape o bloquear movimiento tras completarlo.
        if (agacharse && enSuelo) {

            if (!bloqueoMovimientoAgachado) {

                // Inicia derrape si no hay uno activo y existe dirección.
                if (tiempoDerrape <= 0f && dir != 0f) {
                    tiempoDerrape = DURACION_DERRAPE;
                    dirDerrape = (dir > 0f) ? 1 : -1;
                }

                // Durante el derrape, se aplica un movimiento lateral reducido.
                if (tiempoDerrape > 0f) {
                    x += dirDerrape * velocidadX * MULT_VELOCIDAD_DERRAPE * delta;
                    tiempoDerrape -= delta;

                    // Al terminar el derrape, se bloquea el movimiento lateral mientras se mantenga agachado.
                    if (tiempoDerrape <= 0f) {
                        bloqueoMovimientoAgachado = true;
                        tiempoDerrape = 0f;
                        dirDerrape = 0;
                    }
                }

            }

        } else {
            // Movimiento horizontal normal cuando no está agachado o no está en suelo.
            x += dir * velocidadX * delta;

            // Resetea parámetros del derrape si deja de aplicar la condición.
            tiempoDerrape = 0f;
            dirDerrape = 0;
        }

        // Física vertical: si está en el aire, aplica gravedad e integra posición.
        if (!enSuelo) {
            velY += GRAVEDAD * delta;
            y += velY * delta;

            // Colisión simple con el suelo.
            if (y <= sueloY) {
                y = sueloY;
                velY = 0f;
                enSuelo = true;
            }
        }

        // Máquina de estados: determina estado en función de suelo/entrada.
        if (!enSuelo) {
            setEstado(Estado.JUMP);

        } else if (agacharse) {
            setEstado(Estado.CROUCH);

        } else if (dir != 0f) {
            setEstado(Estado.WALK);

        } else {
            setEstado(Estado.IDLE);
        }

        // Ajuste adicional: cuando está en JUMP y aterriza, espera al final de la animación de salto
        // para transicionar al estado correcto, evitando “cortes” de animación.
        if (estado == Estado.JUMP && enSuelo) {
            if (anims.jump.isAnimationFinished(stateTime)) {
                if (agacharse) setEstado(Estado.CROUCH);
                else if (dir != 0f) setEstado(Estado.WALK);
                else setEstado(Estado.IDLE);
            }
        }
    }

    private void setEstado(Estado nuevoEstado) {
        // Evita reiniciar el stateTime si el estado no cambia.
        if (estado == nuevoEstado) return;

        // Actualiza estado y reinicia el contador de animación.
        estado = nuevoEstado;
        stateTime = 0f;

        // Al entrar en DEAD, reinicia el encadenado de fases de muerte.
        if (estado == Estado.DEAD) {
            deadFase = 1;
        }
    }

    public void draw(SpriteBatch batch, float pixelsPerUnit) {
        // Selecciona la animación correspondiente al estado actual.
        Animation<TextureRegion> anim = getAnimacionActual();

        // Si es DEAD, NO loop: se queda en último frame
        // En DEAD se renderiza sin bucle para que el personaje quede en el frame final de la fase actual.
        TextureRegion frame = (estado == Estado.DEAD)
            ? anim.getKeyFrame(stateTime, false)
            : anim.getKeyFrame(stateTime);

        // Conversión de tamaño de sprite desde píxeles a unidades de mundo.
        float w = frame.getRegionWidth() / pixelsPerUnit;
        float h = frame.getRegionHeight() / pixelsPerUnit;

        // Dibujo con flip horizontal usando ancho negativo cuando mira a la izquierda.
        if (mirandoDerecha) {
            batch.draw(frame, x, y, w, h);
        } else {
            batch.draw(frame, x + w, y, -w, h);
        }
    }

    private Animation<TextureRegion> getAnimacionActual() {
        // Devuelve la animación asociada al estado actual.
        // En DEAD se selecciona una animación distinta según la fase de muerte.
        switch (estado) {
            case WALK:
                return anims.walk;
            case CROUCH:
                return anims.crouch;
            case JUMP:
                return anims.jump;
            case DEAD:
                if (deadFase == 1) return anims.dead1;
                if (deadFase == 2) return anims.dead2;
                return anims.dead3;
            case IDLE:
            default:
                return anims.idle;
        }
    }

    public Rectangle getHitbox(float pixelsPerUnit) {
        // Calcula y devuelve el hitbox en unidades de mundo.
        // Se define como una fracción del sprite para ajustar la jugabilidad (no usa el borde exacto del frame).
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
        // Feedback háptico opcional al recibir daño, controlado por configuración y disponibilidad del hardware.
        if (dmg > 0) {
            if (Configuracion.isVibracionActivada() &&
                Gdx.input.isPeripheralAvailable(com.badlogic.gdx.Input.Peripheral.Vibrator)) {
                Gdx.input.vibrate(40); // corta
            }
        }

        // Si ya está muerto o es invulnerable, ignora el daño.
        if (estado == Estado.DEAD) return;
        if (invulnTimer > 0f) return;

        // Aplica daño y clamp a 0.
        vida -= dmg;
        if (vida < 0) vida = 0;

        // Activa ventana de invulnerabilidad tras el golpe.
        invulnTimer = invulnDuration;

        // Si llega a 0, entra en DEAD y lanza animación
        // Resetea estados de movimiento para evitar “arrastre” cuando el jugador muere.
        if (vida == 0) {
            setEstado(Estado.DEAD);

            // Para que no siga “arrastrando” movimientos
            tiempoDerrape = 0f;
            dirDerrape = 0;
            bloqueoMovimientoAgachado = false;

            // Si estaba en el suelo, se queda estable
            if (enSuelo) {
                velY = 0f;
                y = sueloY;
            }
        }
    }

    public int getVida() {
        // Devuelve la vida actual.
        return vida;
    }

    public boolean isInvulnerable() {
        // Indica si el jugador está en ventana de invulnerabilidad.
        return invulnTimer > 0f;
    }

    // Accesores básicos de posición.
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getWidth(float pixelsPerUnit) {
        // Ancho base del frame convertido a unidades de mundo.
        return PlayerAnimations.FRAME_W / pixelsPerUnit;
    }

    public float getHeight(float pixelsPerUnit) {
        // Alto base del frame convertido a unidades de mundo.
        return PlayerAnimations.FRAME_H / pixelsPerUnit;
    }

    public boolean isMirandoDerecha() {
        // Devuelve la orientación actual (útil para disparos y render).
        return mirandoDerecha;
    }

    public float getMuzzleX(float pixelsPerUnit) {
        // Punto de salida del disparo en X según orientación.
        // Se usa una fracción del ancho para aproximar el “cañón”/mano del personaje.
        float w = getWidth(pixelsPerUnit);
        return mirandoDerecha ? (x + w * 0.85f) : (x + w * 0.15f);
    }

    public float getMuzzleY(float pixelsPerUnit) {
        // Punto de salida del disparo en Y usando una fracción del alto del sprite.
        float h = getHeight(pixelsPerUnit);
        return y + h * 0.62f;
    }
}
