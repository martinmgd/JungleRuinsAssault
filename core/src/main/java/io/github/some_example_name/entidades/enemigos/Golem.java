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

    private enum Estado { IDLE, WALK, THROW, ATTACK, DEAD }
    private Estado estado = Estado.IDLE;

    private float x, y;
    private float vx;

    private final float wWorld;
    private final float hWorld;

    private int vida = 6;
    private boolean eliminar = false;

    private boolean mirandoDerecha = false;

    private final Rectangle hitbox = new Rectangle();

    private float stateTime = 0f;

    private float attackRange = 0.9f;
    private float throwRange  = 5.5f;

    private float cdThrow = 2.4f;
    private float tThrow  = 0f;

    private float cdAttack = 1.1f;
    private float tAttack  = 0f;

    private boolean rocaLanzada = false;

    private static final int FW = 128;
    private static final int FH = 80;

    private final Animation<TextureRegion> idle;
    private final Animation<TextureRegion> walk;
    private final Animation<TextureRegion> throwAnim;
    private final Animation<TextureRegion> attack;
    private final TextureRegion death;

    private float deadTime = 0f;
    private final float blinkStart  = 0.8f;
    private final float disappearAt = 1.8f;
    private final float blinkPeriod = 0.12f;
    private boolean blinkVisible = true;

    private final TextureRegion rocaRegion;

    // Config roca
    private float rocaVx = 4.0f;
    private float rocaVy = 2.0f;
    private float rocaGravity = -18.0f;
    private float rocaW = 0.40f;
    private float rocaH = 0.40f;
    private int rocaDamage = 10;

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
        this.x = x;
        this.y = sueloY;
        this.rocaRegion = rocaRegion;

        this.wWorld = 150f / ppu;
        this.hWorld = 190f / ppu;

        idle     = buildAnimMust(idleTex,  "idle");
        walk     = buildAnimMust(walkTex,  "walk");
        throwAnim= buildAnimMust(throwTex, "throw");
        attack   = buildAnimMust(attackTex,"attack");

        // death suele ser sheet también, pero si es 1 frame (128x160) nos vale igual
        validateSheetOrThrow(deathTex, "death");
        death = new TextureRegion(deathTex, 0, 0, FW, FH);

        hitbox.set(x, y, wWorld * 0.6f, hWorld * 0.85f);
    }

    private void validateSheetOrThrow(Texture tex, String label) {
        int w = tex.getWidth();
        int h = tex.getHeight();

        if (w < FW || h < FH) {
            throw new IllegalArgumentException(
                "Sheet '" + label + "' demasiado pequeño: " + w + "x" + h + " para frame " + FW + "x" + FH
            );
        }
        if (w % FW != 0 || h % FH != 0) {
            throw new IllegalArgumentException(
                "Sheet '" + label + "' NO está alineado a grid " + FW + "x" + FH + ": " + w + "x" + h
                    + " (ancho%FW=" + (w % FW) + ", alto%FH=" + (h % FH) + ")"
            );
        }
    }

    private Animation<TextureRegion> buildAnimMust(Texture tex, String label) {
        validateSheetOrThrow(tex, label);

        int cols = tex.getWidth() / FW;
        int rows = tex.getHeight() / FH;

        Array<TextureRegion> frames = new Array<>(cols * rows);
        TextureRegion base = new TextureRegion(tex);

        // Normalmente será 1 fila, pero esto soporta varias
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                frames.add(new TextureRegion(base, c * FW, r * FH, FW, FH));
            }
        }

        if (frames.size == 0) {
            throw new IllegalArgumentException("Sheet '" + label + "' no generó frames (0). Tamaño=" +
                tex.getWidth() + "x" + tex.getHeight());
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

    public void update(float delta, Jugador jugador) {
        if (eliminar) return;

        stateTime += delta;
        tThrow -= delta;
        tAttack -= delta;

        float dx = jugador.getX() - x;
        float dist = Math.abs(dx);

        mirandoDerecha = dx > 0f;

        switch (estado) {
            case IDLE:
                // 1 frame idle, o anim loop da igual: pasamos a WALK
                estado = Estado.WALK;
                stateTime = 0f;
                break;

            case WALK:
                vx = mirandoDerecha ? 0.6f : -0.6f;
                x += vx * delta;

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
                if (throwAnim.isAnimationFinished(stateTime)) {
                    tThrow = cdThrow;
                    estado = Estado.WALK;
                    stateTime = 0f;
                }
                break;

            case ATTACK:
                if (attack.isAnimationFinished(stateTime)) {
                    tAttack = cdAttack;
                    estado = Estado.WALK;
                    stateTime = 0f;
                }
                break;

            case DEAD:
                deadTime += delta;

                if (deadTime >= blinkStart) {
                    int phase = (int) ((deadTime - blinkStart) / blinkPeriod);
                    blinkVisible = (phase % 2) == 0;
                }

                if (deadTime >= disappearAt) {
                    eliminar = true;
                }
                break;
        }

        hitbox.x = x + (wWorld - hitbox.width) * 0.5f;
        hitbox.y = y;
    }

    public ProyectilRoca tryThrow() {
        if (estado != Estado.THROW) return null;
        if (rocaLanzada || rocaRegion == null) return null;

        float t = stateTime / throwAnim.getAnimationDuration();
        if (t < 0.45f) return null;

        rocaLanzada = true;

        float dir = mirandoDerecha ? 1f : -1f;

        return new ProyectilRoca(
            rocaRegion,
            x + wWorld * 0.55f,
            y + hWorld * 0.65f,
            dir * rocaVx,
            rocaVy,
            rocaGravity,
            rocaW,
            rocaH,
            rocaDamage
        );
    }

    public void recibirDanio(int dmg) {
        if (estado == Estado.DEAD) return;

        vida -= dmg;
        if (vida <= 0) {
            vida = 0;
            estado = Estado.DEAD;
            stateTime = 0f;
            deadTime = 0f;
        }
    }

    public void render(SpriteBatch batch) {
        if (eliminar) return;
        if (estado == Estado.DEAD && !blinkVisible) return;

        TextureRegion frame;
        switch (estado) {
            case WALK:   frame = walk.getKeyFrame(stateTime); break;
            case THROW:  frame = throwAnim.getKeyFrame(stateTime); break;
            case ATTACK: frame = attack.getKeyFrame(stateTime); break;
            case DEAD:   frame = death; break;
            default:     frame = idle.getKeyFrame(stateTime);
        }

        if (mirandoDerecha) {
            batch.draw(frame, x, y, wWorld, hWorld);
        } else {
            batch.draw(frame, x + wWorld, y, -wWorld, hWorld);
        }
    }

    public Rectangle getHitbox() { return hitbox; }
    public boolean isEliminar() { return eliminar; }
    public boolean isDead() { return estado == Estado.DEAD; }
}
