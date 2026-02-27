package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaMuerte extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camera;

    private BitmapFont font;
    private GlyphLayout layout;

    private float factorEscaladoFuente;

    private Texture pixel;

    // Fondo capturado (para ver el juego detrás)
    private Texture fondoCapturado;
    private TextureRegion fondoRegion;

    private int selected = 0;
    private static final int ITEM_COUNT = 2;

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático (menú solo táctil en móvil)
    // ------------------------------------------------------------
    private boolean esMovil = false;

    public PantallaMuerte(Main juego) {
        this.juego = juego;
    }

    // Compatibilidad si lo llamas con 2 parámetros
    public PantallaMuerte(Main juego, Object ignored) {
        this.juego = juego;
    }

    @Override
    public void show() {

        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Captura sin deprecated
        int w = Gdx.graphics.getBackBufferWidth();
        int h = Gdx.graphics.getBackBufferHeight();

        Pixmap shot = Pixmap.createFromFrameBuffer(0, 0, w, h);
        fondoCapturado = new Texture(shot);
        shot.dispose();

        fondoCapturado.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Arreglo del “invertido”: el framebuffer viene al revés en Y
        fondoRegion = new TextureRegion(fondoCapturado);
        fondoRegion.flip(false, true);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        font.setUseIntegerPositions(false);

        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        font.getData().setScale(factorEscaladoFuente * 1.4f);
        font.setColor(Color.WHITE);
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        layout = new GlyphLayout();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        Idiomas.get();
    }

    @Override
    public void render(float delta) {
        handleInput();

        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Fondo capturado (ya NO invertido)
        if (fondoRegion != null) {
            juego.batch.setColor(1f, 1f, 1f, 1f);
            juego.batch.draw(fondoRegion, 0f, 0f, screenW, screenH);
        }

        // Panel estilo PantallaPausa
        float panelW = screenW * 0.245f;
        float panelH = screenH * 0.5f;
        float panelX = (screenW - panelW) * 0.5f;
        float panelY = (screenH - panelH) * 0.5f;

        // Rectángulo semi-transparente
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = screenW * 0.5f;

        // Título
        String titulo = safeT("death_title", "Has muerto");
        font.setColor(1f, 0.25f, 0.25f, 1f);
        drawCentered(titulo, centerX, panelY + panelH * 0.82f);
        font.setColor(Color.WHITE);

        // Opciones centradas
        float y0 = panelY + panelH * 0.55f;
        float dy = panelH * 0.20f;

        drawItem(0, safeT("death_restart", "Reiniciar"), centerX, y0);
        drawItem(1, safeT("death_menu", "Volver al menú"), centerX, y0 - dy);

        juego.batch.end();
    }

    private void handleInput() {

        // ------------------------------------------------------------
        // MÓVIL: SOLO TÁCTIL
        // ------------------------------------------------------------
        if (esMovil) {

            if (Gdx.input.justTouched()) {

                // ✅ NO invertir Y antes de unproject
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                Vector3 tmp = new Vector3(sx, sy, 0f);
                camera.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                float screenW = Gdx.graphics.getWidth();
                float screenH = Gdx.graphics.getHeight();

                float panelW = screenW * 0.245f;
                float panelH = screenH * 0.5f;
                float panelX = (screenW - panelW) * 0.5f;
                float panelY = (screenH - panelH) * 0.5f;

                float centerX = screenW * 0.5f;

                float y0 = panelY + panelH * 0.55f;
                float dy = panelH * 0.20f;
                float y1 = y0 - dy;

                // Zona “tap” horizontal dentro del panel
                boolean dentroPanelX = (wx >= panelX && wx <= panelX + panelW);

                // Altura de toque: aproximamos con el alto de línea de la fuente
                float halfH = font.getLineHeight() * 0.60f;

                if (dentroPanelX && wy >= y0 - halfH && wy <= y0 + halfH) {
                    selected = 0;
                    reiniciar();
                    return;
                }

                if (dentroPanelX && wy >= y1 - halfH && wy <= y1 + halfH) {
                    selected = 1;
                    volverMenu();
                    return;
                }
            }

            return;
        }

        // ------------------------------------------------------------
        // PC: TECLADO como hasta ahora
        // ------------------------------------------------------------
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selected == 0) reiniciar();
            else volverMenu();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volverMenu();
        }
    }

    private void reiniciar() {
        juego.setScreen(new PantallaJuego(juego));
        dispose();
    }

    private void volverMenu() {
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    private void drawItem(int idx, String text, float centerX, float y) {
        String line = (idx == selected ? "> " : "") + text;

        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        drawCentered(line, centerX, y);
        font.setColor(Color.WHITE);
    }

    private void drawCentered(String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    private String safeT(String key, String fallback) {
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (pixel != null) pixel.dispose();

        if (fondoCapturado != null) {
            fondoCapturado.dispose();
            fondoCapturado = null;
        }
        fondoRegion = null;
    }
}
