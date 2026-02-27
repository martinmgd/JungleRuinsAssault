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

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaPausa extends ScreenAdapter {

    private final Main juego;
    private final PantallaJuego juegoPausado;

    private OrthographicCamera camera;

    private BitmapFont fontTitle;
    private BitmapFont fontItem;
    private GlyphLayout layout;

    private float factorEscaladoFuente;

    private Texture pixel;

    private int seleccionado = 0;

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático
    // ------------------------------------------------------------
    private boolean esMovil = false;

    public PantallaPausa(Main juego, PantallaJuego juegoPausado) {
        this.juego = juego;
        this.juegoPausado = juegoPausado;
    }

    @Override
    public void show() {

        // NUEVO: pausar música al entrar en pausa
        if (juegoPausado != null) juegoPausado.pauseMusicaNivel();

        // ✅ NUEVO: marcar al juego como "en pausa" para que NO vuelva a arrancar música con sync()
        if (juegoPausado != null) juegoPausado.setEnPausa(true);

        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Asegura bundle
        Idiomas.get();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontItem = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        fontTitle.getData().setScale(factorEscaladoFuente * 2f);
        fontItem.getData().setScale(factorEscaladoFuente * 1.4f);

        fontTitle.setColor(Color.WHITE);
        fontItem.setColor(Color.WHITE);

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

        // Dibujar el juego congelado detrás (para que la transparencia sea real)
        juegoPausado.render(0);

        // ✅ IMPORTANTE: si PantallaJuego.render() hace sync y puede arrancar música,
        // aquí la forzamos a seguir en pausa.
        if (juegoPausado != null) juegoPausado.pauseMusicaNivel();

        // ------------------------------------------------------------
        // MÓVIL: TÁCTIL (tocar una opción)
        // ------------------------------------------------------------
        if (esMovil && Gdx.input.justTouched()) {

            float screenW = Gdx.graphics.getWidth();
            float screenH = Gdx.graphics.getHeight();

            // Panel: mantenemos altura igual, y reducimos el width un 30% respecto al que tenías (0.35 -> 0.245)
            float panelW = screenW * 0.245f;
            float panelH = screenH * 0.5f;

            float panelX = (screenW - panelW) * 0.5f;
            float panelY = (screenH - panelH) * 0.5f;

            // Coordenadas de toque en "mundo" de la cámara (aquí es igual que pantalla, solo invertimos Y)
            float wx = Gdx.input.getX();
            float wy = screenH - Gdx.input.getY();

            float centerX = screenW * 0.5f;

            // Items centrados
            float y0 = panelY + panelH * 0.55f;
            float dy = panelH * 0.18f;

            float yContinue = y0;
            float yRestart = y0 - dy;
            float yMenu = y0 - dy * 2f;

            float halfH = dy * 0.55f;

            boolean dentroPanelX = (wx >= panelX && wx <= panelX + panelW);

            if (dentroPanelX) {
                if (wy >= yContinue - halfH && wy <= yContinue + halfH) {
                    seleccionado = 0;

                    // ✅ quitar bandera de pausa antes de volver al juego
                    if (juegoPausado != null) juegoPausado.setEnPausa(false);

                    // NUEVO: reanudar música al volver al juego
                    if (juegoPausado != null) juegoPausado.resumeMusicaNivel();

                    juego.setScreen(juegoPausado);
                    return;

                } else if (wy >= yRestart - halfH && wy <= yRestart + halfH) {
                    seleccionado = 1;

                    // ✅ Reiniciar: CORTAR música del nivel aquí (porque hide() NO se llama desde PantallaPausa)
                    if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                    juego.setScreen(new PantallaJuego(juego));
                    return;

                } else if (wy >= yMenu - halfH && wy <= yMenu + halfH) {
                    seleccionado = 2;

                    // ✅ Menú: CORTAR música del nivel aquí (porque hide() NO se llama desde PantallaPausa)
                    if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                    juego.setScreen(new PantallaMenu(juego));
                    return;
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)) {

            // ✅ NUEVO: quitar bandera de pausa antes de volver al juego
            if (juegoPausado != null) juegoPausado.setEnPausa(false);

            // NUEVO: reanudar música al volver al juego
            if (juegoPausado != null) juegoPausado.resumeMusicaNivel();

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

                // ✅ NUEVO: quitar bandera de pausa antes de continuar
                if (juegoPausado != null) juegoPausado.setEnPausa(false);

                // NUEVO: reanudar música al continuar
                if (juegoPausado != null) juegoPausado.resumeMusicaNivel();

                juego.setScreen(juegoPausado);

            } else if (seleccionado == 1) {
                // ✅ Reiniciar: CORTAR música del nivel aquí (porque hide() NO se llama desde PantallaPausa)
                if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                juego.setScreen(new PantallaJuego(juego));

            } else {
                // ✅ Menú: CORTAR música del nivel aquí (porque hide() NO se llama desde PantallaPausa)
                if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Panel: mantenemos altura igual, y reducimos el width un 30% respecto al que tenías (0.35 -> 0.245)
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

        // Título centrado
        drawCentered(fontTitle, Idiomas.t("pause_title"), centerX, panelY + panelH * 0.82f);

        // Items centrados (en vez de ir con left)
        float y0 = panelY + panelH * 0.55f;
        float dy = panelH * 0.18f;

        drawCentered(fontItem, (seleccionado == 0 ? "> " : "") + Idiomas.t("pause_continue"), centerX, y0);
        drawCentered(fontItem, (seleccionado == 1 ? "> " : "") + Idiomas.t("pause_restart"), centerX, y0 - dy);
        drawCentered(fontItem, (seleccionado == 2 ? "> " : "") + Idiomas.t("pause_menu"), centerX, y0 - dy * 2f);

        juego.batch.end();
    }

    private void drawCentered(BitmapFont font, String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width / 2f;
        font.draw(juego.batch, layout, x, y);
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
        if (fontTitle != null) fontTitle.dispose();
        if (fontItem != null) fontItem.dispose();
        if (pixel != null) pixel.dispose();
    }
}
