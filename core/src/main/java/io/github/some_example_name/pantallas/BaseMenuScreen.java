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
 * Pantalla base para menús.
 *
 * Responsabilidades principales:
 * - Configurar cámara + viewport (FitViewport) para mantener proporción estable en distintas resoluciones.
 * - Proveer una estética común: fondo limpio, panel semitransparente centrado y borde sutil.
 * - Gestionar input común de navegación: UP/DOWN + LEFT/RIGHT + ENTER + ESC.
 * - Proveer helpers de layout (posiciones relativas al panel) y de dibujo (texto centrado con sombra, items).
 *
 * Las pantallas concretas heredan de esta clase e implementan:
 * - drawMenu(): contenido del menú (título, items, hints, etc.).
 * - getItemsCount(): número de opciones navegables.
 * - onEnter(): acción al confirmar una opción.
 *
 * Opcionalmente pueden implementar:
 * - onEscape(): acción al pulsar ESC.
 * - onLeft()/onRight(): acción en opciones laterales (por ejemplo, cambiar dificultad).
 */
public abstract class BaseMenuScreen extends ScreenAdapter {

    // Referencia al objeto principal del juego (contiene batch, setScreen, etc.).
    protected final Main juego;

    // Cámara ortográfica para render 2D.
    protected OrthographicCamera camera;

    // Viewport que gestiona escalado y letterboxing según resolución (mantiene relación de aspecto).
    protected Viewport viewport;

    // Fuente para título y para items del menú.
    protected BitmapFont fontTitle;
    protected BitmapFont fontItem;

    // Objeto de layout reutilizable para medir texto (ancho/alto) sin crear instancias por frame.
    protected GlyphLayout layout = new GlyphLayout();

    // Factor de escalado de fuentes (relaciona mundo vs pantalla y densidad).
    protected float factorEscaladoFuente;

    // Textura 1x1 usada como “pixel” para dibujar rectángulos escalados:
    // panel, bordes, highlights, etc.
    protected Texture pixel;

    // Dimensiones y posición del panel centrado (en coordenadas del mundo).
    protected float panelW;
    protected float panelH;
    protected float panelX;
    protected float panelY;

    // Índice seleccionado actualmente dentro de los items del menú.
    protected int selectedIndex = 0;

    /*
     * Constructor base: requiere la instancia principal del juego.
     */
    public BaseMenuScreen(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        // Inicializa cámara y viewport con dimensiones de mundo definidas por Constantes.
        camera = new OrthographicCamera();
        viewport = new FitViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Carga de fuentes desde assets.
        // Se usan dos instancias para permitir escalados/colores distintos si se requiere.
        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontItem = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Escala basada en densidad de pantalla (dpi) y en relación mundo/pantalla.
        // Esto intenta mantener tamaño visual consistente entre dispositivos.
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        // Escalado base de las fuentes.
        // El título se dibuja más grande que los items.
        fontTitle.getData().setScale(factorEscaladoFuente * 2.0f);
        fontItem.getData().setScale(factorEscaladoFuente * 1.4f);

        // Filtro nearest para conservar estética pixel-art (sin suavizado).
        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontItem.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Crea una textura blanca 1x1 para dibujar rectángulos con batch.draw().
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Calcula el panel centrado en el mundo.
        // Los factores definen la “caja” visual común de todos los menús.
        panelW = Constantes.ANCHO_MUNDO * 0.72f;
        panelH = Constantes.ALTO_MUNDO * 0.62f;
        panelX = (Constantes.ANCHO_MUNDO - panelW) * 0.5f;
        panelY = (Constantes.ALTO_MUNDO - panelH) * 0.5f;
    }

    @Override
    public void resize(int width, int height) {
        // Ajusta el viewport a la nueva resolución, manteniendo el mundo.
        if (viewport != null) viewport.update(width, height, true);

        // Recalcula el factor de escalado de fuente por si cambian dimensiones/densidad.
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        // Reaplica escalas a las fuentes si existen.
        if (fontTitle != null) fontTitle.getData().setScale(factorEscaladoFuente * 2.0f);
        if (fontItem != null) fontItem.getData().setScale(factorEscaladoFuente * 1.4f);
    }

    @Override
    public void render(float delta) {
        // Limpia pantalla antes de dibujar (fondo negro sólido).
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Aplica viewport (ajusta glViewport) y actualiza cámara.
        viewport.apply();
        camera.update();

        SpriteBatch batch = juego.batch;
        batch.setProjectionMatrix(camera.combined);

        // Restaura el color del batch para evitar que se hereden tintes/alpha de otras pantallas.
        batch.setColor(1f, 1f, 1f, 1f);

        batch.begin();

        // Dibuja panel semitransparente centrado.
        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil del panel.
        // El grosor se calcula en función de panelH para escalar con el tamaño del panel.
        batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(0.04f, panelH * 0.01f);
        batch.draw(pixel, panelX, panelY, panelW, b);
        batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        batch.draw(pixel, panelX, panelY, b, panelH);
        batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Devuelve el color a blanco opaco para el contenido del menú.
        batch.setColor(1f, 1f, 1f, 1f);

        // Dibuja el contenido específico del menú (implementado por la subclase).
        drawMenu(delta);

        batch.end();

        // Gestiona input común (teclado) una vez por frame.
        handleBaseInput();
    }

    /**
     * Dibuja el menú concreto (título + opciones + hints, etc.).
     * Debe ser implementado por cada pantalla derivada.
     */
    protected abstract void drawMenu(float delta);

    /**
     * Devuelve cuántos items tiene el menú para navegación cíclica.
     */
    protected abstract int getItemsCount();

    /**
     * Acción al pulsar ENTER sobre el índice seleccionado.
     */
    protected abstract void onEnter(int index);

    /**
     * Acción por defecto al pulsar ESC.
     * Se deja vacía para que cada menú decida si vuelve, cierra, etc.
     */
    protected void onEscape() {}

    /**
     * Acción por defecto al pulsar LEFT sobre el índice seleccionado.
     * Útil para opciones de tipo selector (dificultad, volumen, etc.).
     */
    protected void onLeft(int index) {}

    /**
     * Acción por defecto al pulsar RIGHT sobre el índice seleccionado.
     * Útil para opciones de tipo selector (dificultad, volumen, etc.).
     */
    protected void onRight(int index) {}

    /*
     * Gestión común del input de menús (PC):
     * - ESC: delega en onEscape()
     * - UP/DOWN: navegación vertical (con wrap)
     * - LEFT/RIGHT: ajustes laterales por opción
     * - ENTER: confirma opción seleccionada
     */
    protected void handleBaseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) onEscape();

        // Navegación vertical.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            moveSelection(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            moveSelection(+1);
        }

        // Cambios laterales para opciones configurables.
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            onLeft(selectedIndex);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            onRight(selectedIndex);
        }

        // Confirmación de la opción seleccionada.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) onEnter(selectedIndex);
    }

    /*
     * Mueve la selección vertical con wrap-around:
     * - Si baja de 0, salta al último.
     * - Si supera count-1, vuelve a 0.
     */
    protected void moveSelection(int dir) {
        int count = getItemsCount();
        if (count <= 0) return;

        selectedIndex += dir;
        if (selectedIndex < 0) selectedIndex = count - 1;
        if (selectedIndex >= count) selectedIndex = 0;
    }

    // ------------------------------------------------------------------
    // HELPERS DE LAYOUT (POSICIONES RELATIVAS AL PANEL)
    // ------------------------------------------------------------------

    // Devuelve el centro X del panel para dibujar contenido centrado.
    protected float panelCenterX() { return panelX + panelW * 0.5f; }

    // Devuelve una Y dentro del panel por fracción desde abajo (0..1).
    protected float yInPanel(float fracFromBottom) { return panelY + panelH * fracFromBottom; }

    // ------------------------------------------------------------------
    // HELPERS DE DIBUJO
    // ------------------------------------------------------------------

    /*
     * Dibuja un texto centrado horizontalmente en centerX, con sombra.
     * shadowOffset controla el desplazamiento de la sombra en unidades de mundo.
     *
     * El método conserva el color actual del batch (r,g,b,a) para no alterar el flujo de tintes.
     */
    protected void drawCenteredTextShadow(BitmapFont font, String text, float centerX, float y, float shadowOffset) {
        float r = juego.batch.getColor().r;
        float g = juego.batch.getColor().g;
        float b = juego.batch.getColor().b;
        float a = juego.batch.getColor().a;

        // Mide el texto para calcular el X que lo centra.
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;

        // Sombra (negra con alpha alto).
        juego.batch.setColor(0f, 0f, 0f, 0.70f);
        font.draw(juego.batch, layout, x + shadowOffset, y - shadowOffset);

        // Texto principal restaurando el color previo del batch.
        juego.batch.setColor(r, g, b, a);
        font.draw(juego.batch, layout, x, y);
    }

    /*
     * Dibuja un item del menú.
     * Si está seleccionado:
     * - dibuja un rectángulo de highlight suave detrás del texto
     * - prefija el texto con "> "
     *
     * x,y se interpretan como coordenadas de dibujo de la fuente (baseline).
     */
    protected void drawMenuItem(String text, float x, float y, boolean selected) {
        layout.setText(fontItem, text);

        if (selected) {
            // Padding del highlight basado en tamaño del panel para mantener proporción visual.
            float padX = panelW * 0.02f;
            float padY = panelH * 0.035f;

            // Dibuja un highlight translúcido detrás del texto.
            juego.batch.setColor(1f, 1f, 1f, 0.08f);
            juego.batch.draw(pixel,
                x - padX,
                y - layout.height - padY * 0.35f,
                layout.width + padX * 2f,
                layout.height + padY
            );

            // Restaura color para dibujar texto sin tintes.
            juego.batch.setColor(1f, 1f, 1f, 1f);

            // Indicador de selección con prefijo.
            fontItem.draw(juego.batch, "> " + text, x, y);
        } else {
            // Item no seleccionado, sin highlight ni prefijo.
            fontItem.draw(juego.batch, text, x, y);
        }
    }

    @Override
    public void dispose() {
        // Liberación de recursos gráficos asociados a esta base (fuentes y textura pixel).
        if (fontTitle != null) fontTitle.dispose();
        if (fontItem != null) fontItem.dispose();
        if (pixel != null) pixel.dispose();
    }
}
