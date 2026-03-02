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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.Constantes;
import io.github.some_example_name.utilidades.Idiomas;
import io.github.some_example_name.utilidades.UiEscala;

public class PantallaMenu extends ScreenAdapter {

    // Referencia al juego principal: permite acceder al SpriteBatch y cambiar de pantalla.
    private final Main juego;

    // Cámara ortográfica para render 2D; el viewport gestionará escalado y "letterboxing" si procede.
    private OrthographicCamera camara;
    private Viewport viewport;

    // Fuente usada para dibujar el texto del menú.
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;

    // Factor de escalado calculado en función de world->screen y la escala UI definida en el proyecto.
    private float factorEscaladoFuente;

    // Índice del ítem actualmente seleccionado (para navegación por teclado y para resaltar).
    private int selected = 0;

    // Cantidad total de elementos del menú.
    // 0: Jugar, 1: Opciones, 2: Records, 3: Ayuda, 4: Créditos, 5: Salir
    private static final int ITEM_COUNT = 6;

    // --- NUEVO: fondo + pixel 1x1 para panel (como Pausa/Muerte) ---
    // Textura de fondo del menú (imagen completa).
    private Texture fondoMenu;
    // Textura 1x1 (blanca) usada para dibujar rectángulos con alpha (panel y bordes) mediante escalado.
    private Texture pixel;
    // ---------------------------------------------------------------

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático (menú solo táctil en móvil)
    // ------------------------------------------------------------
    // Flag para decidir el esquema de entrada (táctil en móvil, teclado en PC).
    private boolean esMovil = false;

    public PantallaMenu(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        // Inicializa cámara y viewport en unidades de mundo definidas por Constantes.
        // ExtendViewport mantiene proporciones y añade bandas si el aspect ratio no coincide.
        camara = new OrthographicCamera();
        viewport = new ExtendViewport(Constantes.ANCHO_MUNDO, Constantes.ALTO_MUNDO, camara);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Detecta plataforma móvil para usar control táctil.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Fuente externa generada con Hiero (.fnt + .png)
        // Asegúrate de tener en assets/fonts/ el Jersey10.fnt y todos sus png
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Evita redondeos a píxel entero al posicionar, útil al escalar en distintos tamaños.
        font.setUseIntegerPositions(false);

        // FIX: Este menú dibuja en UNIDADES DE MUNDO (viewport),
        // por eso necesitamos el factor world->screen, y encima aplicar UiEscala (sin density).
        // worldToScreen se calcula comparando la altura del mundo con los píxeles reales de la pantalla.
        float worldToScreen = (viewport.getWorldHeight() / (float) Gdx.graphics.getHeight());
        factorEscaladoFuente = worldToScreen * UiEscala.uiScale();

        // Escalado final de la fuente para este menú (multiplicador ajustado a la estética del proyecto).
        font.getData().setScale(factorEscaladoFuente * 2.2f);
        font.setColor(Color.WHITE);

        // Filtro: Nearest para estilo pixelado
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // --- NUEVO: cargar fondo del menú (tu imagen) ---
        // Está en assets/sprites/fondos/fondoMenu.png
        fondoMenu = new Texture(Gdx.files.internal("sprites/fondos/fondoMenu.png"));
        fondoMenu.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // --- NUEVO: pixel 1x1 para dibujar rectángulo con alpha ---
        // Se crea una textura blanca 1x1 para reutilizarla como primitiva de rectángulo.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        // ------------------------------------------------

        // Asegura que el bundle está cargado (por si llegas aquí desde otra pantalla)
        Idiomas.get();
    }

    @Override
    public void render(float delta) {
        // Procesa la entrada (teclado o táctil) antes de renderizar.
        handleInput();

        // Limpia el framebuffer; el fondo real lo dibuja la textura de fondo si está disponible.
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Aplica el viewport (actualiza el glViewport) y configura la proyección del batch.
        viewport.apply();
        juego.batch.setProjectionMatrix(camara.combined);

        juego.batch.begin();

        // Fondo del menú: se estira para cubrir el mundo completo del viewport.
        if (fondoMenu != null) {
            juego.batch.setColor(1f, 1f, 1f, 1f);
            juego.batch.draw(fondoMenu, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        // Panel central semi-transparente (como Pausa/Muerte)
        // Dimensiones y posición relativas al tamaño del mundo para mantener consistencia visual.
        float panelW = viewport.getWorldWidth() * 0.55f;
        float panelH = viewport.getWorldHeight() * 0.55f;
        float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
        float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

        // Rectángulo semi-transparente: oscurece la zona central para mejorar legibilidad del texto.
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil: dibuja 4 rectángulos finos alrededor del panel.
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(0.02f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restaura color del batch para evitar tintado en el texto.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        // Items: centrados dentro del panel (grupo centrado)
        // Distancia vertical entre líneas basada en la altura de la fuente.
        float lineStep = font.getLineHeight() * 1.25f;

        // Calcula un centro vertical dentro del panel y la posición base desde donde se listan los ítems.
        float centerY = panelY + panelH * 0.55f;
        float baseY = centerY + lineStep * 2f;

        // Dibuja cada entrada del menú con localización (fallback si no hay clave).
        drawItem(0, safeT("menu_play", "Jugar"), baseY - lineStep * 0f);
        drawItem(1, safeT("menu_options", "Opciones"), baseY - lineStep * 1f);
        drawItem(2, safeT("menu_records", "Records"), baseY - lineStep * 2f);
        drawItem(3, safeT("menu_help", "Ayuda"), baseY - lineStep * 3f);
        drawItem(4, safeT("menu_credits", "Créditos"), baseY - lineStep * 4f);
        drawItem(5, safeT("menu_exit", "Salir"), baseY - lineStep * 5f);

        // Texto de ayuda inferior para teclado (PC).
        String hint = safeT("menu_hint", "↑↓ para elegir, ENTER para aceptar");

        // Guarda escala original para restaurarla tras dibujar el hint con tamaño menor.
        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;

        // Reduce ligeramente el tamaño del hint para jerarquía tipográfica.
        font.getData().setScale(originalScaleX * 0.85f, originalScaleY * 0.85f);

        // En móvil se elimina el hint inferior para no añadir texto extra en pantalla.
        if (!esMovil) {
            drawCentered(hint, viewport.getWorldHeight() * 0.08f);
        }

        // Restaura la escala original de la fuente para el siguiente frame.
        font.getData().setScale(originalScaleX, originalScaleY);

        juego.batch.end();
    }

    private void handleInput() {

        // ------------------------------------------------------------
        // MÓVIL: SOLO TÁCTIL
        // ------------------------------------------------------------
        // En móvil se interpreta el toque como selección directa del ítem en pantalla.
        if (esMovil) {

            if (Gdx.input.justTouched()) {

                // NO invertir Y antes de unproject
                // Se toma la coordenada en pantalla tal como la entrega libGDX.
                int sx = Gdx.input.getX();
                int sy = Gdx.input.getY();

                // Convierte coordenadas de pantalla a coordenadas de mundo usando el viewport.
                Vector2 tmp = new Vector2(sx, sy);
                viewport.unproject(tmp);

                float wx = tmp.x;
                float wy = tmp.y;

                // Recalcula el layout vertical para mapear el toque a cada línea del menú.
                float lineStep = font.getLineHeight() * 1.25f;

                // Recalcula el panel para delimitar la zona clicable coherente con el render.
                float panelW = viewport.getWorldWidth() * 0.55f;
                float panelH = viewport.getWorldHeight() * 0.55f;
                float panelX = (viewport.getWorldWidth() - panelW) * 0.5f;
                float panelY = (viewport.getWorldHeight() - panelH) * 0.35f;

                float centerY = panelY + panelH * 0.55f;
                float baseY = centerY + lineStep * 2f;

                // Posiciones Y de cada ítem (misma fórmula usada en render).
                float y0 = baseY - lineStep * 0f;
                float y1 = baseY - lineStep * 1f;
                float y2 = baseY - lineStep * 2f;
                float y3 = baseY - lineStep * 3f;
                float y4 = baseY - lineStep * 4f;
                float y5 = baseY - lineStep * 5f;

                // Margen vertical de detección alrededor de cada línea para que sea fácil tocar.
                float halfH = lineStep * 0.55f;

                // Detección: se valida que el toque cae dentro de la banda vertical del ítem
                // y además dentro del rango horizontal del panel.
                if (wy >= y0 - halfH && wy <= y0 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 0;
                    activateSelected();
                } else if (wy >= y1 - halfH && wy <= y1 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 1;
                    activateSelected();
                } else if (wy >= y2 - halfH && wy <= y2 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 2;
                    activateSelected();
                } else if (wy >= y3 - halfH && wy <= y3 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 3;
                    activateSelected();
                } else if (wy >= y4 - halfH && wy <= y4 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 4;
                    activateSelected();
                } else if (wy >= y5 - halfH && wy <= y5 + halfH && wx >= panelX && wx <= panelX + panelW) {
                    selected = 5;
                    activateSelected();
                }
            }

            // Importante: si es móvil, no se procesan controles de teclado.
            return;
        }

        // ------------------------------------------------------------
        // PC: TECLADO como hasta ahora
        // ------------------------------------------------------------
        // Navegación circular: al subir desde 0 va al último, y al bajar desde el último vuelve a 0.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected - 1 + ITEM_COUNT) % ITEM_COUNT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEM_COUNT;
        }

        // Activación del elemento seleccionado.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activateSelected();
        }

        // ESC en menú sale de la aplicación.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (Gdx.app != null) Gdx.app.exit();
        }
    }

    private void activateSelected() {
        // Ejecuta la acción asociada al índice seleccionado y realiza el cambio de pantalla.
        switch (selected) {
            case 0:
                juego.setScreen(new PantallaJuego(juego));
                break;
            case 1:
                juego.setScreen(new PantallaOpciones(juego));
                break;
            case 2:
                // Pantalla de lista TOP10 (la implementamos aparte: fondo negro y "1. ___")
                juego.setScreen(new PantallaRecords(juego));
                break;
            case 3:
                // Pantalla ayuda (la implementamos aparte: controles PC y móvil)
                juego.setScreen(new PantallaAyuda(juego));
                break;
            case 4:
                // Pantalla créditos
                juego.setScreen(new PantallaCreditos(juego));
                break;
            case 5:
                // Salida explícita de la aplicación.
                if (Gdx.app != null) Gdx.app.exit();
                break;
            default:
                break;
        }

        // Libera recursos de esta pantalla una vez se cambia a otra (patrón usado en el proyecto).
        dispose();
    }

    private void drawItem(int idx, String text, float y) {
        // Prefijo visual para indicar selección actual: "> " en el seleccionado, espacios en el resto.
        String prefix = (idx == selected) ? "> " : "  ";
        String line = prefix + text;

        // Resalta el seleccionado con un color diferente; el resto se dibuja en blanco.
        if (idx == selected) font.setColor(1f, 0.85f, 0.2f, 1f);
        else font.setColor(Color.WHITE);

        // Dibuja centrado horizontalmente en la coordenada Y indicada.
        drawCentered(line, y);

        // Restaura el color por consistencia para futuras llamadas.
        font.setColor(Color.WHITE);
    }

    private void drawCentered(String text, float y) {
        // Calcula el layout para obtener el ancho real del texto y centrarlo en el mundo del viewport.
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);

        // Centrado horizontal en coordenadas del mundo (no píxeles).
        float x = (viewport.getWorldWidth() - layout.width) * 0.5f;

        // Dibuja el texto en el SpriteBatch activo.
        font.draw(juego.batch, layout, x, y);
    }

    private String safeT(String key, String fallback) {
        // Acceso seguro al sistema de idiomas: si falla (clave inexistente, bundle no cargado, etc.),
        // devuelve el fallback para evitar crash en el menú.
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        // Actualiza el viewport en cambios de tamaño (ventana o rotación).
        if (viewport != null) viewport.update(width, height, true);

        // opcional pero recomendable: recalcular escala al rotar/cambiar tamaño
        // Se recalcula la escala de la fuente para mantener tamaño visual coherente.
        if (font != null && viewport != null) {
            float worldToScreen = (viewport.getWorldHeight() / (float) Gdx.graphics.getHeight());
            factorEscaladoFuente = worldToScreen * UiEscala.uiScale();
            font.getData().setScale(factorEscaladoFuente * 2.2f);
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
