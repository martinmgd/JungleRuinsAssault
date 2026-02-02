package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class PajaroAssets {

    private Texture ataqueTex;
    private Texture muertoTex;

    public final TextureRegion ataque;
    public final TextureRegion muerto;

    public PajaroAssets() {
        ataqueTex = new Texture("sprites/enemigos/pajaro/pajaro_attack.png");
        muertoTex = new Texture("sprites/enemigos/pajaro/pajaro_dead.png");

        ataqueTex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        muertoTex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        ataque = new TextureRegion(ataqueTex);
        muerto = new TextureRegion(muertoTex);
    }

    public void dispose() {
        ataqueTex.dispose();
        muertoTex.dispose();
    }
}
