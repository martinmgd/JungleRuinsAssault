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

/**
 * Pantalla de ayuda con explicación corta de controles (PC y móvil).
 *
 * <p>Todo el texto es traducible mediante {@link Idiomas}.</p>
 * <p>En móvil muestra imágenes de los controles y una descripción al lado.</p>
 * <p>Incluye opción inferior para volver al menú.</p>
 */
public class PantallaAyuda extends ScreenAdapter {

    // Referencia a la clase principal del juego para acceder al SpriteBatch y cambiar de pantalla.
    private final Main juego;

    // Cámara ortográfica configurada en coordenadas de pantalla (píxeles) para dibujar UI 2D.
    private OrthographicCamera camera;

    // Fuente para dibujar el texto y layout para medir ancho/alto del texto antes de renderizar.
    private BitmapFont font;
    private GlyphLayout layout;

    // Textura 1x1 usada como "pixel" para dibujar rectángulos (fondos, paneles y bordes) escalándola.
    private Texture pixel;

    // Flag para distinguir entre dispositivos móviles y escritorio (PC).
    private boolean esMovil = false;

    // Imágenes controles (móvil): se renderizan en pantalla junto con su descripción.
    private Texture imgJoystick;
    private Texture imgDisparo;
    private Texture imgEspecial;
    private Texture imgPausa;

    // Indica si las imágenes se han cargado correctamente para poder renderizarlas.
    private boolean imgsOk = false;

    // Factor de escalado de fuente recalculable en resize para mantener legibilidad entre resoluciones.
    private float factorEscaladoFuente = 1f;

    // Área del botón "volver" (en coordenadas de mundo/pantalla según la cámara).
    private float btnX, btnY, btnW, btnH;

    /**
     * Crea la pantalla de ayuda.
     * @param juego instancia principal.
     */
    public PantallaAyuda(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {

        // Detecta si se ejecuta en Android o iOS para decidir el modo de ayuda a mostrar.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Configura la cámara con un viewport del tamaño actual de la ventana en píxeles.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Carga la fuente desde assets; se desactiva el posicionamiento entero para evitar "temblores" al escalar.
        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        font.setUseIntegerPositions(false);

        // Ajuste de escala para que el texto se vea consistente entre resoluciones/dispositivos.
        // Esta pantalla dibuja en coordenadas de pantalla (viewport en píxeles), por lo que
        // el factor base equivale a (viewportHeight / screenHeight) y se aplica UiEscala.
        recalcFontScale();

        // Color por defecto de la fuente para el texto general.
        font.setColor(Color.WHITE);

        // Filtro nearest para mantener estilo pixel-art/retro sin suavizado.
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Layout para calcular el tamaño del texto y posicionarlo correctamente.
        layout = new GlyphLayout();

        // Crea una textura 1x1 blanca para dibujar rectángulos (panel, bordes, fondo) mediante escalado.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Cargar idiomas (inicialización/precarga del sistema de traducciones).
        Idiomas.get();

        // En ordenador no se necesita cargar imágenes de controles (se muestra solo la ayuda de PC).
        // En móvil se intentan cargar para mostrar los iconos; si falla, se usa texto de fallback.
        if (esMovil) {
            imgsOk = tryLoadControlImages();
        } else {
            imgsOk = false;
        }
    }

    private void recalcFontScale() {
        // Si aún no se han creado font o camera, no se puede recalcular.
        if (font == null || camera == null) return;

        // Relación entre unidades del mundo (viewport) y píxeles reales; aquí el viewport se iguala a píxeles,
        // pero se mantiene el cálculo para consistencia y para aplicar UiEscala.
        float worldToScreen = (camera.viewportHeight / (float) Gdx.graphics.getHeight());

        // Factor final: base por resolución multiplicado por el escalado global de UI definido por el proyecto.
        factorEscaladoFuente = worldToScreen * UiEscala.uiScale();

        // Escalado final de la fuente (incluye un multiplicador para legibilidad).
        font.getData().setScale(factorEscaladoFuente * 1.15f);
    }

    private boolean tryLoadControlImages() {
        // Rutas reales según tu proyecto:
        // assets/sprites/hud/controles/
        final String base = "sprites/hud/controles/";

        try {
            // Carga de texturas de controles (móvil) desde assets.
            imgJoystick = new Texture(Gdx.files.internal(base + "joystick.png"));
            imgDisparo  = new Texture(Gdx.files.internal(base + "boton_disparar.png"));
            imgEspecial = new Texture(Gdx.files.internal(base + "boton_especial.png"));
            imgPausa    = new Texture(Gdx.files.internal(base + "boton_pausa.png"));

            // Filtro nearest para que las imágenes mantengan el aspecto nítido al escalar.
            imgJoystick.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            imgDisparo.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            imgEspecial.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            imgPausa.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

            // Si todo se carga correctamente, se habilita el render de iconos.
            return true;
        } catch (Exception e) {
            // Si algo falla, liberamos lo que se haya creado a medias para evitar fugas de memoria.
            if (imgJoystick != null) { imgJoystick.dispose(); imgJoystick = null; }
            if (imgDisparo != null)  { imgDisparo.dispose();  imgDisparo = null; }
            if (imgEspecial != null) { imgEspecial.dispose(); imgEspecial = null; }
            if (imgPausa != null)    { imgPausa.dispose();    imgPausa = null; }
            return false;
        }
    }

    @Override
    public void render(float delta) {
        // Procesa entradas del usuario antes de dibujar (volver al menú).
        handleInput();

        // Actualiza la cámara y aplica la matriz de proyección al batch.
        camera.update();
        juego.batch.setProjectionMatrix(camera.combined);

        // Comienza el dibujado 2D.
        juego.batch.begin();

        // Tamaño actual de la pantalla (en píxeles).
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Fondo negro: se dibuja un rectángulo que cubre toda la pantalla usando la textura pixel.
        juego.batch.setColor(0f, 0f, 0f, 1f);
        juego.batch.draw(pixel, 0f, 0f, w, h);

        // Panel grande: área central semitransparente para mejorar legibilidad.
        float panelW = w * 0.78f;
        float panelH = h * 0.80f;
        float panelX = (w - panelW) * 0.5f;
        float panelY = (h - panelH) * 0.5f;

        // Fondo del panel con baja opacidad.
        juego.batch.setColor(1f, 1f, 1f, 0.08f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde del panel: dibuja 4 rectángulos delgados alrededor.
        juego.batch.setColor(1f, 1f, 1f, 0.14f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restaura color del batch para dibujar texto e iconos sin tintado no deseado.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Coordenada X central para centrar elementos.
        float centerX = w * 0.5f;

        // Título: se obtiene desde traducciones con fallback.
        String titulo = safeT("help_title", "Ayuda");
        font.setColor(0.25f, 0.9f, 1f, 1f);
        drawCentered(titulo, centerX, panelY + panelH * 0.92f);
        font.setColor(Color.WHITE);

        // Posición inicial de las secciones y altura de línea del texto.
        float y = panelY + panelH * 0.82f;
        float line = font.getLineHeight() * 1.05f;

        // En ordenador se muestra solo la ayuda de PC.
        // En móvil se muestra solo la ayuda de móvil (con imágenes si están disponibles).
        if (!esMovil) {

            // -------------------------
            // PC CONTROLS (texto)
            // -------------------------
            font.setColor(1f, 1f, 1f, 0.95f);
            drawLeft(safeT("help_pc_title", "Ordenador"), panelX + panelW * 0.08f, y);
            y -= line * 1.10f;

            // Listado de acciones/teclas; se usa un alpha menor para jerarquía visual.
            font.setColor(1f, 1f, 1f, 0.82f);
            drawLeft(safeT("help_pc_move", "A / D o ← / → : moverse"), panelX + panelW * 0.10f, y); y -= line;
            drawLeft(safeT("help_pc_jump", "W / ↑ / ESPACIO : saltar"), panelX + panelW * 0.10f, y); y -= line;
            drawLeft(safeT("help_pc_crouch", "S / ↓ : agacharse"), panelX + panelW * 0.10f, y); y -= line;
            drawLeft(safeT("help_pc_shoot", "J : disparo normal"), panelX + panelW * 0.10f, y); y -= line;
            drawLeft(safeT("help_pc_special", "K : ataque especial"), panelX + panelW * 0.10f, y); y -= line;
            drawLeft(safeT("help_pc_pause", "P o ESC : pausa"), panelX + panelW * 0.10f, y); y -= line * 1.35f;

        } else {

            // -------------------------
            // MOBILE CONTROLS (imágenes + texto)
            // -------------------------
            font.setColor(1f, 1f, 1f, 0.95f);
            drawLeft(safeT("help_mobile_title", "Movil"), panelX + panelW * 0.08f, y);
            y -= line * 1.15f;

            // Mostrar imágenes si han cargado; si no, usar texto de fallback
            if (imgsOk && imgJoystick != null) {

                // Tamaño del icono relativo al panel, y posiciones base para icono (ix) y texto (tx).
                float icon = Math.min(panelW, panelH) * 0.070f;
                float ix = panelX + panelW * 0.10f;
                float tx = ix + icon + panelW * 0.045f;

                // Altura por fila: garantiza separación suficiente entre icono y texto y entre filas.
                float rowH = Math.max(line * 2.25f, icon * 2.10f);

                // Offset vertical para alinear visualmente el icono con la línea base del texto.
                float iconYOffset = icon * 0.70f;

                // Joystick
                juego.batch.setColor(1f, 1f, 1f, 1f);
                juego.batch.draw(imgJoystick, ix, y - iconYOffset, icon, icon);
                font.setColor(1f, 1f, 1f, 0.82f);
                drawLeft(safeT("help_mobile_joystick", "Joystick: moverte (es invisible si no lo tocas)"), tx, y);
                y -= rowH;

                // Disparo
                juego.batch.setColor(1f, 1f, 1f, 1f);
                juego.batch.draw(imgDisparo, ix, y - iconYOffset, icon, icon);
                font.setColor(1f, 1f, 1f, 0.82f);
                drawLeft(safeT("help_mobile_shoot", "Boton disparo: disparo normal"), tx, y);
                y -= rowH;

                // Especial
                juego.batch.setColor(1f, 1f, 1f, 1f);
                juego.batch.draw(imgEspecial, ix, y - iconYOffset, icon, icon);
                font.setColor(1f, 1f, 1f, 0.82f);
                drawLeft(safeT("help_mobile_special", "Boton especial: ataque especial"), tx, y);
                y -= rowH;

                // Pausa
                juego.batch.setColor(1f, 1f, 1f, 1f);
                juego.batch.draw(imgPausa, ix, y - iconYOffset, icon, icon);
                font.setColor(1f, 1f, 1f, 0.82f);
                drawLeft(safeT("help_mobile_pause", "Boton pausa: abrir menu de pausa"), tx, y);
                y -= rowH * 0.85f;

                // Restaura color del batch tras dibujar iconos.
                juego.batch.setColor(1f, 1f, 1f, 1f);

            } else {
                // Si faltan imágenes, se muestra el texto igualmente (traducible) como alternativa robusta.
                font.setColor(1f, 1f, 1f, 0.82f);
                drawLeft(safeT("help_mobile_fallback_1", "Joystick: moverte (invisible si no lo tocas en la zona izquierda inferior)"),
                    panelX + panelW * 0.10f, y); y -= line;
                drawLeft(safeT("help_mobile_fallback_2", "Boton disparo: disparo normal"),
                    panelX + panelW * 0.10f, y); y -= line;
                drawLeft(safeT("help_mobile_fallback_3", "Boton especial: ataque especial"),
                    panelX + panelW * 0.10f, y); y -= line;
                drawLeft(safeT("help_mobile_fallback_4", "Boton pausa: abrir menu de pausa"),
                    panelX + panelW * 0.10f, y); y -= line * 1.35f;
            }
        }

        // Botón volver (abajo): calcula dimensiones y posición basadas en el panel.
        computeButton(panelX, panelY, panelW, panelH);

        // Texto del botón, localizado con fallback.
        String volverTxt = safeT("help_back", "Volver al menu");

        // Fondo del botón con opacidad para diferenciarlo del panel.
        juego.batch.setColor(0f, 0f, 0f, 0.45f);
        juego.batch.draw(pixel, btnX, btnY, btnW, btnH);

        // Borde del botón para mejorar contraste y sensación de "clicable".
        juego.batch.setColor(1f, 1f, 1f, 0.18f);
        juego.batch.draw(pixel, btnX, btnY, btnW, b);
        juego.batch.draw(pixel, btnX, btnY + btnH - b, btnW, b);
        juego.batch.draw(pixel, btnX, btnY, b, btnH);
        juego.batch.draw(pixel, btnX + btnW - b, btnY, b, btnH);

        // Restaura color del batch antes de dibujar el texto del botón.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Texto del botón con un color destacado para llamar la atención.
        // En móvil se elimina para evitar solaparse con el hint "Toca el botón para volver".
        if (!esMovil) {
            font.setColor(1f, 0.85f, 0.2f, 1f);
            drawCentered("> " + volverTxt, centerX, btnY + btnH * 0.65f);
            font.setColor(Color.WHITE);
        }

        // Hint inferior: instrucción contextual según plataforma.
        if (!esMovil) {
            font.setColor(1f, 1f, 1f, 0.55f);
            drawCentered(safeT("help_hint_pc", "ESC o ENTER para volver"), centerX, panelY + panelH * 0.10f);
            font.setColor(Color.WHITE);
        } else {
            font.setColor(1f, 0.85f, 0.2f, 1f);
            drawCentered(safeT("help_hint_mobile", "Toca para volver al menu"), centerX, panelY + panelH * 0.10f);
            font.setColor(Color.WHITE);
        }

        // Finaliza el dibujado del frame.
        juego.batch.end();
    }

    private void handleInput() {

        // Móvil: tap botón volver.
        // Se usa justTouched para capturar el inicio del toque y evitar repeticiones mientras se mantiene pulsado.
        if (esMovil) {
            if (Gdx.input.justTouched()) {
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                // Convierte coordenadas de pantalla (origen arriba-izquierda) a coordenadas del mundo/cámara.
                Vector3 tmp = new Vector3(sx, sy, 0f);
                camera.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                // Test de colisión punto-rectángulo con el área del botón.
                if (wx >= btnX && wx <= btnX + btnW && wy >= btnY && wy <= btnY + btnH) {
                    volverMenu();
                }
            }
            return;
        }

        // PC: permite salir con teclas habituales (ESC/ENTER/SPACE).
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            volverMenu();
        }
    }

    private void volverMenu() {
        // Cambia a la pantalla de menú y libera recursos de esta pantalla.
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    private void computeButton(float panelX, float panelY, float panelW, float panelH) {
        // Dimensiones relativas al panel para mantener proporciones consistentes en distintas resoluciones.
        btnW = panelW * 0.58f;
        btnH = panelH * 0.12f;

        // Centrado horizontal dentro del panel.
        btnX = panelX + (panelW - btnW) * 0.5f;

        // Botón cerca del borde inferior del panel.
        btnY = panelY + panelH * 0.02f;
    }

    private void drawCentered(String text, float centerX, float y) {
        // Calcula el ancho del texto para ajustar X y centrarlo.
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;

        // Dibuja el texto con el layout precalculado.
        font.draw(juego.batch, layout, x, y);
    }

    private void drawLeft(String text, float x, float y) {
        // Calcula el layout del texto (por consistencia y por si se necesita width/height).
        layout.setText(font, text);

        // Dibuja alineado a la izquierda en la coordenada indicada.
        font.draw(juego.batch, layout, x, y);
    }

    private String safeT(String key, String fallback) {
        // Obtiene una traducción; si falla por cualquier motivo, devuelve el texto de respaldo.
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        // Ajusta el viewport de la cámara al nuevo tamaño (ventana/redimensionado/rotación).
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }

        // Recalcula el escalado de la fuente al cambiar el tamaño (rotación / ventana / resolución).
        recalcFontScale();
    }

    @Override
    public void dispose() {
        // Libera recursos GPU asociados a la fuente y al pixel 1x1.
        if (font != null) font.dispose();
        if (pixel != null) pixel.dispose();

        // Libera texturas de controles si fueron creadas.
        if (imgJoystick != null) imgJoystick.dispose();
        if (imgDisparo != null) imgDisparo.dispose();
        if (imgEspecial != null) imgEspecial.dispose();
        if (imgPausa != null) imgPausa.dispose();
    }
}
