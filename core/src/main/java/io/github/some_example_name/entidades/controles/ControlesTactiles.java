package io.github.some_example_name.entidades.controles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ControlesTactiles extends InputAdapter {

    // Viewport de UI/controles: se usa para convertir coordenadas de pantalla a coordenadas del “mundo UI”.
    // Se asume que este mismo viewport es el que utiliza la pantalla para dibujar los controles.
    private final Viewport viewport;

    // Texturas de interfaz: joystick y botones de acción.
    private final Texture texJoystick;
    private final Texture texDisparar;
    private final Texture texEspecial;
    private final Texture texPausa;

    // Tamaños físicos aproximados (cm)
    // Se definen en centímetros para mantener una escala coherente entre dispositivos con distinta densidad (PPI).
    private static final float CM_DISPARAR = 1.0f;   // 1 cm
    private static final float CM_ESPECIAL = 0.9f;   // similar
    private static final float CM_PAUSA = 1.5f;
    private static final float CM_JOYSTICK = 1.6f;   // un poco más grande (antes 1.3)

    // Márgenes y separaciones (cm)
    // Espaciado ergonómico: margen respecto a bordes y separación entre botones.
    private static final float CM_MARGIN = 0.35f;
    private static final float CM_GAP = 0.25f;

    // Zona válida del joystick (zona baja izquierda)
    // Limita la activación del joystick flotante a la esquina inferior izquierda para evitar interferencias.
    private static final float JOY_ZONE_W_FRAC = 0.45f;
    private static final float JOY_ZONE_H_FRAC = 0.45f;

    // “Pausa un poco más abajo de la puntuación”
    // Desplaza el botón de pausa hacia abajo desde el borde superior para no solaparse con HUD.
    private static final float CM_PAUSA_TOP_DROP = 1.2f;

    // Rects en MUNDO
    // Áreas de interacción (hitboxes) de los botones en coordenadas del viewport (mundo UI).
    private final Rectangle rDisparar = new Rectangle();
    private final Rectangle rEspecial = new Rectangle();
    private final Rectangle rPausa = new Rectangle();

    // Joystick flotante (centro y knob en MUNDO)
    // El joystick aparece donde el usuario toca, y se oculta al levantar el dedo.
    private final Vector2 joyCenter = new Vector2();
    private final Vector2 joyKnob = new Vector2();
    private boolean joyVisible = false;
    private int joyPointer = -1;

    // Tamaño del joystick y radio máximo de desplazamiento del knob (en unidades de mundo UI).
    private float joyRadiusW = 1f;
    private float joySizeW = 1f;

    // Salidas
    // Dirección normalizada del joystick (aprox. -1..1) para integrarse con movimiento/acciones.
    private float dirX = 0f;
    private float dirY = 0f;

    // Saltar ahora se hace con el joystick hacia arriba
    // Umbral para considerar “arriba” (ajústalo si quieres más/menos sensible)
    // Se considera salto cuando el joystick está activo y dirY supera el umbral.
    private static final float JOY_JUMP_THRESHOLD = 0.55f;

    // Estados de los botones (presionado / no presionado).
    private boolean disparar = false;
    private boolean especial = false;
    private boolean pausa = false;

    // Multitouch: pointers para botones
    // Se guardan los pointers para asociar “touchUp” al mismo dedo que inició el “touchDown”.
    private int ptrDisparar = -1;
    private int ptrEspecial = -1;
    private int ptrPausa = -1;

    // Reutilizable para unproject
    // Vector temporal para evitar asignaciones repetidas y reducir GC.
    private final Vector2 tmp = new Vector2();

    public ControlesTactiles(Viewport viewport) {
        // Se inyecta el viewport que define el espacio de coordenadas para UI y entradas táctiles.
        this.viewport = viewport;

        // Carga de assets de controles desde el directorio de sprites del HUD.
        texJoystick = new Texture("sprites/hud/controles/joystick.png");
        texDisparar = new Texture("sprites/hud/controles/boton_disparar.png");
        texEspecial = new Texture("sprites/hud/controles/boton_especial.png");
        texPausa = new Texture("sprites/hud/controles/boton_pausa.png");

        // Cálculo inicial del layout de botones y parámetros del joystick.
        recalcularLayout();
    }

    // -------------------------
    // Conversión cm -> px -> mundo
    // -------------------------
    private float pxPorCmX() {
        // Convierte PPI horizontal a píxeles por centímetro.
        // Si el dispositivo no reporta PPI fiable, se usa un fallback estándar.
        float ppi = Gdx.graphics.getPpiX();
        if (ppi <= 1f) ppi = 160f;
        return ppi / 2.54f;
    }

    private float pxPorCmY() {
        // Convierte PPI vertical a píxeles por centímetro con el mismo criterio de fallback.
        float ppi = Gdx.graphics.getPpiY();
        if (ppi <= 1f) ppi = 160f;
        return ppi / 2.54f;
    }

    private float cmToPxX(float cm) {
        // Centímetros -> píxeles (eje X).
        return cm * pxPorCmX();
    }

    private float cmToPxY(float cm) {
        // Centímetros -> píxeles (eje Y).
        return cm * pxPorCmY();
    }

    private float pxToWorldX(float px) {
        // Píxeles -> mundo UI (eje X) usando la proporción viewportWorldWidth / screenWidth.
        float sw = Math.max(1f, (float) Gdx.graphics.getWidth());
        return px * (viewport.getWorldWidth() / sw);
    }

    private float pxToWorldY(float px) {
        // Píxeles -> mundo UI (eje Y) usando la proporción viewportWorldHeight / screenHeight.
        float sh = Math.max(1f, (float) Gdx.graphics.getHeight());
        return px * (viewport.getWorldHeight() / sh);
    }

    private float cmToWorldW(float cm) {
        // Conversión cm -> px -> mundo para medidas horizontales.
        return pxToWorldX(cmToPxX(cm));
    }

    private float cmToWorldH(float cm) {
        // Conversión cm -> px -> mundo para medidas verticales.
        return pxToWorldY(cmToPxY(cm));
    }

    private float clamp(float v, float min, float max) {
        // Restringe valores a un rango (útil para mantener UI dentro de pantalla).
        return Math.max(min, Math.min(max, v));
    }

    private Vector2 screenToWorld(int screenX, int screenY) {
        // Convierte coordenadas de pantalla a coordenadas del viewport.
        // Se delega el manejo del eje Y en viewport.unproject (no se invierte manualmente).
        tmp.set(screenX, screenY);
        viewport.unproject(tmp); // SIN invertir Y
        return tmp;
    }

    // -------------------------
    // Layout en mundo (botones fijos)
    // -------------------------
    public void recalcularLayout() {
        // Recalcula posiciones y tamaños de los botones en función del tamaño actual del viewport.
        float ww = viewport.getWorldWidth();
        float wh = viewport.getWorldHeight();

        float marginX = cmToWorldW(CM_MARGIN);
        float marginY = cmToWorldH(CM_MARGIN);
        float gapX = cmToWorldW(CM_GAP);
        float gapY = cmToWorldH(CM_GAP);

        float dispararW = cmToWorldW(CM_DISPARAR);
        float dispararH = cmToWorldH(CM_DISPARAR);

        float especialW = cmToWorldW(CM_ESPECIAL);
        float especialH = cmToWorldH(CM_ESPECIAL);

        float pausaW = cmToWorldW(CM_PAUSA);
        float pausaH = cmToWorldH(CM_PAUSA);

        // Disparar: abajo derecha, pero un poco más a la izquierda
        // (dejamos hueco para el especial a la derecha)
        float dispararX = ww - marginX - especialW - gapX - dispararW;
        float dispararY = marginY;

        // Especial: al lado, un poco levantado pero más a la derecha
        float especialX = ww - marginX - especialW;
        float especialY = dispararY + (dispararH * 0.35f) + (gapY * 0.4f);

        // Pausa: arriba derecha, bajada desde el HUD
        float pausaX = ww - marginX - pausaW;
        float pausaY = wh - marginY - pausaH - cmToWorldH(CM_PAUSA_TOP_DROP);

        // Clamp
        // Garantiza que los botones quedan completamente dentro del área visible.
        dispararX = clamp(dispararX, 0f, ww - dispararW);
        dispararY = clamp(dispararY, 0f, wh - dispararH);

        especialX = clamp(especialX, 0f, ww - especialW);
        especialY = clamp(especialY, 0f, wh - especialH);

        pausaX = clamp(pausaX, 0f, ww - pausaW);
        pausaY = clamp(pausaY, 0f, wh - pausaH);

        // Actualiza hitboxes de botones.
        rDisparar.set(dispararX, dispararY, dispararW, dispararH);
        rEspecial.set(especialX, especialY, especialW, especialH);
        rPausa.set(pausaX, pausaY, pausaW, pausaH);

        // Configura tamaño de joystick y radio de movimiento del knob.
        joySizeW = cmToWorldW(CM_JOYSTICK);
        joyRadiusW = joySizeW * 0.45f;
        if (joyRadiusW < 0.0001f) joyRadiusW = 0.0001f;
    }

    // -------------------------
    // Input
    // -------------------------
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        // Por seguridad ante resize/cambios
        // Recalcula layout para asegurar que las hitboxes corresponden al tamaño actual.
        recalcularLayout();

        Vector2 t = screenToWorld(screenX, screenY);
        float x = t.x;
        float y = t.y;

        // Botones fijos
        // Asigna el pointer al botón presionado para liberar correctamente en touchUp.
        if (rPausa.contains(x, y)) {
            pausa = true;
            ptrPausa = pointer;
            return true;
        }
        if (rDisparar.contains(x, y)) {
            disparar = true;
            ptrDisparar = pointer;
            return true;
        }
        if (rEspecial.contains(x, y)) {
            especial = true;
            ptrEspecial = pointer;
            return true;
        }

        // Joystick flotante: solo zona baja izquierda
        // Se activa únicamente si el toque cae en la región permitida y no hay otro joystick activo.
        float ww = viewport.getWorldWidth();
        float wh = viewport.getWorldHeight();

        float zoneW = ww * JOY_ZONE_W_FRAC;
        float zoneH = wh * JOY_ZONE_H_FRAC;

        boolean enZonaJoystick = (x <= zoneW && y <= zoneH);

        if (enZonaJoystick && joyPointer == -1) {
            joyPointer = pointer;
            joyVisible = true;

            // El centro y el knob comienzan en la posición de toque.
            joyCenter.set(x, y);
            joyKnob.set(x, y);

            // Direcciones iniciales a 0.
            dirX = 0f;
            dirY = 0f;

            return true;
        }

        // Se consume el evento para evitar propagación a otros input processors.
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // Solo el pointer propietario del joystick puede modificar su dirección.
        if (pointer == joyPointer && joyVisible) {
            Vector2 t = screenToWorld(screenX, screenY);
            actualizarJoystick(t.x, t.y);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        // Libera estados de botones en función del pointer que los activó.
        if (pointer == ptrPausa) { pausa = false; ptrPausa = -1; }
        if (pointer == ptrDisparar) { disparar = false; ptrDisparar = -1; }
        if (pointer == ptrEspecial) { especial = false; ptrEspecial = -1; }

        // Si se suelta el joystick, se oculta y se reinician direcciones.
        if (pointer == joyPointer) {
            joyPointer = -1;
            joyVisible = false;
            dirX = 0f;
            dirY = 0f;
        }

        return true;
    }

    private void actualizarJoystick(float touchX, float touchY) {
        // Vector desde el centro del joystick al punto actual del toque.
        float dx = touchX - joyCenter.x;
        float dy = touchY - joyCenter.y;

        // Limita la magnitud al radio máximo para mantener el knob dentro del círculo de control.
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > joyRadiusW) {
            float k = joyRadiusW / Math.max(0.0001f, len);
            dx *= k;
            dy *= k;
        }

        // Actualiza la posición del knob.
        joyKnob.set(joyCenter.x + dx, joyCenter.y + dy);

        // Normaliza direcciones respecto al radio.
        dirX = dx / Math.max(0.0001f, joyRadiusW);
        dirY = dy / Math.max(0.0001f, joyRadiusW);
    }

    // -------------------------
    // Render (en MUNDO)
    // Se dibuja con el mismo batch/proyección que uses para HUD en tu pantalla.
    // -------------------------
    public void render(SpriteBatch batch) {
        // Asegura color por defecto (sin tint) antes de dibujar texturas de UI.
        batch.setColor(1f, 1f, 1f, 1f);

        // Botones fijos siempre visibles (SIN botón saltar)
        batch.draw(texDisparar, rDisparar.x, rDisparar.y, rDisparar.width, rDisparar.height);
        batch.draw(texEspecial, rEspecial.x, rEspecial.y, rEspecial.width, rEspecial.height);
        batch.draw(texPausa, rPausa.x, rPausa.y, rPausa.width, rPausa.height);

        // Joystick flotante (invisible hasta tocar)
        // Solo se renderiza mientras está activo para no añadir ruido visual.
        if (joyVisible) {
            float s = joySizeW;
            batch.draw(texJoystick, joyKnob.x - s * 0.5f, joyKnob.y - s * 0.5f, s, s);
        }
    }

    // -------------------------
    // Getters
    // -------------------------
    // Devuelve dirección horizontal/vertical del joystick (normalizada).
    public float getDirX() { return dirX; }
    public float getDirY() { return dirY; }

    // Saltar ahora depende del joystick hacia arriba
    public boolean isSaltar() {
        // Considera salto únicamente si el joystick está activo y se empuja hacia arriba por encima del umbral.
        return joyVisible && joyPointer != -1 && dirY > JOY_JUMP_THRESHOLD;
    }

    // Estados actuales de los botones para consumo desde la lógica del juego.
    public boolean isDisparar() { return disparar; }
    public boolean isEspecial() { return especial; }
    public boolean isPausa() { return pausa; }

    public void dispose() {
        // Libera recursos nativos de texturas asociadas a controles.
        if (texJoystick != null) texJoystick.dispose();
        if (texDisparar != null) texDisparar.dispose();
        if (texEspecial != null) texEspecial.dispose();
        if (texPausa != null) texPausa.dispose();
    }
}
