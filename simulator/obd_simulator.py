import time
import random
import json
import paho.mqtt.client as mqtt

MQTT_HOST = "localhost"     # or your PC's LAN IP, or broker.hivemq.com
MQTT_PORT = 1883

TOPIC_SPEED = "car/speed"
TOPIC_RPM   = "car/rpm"
TOPIC_FUEL  = "car/fuel"

client = mqtt.Client(client_id="obd_simulator")
client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
client.loop_start()

speed = 0.0
fuel = 100.0

print(f"Simulator publishing to {MQTT_HOST}:{MQTT_PORT} ... Ctrl+C to stop")

try:
    while True:
        # Simulate speed drifting up/down like real driving
        speed += random.uniform(-5, 8)
        speed = max(0, min(speed, 160))  # clamp 0-160 km/h

        rpm = int(800 + speed * 35 + random.uniform(-100, 100))
        fuel = max(0, fuel - 0.01)  # slowly drains

        client.publish(TOPIC_SPEED, json.dumps({"value": round(speed, 1)}))
        client.publish(TOPIC_RPM, json.dumps({"value": rpm}))
        client.publish(TOPIC_FUEL, json.dumps({"value": round(fuel, 1)}))

        print(f"speed={speed:.1f} km/h  rpm={rpm}  fuel={fuel:.1f}%")
        time.sleep(1)
except KeyboardInterrupt:
    print("Stopped.")
    client.loop_stop()
    client.disconnect()