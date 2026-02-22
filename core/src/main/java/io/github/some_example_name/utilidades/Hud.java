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

    // Referencia para escalar en pantallas pequeñas
    private static final float REF_W = 1280f;
    private static final float REF_H = 720f;

    // Padding general
    private static final float PAD_X = 22f;
    private static final float PAD_Y = 14f;

    // Tamaño corazones (tú dijiste 40 ok)
    private static final float HEART_PX = 40f;
    private static final float HEART_GAP_PX = 10f;

    // Bajada de la fila superior (corazones y score)
    private static final float TOP_DOWN_PX = 10f;

    // Score más grande
    private static final float SCORE_FONT_SCALE = 2.10f;

    // Reloj grande centrado
    private static final float TIME_FONT_SCALE = 2.30f;

    // Reloj un poco más bajo
    private static final float TIME_TOP_PAD_PX = 34f;

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

    private float getUiScale() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float sx = w / REF_W;
        float sy = h / REF_H;
        return Math.min(sx, sy);
    }

    private int getCorazonesLlenos() {
        float frac = vida / 100f;
        int llenos = Math.round(frac * MAX_CORAZONES);
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

        float scale = getUiScale();

        float padX = PAD_X * scale;
        float padY = PAD_Y * scale;

        float heartSize = HEART_PX * scale;
        float gap = HEART_GAP_PX * scale;

        float topDown = TOP_DOWN_PX * scale;

        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        // ------------------------------------------------------------
        // CORAZONES (✅ ARRIBA IZQUIERDA)
        // ------------------------------------------------------------
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

        // ------------------------------------------------------------
        // SCORE (ARRIBA DERECHA, más grande)
        // ------------------------------------------------------------
        String sScore = Integer.toString(score);

        font.getData().setScale(SCORE_FONT_SCALE * scale);
        layout.setText(font, sScore);

        float scoreX = screenW - padX - layout.width;
        float scoreY = yHearts - (14f * scale);

        font.draw(batch, layout, scoreX, scoreY);

        // ------------------------------------------------------------
        // TIEMPO (centrado arriba, grande, un poco más bajo)
        // ------------------------------------------------------------
        String tStr = formatTiempo(tiempoSeg);

        font.getData().setScale(TIME_FONT_SCALE * scale);
        layout.setText(font, tStr);

        float timeX = (screenW - layout.width) * 0.5f;
        float timeY = screenH - (TIME_TOP_PAD_PX * scale);

        font.draw(batch, layout, timeX, timeY);

        // restaurar
        font.getData().setScale(1f);
    }

    @Override
    public void dispose() {
        if (assets != null) assets.dispose();
        if (font != null) font.dispose();
    }
}
