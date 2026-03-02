package io.github.some_example_name.utilidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class DisparoAssets implements Disposable {

    // Normal (3 frames en una sola fila)
    // Ruta del spritesheet del disparo normal.
    public static final String PATH_NORMAL_SHEET = "sprites/proyectiles/fireball_sheet.png";
    // Número de frames horizontales en el spritesheet normal.
    public static final int NORMAL_FRAMES = 3;
    // Duración de cada frame del disparo normal (animación rápida y cíclica).
    public static final float NORMAL_FRAME_TIME = 0.06f;

    // Especial: 12 frames en una sola fila
    // Ruta del spritesheet del disparo especial.
    public static final String PATH_SPECIAL_SHEET = "sprites/proyectiles/special_sheet.png";
    // Número total de frames horizontales en el spritesheet especial.
    public static final int SPECIAL_TOTAL_FRAMES = 12;

    // Reparto dentro del sheet especial:
    // BUILD: frames 1..8
    // LOOP:  frames 9..11 (loop)
    // END:   frame 12 (1 frame)
    // División lógica del spritesheet especial en tres fases:
    // - BUILD: fase de carga/construcción del ataque (no loop).
    // - LOOP: fase sostenida mientras el ataque está activo (loop).
    // - END: frame final para rematar/cerrar el efecto (no loop).
    public static final int SPECIAL_BUILD_FRAMES = 8;
    public static final int SPECIAL_LOOP_FRAMES = 3;
    public static final int SPECIAL_END_FRAMES = 1;

    // Timing de animación para las fases del ataque especial que sí se controlan aquí.
    public static final float SPECIAL_LOOP_FRAME_TIME = 0.06f;
    public static final float SPECIAL_END_FRAME_TIME = 0.08f;

    // Texturas cargadas en memoria para cada spritesheet.
    // Se conservan como campos para poder liberarlas en dispose().
    private final Texture normalTex;
    private final Texture specialTex;

    // Animación del disparo normal: recorre los 3 frames en loop constante.
    public final Animation<TextureRegion> normalAnim;

    // Animaciones del disparo especial separadas por fase:
    // - build: 8 frames, sin loop. El ritmo fino se controla fuera (AtaqueEspecial).
    // - loop: 3 frames, loop constante mientras dure el estado activo.
    // - end: 1 frame, sin loop para cierre del efecto.
    public final Animation<TextureRegion> specialBuildAnim; // 8 frames, no loop (los timings finos se controlan en AtaqueEspecial)
    public final Animation<TextureRegion> specialLoopAnim;  // 3 frames, loop
    public final Animation<TextureRegion> specialEndAnim;   // 1 frame, no loop

    /*
     * Constructor:
     * - Carga texturas desde disco (assets)
     * - Construye Animation<TextureRegion> a partir de spritesheets horizontales
     * - Separa el sheet especial en fases de BUILD/LOOP/END para control de lógica externa
     */
    public DisparoAssets() {
        // Carga de texturas (esto reserva memoria GPU, importante liberar en dispose()).
        normalTex = new Texture(PATH_NORMAL_SHEET);
        specialTex = new Texture(PATH_SPECIAL_SHEET);

        // Animación normal construida directamente desde el sheet con playmode LOOP.
        normalAnim = buildAnimFromSheet(normalTex, NORMAL_FRAMES, NORMAL_FRAME_TIME, Animation.PlayMode.LOOP);

        // Divide el sheet especial en todos sus frames para luego crear sub-secuencias.
        TextureRegion[] all = splitSheetHorizontal(specialTex, SPECIAL_TOTAL_FRAMES);

        // -----------------------------
        // Fase BUILD (frames 0..7)
        // -----------------------------
        TextureRegion[] build = new TextureRegion[SPECIAL_BUILD_FRAMES];
        // Copia los primeros 8 frames del array "all" a "build".
        System.arraycopy(all, 0, build, 0, SPECIAL_BUILD_FRAMES);
        // frameTime "dummy": el tiempo real de avance se controla desde AtaqueEspecial,
        // por lo que aquí se usa un valor pequeño que no gobierna el ritmo final.
        specialBuildAnim = new Animation<>(0.01f, build); // frameTime dummy: el ritmo real lo controla AtaqueEspecial
        // Se reproduce una vez (sin loop).
        specialBuildAnim.setPlayMode(Animation.PlayMode.NORMAL);

        // -----------------------------
        // Fase LOOP (frames 8..10)
        // -----------------------------
        TextureRegion[] loop = new TextureRegion[SPECIAL_LOOP_FRAMES];
        // Copia los frames inmediatamente posteriores al BUILD.
        System.arraycopy(all, SPECIAL_BUILD_FRAMES, loop, 0, SPECIAL_LOOP_FRAMES);
        // Aquí sí se define el frameTime real para una animación cíclica estable.
        specialLoopAnim = new Animation<>(SPECIAL_LOOP_FRAME_TIME, loop);
        // Repetición infinita durante el estado activo.
        specialLoopAnim.setPlayMode(Animation.PlayMode.LOOP);

        // -----------------------------
        // Fase END (frame 11)
        // -----------------------------
        TextureRegion[] end = new TextureRegion[SPECIAL_END_FRAMES];
        // Copia el último frame (cierre) después del bloque LOOP.
        System.arraycopy(all, SPECIAL_BUILD_FRAMES + SPECIAL_LOOP_FRAMES, end, 0, SPECIAL_END_FRAMES);
        // Animación de cierre de 1 frame: útil para uniformidad de acceso con Animation API.
        specialEndAnim = new Animation<>(SPECIAL_END_FRAME_TIME, end);
        // Se reproduce una vez (sin loop).
        specialEndAnim.setPlayMode(Animation.PlayMode.NORMAL);
    }

    /*
     * Construye una animación genérica a partir de un spritesheet horizontal:
     * - Divide el sheet en N frames
     * - Crea Animation con frameTime y playMode indicados
     */
    private Animation<TextureRegion> buildAnimFromSheet(Texture sheet, int frames, float frameTime, Animation.PlayMode mode) {
        TextureRegion[] arr = splitSheetHorizontal(sheet, frames);
        Animation<TextureRegion> anim = new Animation<>(frameTime, arr);
        anim.setPlayMode(mode);
        return anim;
    }

    /*
     * Divide un spritesheet en una única fila horizontal:
     * - Asume que todos los frames tienen el mismo ancho: sheetWidth / frames
     * - Mantiene toda la altura del sheet para cada frame
     * - Devuelve un array de TextureRegion referenciando la misma textura base
     */
    private TextureRegion[] splitSheetHorizontal(Texture sheet, int frames) {
        int frameW = sheet.getWidth() / frames;
        int frameH = sheet.getHeight();

        TextureRegion[] arr = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            // Crea una región por frame recortando la textura base.
            arr[i] = new TextureRegion(sheet, i * frameW, 0, frameW, frameH);
        }
        return arr;
    }

    /*
     * Libera recursos GPU asociados a las texturas.
     * Es crítico llamar a este método cuando ya no se necesiten los assets
     * para evitar fugas de memoria en dispositivos con recursos limitados.
     */
    @Override
    public void dispose() {
        normalTex.dispose();
        specialTex.dispose();
    }
}
