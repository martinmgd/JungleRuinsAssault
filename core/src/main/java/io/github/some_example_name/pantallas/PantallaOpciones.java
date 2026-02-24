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
import io.github.some_example_name.utilidades.Configuracion;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaOpciones extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;
    private BitmapFont font;

    private int selected = 0;

    // 0: Idioma, 1: Dificultad, 2: Volver
    private static final int ITEM_COUNT = 3;

    public PantallaOpciones(Main juego) {
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
        font.getData().setScale(factor * 2.0f);
        font.setColor(Color.WHITE);

        Idiomas.get();
    }

    @Override
    public void render(float delta) {
        handleInput();

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        String titulo = safeT("options_title", "Opciones");
        drawCentered(titulo, viewport.getWorldHeight() * 0.80f);

        float baseY = viewport.getWorldHeight() * 0.55f;
        float step = viewport.getWorldHeight() * 0.12f;

        // Idioma (ES/GL)
        String lang = Idiomas.getIdioma();
        String langName = "es".equalsIgnoreCase(lang)
            ? safeT("language_es", "Español")
            : ("gl".equalsIgnoreCase(lang) ? safeT("language_gl", "Galego") : lang);

        String idiomaLine = safeT("options_language", "Idioma") + ": " + langName;
        drawItem(0, idiomaLine, baseY - step * 0);

        // Dificultad
        Configuracion.Dificultad d = Configuracion.getDificultad();
        String diffName = (d == Configuracion.Dificultad.DIFICIL)
            ? safeT("difficulty_hard", "Difícil")
            : safeT("difficulty_easy", "Fácil");

        String diffLine = safeT("options_difficulty", "Dificultad") + ": " + diffName;
        drawItem(1, diffLine, baseY - step * 1);

        // Volver
        drawItem(2, safeT("back", "Volver"), baseY - step * 2);

        String hint = safeT("options_hint", "←→ cambia, ↑↓ selecciona, ENTER acepta, ESC vuelve");
        font.getData().setScale(font.getData().scaleX * 0.85f, font.getData().scaleY * 0.85f);
        drawCentered(hint, viewport.getWorldHeight() * 0.15f);

        // restaurar scale
        float dpiScale = Gdx.graphics.getDensity();
        float factor = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;
        font.getData().setScale(factor * 2.0f);

        juego.batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + ITEM_COUNT) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }

        boolean left = Gdx.input.isKeyJustPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);

        if ((left || right)) {
            if (selected == 0) {
                toggleIdioma();
            } else if (selected == 1) {
                toggleDificultad();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selected == 0) toggleIdioma();
            else if (selected == 1) toggleDificultad();
            else if (selected == 2) volver();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volver();
        }
    }

    private void toggleIdioma() {
        String current = Idiomas.getIdioma();
        String next = "es".equalsIgnoreCase(current) ? "gl" : "es";
        Idiomas.setIdioma(next);
        // No hace falta recrear pantalla: ya se recarga el bundle en setIdioma()
    }

    private void toggleDificultad() {
        Configuracion.Dificultad current = Configuracion.getDificultad();
        Configuracion.Dificultad next = (current == Configuracion.Dificultad.FACIL)
            ? Configuracion.Dificultad.DIFICIL
            : Configuracion.Dificultad.FACIL;
        Configuracion.setDificultad(next);
    }

    private void volver() {
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
