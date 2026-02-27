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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaMenu extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;

    private com.badlogic.gdx.graphics.g2d.BitmapFont font;

    private float factorEscaladoFuente;

    private int selected = 0;

    // 0: Jugar, 1: Opciones, 2: Salir
    private static final int ITEM_COUNT = 3;

    // --- NUEVO: fondo + pixel 1x1 para panel (como Pausa/Muerte) ---
    private Texture fondoMenu;
    private Texture pixel;
    // ---------------------------------------------------------------

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático (menú solo táctil en móvil)
    // ------------------------------------------------------------
    private boolean esMovil = false;

    public PantallaMenu(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10.fnt y todos sus png
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        font.setUseIntegerPositions(false);

        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        font.getData().setScale(factorEscaladoFuente * 2.2f);
        font.setColor(Color.WHITE);

        // Filtro: Nearest para estilo pixelado
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // --- NUEVO: cargar fondo del menú (tu imagen) ---
        // Está en assets/sprites/fondos/fondoMenu.png
        fondoMenu = new Texture(Gdx.files.internal("sprites/fondos/fondoMenu.png"));
        fondoMenu.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // --- NUEVO: pixel 1x1 para dibujar rectángulo con alpha ---
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        // ------------------------------------------------

        // Asegura que el bundle está cargado (por si llegas aquí desde otra pantalla)
        Idiomas.get();
    }

    @Override
    public void render(float delta) {
        handleInput();

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        // Fondo del menú
        if (fondoMenu != null) {
            juego.batch.setColor(1f, 1f, 1f, 1f);
            juego.batch.draw(fondoMenu, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        // Panel central semi-transparente (como Pausa/Muerte)
        float panelW = viewport.getWorldWidth() * 0.55f;
        float panelH = viewport.getWorldHeight() * 0.55f;
        float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
        float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

        // Rectángulo semi-transparente
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(0.02f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Items: centrados dentro del panel (grupo centrado)
        float lineStep = font.getLineHeight() * 1.25f;

        float centerY = panelY + panelH * 0.55f;
        float baseY = centerY + lineStep;

        drawItem(0, safeT("menu_play", "Jugar"), baseY - lineStep * 0f);
        drawItem(1, safeT("menu_options", "Opciones"), baseY - lineStep * 1f);
        drawItem(2, safeT("menu_exit", "Salir"), baseY - lineStep * 2f);

        String hint = safeT("menu_hint", "↑↓ para elegir, ENTER para aceptar");

        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;

        font.getData().setScale(originalScaleX * 0.85f, originalScaleY * 0.85f);

        if (esMovil) {
            drawCentered(safeT("menu_hint_touch", "Toca una opción para continuar"), viewport.getWorldHeight() * 0.08f);
        } else {
            drawCentered(hint, viewport.getWorldHeight() * 0.08f);
        }

        font.getData().setScale(originalScaleX, originalScaleY);

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

                Vector2 tmp = new Vector2(sx, sy);
                viewport.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                float lineStep = font.getLineHeight() * 1.25f;

                float panelW = viewport.getWorldWidth() * 0.55f;
                float panelH = viewport.getWorldHeight() * 0.55f;
                float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
                float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

                float centerY = panelY + panelH * 0.55f;
                float baseY = centerY + lineStep;

                float y0 = baseY - lineStep * 0f;
                float y1 = baseY - lineStep * 1f;
                float y2 = baseY - lineStep * 2f;

                float halfH = lineStep * 0.55f;

                if (wy >= y0 - halfH && wy <= y0 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 0;
                    activateSelected();
                } else if (wy >= y1 - halfH && wy <= y1 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 1;
                    activateSelected();
                } else if (wy >= y2 - halfH && wy <= y2 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 2;
                    activateSelected();
                }
            }

            return;
        }

        // ------------------------------------------------------------
        // PC: TECLADO como hasta ahora
        // ------------------------------------------------------------
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + ITEM_COUNT) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activateSelected();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (Gdx.app != null) Gdx.app.exit();
        }
    }

    private void activateSelected() {
        switch (selected) {
            case 0:
                juego.setScreen(new PantallaJuego(juego));
                break;
            case 1:
                juego.setScreen(new PantallaOpciones(juego));
                break;
            case 2:
                if (Gdx.app != null) Gdx.app.exit();
                break;
            default:
                break;
        }
        dispose();
    }

    private void drawItem(int idx, String text, float y) {
        String prefix = (idx == selected) ? "> " : "  ";
        String line = prefix + text;

        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        drawCentered(line, y);
        font.setColor(Color.WHITE);
    }

    private void drawCentered(String text, float y) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
        float x = (viewport.getWorldWidth() - layout.width) * 0.5f;
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
        if (viewport != null) viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (fondoMenu != null) fondoMenu.dispose();
        if (pixel != null) pixel.dispose();
    }
}
