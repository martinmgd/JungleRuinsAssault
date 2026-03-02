package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.TextInputListener;
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

/**
 * Pantalla que aparece al conseguir un nuevo record.
 *
 * <p>Permite introducir 3 letras (siempre en mayúsculas), guardar TOP10 en fichero,
 * y volver a PantallaMuerte o PantallaVictoria según corresponda.</p>
 */
public class PantallaNuevoRecord extends ScreenAdapter {

    /**
     * Destino tras guardar el record.
     */
    public enum Destino {
        /** Volver a la pantalla de muerte. */
        MUERTE,
        /** Volver a la pantalla de victoria. */
        VICTORIA
    }

    // Referencia al juego principal para renderizado (batch) y navegación entre pantallas.
    private final Main juego;

    // Puntuación lograda que se va a registrar en el TOP10 si procede.
    private final int score;

    // Pantalla a la que se debe volver tras guardar el record (muerte/victoria).
    private final Destino destino;

    // Cámara ortográfica en coordenadas de pantalla (se trabaja en pixeles de screen space).
    private OrthographicCamera camera;

    // Fuente bitmap y layout para medir texto antes de dibujarlo centrado.
    private BitmapFont font;
    private GlyphLayout layout;

    // Textura 1x1 blanca usada como "pixel" para dibujar rectángulos (fondo, panel, bordes, botón).
    private Texture pixel;

    // Flag de plataforma: habilita teclado virtual y flujo táctil en móvil.
    private boolean esMovil = false;

    // Iniciales del jugador: siempre 3 caracteres A-Z en mayúsculas.
    private String initials = "AAA";

    // Estado de UI: evita abrir múltiples veces el teclado virtual en móvil.
    private boolean tecladoAbiertoEnMovil = false;

    // Geometría del botón "Aceptar" (en coordenadas del mundo/cámara; aquí equivale a pixeles).
    private float btnX, btnY, btnW, btnH;

    /**
     * Crea la pantalla de nuevo record.
     * @param juego instancia principal.
     * @param score puntuación conseguida.
     * @param destino a dónde volver tras aceptar.
     */
    public PantallaNuevoRecord(Main juego, int score, Destino destino) {
        // Dependencias y datos inmutables de esta pantalla.
        this.juego = juego;
        this.score = score;
        this.destino = destino;
    }

    @Override
    public void show() {
        // Determina si se ejecuta en Android/iOS para activar flujo de teclado virtual.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Cámara en coordenadas de pantalla: viewport = ancho/alto actual en pixeles.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Fuente: se carga un .fnt (bitmap font). Se configura filtrado y escala.
        // - setUseIntegerPositions(false) permite subpixel para suavizar movimientos/centrado.
        // - Filtro Nearest preserva estilo pixel-art si la fuente lo requiere.
        font = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        font.setUseIntegerPositions(false);
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        font.getData().setScale(getFontScale());
        font.setColor(Color.WHITE);

        // Layout para medir ancho/alto del texto sin crear objetos extra por frame.
        layout = new GlyphLayout();

        // Crea una textura 1x1 blanca a partir de un Pixmap para dibujar rectángulos escalados.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Inicializa/carga recursos de idiomas (según implementación del proyecto).
        Idiomas.get();

        // En móvil: abre teclado al entrar (1 vez) para acelerar la introducción de iniciales.
        if (esMovil) {
            abrirTecladoMovil();
        }
    }

    @Override
    public void render(float delta) {

        // Mantiene la cámara actualizada (por si hubo resize u otros cambios).
        camera.update();

        // ✅ Calcula panel/botón ANTES del input (para que el hitbox exista ya)
        // Se trabaja en pixeles: w/h equivalen al tamaño real de pantalla.
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Panel centrado con dimensiones relativas a pantalla.
        float panelW = w * 0.58f;
        float panelH = h * 0.55f;
        float panelX = (w - panelW) * 0.5f;
        float panelY = (h - panelH) * 0.5f;

        // Calcula posición/tamaño del botón en función del panel.
        computeButton(panelX, panelY, panelW, panelH);

        // ✅ Ahora sí: input con btnX/btnY correctos
        // Gestiona teclado físico en PC o taps/teclado virtual en móvil.
        handleInput(panelX, panelY, panelW, panelH);

        // Prepara el batch para dibujar en el espacio de la cámara.
        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        // Fondo negro a pantalla completa usando el "pixel" escalado.
        juego.batch.setColor(0f, 0f, 0f, 1f);
        juego.batch.draw(pixel, 0, 0, w, h);

        // Panel centrado con alpha bajo (overlay sutil).
        juego.batch.setColor(1f, 1f, 1f, 0.08f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil del panel: 4 rectángulos finos.
        juego.batch.setColor(1f, 1f, 1f, 0.14f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restaura color del batch para dibujado posterior.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = w * 0.5f;

        // Título: se obtiene por idioma con fallback seguro.
        String titulo = safeT("record_new_title", "Nuevo record");
        font.setColor(0.2f, 0.95f, 0.35f, 1f);
        drawCentered(titulo, centerX, panelY + panelH * 0.83f);
        font.setColor(Color.WHITE);

        // Score: muestra etiqueta + valor numérico logrado.
        String scoreTxt = safeT("record_score", "Puntuación") + ": " + score;
        font.setColor(1f, 1f, 1f, 0.95f);
        drawCentered(scoreTxt, centerX, panelY + panelH * 0.67f);

        // Instrucción: guía al usuario para escribir 3 letras.
        String instr = safeT("record_enter_3", "Escribe 3 letras");
        font.setColor(1f, 1f, 1f, 0.85f);
        drawCentered(instr, centerX, panelY + panelH * 0.55f);

        // Letras grandes: se normalizan a 3 letras A-Z (según Records.normalizeInitials).
        String ini = Records.normalizeInitials(initials);
        font.setColor(1f, 0.9f, 0.25f, 1f);
        drawCentered(ini, centerX, panelY + panelH * 0.40f);
        font.setColor(Color.WHITE);

        // Botón aceptar (siempre visible; en PC se puede usar Enter).
        String aceptarTxt = safeT("record_accept", "Aceptar");

        // Fondo del botón (rectángulo).
        juego.batch.setColor(0f, 0f, 0f, 0.45f);
        juego.batch.draw(pixel, btnX, btnY, btnW, btnH);

        // Borde del botón (4 líneas).
        juego.batch.setColor(1f, 1f, 1f, 0.18f);
        juego.batch.draw(pixel, btnX, btnY, btnW, b);
        juego.batch.draw(pixel, btnX, btnY + btnH - b, btnW, b);
        juego.batch.draw(pixel, btnX, btnY, b, btnH);
        juego.batch.draw(pixel, btnX + btnW - b, btnY, b, btnH);

        // Texto del botón centrado horizontalmente y alineado verticalmente con un factor de altura.
        juego.batch.setColor(1f, 1f, 1f, 1f);
        font.setColor(1f, 0.85f, 0.2f, 1f);
        drawCentered(aceptarTxt, centerX, btnY + btnH * 0.65f);
        font.setColor(Color.WHITE);

        // Hint contextual según plataforma:
        // - PC: sugiere ENTER
        // - Móvil: sugiere tocar letras/panel para editar (teclado virtual)
        if (!esMovil) {
            font.setColor(1f, 1f, 1f, 0.55f);
            drawCentered(safeT("record_hint_pc", "ENTER para aceptar"), centerX, panelY + panelH * 0.18f);
            font.setColor(Color.WHITE);
        } else {
            font.setColor(1f, 1f, 1f, 0.55f);
            drawCentered(safeT("record_hint_mobile", "Toca las letras para editar"), centerX, panelY + panelH * 0.18f);
            font.setColor(Color.WHITE);
        }

        juego.batch.end();
    }

    private void handleInput(float panelX, float panelY, float panelW, float panelH) {

        // PC: capturar letras / backspace / enter
        if (!esMovil) {
            // Letras A-Z: mapea Input.Keys.A..Z a caracteres y los añade al buffer.
            for (int k = Input.Keys.A; k <= Input.Keys.Z; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    char c = (char) ('A' + (k - Input.Keys.A));
                    appendChar(c);
                }
            }

            // Backspace: aplica lógica de borrado (manteniendo 3 letras).
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                backspace();
            }

            // Enter o Space: confirma y guarda.
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                aceptar();
            }

            return;
        }

        // Móvil: tap para abrir teclado / botón aceptar
        if (Gdx.input.justTouched()) {
            int sx = Gdx.input.getX();
            int sy = Gdx.input.getY();
            Vector3 tmp = new Vector3(sx, sy, 0f);
            camera.unproject(tmp);

            float wx = tmp.x;
            float wy = tmp.y;

            // Tap en botón aceptar: confirma y guarda.
            if (wx >= btnX && wx <= btnX + btnW && wy >= btnY && wy <= btnY + btnH) {
                aceptar();
                return;
            }

            // Tap en cualquier sitio del panel (o cerca de letras) -> abrir teclado
            // (con tu lógica actual: lo dejamos igual)
            if (wx >= panelX && wx <= panelX + panelW && wy >= panelY && wy <= panelY + panelH) {
                abrirTecladoMovil();
            } else {
                // Si se desea abrir teclado incluso fuera del panel, se mantiene este comportamiento.
                abrirTecladoMovil();
            }
        }
    }

    private void abrirTecladoMovil() {
        // Evita invocar múltiples diálogos si uno ya está abierto.
        if (tecladoAbiertoEnMovil) return;
        tecladoAbiertoEnMovil = true;

        // Títulos/hints traducibles con fallback.
        final String title = safeT("record_keyboard_title", "Nuevo record");
        final String hint = safeT("record_keyboard_hint", "3 letras");

        // Teclado virtual: callback input/cancel para actualizar iniciales y liberar el lock.
        Gdx.input.getTextInput(new TextInputListener() {
            @Override
            public void input(String text) {
                initials = sanitize3(text);
                tecladoAbiertoEnMovil = false;
            }

            @Override
            public void canceled() {
                tecladoAbiertoEnMovil = false;
            }
        }, title, initials, hint);
    }

    private void aceptar() {
        // Normaliza a 3 letras siempre (garantiza formato TOP10 consistente).
        String ini = Records.normalizeInitials(initials);

        // Guardar record (solo si entra en TOP10 según implementación de Records).
        Records.addIfTop10(ini, score);

        // Volver a la pantalla correspondiente según el contexto (muerte o victoria).
        if (destino == Destino.MUERTE) {
            juego.setScreen(new PantallaMuerte(juego));
        } else {
            juego.setScreen(new PantallaVictoria(juego));
        }
        dispose();
    }

    private void appendChar(char c) {
        // Concatena y re-sanitiza para asegurar siempre 3 letras válidas A-Z.
        String s = sanitize3(initials + c);
        initials = s;
    }

    private void backspace() {
        // Normaliza primero por si había caracteres no estándar.
        String s = Records.normalizeInitials(initials);
        // Quitar 1 letra y rellenar con 'A' al final para mantener 3 (simple).
        String cut = s.substring(0, 2) + "A";
        initials = cut;
    }

    private String sanitize3(String raw) {
        // Normalización robusta: null-safe, uppercase, filtra solo A-Z y completa con 'A' hasta 3.
        if (raw == null) raw = "";
        raw = raw.toUpperCase();

        StringBuilder out = new StringBuilder(3);
        for (int i = 0; i < raw.length() && out.length() < 3; i++) {
            char c = raw.charAt(i);
            if (c >= 'A' && c <= 'Z') out.append(c);
        }
        while (out.length() < 3) out.append('A');
        return out.toString();
    }

    private void computeButton(float panelX, float panelY, float panelW, float panelH) {
        // Define dimensiones proporcionales al panel y centra horizontalmente.
        btnW = panelW * 0.55f;
        btnH = panelH * 0.14f;
        btnX = panelX + (panelW - btnW) * 0.5f;
        btnY = panelY + panelH * 0.08f;
    }

    private float getFontScale() {
        // Ajuste de escala basado en densidad (dpi) y relación entre viewport y resolución física.
        float dpiScale = Gdx.graphics.getDensity();
        float factor = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;
        return factor * 1.35f;
    }

    private void drawCentered(String text, float centerX, float y) {
        // Mide el texto y desplaza el origen para que quede centrado en X.
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    private String safeT(String key, String fallback) {
        // Acceso seguro a traducciones: si falla el sistema de idiomas, usa fallback estable.
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        // Mantiene la cámara sincronizada con nuevas dimensiones de pantalla.
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
    }

    @Override
    public void dispose() {
        // Libera recursos nativos (fuente y textura pixel).
        if (font != null) font.dispose();
        if (pixel != null) pixel.dispose();
    }
}
