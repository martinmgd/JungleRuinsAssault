package io.github.some_example_name.utilidades;

/**
 * Puente simple para compartir la lectura del sensor de luz (lux) desde AndroidLauncher (Android)
 * hacia el módulo core del juego (libGDX).
 *
 * En proyectos libGDX el módulo "core" es multiplataforma y no puede acceder directamente
 * a APIs específicas de Android (como sensores del dispositivo). Por eso se utiliza esta
 * clase estática como intermediaria:
 *
 * AndroidLauncher (módulo Android):
 *  - Lee el sensor de luz mediante SensorManager.
 *  - Llama a SensorLuz.setLux(valor) cada vez que recibe una lectura.
 *
 * Core del juego:
 *  - Consulta el valor mediante SensorLuz.getLux() para adaptar el comportamiento del juego.
 *
 * Ejemplo de uso en el juego:
 *  - Ajustar la oscuridad de una sala según la luz ambiental real del dispositivo.
 *
 * Convención de valores:
 *   lux >= 0  → lectura válida del sensor
 *   lux < 0   → sensor no disponible o todavía sin inicializar
 */
public final class SensorLuz {

    /**
     * Último valor de lux leído desde el sensor.
     *
     * Se declara como volatile para garantizar visibilidad entre hilos, ya que
     * el sensor puede actualizarse desde el hilo del sistema Android mientras
     * el juego lo lee desde el hilo de render de libGDX.
     *
     * Valor inicial:
     *   -1 → indica que no hay sensor o aún no se ha recibido ninguna lectura.
     */
    private static volatile float lux = -1f;

    /**
     * Marca temporal (timestamp) en milisegundos de la última actualización del sensor.
     *
     * Se obtiene mediante System.currentTimeMillis().
     * Permite comprobar cuándo se recibió la última lectura del sensor.
     *
     * Valor inicial:
     *   0 → nunca se ha actualizado.
     */
    private static volatile long lastUpdateMs = 0L;

    /**
     * Constructor privado para evitar instanciación.
     *
     * Esta clase está diseñada como utilitaria estática (singleton implícito),
     * por lo que no tiene sentido crear instancias.
     */
    private SensorLuz() {
        // No instanciable
    }

    /**
     * Actualiza el valor de lux.
     *
     * Este método normalmente se llama desde AndroidLauncher dentro de
     * SensorEventListener.onSensorChanged().
     *
     * @param newLux valor de luz ambiental en lux (habitualmente event.values[0]).
     */
    public static void setLux(float newLux) {
        lux = newLux;
        lastUpdateMs = System.currentTimeMillis();
    }

    /**
     * Devuelve el último valor de lux leído.
     *
     * @return lux >= 0 si existe una lectura válida del sensor.
     *         lux < 0 si el sensor no está disponible o todavía no ha enviado datos.
     */
    public static float getLux() {
        return lux;
    }

    /**
     * Indica si el sensor está proporcionando lecturas válidas.
     *
     * @return true si lux >= 0 (hay lectura disponible),
     *         false si no hay sensor o aún no se ha inicializado.
     */
    public static boolean isDisponible() {
        return lux >= 0f;
    }

    /**
     * Devuelve el instante en que se recibió la última lectura del sensor.
     *
     * Puede utilizarse para comprobar si el sensor está activo o si la lectura
     * está desactualizada.
     *
     * @return timestamp en milisegundos (System.currentTimeMillis()) o 0 si nunca se actualizó.
     */
    public static long getLastUpdateMs() {
        return lastUpdateMs;
    }
}
