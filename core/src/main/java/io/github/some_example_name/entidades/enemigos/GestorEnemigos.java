package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilRoca;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilVeneno;

public class GestorEnemigos {

    // ---------------------------------------------------------------------
    // COLECCIONES DE ENTIDADES ACTIVAS
    // ---------------------------------------------------------------------

    // Serpientes activas en el mundo.
    private final Array<Serpiente> serpientes = new Array<>();

    // Proyectiles de veneno activos (disparados por serpientes).
    private final Array<ProyectilVeneno> venenos = new Array<>();

    // Texturas base de animación de serpiente (walk y death).
    private final Texture serpienteWalk;
    private final Texture serpienteDeath;

    // ---------------------------------------------------------------------
    // CONFIGURACIÓN SERPIENTE (ANIMACIÓN / STATS / SPAWN / ATAQUES)
    // ---------------------------------------------------------------------

    // Dimensiones de frame en píxeles para el spritesheet de caminata.
    private int frameWpx = 128;
    private int frameHpx = 80;

    // Duración de cada frame de la animación de caminar.
    private float walkFrameDuration = 0.20f;

    // Velocidad de movimiento en unidades de mundo (depende de cómo lo interprete Serpiente).
    private float serpVelocidad = 2.0f;

    // Vida base de la serpiente.
    private int serpVida = 10;

    // Temporizador acumulado para spawns de serpientes.
    private float spawnTimerS = 0f;

    // Intervalo entre spawns de serpientes.
    private float spawnIntervalS = 2.0f;

    // Máximo de serpientes simultáneas.
    private int maxSerpientes = 3;

    // Mitad del rango de patrulla (la serpiente patrulla entre x±patrolHalfRange).
    private float patrolHalfRange = 2.5f;

    // Región visual del proyectil de veneno (si es null no se dispara veneno).
    private TextureRegion venenoRegion = null;

    // Daño y cooldown del mordisco cuerpo a cuerpo.
    private int dmgMordisco = 12;
    private float cdMordisco = 0.9f;

    // Daño y cooldown del ataque a distancia (veneno).
    private int dmgVeneno = 8;
    private float cdVeneno = 1.8f;

    // Configuración del ataque de veneno: rangos, velocidad y tamaño del proyectil.
    private float venenoRangoMin = 2.2f;
    private float venenoRangoMax = 7.5f;
    private float velVeneno = 10.0f;
    private float venenoW = 0.35f;
    private float venenoH = 0.35f;

    // ---------------------------------------------------------------------
    // PÁJAROS (ENTIDADES + CONFIGURACIÓN)
    // ---------------------------------------------------------------------

    // Pájaros activos en el mundo.
    private final Array<Pajaro> pajaros = new Array<>();

    // Texturas base del pájaro (ataque y muerte).
    private final Texture pajaroAttak;
    private final Texture pajaroDeath;

    // Temporizador acumulado para spawns de pájaros.
    private float spawnTimerP = 0f;

    // Intervalo entre spawns de pájaros.
    private float spawnIntervalP = 1.2f;

    // Máximo de pájaros simultáneos.
    private int maxPajaros = 2;

    // Altura objetivo superior para el spawn del pájaro (en coordenadas de mundo).
    private float pajaroYTop = 9.5f;

    // Margen horizontal extra fuera de cámara para spawnear/exit (evita pop-in visible).
    private float pajaroSpawnMarginX = 0.8f;

    // Velocidad de descenso/ataque del pájaro.
    private float pajaroDiveSpeed = 12.0f;

    // Daño por contacto y cooldown para aplicar daño repetido.
    private int pajaroDmgContacto = 12;
    private float pajaroCdContacto = 0.60f;

    // Tamaño del pájaro en unidades de mundo para render/hitbox (según implementación de Pajaro).
    private float pajaroWWorld = 1.10f;
    private float pajaroHWorld = 0.95f;

    // Controla hasta qué fracción de la altura del jugador puede bajar el pájaro.
    private float pajaroMaxBajadaFracJugador = 0.62f;

    // Configuración visual/temporal de la muerte del pájaro (delay, parpadeo, desaparición).
    private float pajDeadDelay = 0.10f;
    private float pajBlinkStart = 0.60f;
    private float pajDisappearAt = 1.20f;
    private float pajBlinkPeriod = 0.10f;

    // ---------------------------------------------------------------------
    // GOLEMS + PROYECTILES DE ROCA
    // ---------------------------------------------------------------------

    // Golems activos en el mundo.
    private final Array<Golem> golems = new Array<>();

    // Rocas activas lanzadas por golems.
    private final Array<ProyectilRoca> rocas = new Array<>();

    // Texturas requeridas por el golem (idle, walk, throw, attack, death).
    private Texture golemIdle = null;
    private Texture golemWalk = null;
    private Texture golemThrow = null;
    private Texture golemAttack = null;
    private Texture golemDeath = null;

    // Región visual del proyectil de roca (si es null el golem no puede lanzar).
    private TextureRegion rocaRegion = null;

    // Temporizador acumulado para spawns de golems.
    private float spawnTimerG = 0f;

    // Intervalo entre spawns de golems.
    private float spawnIntervalG = 3.4f;

    // Máximo de golems simultáneos.
    private int maxGolems = 2;

    // Pixels-per-unit: factor de conversión usado por varias entidades/hitboxes.
    private final float ppu;

    // ---------------------------------------------------------------------
    // PARÁMETROS GLOBALES DE MUNDO / LÍMITES
    // ---------------------------------------------------------------------

    // Altura del suelo en coordenadas de mundo.
    private float ySuelo = 2f;

    // Offset vertical global (por ejemplo, si el mundo se desplaza o hay tiles con offset).
    private float yOffsetWorld = 0.0f;

    // Límite físico a la derecha (ej. ruina/pared). Por defecto infinito si no hay pared.
    private float limiteDerecha = Float.POSITIVE_INFINITY;

    // Margen de spawn relativo a cámara para permitir spawns fuera de pantalla (evita aparición brusca).
    private static final float SPAWN_MARGIN_CAM = 2.0f;

    // Distancia mínima al jugador para permitir spawn (evita spawns injustos encima).
    private static final float MIN_DIST_PLAYER = 4.0f;

    // Pequeño margen para evitar colisiones/solapes exactos con la pared de la ruina.
    private static final float RUINA_BUFFER = 0.02f;

    // Control de densidad de serpientes por zonas (anti-clustering).
    private static final float SERP_ZONE_SIZE = 7.0f;
    private static final int SERP_MAX_POR_ZONA = 3;

    // Distancia detrás de cámara a partir de la cual se eliminan enemigos (despawn).
    private static final float DESPAWN_BEHIND = 10.0f;

    /*
     * Constructor:
     * Recibe texturas base necesarias para serpientes y pájaros, y el factor ppu.
     * (Las texturas de golem y regiones de proyectiles se inyectan mediante setters).
     */
    public GestorEnemigos(Texture serpienteWalk, Texture serpienteDeath,
                          Texture pajaroAttak, Texture pajaroDeath,
                          float ppu) {
        this.serpienteWalk = serpienteWalk;
        this.serpienteDeath = serpienteDeath;
        this.pajaroAttak = pajaroAttak;
        this.pajaroDeath = pajaroDeath;
        this.ppu = ppu;
    }

    /*
     * Establece el límite derecho del nivel (pared/ruina).
     * Se usa para restringir spawns y movimiento/ataques.
     */
    public void setLimiteDerecha(float limiteDerecha) {
        this.limiteDerecha = limiteDerecha;
    }

    /*
     * Actualiza la altura del suelo global y propaga el valor a las entidades ya existentes.
     * Esto asegura coherencia si el nivel o el offset cambian durante la ejecución.
     */
    public void setYsuelo(float ySuelo) {
        this.ySuelo = ySuelo;

        for (Serpiente s : serpientes) s.setSueloY(ySuelo);
        for (Pajaro p : pajaros) p.setSueloY(ySuelo);
        for (Golem g : golems) g.setSueloY(ySuelo);
    }

    /*
     * Actualiza el offset vertical global.
     * Además, vuelve a propagar el suelo a entidades (según diseño del proyecto).
     */
    public void setYOffsetWorld(float yOffsetWorld) {
        this.yOffsetWorld = yOffsetWorld;

        for (Serpiente s : serpientes) s.setSueloY(ySuelo);
        for (Pajaro p : pajaros) p.setSueloY(ySuelo);
        for (Golem g : golems) g.setSueloY(ySuelo);
    }

    /*
     * Configura parámetros de animación de caminata para serpientes.
     * Permite reutilizar el mismo gestor con diferentes spritesheets/configs.
     */
    public void setAnimacion(int frameWpx, int frameHpx, float walkFrameDuration) {
        this.frameWpx = frameWpx;
        this.frameHpx = frameHpx;
        this.walkFrameDuration = walkFrameDuration;
    }

    /*
     * Configura estadísticas base de la serpiente (movimiento y vida).
     */
    public void setStats(float velocidad, int vida) {
        this.serpVelocidad = velocidad;
        this.serpVida = vida;
    }

    /*
     * Configura comportamiento de spawn y patrulla de serpientes.
     * Nota: spawnMinX/spawnMaxX se reciben pero no se usan en esta implementación (spawn es relativo a cámara).
     */
    public void setSpawnConfig(float interval, int maxSerpientes, float spawnMinX, float spawnMaxX, float patrolHalfRange) {
        this.spawnIntervalS = Math.max(0.1f, interval);
        this.maxSerpientes = Math.min(4, Math.max(1, maxSerpientes));
        this.patrolHalfRange = Math.max(0.5f, patrolHalfRange);
    }

    /*
     * Asigna la región visual del veneno. Si no se asigna, las serpientes no escupen veneno.
     */
    public void setVenenoRegion(TextureRegion region) {
        this.venenoRegion = region;
    }

    /*
     * Configura daño y cooldown de ataques de serpiente.
     * Se clampa para evitar valores inválidos (negativos o cooldowns demasiado bajos).
     */
    public void setAtaques(int dmgMordisco, float cdMordisco, int dmgVeneno, float cdVeneno) {
        this.dmgMordisco = Math.max(0, dmgMordisco);
        this.cdMordisco = Math.max(0.05f, cdMordisco);
        this.dmgVeneno = Math.max(0, dmgVeneno);
        this.cdVeneno = Math.max(0.10f, cdVeneno);
    }

    /*
     * Configuración del proyectil de veneno: rangos de uso, velocidad y tamaño.
     * Se normalizan mínimos/máximos para evitar rangos invertidos.
     */
    public void setVenenoConfig(float rangoMin, float rangoMax, float velVeneno, float venenoW, float venenoH) {
        this.venenoRangoMin = Math.max(0f, Math.min(rangoMin, rangoMax));
        this.venenoRangoMax = Math.max(this.venenoRangoMin, Math.max(rangoMin, rangoMax));
        this.velVeneno = Math.max(1f, velVeneno);
        this.venenoW = Math.max(0.10f, venenoW);
        this.venenoH = Math.max(0.10f, venenoH);
    }

    /*
     * Configura spawn y comportamiento de ataque por contacto del pájaro.
     * Ajusta alturas mínimas para que no spawnee por debajo del suelo.
     */
    public void setPajaroConfig(float interval, int maxPajaros,
                                float yTopPantalla, float spawnMarginX,
                                float diveSpeed,
                                int dmgContacto, float cdContacto) {
        this.spawnIntervalP = Math.max(0.1f, interval);
        this.maxPajaros = Math.max(1, maxPajaros);

        this.pajaroYTop = Math.max(ySuelo + 1f, yTopPantalla);
        this.pajaroSpawnMarginX = Math.max(0f, spawnMarginX);

        this.pajaroDiveSpeed = Math.max(1f, diveSpeed);

        this.pajaroDmgContacto = Math.max(0, dmgContacto);
        this.pajaroCdContacto = Math.max(0.05f, cdContacto);
    }

    /*
     * Define el tamaño del pájaro en mundo (render/hitbox según implementación).
     */
    public void setPajaroSizeWorld(float w, float h) {
        this.pajaroWWorld = Math.max(0.1f, w);
        this.pajaroHWorld = Math.max(0.1f, h);
    }

    /*
     * Define cuán abajo puede llegar el pájaro en el ataque respecto a la altura del jugador.
     * Se clampa para evitar valores no razonables.
     */
    public void setPajaroAlturaImpacto(float fracAlturaJugador) {
        this.pajaroMaxBajadaFracJugador = Math.max(0.10f, Math.min(fracAlturaJugador, 1.20f));
    }

    /*
     * Configura timings de muerte del pájaro (delay inicial, inicio de blink, desaparición y periodo blink).
     */
    public void setPajaroMuerteConfig(float deadDelay, float blinkStart, float disappearAt, float blinkPeriod) {
        this.pajDeadDelay = Math.max(0f, deadDelay);
        this.pajBlinkStart = Math.max(0f, blinkStart);
        this.pajDisappearAt = Math.max(this.pajBlinkStart, disappearAt);
        this.pajBlinkPeriod = Math.max(0.04f, blinkPeriod);
    }

    /*
     * Inyecta las texturas del golem. Si alguna es null, el spawn de golems se deshabilita.
     */
    public void setGolemTextures(Texture idle, Texture walk,
                                 Texture throwTex, Texture attack, Texture death) {
        this.golemIdle = idle;
        this.golemWalk = walk;
        this.golemThrow = throwTex;
        this.golemAttack = attack;
        this.golemDeath = death;
    }

    /*
     * Inyecta la región visual de la roca. Sin ella, los golems no pueden lanzar proyectiles.
     */
    public void setRocaRegion(TextureRegion rocaRegion) {
        this.rocaRegion = rocaRegion;
    }

    /*
     * Configura spawn de golems. minX/maxX se reciben pero no se usan (spawn es relativo a cámara).
     */
    public void setGolemSpawnConfig(float interval, int maxGolems, float minX, float maxX) {
        this.spawnIntervalG = Math.max(0.1f, interval);
        this.maxGolems = Math.min(3, Math.max(1, maxGolems));
    }

    /*
     * Update “general”:
     * - Actualiza serpientes (incluye lógica interna, animación, IA)
     * - Elimina entidades marcadas para eliminación
     * - Actualiza timers de spawn (serpiente/pájaro/golem)
     *
     * Nota: los proyectiles se actualizan en updateAtaques(), donde se tienen referencias a cámara/jugador.
     */
    public void update(float delta) {

        for (int i = serpientes.size - 1; i >= 0; i--) {
            Serpiente s = serpientes.get(i);
            s.update(delta);
            if (s.isEliminar()) serpientes.removeIndex(i);
        }

        for (int i = pajaros.size - 1; i >= 0; i--) {
            if (pajaros.get(i).isEliminar()) pajaros.removeIndex(i);
        }

        for (int i = golems.size - 1; i >= 0; i--) {
            if (golems.get(i).isEliminar()) golems.removeIndex(i);
        }

        // Acumula tiempo para spawns de cada tipo.
        spawnTimerS += delta;
        spawnTimerP += delta;
        spawnTimerG += delta;
    }

    /*
     * Update de ataques/spawn dependiente del jugador y del estado de cámara.
     * Aquí se decide:
     * - despawn de entidades fuera de rango
     * - spawn relativo a cámara
     * - ataques: mordiscos, veneno, contacto pájaro, lanzamiento de rocas
     * - colisiones proyectil-jugador y aplicación de daño
     */
    public void updateAtaques(float delta, Jugador jugador, float ppu, float camLeftX, float viewW) {

        float rightX = camLeftX + viewW;

        // Hitbox del jugador en coordenadas de mundo (normalmente depende de ppu).
        Rectangle hbJugador = jugador.getHitbox(ppu);

        // Umbral de eliminación detrás de cámara.
        float killX = camLeftX - DESPAWN_BEHIND;

        // Despawn de serpientes por quedar demasiado atrás.
        for (int i = serpientes.size - 1; i >= 0; i--) {
            Serpiente s = serpientes.get(i);
            if (s.getX() < killX) serpientes.removeIndex(i);
        }

        // Despawn de golems por quedar demasiado atrás (comparando hitbox completo).
        for (int i = golems.size - 1; i >= 0; i--) {
            Golem g = golems.get(i);
            if (g.getHitbox().x + g.getHitbox().width < killX) golems.removeIndex(i);
        }

        // Spawning de serpientes basado en timer/intervalo.
        while (spawnTimerS >= spawnIntervalS) {
            spawnTimerS -= spawnIntervalS;
            if (serpientes.size < maxSerpientes) spawnSerpienteCam(camLeftX, viewW, jugador);
            else break;
        }

        // IA de serpientes: límite derecho, mordisco y (opcional) escupir veneno.
        for (Serpiente s : serpientes) {
            s.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);
            s.tryMordiscoJugador(jugador, hbJugador);

            if (venenoRegion != null) {
                ProyectilVeneno v = s.tryEscupirVeneno(jugador, hbJugador, venenoRegion);
                if (v != null) venenos.add(v);
            }
        }

        // Actualización y colisión de proyectiles de veneno.
        for (int i = venenos.size - 1; i >= 0; i--) {
            ProyectilVeneno v = venenos.get(i);
            v.update(delta);

            // Eliminación por estado interno o por salir de rango lateral de cámara.
            if (v.isEliminar() || v.isOutOfRange(camLeftX - 2f, rightX + 2f)) {
                venenos.removeIndex(i);
                continue;
            }

            // Impacto con jugador: aplica daño y marca eliminación del proyectil.
            if (v.getHitbox().overlaps(hbJugador)) {
                jugador.recibirDanio(v.getDamage());
                v.marcarEliminar();
            }
        }

        // Spawning de pájaros.
        while (spawnTimerP >= spawnIntervalP) {
            spawnTimerP -= spawnIntervalP;
            if (pajaros.size < maxPajaros) spawnPajaro(camLeftX, viewW, jugador);
            else break;
        }

        // Cálculo de altura mínima de “dive” del pájaro relativa a altura del jugador.
        float hJ = jugador.getHeight(ppu);
        float minDiveY = ySuelo + hJ * pajaroMaxBajadaFracJugador;

        // Actualización de pájaros + daño por contacto.
        for (Pajaro p : pajaros) {
            p.setSueloY(ySuelo);
            p.setMinDiveY(minDiveY);
            p.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

            p.update(delta);
            p.tryDanioContacto(jugador, hbJugador);
        }

        // Spawning de golems.
        while (spawnTimerG >= spawnIntervalG) {
            spawnTimerG -= spawnIntervalG;
            if (golems.size < maxGolems) spawnGolemCam(camLeftX, viewW, jugador);
            else break;
        }

        // Actualización de golems y generación de proyectiles de roca.
        for (Golem g : golems) {
            g.setSueloY(ySuelo);
            g.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

            g.update(delta, jugador);

            ProyectilRoca r = g.tryThrow();
            if (r != null) rocas.add(r);
        }

        // Actualización y colisión de rocas.
        for (int i = rocas.size - 1; i >= 0; i--) {
            ProyectilRoca r = rocas.get(i);
            r.update(delta);

            // Eliminación por estado interno, por salir de rango o por caer demasiado por debajo del suelo.
            if (r.isEliminar() || r.isOutOfRange(camLeftX - 2f, rightX + 2f) || r.isBelow(ySuelo - 2f)) {
                rocas.removeIndex(i);
                continue;
            }

            // Impacto con jugador: aplica daño y marca eliminación del proyectil.
            if (r.getHitbox().overlaps(hbJugador)) {
                jugador.recibirDanio(r.getDamage());
                r.marcarEliminar();
            }
        }
    }

    /*
     * Spawnea una serpiente usando como referencia los límites de cámara.
     * Incluye:
     * - margen fuera de cámara para evitar pop-in
     * - distancia mínima al jugador
     * - limitación de densidad por zonas para repartir enemigos
     * - patrulla limitada por rango y por pared derecha (ruina)
     */
    private void spawnSerpienteCam(float camLeftX, float viewW, Jugador jugador) {

        float minX = camLeftX - SPAWN_MARGIN_CAM;
        float maxX = camLeftX + viewW + SPAWN_MARGIN_CAM;

        // Evita spawns que atraviesen la pared derecha.
        maxX = Math.min(maxX, limiteDerecha - 0.40f);
        if (maxX <= minX + 0.5f) return;

        // Preferencia de spawn: más hacia delante del jugador/cámara, para que tenga sentido “entrar” en escena.
        float preferMin = camLeftX + viewW * 0.25f;
        float preferMax = maxX;
        if (preferMax <= preferMin + 0.5f) {
            preferMin = minX;
            preferMax = maxX;
        }

        // Varios intentos para encontrar una posición válida.
        for (int tries = 0; tries < 12; tries++) {
            float x = MathUtils.random(preferMin, preferMax);

            // No spawnear demasiado cerca del jugador.
            if (Math.abs(x - jugador.getX()) < MIN_DIST_PLAYER) continue;

            // Control de densidad: se limita el número de serpientes por “zona” del mundo.
            float zoneKey = (float) Math.floor(x / SERP_ZONE_SIZE);
            int count = 0;
            for (Serpiente s : serpientes) {
                float sk = (float) Math.floor(s.getX() / SERP_ZONE_SIZE);
                if (sk == zoneKey) count++;
            }
            if (count >= SERP_MAX_POR_ZONA) continue;

            // Rango de patrulla relativo al punto de spawn.
            float pMin = x - patrolHalfRange;
            float pMax = x + patrolHalfRange;

            // Ajuste para no sobrepasar la pared derecha.
            pMax = Math.min(pMax, limiteDerecha - 0.10f);

            // Construcción de la serpiente con parámetros de animación, stats y referencias de textura.
            Serpiente s = new Serpiente(
                x, ySuelo,
                pMin, pMax,
                serpVelocidad,
                serpVida,
                serpienteWalk, frameWpx, frameHpx, walkFrameDuration,
                serpienteDeath,
                ppu,
                yOffsetWorld
            );

            // Inyección de parámetros de ataques y de veneno.
            s.setAtaques(dmgMordisco, cdMordisco, dmgVeneno, cdVeneno);
            s.setVenenoConfig(venenoRangoMin, venenoRangoMax, velVeneno, venenoW, venenoH);

            // Límite derecho aplicado a la IA/movimiento.
            s.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

            serpientes.add(s);
            return;
        }
    }

    /*
     * Spawnea un golem relativo a cámara, siempre que las texturas y la región de roca estén configuradas.
     * Mantiene distancia mínima al jugador y respeta el límite derecho.
     */
    private void spawnGolemCam(float camLeftX, float viewW, Jugador jugador) {

        // Validación: sin assets completos no se permite spawn.
        if (golemIdle == null || golemWalk == null || golemThrow == null || golemAttack == null || golemDeath == null) return;
        if (rocaRegion == null) return;

        float minX = camLeftX - SPAWN_MARGIN_CAM;
        float maxX = camLeftX + viewW + SPAWN_MARGIN_CAM;

        // Ajuste por pared derecha.
        maxX = Math.min(maxX, limiteDerecha - 0.05f);
        if (maxX <= minX + 1.0f) return;

        // Preferencia de spawn algo más adelantada que serpiente (por tamaño/presencia).
        float preferMin = camLeftX + viewW * 0.35f;
        float preferMax = maxX;
        if (preferMax <= preferMin + 1.0f) {
            preferMin = minX;
            preferMax = maxX;
        }

        // Intentos para encontrar una posición válida.
        for (int tries = 0; tries < 12; tries++) {
            float x = MathUtils.random(preferMin, preferMax);

            if (Math.abs(x - jugador.getX()) < MIN_DIST_PLAYER) continue;

            // Construcción del golem con todas sus texturas y región del proyectil.
            Golem g = new Golem(
                x,
                ySuelo,
                golemIdle,
                golemWalk,
                golemThrow,
                golemAttack,
                golemDeath,
                rocaRegion,
                ppu
            );

            g.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);
            g.setSueloY(ySuelo);

            golems.add(g);
            return;
        }
    }

    /*
     * Spawnea un pájaro que atraviesa la pantalla y realiza un “dive” cerca del jugador.
     * El patrón decide aleatoriamente si entra por izquierda o derecha y calcula un punto de giro (turnX)
     * para permitir que el ataque pueda llegar cerca de la pared derecha si existe ruina.
     */
    private void spawnPajaro(float camLeftX, float viewW, Jugador jugador) {

        // Decide si el pájaro cruza de izquierda a derecha o viceversa.
        boolean salePorDerecha = MathUtils.randomBoolean();

        float exitX;
        float spawnX;

        if (salePorDerecha) {
            exitX = camLeftX + viewW + pajaroSpawnMarginX;
            spawnX = camLeftX - pajaroSpawnMarginX;
        } else {
            exitX = camLeftX - pajaroSpawnMarginX;
            spawnX = camLeftX + viewW + pajaroSpawnMarginX;
        }

        float hJ = jugador.getHeight(ppu);

        // Altura de spawn “top”, con ajuste mínimo para no quedarse demasiado baja.
        float spawnYTop = pajaroYTop - 6.5f;
        spawnYTop = Math.max(spawnYTop, ySuelo + 2.0f);

        // Decide si el pájaro pasa más cerca de la cabeza o del torso.
        boolean aCabeza = MathUtils.randomBoolean();
        float passY = aCabeza
            ? (jugador.getY() + hJ * 0.85f)
            : (jugador.getY() + hJ * 0.55f);

        // Evita que la trayectoria quede demasiado cerca del techo del spawn.
        passY = Math.min(passY, spawnYTop - 1.0f);

        // Cálculo de punto de giro horizontal (turnX) cerca del jugador, limitado por márgenes de cámara
        // y adicionalmente por la pared derecha si existe (para que pueda “llegar” hasta la ruina).
        float margen = 0.6f;
        float turnX = MathUtils.clamp(jugador.getX(), camLeftX + margen, camLeftX + viewW - margen);

        if (limiteDerecha != Float.POSITIVE_INFINITY) {
            turnX = Math.min(turnX, (limiteDerecha - 0.25f));
        }

        // Tiempo de cruce usado por la lógica interna del pájaro para interpolar trayectoria.
        float crossTime = 1.7f;

        Pajaro p = new Pajaro(
            spawnX,
            spawnYTop,
            ySuelo,
            pajaroDiveSpeed,
            exitX,
            turnX,
            passY,
            crossTime,
            pajaroAttak,
            pajaroDeath,
            ppu,
            yOffsetWorld
        );

        // Configuración de daño por contacto, tamaño, comportamiento de muerte y vida.
        p.setAtaqueContacto(pajaroDmgContacto, pajaroCdContacto);
        p.setWorldSize(pajaroWWorld, pajaroHWorld);
        p.setMuerteConfig(pajDeadDelay, pajBlinkStart, pajDisappearAt, pajBlinkPeriod);
        p.setVida(6);
        p.setSueloY(ySuelo);

        // Respeta límite derecho.
        p.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

        pajaros.add(p);
    }

    /*
     * Render de enemigos y proyectiles.
     * Se hace filtrado por rango de cámara para evitar dibujar entidades fuera de vista (optimización).
     */
    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        for (Serpiente s : serpientes) s.render(batch, camLeftX, viewW);
        for (Pajaro p : pajaros) p.render(batch, camLeftX, viewW);

        float rightX = camLeftX + viewW;

        // Filtrado manual de golems por hitbox para no renderizar fuera de margen.
        for (Golem g : golems) {
            Rectangle hb = g.getHitbox();
            if (hb.x + hb.width < camLeftX - 2f || hb.x > rightX + 2f) continue;
            g.render(batch);
        }

        // Render de venenos si no están fuera de rango.
        for (ProyectilVeneno v : venenos) {
            if (!v.isOutOfRange(camLeftX - 2f, rightX + 2f)) v.draw(batch);
        }

        // Render de rocas si no están fuera de rango y no han caído por debajo del suelo.
        for (ProyectilRoca r : rocas) {
            if (!r.isOutOfRange(camLeftX - 2f, rightX + 2f) && !r.isBelow(ySuelo)) r.draw(batch);
        }
    }

    // Getters para exponer colecciones activas (útil para debug, colisiones externas o UI).
    public Array<Serpiente> getSerpientes() { return serpientes; }
    public Array<Pajaro> getPajaros() { return pajaros; }
    public Array<Golem> getGolems() { return golems; }
    public Array<ProyectilRoca> getRocas() { return rocas; }
}
