package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class DisparoAssets implements Disposable {

    public static final String PATH_NORMAL_SHEET  = "sprites/projectiles/fireball_sheet.png";
    public static final String PATH_SPECIAL_SHEET = "sprites/projectiles/special_sheet.png";

    public static final int NORMAL_FRAMES = 3;
    public static final int SPECIAL_FRAMES_TOTAL = 5;

    public static final float NORMAL_FRAME_TIME = 0.06f;
    public static final float SPECIAL_START_FRAME_TIME = 0.05f;

    private final Texture normalTex;
    private final Texture specialTex;

    public final Animation<TextureRegion> normalAnim;
    public final Animation<TextureRegion> specialStartAnim; // frames 1-4 (no loop)

    // Frame 5 (antes “fly”, ahora “segmento del chorro”)
    public final TextureRegion specialFlyRegion;
    public final TextureRegion specialStreamRegion; // alias más claro

    public DisparoAssets() {
        normalTex = new Texture(PATH_NORMAL_SHEET);
        specialTex = new Texture(PATH_SPECIAL_SHEET);

        normalAnim = buildAnimFromSheet(normalTex, NORMAL_FRAMES, NORMAL_FRAME_TIME, Animation.PlayMode.LOOP);

        TextureRegion[] specialFrames = splitSheetHorizontal(specialTex, SPECIAL_FRAMES_TOTAL);

        TextureRegion[] start = new TextureRegion[4];
        System.arraycopy(specialFrames, 0, start, 0, 4);

        specialStartAnim = new Animation<>(SPECIAL_START_FRAME_TIME, start);
        specialStartAnim.setPlayMode(Animation.PlayMode.NORMAL);

        specialFlyRegion = specialFrames[4];
        specialStreamRegion = specialFlyRegion;
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
