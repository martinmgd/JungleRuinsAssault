package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class DisparoAssets implements Disposable {

    // Normal (3 frames en una sola fila)
    public static final String PATH_NORMAL_SHEET = "sprites/projectiles/fireball_sheet.png";
    public static final int NORMAL_FRAMES = 3;
    public static final float NORMAL_FRAME_TIME = 0.06f;

    // Especial: start_sheet (3 frames), body (1 frame), end_sheet (3 frames)
    public static final String PATH_SPECIAL_START_SHEET = "sprites/projectiles/special_start_sheet.png";
    public static final String PATH_SPECIAL_BODY = "sprites/projectiles/special_body.png";
    public static final String PATH_SPECIAL_END_SHEET = "sprites/projectiles/special_end_sheet.png";

    public static final int SPECIAL_START_FRAMES = 3;
    public static final int SPECIAL_END_FRAMES = 3;

    public static final float SPECIAL_START_FRAME_TIME = 0.05f;
    public static final float SPECIAL_END_FRAME_TIME = 0.05f;

    private final Texture normalTex;
    private final Texture specialStartTex;
    private final Texture specialBodyTex;
    private final Texture specialEndTex;

    public final Animation<TextureRegion> normalAnim;

    public final Animation<TextureRegion> specialStartAnim; // no loop
    public final TextureRegion specialBodyRegion;           // 1 frame
    public final Animation<TextureRegion> specialEndAnim;   // no loop

    public DisparoAssets() {
        normalTex = new Texture(PATH_NORMAL_SHEET);
        specialStartTex = new Texture(PATH_SPECIAL_START_SHEET);
        specialBodyTex = new Texture(PATH_SPECIAL_BODY);
        specialEndTex = new Texture(PATH_SPECIAL_END_SHEET);

        normalAnim = buildAnimFromSheet(normalTex, NORMAL_FRAMES, NORMAL_FRAME_TIME, Animation.PlayMode.LOOP);

        TextureRegion[] startFrames = splitSheetHorizontal(specialStartTex, SPECIAL_START_FRAMES);
        specialStartAnim = new Animation<>(SPECIAL_START_FRAME_TIME, startFrames);
        specialStartAnim.setPlayMode(Animation.PlayMode.NORMAL);

        specialBodyRegion = new TextureRegion(specialBodyTex);

        TextureRegion[] endFrames = splitSheetHorizontal(specialEndTex, SPECIAL_END_FRAMES);
        specialEndAnim = new Animation<>(SPECIAL_END_FRAME_TIME, endFrames);
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
        specialStartTex.dispose();
        specialBodyTex.dispose();
        specialEndTex.dispose();
    }
}
