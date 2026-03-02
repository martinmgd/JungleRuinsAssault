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

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Idiomas;
import io.github.some_example_name.utilidades.Records;

public class PantallaVictoria extends ScreenAdapter {

    // Referencia al objeto principal del juego. Permite acceder al SpriteBatch
    // y realizar transiciones entre pantallas.
    private final Main juego;

    // Puntuación final obtenida al terminar el juego.
    // Se utiliza para determinar si el jugador entra en la tabla de récords.
    private final int scoreFinal;

    // Indicador utilizado para asegurar que la comprobación de récord
    // solo se ejecuta una vez al entrar en la pantalla.
    private boolean recordYaGestionado = false;

    // Cámara ortográfica configurada en coordenadas de pantalla.
    private OrthographicCamera camera;

    // Fuente utilizada para renderizar el texto de la interfaz.
    private BitmapFont font;

    // Objeto auxiliar para calcular dimensiones del texto.
    private GlyphLayout layout;

    // Factor de escalado aplicado a la fuente en función de la densidad
    // de la pantalla y del tamaño del viewport.
    private float factorEscaladoFuente;

    // Textura 1x1 utilizada como base para dibujar rectángulos mediante escalado.
    private Texture pixel;

    // Color de fondo sólido para la pantalla de victoria.
    private static final float BG_R = 0.05f;
    private static final float BG_G = 0.06f;
    private static final float BG_B = 0.05f;

    // Índice del elemento actualmente seleccionado en el menú.
    private int selected = 0;

    // Número total de elementos del menú.
    private static final int ITEM_COUNT = 1;

    // Determina si la aplicación se está ejecutando en una plataforma móvil.
    private boolean esMovil = false;

    // Constructor utilizado cuando no se proporciona puntuación final.
    public PantallaVictoria(Main juego) {
        this.juego = juego;
        this.scoreFinal = 0;
    }

    // Constructor principal que recibe la puntuación final del jugador.
    public PantallaVictoria(Main juego, int scoreFinal) {
        this.juego = juego;
        this.scoreFinal = scoreFinal;
    }

    // Constructor de compatibilidad para llamadas que utilicen dos parámetros.
    public PantallaVictoria(Main juego, Object ignored) {
        this.juego = juego;
        this.scoreFinal = 0;
    }

    @Override
    public void show() {

        // Detección de plataforma para activar controles táctiles en dispositivos móviles.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Inicialización de la cámara en coordenadas de pantalla.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Carga de la fuente bitmap utilizada para el texto.
        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        font.setUseIntegerPositions(false);

        // Cálculo del factor de escalado en función de la densidad de pantalla.
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        font.getData().setScale(factorEscaladoFuente * 1.4f);
        font.setColor(Color.WHITE);

        // Configuración del filtrado de la textura de la fuente para mantener
        // la estética pixel-art.
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        layout = new GlyphLayout();

        // Creación de una textura 1x1 blanca utilizada para dibujar rectángulos.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Inicialización del sistema de localización.
        Idiomas.get();

        // Evaluación de la puntuación para determinar si se debe mostrar
        // la pantalla de introducción de récords.
        if (!recordYaGestionado && scoreFinal > 0) {
            recordYaGestionado = true;

            try {
                if (Records.qualifies(scoreFinal)) {
                    juego.setScreen(new PantallaNuevoRecord(juego, scoreFinal, PantallaNuevoRecord.Destino.VICTORIA));
                    dispose();
                    return;
                }
            } catch (Exception ignored) {
                // En caso de error durante la comprobación de récords,
                // se continúa mostrando la pantalla de victoria.
            }
        }
    }

    @Override
    public void render(float delta) {

        // Procesamiento de entrada del usuario.
        handleInput();

        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Renderizado del fondo sólido.
        juego.batch.setColor(BG_R, BG_G, BG_B, 1f);
        juego.batch.draw(pixel, 0f, 0f, screenW, screenH);

        // Dimensiones y posición del panel central.
        float panelW = screenW * 0.245f;
        float panelH = screenH * 0.5f;
        float panelX = (screenW - panelW) * 0.5f;
        float panelY = (screenH - panelH) * 0.5f;

        // Renderizado del fondo del panel.
        juego.batch.setColor(0f, 0f, 0f, 1f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Renderizado del borde del panel.
        juego.batch.setColor(1f, 1f, 1f, 0.18f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = screenW * 0.5f;

        // Renderizado del título localizado.
        String titulo = safeT("victory_title", "Felicidades!!!");
        font.setColor(0.30f, 1f, 0.30f, 1f);
        drawCentered(titulo, centerX, panelY + panelH * 0.82f);
        font.setColor(Color.WHITE);

        // Renderizado del único elemento del menú.
        float y0 = panelY + panelH * 0.55f;

        drawItem(0, safeT("victory_menu", "Volver al menu"), centerX, y0);

        juego.batch.end();
    }

    private void handleInput() {

        // Entrada táctil en plataformas móviles.
        if (esMovil) {

            if (Gdx.input.justTouched()) {

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

                float y0 = panelY + panelH * 0.55f;

                boolean dentroPanelX = (wx >= panelX && wx <= panelX + panelW);

                float halfH = font.getLineHeight() * 0.60f;

                if (dentroPanelX && wy >= y0 - halfH && wy <= y0 + halfH) {
                    selected = 0;
                    volverMenu();
                    return;
                }
            }

            return;
        }

        // Entrada mediante teclado en PC.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            volverMenu();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volverMenu();
        }
    }

    // Transición al menú principal.
    private void volverMenu() {
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    // Renderiza un elemento del menú con indicador visual de selección.
    private void drawItem(int idx, String text, float centerX, float y) {
        String line = (idx == selected ? "> " : "") + text;

        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        drawCentered(line, centerX, y);
        font.setColor(Color.WHITE);
    }

    // Renderiza texto centrado horizontalmente.
    private void drawCentered(String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    // Obtiene una cadena localizada con fallback en caso de error.
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
    }
}
