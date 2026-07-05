# Smart Car Dashboard

An Android app that connects to live vehicle data over MQTT and displays speed, RPM, and fuel level on a car-friendly dashboard. Speaks a voice warning and shows a red alert banner when the driver exceeds a safe speed. Also displays live weather conditions.

## Project Structure

```
android-app/       Android Studio project (Kotlin)
simulator/
  obd_simulator.py  Python script simulating OBD sensor data
README.md
```

## Requirements

- Android Studio
- Python 3
- Mosquitto MQTT broker
- An OpenWeatherMap API key (free tier) for the weather card

## Setup

### 1. Install and start the MQTT broker

Download Mosquitto: https://mosquitto.org/download/

```bash
mosquitto -v
```

This starts a broker on `localhost:1883`.

> Note: on Windows, Mosquitto may already be running as a background service. If `mosquitto -v` fails with a port-in-use error, the broker is already running — you can skip this step.

### 2. Install Python dependencies

```bash
pip install paho-mqtt
```

### 3. Run the OBD simulator

```bash
cd simulator
python obd_simulator.py
```

You should see live speed/RPM/fuel values printing every second. Leave this running.

### 4. Configure and run the Android app

1. Open the `android-app` folder in Android Studio.
2. In `MainActivity.kt`, set the broker URL:
   - **Emulator:** `tcp://10.0.2.2:1883`
   - **Real device:** `tcp://<your-PC's-LAN-IP>:1883` (find it with `ipconfig` / `ifconfig`)
3. Add your OpenWeatherMap API key and city in `MainActivity.kt`:
   ```kotlin
   private val weatherApiKey = "YOUR_API_KEY_HERE"
   private val city = "YourCity"
   ```
4. Build and run the app on an emulator or connected device.

### 5. Verify it's working

With the simulator and broker running, the app should show "Connected" and start displaying live speed, RPM, and fuel values.

To manually trigger the speed alert:

```bash
mosquitto_pub -h localhost -t "car/speed" -m "{\"value\": 130}"
```

The speedometer should turn red, and the app should speak a warning and show the alert banner.

## Troubleshooting

| Issue | Fix |
|---|---|
| `mosquitto` not recognized | Use the full path, e.g. `"C:\Program Files\mosquitto\mosquitto.exe"`, or add it to PATH |
| `pip`/`python` not recognized | Reinstall Python and check "Add python.exe to PATH" during setup |
| App can't connect on real device | Mosquitto defaults to "local only mode" — add a `mosquitto.conf` with `listener 1883 0.0.0.0` and `allow_anonymous true`, then restart the broker with `-c mosquitto.conf` |
| Weather shows "unavailable" | New OpenWeatherMap API keys can take up to an hour to activate |

## Tech Stack

- Kotlin, Android Studio
- Eclipse Paho MQTT client
- Mosquitto (MQTT broker)
- Python 3 + paho-mqtt (simulator)
- Android TextToSpeech API
- OpenWeatherMap REST API
