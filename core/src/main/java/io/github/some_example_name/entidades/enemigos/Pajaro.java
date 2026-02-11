package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import io.github.some_example_name.entidades.jugador.Jugador;

public class Pajaro {

    public enum Estado { LANZANDO, SUBIENDO, MUERTO }

    private Estado estado = Estado.LANZANDO;

    private float x, y;
    private float yTop;
    private float ySuelo = 2f;

    private float velX, velY;
    private boolean mirandoDerecha = false;

    private final float ppu;
    private final float yOffsetWorld;

    private final TextureRegion ataque;
    private final TextureRegion muerto;

    private final Rectangle hitbox = new Rectangle();

    private float diveSpeed = 12.0f;

    private int dmgContacto = 12;
    private float cdContacto = 0.60f;
    private float cdTimer = 0f;

    private int vida = 6;
    private boolean eliminar = false;

    private float minDiveY = Float.NEGATIVE_INFINITY;

    private float wWorld = 1.10f;
    private float hWorld = 0.95f;

    private float deadTime = 0f;

    private float deadDelay = 0.10f;
    private float blinkStart = 0.60f;
    private float disappearAt = 1.20f;
    private float blinkPeriod = 0.10f;

    private boolean blinkVisible = true;

    private boolean deadOnGround = false;
    private float deadDelayTimer = 0f;

    private float gravityDead = -25f;
    private float deadFallVelY = 0f;

    // Tu modo original: "V" de 150º aprox hacia el jugador (no se toca)
    private static final float OBJ_ANGLE_DEG = 75f;
    private static final float OBJ_RATIO = 3.7320508f; // tan(75º)
    private static final float MIN_VX_FRAC = 0.25f;

    // ----------------------------------------------------------------------
    // MODO CRUCE LATERAL (NUEVO)
    // ----------------------------------------------------------------------
    private boolean modoCruce = false;

    private float exitX = 0f;
    private float turnX = 0f;
    private float passY = 0f;
    private float crossTime = 1.6f;

    // Velocidad horizontal del cruce: como pediste (6..9)
    private static final float CRUCE_VX_MIN = 13.0f;
    private static final float CRUCE_VX_MAX = 14.0f;

    // Para que no gire nada más entrar
    private static final float CRUCE_MIN_DX_DIVE = 2.5f;

    // Permitir giro por X solo si ya está cerca de la Y objetivo
    private static final float CRUCE_Y_EPS = 0.15f;

    // Probabilidad de ataque bajo (saltable)
    private static final float PROB_ATAQUE_BAJO = 0.45f;

    // Qué tan abajo apunta respecto al jugador
    private static final float ATAQUE_BAJO_OFFSET_Y = -0.55f;

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

        this.modoCruce = false;

        iniciarLanzamiento(jugadorObjetivo);
    }

    // Constructor nuevo: CRUCE lateral->lateral apuntando a cabeza/cuerpo
    public Pajaro(
        float spawnX, float spawnYTop,
        float ySuelo,
        float diveSpeed,
        float exitX,
        float turnX,
        float passY,
        float crossTime,
        Texture texAtaque,
        Texture texMuerto,
        float ppu,
        float yOffsetWorld
    ) {
        this.x = spawnX;
        this.yTop = spawnYTop;
        this.y = spawnYTop;

        this.ySuelo = ySuelo;
        this.diveSpeed = Math.max(1f, diveSpeed);

        this.exitX = exitX;
        this.turnX = turnX;
        this.passY = passY;
        this.crossTime = Math.max(0.6f, crossTime);

        this.ppu = ppu;
        this.yOffsetWorld = yOffsetWorld;

        this.ataque = new TextureRegion(texAtaque);
        this.muerto = new TextureRegion(texMuerto);

        this.modoCruce = true;

        iniciarCruce();
    }

    public void setAtaqueContacto(int dmg, float cd) {
        this.dmgContacto = Math.max(0, dmg);
        this.cdContacto = Math.max(0.05f, cd);
    }

    public void setVida(int vida) {
        this.vida = Math.max(1, vida);
    }

    public void setMinDiveY(float minDiveY) {
        this.minDiveY = minDiveY;
    }

    public void setWorldSize(float wWorld, float hWorld) {
        this.wWorld = Math.max(0.10f, wWorld);
        this.hWorld = Math.max(0.10f, hWorld);
    }

    public void setMuerteConfig(float deadDelay, float blinkStart, float disappearAt, float blinkPeriod) {
        this.deadDelay = Math.max(0f, deadDelay);
        this.blinkStart = Math.max(0f, blinkStart);
        this.disappearAt = Math.max(this.blinkStart, disappearAt);
        this.blinkPeriod = Math.max(0.04f, blinkPeriod);
    }

    public void setSueloY(float sueloY) {
        this.ySuelo = sueloY;

        if (estado == Estado.MUERTO && deadOnGround) {
            y = ySuelo;
        }
    }

    public void update(float delta) {
        if (eliminar) return;

        if (cdTimer > 0f) cdTimer = Math.max(0f, cdTimer - delta);

        if (estado == Estado.MUERTO) {
            if (!deadOnGround) {
                deadFallVelY += gravityDead * delta;
                y += deadFallVelY * delta;

                if (y <= ySuelo) {
                    y = ySuelo;
                    deadOnGround = true;
                    deadDelayTimer = deadDelay;
                    deadTime = 0f;
                }
                return;
            }

            if (deadDelayTimer > 0f) {
                deadDelayTimer = Math.max(0f, deadDelayTimer - delta);
                return;
            }

            deadTime += delta;

            if (deadTime >= blinkStart) {
                int phase = (int) ((deadTime - blinkStart) / blinkPeriod);
                blinkVisible = (phase % 2) == 0;
            } else {
                blinkVisible = true;
            }

            if (deadTime >= disappearAt) {
                eliminar = true;
            }
            return;
        }

        // ---------------------------
        // MODO CRUCE
        // ---------------------------
        if (modoCruce) {

            x += velX * delta;
            y += velY * delta;

            // Sale por el lateral de destino
            if (mirandoDerecha) {
                if (x >= exitX) {
                    eliminar = true;
                    return;
                }
            } else {
                if (x <= exitX) {
                    eliminar = true;
                    return;
                }
            }

            float sueloControl = Math.max(ySuelo, minDiveY);
            float objetivoBajada = Math.max(sueloControl, passY);

            if (estado == Estado.LANZANDO) {

                boolean reachedTurnX = mirandoDerecha ? (x >= turnX) : (x <= turnX);
                boolean reachedY = (y <= objetivoBajada);

                // Evita el bug de “aparece y ya sube”: solo gira por X si ya bajó casi a la Y objetivo
                boolean puedeGirarPorX = reachedTurnX && (y <= (objetivoBajada + CRUCE_Y_EPS));

                if (reachedY || puedeGirarPorX) {

                    if (y < objetivoBajada) y = objetivoBajada;

                    estado = Estado.SUBIENDO;

                    float dxRestante = Math.abs(exitX - x);
                    float absVx = Math.abs(velX);
                    if (absVx < 0.0001f) absVx = 0.0001f;

                    float tUp = dxRestante / absVx;
                    tUp = Math.max(0.25f, tUp);

                    velY = (yTop - y) / tUp;
                    if (velY < 0.001f) velY = Math.abs(velY);
                }

            } else if (estado == Estado.SUBIENDO) {
                if (y > yTop) y = yTop;
            }

            mirandoDerecha = velX > 0f;
            return;
        }

        // ---------------------------
        // MODO ORIGINAL
        // ---------------------------
        if (estado == Estado.LANZANDO) {
            x += velX * delta;
            y += velY * delta;

            float sueloControl = Math.max(ySuelo, minDiveY);

            if (y <= sueloControl) {
                y = sueloControl;
                velY = Math.abs(velY);
                estado = Estado.SUBIENDO;
            }
        } else if (estado == Estado.SUBIENDO) {
            x += velX * delta;
            y += velY * delta;

            if (y >= yTop) {
                y = yTop;
                eliminar = true;
            }
        }

        mirandoDerecha = velX > 0f;
    }

    private void iniciarCruce() {
        estado = Estado.LANZANDO;

        float dxTotal = exitX - x;
        float dir = (dxTotal >= 0f) ? 1f : -1f;
        mirandoDerecha = dir > 0f;

        // Vx “intención” por tiempo, pero clamped entre 6 y 9
        float absVx = Math.abs(dxTotal) / crossTime;
        absVx = MathUtils.clamp(absVx, CRUCE_VX_MIN, CRUCE_VX_MAX);
        velX = dir * absVx;

        // turnX no puede quedar pegado al spawn
        float dxDive = Math.abs(turnX - x);
        if (dxDive < CRUCE_MIN_DX_DIVE) {
            turnX = x + dir * CRUCE_MIN_DX_DIVE;
            dxDive = CRUCE_MIN_DX_DIVE;
        }

        float tDive = dxDive / absVx;
        tDive = Math.max(0.12f, tDive);

        float sueloControl = Math.max(ySuelo, minDiveY);
        float objetivoBajada = Math.max(sueloControl, passY);

        // Velocidad vertical para llegar a la altura objetivo en turnX
        velY = (objetivoBajada - yTop) / tDive;

        // Debe bajar
        if (velY > -0.001f) velY = -Math.abs(velY);
    }

    private void iniciarLanzamiento(Jugador j) {
        estado = Estado.LANZANDO;

        float targetX = (j != null) ? j.getX() : x;
        float targetY;

        if (j != null) {
            boolean ataqueBajo = MathUtils.random() < PROB_ATAQUE_BAJO;

            if (ataqueBajo) {
                // Ataque bajo: se puede saltar
                targetY = j.getY() + ATAQUE_BAJO_OFFSET_Y;
            } else {
                // Ataque alto: cabeza / pecho
                targetY = j.getY() + 0.85f;
            }
        } else {
            targetY = ySuelo + 1.0f;
        }

        float tx = targetX - x;
        float ty = targetY - y;

        float len = (float) Math.sqrt(tx * tx + ty * ty);
        if (len < 0.0001f) len = 1f;

        float nx = tx / len;
        float ny = ty / len;

        float dir = (nx >= 0f) ? 1f : -1f;

        float vx0 = nx * diveSpeed;
        float vy0 = ny * diveSpeed;

        if (vy0 > -0.001f) vy0 = -Math.abs(vy0);

        float absVx = Math.abs(vx0);
        float minAbsVx = diveSpeed * MIN_VX_FRAC;

        if (absVx < minAbsVx) {
            float absVxNew = minAbsVx;

            float absVyTarget = absVxNew * OBJ_RATIO;

            float absVyMax = (float) Math.sqrt(Math.max(0.0001f, diveSpeed * diveSpeed - absVxNew * absVxNew));
            float absVyNew = Math.min(absVyTarget, absVyMax);

            velX = dir * absVxNew;
            velY = -absVyNew;
        } else {
            velX = vx0;
            velY = vy0;
        }

        float sp = (float) Math.sqrt(velX * velX + velY * velY);
        if (sp < 0.0001f) sp = 1f;
        float k = diveSpeed / sp;
        velX *= k;
        velY *= k;

        mirandoDerecha = velX > 0f;
    }

    public void tryDanioContacto(Jugador jugador, Rectangle hbJugador) {
        if (estado == Estado.MUERTO) return;
        if (cdTimer > 0f) return;

        Rectangle hbPajaro = getHitbox(ppu);

        if (hbPajaro.overlaps(hbJugador)) {
            jugador.recibirDanio(dmgContacto);
            cdTimer = cdContacto;

            if (estado == Estado.LANZANDO) {
                velY = Math.abs(velY);
                estado = Estado.SUBIENDO;
            }
        }
    }

    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        if (eliminar) return;

        float rightX = camLeftX + viewW;

        float w = wWorld;
        float h = hWorld;

        if (x + w < camLeftX - 2f || x > rightX + 2f) return;

        float drawY = y + yOffsetWorld;

        if (estado == Estado.LANZANDO) {
            drawY += h * 0.10f;
        }

        if (estado == Estado.MUERTO) {
            if (!deadOnGround) {
                drawRegion(batch, muerto, x, drawY, w, h, 0f, mirandoDerecha);
                return;
            }

            if (deadDelayTimer > 0f) {
                drawRegion(batch, muerto, x, drawY, w, h, 0f, mirandoDerecha);
                return;
            }

            if (!blinkVisible) return;

            drawRegion(batch, muerto, x, drawY, w, h, 0f, mirandoDerecha);
            return;
        }

        float rot = 0f;
        if (estado == Estado.SUBIENDO) {
            rot = mirandoDerecha ? 90f : -90f;
        }

        drawRegion(batch, ataque, x, drawY, w, h, rot, mirandoDerecha);
    }

    private void drawRegion(SpriteBatch batch, TextureRegion region,
                            float x, float y, float w, float h,
                            float rotationDeg, boolean mirandoDerecha) {

        float originX = w * 0.5f;
        float originY = h * 0.5f;

        float scaleX = mirandoDerecha ? 1f : -1f;
        float scaleY = 1f;

        batch.draw(region,
            x, y,
            originX, originY,
            w, h,
            scaleX, scaleY,
            rotationDeg
        );
    }

    public Rectangle getHitbox(float pixelsPerUnit) {
        float w = wWorld;
        float h = hWorld;

        float hbW = w * 0.70f;
        float hbH = h * 0.55f;

        float hbX = x + (w - hbW) * 0.5f;
        float hbY = y + h * 0.20f;

        hitbox.set(hbX, hbY, hbW, hbH);
        return hitbox;
    }

    public void recibirDanio(int dmg) {
        if (estado == Estado.MUERTO) return;

        vida -= dmg;
        if (vida <= 0) {
            vida = 0;
            matar();
        }
    }

    public void matar() {
        if (estado == Estado.MUERTO) return;

        estado = Estado.MUERTO;

        deadOnGround = false;
        deadDelayTimer = 0f;
        deadTime = 0f;

        deadFallVelY = 0f;

        velX = 0f;
        velY = 0f;
    }

    public boolean isDead() {
        return estado == Estado.MUERTO;
    }

    public boolean isEliminar() {
        return eliminar;
    }
}
