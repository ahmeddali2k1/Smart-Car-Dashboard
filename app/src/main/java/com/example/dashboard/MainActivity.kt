package com.example.dashboard

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // --- CHANGE THIS to your PC's IP, or "10.0.2.2" for the emulator ---
    private val brokerUrl = "tcp://192.168.178.143:1883"
    private lateinit var tvAlertBanner: TextView
    private lateinit var mqttClient: MqttAsyncClient
    private lateinit var tts: TextToSpeech

    private lateinit var speedometer: SpeedometerView
    private lateinit var tvRpm: TextView
    private lateinit var tvFuel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvWeatherIcon: TextView
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvWeatherDesc: TextView
    private lateinit var tvWeatherCity: TextView

    private val weatherApiKey = "cc82c63a6ea86555aea88ba6efaf11d2"
    private val city = "Deggendorf"  // change to your demo location, or make it dynamic later

    private var lastWarningTime = 0L
    private val SPEED_LIMIT = 100.0  // km/h, adjust for your demo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvWeatherIcon = findViewById(R.id.tvWeatherIcon)
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp)
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc)
        tvWeatherCity = findViewById(R.id.tvWeatherCity)
        tvAlertBanner = findViewById(R.id.tvAlertBanner)
        fetchWeather()

        speedometer = findViewById(R.id.speedometer)
        tvRpm = findViewById(R.id.tvRpm)
        tvFuel = findViewById(R.id.tvFuel)
        tvStatus = findViewById(R.id.tvStatus)

        tts = TextToSpeech(this, this)
        connectMqtt()
    }
    private fun fetchWeather() {
        Thread {
            try {
                val url = java.net.URL(
                    "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$weatherApiKey"
                )
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 8000
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val temp = json.getJSONObject("main").getDouble("temp")
                val weatherArray = json.getJSONArray("weather")
                val condition = weatherArray.getJSONObject(0).getString("main")
                val cityName = json.getString("name")

                val icon = when (condition.lowercase()) {
                    "clear" -> "☀️"
                    "clouds" -> "☁️"
                    "rain", "drizzle" -> "🌧️"
                    "thunderstorm" -> "⛈️"
                    "snow" -> "❄️"
                    "mist", "fog", "haze" -> "🌫️"
                    else -> "⛅"
                }

                runOnUiThread {
                    tvWeatherIcon.text = icon
                    tvWeatherTemp.text = "${temp.toInt()}°C"
                    tvWeatherDesc.text = condition
                    tvWeatherCity.text = cityName
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvWeatherDesc.text = "Weather unavailable"
                }
            }
        }.start()
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun connectMqtt() {
        val clientId = "android_dashboard_${System.currentTimeMillis()}"
        mqttClient = MqttAsyncClient(brokerUrl, clientId, null)

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                runOnUiThread { tvStatus.text = "Connection lost, retrying..." }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: return
                val value = try {
                    JSONObject(payload).getDouble("value")
                } catch (e: Exception) {
                    return
                }
                runOnUiThread { handleMessage(topic, value) }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                runOnUiThread { tvStatus.text = "Connected" }
                subscribeToTopics()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                runOnUiThread { tvStatus.text = "Connect failed: ${exception?.message}" }
            }
        })
    }

    private fun subscribeToTopics() {
        mqttClient.subscribe("car/speed", 0)
        mqttClient.subscribe("car/rpm", 0)
        mqttClient.subscribe("car/fuel", 0)
    }

    private fun handleMessage(topic: String?, value: Double) {
        when (topic) {
            "car/speed" -> {
                speedometer.setSpeed(value.toFloat())
                checkSpeedWarning(value)
            }
            "car/rpm" -> tvRpm.text = value.toInt().toString()
            "car/fuel" -> tvFuel.text = "${value.toInt()}%"
        }
    }

    private val bannerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideBannerRunnable = Runnable {
        tvAlertBanner.animate().alpha(0f).setDuration(300).withEndAction {
            tvAlertBanner.visibility = android.view.View.GONE
        }.start()
    }

    private fun checkSpeedWarning(speed: Double) {
        val now = System.currentTimeMillis()
        if (speed > SPEED_LIMIT && now - lastWarningTime > 8000) {
            tts.speak("You are driving too fast", TextToSpeech.QUEUE_FLUSH, null, null)
            showAlertBanner()
            lastWarningTime = now
        }
    }

    private fun showAlertBanner() {
        bannerHandler.removeCallbacks(hideBannerRunnable)
        tvAlertBanner.alpha = 1f
        tvAlertBanner.visibility = android.view.View.VISIBLE
        bannerHandler.postDelayed(hideBannerRunnable, 3000) // visible for 3 seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        try { mqttClient.disconnect() } catch (e: Exception) {}
        bannerHandler.removeCallbacks(hideBannerRunnable)
    }
}