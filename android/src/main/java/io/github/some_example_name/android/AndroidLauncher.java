package io.github.some_example_name.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import io.github.some_example_name.Main;
import io.github.some_example_name.utilidades.SensorLuz;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication implements SensorEventListener {

    /** Manager del sistema para sensores. */
    private SensorManager sensorManager;

    /** Sensor de luz ambiente (hardware real). Puede ser null si el dispositivo no lo tiene. */
    private Sensor sensorLuz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Recommended, but not required.
        initialize(new Main(), configuration);

        // ✅ HARDWARE: Sensor de luz (TYPE_LIGHT)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        // Si no hay sensor, dejamos un valor inválido (el juego debe manejarlo sin fallar)
        if (sensorLuz == null) {
            SensorLuz.setLux(-1f);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Registrar listener al volver a primer plano (hardware real)
        if (sensorManager != null && sensorLuz != null) {
            sensorManager.registerListener(this, sensorLuz, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // ✅ Liberar listener para evitar consumo / fallos
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        // ✅ Lux leído del HARDWARE (event.values[0])
        float lux = event.values[0];

        // ✅ Pasar lectura real al core
        SensorLuz.setLux(lux);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No requerido, pero se deja por contrato de SensorEventListener
    }
}
