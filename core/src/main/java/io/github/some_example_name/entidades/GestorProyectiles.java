package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import io.github.some_example_name.utilidades.DisparoAssets;

public class GestorProyectiles {

    private final Array<Proyectil> normales = new Array<>();
    private final DisparoAssets assets;

    // Normal
    private float velNormal = 18f;
    private int dmgNormal = 10;
    private float normalW = 0.8f;
    private float normalH = 0.8f;
    private float cdNormal = 0.12f;
    private float tNormal = 0f;

    // Energía especial
    private float energia = 0f;
    private float energiaMax = 100f;

    // Carga por tiempo
    private float regenPorSegundo = 6f;

    // Coste del especial (si quieres que solo salga con barra completa: 100)
    private float costeEspecial = 100f;

    // Cooldown del especial (para evitar dobles disparos)
    private float cdEspecial = 0.35f;
    private float tEspecial = 0f;

    // Especial: chorro
    private final ChorroEspecial chorro;
    private boolean especialActivo = false;

    // Tamaño del inicio (frames 1-4)
    private float startW = 2.4f;
    private float startH = 2.4f;

    // Stream: grosor de menos a más (ajusta “3 o 4 veces más grande” aquí)
    private float streamHMin = 1.0f;
    private float streamHMax = 4.0f;

    // Velocidades (muy rápido)
    private float streamVelCrecimiento = 90f;
    private float streamVelAvance = 140f;

    // Ajuste vertical del segmento (frame 5)
    private float streamYOffset = -0.25f;

    // Origen para dibujar el start
    private float especialX = 0f;
    private float especialY = 0f;
    private boolean especialDerecha = true;

    public GestorProyectiles(DisparoAssets assets) {
        this.assets = assets;

        this.chorro = new ChorroEspecial(
            assets.specialStartAnim,
            assets.specialStreamRegion,
            streamHMin,
            streamHMax,
            streamVelCrecimiento,
            streamVelAvance,
            streamYOffset
        );
    }

    public void addEnergiaPorKill(float cantidad) {
        energia = Math.min(energiaMax, energia + cantidad);
    }

    public float getEnergia01() {
        return energiaMax <= 0f ? 0f : energia / energiaMax;
    }

    public boolean isEspecialActivo() {
        return especialActivo;
    }

    public void setEspecialOrigin(float x, float y, boolean derecha) {
        especialX = x;
        especialY = y;
        especialDerecha = derecha;

        if (especialActivo) {
            chorro.setOrigin(x, y, derecha);
        }
    }

    public void update(float delta, float camLeftX, float viewW) {
        tNormal = Math.max(0f, tNormal - delta);
        tEspecial = Math.max(0f, tEspecial - delta);

        energia = Math.min(energiaMax, energia + regenPorSegundo * delta);

        float rightX = camLeftX + viewW;

        for (int i = normales.size - 1; i >= 0; i--) {
            Proyectil p = normales.get(i);
            p.update(delta);
            if (p.isOutOfRange(camLeftX - 3f, rightX + 3f)) {
                normales.removeIndex(i);
            }
        }

        if (especialActivo) {
            chorro.update(delta, camLeftX, viewW);
            if (!chorro.isActive()) {
                especialActivo = false;
            }
        }
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        for (Proyectil p : normales) {
            p.draw(batch);
        }

        if (especialActivo) {
            if (chorro.isInStart()) {
                if (especialDerecha) {
                    batch.draw(chorro.getStartFrame(), especialX, especialY, startW, startH);
                } else {
                    batch.draw(chorro.getStartFrame(), especialX + startW, especialY, -startW, startH);
                }
            } else {
                chorro.draw(batch);
            }
        }
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
    }

    public void shootEspecial(float x, float y, boolean derecha, float camLeftX, float viewW) {
        if (tEspecial > 0f) return;
        if (especialActivo) return;
        if (energia < costeEspecial) return;

        energia -= costeEspecial;
        tEspecial = cdEspecial;

        especialActivo = true;
        especialX = x;
        especialY = y;
        especialDerecha = derecha;

        chorro.begin(x, y, derecha, camLeftX, viewW);
    }
}
