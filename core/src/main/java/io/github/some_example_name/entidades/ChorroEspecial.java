package io.github.some_example_name.entidades;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ChorroEspecial {

    private enum Estado { START, BODY, END, FIN }

    private final Animation<TextureRegion> startAnim;
    private final TextureRegion bodyRegion;
    private final Animation<TextureRegion> endAnim;

    private Estado estado = Estado.START;

    private float stateTime = 0f;

    // Origen (se actualiza desde PantallaJuego mientras esté activo)
    private float ox, oy;
    private boolean derecha = true;

    // Crecimiento del chorro
    private float bodyLen = 0f;

    // Ajustes visuales
    private float startW = 2.8f;
    private float startH = 2.8f;

    private float bodyH = 6.0f;     // ajusta para que ocupe 50%-70% del alto, combinado con zoom/viewport
    private float endW = 4.8f;
    private float endH = 4.8f;

    // Velocidad de crecimiento (unidades mundo por segundo)
    private float growSpeed = 70f;

    // Cuando llega al borde, termina (puede explotar y desaparecer)
    private boolean finishWhenHitEdge = true;

    // Bajar el chorro respecto al arma (tu pedido: 0.2f aprox)
    private float yOffset = -0.2f;

    // Control: si el jugador sigue manteniendo
    private boolean holding = true;

    public ChorroEspecial(Animation<TextureRegion> startAnim,
                          TextureRegion bodyRegion,
                          Animation<TextureRegion> endAnim,
                          float ox, float oy, boolean derecha) {
        this.startAnim = startAnim;
        this.bodyRegion = bodyRegion;
        this.endAnim = endAnim;
        this.ox = ox;
        this.oy = oy;
        this.derecha = derecha;
    }

    public void setOrigin(float x, float y, boolean derecha) {
        this.ox = x;
        this.oy = y;
        this.derecha = derecha;
    }

    public void setHolding(boolean holding) {
        this.holding = holding;
        if (!holding && estado != Estado.END && estado != Estado.FIN) {
            // si suelta, forzamos final
            estado = Estado.END;
            stateTime = 0f;
        }
    }

    public boolean isFinished() {
        return estado == Estado.FIN;
    }

    public void update(float delta, float camLeftX, float viewW) {
        stateTime += delta;

        float viewRightX = camLeftX + viewW;

        switch (estado) {
            case START: {
                if (startAnim.isAnimationFinished(stateTime)) {
                    estado = Estado.BODY;
                    stateTime = 0f;
                    bodyLen = 0f;
                }
                break;
            }

            case BODY: {
                // crece muy rápido hasta el borde visible
                bodyLen += growSpeed * delta;

                float maxLen = computeMaxLenToScreenEdge(camLeftX, viewW, viewRightX);

                if (bodyLen >= maxLen) {
                    bodyLen = maxLen;

                    if (finishWhenHitEdge) {
                        estado = Estado.END;
                        stateTime = 0f;
                    } else if (!holding) {
                        estado = Estado.END;
                        stateTime = 0f;
                    }
                } else {
                    if (!holding) {
                        estado = Estado.END;
                        stateTime = 0f;
                    }
                }
                break;
            }

            case END: {
                if (endAnim.isAnimationFinished(stateTime)) {
                    estado = Estado.FIN;
                }
                break;
            }

            case FIN:
            default:
                break;
        }
    }

    private float computeMaxLenToScreenEdge(float camLeftX, float viewW, float viewRightX) {
        // Longitud desde el arma hasta el borde derecho/izquierdo de la pantalla
        if (derecha) {
            return Math.max(0f, viewRightX - ox);
        } else {
            return Math.max(0f, ox - camLeftX);
        }
    }

    public void draw(SpriteBatch batch, float camLeftX, float viewW) {
        float drawY = oy + yOffset;

        if (estado == Estado.START) {
            TextureRegion fr = startAnim.getKeyFrame(stateTime, false);
            drawFlipped(batch, fr, ox, drawY, startW, startH);
            return;
        }

        if (estado == Estado.BODY) {
            // Dibujamos start “fijo” en el arma (opcional, pero queda mejor)
            TextureRegion frStart = startAnim.getKeyFrame(Math.min(stateTime, 0.0001f), false);
            drawFlipped(batch, frStart, ox, drawY, startW, startH);

            // Body: estiramos a lo largo (continuo)
            float bodyX = derecha ? ox : (ox - bodyLen);
            float bodyW = bodyLen;

            if (bodyW > 0.01f) {
                if (derecha) {
                    batch.draw(bodyRegion, bodyX, drawY, bodyW, bodyH);
                } else {
                    // para la izquierda, dibujamos con ancho negativo
                    batch.draw(bodyRegion, bodyX + bodyW, drawY, -bodyW, bodyH);
                }
            }

            return;
        }

        if (estado == Estado.END) {
            // dibuja el body hasta el borde (si aún no estaba clavado)
            float viewRightX = camLeftX + viewW;
            float maxLen = computeMaxLenToScreenEdge(camLeftX, viewW, viewRightX);

            float len = Math.max(bodyLen, maxLen);
            float bodyX = derecha ? ox : (ox - len);
            float bodyW = len;

            if (bodyW > 0.01f) {
                if (derecha) batch.draw(bodyRegion, bodyX, drawY, bodyW, bodyH);
                else batch.draw(bodyRegion, bodyX + bodyW, drawY, -bodyW, bodyH);
            }

            // explosión en el extremo
            TextureRegion frEnd = endAnim.getKeyFrame(stateTime, false);

            float endX = derecha ? (ox + bodyW) : (ox - bodyW);
            float ex = derecha ? (endX - endW * 0.35f) : (endX - endW * 0.65f);
            float ey = drawY + bodyH * 0.25f;

            drawFlipped(batch, frEnd, ex, ey, endW, endH);
        }
    }

    private void drawFlipped(SpriteBatch batch, TextureRegion region, float x, float y, float w, float h) {
        if (derecha) {
            batch.draw(region, x, y, w, h);
        } else {
            batch.draw(region, x + w, y, -w, h);
        }
    }
}
