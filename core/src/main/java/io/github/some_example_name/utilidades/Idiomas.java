package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

public class Idiomas {

    // Nombre del archivo de preferencias donde se guardan configuraciones del juego.
    // En este caso se utiliza para almacenar el idioma elegido por el usuario.
    private static final String PREFS_NAME = "juego_prefs";

    // Clave usada dentro de las preferencias para almacenar el idioma actual.
    // El valor suele ser un código ISO de idioma: "es", "en", "gl", etc.
    private static final String KEY_LANG = "lang";

    // Bundle de internacionalización cargado actualmente.
    // Contiene todas las cadenas traducidas para el idioma activo.
    private static I18NBundle bundle;

    // Constructor privado para evitar instanciación de la clase.
    // La clase se usa únicamente mediante métodos estáticos.
    private Idiomas() {}

    public static void cargar() {

        // Obtiene el objeto Preferences donde se almacenan las configuraciones del juego.
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Recupera el idioma guardado en preferencias.
        // Si no existe, se usa el idioma por defecto del sistema operativo.
        String lang = prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());

        // Ruta base del bundle de traducciones.
        // No se especifica extensión ni idioma porque LibGDX busca automáticamente
        // archivos como:
        //   MyBundle_es.properties
        //   MyBundle_en.properties
        //   MyBundle_gl.properties
        // según el Locale indicado.
        FileHandle base = Gdx.files.internal("i18n/MyBundle");

        // Construye el objeto Locale a partir del código de idioma recuperado.
        Locale locale = new Locale(lang);

        // Carga el bundle correspondiente al locale especificado.
        // LibGDX seleccionará el archivo adecuado según el idioma disponible.
        bundle = I18NBundle.createBundle(base, locale);
    }

    public static void setIdioma(String lang) {

        // Obtiene las preferencias del juego.
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Guarda el nuevo idioma seleccionado.
        prefs.putString(KEY_LANG, lang);

        // Escribe los cambios físicamente en el almacenamiento.
        prefs.flush();

        // Recarga el bundle para aplicar inmediatamente el nuevo idioma.
        cargar();
    }

    public static String getIdioma() {

        // Recupera las preferencias.
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Devuelve el idioma almacenado o el idioma por defecto del sistema.
        return prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());
    }

    public static I18NBundle get() {

        // Si el bundle aún no se ha cargado (primera llamada),
        // se ejecuta el método cargar().
        if (bundle == null) cargar();

        // Devuelve el bundle activo.
        return bundle;
    }

    public static String t(String key) {

        // Devuelve la traducción asociada a la clave indicada.
        // Ejemplo: t("menu_play") -> "Jugar" / "Play" / etc.
        return get().get(key);
    }

    public static String f(String key, Object... args) {

        // Devuelve una cadena formateada usando parámetros.
        // Se utiliza cuando la traducción contiene placeholders.
        //
        // Ejemplo en properties:
        // score_message=Score: {0}
        //
        // Uso:
        // f("score_message", 1500)
        //
        // Resultado:
        // "Score: 1500"
        return get().format(key, args);
    }
}
