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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Constantes;

/**
 * Pantalla base para menús:
 * - Render correcto (clear + viewport.apply + projectionMatrix)
 * - Panel semitransparente centrado
 * - Texto con sombra
 * - Navegación UP/DOWN + LEFT/RIGHT + ENTER + ESC
 */
public abstract class BaseMenuScreen extends ScreenAdapter {

    protected final Main juego;

    protected OrthographicCamera camera;
    protected Viewport viewport;

    protected BitmapFont fontTitle;
    protected BitmapFont fontItem;
    protected GlyphLayout layout = new GlyphLayout();

    protected float factorEscaladoFuente;

    // textura 1x1 para dibujar rectángulos (panel, bordes, highlight)
    protected Texture pixel;

    // panel centrado
    protected float panelW;
    protected float panelH;
    protected float panelX;
    protected float panelY;

    protected int selectedIndex = 0;

    public BaseMenuScreen(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10.fnt y todos sus png
        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontItem = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Densidad de la pantalla del dispositivo (apuntes)
        float dpiScale = Gdx.graphics.getDensity();

        // Factor para trabajar con unidades del mundo y no con píxeles
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        // Escalado base (ajusta si quieres más grande/pequeño)
        fontTitle.getData().setScale(factorEscaladoFuente * 2.0f);
        fontItem.getData().setScale(factorEscaladoFuente * 1.4f);

        // Filtro: Nearest para mantener estilo pixelado
        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontItem.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // pixel blanco 1x1
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // panel centrado en mundo
        panelW = Constantes.ANCHO_MUNDO * 0.72f;
        panelH = Constantes.ALTO_MUNDO * 0.62f;
        panelX = (Constantes.ANCHO_MUNDO - panelW) * 0.5f;
        panelY = (Constantes.ALTO_MUNDO - panelH) * 0.5f;
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) viewport.update(width, height, true);

        // Recalcular factor por si cambian dimensiones
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        if (fontTitle != null) fontTitle.getData().setScale(factorEscaladoFuente * 2.0f);
        if (fontItem != null) fontItem.getData().setScale(factorEscaladoFuente * 1.4f);
    }

    @Override
    public void render(float delta) {
        // limpiar pantalla
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // viewport + cámara
        viewport.apply();
        camera.update();

        SpriteBatch batch = juego.batch;
        batch.setProjectionMatrix(camera.combined);

        // reset color (evita que una pantalla anterior deje alpha raro)
        batch.setColor(1f, 1f, 1f, 1f);

        batch.begin();

        // panel semitransparente
        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(pixel, panelX, panelY, panelW, panelH);

        // borde sutil
        batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(0.04f, panelH * 0.01f);
        batch.draw(pixel, panelX, panelY, panelW, b);
        batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        batch.draw(pixel, panelX, panelY, b, panelH);
        batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // devolver color
        batch.setColor(1f, 1f, 1f, 1f);

        // dibuja el menú concreto
        drawMenu(delta);

        batch.end();

        // input común
        handleBaseInput();
    }

    /** Dibuja título + items + hints en cada pantalla */
    protected abstract void drawMenu(float delta);

    /** Cuántos items tiene este menú */
    protected abstract int getItemsCount();

    /** Acción al pulsar ENTER */
    protected abstract void onEnter(int index);

    /** ESC por defecto */
    protected void onEscape() {}

    /** LEFT/RIGHT por defecto */
    protected void onLeft(int index) {}
    protected void onRight(int index) {}

    protected void handleBaseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) onEscape();

        // navegación vertical
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            moveSelection(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            moveSelection(+1);
        }

        // cambios laterales (opciones)
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            onLeft(selectedIndex);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            onRight(selectedIndex);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) onEnter(selectedIndex);
    }

    protected void moveSelection(int dir) {
        int count = getItemsCount();
        if (count <= 0) return;

        selectedIndex += dir;
        if (selectedIndex < 0) selectedIndex = count - 1;
        if (selectedIndex >= count) selectedIndex = 0;
    }

    // ---------------------------
    // Helpers de layout (por % del panel)
    // ---------------------------
    protected float panelCenterX() { return panelX + panelW * 0.5f; }
    protected float yInPanel(float fracFromBottom) { return panelY + panelH * fracFromBottom; }

    // ---------------------------
    // Helpers de dibujo
    // ---------------------------
    protected void drawCenteredTextShadow(BitmapFont font, String text, float centerX, float y, float shadowOffset) {
        float r = juego.batch.getColor().r;
        float g = juego.batch.getColor().g;
        float b = juego.batch.getColor().b;
        float a = juego.batch.getColor().a;

        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;

        // sombra
        juego.batch.setColor(0f, 0f, 0f, 0.70f);
        font.draw(juego.batch, layout, x + shadowOffset, y - shadowOffset);

        // texto
        juego.batch.setColor(r, g, b, a);
        font.draw(juego.batch, layout, x, y);
    }

    protected void drawMenuItem(String text, float x, float y, boolean selected) {
        layout.setText(fontItem, text);

        if (selected) {
            // highlight suave (en función del panel)
            float padX = panelW * 0.02f;
            float padY = panelH * 0.035f;

            juego.batch.setColor(1f, 1f, 1f, 0.08f);
            juego.batch.draw(pixel,
                x - padX,
                y - layout.height - padY * 0.35f,
                layout.width + padX * 2f,
                layout.height + padY
            );
            juego.batch.setColor(1f, 1f, 1f, 1f);

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
