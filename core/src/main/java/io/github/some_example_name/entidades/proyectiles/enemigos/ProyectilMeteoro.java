package io.github.some_example_name.entidades.proyectiles.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ProyectilMeteoro {

    // Textura compartida por todas las instancias del proyectil.
    // Se define como estática para evitar duplicar recursos y porque todas las balas comparten sprite.
    private static Texture textura;

    // PPU (pixels per unit) usado para convertir dimensiones en píxeles a unidades de mundo.
    // Debe coincidir con el escalado general del juego para que el tamaño del sprite sea consistente.
    private static float PPU = 64f;

    // Tamaño del proyectil en coordenadas de mundo.
    // Se recalcula automáticamente a partir de la textura y el PPU cuando se asigna una textura o se cambia el PPU.
    private static float anchoWorld = 0.6f;
    private static float altoWorld = 0.6f;

    // Posición actual del proyectil en el mundo (centro del sprite).
    private float x;
    private float y;

    // Velocidad actual del proyectil en el mundo.
    private float vx;
    private float vy;

    // Aceleración vertical (gravedad) aplicada al proyectil.
    // Se almacena por instancia para permitir diferentes configuraciones por proyectil si se desea.
    private final float g;

    // Nivel del suelo objetivo para este proyectil.
    // Cuando y cae por debajo de este valor, se considera que impacta y se detiene.
    private final float sueloY;

    // Flag de vida del proyectil:
    // - true: se actualiza y se dibuja
    // - false: se considera finalizado (no se actualiza ni se dibuja)
    private boolean vivo = true;

    /*
     * Constructor privado:
     * Se fuerza el uso del método factoría crear(), que calcula vx/vy para llegar a un objetivo en un tiempo dado.
     */
    private ProyectilMeteoro(float x, float y, float vx, float vy, float g, float sueloY) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.g = g;
        this.sueloY = sueloY;
    }

    /*
     * Asigna la textura compartida del proyectil.
     * Además recalcula el tamaño en mundo (anchoWorld/altoWorld) usando el PPU actual.
     */
    public static void setTextura(Texture tex) {
        textura = tex;

        // Recalcular tamaño en mundo al asignar textura para mantener escalado consistente.
        if (textura != null) {
            anchoWorld = textura.getWidth() / PPU;
            altoWorld = textura.getHeight() / PPU;
        }
    }

    /*
     * Permite ajustar el PPU global para este proyectil.
     * Útil si el juego cambia de escala o si se quiere ajustar tamaño sin modificar la textura.
     */
    public static void setPPU(float ppu) {
        // Protección ante valores inválidos o cercanos a cero.
        if (ppu <= 0.0001f) return;

        PPU = ppu;

        // Si ya hay textura asignada, recalcula inmediatamente el tamaño en mundo.
        if (textura != null) {
            anchoWorld = textura.getWidth() / PPU;
            altoWorld = textura.getHeight() / PPU;
        }
    }

    /*
     * Crea un proyectil que sale desde (x0,y0) y llega a (xObjetivo,yObjetivo) en un tiempo tVuelo,
     * bajo aceleración constante g (gravedad).
     *
     * Fórmulas usadas:
     * - vx = (dx / t)
     * - vy = (dy - 0.5*g*t^2) / t
     *
     * Nota: aquí sueloY se fija a yObjetivo, interpretando el objetivo como altura de impacto/suelo.
     */
    public static ProyectilMeteoro crear(float x0, float y0, float xObjetivo, float yObjetivo, float tVuelo, float g) {
        float vx = (xObjetivo - x0) / tVuelo;
        float vy = (yObjetivo - y0 - 0.5f * g * tVuelo * tVuelo) / tVuelo;
        return new ProyectilMeteoro(x0, y0, vx, vy, g, yObjetivo);
    }

    /*
     * Actualiza la simulación del proyectil:
     * - Integración simple (Euler):
     *   x += vx*dt
     *   y += vy*dt
     *   vy += g*dt
     * - Si cae por debajo de sueloY, se “clampa” al suelo y se marca como no vivo.
     */
    public void update(float dt) {
        if (!vivo) return;

        x += vx * dt;
        y += vy * dt;
        vy += g * dt;

        // Condición de impacto con el suelo: al tocar o bajar de sueloY se detiene.
        if (y <= sueloY) {
            y = sueloY;
            vivo = false;
        }
    }

    /*
     * Dibuja el proyectil:
     * - Solo si está vivo
     * - Solo si existe textura asignada
     *
     * Se dibuja centrado en (x,y), por eso se resta la mitad del ancho/alto.
     */
    public void draw(SpriteBatch batch) {
        if (!vivo) return;

        if (textura != null) {
            batch.draw(
                textura,
                x - anchoWorld * 0.5f,
                y - altoWorld * 0.5f,
                anchoWorld,
                altoWorld
            );
        }
    }

    /*
     * Indica si el proyectil sigue activo.
     * Cuando deja de estar vivo, no se actualiza ni se renderiza y se puede retirar del gestor.
     */
    public boolean isVivo() {
        return vivo;
    }

    // Accesores de posición (útiles para colisiones, efectos, etc.).
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    // Accesores del tamaño en mundo (dimensiones compartidas, dependen de textura/PPU).
    public float getAncho() {
        return anchoWorld;
    }

    public float getAlto() {
        return altoWorld;
    }
}
