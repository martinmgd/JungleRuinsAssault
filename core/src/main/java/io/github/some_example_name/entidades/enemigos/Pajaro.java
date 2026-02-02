package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import io.github.some_example_name.entidades.jugador.Jugador;

public class Pajaro {

    public enum Estado { LANZANDO, SUBIENDO, MUERTO }

    private Estado estado = Estado.LANZANDO;

    private float x, y;
    private float yTop;       // altura a la que vuelve (arriba de pantalla)
    private float ySuelo = 2f;

    private float velX, velY;
    private boolean mirandoDerecha = false;

    private final float ppu;
    private final float yOffsetWorld;

    private final TextureRegion ataque;
    private final TextureRegion muerto;

    private final Rectangle hitbox = new Rectangle();

    // Config
    private float diveSpeed = 12.0f;

    // Daño por contacto
    private int dmgContacto = 12;
    private float cdContacto = 0.60f;
    private float cdTimer = 0f;

    // Muerte / cleanup
    private boolean eliminar = false;
    private float deadTimer = 0f;
    private float deadTime = 0.35f;

    public Pajaro(
        float spawnX, float spawnYTop,
        float ySuelo,
        float diveSpeed,
        Texture texAtaque,
        Texture texMuerto,
        float ppu,
        float yOffsetWorld,
        Jugador jugadorObjetivo
    ) {
        this.x = spawnX;
        this.yTop = spawnYTop;
        this.y = spawnYTop;

        this.ySuelo = ySuelo;
        this.diveSpeed = Math.max(1f, diveSpeed);

        this.ppu = ppu;
        this.yOffsetWorld = yOffsetWorld;

        this.ataque = new TextureRegion(texAtaque);
        this.muerto = new TextureRegion(texMuerto);

        // Se lanza inmediatamente hacia el jugador
        iniciarLanzamiento(jugadorObjetivo);
    }

    // -------------------------
    // Setters opcionales
    // -------------------------
    public void setAtaqueContacto(int dmg, float cd) {
        this.dmgContacto = Math.max(0, dmg);
        this.cdContacto = Math.max(0.05f, cd);
    }

    public void update(float delta) {
        if (eliminar) return;

        if (cdTimer > 0f) cdTimer = Math.max(0f, cdTimer - delta);

        switch (estado) {
            case MUERTO:
                deadTimer += delta;
                if (deadTimer >= deadTime) eliminar = true;
                return;

            case LANZANDO:
                x += velX * delta;
                y += velY * delta;

                // Si llega al suelo sin golpear, sube por la misma trayectoria
                if (y <= ySuelo) {
                    y = ySuelo;
                    velY = Math.abs(velY); // invierte: ahora sube igual de rápido
                    estado = Estado.SUBIENDO;
                }
                break;

            case SUBIENDO:
                x += velX * delta;
                y += velY * delta;

                // Al volver arriba, se elimina (y el gestor spawnea otro)
                if (y >= yTop) {
                    y = yTop;
                    eliminar = true;
                }
                break;
        }

        mirandoDerecha = velX > 0f;
    }

    private void iniciarLanzamiento(Jugador j) {
        estado = Estado.LANZANDO;

        float targetX = (j != null) ? j.getX() : x;
        float targetY = (j != null) ? (j.getY() + 0.5f) : (ySuelo + 1.0f);

        float tx = targetX - x;
        float ty = targetY - y;

        float len = (float)Math.sqrt(tx * tx + ty * ty);
        if (len < 0.0001f) len = 1f;

        float nx = tx / len;
        float ny = ty / len;

        velX = nx * diveSpeed;
        velY = ny * diveSpeed;
    }

    // -------------------------
    // Daño por contacto
    // -------------------------
    public void tryDanioContacto(Jugador jugador, Rectangle hbJugador) {
        if (estado == Estado.MUERTO) return;
        if (cdTimer > 0f) return;

        Rectangle hbPajaro = getHitbox(ppu);

        if (hbPajaro.overlaps(hbJugador)) {
            jugador.recibirDanio(dmgContacto);
            cdTimer = cdContacto;

            // Si golpea durante el picado, sube igual (se "va")
            if (estado == Estado.LANZANDO) {
                velY = Math.abs(velY);
                estado = Estado.SUBIENDO;
            }
        }
    }

    // -------------------------
    // Render / Hitbox
    // -------------------------
    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        float rightX = camLeftX + viewW;

        TextureRegion frame = (estado == Estado.MUERTO) ? muerto : ataque;

        float w = frame.getRegionWidth() / ppu;
        float h = frame.getRegionHeight() / ppu;

        // Culling
        if (x + w < camLeftX - 2f || x > rightX + 2f) return;

        float drawY = y + yOffsetWorld;

        if (mirandoDerecha) {
            batch.draw(frame, x, drawY, w, h);
        } else {
            batch.draw(frame, x + w, drawY, -w, h);
        }
    }

    public Rectangle getHitbox(float pixelsPerUnit) {
        TextureRegion frame = (estado == Estado.MUERTO) ? muerto : ataque;

        float w = frame.getRegionWidth() / pixelsPerUnit;
        float h = frame.getRegionHeight() / pixelsPerUnit;

        float hbW = w * 0.70f;
        float hbH = h * 0.55f;

        float hbX = x + (w - hbW) * 0.5f;
        float hbY = y + h * 0.20f;

        hitbox.set(hbX, hbY, hbW, hbH);
        return hitbox;
    }

    // -------------------------
    // Muerte / Flags
    // -------------------------
    public void matar() {
        if (estado == Estado.MUERTO) return;
        estado = Estado.MUERTO;
        deadTimer = 0f;
        velX = 0f;
        velY = 0f;
    }

    public boolean isEliminar() { return eliminar; }
}
