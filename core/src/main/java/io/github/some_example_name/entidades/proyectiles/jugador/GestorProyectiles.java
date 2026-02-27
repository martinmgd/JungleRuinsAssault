package io.github.some_example_name.entidades.proyectiles.jugador;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import io.github.some_example_name.utilidades.DisparoAssets;

public class GestorProyectiles {

    private final Array<Proyectil> normales = new Array<>();
    private AtaqueEspecial especial = null;

    private final DisparoAssets assets;

    private Sound sonidoNormal;
    private Sound sonidoEspecial;

    // Volúmenes (ajusta aquí)
    private float volNormal = 0.20f;
    private float volEspecial = 0.20f;

    private float velNormal = 18f;
    private int dmgNormal = 10;
    private float normalW = 0.8f;
    private float normalH = 0.8f;
    private float cdNormal = 0.12f;
    private float tNormal = 0f;

    private float cdEspecial = 0.60f;
    private float tEspecial = 0f;

    public GestorProyectiles(DisparoAssets assets) {
        this.assets = assets;

        // Carga de sonidos (assets/audio/)
        sonidoNormal = Gdx.audio.newSound(Gdx.files.internal("audio/ataque_normal.mp3"));
        sonidoEspecial = Gdx.audio.newSound(Gdx.files.internal("audio/ataque_especial.mp3"));
    }

    public void update(float delta, float camLeftX, float viewW) {
        tNormal = Math.max(0f, tNormal - delta);
        tEspecial = Math.max(0f, tEspecial - delta);

        float rightX = camLeftX + viewW;

        for (int i = normales.size - 1; i >= 0; i--) {
            Proyectil p = normales.get(i);
            p.update(delta);

            if (p.isEliminar() || p.isOutOfRange(camLeftX - 3f, rightX + 3f)) {
                normales.removeIndex(i);
            }
        }

        if (especial != null) {
            especial.update(delta, camLeftX, viewW);
            if (especial.isFinished()) {
                especial = null;
            }
        }
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        for (Proyectil p : normales) p.draw(batch);
        if (especial != null) especial.draw(batch, camLeftX, viewW);
    }

    public void shootNormal(float x, float y, boolean derecha) {
        if (tNormal > 0f) return;
        tNormal = cdNormal;

        float vx = derecha ? velNormal : -velNormal;

        normales.add(new Proyectil(
            assets.normalAnim,
            x, y,
            vx,
            derecha,
            normalW, normalH,
            dmgNormal
        ));

        if (sonidoNormal != null) sonidoNormal.play(volNormal);
    }

    public void startEspecial(float x, float y, boolean derecha, float viewH) {
        if (especial != null) return;
        if (tEspecial > 0f) return;
        tEspecial = cdEspecial;

        especial = new AtaqueEspecial(
            assets.specialBuildAnim,
            assets.specialLoopAnim,
            assets.specialEndAnim,
            x, y, derecha, viewH
        );

        especial.setDamage(50);

        if (sonidoEspecial != null) sonidoEspecial.play(volEspecial);
    }

    public boolean isEspecialActivo() {
        return especial != null;
    }

    public Array<Proyectil> getNormales() {
        return normales;
    }

    public AtaqueEspecial getEspecial() {
        return especial;
    }

    // (Opcional) cambiar volúmenes en runtime
    public void setVolumenNormal(float v) {
        volNormal = Math.max(0f, Math.min(1f, v));
    }

    public void setVolumenEspecial(float v) {
        volEspecial = Math.max(0f, Math.min(1f, v));
    }

    public void dispose() {
        if (sonidoNormal != null) {
            sonidoNormal.dispose();
            sonidoNormal = null;
        }
        if (sonidoEspecial != null) {
            sonidoEspecial.dispose();
            sonidoEspecial = null;
        }
    }
}
