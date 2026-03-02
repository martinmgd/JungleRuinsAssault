package io.github.some_example_name.entidades.proyectiles.jugador;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Proyectil {

    // Animación asociada al proyectil (por ejemplo, sprite animado de bala/energía).
    // Se asume que es una animación cíclica (loop) durante su vida útil.
    private final Animation<TextureRegion> anim;

    // Posición actual del proyectil en unidades de mundo.
    private float x, y;

    // Velocidad horizontal del proyectil (unidades de mundo por segundo).
    // La dirección se controla adicionalmente con "haciaDerecha" para el render (flip), aunque vx ya define el signo.
    private float vx;

    // Tiempo acumulado para muestrear la animación (state time típico de libGDX).
    private float stateTime = 0f;

    // Indica hacia dónde se dibuja el proyectil:
    // - true: normal (sin flip)
    // - false: flip horizontal (dibujado con ancho negativo)
    private final boolean haciaDerecha;

    // Dimensiones del proyectil en unidades de mundo.
    private final float w;
    private final float h;

    // Daño aplicado por este proyectil al impactar.
    private final int damage;

    // Flag de eliminación lógica: cuando es true, el gestor externo suele retirar el proyectil de la lista.
    private boolean eliminar = false;

    // Hitbox reutilizable para colisiones; se mantiene sincronizado con x/y en update().
    private final Rectangle hitbox = new Rectangle();

    public Proyectil(Animation<TextureRegion> anim,
                     float x, float y, float vx,
                     boolean haciaDerecha,
                     float w, float h,
                     int damage) {
        // Inyección de animación y parámetros iniciales de estado.
        this.anim = anim;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.haciaDerecha = haciaDerecha;
        this.w = w;
        this.h = h;
        this.damage = damage;

        // Inicializa el hitbox en la posición y tamaño iniciales del proyectil.
        hitbox.set(x, y, w, h);
    }

    public void update(float delta) {
        // Avanza el tiempo de animación.
        stateTime += delta;

        // Integra movimiento horizontal.
        x += vx * delta;

        // Sincroniza hitbox con la posición actual.
        // Nota: y no cambia en este proyectil, pero se reasigna para mantener coherencia si se modifica en el futuro.
        hitbox.x = x;
        hitbox.y = y;
    }

    public void draw(SpriteBatch batch) {
        // Obtiene el frame actual de la animación en modo loop.
        TextureRegion frame = anim.getKeyFrame(stateTime, true);

        // Dibuja el proyectil en la orientación correspondiente.
        // Para "izquierda", se usa ancho negativo y se corrige el origen sumando w.
        if (haciaDerecha) {
            batch.draw(frame, x, y, w, h);
        } else {
            batch.draw(frame, x + w, y, -w, h);
        }
    }

    public boolean isOutOfRange(float leftX, float rightX) {
        // Devuelve true si el proyectil está completamente fuera de los límites horizontales indicados.
        // Útil para eliminación por límites de cámara o área de juego.
        return (x + w) < leftX || x > rightX;
    }

    public int getDamage() { return damage; }

    public Rectangle getHitbox() {
        // Devuelve el hitbox actual (referencia directa, actualizada internamente).
        return hitbox;
    }

    public void marcarEliminar() {
        // Marca el proyectil como eliminable para que el gestor lo retire del mundo.
        eliminar = true;
    }

    public boolean isEliminar() {
        // Indica si el proyectil ha sido marcado para eliminación.
        return eliminar;
    }

    public float getVx() {
        // Devuelve la velocidad horizontal actual (por si se necesita para lógica externa o depuración).
        return vx;
    }
}
