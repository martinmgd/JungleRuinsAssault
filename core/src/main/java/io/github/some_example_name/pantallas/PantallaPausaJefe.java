package io.github.some_example_name.pantallas;

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
import com.badlogic.gdx.utils.ScreenUtils;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaPausaJefe extends ScreenAdapter {

    private final Main juego;
    private final PantallaSalaJefe salaJefe;

    private OrthographicCamera camera;

    private BitmapFont fontTitle;
    private BitmapFont fontItem;
    private GlyphLayout layout;

    private float factorEscaladoFuente;

    private Texture pixel;

    private int seleccionado = 0;

    public PantallaPausaJefe(Main juego, PantallaSalaJefe salaJefe) {
        this.juego = juego;
        this.salaJefe = salaJefe;
    }

    @Override
    public void show() {

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10-Regular.fnt y todos sus png
        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontItem = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Densidad de la pantalla del dispositivo (apuntes)
        float dpiScale = Gdx.graphics.getDensity();

        // Factor para trabajar con unidades del "mundo" y no con píxeles (adaptado a esta pantalla)
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        // Escalado (similar a apuntes)
        fontTitle.getData().setScale(factorEscaladoFuente * 2f);
        fontItem.getData().setScale(factorEscaladoFuente * 1.4f);

        fontTitle.setColor(Color.WHITE);
        fontItem.setColor(Color.WHITE);

        // Filtro: Nearest para mantener estilo pixelado
        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontItem.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        layout = new GlyphLayout();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    @Override
    public void render(float delta) {

        // ESC / P -> continuar (reanudar música del jefe)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)) {

            if (salaJefe != null) salaJefe.resumeMusica();
            juego.setScreen(salaJefe);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            seleccionado = (seleccionado + 3 - 1) % 3;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            seleccionado = (seleccionado + 1) % 3;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (seleccionado == 0) {
                // Continuar: reanudar música
                if (salaJefe != null) salaJefe.resumeMusica();
                juego.setScreen(salaJefe);

            } else if (seleccionado == 1) {
                // Reiniciar: parar música actual del jefe y recrear sala
                if (salaJefe != null) salaJefe.stopMusica();
                juego.setScreen(new PantallaSalaJefe(juego, salaJefe.getJugador()));

            } else {
                // Menú: parar música del jefe y volver al menú
                if (salaJefe != null) salaJefe.stopMusica();
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.setColor(1f, 1f, 1f, 1f);

        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float panelW = screenW * 0.6f;
        float panelH = screenH * 0.6f;
        float panelX = (screenW - panelW) / 2f;
        float panelY = (screenH - panelH) / 2f;

        juego.batch.setColor(0f, 0f, 0f, 0.7f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = screenW / 2f;

        drawCentered(fontTitle, Idiomas.t("pause_title"), centerX, panelY + panelH * 0.85f);

        float left = panelX + panelW * 0.2f;
        float y0 = panelY + panelH * 0.6f;
        float dy = panelH * 0.15f;

        drawItem(Idiomas.t("pause_continue"), left, y0, seleccionado == 0);
        drawItem(Idiomas.t("pause_restart"), left, y0 - dy, seleccionado == 1);
        drawItem(Idiomas.t("pause_menu"), left, y0 - dy * 2f, seleccionado == 2);

        juego.batch.end();
    }

    private void drawCentered(BitmapFont font, String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width / 2f;
        font.draw(juego.batch, layout, x, y);
    }

    private void drawItem(String text, float x, float y, boolean selected) {
        if (selected) {
            fontItem.draw(juego.batch, "> " + text, x, y);
        } else {
            fontItem.draw(juego.batch, text, x, y);
        }
    }

    @Override
    public void dispose() {
        if (fontTitle != null) fontTitle.dispose();
        if (fontItem != null) fontItem.dispose();
        if (pixel != null) pixel.dispose();
    }
}
