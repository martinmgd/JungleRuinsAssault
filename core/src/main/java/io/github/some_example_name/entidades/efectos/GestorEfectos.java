package io.github.some_example_name.entidades.efectos;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class GestorEfectos {

    private final Array<EfectoImpacto> impactos = new Array<>();

    private final TextureRegion impactoRegion;

    private float impactoW = 0.55f;
    private float impactoH = 0.55f;
    private float impactoDuracion = 0.14f;

    public GestorEfectos(TextureRegion impactoRegion) {
        this.impactoRegion = impactoRegion;
    }

    public void setImpactoConfig(float w, float h, float duracion) {
        this.impactoW = w;
        this.impactoH = h;
        this.impactoDuracion = duracion;
    }

    public void spawnImpacto(float cx, float cy, Color color) {
        impactos.add(new EfectoImpacto(
            impactoRegion,
            cx, cy,
            impactoW, impactoH,
            impactoDuracion,
            color
        ));
    }

    public void update(float delta) {
        for (int i = impactos.size - 1; i >= 0; i--) {
            EfectoImpacto e = impactos.get(i);
            e.update(delta);
            if (e.isFinished()) impactos.removeIndex(i);
        }
    }

    public void draw(SpriteBatch batch) {
        Color prev = new Color(batch.getColor());

        for (EfectoImpacto e : impactos) {
            e.draw(batch);
        }

        batch.setColor(prev);
    }
}
