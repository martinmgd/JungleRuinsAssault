package io.github.some_example_name.entidades.entidades.controles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ControlesTactiles extends InputAdapter {

    private final Viewport viewport;

    private final Texture texJoystick;
    private final Texture texDisparar;
    private final Texture texEspecial;
    private final Texture texPausa;

    // Tamaños físicos aproximados (cm)
    private static final float CM_DISPARAR = 1.0f;   // 1 cm
    private static final float CM_ESPECIAL = 0.9f;   // similar
    private static final float CM_PAUSA = 1.5f;
    private static final float CM_JOYSTICK = 1.6f;   // ✅ un poco más grande (antes 1.3)

    // Márgenes y separaciones (cm)
    private static final float CM_MARGIN = 0.35f;
    private static final float CM_GAP = 0.25f;

    // Zona válida del joystick (zona baja izquierda)
    private static final float JOY_ZONE_W_FRAC = 0.45f;
    private static final float JOY_ZONE_H_FRAC = 0.45f;

    // “Pausa un poco más abajo de la puntuación”
    private static final float CM_PAUSA_TOP_DROP = 1.2f;

    // Rects en MUNDO
    private final Rectangle rDisparar = new Rectangle();
    private final Rectangle rEspecial = new Rectangle();
    private final Rectangle rPausa = new Rectangle();

    // Joystick flotante (centro y knob en MUNDO)
    private final Vector2 joyCenter = new Vector2();
    private final Vector2 joyKnob = new Vector2();
    private boolean joyVisible = false;
    private int joyPointer = -1;

    private float joyRadiusW = 1f;
    private float joySizeW = 1f;

    // Salidas
    private float dirX = 0f;
    private float dirY = 0f;

    // ✅ Saltar ahora se hace con el joystick hacia arriba
    // Umbral para considerar “arriba” (ajústalo si quieres más/menos sensible)
    private static final float JOY_JUMP_THRESHOLD = 0.55f;

    private boolean disparar = false;
    private boolean especial = false;
    private boolean pausa = false;

    // Multitouch: pointers para botones
    private int ptrDisparar = -1;
    private int ptrEspecial = -1;
    private int ptrPausa = -1;

    // Reutilizable para unproject
    private final Vector2 tmp = new Vector2();

    public ControlesTactiles(Viewport viewport) {
        this.viewport = viewport;

        texJoystick = new Texture("sprites/hud/controles/joystick.png");
        texDisparar = new Texture("sprites/hud/controles/boton_disparar.png");
        texEspecial = new Texture("sprites/hud/controles/boton_especial.png");
        texPausa = new Texture("sprites/hud/controles/boton_pausa.png");

        recalcularLayout();
    }

    // -------------------------
    // Conversión cm -> px -> mundo
    // -------------------------
    private float pxPorCmX() {
        float ppi = Gdx.graphics.getPpiX();
        if (ppi <= 1f) ppi = 160f;
        return ppi / 2.54f;
    }

    private float pxPorCmY() {
        float ppi = Gdx.graphics.getPpiY();
        if (ppi <= 1f) ppi = 160f;
        return ppi / 2.54f;
    }

    private float cmToPxX(float cm) {
        return cm * pxPorCmX();
    }

    private float cmToPxY(float cm) {
        return cm * pxPorCmY();
    }

    private float pxToWorldX(float px) {
        float sw = Math.max(1f, (float) Gdx.graphics.getWidth());
        return px * (viewport.getWorldWidth() / sw);
    }

    private float pxToWorldY(float px) {
        float sh = Math.max(1f, (float) Gdx.graphics.getHeight());
        return px * (viewport.getWorldHeight() / sh);
    }

    private float cmToWorldW(float cm) {
        return pxToWorldX(cmToPxX(cm));
    }

    private float cmToWorldH(float cm) {
        return pxToWorldY(cmToPxY(cm));
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private Vector2 screenToWorld(int screenX, int screenY) {
        tmp.set(screenX, screenY);
        viewport.unproject(tmp); // ✅ SIN invertir Y
        return tmp;
    }

    // -------------------------
    // Layout en mundo (botones fijos)
    // -------------------------
    public void recalcularLayout() {
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

        // ✅ Disparar: abajo derecha, pero un poco más a la izquierda
        // (dejamos hueco para el especial a la derecha)
        float dispararX = ww - marginX - especialW - gapX - dispararW;
        float dispararY = marginY;

        // ✅ Especial: al lado, un poco levantado pero más a la derecha
        float especialX = ww - marginX - especialW;
        float especialY = dispararY + (dispararH * 0.35f) + (gapY * 0.4f);

        // ✅ Pausa: arriba derecha, bajada desde el HUD
        float pausaX = ww - marginX - pausaW;
        float pausaY = wh - marginY - pausaH - cmToWorldH(CM_PAUSA_TOP_DROP);

        // Clamp
        dispararX = clamp(dispararX, 0f, ww - dispararW);
        dispararY = clamp(dispararY, 0f, wh - dispararH);

        especialX = clamp(especialX, 0f, ww - especialW);
        especialY = clamp(especialY, 0f, wh - especialH);

        pausaX = clamp(pausaX, 0f, ww - pausaW);
        pausaY = clamp(pausaY, 0f, wh - pausaH);

        rDisparar.set(dispararX, dispararY, dispararW, dispararH);
        rEspecial.set(especialX, especialY, especialW, especialH);
        rPausa.set(pausaX, pausaY, pausaW, pausaH);

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
        recalcularLayout();

        Vector2 t = screenToWorld(screenX, screenY);
        float x = t.x;
        float y = t.y;

        // Botones fijos
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
        float ww = viewport.getWorldWidth();
        float wh = viewport.getWorldHeight();

        float zoneW = ww * JOY_ZONE_W_FRAC;
        float zoneH = wh * JOY_ZONE_H_FRAC;

        boolean enZonaJoystick = (x <= zoneW && y <= zoneH);

        if (enZonaJoystick && joyPointer == -1) {
            joyPointer = pointer;
            joyVisible = true;

            joyCenter.set(x, y);
            joyKnob.set(x, y);

            dirX = 0f;
            dirY = 0f;

            return true;
        }

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer == joyPointer && joyVisible) {
            Vector2 t = screenToWorld(screenX, screenY);
            actualizarJoystick(t.x, t.y);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if (pointer == ptrPausa) { pausa = false; ptrPausa = -1; }
        if (pointer == ptrDisparar) { disparar = false; ptrDisparar = -1; }
        if (pointer == ptrEspecial) { especial = false; ptrEspecial = -1; }

        if (pointer == joyPointer) {
            joyPointer = -1;
            joyVisible = false;
            dirX = 0f;
            dirY = 0f;
        }

        return true;
    }

    private void actualizarJoystick(float touchX, float touchY) {
        float dx = touchX - joyCenter.x;
        float dy = touchY - joyCenter.y;

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > joyRadiusW) {
            float k = joyRadiusW / Math.max(0.0001f, len);
            dx *= k;
            dy *= k;
        }

        joyKnob.set(joyCenter.x + dx, joyCenter.y + dy);

        dirX = dx / Math.max(0.0001f, joyRadiusW);
        dirY = dy / Math.max(0.0001f, joyRadiusW);
    }

    // -------------------------
    // Render (en MUNDO)
    // Se dibuja con el mismo batch/proyección que uses para HUD en tu pantalla.
    // -------------------------
    public void render(SpriteBatch batch) {
        batch.setColor(1f, 1f, 1f, 1f);

        // ✅ Botones fijos siempre visibles (SIN botón saltar)
        batch.draw(texDisparar, rDisparar.x, rDisparar.y, rDisparar.width, rDisparar.height);
        batch.draw(texEspecial, rEspecial.x, rEspecial.y, rEspecial.width, rEspecial.height);
        batch.draw(texPausa, rPausa.x, rPausa.y, rPausa.width, rPausa.height);

        // Joystick flotante (invisible hasta tocar)
        if (joyVisible) {
            float s = joySizeW;
            batch.draw(texJoystick, joyKnob.x - s * 0.5f, joyKnob.y - s * 0.5f, s, s);
        }
    }

    // -------------------------
    // Getters
    // -------------------------
    public float getDirX() { return dirX; }
    public float getDirY() { return dirY; }

    // ✅ Saltar ahora depende del joystick hacia arriba
    public boolean isSaltar() {
        return joyVisible && joyPointer != -1 && dirY > JOY_JUMP_THRESHOLD;
    }

    public boolean isDisparar() { return disparar; }
    public boolean isEspecial() { return especial; }
    public boolean isPausa() { return pausa; }

    public void dispose() {
        if (texJoystick != null) texJoystick.dispose();
        if (texDisparar != null) texDisparar.dispose();
        if (texEspecial != null) texEspecial.dispose();
        if (texPausa != null) texPausa.dispose();
    }
}
