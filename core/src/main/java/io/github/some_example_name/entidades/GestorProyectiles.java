    package io.github.some_example_name.entidades;

    import com.badlogic.gdx.graphics.g2d.SpriteBatch;
    import com.badlogic.gdx.utils.Array;

    import io.github.some_example_name.utilidades.DisparoAssets;

    public class GestorProyectiles {

        private final Array<Proyectil> normales = new Array<>();
        private ChorroEspecial especial = null;

        private final DisparoAssets assets;

        // Normal (bola de fuego)
        private float velNormal = 18f;
        private int dmgNormal = 10;
        private float normalW = 0.8f;
        private float normalH = 0.8f;
        private float cdNormal = 0.12f;
        private float tNormal = 0f;

        // Especial (chorro)
        private float cdEspecial = 0.60f;
        private float tEspecial = 0f;

        public GestorProyectiles(DisparoAssets assets) {
            this.assets = assets;
        }

        public void update(float delta, float camLeftX, float viewW) {
            tNormal = Math.max(0f, tNormal - delta);
            tEspecial = Math.max(0f, tEspecial - delta);

            float rightX = camLeftX + viewW;

            for (int i = normales.size - 1; i >= 0; i--) {
                Proyectil p = normales.get(i);
                p.update(delta);
                if (p.isOutOfRange(camLeftX - 3f, rightX + 3f)) {
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
        }

        public void startEspecial(float x, float y, boolean derecha) {
            if (especial != null) return;     // ya hay uno activo
            if (tEspecial > 0f) return;
            tEspecial = cdEspecial;

            especial = new ChorroEspecial(
                assets.specialStartAnim,
                assets.specialBodyRegion,
                assets.specialEndAnim,
                x, y, derecha
            );
        }

        public void stopEspecial() {
            if (especial != null) {
                especial.setHolding(false);
            }
        }

        public boolean isEspecialActivo() {
            return especial != null;
        }

        public void setEspecialOrigin(float x, float y, boolean derecha) {
            if (especial != null) {
                especial.setOrigin(x, y, derecha);
            }
        }

        public void setEspecialHolding(boolean holding) {
            if (especial != null) {
                especial.setHolding(holding);
            }
        }
    }
