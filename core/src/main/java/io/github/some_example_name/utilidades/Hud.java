package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Hud implements Disposable {

    // Cámara ortográfica dedicada al HUD (espacio de pantalla).
    private final OrthographicCamera cam;

    // Viewport de pantalla: mantiene coordenadas en píxeles lógicos de pantalla para UI.
    private final Viewport viewport;

    // Contenedor de recursos del HUD (corazones, etc.). Gestiona texturas/regiones.
    private final HudAssets assets;

    // Fuente para texto del HUD (score, tiempo, bonus) y layout para medir texto.
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    // Estado actual del HUD: vida del jugador, puntuación y tiempo acumulado.
    private int vida = 100;
    private int score = 0;
    private float tiempoSeg = 0f;

    // Número máximo de corazones que se dibujan en pantalla.
    private static final int MAX_CORAZONES = 5;

    // Resolución de referencia usada para calcular escalado proporcional del HUD.
    // La UI se escala en función de la resolución real comparada con esta referencia.
    private static final float REF_W = 1280f;
    private static final float REF_H = 720f;

    // Padding base del HUD (en píxeles de referencia) desde los bordes izquierdo/superior.
    private static final float PAD_X = 22f;
    private static final float PAD_Y = 14f;

    // Tamaño base y separación de iconos de corazón (en píxeles de referencia).
    private static final float HEART_PX = 40f;
    private static final float HEART_GAP_PX = 10f;

    // Desplazamiento adicional desde el borde superior hacia abajo para la fila superior de UI.
    private static final float TOP_DOWN_PX = 10f;

    // Escalas base para tipografía del score y el tiempo (se multiplican por el escalado de UI).
    private static final float SCORE_FONT_SCALE = 2.10f;
    private static final float TIME_FONT_SCALE = 2.30f;

    // Separación vertical del texto de tiempo respecto al borde superior (en píxeles de referencia).
    private static final float TIME_TOP_PAD_PX = 34f;

    // ------------------------------------------------------------
    // BONUS (cartel temporal al terminar)
    // ------------------------------------------------------------
    // Estado del cartel de bonus: visible/no visible, valor y temporizador.
    private boolean showBonus = false;
    private int bonusValue = 0;
    private float bonusTimer = 0f;

    // Duración del cartel en pantalla y escala de fuente específica para el bonus.
    private static final float BONUS_SHOW_SECONDS = 2.2f;
    private static final float BONUS_FONT_SCALE = 1.60f;

    // Cada corazón representa este "bloque" de vida:
    // Conversión entre vida numérica y corazones visibles.
    private static final int VIDA_POR_CORAZON = 20;

    public Hud() {
        // Cámara y viewport en modo "pantalla": se renderiza UI sin depender de la cámara del mundo.
        cam = new OrthographicCamera();
        viewport = new ScreenViewport(cam);

        // Carga los recursos necesarios del HUD (texturas/regiones).
        assets = new HudAssets();

        // Fuente por defecto (BitmapFont sin archivo explícito); se escala dinámicamente en draw().
        font = new BitmapFont();
        font.getData().setScale(1f);

        // En UI pixel-art suele ser deseable mantener posiciones enteras para evitar "blur" en el texto.
        font.setUseIntegerPositions(true);
    }

    public void resize(int width, int height) {
        // Actualiza el viewport del HUD para ajustarse al tamaño actual de pantalla.
        viewport.update(width, height, true);
    }

    public void setVida(int vida) {
        // Guarda vida, asegurando que no sea negativa.
        this.vida = Math.max(0, vida);
    }

    public void setScore(int score) {
        // Guarda puntuación, asegurando que no sea negativa.
        this.score = Math.max(0, score);
    }

    public void setTiempoSeg(float tiempoSeg) {
        // Guarda tiempo en segundos, evitando valores negativos.
        this.tiempoSeg = Math.max(0f, tiempoSeg);
    }

    public void showBonusTiempo(int bonus) {
        // Activa el cartel de bonus si el valor es positivo, reiniciando su temporizador.
        if (bonus <= 0) return;
        showBonus = true;
        bonusValue = bonus;
        bonusTimer = BONUS_SHOW_SECONDS;
    }

    public void clearBonus() {
        // Fuerza a ocultar el bonus y reinicia sus valores.
        showBonus = false;
        bonusValue = 0;
        bonusTimer = 0f;
    }

    // ✅ NUEVO: para usar el mismo corazón en drops del mundo
    public TextureRegion getHeartFullRegion() {
        // Exposición de la región de corazón lleno para reutilización en otras partes del juego (p.ej. drops).
        return assets.heartFull;
    }

    private float getUiScale() {
        // Calcula un factor de escala uniforme comparando la resolución actual con la de referencia.
        // Se usa el mínimo entre escala horizontal y vertical para mantener proporciones.
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float sx = w / REF_W;
        float sy = h / REF_H;
        return Math.min(sx, sy);
    }

    //  FIX: corazones por "bloques" (20 vida = 1 corazón), NO por porcentaje con round()
    private int getCorazonesLlenos() {
        // Convierte vida a número de corazones llenos usando "ceil(vida / VIDA_POR_CORAZON)".
        // Esto garantiza que cualquier resto de vida dentro del bloque se represente como un corazón lleno.
        if (vida <= 0) return 0;

        int llenos = (vida + VIDA_POR_CORAZON - 1) / VIDA_POR_CORAZON; // ceil(vida/20)
        if (llenos < 0) llenos = 0;
        if (llenos > MAX_CORAZONES) llenos = MAX_CORAZONES;
        return llenos;
    }

    private String formatTiempo(float s) {
        // Formatea segundos acumulados en "SS" si < 1 minuto, o "M:SS" a partir de 1 minuto.
        int total = (int) Math.floor(s);
        int min = total / 60;
        int sec = total % 60;
        if (min <= 0) return String.format("%02d", sec);
        return String.format("%d:%02d", min, sec);
    }

    public void draw(SpriteBatch batch) {
        // Aplica el viewport del HUD y fija la proyección del batch a la cámara del HUD.
        viewport.apply();
        batch.setProjectionMatrix(cam.combined);

        // Actualiza temporizador del cartel de bonus usando deltaTime actual.
        float dt = Gdx.graphics.getDeltaTime();
        if (showBonus) {
            bonusTimer -= dt;
            if (bonusTimer <= 0f) {
                showBonus = false;
                bonusValue = 0;
                bonusTimer = 0f;
            }
        }

        // Escalado general del HUD según resolución.
        float scale = getUiScale();

        // Paddings escalados.
        float padX = PAD_X * scale;
        float padY = PAD_Y * scale;

        // Tamaño y separación de corazones escalados.
        float heartSize = HEART_PX * scale;
        float gap = HEART_GAP_PX * scale;

        // Desplazamiento vertical superior escalado.
        float topDown = TOP_DOWN_PX * scale;

        // Dimensiones de pantalla en coordenadas del viewport del HUD.
        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        // CORAZONES
        // Calcula cuántos corazones deben aparecer como llenos según la vida actual.
        int llenos = getCorazonesLlenos();

        // Coordenadas de inicio de la fila de corazones (izquierda-arriba).
        float startX = padX;
        float yHearts = screenH - padY - heartSize - topDown;

        // Regiones de textura para corazón lleno y vacío.
        TextureRegion full = assets.heartFull;
        TextureRegion empty = assets.heartEmpty;

        // Dibuja MAX_CORAZONES corazones, escogiendo lleno/vacío según el índice.
        for (int i = 0; i < MAX_CORAZONES; i++) {
            float x = startX + i * (heartSize + gap);
            TextureRegion r = (i < llenos) ? full : empty;
            batch.draw(r, x, yHearts, heartSize, heartSize);
        }

        // SCORE
        // Convierte score a string y lo mide para alinearlo a la derecha.
        String sScore = Integer.toString(score);

        // Ajusta escala de fuente específica del score.
        font.getData().setScale(SCORE_FONT_SCALE * scale);
        layout.setText(font, sScore);

        // Posición X alineada al borde derecho con padding.
        float scoreX = screenW - padX - layout.width;

        // Posición Y superior usada para textos en la parte alta de la UI.
        float yTopUI = screenH - padY - topDown;
        float scoreY = yTopUI;

        // Dibuja el score.
        font.draw(batch, layout, scoreX, scoreY);

        // TIEMPO
        // Formatea el tiempo y lo centra horizontalmente.
        String tStr = formatTiempo(tiempoSeg);

        // Ajusta escala de fuente específica del tiempo.
        font.getData().setScale(TIME_FONT_SCALE * scale);
        layout.setText(font, tStr);

        // Centrado horizontal: (anchoPantalla - anchoTexto)/2.
        float timeX = (screenW - layout.width) * 0.5f;
        float timeY = screenH - (TIME_TOP_PAD_PX * scale);

        // Dibuja el tiempo.
        font.draw(batch, layout, timeX, timeY);

        // BONUS TIEMPO
        // Si está activo, dibuja un texto adicional debajo del tiempo.
        if (showBonus && bonusValue > 0) {
            String bStr = "BONUS +" + bonusValue;

            // Escala de fuente específica del bonus.
            font.getData().setScale(BONUS_FONT_SCALE * scale);
            layout.setText(font, bStr);

            // Centrado horizontal; y posicionada debajo del tiempo.
            float bx = (screenW - layout.width) * 0.5f;
            float by = timeY - (34f * scale);

            // Dibuja el bonus.
            font.draw(batch, layout, bx, by);
        }

        // Restaura escala por defecto para evitar efectos colaterales si el font se usa en otro contexto.
        font.getData().setScale(1f);
    }

    @Override
    public void dispose() {
        // Libera recursos del HUD (assets y fuente).
        if (assets != null) assets.dispose();
        if (font != null) font.dispose();
    }
}
