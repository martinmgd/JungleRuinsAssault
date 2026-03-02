package io.github.some_example_name.entidades.enemigos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.some_example_name.entidades.jugador.Jugador;
import io.github.some_example_name.entidades.proyectiles.enemigos.ProyectilMeteoro;

import java.util.List;

public class Jefe {

    // Estados de comportamiento del jefe: inactivo, activo o ejecutando salto.
    public enum Estado {
        DORMIDO,
        DESPIERTO,
        SALTANDO
    }

    // Texturas para representar el estado visual dormido y despierto.
    private final Texture texDormido;
    private final Texture texDespierto;

    // Estado actual de la IA del jefe.
    private Estado estado = Estado.DORMIDO;

    // Posición en coordenadas de mundo.
    private float x;
    private float y;

    // Altura base (suelo) utilizada como referencia para el arco del salto.
    private final float baseY;

    // Tamaño del jefe en unidades de mundo.
    private final float anchoW;
    private final float altoW;

    // Distancia en mundo a partir de la cual el jefe pasa de dormido a despierto.
    private final float despertarDistW;

    // Control de cadencia de disparo mediante timestamps en milisegundos.
    private long ultimoDisparoMs = 0L;
    private final long cadenciaDisparoMs;

    // Control de cooldown de salto mediante timestamps en milisegundos.
    private long ultimoSaltoMs = 0L;
    private final long cooldownSaltoMs;

    // Parámetros del salto: distancia horizontal, duración y altura del arco.
    private final float saltoDistW;
    private final float saltoDuracionS;
    private final float saltoAlturaW;

    // Progreso del salto y posiciones inicial/final del desplazamiento horizontal.
    private float saltoT = 0f;
    private float saltoX0;
    private float saltoX1;

    // Controla que la vibración asociada al aterrizaje se ejecute una sola vez por salto.
    private boolean vibradoEnEsteSalto = false;

    // Estado de vida del jefe y valores de vida actual/máxima.
    private boolean vivo = true;
    private int vida;
    private final int vidaMax;

    // Factores de conversión píxeles-a-mundo para jefe y jugador.
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

        // Texturas del jefe según estado visual.
        this.texDormido = texDormido;
        this.texDespierto = texDespierto;

        // Posición inicial y referencia de suelo.
        this.x = xInicial;
        this.y = yInicial;
        this.baseY = yInicial;

        // PPU para convertir tamaños del sprite a unidades de mundo.
        this.ppuBoss = ppuBoss;
        this.ppuPlayer = ppuPlayer;

        // Tamaño en mundo derivado del tamaño de la textura y el PPU del jefe.
        this.anchoW = texDormido.getWidth() / ppuBoss;
        this.altoW = texDormido.getHeight() / ppuBoss;

        // Umbral de activación (despertar) respecto al jugador.
        this.despertarDistW = despertarDistW;

        // Inicialización de vida.
        this.vidaMax = vidaMax;
        this.vida = vidaMax;

        // Configuración de cadencias de acciones (disparo y salto).
        this.cadenciaDisparoMs = cadenciaDisparoMs;
        this.cooldownSaltoMs = cooldownSaltoMs;

        // Configuración del salto.
        this.saltoDistW = saltoDistW;
        this.saltoDuracionS = saltoDuracionS;
        this.saltoAlturaW = saltoAlturaW;
    }

    // Actualiza el comportamiento del jefe según su estado y el tiempo transcurrido.
    // Los parámetros marcados como ignored se mantienen por compatibilidad con la firma original.
    public void update(float dt, Jugador jugador, int pantallaAnchoIgnored, int sueloYIgnored, List<ProyectilMeteoro> meteoroOut) {
        if (!vivo) return;

        // Mientras está dormido, solo evalúa la distancia al jugador para despertar.
        if (estado == Estado.DORMIDO) {
            if (distanciaAlJugador(jugador) <= despertarDistW) {
                estado = Estado.DESPIERTO;
                long now = System.currentTimeMillis();
                ultimoDisparoMs = now;
                ultimoSaltoMs = now;
            }
            return;
        }

        // Mientras salta, actualiza la trayectoria del salto y no ejecuta otras acciones.
        if (estado == Estado.SALTANDO) {
            actualizarSalto(dt);
            return;
        }

        long now = System.currentTimeMillis();

        // Lógica de salto por cooldown.
        if (now - ultimoSaltoMs >= cooldownSaltoMs) {
            iniciarSaltoHaciaJugador(jugador);
            ultimoSaltoMs = now;
        }

        // Lógica de disparo por cadencia.
        if (now - ultimoDisparoMs >= cadenciaDisparoMs) {
            dispararTresMeteoros(meteoroOut);
            ultimoDisparoMs = now;
        }
    }

    // Calcula la distancia horizontal entre el centro del jefe y el centro del jugador.
    private float distanciaAlJugador(Jugador jugador) {
        float bossCx = x + anchoW * 0.5f;

        float jugadorW = jugador.getWidth(ppuPlayer);
        float jugadorCx = jugador.getX() + jugadorW * 0.5f;

        return Math.abs(bossCx - jugadorCx);
    }

    // Inicializa un salto hacia la posición del jugador calculando el destino horizontal.
    private void iniciarSaltoHaciaJugador(Jugador jugador) {
        estado = Estado.SALTANDO;
        saltoT = 0f;

        // Reinicio del control de vibración asociado al aterrizaje.
        vibradoEnEsteSalto = false;

        saltoX0 = x;

        float bossCx = x + anchoW * 0.5f;

        float jugadorW = jugador.getWidth(ppuPlayer);
        float jugadorCx = jugador.getX() + jugadorW * 0.5f;

        float dir = (jugadorCx >= bossCx) ? 1f : -1f;

        saltoX1 = x + dir * saltoDistW;
    }

    // Actualiza la trayectoria del salto usando interpolación lineal en X y un arco sinusoidal en Y.
    private void actualizarSalto(float dt) {
        saltoT += dt;
        float p = saltoT / saltoDuracionS;
        if (p >= 1f) p = 1f;

        x = lerp(saltoX0, saltoX1, p);

        float arco = (float) Math.sin(Math.PI * p);
        y = baseY + (saltoAlturaW * arco);

        // Al completar el salto, ajusta la posición al suelo y retorna al estado activo.
        if (p >= 1f) {
            y = baseY;

            // Vibración asociada al aterrizaje, ejecutada una vez por salto si el periférico está disponible.
            if (!vibradoEnEsteSalto
                && Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator)) {
                Gdx.input.vibrate(180);
                vibradoEnEsteSalto = true;
            }

            estado = Estado.DESPIERTO;
        }
    }

    // Interpolación lineal básica.
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Genera tres proyectiles tipo meteoro con destinos horizontales prefijados en el mundo.
    private void dispararTresMeteoros(List<ProyectilMeteoro> meteoroOut) {
        // Punto de salida aproximado relativo al sprite del jefe.
        float bocaX = x + anchoW * 0.55f;
        float bocaY = y + altoW * 0.55f;

        // Carriles horizontales objetivos en coordenadas de mundo.
        float lane1 = 2.5f;
        float lane2 = 6.5f;
        float lane3 = 10.5f;

        // Suelo objetivo para el aterrizaje del meteoro.
        float ySuelo = baseY;

        // Parámetros de vuelo del meteoro.
        float tVuelo = 0.90f;
        float g = 18.0f;

        meteoroOut.add(ProyectilMeteoro.crear(bocaX, bocaY, lane1, ySuelo, tVuelo, g));
        meteoroOut.add(ProyectilMeteoro.crear(bocaX, bocaY, lane2, ySuelo, tVuelo, g));
        meteoroOut.add(ProyectilMeteoro.crear(bocaX, bocaY, lane3, ySuelo, tVuelo, g));
    }

    // Dibuja el jefe en pantalla según su estado visual actual.
    public void draw(SpriteBatch batch) {
        if (!vivo) return;

        Texture tex = (estado == Estado.DORMIDO) ? texDormido : texDespierto;
        batch.draw(tex, x, y, anchoW, altoW);
    }

    // Indica si el jefe permanece en estado dormido.
    public boolean isDormido() { return estado == Estado.DORMIDO; }

    // Indica si el jefe está vivo.
    public boolean isVivo() { return vivo; }

    // Aplica daño al jefe y actualiza su estado de vida.
    public void recibirDanio(int danio) {
        if (!vivo) return;
        vida -= danio;
        if (vida <= 0) {
            vida = 0;
            vivo = false;
        }
    }

    // Indica si el jefe se encuentra actualmente ejecutando un salto.
    public boolean isSaltando() {
        return estado == Estado.SALTANDO;
    }

    // Accesores de vida actual y máxima.
    public int getVida() { return vida; }
    public int getVidaMax() { return vidaMax; }

    // Accesores de posición y dimensiones en mundo.
    public float getX() { return x; }
    public float getY() { return y; }
    public float getAncho() { return anchoW; }
    public float getAlto() { return altoW; }
}
