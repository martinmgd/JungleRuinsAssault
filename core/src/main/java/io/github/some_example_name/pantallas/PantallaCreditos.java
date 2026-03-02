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
import io.github.some_example_name.utilidades.UiEscala;

/*
 * Pantalla de créditos del juego.
 *
 * Esta clase se encarga de mostrar una interfaz simple con:
 * - Un panel central
 * - Un listado de créditos
 * - Un botón para volver al menú principal
 *
 * Se adapta automáticamente a dispositivos móviles o escritorio.
 */
public class PantallaCreditos extends ScreenAdapter {

    // Referencia al objeto principal del juego
    private final Main juego;

    // Cámara ortográfica utilizada para renderizar la escena
    private OrthographicCamera camera;

    // Fuentes utilizadas en la interfaz
    private BitmapFont fontTitle;
    private BitmapFont fontBody;
    private BitmapFont fontBack;

    // Objeto auxiliar para medir texto antes de dibujarlo
    private GlyphLayout layout;

    // Textura de 1x1 píxel usada para dibujar paneles, bordes y fondos
    private Texture pixel;

    // Indica si la aplicación está ejecutándose en móvil
    private boolean esMovil = false;

    // Variables que definen la posición y tamaño del botón "volver"
    private float btnX, btnY, btnW, btnH;

    // Factor de escalado de fuente que se recalcula en resize
    private float factorEscaladoFuente = 1f;

    /*
     * Constructor de la pantalla.
     * Recibe una referencia al juego principal para poder cambiar de pantalla
     * y utilizar el SpriteBatch compartido.
     */
    public PantallaCreditos(Main juego) {
        this.juego = juego;
    }

    /*
     * Método ejecutado cuando esta pantalla se muestra por primera vez.
     * Inicializa cámara, fuentes, layout y texturas auxiliares.
     */
    @Override
    public void show() {

        // Detecta si la aplicación se está ejecutando en un dispositivo móvil
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Asegura que el sistema de idiomas esté cargado
        Idiomas.get();

        // Inicialización de la cámara ortográfica
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Carga de fuentes desde archivo
        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontBody  = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontBack  = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Permite posicionamiento subpixel para evitar artefactos visuales
        fontTitle.setUseIntegerPositions(false);
        fontBody.setUseIntegerPositions(false);
        fontBack.setUseIntegerPositions(false);

        // Recalcula el escalado inicial de fuentes
        recalcFontScale();

        // Colores base de las fuentes
        fontTitle.setColor(Color.WHITE);
        fontBody.setColor(Color.WHITE);
        fontBack.setColor(Color.WHITE);

        // Uso de filtro Nearest para mantener apariencia pixel-art
        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontBody.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontBack.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Inicialización del objeto para medir texto
        layout = new GlyphLayout();

        // Creación de una textura blanca de 1x1 píxel para dibujar formas simples
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    /*
     * Recalcula el escalado de las fuentes en función del tamaño de pantalla
     * y del factor global de escala de la interfaz.
     */
    private void recalcFontScale() {
        if (camera == null || fontTitle == null || fontBody == null || fontBack == null) return;

        float worldToScreen = (camera.viewportHeight / (float) Gdx.graphics.getHeight());
        factorEscaladoFuente = worldToScreen * UiEscala.uiScale();

        // Ajuste de tamaño relativo de cada fuente
        fontTitle.getData().setScale(factorEscaladoFuente * 1.60f);
        // Subida mínima equivalente a "1 punto" aprox (12 -> 13)
        fontBody.getData().setScale(factorEscaladoFuente * 1.08f);
        fontBack.getData().setScale(factorEscaladoFuente * 1.10f);
    }

    /*
     * Método de renderizado ejecutado cada frame.
     * Se encarga de:
     * - procesar entrada
     * - dibujar fondo
     * - dibujar panel
     * - renderizar texto
     * - renderizar botón
     */
    @Override
    public void render(float delta) {
        handleInput();

        camera.update();
        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Fondo negro de pantalla completa
        juego.batch.setColor(0f, 0f, 0f, 1f);
        juego.batch.draw(pixel, 0f, 0f, w, h);

        // Dimensiones del panel central
        float panelW = w * 0.80f;
        float panelH = h * 0.82f;
        float panelX = (w - panelW) * 0.5f;
        float panelY = (h - panelH) * 0.5f;

        // Fondo semitransparente del panel
        juego.batch.setColor(1f, 1f, 1f, 0.08f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Dibujado de bordes del panel
        juego.batch.setColor(1f, 1f, 1f, 0.14f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = w * 0.5f;

        // Título de la pantalla de créditos
        String titulo = safeT("credits_title", "Creditos");
        fontTitle.setColor(0.25f, 0.9f, 1f, 1f);
        drawCentered(fontTitle, titulo, centerX, panelY + panelH * 0.90f);
        fontTitle.setColor(Color.WHITE);

        // Lista de líneas de créditos
        String[] lines = new String[] {
            safeT("credits_line_1", "Juego creado por Martin Miguens"),
            safeT("credits_line_2", "Fondo adquirido en itch.io."),
            safeT("credits_line_3", "Sonidos obtenidos en Pixabay."),
            safeT("credits_line_4", "Gracias a ChatGPT por ayudarme y ensenarme tanto con codigo e imagenes."),
            safeT("credits_line_5", "Gracias a Grok por los vídeos de los que extrai frames para animaciones."),
            safeT("credits_line_6", "Gracias a Diego Santos por su contribucion al proyecto."),
            safeT("credits_line_6", "Gracias a Alex Alvarez por mostrarme la musica de la pantalla del jefe."),
            safeT("credits_line_6", "Gracias por jugar.")
        };

        float y = panelY + panelH * 0.78f;
        float line = fontBody.getLineHeight() * 1.15f;

        fontBody.setColor(1f, 1f, 1f, 0.88f);

        // Posición izquierda para dibujar las líneas
        float leftX = panelX + panelW * 0.08f;
        for (String s : lines) {
            drawLeft(fontBody, s, leftX, y);
            y -= line;
        }
        fontBody.setColor(Color.WHITE);

        // Cálculo de posición y tamaño del botón
        computeButton(panelX, panelY, panelW, panelH);

        String volverTxt = safeT("credits_back", "Toca para volver al menu");

        // Fondo del botón
        juego.batch.setColor(0f, 0f, 0f, 0.45f);
        juego.batch.draw(pixel, btnX, btnY, btnW, btnH);

        // Bordes del botón
        juego.batch.setColor(1f, 1f, 1f, 0.18f);
        juego.batch.draw(pixel, btnX, btnY, btnW, b);
        juego.batch.draw(pixel, btnX, btnY + btnH - b, btnW, b);
        juego.batch.draw(pixel, btnX, btnY, b, btnH);
        juego.batch.draw(pixel, btnX + btnW - b, btnY, b, btnH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Texto del botón
        fontBack.setColor(1f, 0.85f, 0.2f, 1f);
        drawCentered(fontBack, "> " + volverTxt, centerX, btnY + btnH * 0.65f);
        fontBack.setColor(Color.WHITE);

        juego.batch.end();
    }

    /*
     * Gestión de entrada del usuario.
     * - En móvil: detecta toque dentro del botón
     * - En PC: detecta teclas ESC, ENTER o SPACE
     */
    private void handleInput() {

        if (esMovil) {

            if (Gdx.input.justTouched()) {

                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                Vector3 tmp = new Vector3(sx, sy, 0f);
                camera.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                if (wx >= btnX && wx <= btnX + btnW &&
                    wy >= btnY && wy <= btnY + btnH) {
                    volverMenu();
                }
            }

            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            volverMenu();
        }
    }

    /*
     * Cambia la pantalla actual al menú principal
     * y libera los recursos de esta pantalla.
     */
    private void volverMenu() {
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    /*
     * Calcula tamaño y posición del botón dentro del panel.
     */
    private void computeButton(float panelX, float panelY, float panelW, float panelH) {
        btnW = panelW * 0.55f;
        btnH = panelH * 0.11f;
        btnX = panelX + (panelW - btnW) * 0.5f;

        btnY = panelY + panelH * 0.02f;
    }

    /*
     * Dibuja texto centrado horizontalmente respecto a una coordenada X.
     */
    private void drawCentered(BitmapFont font, String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    /*
     * Dibuja texto alineado a la izquierda.
     */
    private void drawLeft(BitmapFont font, String text, float x, float y) {
        layout.setText(font, text);
        font.draw(juego.batch, layout, x, y);
    }

    /*
     * Obtiene una cadena traducida desde el sistema de idiomas.
     * Si ocurre cualquier error se devuelve un texto de respaldo.
     */
    private String safeT(String key, String fallback) {
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    /*
     * Método llamado cuando cambia el tamaño de la ventana.
     * Reajusta la cámara y recalcula el tamaño de las fuentes.
     */
    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
        recalcFontScale();
    }

    /*
     * Libera los recursos gráficos utilizados por esta pantalla.
     */
    @Override
    public void dispose() {
        if (fontTitle != null) fontTitle.dispose();
        if (fontBody != null) fontBody.dispose();
        if (fontBack != null) fontBack.dispose();
        if (pixel != null) pixel.dispose();
    }
}
