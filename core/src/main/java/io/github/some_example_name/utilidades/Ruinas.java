package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class Ruinas implements Disposable {

    private final Texture tex;

    private float worldX;
    private float worldY;

    private float parallaxFactor = 0.70f;

    public Ruinas(String path, float worldX, float worldY) {
        tex = new Texture(path);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tex.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

        this.worldX = worldX;
        this.worldY = worldY;
    }

    public void render(SpriteBatch batch, float cameraLeftX) {
        float drawX = worldX - cameraLeftX * parallaxFactor;
        batch.draw(tex, drawX, worldY);
    }

    public void setParallaxFactor(float factor) { this.parallaxFactor = factor; }

    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }
    public void setWorldX(float x) { this.worldX = x; }
    public void setWorldY(float y) { this.worldY = y; }

    @Override
    public void dispose() {
        tex.dispose();
    }
}
