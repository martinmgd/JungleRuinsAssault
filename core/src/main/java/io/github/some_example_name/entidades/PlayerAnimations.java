package io.github.some_example_name.entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class PlayerAnimations {

    public static final int FRAME_W = 128;
    public static final int FRAME_H = 160;

    private static final String IDLE_SHEET = "sprites/player/idle/idle_sheet.png";
    private static final String WALK_SHEET = "sprites/player/walk/walk_sheet.png";

    private static final String CROUCH_SHEET = "sprites/player/crouch/crouch_sheet.png";

    private static final String JUMP_UP_SHEET   = "sprites/player/jump/jump_sheet_1.png";
    private static final String JUMP_TOP_SHEET  = "sprites/player/jump/jump_sheet_2.png";
    private static final String JUMP_DOWN_SHEET = "sprites/player/jump/jump_sheet_3.png";

    private static final String DEAD_1_SHEET = "sprites/player/dead/dead_sheet_1.png";
    private static final String DEAD_2_SHEET = "sprites/player/dead/dead_sheet_2.png";
    private static final String DEAD_3_SHEET = "sprites/player/dead/dead_sheet_3.png";

    private static final float IDLE_FRAME_TIME = 0.25f;
    private static final float WALK_FRAME_TIME = 0.08f;

    private static final float CROUCH_FRAME_TIME = 0.20f;
    private static final float JUMP_FRAME_TIME = 0.06f;
    private static final float DEAD_FRAME_TIME = 0.07f;

    private final Texture idleTex;
    private final Texture walkTex;

    private final Texture crouchTex;

    private final Texture jumpUpTex;
    private final Texture jumpTopTex;
    private final Texture jumpDownTex;

    private final Texture dead1Tex;
    private final Texture dead2Tex;
    private final Texture dead3Tex;

    public final Animation<TextureRegion> idle;
    public final Animation<TextureRegion> walk;

    public final Animation<TextureRegion> crouch;

    public final Animation<TextureRegion> jumpUp;
    public final Animation<TextureRegion> jumpTop;
    public final Animation<TextureRegion> jumpDown;

    public final Animation<TextureRegion> dead1;
    public final Animation<TextureRegion> dead2;
    public final Animation<TextureRegion> dead3;

    public PlayerAnimations() {
        idleTex = loadTexMust(IDLE_SHEET);
        walkTex = loadTexMust(WALK_SHEET);

        crouchTex = loadTexMust(CROUCH_SHEET);

        jumpUpTex = loadTexMust(JUMP_UP_SHEET);
        jumpTopTex = loadTexMust(JUMP_TOP_SHEET);
        jumpDownTex = loadTexMust(JUMP_DOWN_SHEET);

        dead1Tex = loadTexMust(DEAD_1_SHEET);
        dead2Tex = loadTexMust(DEAD_2_SHEET);
        dead3Tex = loadTexMust(DEAD_3_SHEET);

        idle = buildRowAnimation(idleTex, IDLE_FRAME_TIME, true);
        walk = buildRowAnimation(walkTex, WALK_FRAME_TIME, true);

        crouch = buildRowAnimation(crouchTex, CROUCH_FRAME_TIME, true);

        // El salto se selecciona según la velocidad vertical
        jumpUp = buildRowAnimation(jumpUpTex, JUMP_FRAME_TIME, true);
        jumpTop = buildRowAnimation(jumpTopTex, JUMP_FRAME_TIME, true);
        jumpDown = buildRowAnimation(jumpDownTex, JUMP_FRAME_TIME, true);

        // Muerte preparada (se decidirá el flujo exacto más adelante)
        dead1 = buildRowAnimation(dead1Tex, DEAD_FRAME_TIME, true);
        dead2 = buildRowAnimation(dead2Tex, DEAD_FRAME_TIME, true);
        dead3 = buildRowAnimation(dead3Tex, DEAD_FRAME_TIME, true);
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
        for (int c = 0; c < cols; c++) frames.add(grid[0][c]);

        Animation<TextureRegion> anim = new Animation<>(frameTime, frames);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);

        Gdx.app.log("ANIM", "Frames=" + frames.size + " (cols=" + cols + ")");
        return anim;
    }

    public void dispose() {
        idleTex.dispose();
        walkTex.dispose();

        crouchTex.dispose();

        jumpUpTex.dispose();
        jumpTopTex.dispose();
        jumpDownTex.dispose();

        dead1Tex.dispose();
        dead2Tex.dispose();
        dead3Tex.dispose();
    }
}
