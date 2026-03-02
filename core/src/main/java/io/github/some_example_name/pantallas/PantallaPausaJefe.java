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

public class PantallaPausaJefe extends ScreenAdapter {

    // Referencia al juego principal para cambiar pantallas y acceder al batch de render.
    private final Main juego;

    // Referencia a la pantalla de la sala del jefe (se usa para reanudar, reiniciar o dibujar "congelada" detrás).
    private final PantallaSalaJefe salaJefe;

    // Cámara ortográfica en coordenadas de pantalla (pixeles).
    private OrthographicCamera camera;

    // Fuentes separadas para título y opciones, con escalas distintas.
    private BitmapFont fontTitle;
    private BitmapFont fontItem;

    // Layout para medir texto y centrarlo en pantalla.
    private GlyphLayout layout;

    // Factor de escalado de fuente basado en densidad (dpi) y tamaño de viewport.
    private float factorEscaladoFuente;

    // Textura 1x1 blanca usada como "pixel" para dibujar paneles/bordes semi-transparentes.
    private Texture pixel;

    // Índice del elemento de menú actualmente seleccionado (0..2).
    private int seleccionado = 0;

    // ------------------------------------------------------------
    // CONTROLES: PC teclado / Móvil táctil automático
    // ------------------------------------------------------------
    // Flag de plataforma para activar interacción táctil en móvil.
    private boolean esMovil = false;

    public PantallaPausaJefe(Main juego, PantallaSalaJefe salaJefe) {
        // Dependencias: juego para navegación, salaJefe para reanudar/reiniciar y dibujar fondo.
        this.juego = juego;
        this.salaJefe = salaJefe;
    }

    @Override
    public void show() {

        // Detección de plataforma móvil (Android/iOS) para habilitar selección por toque.
        esMovil = (Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.iOS);

        // Asegura que el sistema de idiomas/bundle está cargado.
        Idiomas.get();

        // Si vienes a pausa, pausa música (no stop) para poder reanudar sin reiniciar la pista.
        if (salaJefe != null) salaJefe.pauseMusica();

        // Cámara en coordenadas de pantalla: viewport igual a la resolución actual.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Fuentes bitmap: se usan dos instancias para poder escalar y colorear de forma independiente.
        fontTitle = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));
        fontItem = new BitmapFont(Gdx.files.internal("fonts/Jersey10-Regular.fnt"));

        // Escalado de fuente: combina densidad de pantalla y proporción viewport/pantalla.
        float dpiScale = Gdx.graphics.getDensity();
        factorEscaladoFuente = (camera.viewportHeight / Gdx.graphics.getHeight()) * dpiScale;

        // Escalas distintas: título más grande que las opciones.
        fontTitle.getData().setScale(factorEscaladoFuente * 2f);
        fontItem.getData().setScale(factorEscaladoFuente * 1.4f);

        // Color base blanco; luego se aplican prefijos/estilos en el texto.
        fontTitle.setColor(Color.WHITE);
        fontItem.setColor(Color.WHITE);

        // Filtro Nearest para preservar estética pixel-art en el atlas de la fuente.
        fontTitle.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fontItem.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Layout reutilizable para centrado de texto sin crear objetos por frame.
        layout = new GlyphLayout();

        // Textura 1x1 para dibujar rectángulos: panel, borde y overlays.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    @Override
    public void render(float delta) {

        // Forzar viewport completo para que el render de detrás use toda la pantalla real.
        // Esto asegura que el frame "congelado" del boss se vea correctamente a pantalla completa.
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Dibujar el boss congelado detrás (para que la transparencia sea real).
        // Se renderiza con delta 0 para evitar avanzar lógica/animaciones del jefe durante la pausa.
        if (salaJefe != null) salaJefe.render(0);

        // ------------------------------------------------------------
        // MÓVIL: TÁCTIL (tocar una opción)
        // ------------------------------------------------------------
        // En móvil, se resuelve la opción seleccionada por posición del toque respecto al panel.
        if (esMovil && Gdx.input.justTouched()) {

            float screenW = Gdx.graphics.getWidth();
            float screenH = Gdx.graphics.getHeight();

            // Panel estrecho y centrado (idéntico a la lógica de dibujo).
            float panelW = screenW * 0.245f;
            float panelH = screenH * 0.5f;
            float panelX = (screenW - panelW) * 0.5f;
            float panelY = (screenH - panelH) * 0.5f;

            // Coordenadas toque (pantalla), invertimos Y para compararlas con el layout.
            // Se evita unproject y se trabaja directamente en pixeles (Y invertida).
            float wx = Gdx.input.getX();
            float wy = screenH - Gdx.input.getY();

            // Posiciones verticales de los items dentro del panel.
            float y0 = panelY + panelH * 0.55f;
            float dy = panelH * 0.18f;

            float yContinue = y0;
            float yRestart = y0 - dy;
            float yMenu = y0 - dy * 2f;

            // Medio alto de banda clicable por item para tolerancia al toque.
            float halfH = dy * 0.55f;

            // El toque solo se considera si está dentro del rango horizontal del panel.
            boolean dentroPanelX = (wx >= panelX && wx <= panelX + panelW);

            if (dentroPanelX) {
                if (wy >= yContinue - halfH && wy <= yContinue + halfH) {
                    // Opción 0: continuar.
                    seleccionado = 0;

                    if (salaJefe != null) salaJefe.resumeMusica();
                    juego.setScreen(salaJefe);
                    return;

                } else if (wy >= yRestart - halfH && wy <= yRestart + halfH) {
                    // Opción 1: reiniciar sala del jefe.
                    seleccionado = 1;

                    if (salaJefe != null) salaJefe.stopMusica();
                    juego.setScreen(new PantallaSalaJefe(juego, salaJefe.getJugador(), salaJefe.getScore()));
                    return;

                } else if (wy >= yMenu - halfH && wy <= yMenu + halfH) {
                    // Opción 2: volver al menú principal.
                    seleccionado = 2;

                    if (salaJefe != null) salaJefe.stopMusica();
                    juego.setScreen(new PantallaMenu(juego));
                    return;
                }
            }
        }

        // ESC / P -> continuar (reanudar música del jefe).
        // En PC, esto actúa como un atajo rápido para salir de la pausa.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)) {

            if (salaJefe != null) salaJefe.resumeMusica();
            juego.setScreen(salaJefe);
            return;
        }

        // Navegación de selección por teclado: arriba decrementa (con wrap).
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            seleccionado = (seleccionado + 3 - 1) % 3;
        }

        // Navegación de selección por teclado: abajo incrementa (con wrap).
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            seleccionado = (seleccionado + 1) % 3;
        }

        // Confirmación por ENTER: ejecuta la acción según opción seleccionada.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (seleccionado == 0) {
                // Continuar: reanudar música y volver a la sala del jefe.
                if (salaJefe != null) salaJefe.resumeMusica();
                juego.setScreen(salaJefe);

            } else if (seleccionado == 1) {
                // Reiniciar: parar música actual del jefe y recrear la sala (nuevo Screen).
                if (salaJefe != null) salaJefe.stopMusica();
                juego.setScreen(new PantallaSalaJefe(juego, salaJefe.getJugador(), salaJefe.getScore()));

            } else {
                // Menú: parar música del jefe y volver al menú principal.
                if (salaJefe != null) salaJefe.stopMusica();
                juego.setScreen(new PantallaMenu(juego));
            }
            return;
        }

        // Render del overlay de pausa (panel + texto).
        juego.batch.setProjectionMatrix(camera.combined);
        juego.batch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Igual que PantallaPausa: panel más estrecho y centrado.
        float panelW = screenW * 0.245f;
        float panelH = screenH * 0.5f;
        float panelX = (screenW - panelW) * 0.5f;
        float panelY = (screenH - panelH) * 0.5f;

        // Rectángulo semi-transparente como fondo del panel.
        juego.batch.setColor(0f, 0f, 0f, 0.35f);
        juego.batch.draw(pixel, panelX, panelY, panelW, panelH);

        // Borde sutil del panel: 4 líneas finas basadas en un grosor proporcional.
        juego.batch.setColor(1f, 1f, 1f, 0.10f);
        float b = Math.max(2f, Math.min(panelW, panelH) * 0.01f);
        juego.batch.draw(pixel, panelX, panelY, panelW, b);
        juego.batch.draw(pixel, panelX, panelY + panelH - b, panelW, b);
        juego.batch.draw(pixel, panelX, panelY, b, panelH);
        juego.batch.draw(pixel, panelX + panelW - b, panelY, b, panelH);

        // Restaura color para texto.
        juego.batch.setColor(1f, 1f, 1f, 1f);

        float centerX = screenW * 0.5f;

        // Título centrado (safe por si falta la key).
        drawCentered(fontTitle, safeT("pause_title", "Pausa"), centerX, panelY + panelH * 0.82f);

        // Items centrados con separación vertical constante.
        float y0 = panelY + panelH * 0.55f;
        float dy = panelH * 0.18f;

        // Prefijo "> " indica visualmente la opción seleccionada (estilo menú clásico).
        drawCentered(fontItem, (seleccionado == 0 ? "> " : "") + safeT("pause_continue", "Continuar"), centerX, y0);
        drawCentered(fontItem, (seleccionado == 1 ? "> " : "") + safeT("pause_restart", "Reiniciar"), centerX, y0 - dy);
        drawCentered(fontItem, (seleccionado == 2 ? "> " : "") + safeT("pause_menu", "Menu"), centerX, y0 - dy * 2f);

        juego.batch.end();
    }

    private void drawCentered(BitmapFont font, String text, float centerX, float y) {
        // Mide el texto con GlyphLayout y desplaza X para dibujar centrado.
        layout.setText(font, text);
        float x = centerX - layout.width / 2f;
        font.draw(juego.batch, layout, x, y);
    }

    private String safeT(String key, String fallback) {
        // Traducción con fallback: si falta la key o hay error de bundle, no rompe la UI.
        try {
            return Idiomas.t(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        // Ajusta la cámara a la nueva resolución para mantener el layout en coordenadas de pantalla.
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
    }

    @Override
    public void dispose() {
        // Libera recursos nativos asociados a fuentes y textura pixel.
        if (fontTitle != null) fontTitle.dispose();
        if (fontItem != null) fontItem.dispose();
        if (pixel != null) pixel.dispose();
    }
}
