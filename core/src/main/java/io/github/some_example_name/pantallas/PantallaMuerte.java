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

public class PantallaMuerte extends ScreenAdapter {

    // Referencia al objeto principal del juego, necesaria para acceder al batch compartido
    // y para realizar transiciones entre pantallas.
    private final Main juego;

    // Cámara ortográfica para renderizar la interfaz en coordenadas de pantalla.
    private OrthographicCamera camera;

    // Fuente principal utilizada en esta pantalla (título y opciones).
    private BitmapFont font;

    // Utilidad para medir el texto (ancho) antes de dibujarlo centrado.
    private GlyphLayout layout;

    // Factor de escalado aplicado a la fuente para adaptarse a densidad/tamaño de pantalla.
    private float factorEscaladoFuente;

    // Textura de 1x1 píxel usada como “bloque de dibujo” para fondos, paneles y bordes.
    private Texture pixel;

    // Color del fondo sólido (sin captura), definido como constantes para consistencia visual.
    private static final float BG_R = 0.05f;
    private static final float BG_G = 0.05f;
    private static final float BG_B = 0.06f;

    // Índice de la opción seleccionada actualmente (0..ITEM_COUNT-1).
    private int selected = 0;

    // Número total de elementos seleccionables en el menú.
    private static final int ITEM_COUNT = 2;

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático (menú solo táctil en móvil)
    // ------------------------------------------------------------

    // Bandera para distinguir flujo de entrada y comportamiento entre móvil y escritorio.
    private boolean esMovil = false;

    /*
     * Constructor principal: almacena la referencia al juego.
     */
    public PantallaMuerte(Main juego) {
        this.juego = juego;
    }

    /*
     * Constructor de compatibilidad para llamadas antiguas o firmas alternativas.
     * El segundo parámetro no se utiliza, pero evita romper código existente.
     */
    public PantallaMuerte(Main juego, Object ignored) {
        this.juego = juego;
    }

    /*
     * Inicialización de la pantalla:
     * - Detecta plataforma (móvil/escritorio)
     * - Configura cámara
     * - Carga y configura fuentes
     * - Prepara textura pixel para dibujar paneles/fondos
     * - Asegura el sistema de idiomas
     */
    @Override
    public void show() {

        // Determina si la app se está ejecutando en Android o iOS para ajustar controles.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Cámara en coordenadas de pantalla (0..width, 0..height).
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Fuente bitmap utilizada en toda la pantalla.
        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        // Permite posiciones subpíxel para reducir vibraciones/artefactos al dibujar texto.
        font.setUseIntegerPositions(false);

        // Escalado basado en densidad (dpi) y relación viewport/pantalla.
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        // Tamaño de fuente ajustado para legibilidad en esta pantalla.
        font.getData().setScale(factorEscaladoFuente * 1.4f);
        font.setColor(Color.WHITE);

        // Filtro Nearest para mantener estética pixel-art en la textura de la fuente.
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Inicializa el layout para medir texto (centrado).
        layout = new GlyphLayout();

        // Crea una textura blanca 1x1 para dibujar rectángulos escalados (fondo/panel/bordes).
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Asegura carga del bundle/recursos de traducción antes de usarlos.
        Idiomas.get();
    }

    /*
     * Render por frame:
     * - Procesa la entrada
     * - Dibuja fondo sólido
     * - Dibuja panel central opaco con borde
     * - Dibuja título y opciones centradas
     */
    @Override
    public void render(float delta) {
        handleInput();

        // Proyección alineada con la cámara para dibujar en coordenadas de pantalla.
        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Fondo sólido de pantalla completa.
        juego.batch.setColor(BG_R, BG_G, BG_B, 1f);
        juego.batch.draw(pixel, 0f, 0f, screenW, screenH);

        // Panel central con proporciones estilo menú/pausa.
        float panelW = screenW * 0.245f;
        float panelH = screenH * 0.5f;
        float panelX = (screenW - panelW) * 0.5f;
        float panelY = (screenH - panelH) * 0.5f;

        // Fondo del panel (opaco) para máxima legibilidad.
        juego.batch.setColor(0f, 0f, 0f, 1f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde fino del panel para separarlo visualmente del fondo.
        juego.batch.setColor(1f, 1f, 1f, 0.18f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restablece el color del batch para evitar contaminar renders posteriores.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = screenW * 0.5f;

        // Título principal: se pinta en rojo para reforzar el estado de “muerte”.
        String titulo = safeT("death_title", "Has muerto");
        font.setColor(1f, 0.25f, 0.25f, 1f);
        drawCentered(titulo, centerX, panelY + panelH * 0.82f);
        font.setColor(Color.WHITE);

        // Coordenadas base para las opciones (centradas verticalmente dentro del panel).
        float y0 = panelY + panelH * 0.55f;
        float dy = panelH * 0.20f;

        // Renderiza las opciones del menú, destacando la seleccionada.
        drawItem(0, safeT("death_restart", "Reiniciar"), centerX, y0);
        drawItem(1, safeT("death_menu", "Volver al menú"), centerX, y0 - dy);

        juego.batch.end();
    }

    /*
     * Gestión de entrada:
     * - En móvil: interfaz exclusivamente táctil (selección por toque sobre cada opción)
     * - En PC: navegación por teclado (UP/DOWN) y confirmación (ENTER/SPACE), salida (ESC)
     */
    private void handleInput() {

        // ------------------------------------------------------------
        // MÓVIL: SOLO TÁCTIL
        // ------------------------------------------------------------
        if (esMovil) {

            if (Gdx.input.justTouched()) {

                // Coordenadas de pantalla (en píxeles) del toque.
                // Se usan tal cual y luego se convierten a coordenadas del mundo con unproject.
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                // Convierte coordenadas de pantalla a coordenadas del mundo de la cámara.
                Vector3 tmp = new Vector3(sx, sy, 0f);
                camera.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                // Recalcula dimensiones del panel para poder determinar zonas “clicables”.
                float screenW = Gdx.graphics.getWidth();
                float screenH = Gdx.graphics.getHeight();

                float panelW = screenW * 0.245f;
                float panelH = screenH * 0.5f;
                float panelX = (screenW - panelW) * 0.5f;
                float panelY = (screenH - panelH) * 0.5f;

                // Posiciones Y de las opciones (mismas que en render).
                float y0 = panelY + panelH * 0.55f;
                float dy = panelH * 0.20f;
                float y1 = y0 - dy;

                // Comprueba que el toque cae dentro del ancho del panel (zona horizontal válida).
                boolean dentroPanelX = (wx >= panelX && wx <= panelX + panelW);

                // Define un rango vertical aproximado para “hitbox” de cada línea,
                // usando el alto de línea de la fuente como referencia.
                float halfH = font.getLineHeight() * 0.60f;

                // Si el toque cae sobre la primera opción, se ejecuta reiniciar.
                if (dentroPanelX && wy >= y0 - halfH && wy <= y0 + halfH) {
                    selected = 0;
                    reiniciar();
                    return;
                }

                // Si el toque cae sobre la segunda opción, se vuelve al menú.
                if (dentroPanelX && wy >= y1 - halfH && wy <= y1 + halfH) {
                    selected = 1;
                    volverMenu();
                    return;
                }
            }

            // En móvil no se procesan entradas de teclado; se sale del método aquí.
            return;
        }

        // ------------------------------------------------------------
        // PC: TECLADO como hasta ahora
        // ------------------------------------------------------------

        // Navegación entre opciones: alterna la selección con UP/DOWN.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }

        // Confirmación de selección con ENTER o SPACE.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selected == 0) reiniciar();
            else volverMenu();
        }

        // Escape devuelve al menú directamente.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volverMenu();
        }
    }

    /*
     * Reinicia la partida cambiando a la pantalla de juego
     * y liberando los recursos de esta pantalla.
     */
    private void reiniciar() {
        juego.setScreen(new PantallaJuego(juego));
        dispose();
    }

    /*
     * Vuelve al menú principal y libera los recursos de esta pantalla.
     */
    private void volverMenu() {
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    /*
     * Dibuja un item de menú:
     * - Si es el seleccionado, se resalta con prefijo y color
     * - Si no, se dibuja en blanco
     */
    private void drawItem(int idx, String text, float centerX, float y) {
        String line = (idx == selected ? "> " : "") + text;

        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        drawCentered(line, centerX, y);
        // Restablece el color para evitar que afecte al siguiente render.
        font.setColor(Color.WHITE);
    }

    /*
     * Dibuja un texto centrado horizontalmente respecto a centerX.
     * Se mide el ancho con GlyphLayout para calcular el offset X.
     */
    private void drawCentered(String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    /*
     * Traducción segura:
     * Intenta obtener el texto por clave; si falla (por recurso ausente o error),
     * devuelve el fallback proporcionado para evitar romper el render.
     */
    private String safeT(String key, String fallback) {
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    /*
     * Ajusta la cámara cuando cambia el tamaño de ventana.
     * En esta pantalla no se recalcula el escalado de fuente aquí.
     */
    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
    }

    /*
     * Libera recursos gráficos para evitar fugas de memoria.
     */
    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (pixel != null) pixel.dispose();
    }
}
