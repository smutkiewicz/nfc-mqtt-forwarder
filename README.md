# NfcMqttForwarder

Simple NFC tag message MQTT forwarder based on Paho Android Service. Processes NFC intent and sends its content directly to MQTT Server.

## Setup

1. First things first, simply configure NFC in your app's manifest - follow [Google's documentation](https://developer.android.com/guide/topics/connectivity/nfc/nfc#manifest).

2. Finally, add the library to your Gradle app script.

- Project level Gradle:
```
   repositories {
        jcenter()
        maven { url "https://jitpack.io" }
   }
```  
- App level Gradle:
```   
   dependencies {
         implementation "com.github.smutkiewicz:nfc-mqtt-forwarder:v0.92"
   }
```   
3. Happy coding! :)
