package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import io.github.some_example_name.entidades.jugador.Jugador;

public class Pajaro {

    // Estados principales del enemigo:
    // - LANZANDO: fase de descenso/ataque (dive)
    // - SUBIENDO: fase de retirada (asciende tras tocar suelo o golpear)
    // - MUERTO: estado de muerte con caída, espera y parpadeo antes de eliminar
    public enum Estado { LANZANDO, SUBIENDO, MUERTO }

    // Estado actual del pájaro.
    private Estado estado = Estado.LANZANDO;

    // Posición actual (coordenadas de mundo).
    private float x, y;

    // Altura “techo” a la que el pájaro vuelve antes de eliminarse.
    private float yTop;

    // Altura del suelo (coordenadas de mundo) para colisión/limitación vertical.
    private float ySuelo = 2f;

    // Velocidades actuales (componentes X/Y) para movimiento lineal.
    private float velX, velY;

    // Dirección visual: afecta flip horizontal al render y también lógica de algunos checks.
    private boolean mirandoDerecha = false;

    // Factor pixels-per-unit; se usa para hitbox y coherencia con el resto del proyecto.
    private final float ppu;

    // Offset vertical global aplicado al dibujado (por ejemplo si el mundo se desplaza visualmente).
    private final float yOffsetWorld;

    // Regiones de textura para el estado vivo (ataque) y muerto.
    private final TextureRegion ataque;
    private final TextureRegion muerto;

    // Hitbox reutilizable para evitar crear objetos por frame.
    private final Rectangle hitbox = new Rectangle();

    // Velocidad nominal del “dive” (magnitud objetivo del vector de velocidad).
    private float diveSpeed = 12.0f;

    // Daño por contacto y cooldown para evitar aplicar daño cada frame mientras se solapa.
    private int dmgContacto = 12;
    private float cdContacto = 0.60f;
    private float cdTimer = 0f;

    // Vida del pájaro. Cuando llega a 0 pasa a estado MUERTO.
    private int vida = 6;

    // Bandera de eliminación: cuando es true el gestor lo puede retirar de la lista.
    private boolean eliminar = false;

    // Altura mínima permitida para el descenso (protección para no bajar demasiado).
    private float minDiveY = Float.NEGATIVE_INFINITY;

    // Tamaño en mundo usado para render y para hitbox (según cálculos internos).
    private float wWorld = 1.10f;
    private float hWorld = 0.95f;

    // Tiempo acumulado desde que empieza la secuencia de muerte “en suelo” (para blink/despawn).
    private float deadTime = 0f;

    // Configuración de muerte: delay inicial, inicio de parpadeo, momento de desaparición y periodo de parpadeo.
    private float deadDelay = 0.10f;
    private float blinkStart = 0.60f;
    private float disappearAt = 1.20f;
    private float blinkPeriod = 0.10f;

    // Control de visibilidad durante el parpadeo.
    private boolean blinkVisible = true;

    // Estado interno de la muerte:
    // - deadOnGround: indica si ya impactó con el suelo tras caer
    // - deadDelayTimer: espera inicial tras tocar suelo antes de iniciar blink/timeouts
    private boolean deadOnGround = false;
    private float deadDelayTimer = 0f;

    // Física simplificada de caída al morir (aceleración constante y velocidad vertical).
    private float gravityDead = -25f;
    private float deadFallVelY = 0f;

    // Parámetros del modo “original” de ataque:
    // OBJ_RATIO modela una relación objetivo entre |vy| y |vx| para un ángulo aproximado (tan 75º).
    // MIN_VX_FRAC evita que vx sea demasiado pequeño respecto a diveSpeed.
    private static final float OBJ_RATIO = 3.7320508f; // tan(75º)
    private static final float MIN_VX_FRAC = 0.25f;

    // ---------------------------------------------------------
    // MODO CRUCE
    // ---------------------------------------------------------

    // Bandera que activa el comportamiento de cruce (trayectoria por segmentos: baja y luego sube).
    private boolean modoCruce = false;

    // Parámetros del modo cruce:
    // - exitX: punto de salida horizontal fuera de pantalla
    // - turnX: punto horizontal en el que puede “girar” para iniciar subida
    // - passY: altura objetivo de paso (donde se espera que el pájaro “cruce” cerca del jugador)
    // - crossTime: tiempo total aproximado para recorrer el cruce horizontal
    private float exitX = 0f;
    private float turnX = 0f;
    private float passY = 0f;
    private float crossTime = 1.6f;

    // Límites para velocidad horizontal en modo cruce (control de ritmo y consistencia).
    private static final float CRUCE_VX_MIN = 13.0f;
    private static final float CRUCE_VX_MAX = 14.0f;

    // Distancia horizontal mínima antes de iniciar la bajada para evitar “dive” demasiado corto.
    private static final float CRUCE_MIN_DX_DIVE = 2.5f;

    // Tolerancia vertical al decidir giro en modo cruce (reduce comportamiento “nervioso” por eps).
    private static final float CRUCE_Y_EPS = 0.15f;

    // Margen extra de subida para asegurarse de que no se elimina “tapado” por elementos superiores.
    private static final float EXTRA_SUBIDA = 3.5f;

    // ---------------------------------------------------------
    // PARED RUINA (derecha)
    // ---------------------------------------------------------

    // Límite vertical derecho del nivel. Si es infinito, no se aplica rebote.
    private float limiteDerecha = Float.POSITIVE_INFINITY;

    /*
     * Configura el límite derecho para rebote contra pared vertical.
     * Se usa para impedir que el pájaro atraviese la ruina o se meta fuera del área jugable.
     */
    public void setLimiteDerecha(float limiteDerecha) {
        this.limiteDerecha = limiteDerecha;
    }

    /*
     * Constructor “modo original”:
     * - Inicializa el pájaro en una posición
     * - Activa modoCruce=false
     * - Calcula el lanzamiento apuntando a un jugador objetivo
     */
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

        // Se crean regiones completas a partir de las texturas. Se asume que son sprites individuales.
        this.ataque = new TextureRegion(texAtaque);
        this.muerto = new TextureRegion(texMuerto);

        this.modoCruce = false;

        // Inicializa velocidades para el dive apuntando al jugador.
        iniciarLanzamiento(jugadorObjetivo);
    }

    /*
     * Constructor “modo cruce”:
     * - Inicializa el pájaro con parámetros de trayectoria predefinidos
     * - Activa modoCruce=true
     * - Configura velocidades de cruce (velX/velY) con iniciarCruce()
     */
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

        // Inicializa velocidades para el patrón de cruce.
        iniciarCruce();
    }

    /*
     * Configura el ataque por contacto:
     * - dmg: daño aplicado al jugador al solapar hitboxes
     * - cd: cooldown para evitar daño continuo por frame
     */
    public void setAtaqueContacto(int dmg, float cd) {
        this.dmgContacto = Math.max(0, dmg);
        this.cdContacto = Math.max(0.05f, cd);
    }

    /*
     * Ajusta la vida del pájaro (mínimo 1 para mantener consistencia).
     */
    public void setVida(int vida) {
        this.vida = Math.max(1, vida);
    }

    /*
     * Establece la altura mínima permitida durante el descenso.
     * Se usa para evitar que el pájaro baje demasiado y atraviese/pegue al suelo en exceso.
     */
    public void setMinDiveY(float minDiveY) {
        this.minDiveY = minDiveY;
    }

    /*
     * Define tamaño del pájaro en unidades de mundo (afecta render y hitbox).
     */
    public void setWorldSize(float wWorld, float hWorld) {
        this.wWorld = Math.max(0.10f, wWorld);
        this.hWorld = Math.max(0.10f, hWorld);
    }

    /*
     * Configura la secuencia de muerte:
     * - deadDelay: espera tras tocar suelo
     * - blinkStart: tiempo a partir del cual empieza el parpadeo
     * - disappearAt: tiempo total para eliminar definitivamente
     * - blinkPeriod: periodo del parpadeo
     */
    public void setMuerteConfig(float deadDelay, float blinkStart, float disappearAt, float blinkPeriod) {
        this.deadDelay = Math.max(0f, deadDelay);
        this.blinkStart = Math.max(0f, blinkStart);
        this.disappearAt = Math.max(this.blinkStart, disappearAt);
        this.blinkPeriod = Math.max(0.04f, blinkPeriod);
    }

    /*
     * Actualiza la altura del suelo.
     * Si el pájaro está muerto y ya en suelo, ajusta su Y para mantenerse alineado.
     */
    public void setSueloY(float sueloY) {
        this.ySuelo = sueloY;
        if (estado == Estado.MUERTO && deadOnGround) y = ySuelo;
    }

    // Rebote en pared vertical derecha: reflejo del vector (velX = -velX).
    // Se aplica únicamente si:
    // - no está muerto
    // - existe un límite derecho válido
    // - el pájaro se mueve hacia la derecha y su borde derecho supera el límite
    private void aplicarReboteParedDerecha() {
        if (estado == Estado.MUERTO) return;
        if (limiteDerecha == Float.POSITIVE_INFINITY) return;

        if (velX > 0f) {
            float right = x + wWorld;
            if (right >= limiteDerecha) {
                // Recoloca la entidad justo antes de la pared para evitar solape persistente.
                x = limiteDerecha - wWorld;
                // Refleja componente horizontal para simular rebote.
                velX = -velX;
                // Al rebotar, la dirección visual cambia.
                mirandoDerecha = false;
            }
        }
    }

    /*
     * Actualización por frame:
     * - Gestiona cooldown de daño por contacto
     * - Ejecuta lógica de muerte si está en MUERTO
     * - Si está vivo, actualiza movimiento según modo (cruce u original)
     * - Aplica rebote contra pared derecha si procede
     */
    public void update(float delta) {
        if (eliminar) return;

        // Actualiza el temporizador de cooldown del daño por contacto.
        if (cdTimer > 0f) cdTimer = Math.max(0f, cdTimer - delta);

        // ---------------------------
        // ESTADO MUERTO
        // ---------------------------
        if (estado == Estado.MUERTO) {

            // Mientras no haya tocado suelo, simula caída con gravedad.
            if (!deadOnGround) {
                deadFallVelY += gravityDead * delta;
                y += deadFallVelY * delta;

                // Al llegar al suelo, se fija posición y se inicia la secuencia de espera/parpadeo.
                if (y <= ySuelo) {
                    y = ySuelo;
                    deadOnGround = true;
                    deadDelayTimer = deadDelay;
                    deadTime = 0f;
                }
                return;
            }

            // Delay inicial tras tocar suelo (no parpadea aún).
            if (deadDelayTimer > 0f) {
                deadDelayTimer = Math.max(0f, deadDelayTimer - delta);
                return;
            }

            // Tiempo desde el inicio real del estado “post-delay”.
            deadTime += delta;

            // Parpadeo por fases a partir de blinkStart.
            if (deadTime >= blinkStart) {
                int phase = (int) ((deadTime - blinkStart) / blinkPeriod);
                blinkVisible = (phase % 2) == 0;
            } else {
                blinkVisible = true;
            }

            // Al llegar al tiempo de desaparición, se marca para eliminar.
            if (deadTime >= disappearAt) eliminar = true;
            return;
        }

        // ---------------------------
        // MODO CRUCE: DOS RECTAS (baja y sube)
        // ---------------------------
        if (modoCruce) {

            // Movimiento lineal por integración simple.
            x += velX * delta;
            y += velY * delta;

            // Rebote contra pared derecha si aplica.
            aplicarReboteParedDerecha();

            // Control de altura mínima del dive: suelo o minDiveY.
            float sueloControl = Math.max(ySuelo, minDiveY);
            float objetivoBajada = Math.max(sueloControl, passY);

            if (estado == Estado.LANZANDO) {
                // En descenso, se evalúa si ya se alcanzó el punto de giro:
                // - por X (llegar a turnX) y suficientemente cerca en Y
                // - o por Y (haber bajado hasta la altura objetivo)
                boolean reachedTurnX = mirandoDerecha ? (x >= turnX) : (x <= turnX);
                boolean reachedY = (y <= objetivoBajada);
                boolean puedeGirarPorX = reachedTurnX && (y <= (objetivoBajada + CRUCE_Y_EPS));

                if (reachedY || puedeGirarPorX) {
                    if (y < objetivoBajada) y = objetivoBajada;

                    // Invierte velY a positivo para iniciar subida.
                    velY = Math.abs(velY);
                    estado = Estado.SUBIENDO;
                }

            } else if (estado == Estado.SUBIENDO) {
                // En subida, al superar techo + margen extra, se elimina.
                if (y >= (yTop + EXTRA_SUBIDA)) {
                    eliminar = true;
                    return;
                }
            }

            // Actualiza dirección visual en base a la velocidad.
            mirandoDerecha = velX > 0f;
            return;
        }

        // ---------------------------
        // MODO ORIGINAL
        // ---------------------------
        if (estado == Estado.LANZANDO) {
            x += velX * delta;
            y += velY * delta;

            aplicarReboteParedDerecha();

            float sueloControl = Math.max(ySuelo, minDiveY);

            // Al tocar la altura mínima, se inicia subida.
            if (y <= sueloControl) {
                y = sueloControl;
                velY = Math.abs(velY);
                estado = Estado.SUBIENDO;
            }
        } else if (estado == Estado.SUBIENDO) {
            x += velX * delta;
            y += velY * delta;

            aplicarReboteParedDerecha();

            // Al volver al techo (yTop), se elimina para salir de escena.
            if (y >= yTop) {
                y = yTop;
                eliminar = true;
            }
        }

        mirandoDerecha = velX > 0f;
    }

    /*
     * Inicializa parámetros de movimiento para el modo cruce.
     * Calcula:
     * - velX para completar el cruce en crossTime (clamp)
     * - velY para alcanzar objetivoBajada en el tiempo estimado de dive (tDive)
     */
    private void iniciarCruce() {
        estado = Estado.LANZANDO;

        float dxTotal = exitX - x;
        float dir = (dxTotal >= 0f) ? 1f : -1f;
        mirandoDerecha = dir > 0f;

        // Velocidad horizontal ajustada para durar crossTime, limitada por rangos min/max.
        float absVx = Math.abs(dxTotal) / crossTime;
        absVx = MathUtils.clamp(absVx, CRUCE_VX_MIN, CRUCE_VX_MAX);
        velX = dir * absVx;

        // Asegura una distancia mínima antes del giro para que la trayectoria sea consistente.
        float dxDive = Math.abs(turnX - x);
        if (dxDive < CRUCE_MIN_DX_DIVE) {
            turnX = x + dir * CRUCE_MIN_DX_DIVE;
            dxDive = CRUCE_MIN_DX_DIVE;
        }

        // Tiempo estimado hasta llegar al punto de giro.
        float tDive = dxDive / absVx;
        tDive = Math.max(0.12f, tDive);

        float sueloControl = Math.max(ySuelo, minDiveY);
        float objetivoBajada = Math.max(sueloControl, passY);

        // Calcula velY para llegar desde yTop hasta objetivoBajada en tDive.
        velY = (objetivoBajada - yTop) / tDive;

        // Garantiza que velY sea negativa durante el descenso.
        if (velY > -0.001f) velY = -Math.abs(velY);
    }

    /*
     * Inicializa el lanzamiento en modo original apuntando al jugador:
     * - Construye un vector hacia un punto del jugador (aprox. altura de cabeza/torso)
     * - Ajusta componentes para garantizar un ángulo razonable (mínimo vx)
     * - Normaliza la velocidad final para que su magnitud sea diveSpeed
     */
    private void iniciarLanzamiento(Jugador j) {
        estado = Estado.LANZANDO;

        float targetX = (j != null) ? j.getX() : x;
        float targetY = (j != null) ? (j.getY() + 0.85f) : (ySuelo + 1.0f);

        float tx = targetX - x;
        float ty = targetY - y;

        // Normaliza dirección hacia objetivo.
        float len = (float) Math.sqrt(tx * tx + ty * ty);
        if (len < 0.0001f) len = 1f;

        float nx = tx / len;
        float ny = ty / len;

        // Dirección horizontal para forzar signo consistente.
        float dir = (nx >= 0f) ? 1f : -1f;

        // Velocidad inicial “apuntada” a objetivo.
        float vx0 = nx * diveSpeed;
        float vy0 = ny * diveSpeed;

        // Asegura que el movimiento inicial tenga componente vertical de descenso.
        if (vy0 > -0.001f) vy0 = -Math.abs(vy0);

        float absVx = Math.abs(vx0);
        float minAbsVx = diveSpeed * MIN_VX_FRAC;

        // Si la componente horizontal es demasiado pequeña, se fuerza un mínimo y se recalcula vy.
        if (absVx < minAbsVx) {
            float absVxNew = minAbsVx;

            // Objetivo de vy basado en ratio (ángulo aproximado).
            float absVyTarget = absVxNew * OBJ_RATIO;

            // Límite máximo de vy para no superar magnitud total diveSpeed.
            float absVyMax = (float) Math.sqrt(Math.max(0.0001f, diveSpeed * diveSpeed - absVxNew * absVxNew));
            float absVyNew = Math.min(absVyTarget, absVyMax);

            velX = dir * absVxNew;
            velY = -absVyNew;
        } else {
            velX = vx0;
            velY = vy0;
        }

        // Normaliza para asegurar velocidad total exactamente igual a diveSpeed.
        float sp = (float) Math.sqrt(velX * velX + velY * velY);
        if (sp < 0.0001f) sp = 1f;
        float k = diveSpeed / sp;
        velX *= k;
        velY *= k;

        mirandoDerecha = velX > 0f;
    }

    /*
     * Intenta aplicar daño por contacto al jugador si las hitboxes se solapan.
     * Respeta cooldown para evitar múltiples impactos consecutivos inmediatos.
     */
    public void tryDanioContacto(Jugador jugador, Rectangle hbJugador) {
        if (estado == Estado.MUERTO) return;
        if (cdTimer > 0f) return;

        Rectangle hbPajaro = getHitbox(ppu);

        if (hbPajaro.overlaps(hbJugador)) {
            jugador.recibirDanio(dmgContacto);
            cdTimer = cdContacto;

            // Si estaba en fase de descenso, tras golpear inicia retirada.
            if (estado == Estado.LANZANDO) {
                velY = Math.abs(velY);
                estado = Estado.SUBIENDO;
            }
        }
    }

    /*
     * Render del pájaro:
     * - Aplica culling horizontal según cámara
     * - Ajusta drawY con yOffsetWorld y pequeño tweak en LANZANDO
     * - En MUERTO: renderiza caída, espera y parpadeo
     * - En vivo: rota sprite en SUBIENDO para dar feedback visual
     */
    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        if (eliminar) return;

        float rightX = camLeftX + viewW;

        float w = wWorld;
        float h = hWorld;

        // Culling con margen para no cortar demasiado pronto.
        if (x + w < camLeftX - 2f || x > rightX + 2f) return;

        float drawY = y + yOffsetWorld;

        // Pequeño desplazamiento en descenso para dar sensación de ataque.
        if (estado == Estado.LANZANDO) drawY += h * 0.10f;

        // Render del estado muerto con lógica de caída/parpadeo.
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

        // Rotación en subida para reforzar visualmente la retirada.
        float rot = 0f;
        if (estado == Estado.SUBIENDO) rot = mirandoDerecha ? 90f : -90f;

        drawRegion(batch, ataque, x, drawY, w, h, rot, mirandoDerecha);
    }

    /*
     * Dibuja una región con:
     * - origen centrado para permitir rotaciones naturales
     * - flip horizontal controlado mediante scaleX
     * - rotación en grados
     */
    private void drawRegion(SpriteBatch batch, TextureRegion region,
                            float x, float y, float w, float h,
                            float rotationDeg, boolean mirandoDerecha) {

        float originX = w * 0.5f;
        float originY = h * 0.5f;

        // Flip horizontal: si no mira a la derecha, se invierte escala X.
        float scaleX = mirandoDerecha ? 1f : -1f;

        batch.draw(region,
            x, y,
            originX, originY,
            w, h,
            scaleX, 1f,
            rotationDeg
        );
    }

    /*
     * Calcula y devuelve la hitbox del pájaro.
     * Se usa un rectángulo más pequeño que el sprite para permitir colisiones más justas.
     * El parámetro pixelsPerUnit se recibe por firma, aunque el cálculo actual usa wWorld/hWorld.
     */
    public Rectangle getHitbox(float pixelsPerUnit) {
        float w = wWorld;
        float h = hWorld;

        // Ajuste de hitbox relativo al sprite para evitar colisiones “injustas”.
        float hbW = w * 0.70f;
        float hbH = h * 0.55f;

        float hbX = x + (w - hbW) * 0.5f;
        float hbY = y + h * 0.20f;

        hitbox.set(hbX, hbY, hbW, hbH);
        return hitbox;
    }

    /*
     * Recibe daño. Si la vida llega a cero, cambia a estado MUERTO.
     */
    public void recibirDanio(int dmg) {
        if (estado == Estado.MUERTO) return;

        vida -= dmg;
        if (vida <= 0) {
            vida = 0;
            matar();
        }
    }

    /*
     * Fuerza transición a MUERTO:
     * - resetea flags y timers de muerte
     * - resetea velocidad y activa caída
     */
    public void matar() {
        if (estado == Estado.MUERTO) return;

        estado = Estado.MUERTO;

        deadOnGround = false;
        deadDelayTimer = 0f;
        deadTime = 0f;

        deadFallVelY = 0f;

        // Se anulan velocidades “vivas”; la caída se gestiona con deadFallVelY y gravityDead.
        velX = 0f;
        velY = 0f;
    }

    // Consultas de estado para el gestor externo.
    public boolean isDead() { return estado == Estado.MUERTO; }
    public boolean isEliminar() { return eliminar; }
}
