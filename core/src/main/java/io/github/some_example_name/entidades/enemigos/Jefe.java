package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilMeteoro;

import java.util.List;

public class Jefe {

    public enum Estado {
        DORMIDO,
        DESPIERTO,
        SALTANDO
    }

    private final Texture texDormido;
    private final Texture texDespierto;

    private Estado estado = Estado.DORMIDO;

    // POSICIÓN EN MUNDO
    private float x;
    private float y;
    private final float baseY;

    // TAMAÑO EN MUNDO
    private final float anchoW;
    private final float altoW;

    // Distancias en MUNDO
    private final float despertarDistW;

    private long ultimoDisparoMs = 0L;
    private final long cadenciaDisparoMs;

    private long ultimoSaltoMs = 0L;
    private final long cooldownSaltoMs;

    private final float saltoDistW;
    private final float saltoDuracionS;
    private final float saltoAlturaW;

    private float saltoT = 0f;
    private float saltoX0;
    private float saltoX1;

    private boolean vivo = true;
    private int vida;
    private final int vidaMax;

    private final float ppuBoss;
    private final float ppuPlayer;

    public Jefe(Texture texDormido,
                Texture texDespierto,
                float xInicial,
                float yInicial,
                float despertarDistW,
                int vidaMax,
                long cadenciaDisparoMs,
                long cooldownSaltoMs,
                float saltoDistW,
                float saltoDuracionS,
                float saltoAlturaW,
                float ppuBoss,
                float ppuPlayer) {

        this.texDormido = texDormido;
        this.texDespierto = texDespierto;

        this.x = xInicial;
        this.y = yInicial;
        this.baseY = yInicial;

        this.ppuBoss = ppuBoss;
        this.ppuPlayer = ppuPlayer;

        this.anchoW = texDormido.getWidth() / ppuBoss;
        this.altoW = texDormido.getHeight() / ppuBoss;

        this.despertarDistW = despertarDistW;

        this.vidaMax = vidaMax;
        this.vida = vidaMax;

        this.cadenciaDisparoMs = cadenciaDisparoMs;
        this.cooldownSaltoMs = cooldownSaltoMs;

        this.saltoDistW = saltoDistW;
        this.saltoDuracionS = saltoDuracionS;
        this.saltoAlturaW = saltoAlturaW;
    }

    public void update(float dt, Jugador jugador, int pantallaAnchoIgnored, int sueloYIgnored, List<ProyectilMeteoro> meteoroOut) {
        if (!vivo) return;

        if (estado == Estado.DORMIDO) {
            if (distanciaAlJugador(jugador) <= despertarDistW) {
                estado = Estado.DESPIERTO;
                long now = System.currentTimeMillis();
                ultimoDisparoMs = now;
                ultimoSaltoMs = now;
            }
            return;
        }

        if (estado == Estado.SALTANDO) {
            actualizarSalto(dt);
            return;
        }

        long now = System.currentTimeMillis();

        if (now - ultimoSaltoMs >= cooldownSaltoMs) {
            iniciarSaltoHaciaJugador(jugador);
            ultimoSaltoMs = now;
        }

        if (now - ultimoDisparoMs >= cadenciaDisparoMs) {
            dispararTresMeteoros(meteoroOut);
            ultimoDisparoMs = now;
        }
    }

    private float distanciaAlJugador(Jugador jugador) {
        float bossCx = x + anchoW * 0.5f;

        float jugadorW = jugador.getWidth(ppuPlayer);
        float jugadorCx = jugador.getX() + jugadorW * 0.5f;

        return Math.abs(bossCx - jugadorCx);
    }

    private void iniciarSaltoHaciaJugador(Jugador jugador) {
        estado = Estado.SALTANDO;
        saltoT = 0f;

        saltoX0 = x;

        float bossCx = x + anchoW * 0.5f;

        float jugadorW = jugador.getWidth(ppuPlayer);
        float jugadorCx = jugador.getX() + jugadorW * 0.5f;

        float dir = (jugadorCx >= bossCx) ? 1f : -1f;

        saltoX1 = x + dir * saltoDistW;
    }

    private void actualizarSalto(float dt) {
        saltoT += dt;
        float p = saltoT / saltoDuracionS;
        if (p >= 1f) p = 1f;

        x = lerp(saltoX0, saltoX1, p);

        float arco = (float) Math.sin(Math.PI * p);
        y = baseY + (saltoAlturaW * arco);

        if (p >= 1f) {
            y = baseY;
            estado = Estado.DESPIERTO;
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void dispararTresMeteoros(List<ProyectilMeteoro> meteoroOut) {
        // Boca en mundo (aprox)
        float bocaX = x + anchoW * 0.55f;
        float bocaY = y + altoW * 0.55f;

        // 3 zonas en mundo, separadas
        float lane1 = 2.5f;
        float lane2 = 6.5f;
        float lane3 = 10.5f;

        float ySuelo = baseY;

        float tVuelo = 0.90f;
        float g = 18.0f;

        meteoroOut.add(ProyectilMeteoro.crear(bocaX, bocaY, lane1, ySuelo, tVuelo, g));
        meteoroOut.add(ProyectilMeteoro.crear(bocaX, bocaY, lane2, ySuelo, tVuelo, g));
        meteoroOut.add(ProyectilMeteoro.crear(bocaX, bocaY, lane3, ySuelo, tVuelo, g));
    }

    public void draw(SpriteBatch batch) {
        if (!vivo) return;

        Texture tex = (estado == Estado.DORMIDO) ? texDormido : texDespierto;
        batch.draw(tex, x, y, anchoW, altoW);
    }

    public boolean isDormido() { return estado == Estado.DORMIDO; }
    public boolean isVivo() { return vivo; }

    public void recibirDanio(int danio) {
        if (!vivo) return;
        vida -= danio;
        if (vida <= 0) {
            vida = 0;
            vivo = false;
        }
    }

    public int getVida() { return vida; }
    public int getVidaMax() { return vidaMax; }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getAncho() { return anchoW; }
    public float getAlto() { return altoW; }
}
