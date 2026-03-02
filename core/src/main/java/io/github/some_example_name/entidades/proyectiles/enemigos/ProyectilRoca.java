package io.github.some_example_name.entidades.proyectiles.enemigos;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class ProyectilRoca {

    // Región de textura utilizada para renderizar el proyectil.
    private final TextureRegion region;

    // Posición del proyectil en coordenadas de mundo.
    private float x;
    private float y;

    // Velocidad del proyectil en coordenadas de mundo.
    private float vx;
    private float vy;

    // Aceleración vertical aplicada en cada actualización (gravedad).
    private float gravity;

    // Dimensiones del proyectil en unidades de mundo.
    private float w;
    private float h;

    // Daño que inflige el proyectil al colisionar.
    private final int damage;

    // Hitbox utilizada para detección de colisiones.
    private final Rectangle hitbox = new Rectangle();

    // Indica si el proyectil debe eliminarse (retirarse de la simulación/render).
    private boolean eliminar = false;

    public ProyectilRoca(TextureRegion region,
                         float x, float y,
                         float vx, float vy,
                         float gravity,
                         float w, float h,
                         int damage) {
        // Recursos gráficos asociados al proyectil.
        this.region = region;

        // Estado inicial de posición.
        this.x = x;
        this.y = y;

        // Estado inicial de velocidad.
        this.vx = vx;
        this.vy = vy;

        // Parámetro físico de gravedad.
        this.gravity = gravity;

        // Tamaño inicial del proyectil.
        this.w = w;
        this.h = h;

        // Daño asociado a este proyectil.
        this.damage = damage;

        // Inicialización de la hitbox con el estado inicial.
        hitbox.set(x, y, w, h);
    }

    public void update(float delta) {
        // Si está marcado para eliminar, no actualiza física ni hitbox.
        if (eliminar) return;

        // Integración simple: actualiza velocidad vertical por gravedad.
        vy += gravity * delta;

        // Integración de posición.
        x += vx * delta;
        y += vy * delta;

        // Sincronización de hitbox con la posición.
        hitbox.x = x;
        hitbox.y = y;
    }

    public void draw(SpriteBatch batch) {
        // No dibuja si está marcado para eliminar.
        if (eliminar) return;
        batch.draw(region, x, y, w, h);
    }

    public boolean isOutOfRange(float leftX, float rightX) {
        // Comprueba si el proyectil queda totalmente fuera del rango horizontal visible/permitido.
        return (x + w) < leftX || x > rightX;
    }

    public boolean isBelow(float sueloY) {
        // Comprueba si el proyectil ha quedado por debajo del suelo (incluyendo su alto).
        return (y + h) < sueloY;
    }

    public Rectangle getHitbox() {
        // Devuelve la referencia a la hitbox para colisiones externas.
        return hitbox;
    }

    public int getDamage() {
        // Devuelve el daño que aplica este proyectil.
        return damage;
    }

    public void marcarEliminar() {
        // Marca el proyectil para ser eliminado por el gestor que lo contenga.
        eliminar = true;
    }

    public boolean isEliminar() {
        // Indica si el proyectil está marcado para eliminación.
        return eliminar;
    }

    // Accesores de estado de posición.
    public float getX() { return x; }
    public float getY() { return y; }

    // Accesores de velocidad actual.
    public float getVx() { return vx; }
    public float getVy() { return vy; }

    public void setVel(float vx, float vy) {
        // Ajusta la velocidad del proyectil.
        this.vx = vx;
        this.vy = vy;
    }

    public void setGravity(float gravity) {
        // Ajusta la gravedad aplicada al proyectil.
        this.gravity = gravity;
    }

    public void setSize(float w, float h) {
        // Ajusta el tamaño del proyectil y sincroniza la hitbox con las nuevas dimensiones.
        this.w = w;
        this.h = h;
        hitbox.width = w;
        hitbox.height = h;
    }
}
