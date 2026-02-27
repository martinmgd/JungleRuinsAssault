package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class Configuracion {

    private static final String PREFS_NAME = "juego_prefs";

    private static final String KEY_DIFICULTAD = "diff"; // "easy" | "hard"

    // NUEVO: opciones
    private static final String KEY_SONIDO = "sound";      // true | false
    private static final String KEY_VIBRACION = "vibration"; // true | false

    public enum Dificultad { FACIL, DIFICIL }

    private Configuracion() {}

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    // -----------------------
    // DIFICULTAD (sin cambios)
    // -----------------------
    public static Dificultad getDificultad() {
        String v = prefs().getString(KEY_DIFICULTAD, "hard");
        return "easy".equalsIgnoreCase(v) ? Dificultad.FACIL : Dificultad.DIFICIL;
    }

    public static void setDificultad(Dificultad d) {
        prefs().putString(KEY_DIFICULTAD, (d == Dificultad.FACIL) ? "easy" : "hard");
        prefs().flush();
    }

    // -----------------------
    // NUEVO: SONIDO
    // -----------------------
    public static boolean isSonidoActivado() {
        return prefs().getBoolean(KEY_SONIDO, true);
    }

    public static void setSonidoActivado(boolean activado) {
        prefs().putBoolean(KEY_SONIDO, activado);
        prefs().flush();
    }

    // -----------------------
    // NUEVO: VIBRACIÓN
    // -----------------------
    public static boolean isVibracionActivada() {
        return prefs().getBoolean(KEY_VIBRACION, true);
    }

    public static void setVibracionActivada(boolean activado) {
        prefs().putBoolean(KEY_VIBRACION, activado);
        prefs().flush();
    }
}
