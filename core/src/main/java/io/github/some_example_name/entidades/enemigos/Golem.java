package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilRoca;

public class Golem {

    // Estados internos de comportamiento/animación del enemigo.
    private enum Estado { IDLE, WALK, THROW, ATTACK, DYING }
    private Estado estado = Estado.IDLE;

    // Escalado adicional aplicado al tamaño del sprite en el mundo.
    private static final float SCALE_W = 1.6f;
    private static final float SCALE_H = 1.2f;

    // Posición y velocidad horizontal en coordenadas de mundo.
    private float x, y;
    private float vx;

    // Dimensiones en unidades de mundo utilizadas para render y colisiones.
    private final float wWorld;
    private final float hWorld;

    // Vida actual del golem y bandera de eliminación (retirada del gestor).
    private int vida = 6;
    private boolean eliminar = false;

    // Dirección de mirada utilizada para decidir movimiento y render con flip.
    private boolean mirandoDerecha = false;

    // Hitbox rectangular utilizada para detección de impactos.
    private final Rectangle hitbox = new Rectangle();

    // Temporizador de animación/estado, acumulado en segundos.
    private float stateTime = 0f;

    // Rangos de decisión para ataque cuerpo a cuerpo y lanzamiento de roca.
    private float attackRange = 0.9f;
    private float throwRange  = 5.5f;

    // Cooldowns y contadores para limitar frecuencia de lanzamiento y ataque.
    private float cdThrow = 2.4f;
    private float tThrow  = 0f;

    private float cdAttack = 1.1f;
    private float tAttack  = 0f;

    // Controla que solo se genere una roca por animación de lanzamiento.
    private boolean rocaLanzada = false;

    // Tamaño de cada frame dentro de los sheets de animación.
    private static final int FW = 128;
    private static final int FH = 80;

    // Animaciones del golem para cada estado.
    private final Animation<TextureRegion> idle;
    private final Animation<TextureRegion> walk;
    private final Animation<TextureRegion> throwAnim;
    private final Animation<TextureRegion> attack;
    private final Animation<TextureRegion> deathAnim;

    // Región de textura utilizada por el proyectil de roca.
    private final TextureRegion rocaRegion;

    // Parámetros físicos del proyectil roca.
    private float rocaVx = 9.5f;
    private float rocaVy = 2.9f;
    private float rocaGravity = -16.0f;

    // Dimensiones del proyectil y daño que aplica al jugador.
    private float rocaW = 1.5f;
    private float rocaH = 1.5f;
    private int rocaDamage = 10;

    // Límite derecho de movimiento (p. ej., pared/ruina) y altura del suelo.
    private float limiteDerecha = Float.POSITIVE_INFINITY;
    private float sueloY = 2f;

    // Define el límite derecho en coordenadas de mundo a partir del cual el golem no puede avanzar.
    public void setLimiteDerecha(float limiteDerecha) {
        this.limiteDerecha = limiteDerecha;
    }

    // Define la altura del suelo en coordenadas de mundo y sincroniza la posición vertical.
    public void setSueloY(float sueloY) {
        this.sueloY = sueloY;
        this.y = sueloY;
        hitbox.y = y;
    }

    // Aplica un clamp horizontal contra el límite derecho, teniendo en cuenta el ancho del sprite.
    private void clampContraPared() {
        if (limiteDerecha == Float.POSITIVE_INFINITY) return;

        float maxX = limiteDerecha - wWorld;
        if (x > maxX) x = maxX;
    }

    // Devuelve una X objetivo para la IA. Si existe un límite derecho, el objetivo se restringe
    // para que el golem no intente perseguir posiciones situadas más allá de la pared.
    private float getTargetXForAI(Jugador jugador) {
        float tx = jugador.getX();

        if (limiteDerecha != Float.POSITIVE_INFINITY) {
            float maxTarget = limiteDerecha - 0.05f;
            if (tx > maxTarget) tx = maxTarget;
        }
        return tx;
    }

    public Golem(
        float x,
        float sueloY,
        Texture idleTex,
        Texture walkTex,
        Texture throwTex,
        Texture attackTex,
        Texture deathTex,
        TextureRegion rocaRegion,
        float ppu
    ) {
        // Posición inicial y configuración de suelo.
        this.x = x;
        this.sueloY = sueloY;
        this.y = sueloY;

        // Región del proyectil roca compartida con el gestor de enemigos.
        this.rocaRegion = rocaRegion;

        // Dimensiones en mundo derivadas de un tamaño base en píxeles y escalado adicional.
        this.wWorld = (150f / ppu) * SCALE_W;
        this.hWorld = (190f / ppu) * SCALE_H;

        // Construcción y validación de animaciones a partir de sheets.
        idle      = buildAnimMust(idleTex,  "idle");
        walk      = buildAnimMust(walkTex,  "walk");
        throwAnim = buildAnimMust(throwTex, "throw");
        attack    = buildAnimMust(attackTex,"attack");
        deathAnim = buildAnimMust(deathTex, "death");

        // Hitbox relativo al sprite, más estrecho y más bajo que el render para ajuste de colisiones.
        hitbox.set(x, y, wWorld * 0.6f, hWorld * 0.85f);
    }

    // Verifica que el sheet cumple el grid esperado (FW x FH) y contiene al menos un frame válido.
    private void validateSheetOrThrow(Texture tex, String label) {
        int w = tex.getWidth();
        int h = tex.getHeight();

        if (w < FW || h < FH) {
            throw new IllegalArgumentException(
                "Sheet '" + label + "' demasiado pequeño: " + w + "x" + h
            );
        }
        if (w % FW != 0 || h % FH != 0) {
            throw new IllegalArgumentException(
                "Sheet '" + label + "' no alineado a grid " + FW + "x" + FH
            );
        }
    }

    // Crea una animación a partir de un sheet, generando todos los frames por filas/columnas.
    // La velocidad y modo (loop/normal) se determinan en función de la etiqueta.
    private Animation<TextureRegion> buildAnimMust(Texture tex, String label) {
        validateSheetOrThrow(tex, label);

        int cols = tex.getWidth() / FW;
        int rows = tex.getHeight() / FH;

        Array<TextureRegion> frames = new Array<>(cols * rows);
        TextureRegion base = new TextureRegion(tex);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                frames.add(new TextureRegion(base, c * FW, r * FH, FW, FH));
            }
        }

        float frameTime;
        boolean loop;

        switch (label) {
            case "idle":   frameTime = 0.25f; loop = true;  break;
            case "walk":   frameTime = 0.12f; loop = true;  break;
            case "throw":  frameTime = 0.10f; loop = false; break;
            case "attack": frameTime = 0.08f; loop = false; break;
            case "death":  frameTime = 0.10f; loop = false; break;
            default:       frameTime = 0.12f; loop = true;
        }

        Animation<TextureRegion> anim = new Animation<>(frameTime, frames);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return anim;
    }

    // Actualiza la IA, estados, timers, posición y hitbox del golem.
    public void update(float delta, Jugador jugador) {
        if (eliminar) return;

        // Mantiene al golem adherido al suelo configurado.
        y = sueloY;

        // Estado de muerte: solo avanza la animación y marca eliminación al finalizar.
        if (estado == Estado.DYING) {
            stateTime += delta;

            if (deathAnim.isAnimationFinished(stateTime)) {
                eliminar = true;
            }

            clampContraPared();

            hitbox.x = x + (wWorld - hitbox.width) * 0.5f;
            hitbox.y = y;
            return;
        }

        // Timers de animación y cooldowns.
        stateTime += delta;
        tThrow -= delta;
        tAttack -= delta;

        // Objetivo horizontal restringido por el límite derecho, si existe.
        float targetX = getTargetXForAI(jugador);

        float dx = targetX - x;
        float dist = Math.abs(dx);

        // La dirección de mirada se define por el signo del desplazamiento hacia el objetivo.
        mirandoDerecha = dx > 0f;

        switch (estado) {
            case IDLE:
                // Transición inicial hacia movimiento.
                estado = Estado.WALK;
                stateTime = 0f;
                break;

            case WALK:
                // Movimiento horizontal a velocidad constante en función de la dirección.
                vx = mirandoDerecha ? 0.6f : -0.6f;
                x += vx * delta;

                clampContraPared();

                // Priorización: ataque melee si está en rango y el cooldown lo permite.
                // En caso contrario, lanzamiento si está en rango y cooldown disponible.
                if (dist <= attackRange && tAttack <= 0f) {
                    estado = Estado.ATTACK;
                    stateTime = 0f;
                } else if (dist <= throwRange && tThrow <= 0f) {
                    estado = Estado.THROW;
                    rocaLanzada = false;
                    stateTime = 0f;
                }
                break;

            case THROW:
                // Durante el lanzamiento se mantiene el clamp para no atravesar el límite.
                clampContraPared();

                // Al finalizar animación, aplica cooldown y vuelve a caminar.
                if (throwAnim.isAnimationFinished(stateTime)) {
                    tThrow = cdThrow;
                    estado = Estado.WALK;
                    stateTime = 0f;
                }
                break;

            case ATTACK:
                // Durante el ataque se mantiene el clamp para no atravesar el límite.
                clampContraPared();

                // Al finalizar animación, aplica cooldown y vuelve a caminar.
                if (attack.isAnimationFinished(stateTime)) {
                    tAttack = cdAttack;
                    estado = Estado.WALK;
                    stateTime = 0f;
                }
                break;
        }

        // Reposiciona hitbox centrada horizontalmente dentro del sprite.
        hitbox.x = x + (wWorld - hitbox.width) * 0.5f;
        hitbox.y = y;
    }

    // Intenta generar un proyectil roca en un punto de la animación de lanzamiento.
    // Devuelve null si no se cumplen condiciones de estado, timing o si ya se lanzó.
    public ProyectilRoca tryThrow() {
        if (estado != Estado.THROW) return null;
        if (rocaLanzada || rocaRegion == null) return null;

        float t = stateTime / throwAnim.getAnimationDuration();
        if (t < 0.45f) return null;

        rocaLanzada = true;
        float dir = mirandoDerecha ? 1f : -1f;

        float spawnX = x + wWorld * (mirandoDerecha ? 0.75f : 0.25f);
        float spawnY = y + hWorld * 0.25f;

        return new ProyectilRoca(
            rocaRegion,
            spawnX,
            spawnY,
            dir * rocaVx,
            rocaVy,
            rocaGravity,
            rocaW,
            rocaH,
            rocaDamage
        );
    }

    // Aplica daño al golem y gestiona transición al estado de muerte al agotar la vida.
    public void recibirDanio(int dmg) {
        if (estado == Estado.DYING) return;

        vida -= dmg;
        if (vida <= 0) {
            vida = 0;
            estado = Estado.DYING;
            stateTime = 0f;

            vx = 0f;
            rocaLanzada = true;
        }
    }

    // Renderiza el frame actual según estado, aplicando flip horizontal si mira a la izquierda.
    public void render(SpriteBatch batch) {
        if (eliminar) return;

        TextureRegion frame;

        if (estado == Estado.DYING) {
            frame = deathAnim.getKeyFrame(stateTime);
        } else {
            switch (estado) {
                case WALK:   frame = walk.getKeyFrame(stateTime); break;
                case THROW:  frame = throwAnim.getKeyFrame(stateTime); break;
                case ATTACK: frame = attack.getKeyFrame(stateTime); break;
                default:     frame = idle.getKeyFrame(stateTime);
            }
        }

        if (mirandoDerecha) {
            batch.draw(frame, x, y, wWorld, hWorld);
        } else {
            batch.draw(frame, x + wWorld, y, -wWorld, hWorld);
        }
    }

    // Accesores utilizados por gestores externos para colisiones y eliminación.
    public Rectangle getHitbox() { return hitbox; }
    public boolean isEliminar() { return eliminar; }
    public boolean isDead() { return estado == Estado.DYING; }
}
