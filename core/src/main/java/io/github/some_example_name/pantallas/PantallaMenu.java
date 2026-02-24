package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

    private BitmapFont font;

    private int selected = 0;

    // 0: Jugar, 1: Opciones, 2: Salir
    private static final int ITEM_COUNT = 3;

    public PantallaMenu(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        font = new BitmapFont();
        font.setUseIntegerPositions(false);
        float dpiScale = Gdx.graphics.getDensity();
        float factor = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;
        font.getData().setScale(factor * 2.2f);
        font.setColor(Color.WHITE);

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

        // Título
        String titulo = safeT("menu_title", "Jungle Ruins Assault");
        drawCentered(titulo, viewport.getWorldHeight() * 0.78f);

        // Items
        float baseY = viewport.getWorldHeight() * 0.52f;
        float step = viewport.getWorldHeight() * 0.10f;

        drawItem(0, safeT("menu_play", "Jugar"), baseY - step * 0);
        drawItem(1, safeT("menu_options", "Opciones"), baseY - step * 1);
        drawItem(2, safeT("menu_exit", "Salir"), baseY - step * 2);

        // Hint
        String hint = safeT("menu_hint", "↑↓ para elegir, ENTER para aceptar");
        font.getData().setScale(font.getData().scaleX * 0.85f, font.getData().scaleY * 0.85f);
        drawCentered(hint, viewport.getWorldHeight() * 0.15f);
        // volver al scale original
        float dpiScale = Gdx.graphics.getDensity();
        float factor = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;
        font.getData().setScale(factor * 2.2f);

        juego.batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + ITEM_COUNT) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activateSelected();
        }
        // Escape para salir (en desktop)
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
        float w = font.getRegion().getRegionWidth(); // no real width of text, but ok; we center roughly with layout below
        // Mejor: usar GlyphLayout
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
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
    }
}
