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
import com.badlogic.gdx.math.Vector3;

import java.util.List;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Idiomas;
import io.github.some_example_name.utilidades.Records;
import io.github.some_example_name.utilidades.UiEscala;

public class PantallaRecords extends ScreenAdapter {

    // Referencia al juego principal (batch + cambios de pantalla).
    private final Main juego;

    // Cámara en coordenadas de pantalla (píxeles) para UI 2D.
    private OrthographicCamera camera;

    // Fuente principal para título, lista y botón.
    private BitmapFont font;

    // Layout para medir texto y poder centrar/alinear correctamente.
    private GlyphLayout layout;

    // Textura 1x1 usada para dibujar fondos, paneles y bordes por escalado.
    private Texture pixel;

    // Detecta si se ejecuta en móvil para cambiar el esquema de entrada y el hint.
    private boolean esMovil = false;

    // Estado de selección (solo un ítem: volver). Se mantiene por consistencia visual.
    private int selected = 0;

    // Número total de ítems seleccionables (aquí solo "volver").
    private static final int ITEM_COUNT = 1;

    // Rectángulo interactivo del botón "volver" (especialmente usado en móvil).
    private float btnX, btnY, btnW, btnH;

    // Cache del TOP cargado desde almacenamiento.
    private List<Records.Entry> top;

    // Factor de escalado de fuente recalculable al cambiar resolución.
    private float factorEscaladoFuente = 1.15f;

    public PantallaRecords(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {

        // Determina plataforma para activar control táctil en Android/iOS.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Configura cámara en píxeles.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Carga la fuente bitmap y permite posicionamiento subpíxel.
        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        font.setUseIntegerPositions(false);

        // Calcula escalado inicial para mantener legibilidad.
        recalcFontScale();

        // Configuración base de la fuente (color + filtro Nearest pixel-art).
        font.setColor(Color.WHITE);
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Layout para medir texto antes de dibujarlo.
        layout = new GlyphLayout();

        // Textura blanca 1x1 para UI (fondos/paneles/bordes).
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Asegura carga del sistema de idiomas y carga los records persistidos.
        Idiomas.get();
        top = Records.load();
    }

    private void recalcFontScale() {
        // Recalcula la escala final de la fuente en función de viewport, densidad y UiEscala.
        if (font == null || camera == null) return;

        float worldToScreen = (camera.viewportHeight / (float) Gdx.graphics.getHeight());

        float dpiScale = Gdx.graphics.getDensity();
        if (dpiScale < 1f) dpiScale = 1f;
        if (dpiScale > 1.5f) dpiScale = 1.5f;

        factorEscaladoFuente = worldToScreen * dpiScale * UiEscala.uiScale();

        // Tamaño general de la fuente en esta pantalla.
        font.getData().setScale(factorEscaladoFuente * 1.15f);
    }

    @Override
    public void render(float delta) {
        handleInput();

        camera.update();
        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Fondo negro a pantalla completa.
        juego.batch.setColor(0f, 0f, 0f, 1f);
        juego.batch.draw(pixel, 0f, 0f, w, h);

        // Panel centrado que contiene título, lista y botón.
        float panelW = w * 0.66f;
        float panelH = h * 0.72f;
        float panelX = (w - panelW) * 0.5f;
        float panelY = (h - panelH) * 0.65f;

        // Fondo del panel con transparencia.
        juego.batch.setColor(1f, 1f, 1f, 0.08f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde del panel.
        juego.batch.setColor(1f, 1f, 1f, 0.14f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restaura color del batch para texto.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = w * 0.5f;

        // Título.
        String titulo = safeT("records_title", "Records");
        font.setColor(0.25f, 0.9f, 1f, 1f);
        drawCentered(titulo, centerX, panelY + panelH * 0.90f);
        font.setColor(Color.WHITE);

        // Espaciado vertical de la lista.
        float lineStep = font.getLineHeight() * 1.15f;

        // Coordenadas X de las dos columnas.
        float colLeftX  = panelX + panelW * 0.18f;
        float colRightX = panelX + panelW * 0.60f;

        // Y inicial de la lista.
        float startY = panelY + panelH * 0.78f;

        // Columna izquierda (1-5).
        for (int i = 0; i < 5; i++) {
            String line;

            if (top != null && i < top.size()) {
                Records.Entry e = top.get(i);
                String ini = Records.normalizeInitials(e.initials);
                line = (i + 1) + ". " + ini + " , " + e.score;
            } else {
                line = (i + 1) + ". ___";
            }

            font.setColor(1f, 1f, 1f, 0.92f);
            drawLeft(line, colLeftX, startY - (lineStep * i));
        }

        // Columna derecha (6-10).
        for (int i = 5; i < 10; i++) {
            String line;

            if (top != null && i < top.size()) {
                Records.Entry e = top.get(i);
                String ini = Records.normalizeInitials(e.initials);
                line = (i + 1) + ". " + ini + " , " + e.score;
            } else {
                line = (i + 1) + ". ___";
            }

            font.setColor(1f, 1f, 1f, 0.92f);
            drawLeft(line, colRightX, startY - (lineStep * (i - 5)));
        }

        font.setColor(Color.WHITE);

        // Botón volver: calcula rectángulo interactivo según el panel.
        computeButton(panelX, panelY, panelW, panelH);

        // Texto del botón (solo PC, en móvil se deja vacío para no duplicar con el hint).
        String volverTxt = safeT("records_back", "Volver");

        // Fondo del botón.
        juego.batch.setColor(0f, 0f, 0f, 0.45f);
        juego.batch.draw(pixel, btnX, btnY, btnW, btnH);

        // Borde del botón.
        juego.batch.setColor(1f, 1f, 1f, 0.18f);
        juego.batch.draw(pixel, btnX, btnY, btnW, b);
        juego.batch.draw(pixel, btnX, btnY + btnH - b, btnW, b);
        juego.batch.draw(pixel, btnX, btnY, b, btnH);
        juego.batch.draw(pixel, btnX + btnW - b, btnY, b, btnH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Texto del botón: estilo resto de pantallas (amarillo + prefijo + ligeramente más grande).
        // En móvil se elimina para evitar solaparse con el hint "Toca el botón para volver".
        if (!esMovil) {

            float oldScaleX = font.getData().scaleX;
            float oldScaleY = font.getData().scaleY;

            font.getData().setScale(oldScaleX * 1.08f, oldScaleY * 1.08f);

            font.setColor(1f, 0.85f, 0.2f, 1f);
            drawCentered("> " + volverTxt, centerX, btnY + btnH * 0.65f);

            font.getData().setScale(oldScaleX, oldScaleY);
            font.setColor(Color.WHITE);
        }

        // Hint contextual según plataforma (formato como PantallaAyuda).
        if (!esMovil) {
            font.setColor(1f, 1f, 1f, 0.55f);
            drawCentered(safeT("records_hint_pc", "ESC para volver"), centerX, panelY + panelH * 0.12f);
            font.setColor(Color.WHITE);
        } else {
            font.setColor(1f, 0.85f, 0.2f, 1f);
            drawCentered(safeT("records_hint_mobile", "Toca para volver al menu"), centerX, panelY + panelH * 0.12f);
            font.setColor(Color.WHITE);
        }

        juego.batch.end();
    }

    private void handleInput() {

        // Móvil: volver tocando dentro del rectángulo del botón.
        if (esMovil) {
            if (Gdx.input.justTouched()) {
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                Vector3 tmp = new Vector3(sx, sy, 0f);
                camera.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                if (wx >= btnX && wx <= btnX + btnW && wy >= btnY && wy <= btnY + btnH) {
                    volverMenu();
                }
            }
            return;
        }

        // PC: volver con teclas rápidas.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volverMenu();
        }
    }

    private void volverMenu() {
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    private void computeButton(float panelX, float panelY, float panelW, float panelH) {
        // Dimensiones del botón relativas al panel para mantener layout responsive.
        btnW = panelW * 0.50f;
        btnH = panelH * 0.12f;

        // Centrado horizontal y colocado en la base del panel.
        btnX = panelX + (panelW - btnW) * 0.5f;
        btnY = panelY;
    }

    private void drawCentered(String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    private void drawLeft(String text, float x, float y) {
        layout.setText(font, text);
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
        recalcFontScale();
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (pixel != null) pixel.dispose();
    }
}
