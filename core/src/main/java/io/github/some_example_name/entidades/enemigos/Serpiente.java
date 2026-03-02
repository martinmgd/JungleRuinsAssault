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

    // Estados internos:
    // - WALK: estado activo (patrulla, puede atacar y recibir daño)
    // - DEAD: estado de muerte (parpadeo y desaparición)
    private enum Estado { WALK, DEAD }

    // Posición del enemigo en coordenadas de mundo.
    private float x;
    private float y;

    // Límites horizontales de patrulla.
    private final float minX;
    private final float maxX;

    // Velocidad horizontal actual (signo indica dirección).
    private float vx;

    // Vida actual de la serpiente.
    private int vida;

    // Bandera de eliminación (cuando es true el gestor la retira de la lista).
    private boolean eliminar;

    // Estado actual.
    private Estado estado = Estado.WALK;

    // Animación de caminar y frame de muerte.
    private final Animation<TextureRegion> animWalk;
    private final TextureRegion frameDeath;

    // Tiempo acumulado para avanzar la animación de caminar.
    private float tAnim = 0f;

    // Tamaño del sprite en unidades de mundo (derivado de ppu).
    private final float wWorld;
    private final float hWorld;

    // Hitbox reutilizable para colisiones.
    private final Rectangle hitbox;

    // Tiempo acumulado desde que entra en estado DEAD.
    private float deadTime = 0f;

    // Configuración de parpadeo y desaparición en muerte.
    private final float blinkStart = 1.5f;
    private final float disappearAt = 4.0f;
    private final float blinkPeriod = 0.16f;

    // Flag de visibilidad alternante durante el parpadeo.
    private boolean blinkVisible = true;

    // Offset vertical global aplicado al posicionamiento en pantalla.
    private final float yOffsetWorld;

    // Posición congelada de muerte (para que el cadáver no “salte” si la serpiente se movía).
    private float deathX;
    private float deathY;

    // Temporizador previo a DEAD: permite un pequeño delay antes de marcar como “dead”.
    // Se usa para timing visual/feedback y para permitir puntuar con isDying().
    private float preDeathTimer = 0f;

    // Duración del delay previo a entrar en estado DEAD.
    private final float deathDelay = 0.08f;

    // Configuración de ataques:
    // - Mordisco: daño + cooldown
    // - Veneno: daño + cooldown
    private int dmgMordisco;
    private float cdMordisco;
    private float tMordisco = 0f;

    private int dmgVeneno;
    private float cdVeneno;
    private float tVeneno = 0f;

    // Configuración del ataque de veneno:
    // - rango válido (min/max) para disparar
    // - velocidad del proyectil (aunque aquí se usa una constante base)
    // - tamaño del proyectil en mundo
    private float venenoRangoMin;
    private float venenoRangoMax;
    private float velVeneno;
    private float venenoW;
    private float venenoH;

    // Altura del suelo en mundo para referencias del proyectil y colocación del enemigo.
    private float sueloY = 2f;

    // Parámetros físicos del veneno (componentes iniciales y gravedad).
    // El proyectil se construye con estos valores para simular arco.
    private static final float VENENO_VX = 9.5f;
    private static final float VENENO_VY = 2.9f;
    private static final float VENENO_GRAVITY = -16.0f;

    // RUINA = pared (límite derecha)
    // Se usa para restringir la patrulla y evitar que atraviese la pared.
    private float limiteDerecha = Float.POSITIVE_INFINITY;

    /*
     * Configura el límite derecho del nivel (pared/ruina).
     * Si es infinito, no se aplica restricción.
     */
    public void setLimiteDerecha(float limiteDerecha) {
        this.limiteDerecha = limiteDerecha;
    }

    // Acceso para el gestor (por ejemplo, para despawn o control de densidad por zonas).
    public float getX() { return x; }

    /*
     * Constructor:
     * - Inicializa posición, límites de patrulla y stats base
     * - Construye animación de caminar a partir de spritesheet (split por frameWpx/frameHpx)
     * - Calcula dimensiones en mundo a partir de ppu
     * - Inicializa hitbox y posición de muerte
     */
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

        // La serpiente se dibuja sobre el suelo más el offset global.
        this.sueloY = sueloY;
        this.y = sueloY + yOffsetWorld;

        this.minX = minX;
        this.maxX = maxX;

        // Inicia moviéndose hacia la izquierda.
        this.vx = -Math.abs(velocidadWorld);

        this.vida = vidaInicial;

        // Divide el sheet en una matriz de frames; se asume que la animación está en la primera fila.
        TextureRegion[][] split = TextureRegion.split(sheetWalk, frameWpx, frameHpx);

        // Se seleccionan explícitamente dos frames de caminata (0 y 1).
        // (La capacidad inicial (2) es una micro-optimización para evitar reallocs.)
        Array<TextureRegion> frames = new Array<>(2);
        frames.add(split[0][0]);
        frames.add(split[0][1]);

        // Animación en bucle continuo.
        animWalk = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);

        // Frame de muerte como textura completa (se asume sprite único).
        frameDeath = new TextureRegion(texDeath);

        // Conversión de píxeles a unidades de mundo.
        wWorld = frameWpx / ppu;
        hWorld = frameHpx / ppu;

        // Hitbox ajustada (más pequeña que el sprite para colisión más justa).
        hitbox = new Rectangle(x, this.y, wWorld * 0.8f, hWorld * 0.6f);

        // Inicializa posición de muerte con la posición actual.
        deathX = x;
        deathY = this.y;
    }

    /*
     * Configura el daño y cooldown de los ataques.
     * Normalmente es inyectado desde el GestorEnemigos.
     */
    public void setAtaques(int dmgMordisco, float cdMordisco, int dmgVeneno, float cdVeneno) {
        this.dmgMordisco = dmgMordisco;
        this.cdMordisco = cdMordisco;
        this.dmgVeneno = dmgVeneno;
        this.cdVeneno = cdVeneno;
    }

    /*
     * Configura parámetros del ataque de veneno:
     * - rango mínimo y máximo para disparar
     * - velocidad y tamaño del proyectil (en esta clase la velocidad se fija con constantes, pero se guarda)
     */
    public void setVenenoConfig(float rangoMin, float rangoMax, float velVeneno, float venenoW, float venenoH) {
        this.venenoRangoMin = rangoMin;
        this.venenoRangoMax = rangoMax;
        this.velVeneno = velVeneno;
        this.venenoW = venenoW;
        this.venenoH = venenoH;
    }

    /*
     * Actualiza el suelo:
     * - Si está muerta, no se reposiciona (se mantiene cadáver estático)
     * - Si está viva, ajusta y y la hitbox según nuevo suelo + offset
     */
    public void setSueloY(float sueloY) {
        if (estado == Estado.DEAD) return;

        this.sueloY = sueloY;

        y = sueloY + yOffsetWorld;
        hitbox.y = y;
    }

    /*
     * Update por frame:
     * - Reduce timers de cooldown
     * - Si está en WALK: gestiona delay previo a muerte y patrulla entre minX/maxX con límite por pared
     * - Si está en DEAD: gestiona parpadeo y eliminación por tiempo
     */
    public void update(float delta) {
        if (eliminar) return;

        // Cooldowns se tratan como temporizadores decrecientes.
        tMordisco -= delta;
        tVeneno -= delta;

        if (estado == Estado.WALK) {

            // Delay previo a entrar en DEAD (permite feedback/consistencia con puntuación).
            if (preDeathTimer > 0f) {
                preDeathTimer -= delta;
                if (preDeathTimer <= 0f) {
                    estado = Estado.DEAD;
                    deadTime = 0f;
                    // Detiene movimiento al morir.
                    vx = 0f;
                }
                return;
            }

            // Avanza animación y posición.
            tAnim += delta;
            x += vx * delta;

            // Patrulla estable: de minX a maxX.
            float min = minX;
            float max = maxX;

            // Si existe pared (ruina), permite acercarse pero no atravesarla.
            if (limiteDerecha != Float.POSITIVE_INFINITY) {
                float maxPared = limiteDerecha - wWorld * 0.25f;
                max = Math.min(max, maxPared);
            }

            // Evita rangos degenerados: si max < min, colapsa ambos al punto medio.
            if (max < min) {
                float mid = (min + max) * 0.5f;
                min = mid;
                max = mid;
            }

            // Reglas de rebote dentro del rango de patrulla.
            if (x < min) {
                x = min;
                vx = Math.abs(vx);
            } else if (x > max) {
                x = max;
                vx = -Math.abs(vx);
            }

            // Actualiza hitbox con la posición actual.
            hitbox.x = x;
            hitbox.y = y;
            return;
        }

        if (estado == Estado.DEAD) {
            deadTime += delta;

            // Parpadeo a partir de blinkStart alternando visibilidad cada blinkPeriod.
            if (deadTime >= blinkStart) {
                int phase = (int) ((deadTime - blinkStart) / blinkPeriod);
                blinkVisible = (phase % 2) == 0;
            } else {
                blinkVisible = true;
            }

            // Al superar el tiempo de desaparición, se marca para eliminar.
            if (deadTime >= disappearAt) eliminar = true;
        }
    }

    /*
     * Ataque cuerpo a cuerpo: mordisco si el jugador solapa la hitbox.
     * Respeta cooldown tMordisco.
     */
    public void tryMordiscoJugador(Jugador jugador, Rectangle hbJugador) {
        if (estado != Estado.WALK) return;
        if (tMordisco > 0f) return;

        if (hitbox.overlaps(hbJugador)) {
            jugador.recibirDanio(dmgMordisco);
            tMordisco = cdMordisco;
        }
    }

    /*
     * Ataque a distancia: intenta escupir veneno.
     * Condiciones:
     * - Debe estar en WALK
     * - Cooldown tVeneno debe estar disponible
     * - Distancia horizontal al jugador debe estar entre rangoMin y rangoMax
     *
     * Devuelve:
     * - Un ProyectilVeneno listo para añadirse al gestor, o null si no dispara.
     */
    public ProyectilVeneno tryEscupirVeneno(Jugador jugador, Rectangle hbJugador, TextureRegion region) {
        if (estado != Estado.WALK) return null;
        if (tVeneno > 0f) return null;

        // Centro aproximado del enemigo y del jugador (usando hitbox del jugador).
        float cx = x + wWorld * 0.5f;
        float jx = hbJugador.x + hbJugador.width * 0.5f;
        float dist = Math.abs(jx - cx);

        // Solo dispara si el jugador está dentro del rango permitido.
        if (dist < venenoRangoMin || dist > venenoRangoMax) return null;

        // Dirección del disparo (signo hacia el jugador).
        float dir = Math.signum(jx - cx);

        // Activa cooldown del veneno.
        tVeneno = cdVeneno;

        // Punto de spawn del proyectil: desde el centro del sprite.
        float spawnX = cx;
        float spawnY = y + hWorld * 0.5f;

        // Velocidad inicial del veneno con arco balístico simple.
        float vxVen = dir * VENENO_VX;
        float vyVen = VENENO_VY;

        return new ProyectilVeneno(
            region,
            spawnX,
            spawnY,
            vxVen,
            vyVen,
            VENENO_GRAVITY,
            venenoW,
            venenoH,
            dmgVeneno,
            sueloY
        );
    }

    /*
     * Render:
     * - Hace culling horizontal según cámara
     * - En WALK: dibuja frame animado con flip según vx
     * - En DEAD: dibuja frameDeath solo si blinkVisible
     *
     * Nota: en DEAD se usa deathX/deathY congelados para estabilidad visual.
     */
    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        if (eliminar) return;

        float drawX = (estado == Estado.DEAD) ? deathX : x;
        float drawY = (estado == Estado.DEAD) ? deathY : y;

        // Culling básico en X para ahorrar draw calls fuera de cámara.
        if (drawX + wWorld < camLeftX || drawX > camLeftX + viewW) return;

        if (estado == Estado.WALK) {
            TextureRegion frame = animWalk.getKeyFrame(tAnim);

            // Flip horizontal por escala negativa si va hacia la izquierda.
            if (vx < 0f) batch.draw(frame, drawX + wWorld, drawY, -wWorld, hWorld);
            else batch.draw(frame, drawX, drawY, wWorld, hWorld);
            return;
        }

        if (blinkVisible) {
            batch.draw(frameDeath, drawX, drawY, wWorld, hWorld);
        }
    }

    // Accesos para colisiones/gestor.
    public Rectangle getHitbox() { return hitbox; }
    public boolean isEliminar() { return eliminar; }

    // Estado DEAD: ya completó el delay y entró en la animación de muerte.
    public boolean isDead() { return estado == Estado.DEAD; }

    // “Dying”: incluye el tiempo de delay previo (útil para puntuar inmediatamente al matar).
    public boolean isDying() {
        return estado == Estado.DEAD || preDeathTimer > 0f;
    }

    /*
     * Recibe daño:
     * - Solo se procesa en WALK
     * - Al llegar a 0: congela posición de muerte y activa preDeathTimer
     *   (la transición a DEAD real ocurre en update()).
     */
    public void recibirDanio(int dmg) {
        if (estado != Estado.WALK) return;

        vida -= dmg;
        if (vida <= 0) {
            vida = 0;

            // Congela posición para render de muerte estable.
            deathX = x;
            deathY = y;

            // Activa delay previo a entrar en estado DEAD.
            preDeathTimer = deathDelay;
        }
    }
}
