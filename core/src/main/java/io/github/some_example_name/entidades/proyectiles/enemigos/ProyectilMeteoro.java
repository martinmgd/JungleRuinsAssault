package io.github.some_example_name.entidades.proyectiles.enemigos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ProyectilMeteoro {

    private static Texture textura;

    // PPU para convertir px -> mundo (igual que tu juego)
    private static float PPU = 64f;

    // Tamaño en mundo
    private static float anchoWorld = 0.6f;
    private static float altoWorld = 0.6f;

    private float x;
    private float y;

    private float vx;
    private float vy;

    private final float g;
    private final float sueloY;

    private boolean vivo = true;

    private ProyectilMeteoro(float x, float y, float vx, float vy, float g, float sueloY) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.g = g;
        this.sueloY = sueloY;
    }

    public static void setTextura(Texture tex) {
        textura = tex;

        // recalcular tamaño en mundo al asignar textura
        if (textura != null) {
            anchoWorld = textura.getWidth() / PPU;
            altoWorld = textura.getHeight() / PPU;
        }
    }

    // si quieres cambiar el tamaño global sin tocar la textura
    public static void setPPU(float ppu) {
        if (ppu <= 0.0001f) return;
        PPU = ppu;
        if (textura != null) {
            anchoWorld = textura.getWidth() / PPU;
            altoWorld = textura.getHeight() / PPU;
        }
    }

    public static ProyectilMeteoro crear(float x0, float y0, float xObjetivo, float yObjetivo, float tVuelo, float g) {
        float vx = (xObjetivo - x0) / tVuelo;
        float vy = (yObjetivo - y0 - 0.5f * g * tVuelo * tVuelo) / tVuelo;
        return new ProyectilMeteoro(x0, y0, vx, vy, g, yObjetivo);
    }

    public void update(float dt) {
        if (!vivo) return;

        x += vx * dt;
        y += vy * dt;
        vy += g * dt;

        if (y <= sueloY) {
            y = sueloY;
            vivo = false;
        }
    }

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

    public boolean isVivo() {
        return vivo;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getAncho() {
        return anchoWorld;
    }

    public float getAlto() {
        return altoWorld;
    }
}
