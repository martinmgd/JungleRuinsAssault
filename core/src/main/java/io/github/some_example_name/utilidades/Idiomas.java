package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

public class Idiomas {

    private static final String PREFS_NAME = "juego_prefs";
    private static final String KEY_LANG = "lang"; // "es", "en", "gl", etc.

    private static I18NBundle bundle;

    private Idiomas() {}

    public static void cargar() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String lang = prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());

        // Base file sin extensión, como indica el temario
        FileHandle base = Gdx.files.internal("i18n/MyBundle");

        Locale locale = new Locale(lang);
        bundle = I18NBundle.createBundle(base, locale);
    }

    public static void setIdioma(String lang) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(KEY_LANG, lang);
        prefs.flush();
        cargar(); // recarga el bundle al cambiar
    }

    public static String getIdioma() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());
    }

    public static I18NBundle get() {
        if (bundle == null) cargar();
        return bundle;
    }

    public static String t(String key) {
        return get().get(key);
    }

    public static String f(String key, Object... args) {
        return get().format(key, args);
    }
}
