package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilVeneno;

public class Serpiente {

    private enum Estado { WALK, DEAD }

    private float x;
    private float y;

    private final float minX;
    private final float maxX;

    private float vx;

    private int vida;
    private boolean eliminar;

    private Estado estado = Estado.WALK;

    private final Animation<TextureRegion> animWalk;
    private final TextureRegion frameDeath;

    private float tAnim = 0f;

    private final float wWorld;
    private final float hWorld;

    private final Rectangle hitbox;

    private float deadTime = 0f;

    private final float blinkStart = 1.5f;
    private final float disappearAt = 4.0f;
    private final float blinkPeriod = 0.16f;

    private boolean blinkVisible = true;

    private final float yOffsetWorld;

    private float deathX;
    private float deathY;

    private float preDeathTimer = 0f;
    private final float deathDelay = 0.08f;

    private int dmgMordisco;
    private float cdMordisco;
    private float tMordisco = 0f;

    private int dmgVeneno;
    private float cdVeneno;
    private float tVeneno = 0f;

    private float venenoRangoMin;
    private float venenoRangoMax;
    private float velVeneno;
    private float venenoW;
    private float venenoH;

    public Serpiente(
        float x, float sueloY,
        float minX, float maxX,
        float velocidadWorld,
        int vidaInicial,
        Texture sheetWalk, int frameWpx, int frameHpx, float frameDuration,
        Texture texDeath,
        float ppu,
        float yOffsetWorld
    ) {
        this.x = x;
        this.yOffsetWorld = yOffsetWorld;
        this.y = sueloY + yOffsetWorld;

        this.minX = minX;
        this.maxX = maxX;

        this.vx = -Math.abs(velocidadWorld);
        this.vida = vidaInicial;

        TextureRegion[][] split = TextureRegion.split(sheetWalk, frameWpx, frameHpx);

        Array<TextureRegion> frames = new Array<>(2);
        frames.add(split[0][0]);
        frames.add(split[0][1]);

        animWalk = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
        frameDeath = new TextureRegion(texDeath);

        wWorld = frameWpx / ppu;
        hWorld = frameHpx / ppu;

        hitbox = new Rectangle(x, this.y, wWorld * 0.8f, hWorld * 0.6f);

        deathX = x;
        deathY = this.y;
    }

    public void setAtaques(int dmgMordisco, float cdMordisco, int dmgVeneno, float cdVeneno) {
        this.dmgMordisco = dmgMordisco;
        this.cdMordisco = cdMordisco;
        this.dmgVeneno = dmgVeneno;
        this.cdVeneno = cdVeneno;
    }

    public void setVenenoConfig(float rangoMin, float rangoMax, float velVeneno, float venenoW, float venenoH) {
        this.venenoRangoMin = rangoMin;
        this.venenoRangoMax = rangoMax;
        this.velVeneno = velVeneno;
        this.venenoW = venenoW;
        this.venenoH = venenoH;
    }

    public void setSueloY(float sueloY) {
        if (estado == Estado.DEAD) {
            return;
        }
        y = sueloY + yOffsetWorld;
        hitbox.y = y;
    }

    public void update(float delta) {
        if (eliminar) return;

        tMordisco -= delta;
        tVeneno -= delta;

        if (estado == Estado.WALK) {

            if (preDeathTimer > 0f) {
                preDeathTimer -= delta;
                if (preDeathTimer <= 0f) {
                    estado = Estado.DEAD;
                    deadTime = 0f;
                    vx = 0f;
                }
                return;
            }

            tAnim += delta;
            x += vx * delta;

            if (x < minX) {
                x = minX;
                vx = Math.abs(vx);
            } else if (x > maxX) {
                x = maxX;
                vx = -Math.abs(vx);
            }

            hitbox.x = x;
            hitbox.y = y;
            return;
        }

        if (estado == Estado.DEAD) {
            deadTime += delta;

            if (deadTime >= blinkStart) {
                int phase = (int) ((deadTime - blinkStart) / blinkPeriod);
                blinkVisible = (phase % 2) == 0;
            } else {
                blinkVisible = true;
            }

            if (deadTime >= disappearAt) eliminar = true;
        }
    }

    public void tryMordiscoJugador(Jugador jugador, Rectangle hbJugador) {
        if (estado != Estado.WALK) return;
        if (tMordisco > 0f) return;

        if (hitbox.overlaps(hbJugador)) {
            jugador.recibirDanio(dmgMordisco);
            tMordisco = cdMordisco;
        }
    }

    public ProyectilVeneno tryEscupirVeneno(Jugador jugador, Rectangle hbJugador, TextureRegion region) {
        if (estado != Estado.WALK) return null;
        if (tVeneno > 0f) return null;

        float cx = x + wWorld * 0.5f;
        float jx = hbJugador.x + hbJugador.width * 0.5f;
        float dist = Math.abs(jx - cx);

        if (dist < venenoRangoMin || dist > venenoRangoMax) return null;

        float dir = Math.signum(jx - cx);
        tVeneno = cdVeneno;

        return new ProyectilVeneno(
            region,
            cx,
            y + hWorld * 0.5f,
            dir * velVeneno,
            venenoW,
            venenoH,
            dmgVeneno
        );
    }

    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        if (eliminar) return;

        float drawX = (estado == Estado.DEAD) ? deathX : x;
        float drawY = (estado == Estado.DEAD) ? deathY : y;

        if (drawX + wWorld < camLeftX || drawX > camLeftX + viewW) return;

        if (estado == Estado.WALK) {
            TextureRegion frame = animWalk.getKeyFrame(tAnim);
            if (vx < 0f) batch.draw(frame, drawX + wWorld, drawY, -wWorld, hWorld);
            else batch.draw(frame, drawX, drawY, wWorld, hWorld);
            return;
        }

        if (blinkVisible) {
            batch.draw(frameDeath, drawX, drawY, wWorld, hWorld);
        }
    }

    public Rectangle getHitbox() { return hitbox; }
    public boolean isEliminar() { return eliminar; }
    public boolean isDead() { return estado == Estado.DEAD; }

    public void recibirDanio(int dmg) {
        if (estado != Estado.WALK) return;

        vida -= dmg;
        if (vida <= 0) {
            vida = 0;

            deathX = x;
            deathY = y;

            preDeathTimer = deathDelay;
        }
    }
}
