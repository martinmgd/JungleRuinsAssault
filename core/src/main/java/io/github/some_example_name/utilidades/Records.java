package io.github.some_example_name.utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestor de Records (TOP 10) persistidos en fichero local.
 *
 * <p>Formato del archivo: una línea por record</p>
 * <pre>
 * ABC,12345
 * XYZ,999
 * </pre>
 *
 * <p>Orden: de mayor a menor puntuación.</p>
 */
public final class Records {

    /** Nombre del fichero local donde se guardan los records. */
    // Nombre del archivo en almacenamiento local (por plataforma) donde se persiste el TOP.
    private static final String FILE_NAME = "records.txt";

    /** Máximo de records guardados. */
    // Tamaño máximo permitido del ranking. Cualquier entrada adicional se descarta al guardar.
    public static final int MAX_RECORDS = 10;

    /*
     * Constructor privado: evita instanciación.
     * Esta clase es un utilitario estático (API de acceso/gestión de records).
     */
    private Records() { }

    /**
     * Entrada de record (3 letras + score).
     */
    public static class Entry {
        /** Iniciales (3 letras). */
        // Iniciales normalizadas a 3 caracteres A-Z en mayúsculas para garantizar formato estable.
        public final String initials;

        /** Puntuación. */
        // Puntuación numérica asociada a la entrada.
        public final int score;

        /**
         * Crea una entrada.
         * @param initials iniciales (se normaliza a 3 letras mayúsculas).
         * @param score puntuación.
         */
        public Entry(String initials, int score) {
            // Normaliza las iniciales a un formato consistente para evitar inconsistencias de guardado/lectura.
            this.initials = normalizeInitials(initials);
            this.score = score;
        }
    }

    /**
     * Devuelve el fichero local donde se guardan los records.
     * @return FileHandle local.
     */
    private static FileHandle file() {
        // Uso de Gdx.files.local(): almacenamiento persistente local de la aplicación.
        // Permite lectura/escritura sin depender del classpath (a diferencia de internal()).
        return Gdx.files.local(FILE_NAME);
    }

    /**
     * Carga los records del fichero (si no existe, devuelve lista vacía).
     * @return lista de entries (no nula).
     */
    public static List<Entry> load() {
        // Obtiene handle al fichero de records.
        FileHandle fh = file();

        // Si no existe, no hay records aún; se devuelve lista vacía para evitar nulls.
        if (!fh.exists()) return new ArrayList<>();

        // Lee el archivo completo como texto UTF-8.
        String text = fh.readString("UTF-8");

        // Divide por líneas soportando saltos Windows (\r\n) o Unix (\n).
        String[] lines = text.split("\\r?\\n");

        ArrayList<Entry> out = new ArrayList<>();

        // Parseo defensivo línea a línea: ignora líneas vacías/corruptas.
        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            // Cada línea debe ser "INI,SCORE".
            String[] parts = line.split(",");
            if (parts.length != 2) continue;

            String ini = parts[0].trim();
            String scs = parts[1].trim();

            try {
                // Convierte la puntuación a entero; si falla, la línea se considera corrupta.
                int sc = Integer.parseInt(scs);
                out.add(new Entry(ini, sc));
            } catch (Exception ignored) {
                // línea corrupta -> ignorar
                // Se ignora para no romper la carga completa por un dato mal formado.
            }
        }

        // Ordena descendentemente por score y recorta a TOP10.
        sortDesc(out);
        if (out.size() > MAX_RECORDS) {
            out.subList(MAX_RECORDS, out.size()).clear();
        }
        return out;
    }

    /**
     * Guarda una lista de records en fichero (se trunca a TOP 10).
     * @param entries lista a guardar.
     */
    public static void save(List<Entry> entries) {
        // Entrada defensiva: si llega null, se guarda lista vacía (archivo quedará vacío).
        if (entries == null) entries = new ArrayList<>();

        // Se copia a una lista temporal para no mutar la colección original del llamador.
        ArrayList<Entry> tmp = new ArrayList<>(entries);

        // Se ordena y se recorta al tamaño máximo permitido.
        sortDesc(tmp);
        if (tmp.size() > MAX_RECORDS) {
            tmp.subList(MAX_RECORDS, tmp.size()).clear();
        }

        // Serialización a texto: una línea por record "INI,SCORE".
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tmp.size(); i++) {
            Entry e = tmp.get(i);
            // Se normalizan iniciales al escribir para garantizar formato estable en disco.
            sb.append(normalizeInitials(e.initials)).append(",").append(e.score);
            if (i < tmp.size() - 1) sb.append("\n");
        }

        // Escribe el contenido reemplazando el fichero existente (append=false) en UTF-8.
        file().writeString(sb.toString(), false, "UTF-8");
    }

    /**
     * Añade un record al TOP 10 (y persiste).
     * @param initials iniciales (3 letras).
     * @param score puntuación.
     * @return true si entró en el TOP 10; false si no.
     */
    public static boolean addIfTop10(String initials, int score) {
        // Carga ranking actual.
        List<Entry> list = load();

        // Añade la nueva entrada y reordena.
        list.add(new Entry(initials, score));
        sortDesc(list);

        // Determina si la entrada ha quedado dentro del rango TOP10.
        // Se compara por score y por iniciales normalizadas para confirmar presencia.
        boolean dentro = false;
        for (int i = 0; i < list.size() && i < MAX_RECORDS; i++) {
            Entry e = list.get(i);
            if (e.score == score && e.initials.equals(normalizeInitials(initials))) {
                dentro = true;
                break;
            }
        }

        // Recorta lista a MAX_RECORDS antes de guardar.
        if (list.size() > MAX_RECORDS) {
            list = new ArrayList<>(list.subList(0, MAX_RECORDS));
        }

        // Persiste el TOP actualizado.
        save(list);
        return dentro;
    }

    /**
     * Devuelve si una puntuación califica para TOP 10.
     * @param score puntuación.
     * @return true si califica (lista incompleta o supera al último).
     */
    public static boolean qualifies(int score) {
        // Carga ranking actual para comparar contra el último (peor) del TOP10.
        List<Entry> list = load();

        // Si hay menos de 10 entries, cualquier score entra por definición.
        if (list.size() < MAX_RECORDS) return true;

        // Compara con la puntuación del último elemento (mínimo dentro del TOP).
        int last = list.get(list.size() - 1).score;
        return score > last;
    }

    /**
     * Normaliza iniciales a 3 letras A-Z en mayúsculas.
     * Si faltan letras, se rellena con 'A' para no romper formato.
     * @param s texto entrada.
     * @return "ABC" siempre longitud 3.
     */
    public static String normalizeInitials(String s) {
        // Entrada defensiva.
        if (s == null) s = "";
        s = s.toUpperCase();

        // Se filtra a caracteres A-Z para evitar símbolos/espacios/números.
        StringBuilder out = new StringBuilder(3);
        for (int i = 0; i < s.length() && out.length() < 3; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') out.append(c);
        }

        // Rellena hasta longitud 3 para mantener el formato del archivo y UI.
        while (out.length() < 3) out.append('A');

        return out.toString();
    }

    /*
     * Ordena una lista de entries por score descendente.
     * Se usa Comparator explícito para mantener compatibilidad y control claro del criterio.
     */
    private static void sortDesc(List<Entry> list) {
        Collections.sort(list, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                // Orden descendente: b.score vs a.score.
                return Integer.compare(b.score, a.score);
            }
        });
    }
}
