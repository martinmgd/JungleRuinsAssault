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

    // Referencia al contenedor principal del juego, usada para cambiar de pantalla y renderizar con el batch compartido.
    private final Main juego;

    // Referencia al estado de juego que se está pausando. Se renderiza detrás para dar efecto de "juego congelado".
    private final PantallaJuego juegoPausado;

    // Cámara ortográfica para dibujar la UI en coordenadas de pantalla.
    private OrthographicCamera camera;

    // Fuentes separadas para título y elementos del menú, permitiendo distintos tamaños/jerarquía visual.
    private BitmapFont fontTitle;
    private BitmapFont fontItem;

    // Utilidad para medir texto y poder centrarlo correctamente.
    private GlyphLayout layout;

    // Factor de escalado para ajustar fuentes según densidad y tamaño real de pantalla.
    private float factorEscaladoFuente;

    // Textura 1x1 usada como “píxel” para dibujar rectángulos escalados (panel, bordes, etc.).
    private Texture pixel;

    // Índice del elemento actualmente seleccionado (0..2).
    private int seleccionado = 0;

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático
    // ------------------------------------------------------------

    // Bandera para separar flujo de entrada y comportamiento entre móvil y escritorio.
    private boolean esMovil = false;

    /*
     * Constructor:
     * - juego: referencia al motor/launcher principal
     * - juegoPausado: pantalla de juego actual que se queda congelada de fondo
     */
    public PantallaPausa(Main juego, PantallaJuego juegoPausado) {
        this.juego = juego;
        this.juegoPausado = juegoPausado;
    }

    /*
     * Inicialización de la pantalla de pausa:
     * - Pausa la música del nivel y marca el juego como "en pausa"
     * - Detecta si se ejecuta en móvil
     * - Prepara cámara, fuentes y textura auxiliar
     * - Asegura el bundle de idiomas
     */
    @Override
    public void show() {

        // Pausa la música del nivel al entrar en la pantalla de pausa.
        if (juegoPausado != null) juegoPausado.pauseMusicaNivel();

        // Marca el juego como "en pausa" para evitar re-sincronizaciones que reactiven música.
        if (juegoPausado != null) juegoPausado.setEnPausa(true);

        // Determina si el runtime es Android o iOS para activar el esquema táctil.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Asegura que el sistema de traducciones esté cargado antes de utilizar claves.
        Idiomas.get();

        // Configuración de la cámara en coordenadas de pantalla.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Carga de fuentes bitmap para título e items.
        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontItem = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Calcula escalado basado en densidad (dpi) y viewport.
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        // Ajusta tamaños relativos para jerarquía tipográfica (título más grande que items).
        fontTitle.getData().setScale(factorEscaladoFuente * 2f);
        fontItem.getData().setScale(factorEscaladoFuente * 1.4f);

        // Color base de las fuentes.
        fontTitle.setColor(Color.WHITE);
        fontItem.setColor(Color.WHITE);

        // Filtro Nearest para estética pixel-art y evitar suavizados no deseados.
        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontItem.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Layout para mediciones de texto.
        layout = new GlyphLayout();

        // Textura blanca 1x1 para dibujar panel/bordes mediante escalado.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    /*
     * Render por frame:
     * - Dibuja el juego pausado detrás para obtener un overlay real con transparencia
     * - Fuerza la música a permanecer en pausa (seguridad contra re-activaciones)
     * - Procesa entrada táctil (móvil) o teclado (PC)
     * - Dibuja panel de pausa, título y opciones centradas
     */
    @Override
    public void render(float delta) {

        // Renderiza el juego congelado detrás. Se usa delta=0 para evitar avance de lógica.
        juegoPausado.render(0);

        // Refuerza el estado de pausa de la música por si el render del juego re-sincroniza audio.
        if (juegoPausado != null) juegoPausado.pauseMusicaNivel();

        // ------------------------------------------------------------
        // MÓVIL: TÁCTIL (tocar una opción)
        // ------------------------------------------------------------
        if (esMovil && Gdx.input.justTouched()) {

            float screenW = Gdx.graphics.getWidth();
            float screenH = Gdx.graphics.getHeight();

            // Geometría del panel: ancho reducido y altura fija relativa.
            float panelW = screenW * 0.245f;
            float panelH = screenH * 0.5f;

            float panelX = (screenW - panelW) * 0.5f;
            float panelY = (screenH - panelH) * 0.5f;

            // Coordenadas del toque convertidas al sistema de coordenadas usado para dibujar (Y invertida).
            float wx = Gdx.input.getX();
            float wy = screenH - Gdx.input.getY();

            float centerX = screenW * 0.5f;

            // Coordenadas base de los items centrados.
            float y0 = panelY + panelH * 0.55f;
            float dy = panelH * 0.18f;

            float yContinue = y0;
            float yRestart = y0 - dy;
            float yMenu = y0 - dy * 2f;

            // Margen vertical para tolerancia de toque en cada opción.
            float halfH = dy * 0.55f;

            // Valida que el toque cae dentro del ancho del panel.
            boolean dentroPanelX = (wx >= panelX && wx <= panelX + panelW);

            if (dentroPanelX) {
                if (wy >= yContinue - halfH && wy <= yContinue + halfH) {
                    seleccionado = 0;

                    // Desmarca estado de pausa antes de regresar a juego.
                    if (juegoPausado != null) juegoPausado.setEnPausa(false);

                    // Reanuda la música del nivel al volver al juego.
                    if (juegoPausado != null) juegoPausado.resumeMusicaNivel();

                    // Vuelve al juego pausado.
                    juego.setScreen(juegoPausado);
                    return;

                } else if (wy >= yRestart - halfH && wy <= yRestart + halfH) {
                    seleccionado = 1;

                    // Reiniciar: detiene la música del nivel explícitamente porque hide() no se ejecuta aquí.
                    if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                    // Crea una nueva instancia del juego para reiniciar estado.
                    juego.setScreen(new PantallaJuego(juego));
                    return;

                } else if (wy >= yMenu - halfH && wy <= yMenu + halfH) {
                    seleccionado = 2;

                    // Menú: detiene la música del nivel explícitamente porque hide() no se ejecuta aquí.
                    if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                    // Cambia al menú principal.
                    juego.setScreen(new PantallaMenu(juego));
                    return;
                }
            }
        }

        // Salida rápida de pausa por teclado: ESC o P.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)) {

            // Desmarca estado de pausa antes de volver a juego.
            if (juegoPausado != null) juegoPausado.setEnPausa(false);

            // Reanuda música del nivel.
            if (juegoPausado != null) juegoPausado.resumeMusicaNivel();

            // Regresa al juego.
            juego.setScreen(juegoPausado);
            return;
        }

        // Navegación hacia arriba (cíclica) por teclado.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            seleccionado = (seleccionado + 3 - 1) % 3;
        }

        // Navegación hacia abajo (cíclica) por teclado.
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            seleccionado = (seleccionado + 1) % 3;
        }

        // Confirmación por teclado: ENTER ejecuta la acción según el elemento seleccionado.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (seleccionado == 0) {

                // Desmarca estado de pausa antes de continuar.
                if (juegoPausado != null) juegoPausado.setEnPausa(false);

                // Reanuda música del nivel.
                if (juegoPausado != null) juegoPausado.resumeMusicaNivel();

                // Continúa el juego.
                juego.setScreen(juegoPausado);

            } else if (seleccionado == 1) {
                // Reiniciar: detiene música del nivel explícitamente.
                if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                // Reinicia creando un nuevo juego.
                juego.setScreen(new PantallaJuego(juego));

            } else {
                // Menú: detiene música del nivel explícitamente.
                if (juegoPausado != null) juegoPausado.stopMusicaNivel();

                // Cambia al menú principal.
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        // Render de la UI de pausa (panel + textos).
        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Geometría del panel (mismo cálculo que en la sección táctil).
        float panelW = screenW * 0.245f;
        float panelH = screenH * 0.5f;

        float panelX = (screenW - panelW) * 0.5f;
        float panelY = (screenH - panelH) * 0.5f;

        // Fondo del panel con transparencia para dejar ver el juego detrás.
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil del panel para legibilidad y separación.
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restablece el color para evitar contaminación en texto u otros draws.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = screenW * 0.5f;

        // Título de la pantalla de pausa, centrado.
        drawCentered(fontTitle, safeT("pause_title", "Pausa"), centerX, panelY + panelH * 0.82f);

        // Coordenadas base de items, centrados en el panel.
        float y0 = panelY + panelH * 0.55f;
        float dy = panelH * 0.18f;

        // Render de opciones. Se marca la selección con prefijo textual.
        drawCentered(fontItem, (seleccionado == 0 ? "> " : "") + safeT("pause_continue", "Continuar"), centerX, y0);
        drawCentered(fontItem, (seleccionado == 1 ? "> " : "") + safeT("pause_restart", "Reiniciar"), centerX, y0 - dy);
        drawCentered(fontItem, (seleccionado == 2 ? "> " : "") + safeT("pause_menu", "Menú"), centerX, y0 - dy * 2f);

        juego.batch.end();
    }

    /*
     * Dibuja texto centrado horizontalmente en centerX, calculando el offset X según el ancho medido.
     */
    private void drawCentered(BitmapFont font, String text, float centerX, float y) {
        layout.setText(font, text);
        float x = centerX - layout.width / 2f;
        font.draw(juego.batch, layout, x, y);
    }

    /*
     * Traducción segura:
     * - Intenta obtener la traducción por clave
     * - Si hay error o no existe, usa fallback para evitar fallos en runtime
     */
    private String safeT(String key, String fallback) {
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    /*
     * Ajuste al redimensionar ventana:
     * - Reconfigura la cámara para el nuevo tamaño
     */
    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
    }

    /*
     * Liberación de recursos gráficos (fuentes y textura auxiliar).
     */
    @Override
    public void dispose() {
        if (fontTitle != null) fontTitle.dispose();
        if (fontItem != null) fontItem.dispose();
        if (pixel != null) pixel.dispose();
    }
}
