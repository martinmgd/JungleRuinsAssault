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
import io.github.some_example_name.utilidades.UiEscala;

public class PantallaOpciones extends ScreenAdapter {

    // Referencia al juego principal: se usa para acceder al batch y para cambiar de pantalla.
    private final Main juego;

    // Cámara y viewport: esta pantalla dibuja en UNIDADES DE MUNDO (no en píxeles directos).
    private OrthographicCamera camara;
    private Viewport viewport;

    // Fuente para renderizar textos de opciones y ayudas.
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;

    // Factor de escalado calculado para mantener legibilidad consistente entre resoluciones/dispositivos.
    private float factorEscaladoFuente;

    // Índice del ítem seleccionado (navegación con teclado o selección táctil).
    private int selected = 0;

    // Número total de opciones del menú de opciones.
    // 0: Idioma, 1: Dificultad, 2: Sonidos, 3: Vibración, 4: Volver
    private static final int ITEM_COUNT = 5;

    // --- ESTILO COMO PantallaMenu: fondo + panel semi-transparente ---
    // Textura de fondo (reutiliza el mismo asset del menú).
    private Texture fondoMenu;
    // Textura 1x1 para dibujar rectángulos escalados (panel y bordes).
    private Texture pixel;
    // ---------------------------------------------------------------

    // --- Preferencias para switches (no rompe nada aunque aún no se use en gameplay) ---
    // Preferencias persistentes para almacenar estado de sonido y vibración.
    private Preferences prefs;

    // IMPORTANTE: ahora coincide con Configuracion.java
    // Nombre del contenedor de preferencias y claves principales.
    private static final String PREFS_NAME = "juego_prefs";
    private static final String KEY_SONIDO = "sound";           // música + efectos
    private static final String KEY_VIBRACION = "vibration";
    // ----------------------------------------------------------------------------------

    // Claves alternativas por si el resto del juego usa otras keys
    // Se mantienen por compatibilidad con versiones anteriores o con otras partes del proyecto.
    private static final String KEY_SONIDO_ALT = "sonido";
    private static final String KEY_VIBRACION_ALT = "vibracion";

    // ✅ NUEVO: dificultad persistida (para que cambie aunque Configuracion esté forzando un valor)
    // true = difícil, false = fácil
    private static final String KEY_DIFICULTAD_DIFICIL = "difficulty_hard";
    private static final String KEY_DIFICULTAD_DIFICIL_ALT = "dificultad_dificil";

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático
    // ------------------------------------------------------------
    // Flag para activar lógica de entrada táctil en Android/iOS.
    private boolean esMovil = false;

    public PantallaOpciones(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        // Inicializa cámara y viewport en el sistema de coordenadas del juego.
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Detecta plataforma para alternar entre controles táctiles y teclado.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10-Regular.fnt y todos sus png
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Evita redondeo a coordenadas enteras; importante cuando se escala en viewport.
        font.setUseIntegerPositions(false);

        // Esta pantalla dibuja en UNIDADES DE MUNDO (viewport).
        // Para que el texto se adapte a cualquier dispositivo (como PantallaMenu),
        // convertimos px->mundo y aplicamos UiEscala (evita que en móvil se vea gigante).
        recalcularEscalaFuente();

        // Color por defecto del texto.
        font.setColor(Color.WHITE);

        // Filtro: Nearest para estilo pixelado
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Asegura que el sistema de idiomas está disponible antes de dibujar textos traducibles.
        Idiomas.get();

        // --- NUEVO: fondo del menú (igual que PantallaMenu) ---
        // Carga la textura del fondo compartida con el menú principal.
        fondoMenu = new Texture(Gdx.files.internal("sprites/fondos/fondoMenu.png"));
        fondoMenu.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // --- NUEVO: pixel 1x1 para panel/borde ---
        // Se crea un "pixel" reutilizable para construir rectángulos y bordes por escalado.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // --- NUEVO: prefs switches ---
        // Recupera el contenedor de preferencias persistentes.
        prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Defaults (en ambas keys, por compatibilidad)
        // Inicializa valores por defecto si no existen, y mantiene sincronizadas keys nuevas/antiguas.
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

        // Mismo enfoque para vibración: default + sincronización entre claves.
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

        // ✅ NUEVO: default dificultad (si no existe, usamos Configuracion si se puede; si no, fácil)
        if (!prefs.contains(KEY_DIFICULTAD_DIFICIL) && !prefs.contains(KEY_DIFICULTAD_DIFICIL_ALT)) {
            boolean hardDefault = false;
            try {
                hardDefault = (Configuracion.getDificultad() == Configuracion.Dificultad.DIFICIL);
            } catch (Exception ignored) {}
            prefs.putBoolean(KEY_DIFICULTAD_DIFICIL, hardDefault);
            prefs.putBoolean(KEY_DIFICULTAD_DIFICIL_ALT, hardDefault);
        } else {
            // Si existe una pero no la otra, la copiamos
            if (prefs.contains(KEY_DIFICULTAD_DIFICIL) && !prefs.contains(KEY_DIFICULTAD_DIFICIL_ALT)) {
                prefs.putBoolean(KEY_DIFICULTAD_DIFICIL_ALT, prefs.getBoolean(KEY_DIFICULTAD_DIFICIL, false));
            } else if (prefs.contains(KEY_DIFICULTAD_DIFICIL_ALT) && !prefs.contains(KEY_DIFICULTAD_DIFICIL)) {
                prefs.putBoolean(KEY_DIFICULTAD_DIFICIL, prefs.getBoolean(KEY_DIFICULTAD_DIFICIL_ALT, false));
            }
        }

        // Persiste cambios de inicialización/sincronización.
        prefs.flush();

        // Además, sincroniza Configuracion si el proyecto lo usa
        // (si estos métodos existen en tu Configuracion, queda alineado al instante)
        try {
            Configuracion.setSonidoActivado(isSonidoActivado());
        } catch (Exception ignored) {}
        try {
            Configuracion.setVibracionActivada(isVibracionActivada());
        } catch (Exception ignored) {}
        try {
            Configuracion.setDificultad(isDificultadDificil() ? Configuracion.Dificultad.DIFICIL : Configuracion.Dificultad.FACIL);
        } catch (Exception ignored) {}
    }

    private void recalcularEscalaFuente() {
        // Evita NPE si se llama antes de inicializar font o viewport.
        if (font == null || viewport == null) return;

        // px -> mundo (porque dibujamos con viewport/camara)
        // worldToScreen refleja el factor de conversión basado en la altura del mundo vs píxeles reales.
        float worldToScreen = (viewport.getWorldHeight() / (float) Gdx.graphics.getHeight());

        // Clamp del density para que no infle en móvil ni lo hunda en PC raro
        // Se limita el "density" para que el tamaño del texto no se dispare en ciertos dispositivos.
        float dpiScale = Gdx.graphics.getDensity();
        if (dpiScale < 1f) dpiScale = 1f;
        if (dpiScale > 1.5f) dpiScale = 1.5f;

        // Factor final: conversión world/píxel * densidad limitada * escala de UI del proyecto.
        factorEscaladoFuente = worldToScreen * dpiScale * UiEscala.uiScale();

        // Escala final aplicada a la fuente para el menú de opciones.
        font.getData().setScale(factorEscaladoFuente * 1.50f);
    }

    @Override
    public void render(float delta) {
        // Gestiona entradas antes de renderizar para reflejar cambios en el mismo frame.
        handleInput();

        // Limpieza del buffer; el fondo real se dibuja con textura si está disponible.
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Aplica viewport y ajusta proyección del batch.
        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        // Fondo (igual que menú)
        // Dibuja el fondo ocupando todo el "mundo" del viewport.
        if (fondoMenu != null) {
            juego.batch.setColor(1f, 1f, 1f, 1f);
            juego.batch.draw(fondoMenu, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        // Panel central semi-transparente (igual estilo)
        // Se calcula en proporción al tamaño del mundo para consistencia visual.
        float panelW = viewport.getWorldWidth() * 0.55f;
        float panelH = viewport.getWorldHeight() * 0.55f;
        float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
        // Bajamos un poco para no tapar el logo del fondo
        float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

        // Rectángulo semi-transparente
        // Oscurece el área para mejorar contraste del texto.
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil
        // 4 rectángulos finos para simular borde con baja opacidad.
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(0.02f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restaura el color del batch antes de dibujar texto.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        // -------------------------
        // Contenido (centrado)
        // -------------------------
        float centerX = viewport.getWorldWidth() * 0.5f;

        // (Mantengo título, pero dentro del panel y más discreto)
        // Se dibuja un título con escala ligeramente reducida para no dominar el panel.
        String titulo = safeT("options_title", "Opciones");
        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;

        font.getData().setScale(originalScaleX * 0.95f, originalScaleY * 0.95f);
        drawCentered(titulo, panelY + panelH * 0.90f);
        font.getData().setScale(originalScaleX, originalScaleY);

        // Items dentro del panel (sin solaparse)
        // Se distribuyen verticalmente según una base y un paso proporcional a la altura del panel.
        float baseY = panelY + panelH * 0.72f;
        float step = panelH * 0.18f;

        // Idioma (ES/GL/EN) + mostrar nombre
        // Obtiene código de idioma y lo convierte a un nombre localizable para mostrarlo.
        String lang = Idiomas.getIdioma();
        String langName =
            "es".equalsIgnoreCase(lang) ? safeT("language_es", "Castellano")
                : ("gl".equalsIgnoreCase(lang) ? safeT("language_gl", "Galego")
                : ("en".equalsIgnoreCase(lang) ? safeT("language_en", "English") : lang));

        String idiomaLine = safeT("options_language", "Idioma") + ": " + langName;
        drawItem(0, idiomaLine, baseY - step * 0);

        // Dificultad
        // ✅ FIX: se muestra desde prefs para que se vea "Fácil" aunque Configuracion esté forzando un valor.
        boolean hard = isDificultadDificil();
        String diffName = hard
            ? safeT("difficulty_hard", "Dificil")
            : safeT("difficulty_easy", "Facil");

        String diffLine = safeT("options_difficulty", "Dificultad") + ": " + diffName;
        drawItem(1, diffLine, baseY - step * 1);

        // Sonidos (música + efectos)
        // Lee el estado persistido y lo muestra como ON/OFF localizado.
        boolean sonidoOn = isSonidoActivado();
        String onOff = sonidoOn ? safeT("on", "ON") : safeT("off", "OFF");
        String sonidoLine = safeT("options_sound", "Sonidos") + ": " + onOff;
        drawItem(2, sonidoLine, baseY - step * 2);

        // Vibración
        // Igual que sonido: estado persistido mostrado como ON/OFF.
        boolean vibraOn = isVibracionActivada();
        String onOffV = vibraOn ? safeT("on", "ON") : safeT("off", "OFF");
        String vibraLine = safeT("options_vibration", "Vibracion") + ": " + onOffV;
        drawItem(3, vibraLine, baseY - step * 3);

        // Volver
        // Elemento de salida del menú de opciones hacia el menú principal.
        drawItem(4, safeT("back", "Volver"), baseY - step * 4);

        // Hint FUERA del panel, abajo del todo
        // Instrucciones de control: en móvil no se muestran.
        String hint = safeT("options_hint", "←→ cambia, ↑↓ selecciona, ENTER acepta, ESC vuelve");
        font.getData().setScale(originalScaleX * 0.85f, originalScaleY * 0.85f);

        if (!esMovil) {
            drawCentered(hint, viewport.getWorldHeight() * 0.08f);
        }

        // Restaura escala original tras dibujar el hint.
        font.getData().setScale(originalScaleX, originalScaleY);

        juego.batch.end();
    }

    private void handleInput() {

        // ------------------------------------------------------------
        // MÓVIL: SOLO TÁCTIL
        // ------------------------------------------------------------
        // Selección por toque: se detecta qué banda vertical (ítem) se ha tocado dentro del panel.
        if (esMovil) {

            if (Gdx.input.justTouched()) {

                // NO invertir Y antes de unproject
                // Se convierten coordenadas de pantalla a mundo mediante viewport.unproject.
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                Vector2 tmp = new Vector2(sx, sy);
                viewport.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                // Recalcula las dimensiones del panel exactamente igual que en render.
                float panelW = viewport.getWorldWidth() * 0.55f;
                float panelH = viewport.getWorldHeight() * 0.55f;
                float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
                float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

                // Base y paso vertical para el mapeo de toque a cada item.
                float baseY = panelY + panelH * 0.72f;
                float step = panelH * 0.18f;

                // Zona clicable: todo el ancho del panel, y una "banda" por item
                float halfH = step * 0.55f;

                float y0 = baseY - step * 0;
                float y1 = baseY - step * 1;
                float y2 = baseY - step * 2;
                float y3 = baseY - step * 3;
                float y4 = baseY - step * 4;

                // Primero se comprueba si el toque está dentro del ancho del panel.
                if (wx >= panelX && wx <= panelX + panelW) {

                    // Cada condición detecta si el toque cae en la banda del ítem correspondiente.
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

            // En móvil se evita procesar teclas.
            return;
        }

        // ------------------------------------------------------------
        // PC: TECLADO como hasta ahora
        // ------------------------------------------------------------
        // Navegación vertical circular entre ítems.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + ITEM_COUNT) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }

        // Se detectan cambios izquierda/derecha para alternar valores (idioma/dificultad/switches).
        boolean left = Gdx.input.isKeyJustPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);

        // Cambios por flechas laterales según el ítem seleccionado.
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

        // ENTER/SPACE ejecuta la acción del ítem seleccionado.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selected == 0) toggleIdioma();
            else if (selected == 1) toggleDificultad();
            else if (selected == 2) toggleSonido();
            else if (selected == 3) toggleVibracion();
            else if (selected == 4) volver();
        }

        // ESC vuelve al menú anterior.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volver();
        }
    }

    private void toggleIdioma() {
        // Ahora cicla ES -> GL -> EN -> ES
        // Cambia el idioma de forma circular; el sistema de Idiomas se encarga de recargar el bundle.
        String current = Idiomas.getIdioma();
        String next;
        if ("es".equalsIgnoreCase(current)) next = "gl";
        else if ("gl".equalsIgnoreCase(current)) next = "en";
        else next = "es";
        Idiomas.setIdioma(next);
        // No hace falta recrear pantalla: ya se recarga el bundle en setIdioma()
    }

    private boolean isDificultadDificil() {
        // Devuelve false por defecto (fácil) si prefs aún no está disponible.
        if (prefs == null) return false;
        // lee cualquiera de las dos (prioriza KEY_DIFICULTAD_DIFICIL si existe)
        if (prefs.contains(KEY_DIFICULTAD_DIFICIL)) return prefs.getBoolean(KEY_DIFICULTAD_DIFICIL, false);
        return prefs.getBoolean(KEY_DIFICULTAD_DIFICIL_ALT, false);
    }

    private void toggleDificultad() {
        // Alterna entre fácil y difícil persistiendo el valor para que el cambio se refleje siempre.
        boolean nextHard = !isDificultadDificil();

        if (prefs != null) {
            prefs.putBoolean(KEY_DIFICULTAD_DIFICIL, nextHard);
            prefs.putBoolean(KEY_DIFICULTAD_DIFICIL_ALT, nextHard);
            prefs.flush();
        }

        // Sincroniza Configuracion para que el gameplay lo use.
        try {
            Configuracion.setDificultad(nextHard ? Configuracion.Dificultad.DIFICIL : Configuracion.Dificultad.FACIL);
        } catch (Exception ignored) {}
    }

    private boolean isSonidoActivado() {
        // Devuelve true por defecto si prefs aún no está disponible.
        if (prefs == null) return true;
        // lee cualquiera de las dos (prioriza KEY_SONIDO si existe)
        if (prefs.contains(KEY_SONIDO)) return prefs.getBoolean(KEY_SONIDO, true);
        return prefs.getBoolean(KEY_SONIDO_ALT, true);
    }

    private void toggleSonido() {
        // Alterna el estado de sonido y lo persiste en preferencias (en ambas claves por compatibilidad).
        if (prefs == null) return;
        boolean current = isSonidoActivado();
        boolean next = !current;

        // escribe en ambas keys (compat)
        prefs.putBoolean(KEY_SONIDO, next);
        prefs.putBoolean(KEY_SONIDO_ALT, next);
        prefs.flush();

        // sincroniza Configuracion si existe el setter
        try {
            Configuracion.setSonidoActivado(next);
        } catch (Exception ignored) {}
    }

    private boolean isVibracionActivada() {
        // Devuelve true por defecto si prefs aún no está disponible.
        if (prefs == null) return true;
        // lee cualquiera de las dos (prioriza KEY_VIBRACION si existe)
        if (prefs.contains(KEY_VIBRACION)) return prefs.getBoolean(KEY_VIBRACION, true);
        return prefs.getBoolean(KEY_VIBRACION_ALT, true);
    }

    private void toggleVibracion() {
        // Alterna el estado de vibración y lo persiste en preferencias (en ambas claves por compatibilidad).
        if (prefs == null) return;
        boolean current = isVibracionActivada();
        boolean next = !current;

        // escribe en ambas keys (compat)
        prefs.putBoolean(KEY_VIBRACION, next);
        prefs.putBoolean(KEY_VIBRACION_ALT, next);
        prefs.flush();

        // sincroniza Configuracion si existe el setter
        try {
            Configuracion.setVibracionActivada(next);
        } catch (Exception ignored) {}
    }

    private void volver() {
        // Regresa al menú principal y libera recursos asociados a esta pantalla.
        juego.setScreen(new PantallaMenu(juego));
        dispose();
    }

    private void drawItem(int idx, String text, float y) {
        // Prefijo para indicar selección actual de forma visible.
        String prefix = (idx == selected) ? "> " : "  ";
        String line = prefix + text;

        // Resalta el seleccionado con un color distinto para jerarquía visual.
        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        // Dibuja el texto centrado horizontalmente en la coordenada Y indicada.
        drawCentered(line, y);

        // Restaura el color por consistencia.
        font.setColor(Color.WHITE);
    }

    private void drawCentered(String text, float y) {
        // Layout para medir el ancho del texto y centrarlo respecto al ancho del mundo.
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
        float x = (viewport.getWorldWidth() - layout.width) * 0.5f;
        font.draw(juego.batch, layout, x, y);
    }

    private String safeT(String key, String fallback) {
        // Acceso seguro a traducciones: evita que una clave inválida provoque errores en runtime.
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        // Actualiza el viewport para mantener proporciones y adaptar el área visible.
        if (viewport != null) viewport.update(width, height, true);

        // Recalcular escala al rotar/cambiar tamaño
        // Mantiene el texto con un tamaño coherente tras cambios de resolución.
        if (font != null && viewport != null) {
            recalcularEscalaFuente();
        }
    }

    @Override
    public void dispose() {
        // Libera recursos gráficos asociados a esta pantalla.
        if (font != null) font.dispose();
        if (fondoMenu != null) fondoMenu.dispose();
        if (pixel != null) pixel.dispose();
    }
}
