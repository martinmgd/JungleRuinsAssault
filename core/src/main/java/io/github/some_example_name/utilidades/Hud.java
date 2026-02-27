package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Hud implements Disposable {

    private final OrthographicCamera cam;
    private final Viewport viewport;

    private final HudAssets assets;

    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private int vida = 100;
    private int score = 0;
    private float tiempoSeg = 0f;

    private static final int MAX_CORAZONES = 5;

    private static final float REF_W = 1280f;
    private static final float REF_H = 720f;

    private static final float PAD_X = 22f;
    private static final float PAD_Y = 14f;

    private static final float HEART_PX = 40f;
    private static final float HEART_GAP_PX = 10f;

    private static final float TOP_DOWN_PX = 10f;

    private static final float SCORE_FONT_SCALE = 2.10f;
    private static final float TIME_FONT_SCALE = 2.30f;
    private static final float TIME_TOP_PAD_PX = 34f;

    // ------------------------------------------------------------
    // BONUS (cartel temporal al terminar)
    // ------------------------------------------------------------
    private boolean showBonus = false;
    private int bonusValue = 0;
    private float bonusTimer = 0f;
    private static final float BONUS_SHOW_SECONDS = 2.2f;
    private static final float BONUS_FONT_SCALE = 1.60f;

    // Cada corazón representa este "bloque" de vida:
    private static final int VIDA_POR_CORAZON = 20;

    public Hud() {
        cam = new OrthographicCamera();
        viewport = new ScreenViewport(cam);

        assets = new HudAssets();

        font = new BitmapFont();
        font.getData().setScale(1f);
        font.setUseIntegerPositions(true);
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void setVida(int vida) {
        this.vida = Math.max(0, vida);
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void setTiempoSeg(float tiempoSeg) {
        this.tiempoSeg = Math.max(0f, tiempoSeg);
    }

    public void showBonusTiempo(int bonus) {
        if (bonus <= 0) return;
        showBonus = true;
        bonusValue = bonus;
        bonusTimer = BONUS_SHOW_SECONDS;
    }

    public void clearBonus() {
        showBonus = false;
        bonusValue = 0;
        bonusTimer = 0f;
    }

    // ✅ NUEVO: para usar el mismo corazón en drops del mundo
    public TextureRegion getHeartFullRegion() {
        return assets.heartFull;
    }

    private float getUiScale() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float sx = w / REF_W;
        float sy = h / REF_H;
        return Math.min(sx, sy);
    }

    // ✅ FIX: corazones por "bloques" (20 vida = 1 corazón), NO por porcentaje con round()
    private int getCorazonesLlenos() {
        if (vida <= 0) return 0;

        int llenos = (vida + VIDA_POR_CORAZON - 1) / VIDA_POR_CORAZON; // ceil(vida/20)
        if (llenos < 0) llenos = 0;
        if (llenos > MAX_CORAZONES) llenos = MAX_CORAZONES;
        return llenos;
    }

    private String formatTiempo(float s) {
        int total = (int) Math.floor(s);
        int min = total / 60;
        int sec = total % 60;
        if (min <= 0) return String.format("%02d", sec);
        return String.format("%d:%02d", min, sec);
    }

    public void draw(SpriteBatch batch) {
        viewport.apply();
        batch.setProjectionMatrix(cam.combined);

        float dt = Gdx.graphics.getDeltaTime();
        if (showBonus) {
            bonusTimer -= dt;
            if (bonusTimer <= 0f) {
                showBonus = false;
                bonusValue = 0;
                bonusTimer = 0f;
            }
        }

        float scale = getUiScale();

        float padX = PAD_X * scale;
        float padY = PAD_Y * scale;

        float heartSize = HEART_PX * scale;
        float gap = HEART_GAP_PX * scale;

        float topDown = TOP_DOWN_PX * scale;

        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        // CORAZONES
        int llenos = getCorazonesLlenos();

        float startX = padX;
        float yHearts = screenH - padY - heartSize - topDown;

        TextureRegion full = assets.heartFull;
        TextureRegion empty = assets.heartEmpty;

        for (int i = 0; i < MAX_CORAZONES; i++) {
            float x = startX + i * (heartSize + gap);
            TextureRegion r = (i < llenos) ? full : empty;
            batch.draw(r, x, yHearts, heartSize, heartSize);
        }

        // SCORE
        String sScore = Integer.toString(score);

        font.getData().setScale(SCORE_FONT_SCALE * scale);
        layout.setText(font, sScore);

        float scoreX = screenW - padX - layout.width;

        float yTopUI = screenH - padY - topDown;
        float scoreY = yTopUI;

        font.draw(batch, layout, scoreX, scoreY);

        // TIEMPO
        String tStr = formatTiempo(tiempoSeg);

        font.getData().setScale(TIME_FONT_SCALE * scale);
        layout.setText(font, tStr);

        float timeX = (screenW - layout.width) * 0.5f;
        float timeY = screenH - (TIME_TOP_PAD_PX * scale);

        font.draw(batch, layout, timeX, timeY);

        // BONUS TIEMPO
        if (showBonus && bonusValue > 0) {
            String bStr = "BONUS +" + bonusValue;

            font.getData().setScale(BONUS_FONT_SCALE * scale);
            layout.setText(font, bStr);

            float bx = (screenW - layout.width) * 0.5f;
            float by = timeY - (34f * scale);

            font.draw(batch, layout, bx, by);
        }

        font.getData().setScale(1f);
    }

    @Override
    public void dispose() {
        if (assets != null) assets.dispose();
        if (font != null) font.dispose();
    }
}
