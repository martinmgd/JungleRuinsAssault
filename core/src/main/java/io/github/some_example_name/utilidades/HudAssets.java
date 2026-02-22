package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class HudAssets implements Disposable {

    private static final String HEART_FULL_PATH = "sprites/hud/corazon_lleno.png";
    private static final String HEART_EMPTY_PATH = "sprites/hud/corazon_vacio.png";

    private Texture texFull;
    private Texture texEmpty;

    public TextureRegion heartFull;
    public TextureRegion heartEmpty;

    public HudAssets() {
        texFull = loadTexMust(HEART_FULL_PATH);
        texEmpty = loadTexMust(HEART_EMPTY_PATH);

        heartFull = new TextureRegion(texFull);
        heartEmpty = new TextureRegion(texEmpty);
    }

    private Texture loadTexMust(String path) {
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            throw new RuntimeException("NO EXISTE asset HUD: " + path);
        }
        Texture t = new Texture(fh);
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return t;
    }

    @Override
    public void dispose() {
        if (texFull != null) texFull.dispose();
        if (texEmpty != null) texEmpty.dispose();
    }
}
