package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaMuerte extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;
    private BitmapFont font;

    private float factorEscaladoFuente;

    private int selected = 0;
    // 0: Reiniciar, 1: Menú
    private static final int ITEM_COUNT = 2;

    public PantallaMuerte(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10.fnt y todos sus png
        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        font.setUseIntegerPositions(false);

        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        font.getData().setScale(factorEscaladoFuente * 2.2f);
        font.setColor(Color.WHITE);

        // Filtro: Nearest para estilo pixelado
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        Idiomas.get();
    }

    @Override
    public void render(float delta) {
        handleInput();

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        String titulo = safeT("death_title", "Has muerto");
        font.setColor(1f, 0.25f, 0.25f, 1f);
        drawCentered(titulo, viewport.getWorldHeight() * 0.75f);
        font.setColor(Color.WHITE);

        float baseY = viewport.getWorldHeight() * 0.50f;
        float step = viewport.getWorldHeight() * 0.12f;

        drawItem(0, safeT("death_restart", "Reiniciar"), baseY - step * 0);
        drawItem(1, safeT("death_menu", "Volver al menú"), baseY - step * 1);

        juego.batch.end();
    }

    private void handleInput() {
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

    private void drawItem(int idx, String text, float y) {
        String prefix = (idx == selected) ? "> " : "  ";
        String line = prefix + text;

        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        drawCentered(line, y);
        font.setColor(Color.WHITE);
    }

    private void drawCentered(String text, float y) {
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
