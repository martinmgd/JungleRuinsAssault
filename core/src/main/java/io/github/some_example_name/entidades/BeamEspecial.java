package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BeamEspecial {

    private final TextureRegion beamRegion;

    private float x; // origen (muzzle)
    private float y; // centro vertical del beam
    private boolean derecha;

    private float h; // grosor en unidades de mundo
    private float tiempo = 0f;
    private float duracion;

    private int damage;

    public BeamEspecial(TextureRegion beamRegion, float x, float y, boolean derecha, float h, float duracion, int damage) {
        this.beamRegion = beamRegion;
        this.x = x;
        this.y = y;
        this.derecha = derecha;
        this.h = h;
        this.duracion = duracion;
        this.damage = damage;
    }

    public void update(float delta) {
        tiempo += delta;
    }

    public boolean finished() {
        return tiempo >= duracion;
    }

    /**
     * Dibuja el beam desde el muzzle hasta el borde de pantalla (según cámara).
     */
    public void draw(SpriteBatch batch, float cameraLeftX, float viewW) {
        float screenLeft = cameraLeftX;
        float screenRight = cameraLeftX + viewW;

        float startX = x;
        float endX = derecha ? screenRight : screenLeft;

        float width = endX - startX;
        float drawY = y - h * 0.5f;

        // Si dispara a la izquierda, width será negativo: batch.draw lo acepta.
        batch.draw(beamRegion, startX, drawY, width, h);
    }

    public int getDamage() { return damage; }
}
