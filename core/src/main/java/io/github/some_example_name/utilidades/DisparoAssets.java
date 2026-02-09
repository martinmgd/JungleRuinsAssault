package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class DisparoAssets implements Disposable {

    // Normal (3 frames en una sola fila)
    public static final String PATH_NORMAL_SHEET = "sprites/proyectiles/fireball_sheet.png";
    public static final int NORMAL_FRAMES = 3;
    public static final float NORMAL_FRAME_TIME = 0.06f;

    // Especial: 12 frames en una sola fila
    public static final String PATH_SPECIAL_SHEET = "sprites/proyectiles/special_sheet.png";
    public static final int SPECIAL_TOTAL_FRAMES = 12;

    // Reparto dentro del sheet especial:
    // BUILD: frames 1..8
    // LOOP:  frames 9..11 (loop)
    // END:   frame 12 (1 frame)
    public static final int SPECIAL_BUILD_FRAMES = 8;
    public static final int SPECIAL_LOOP_FRAMES = 3;
    public static final int SPECIAL_END_FRAMES = 1;

    public static final float SPECIAL_LOOP_FRAME_TIME = 0.06f;
    public static final float SPECIAL_END_FRAME_TIME = 0.08f;

    private final Texture normalTex;
    private final Texture specialTex;

    public final Animation<TextureRegion> normalAnim;

    public final Animation<TextureRegion> specialBuildAnim; // 8 frames, no loop (los timings finos se controlan en AtaqueEspecial)
    public final Animation<TextureRegion> specialLoopAnim;  // 3 frames, loop
    public final Animation<TextureRegion> specialEndAnim;   // 1 frame, no loop

    public DisparoAssets() {
        normalTex = new Texture(PATH_NORMAL_SHEET);
        specialTex = new Texture(PATH_SPECIAL_SHEET);

        normalAnim = buildAnimFromSheet(normalTex, NORMAL_FRAMES, NORMAL_FRAME_TIME, Animation.PlayMode.LOOP);

        TextureRegion[] all = splitSheetHorizontal(specialTex, SPECIAL_TOTAL_FRAMES);

        TextureRegion[] build = new TextureRegion[SPECIAL_BUILD_FRAMES];
        System.arraycopy(all, 0, build, 0, SPECIAL_BUILD_FRAMES);
        specialBuildAnim = new Animation<>(0.01f, build); // frameTime dummy: el ritmo real lo controla AtaqueEspecial
        specialBuildAnim.setPlayMode(Animation.PlayMode.NORMAL);

        TextureRegion[] loop = new TextureRegion[SPECIAL_LOOP_FRAMES];
        System.arraycopy(all, SPECIAL_BUILD_FRAMES, loop, 0, SPECIAL_LOOP_FRAMES);
        specialLoopAnim = new Animation<>(SPECIAL_LOOP_FRAME_TIME, loop);
        specialLoopAnim.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] end = new TextureRegion[SPECIAL_END_FRAMES];
        System.arraycopy(all, SPECIAL_BUILD_FRAMES + SPECIAL_LOOP_FRAMES, end, 0, SPECIAL_END_FRAMES);
        specialEndAnim = new Animation<>(SPECIAL_END_FRAME_TIME, end);
        specialEndAnim.setPlayMode(Animation.PlayMode.NORMAL);
    }

    private Animation<TextureRegion> buildAnimFromSheet(Texture sheet, int frames, float frameTime, Animation.PlayMode mode) {
        TextureRegion[] arr = splitSheetHorizontal(sheet, frames);
        Animation<TextureRegion> anim = new Animation<>(frameTime, arr);
        anim.setPlayMode(mode);
        return anim;
    }

    private TextureRegion[] splitSheetHorizontal(Texture sheet, int frames) {
        int frameW = sheet.getWidth() / frames;
        int frameH = sheet.getHeight();

        TextureRegion[] arr = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            arr[i] = new TextureRegion(sheet, i * frameW, 0, frameW, frameH);
        }
        return arr;
    }

    @Override
    public void dispose() {
        normalTex.dispose();
        specialTex.dispose();
    }
}
