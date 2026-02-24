package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaPausa extends ScreenAdapter {

    private final Main juego;
    private final PantallaJuego juegoPausado;

    private OrthographicCamera camera;

    private BitmapFont fontTitle;
    private BitmapFont fontItem;
    private GlyphLayout layout;

    private Texture pixel;

    private int seleccionado = 0;

    public PantallaPausa(Main juego, PantallaJuego juegoPausado) {
        this.juego = juego;
        this.juegoPausado = juegoPausado;
    }

    @Override
    public void show() {

        // Cámara en coordenadas de pantalla reales
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        fontTitle = new BitmapFont();
        fontItem = new BitmapFont();

        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        fontItem.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);

        fontTitle.getData().setScale(3f);
        fontItem.getData().setScale(2f);

        layout = new GlyphLayout();

        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(1,1,1,1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            juego.setScreen(juegoPausado);
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
                juego.setScreen(juegoPausado);
            } else if (seleccionado == 1) {
                juego.setScreen(new PantallaJuego(juego));
            } else {
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        ScreenUtils.clear(0f,0f,0f,1f);

        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.setColor(1f,1f,1f,1f);

        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float panelW = screenW * 0.6f;
        float panelH = screenH * 0.6f;
        float panelX = (screenW - panelW) / 2f;
        float panelY = (screenH - panelH) / 2f;

        // Fondo oscuro
        juego.batch.setColor(0f,0f,0f,0.7f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        juego.batch.setColor(1f,1f,1f,1f);

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
        fontTitle.dispose();
        fontItem.dispose();
        pixel.dispose();
    }
}
