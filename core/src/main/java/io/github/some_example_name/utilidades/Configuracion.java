package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class Configuracion {

    // Nombre del contenedor de preferencias persistentes (almacenamiento local por plataforma).
    // LibGDX crea/gestiona este storage internamente (SharedPreferences en Android, etc.).
    private static final String PREFS_NAME = "juego_prefs";

    // Clave usada para persistir la dificultad del juego.
    // Se almacena como string para permitir compatibilidad y lectura sencilla.
    private static final String KEY_DIFICULTAD = "diff"; // "easy" | "hard"

    // NUEVO: opciones
    // Clave para activar/desactivar el sonido en el juego.
    private static final String KEY_SONIDO = "sound";      // true | false

    // Clave para activar/desactivar la vibración (especialmente relevante en móvil).
    private static final String KEY_VIBRACION = "vibration"; // true | false

    // Enumeración de dificultad expuesta al resto del código para evitar valores mágicos.
    public enum Dificultad { FACIL, DIFICIL }

    /*
     * Constructor privado para impedir instanciación.
     * Esta clase actúa como utilitario estático (accessor de preferencias).
     */
    private Configuracion() {}

    /*
     * Acceso centralizado a las preferencias.
     * Garantiza que todas las operaciones lean/escriban sobre el mismo espacio de storage.
     */
    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    // -----------------------
    // DIFICULTAD (sin cambios)
    // -----------------------

    /*
     * Recupera la dificultad configurada desde preferencias.
     * - Si no existe valor guardado, usa "hard" como valor por defecto.
     * - Traduce el string persistido a la enumeración Dificultad.
     */
    public static Dificultad getDificultad() {
        String v = prefs().getString(KEY_DIFICULTAD, "hard");
        return "easy".equalsIgnoreCase(v) ? Dificultad.FACIL : Dificultad.DIFICIL;
    }

    /*
     * Guarda la dificultad seleccionada en preferencias.
     * - Persiste el valor como string ("easy" / "hard") para mantener un formato estable.
     * - flush() fuerza la escritura inmediata a disco (persistencia).
     */
    public static void setDificultad(Dificultad d) {
        prefs().putString(KEY_DIFICULTAD, (d == Dificultad.FACIL) ? "easy" : "hard");
        prefs().flush();
    }

    // -----------------------
    // NUEVO: SONIDO
    // -----------------------

    /*
     * Indica si el sonido está activado.
     * - Valor por defecto: true (sonido habilitado si el usuario no ha configurado nada).
     */
    public static boolean isSonidoActivado() {
        return prefs().getBoolean(KEY_SONIDO, true);
    }

    /*
     * Activa o desactiva el sonido y persiste el cambio.
     * flush() asegura que el estado quede guardado inmediatamente.
     */
    public static void setSonidoActivado(boolean activado) {
        prefs().putBoolean(KEY_SONIDO, activado);
        prefs().flush();
    }

    // -----------------------
    // NUEVO: VIBRACIÓN
    // -----------------------

    /*
     * Indica si la vibración está activada.
     * - Valor por defecto: true (habilitada si el usuario no ha configurado nada).
     */
    public static boolean isVibracionActivada() {
        return prefs().getBoolean(KEY_VIBRACION, true);
    }

    /*
     * Activa o desactiva la vibración y persiste el cambio.
     * flush() fuerza persistencia para que el ajuste sobreviva reinicios de la app.
     */
    public static void setVibracionActivada(boolean activado) {
        prefs().putBoolean(KEY_VIBRACION, activado);
        prefs().flush();
    }
}
