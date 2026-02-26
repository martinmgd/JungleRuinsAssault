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

    private final Array<Serpiente> serpientes = new Array<>();
    private final Array<ProyectilVeneno> venenos = new Array<>();

    private final Texture serpienteWalk;
    private final Texture serpienteDeath;

    private int frameWpx = 128;
    private int frameHpx = 80;
    private float walkFrameDuration = 0.20f;

    private float serpVelocidad = 2.0f;
    private int serpVida = 10;

    private float spawnTimerS = 0f;
    private float spawnIntervalS = 2.0f;

    private int maxSerpientes = 3;

    private float patrolHalfRange = 2.5f;

    private TextureRegion venenoRegion = null;

    private int dmgMordisco = 12;
    private float cdMordisco = 0.9f;
    private int dmgVeneno = 8;
    private float cdVeneno = 1.8f;

    private float venenoRangoMin = 2.2f;
    private float venenoRangoMax = 7.5f;
    private float velVeneno = 10.0f;
    private float venenoW = 0.35f;
    private float venenoH = 0.35f;

    // Pájaros
    private final Array<Pajaro> pajaros = new Array<>();

    private final Texture pajaroAttak;
    private final Texture pajaroDeath;

    private float spawnTimerP = 0f;
    private float spawnIntervalP = 1.2f;
    private int maxPajaros = 2;

    private float pajaroYTop = 9.5f;
    private float pajaroSpawnMarginX = 0.8f;
    private float pajaroDiveSpeed = 12.0f;

    private int pajaroDmgContacto = 12;
    private float pajaroCdContacto = 0.60f;

    private float pajaroWWorld = 1.10f;
    private float pajaroHWorld = 0.95f;

    private float pajaroMaxBajadaFracJugador = 0.62f;

    private float pajDeadDelay = 0.10f;
    private float pajBlinkStart = 0.60f;
    private float pajDisappearAt = 1.20f;
    private float pajBlinkPeriod = 0.10f;

    // Golems + rocas
    private final Array<Golem> golems = new Array<>();
    private final Array<ProyectilRoca> rocas = new Array<>();

    private Texture golemIdle = null;
    private Texture golemWalk = null;
    private Texture golemThrow = null;
    private Texture golemAttack = null;
    private Texture golemDeath = null;

    private TextureRegion rocaRegion = null;

    private float spawnTimerG = 0f;
    private float spawnIntervalG = 3.4f;
    private int maxGolems = 2;

    private final float ppu;

    // Global
    private float ySuelo = 2f;
    private float yOffsetWorld = 0.0f;

    // Ruina = pared (límite derecha)
    private float limiteDerecha = Float.POSITIVE_INFINITY;

    private static final float SPAWN_MARGIN_CAM = 2.0f;
    private static final float MIN_DIST_PLAYER = 4.0f;
    private static final float RUINA_BUFFER = 0.02f;

    private static final float SERP_ZONE_SIZE = 7.0f;
    private static final int SERP_MAX_POR_ZONA = 3;

    private static final float DESPAWN_BEHIND = 10.0f;

    public GestorEnemigos(Texture serpienteWalk, Texture serpienteDeath,
                          Texture pajaroAttak, Texture pajaroDeath,
                          float ppu) {
        this.serpienteWalk = serpienteWalk;
        this.serpienteDeath = serpienteDeath;
        this.pajaroAttak = pajaroAttak;
        this.pajaroDeath = pajaroDeath;
        this.ppu = ppu;
    }

    public void setLimiteDerecha(float limiteDerecha) {
        this.limiteDerecha = limiteDerecha;
    }

    public void setYsuelo(float ySuelo) {
        this.ySuelo = ySuelo;

        for (Serpiente s : serpientes) s.setSueloY(ySuelo);
        for (Pajaro p : pajaros) p.setSueloY(ySuelo);
        for (Golem g : golems) g.setSueloY(ySuelo);
    }

    public void setYOffsetWorld(float yOffsetWorld) {
        this.yOffsetWorld = yOffsetWorld;

        for (Serpiente s : serpientes) s.setSueloY(ySuelo);
        for (Pajaro p : pajaros) p.setSueloY(ySuelo);
        for (Golem g : golems) g.setSueloY(ySuelo);
    }

    public void setAnimacion(int frameWpx, int frameHpx, float walkFrameDuration) {
        this.frameWpx = frameWpx;
        this.frameHpx = frameHpx;
        this.walkFrameDuration = walkFrameDuration;
    }

    public void setStats(float velocidad, int vida) {
        this.serpVelocidad = velocidad;
        this.serpVida = vida;
    }

    public void setSpawnConfig(float interval, int maxSerpientes, float spawnMinX, float spawnMaxX, float patrolHalfRange) {
        this.spawnIntervalS = Math.max(0.1f, interval);
        this.maxSerpientes = Math.min(4, Math.max(1, maxSerpientes));
        this.patrolHalfRange = Math.max(0.5f, patrolHalfRange);
    }

    public void setVenenoRegion(TextureRegion region) {
        this.venenoRegion = region;
    }

    public void setAtaques(int dmgMordisco, float cdMordisco, int dmgVeneno, float cdVeneno) {
        this.dmgMordisco = Math.max(0, dmgMordisco);
        this.cdMordisco = Math.max(0.05f, cdMordisco);
        this.dmgVeneno = Math.max(0, dmgVeneno);
        this.cdVeneno = Math.max(0.10f, cdVeneno);
    }

    public void setVenenoConfig(float rangoMin, float rangoMax, float velVeneno, float venenoW, float venenoH) {
        this.venenoRangoMin = Math.max(0f, Math.min(rangoMin, rangoMax));
        this.venenoRangoMax = Math.max(this.venenoRangoMin, Math.max(rangoMin, rangoMax));
        this.velVeneno = Math.max(1f, velVeneno);
        this.venenoW = Math.max(0.10f, venenoW);
        this.venenoH = Math.max(0.10f, venenoH);
    }

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

    public void setPajaroSizeWorld(float w, float h) {
        this.pajaroWWorld = Math.max(0.1f, w);
        this.pajaroHWorld = Math.max(0.1f, h);
    }

    public void setPajaroAlturaImpacto(float fracAlturaJugador) {
        this.pajaroMaxBajadaFracJugador = Math.max(0.10f, Math.min(fracAlturaJugador, 1.20f));
    }

    public void setPajaroMuerteConfig(float deadDelay, float blinkStart, float disappearAt, float blinkPeriod) {
        this.pajDeadDelay = Math.max(0f, deadDelay);
        this.pajBlinkStart = Math.max(0f, blinkStart);
        this.pajDisappearAt = Math.max(this.pajBlinkStart, disappearAt);
        this.pajBlinkPeriod = Math.max(0.04f, blinkPeriod);
    }

    public void setGolemTextures(Texture idle, Texture walk,
                                 Texture throwTex, Texture attack, Texture death) {
        this.golemIdle = idle;
        this.golemWalk = walk;
        this.golemThrow = throwTex;
        this.golemAttack = attack;
        this.golemDeath = death;
    }

    public void setRocaRegion(TextureRegion rocaRegion) {
        this.rocaRegion = rocaRegion;
    }

    public void setGolemSpawnConfig(float interval, int maxGolems, float minX, float maxX) {
        this.spawnIntervalG = Math.max(0.1f, interval);
        this.maxGolems = Math.min(3, Math.max(1, maxGolems));
    }

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

        spawnTimerS += delta;
        spawnTimerP += delta;
        spawnTimerG += delta;
    }

    public void updateAtaques(float delta, Jugador jugador, float ppu, float camLeftX, float viewW) {

        float rightX = camLeftX + viewW;
        Rectangle hbJugador = jugador.getHitbox(ppu);

        float killX = camLeftX - DESPAWN_BEHIND;

        for (int i = serpientes.size - 1; i >= 0; i--) {
            Serpiente s = serpientes.get(i);
            if (s.getX() < killX) serpientes.removeIndex(i);
        }

        for (int i = golems.size - 1; i >= 0; i--) {
            Golem g = golems.get(i);
            if (g.getHitbox().x + g.getHitbox().width < killX) golems.removeIndex(i);
        }

        while (spawnTimerS >= spawnIntervalS) {
            spawnTimerS -= spawnIntervalS;
            if (serpientes.size < maxSerpientes) spawnSerpienteCam(camLeftX, viewW, jugador);
            else break;
        }

        for (Serpiente s : serpientes) {
            s.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);
            s.tryMordiscoJugador(jugador, hbJugador);

            if (venenoRegion != null) {
                ProyectilVeneno v = s.tryEscupirVeneno(jugador, hbJugador, venenoRegion);
                if (v != null) venenos.add(v);
            }
        }

        for (int i = venenos.size - 1; i >= 0; i--) {
            ProyectilVeneno v = venenos.get(i);
            v.update(delta);

            if (v.isEliminar() || v.isOutOfRange(camLeftX - 2f, rightX + 2f)) {
                venenos.removeIndex(i);
                continue;
            }

            if (v.getHitbox().overlaps(hbJugador)) {
                jugador.recibirDanio(v.getDamage());
                v.marcarEliminar();
            }
        }

        while (spawnTimerP >= spawnIntervalP) {
            spawnTimerP -= spawnIntervalP;
            if (pajaros.size < maxPajaros) spawnPajaro(camLeftX, viewW, jugador);
            else break;
        }

        float hJ = jugador.getHeight(ppu);
        float minDiveY = ySuelo + hJ * pajaroMaxBajadaFracJugador;

        for (Pajaro p : pajaros) {
            p.setSueloY(ySuelo);
            p.setMinDiveY(minDiveY);
            p.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

            p.update(delta);
            p.tryDanioContacto(jugador, hbJugador);
        }

        while (spawnTimerG >= spawnIntervalG) {
            spawnTimerG -= spawnIntervalG;
            if (golems.size < maxGolems) spawnGolemCam(camLeftX, viewW, jugador);
            else break;
        }

        for (Golem g : golems) {
            g.setSueloY(ySuelo);
            g.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

            g.update(delta, jugador);

            ProyectilRoca r = g.tryThrow();
            if (r != null) rocas.add(r);
        }

        for (int i = rocas.size - 1; i >= 0; i--) {
            ProyectilRoca r = rocas.get(i);
            r.update(delta);

            if (r.isEliminar() || r.isOutOfRange(camLeftX - 2f, rightX + 2f) || r.isBelow(ySuelo - 2f)) {
                rocas.removeIndex(i);
                continue;
            }

            if (r.getHitbox().overlaps(hbJugador)) {
                jugador.recibirDanio(r.getDamage());
                r.marcarEliminar();
            }
        }
    }

    private void spawnSerpienteCam(float camLeftX, float viewW, Jugador jugador) {

        float minX = camLeftX - SPAWN_MARGIN_CAM;
        float maxX = camLeftX + viewW + SPAWN_MARGIN_CAM;

        maxX = Math.min(maxX, limiteDerecha - 0.40f);
        if (maxX <= minX + 0.5f) return;

        float preferMin = camLeftX + viewW * 0.25f;
        float preferMax = maxX;
        if (preferMax <= preferMin + 0.5f) {
            preferMin = minX;
            preferMax = maxX;
        }

        for (int tries = 0; tries < 12; tries++) {
            float x = MathUtils.random(preferMin, preferMax);

            if (Math.abs(x - jugador.getX()) < MIN_DIST_PLAYER) continue;

            float zoneKey = (float) Math.floor(x / SERP_ZONE_SIZE);
            int count = 0;
            for (Serpiente s : serpientes) {
                float sk = (float) Math.floor(s.getX() / SERP_ZONE_SIZE);
                if (sk == zoneKey) count++;
            }
            if (count >= SERP_MAX_POR_ZONA) continue;

            float pMin = x - patrolHalfRange;
            float pMax = x + patrolHalfRange;

            pMax = Math.min(pMax, limiteDerecha - 0.10f);

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

            s.setAtaques(dmgMordisco, cdMordisco, dmgVeneno, cdVeneno);
            s.setVenenoConfig(venenoRangoMin, venenoRangoMax, velVeneno, venenoW, venenoH);
            s.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

            serpientes.add(s);
            return;
        }
    }

    private void spawnGolemCam(float camLeftX, float viewW, Jugador jugador) {

        if (golemIdle == null || golemWalk == null || golemThrow == null || golemAttack == null || golemDeath == null) return;
        if (rocaRegion == null) return;

        float minX = camLeftX - SPAWN_MARGIN_CAM;
        float maxX = camLeftX + viewW + SPAWN_MARGIN_CAM;

        maxX = Math.min(maxX, limiteDerecha - 0.05f);
        if (maxX <= minX + 1.0f) return;

        float preferMin = camLeftX + viewW * 0.35f;
        float preferMax = maxX;
        if (preferMax <= preferMin + 1.0f) {
            preferMin = minX;
            preferMax = maxX;
        }

        for (int tries = 0; tries < 12; tries++) {
            float x = MathUtils.random(preferMin, preferMax);

            if (Math.abs(x - jugador.getX()) < MIN_DIST_PLAYER) continue;

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

    private void spawnPajaro(float camLeftX, float viewW, Jugador jugador) {

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

        float spawnYTop = pajaroYTop - 6.5f;
        spawnYTop = Math.max(spawnYTop, ySuelo + 2.0f);

        boolean aCabeza = MathUtils.randomBoolean();
        float passY = aCabeza
            ? (jugador.getY() + hJ * 0.85f)
            : (jugador.getY() + hJ * 0.55f);

        passY = Math.min(passY, spawnYTop - 1.0f);

        // ✅ CAMBIO MÍNIMO AQUÍ:
        // antes: margen = 2.0f y turnX se quedaba lejos del borde.
        // ahora: margen pequeño + clamp por limiteDerecha para permitir llegar hasta la ruina.
        float margen = 0.6f;
        float turnX = MathUtils.clamp(jugador.getX(), camLeftX + margen, camLeftX + viewW - margen);

        if (limiteDerecha != Float.POSITIVE_INFINITY) {
            turnX = Math.min(turnX, (limiteDerecha - 0.25f)); // ✅ casi en la pared
        }

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

        p.setAtaqueContacto(pajaroDmgContacto, pajaroCdContacto);
        p.setWorldSize(pajaroWWorld, pajaroHWorld);
        p.setMuerteConfig(pajDeadDelay, pajBlinkStart, pajDisappearAt, pajBlinkPeriod);
        p.setVida(6);
        p.setSueloY(ySuelo);

        p.setLimiteDerecha(limiteDerecha - RUINA_BUFFER);

        pajaros.add(p);
    }

    public void render(SpriteBatch batch, float camLeftX, float viewW) {
        for (Serpiente s : serpientes) s.render(batch, camLeftX, viewW);
        for (Pajaro p : pajaros) p.render(batch, camLeftX, viewW);

        float rightX = camLeftX + viewW;

        for (Golem g : golems) {
            Rectangle hb = g.getHitbox();
            if (hb.x + hb.width < camLeftX - 2f || hb.x > rightX + 2f) continue;
            g.render(batch);
        }

        for (ProyectilVeneno v : venenos) {
            if (!v.isOutOfRange(camLeftX - 2f, rightX + 2f)) v.draw(batch);
        }

        for (ProyectilRoca r : rocas) {
            if (!r.isOutOfRange(camLeftX - 2f, rightX + 2f) && !r.isBelow(ySuelo)) r.draw(batch);
        }
    }

    public Array<Serpiente> getSerpientes() { return serpientes; }
    public Array<Pajaro> getPajaros() { return pajaros; }
    public Array<Golem> getGolems() { return golems; }
    public Array<ProyectilRoca> getRocas() { return rocas; }
}

