package io.github.some_example_name.entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class PlayerAnimations {

    // Ajustar si los frames del spritesheet final no coinciden con estas dimensiones
    public static final int FRAME_W = 128;
    public static final int FRAME_H = 160;

    private static final String IDLE_SHEET = "sprites/player/idle_sheet.png";
    private static final String WALK_SHEET = "sprites/player/walk_sheet.png";

    // Tiempo por frame (en segundos)
    private static final float IDLE_FRAME_TIME = 0.25f;
    private static final float WALK_FRAME_TIME = 0.08f;

    private final Texture idleTex;
    private final Texture walkTex;

    public final Animation<TextureRegion> idle;
    public final Animation<TextureRegion> walk;

    public PlayerAnimations() {
        idleTex = loadTexMust(IDLE_SHEET);
        walkTex = loadTexMust(WALK_SHEET);

        idle = buildRowAnimation(idleTex, IDLE_FRAME_TIME, true);
        walk = buildRowAnimation(walkTex, WALK_FRAME_TIME, true);
    }

    private Texture loadTexMust(String path) {
        FileHandle fh = Gdx.files.internal(path);
        Gdx.app.log("ASSETS", "Buscando: " + path + " -> exists=" + fh.exists());

        if (!fh.exists()) {
            throw new RuntimeException("NO EXISTE asset: " + path +
                " (revisa la carpeta assets/ y la ruta exacta)");
        }

        Texture t = new Texture(fh);
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        Gdx.app.log("ASSETS", "Cargado " + path + " size=" + t.getWidth() + "x" + t.getHeight());

        // Validación: el sheet debe ser múltiplo de FRAME_W/H
        if (t.getWidth() % FRAME_W != 0 || t.getHeight() % FRAME_H != 0) {
            throw new RuntimeException(
                "El sheet " + path + " (" + t.getWidth() + "x" + t.getHeight() + ") no es múltiplo de " +
                    FRAME_W + "x" + FRAME_H + ". Ajusta FRAME_W/H o reexporta el sheet."
            );
        }

        return t;
    }

    private Animation<TextureRegion> buildRowAnimation(Texture tex, float frameTime, boolean loop) {
        int cols = tex.getWidth() / FRAME_W;
        TextureRegion[][] grid = TextureRegion.split(tex, FRAME_W, FRAME_H);

        Array<TextureRegion> frames = new Array<>(cols);
        for (int c = 0; c < cols; c++) {
            frames.add(grid[0][c]);
        }

        Animation<TextureRegion> anim = new Animation<>(frameTime, frames);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);

        Gdx.app.log("ANIM", "Frames=" + frames.size + " (cols=" + cols + ")");
        return anim;
    }

    public void dispose() {
        idleTex.dispose();
        walkTex.dispose();
    }
}
