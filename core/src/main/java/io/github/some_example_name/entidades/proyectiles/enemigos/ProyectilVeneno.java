package io.github.some_example_name.entidades.proyectiles.enemigos;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class ProyectilVeneno {

    // Región de textura que representa visualmente el proyectil (sprite ya recortado si procede).
    private final TextureRegion region;

    // Posición del proyectil en unidades de mundo.
    private float x;
    private float y;

    // Velocidad del proyectil en los ejes X e Y (unidades de mundo por segundo).
    private float vx;
    private float vy;

    // Aceleración vertical aplicada cada frame (gravedad). Puede ser 0 para movimiento recto.
    private float gravity;

    // Dimensiones del proyectil en unidades de mundo.
    private float w;
    private float h;

    // Daño que aplica al impactar con el jugador (o entidad objetivo).
    private final int damage;

    // Hitbox reutilizable para colisiones; se actualiza con la posición actual del proyectil.
    private final Rectangle hitbox = new Rectangle();

    // Flag de eliminación lógica: cuando es true, no se actualiza ni se dibuja.
    private boolean eliminar = false;

    // Si sueloYForKill es NaN, no auto-elimina por suelo
    // Permite autodestruir el proyectil al caer por debajo de un nivel de suelo sin depender de gestores externos.
    private final float sueloYForKill;

    // Constructor antiguo (compatibilidad total): veneno recto como antes
    // Mantiene firma y comportamiento original: solo velocidad horizontal, sin gravedad ni velocidad vertical.
    public ProyectilVeneno(TextureRegion region,
                           float x, float y,
                           float vx,
                           float w, float h,
                           int damage) {
        this(region, x, y, vx, 0f, 0f, w, h, damage, Float.NaN);
    }

    // Constructor nuevo: veneno en parábola (tipo roca)
    // Permite movimiento balístico configurando vy (velocidad vertical inicial) y gravity (aceleración vertical).
    // sueloYForKill controla el autodescarte al caer por debajo del suelo.
    public ProyectilVeneno(TextureRegion region,
                           float x, float y,
                           float vx, float vy,
                           float gravity,
                           float w, float h,
                           int damage,
                           float sueloYForKill) {
        this.region = region;

        this.x = x;
        this.y = y;

        this.vx = vx;
        this.vy = vy;
        this.gravity = gravity;

        this.w = w;
        this.h = h;

        this.damage = damage;
        this.sueloYForKill = sueloYForKill;

        // Inicializa hitbox en la posición y tamaño actuales.
        hitbox.set(x, y, w, h);
    }

    public void update(float delta) {
        // Si está marcado para eliminar, no se actualiza para evitar trabajo innecesario.
        if (eliminar) return;

        // Física parabólica (si gravity != 0 o vy != 0)
        // Se integra la velocidad vertical con gravedad y luego se integra la posición con velocidades.
        vy += gravity * delta;

        x += vx * delta;
        y += vy * delta;

        // Mantiene el hitbox sincronizado con la posición del proyectil.
        hitbox.x = x;
        hitbox.y = y;

        // Auto-eliminar al caer bajo el suelo (sin tocar GestorEnemigos)
        // Esta condición es opcional: si sueloYForKill es NaN, no se aplica.
        if (!Float.isNaN(sueloYForKill)) {
            // Se usa (y + h) para considerar el borde superior/inferior según convención del sprite.
            // Aquí se elimina cuando el proyectil queda completamente por debajo del suelo.
            if ((y + h) < sueloYForKill) {
                eliminar = true;
            }
        }
    }

    public void draw(SpriteBatch batch) {
        // No se dibuja si está marcado para eliminar.
        if (eliminar) return;

        // Dibuja el sprite en la posición actual y con el tamaño actual.
        batch.draw(region, x, y, w, h);
    }

    public boolean isOutOfRange(float leftX, float rightX) {
        // Comprueba si el proyectil ha quedado completamente fuera del rango horizontal visible/activo.
        // Útil para eliminar proyectiles que salen de cámara o de límites de juego.
        return (x + w) < leftX || x > rightX;
    }

    public Rectangle getHitbox() {
        // Devuelve referencia directa al hitbox (se actualiza internamente en update()).
        return hitbox;
    }

    public int getDamage() {
        // Devuelve el daño configurado para este proyectil.
        return damage;
    }

    public void marcarEliminar() {
        // Marca el proyectil para su eliminación lógica (no update/draw).
        eliminar = true;
    }

    public boolean isEliminar() {
        // Indica si el proyectil debe considerarse inactivo.
        return eliminar;
    }

    // Opcional por si ajustas luego
    // Accesores de depuración o para lógica externa (por ejemplo, ajustes dinámicos o IA).
    public float getX() { return x; }
    public float getY() { return y; }
    public float getVx() { return vx; }
    public float getVy() { return vy; }

    public void setVel(float vx, float vy) {
        // Permite ajustar velocidades en caliente (por ejemplo, cambios de patrón o dificultades).
        this.vx = vx;
        this.vy = vy;
    }

    public void setGravity(float gravity) {
        // Permite ajustar la gravedad aplicada (por ejemplo, trayectorias más/menos pronunciadas).
        this.gravity = gravity;
    }

    public void setSize(float w, float h) {
        // Permite ajustar tamaño en caliente y sincroniza hitbox con nuevas dimensiones.
        this.w = w;
        this.h = h;
        hitbox.width = w;
        hitbox.height = h;
    }
}
