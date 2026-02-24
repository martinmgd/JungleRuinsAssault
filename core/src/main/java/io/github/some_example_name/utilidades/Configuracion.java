package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class Configuracion {

    private static final String PREFS_NAME = "juego_prefs";
    private static final String KEY_DIFICULTAD = "diff"; // "easy" | "hard"

    public enum Dificultad { FACIL, DIFICIL }

    private Configuracion() {}

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    public static Dificultad getDificultad() {
        String v = prefs().getString(KEY_DIFICULTAD, "hard");
        return "easy".equalsIgnoreCase(v) ? Dificultad.FACIL : Dificultad.DIFICIL;
    }

    public static void setDificultad(Dificultad d) {
        prefs().putString(KEY_DIFICULTAD, (d == Dificultad.FACIL) ? "easy" : "hard");
        prefs().flush();
    }
}
