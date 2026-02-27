package io.github.some_example_name.pantallas;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Configuracion;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.Idiomas;

public class PantallaOpciones extends ScreenAdapter {

    private final Main juego;

    private OrthographicCamera camara;
    private Viewport viewport;
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;

    private float factorEscaladoFuente;

    private int selected = 0;

    // 0: Idioma, 1: Dificultad, 2: Sonidos, 3: Vibración, 4: Volver
    private static final int ITEM_COUNT = 5;

    // --- ESTILO COMO PantallaMenu: fondo + panel semi-transparente ---
    private Texture fondoMenu;
    private Texture pixel;
    // ---------------------------------------------------------------

    // --- Preferencias para switches (no rompe nada aunque aún no se use en gameplay) ---
    private Preferences prefs;

    // ✅ IMPORTANTE: ahora coincide con Configuracion.java
    private static final String PREFS_NAME = "juego_prefs";
    private static final String KEY_SONIDO = "sound";           // música + efectos
    private static final String KEY_VIBRACION = "vibration";
    // ----------------------------------------------------------------------------------

    // ✅ NUEVO: claves alternativas por si el resto del juego usa otras keys
    private static final String KEY_SONIDO_ALT = "sonido";
    private static final String KEY_VIBRACION_ALT = "vibracion";

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático
    // ------------------------------------------------------------
    private boolean esMovil = false;

    public PantallaOpciones(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10-Regular.fnt y todos sus png
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        font.setUseIntegerPositions(false);

        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (viewport.getWorldHeight() / Gdx.graphics.getHeight()) * dpiScale;

        font.getData().setScale(factorEscaladoFuente * 2.0f);
        font.setColor(Color.WHITE);

        // Filtro: Nearest para estilo pixelado
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        Idiomas.get();

        // --- NUEVO: fondo del menú (igual que PantallaMenu) ---
        fondoMenu = new Texture(Gdx.files.internal("sprites/fondos/fondoMenu.png"));
        fondoMenu.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // --- NUEVO: pixel 1x1 para panel/borde ---
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // --- NUEVO: prefs switches ---
        prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Defaults (en ambas keys, por compatibilidad)
        if (!prefs.contains(KEY_SONIDO) && !prefs.contains(KEY_SONIDO_ALT)) {
            prefs.putBoolean(KEY_SONIDO, true);
            prefs.putBoolean(KEY_SONIDO_ALT, true);
        } else {
            // Si existe una pero no la otra, la copiamos
            if (prefs.contains(KEY_SONIDO) && !prefs.contains(KEY_SONIDO_ALT)) {
                prefs.putBoolean(KEY_SONIDO_ALT, prefs.getBoolean(KEY_SONIDO, true));
            } else if (prefs.contains(KEY_SONIDO_ALT) && !prefs.contains(KEY_SONIDO)) {
                prefs.putBoolean(KEY_SONIDO, prefs.getBoolean(KEY_SONIDO_ALT, true));
            }
        }

        if (!prefs.contains(KEY_VIBRACION) && !prefs.contains(KEY_VIBRACION_ALT)) {
            prefs.putBoolean(KEY_VIBRACION, true);
            prefs.putBoolean(KEY_VIBRACION_ALT, true);
        } else {
            if (prefs.contains(KEY_VIBRACION) && !prefs.contains(KEY_VIBRACION_ALT)) {
                prefs.putBoolean(KEY_VIBRACION_ALT, prefs.getBoolean(KEY_VIBRACION, true));
            } else if (prefs.contains(KEY_VIBRACION_ALT) && !prefs.contains(KEY_VIBRACION)) {
                prefs.putBoolean(KEY_VIBRACION, prefs.getBoolean(KEY_VIBRACION_ALT, true));
            }
        }

        prefs.flush();

        // ✅ CLAVE: además, sincroniza Configuracion si el proyecto lo usa
        // (si estos métodos existen en tu Configuracion, queda alineado al instante)
        try {
            Configuracion.setSonidoActivado(isSonidoActivado());
        } catch (Exception ignored) {}
        try {
            Configuracion.setVibracionActivada(isVibracionActivada());
        } catch (Exception ignored) {}
    }

    @Override
    public void render(float delta) {
        handleInput();

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        // Fondo (igual que menú)
        if (fondoMenu != null) {
            juego.batch.setColor(1f, 1f, 1f, 1f);
            juego.batch.draw(fondoMenu, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        // Panel central semi-transparente (igual estilo)
        float panelW = viewport.getWorldWidth() * 0.55f;
        float panelH = viewport.getWorldHeight() * 0.55f;
        float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
        // Bajamos un poco para no tapar el logo del fondo
        float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

        // Rectángulo semi-transparente
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(0.02f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        juego.batch.setColor(1f, 1f, 1f, 1f);

        // -------------------------
        // Contenido (centrado)
        // -------------------------
        float centerX = viewport.getWorldWidth() * 0.5f;

        // (Mantengo título, pero dentro del panel y más discreto)
        String titulo = safeT("options_title", "Opciones");
        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;

        font.getData().setScale(originalScaleX * 0.95f, originalScaleY * 0.95f);
        drawCentered(titulo, panelY + panelH * 0.82f);
        font.getData().setScale(originalScaleX, originalScaleY);

        // Items dentro del panel (sin solaparse)
        float baseY = panelY + panelH * 0.72f;
        float step = panelH * 0.14f;

        // Idioma (ES/GL/EN) + mostrar nombre
        String lang = Idiomas.getIdioma();
        String langName =
            "es".equalsIgnoreCase(lang) ? safeT("language_es", "Español")
                : ("gl".equalsIgnoreCase(lang) ? safeT("language_gl", "Galego")
                : ("en".equalsIgnoreCase(lang) ? safeT("language_en", "English") : lang));

        String idiomaLine = safeT("options_language", "Idioma") + ": " + langName;
        drawItem(0, idiomaLine, baseY - step * 0);

        // Dificultad
        Configuracion.Dificultad d = Configuracion.getDificultad();
        String diffName = (d == Configuracion.Dificultad.DIFICIL)
            ? safeT("difficulty_hard", "Difícil")
            : safeT("difficulty_easy", "Fácil");

        String diffLine = safeT("options_difficulty", "Dificultad") + ": " + diffName;
        drawItem(1, diffLine, baseY - step * 1);

        // Sonidos (música + efectos)
        boolean sonidoOn = isSonidoActivado();
        String onOff = sonidoOn ? safeT("on", "ON") : safeT("off", "OFF");
        String sonidoLine = safeT("options_sound", "Sonidos") + ": " + onOff;
        drawItem(2, sonidoLine, baseY - step * 2);

        // Vibración
        boolean vibraOn = isVibracionActivada();
        String onOffV = vibraOn ? safeT("on", "ON") : safeT("off", "OFF");
        String vibraLine = safeT("options_vibration", "Vibración") + ": " + onOffV;
        drawItem(3, vibraLine, baseY - step * 3);

        // Volver
        drawItem(4, safeT("back", "Volver"), baseY - step * 4);

        // Hint FUERA del panel, abajo del todo (como pediste)
        String hint = safeT("options_hint", "←→ cambia, ↑↓ selecciona, ENTER acepta, ESC vuelve");
        font.getData().setScale(originalScaleX * 0.85f, originalScaleY * 0.85f);

        if (esMovil) {
            drawCentered(safeT("options_hint_touch", "Toca una opción para cambiar, o Volver"), viewport.getWorldHeight() * 0.08f);
        } else {
            drawCentered(hint, viewport.getWorldHeight() * 0.08f);
        }

        font.getData().setScale(originalScaleX, originalScaleY);

        juego.batch.end();
    }

    private void handleInput() {

        // ------------------------------------------------------------
        // MÓVIL: SOLO TÁCTIL
        // ------------------------------------------------------------
        if (esMovil) {

            if (Gdx.input.justTouched()) {

                // ✅ NO invertir Y antes de unproject
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                Vector2 tmp = new Vector2(sx, sy);
                viewport.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                float panelW = viewport.getWorldWidth() * 0.55f;
                float panelH = viewport.getWorldHeight() * 0.55f;
                float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
                float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

                float baseY = panelY + panelH * 0.72f;
                float step = panelH * 0.14f;

                // Zona clicable: todo el ancho del panel, y una "banda" por item
                float halfH = step * 0.55f;

                float y0 = baseY - step * 0;
                float y1 = baseY - step * 1;
                float y2 = baseY - step * 2;
                float y3 = baseY - step * 3;
                float y4 = baseY - step * 4;

                if (wx >= panelX && wx <= panelX + panelW) {

                    if (wy >= y0 - halfH && wy <= y0 + halfH) {
                        selected = 0;
                        toggleIdioma();
                    } else if (wy >= y1 - halfH && wy <= y1 + halfH) {
                        selected = 1;
                        toggleDificultad();
                    } else if (wy >= y2 - halfH && wy <= y2 + halfH) {
                        selected = 2;
                        toggleSonido();
                    } else if (wy >= y3 - halfH && wy <= y3 + halfH) {
                        selected = 3;
                        toggleVibracion();
                    } else if (wy >= y4 - halfH && wy <= y4 + halfH) {
                        selected = 4;
                        volver();
                    }
                }
            }

            return;
        }

        // ------------------------------------------------------------
        // PC: TECLADO como hasta ahora
        // ------------------------------------------------------------
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + ITEM_COUNT) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }

        boolean left = Gdx.input.isKeyJustPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);

        if ((left || right)) {
            if (selected == 0) {
                toggleIdioma();
            } else if (selected == 1) {
                toggleDificultad();
            } else if (selected == 2) {
                toggleSonido();
            } else if (selected == 3) {
                toggleVibracion();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selected == 0) toggleIdioma();
            else if (selected == 1) toggleDificultad();
            else if (selected == 2) toggleSonido();
            else if (selected == 3) toggleVibracion();
            else if (selected == 4) volver();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volver();
        }
    }

    private void toggleIdioma() {
        // Ahora cicla ES -> GL -> EN -> ES
        String current = Idiomas.getIdioma();
        String next;
        if ("es".equalsIgnoreCase(current)) next = "gl";
        else if ("gl".equalsIgnoreCase(current)) next = "en";
        else next = "es";
        Idiomas.setIdioma(next);
        // No hace falta recrear pantalla: ya se recarga el bundle en setIdioma()
    }

    private void toggleDificultad() {
        Configuracion.Dificultad current = Configuracion.getDificultad();
        Configuracion.Dificultad next = (current == Configuracion.Dificultad.FACIL)
            ? Configuracion.Dificultad.DIFICIL
            : Configuracion.Dificultad.FACIL;
        Configuracion.setDificultad(next);
    }

    private boolean isSonidoActivado() {
        if (prefs == null) return true;
        // ✅ lee cualquiera de las dos (prioriza KEY_SONIDO si existe)
        if (prefs.contains(KEY_SONIDO)) return prefs.getBoolean(KEY_SONIDO, true);
        return prefs.getBoolean(KEY_SONIDO_ALT, true);
    }

    private void toggleSonido() {
        if (prefs == null) return;
        boolean current = isSonidoActivado();
        boolean next = !current;

        // ✅ escribe en ambas keys (compat)
        prefs.putBoolean(KEY_SONIDO, next);
        prefs.putBoolean(KEY_SONIDO_ALT, next);
        prefs.flush();

        // ✅ sincroniza Configuracion si existe el setter
        try {
            Configuracion.setSonidoActivado(next);
        } catch (Exception ignored) {}
    }

    private boolean isVibracionActivada() {
        if (prefs == null) return true;
        // ✅ lee cualquiera de las dos (prioriza KEY_VIBRACION si existe)
        if (prefs.contains(KEY_VIBRACION)) return prefs.getBoolean(KEY_VIBRACION, true);
        return prefs.getBoolean(KEY_VIBRACION_ALT, true);
    }

    private void toggleVibracion() {
        if (prefs == null) return;
        boolean current = isVibracionActivada();
        boolean next = !current;

        // ✅ escribe en ambas keys (compat)
        prefs.putBoolean(KEY_VIBRACION, next);
        prefs.putBoolean(KEY_VIBRACION_ALT, next);
        prefs.flush();

        // ✅ sincroniza Configuracion si existe el setter
        try {
            Configuracion.setVibracionActivada(next);
        } catch (Exception ignored) {}
    }

    private void volver() {
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    private void drawItem(int idx, String text, float y) {
        String prefix = (idx == selected) ? "> " : "  ";
        String line = prefix + text;

        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        drawCentered(line, y);
        font.setColor(Color.WHITE);
    }

    private void drawCentered(String text, float y) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
        float x = (viewport.getWorldWidth() - layout.width) * 0.5f;
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
        if (viewport != null) viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (fondoMenu != null) fondoMenu.dispose();
        if (pixel != null) pixel.dispose();
    }
}
