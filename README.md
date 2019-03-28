# NfcMqttForwarder

Simple NFC tag message MQTT forwarder based on Paho Android Service. Processes NFC intent and sends its content directly to MQTT Server.

## Setup

Where to get the library:

```
   repositories {
        jcenter()
        maven { url "https://jitpack.io" }
   }

   dependencies {
         implementation "com.github.smutkiewicz:nfc-mqtt-forwarder:v0.1"
   }
```   
