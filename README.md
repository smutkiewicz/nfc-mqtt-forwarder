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
         implementation "com.github.smutkiewicz:nfc-mqtt-forwarder:v0.93"
   }
```   

3. Configure your Activity.

- Example in practice:
```
// You can implement Listener in your Activity
class MainActivity : AppCompatActivity(), OnNfcMqttForwardingResultListener {

    // Initialize Forwarder class
    private val forwarder: NfcMqttForwarder by lazy {
        NfcMqttForwarder(
            application,
            brokerUri = getString(R.string.brokerUri),
            defaultTopic = getString(R.string.defaultTopic),
            isTlsEnabled = true,
            tlsVersion = TLSVersion.TLSv1_2,
            tlsCertificateType = TLSCertificateType.SELF_SIGNED_CERTIFICATE,
            caInputStream = application.assets.open("ca.crt"),
            messageType = MessageType.PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON,
            trimUnwantedBytesFromPayload = true
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Specify forwarding result listener
        forwarder.onResultListener = this

        // Process in case this intent is NFC intent.
        processNfcOperations(intent)
    }

    override fun onStop() {
        super.onStop()

        // Optional, Forwarder automatically disconnects after processing.
        // See function doc for more.
        forwarder.disconnectFromBroker()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Process in case this intent is NFC intent.
        processNfcOperations(intent)
    }

    private fun processNfcOperations(intent: Intent) {
        Log.d("TAG", "New NFC intent")

        // Check if library supports intent action
        if (NfcMqttForwarder.isIntentsNfcActionSupported(intent)) {
            // Process intent
            forwarder.processNfcIntent(
                intent = intent,
                additionalMessages = mapOf(
                    "attrX" to "nice msg",
                    "attrY" to "some msg"
                )
            )
        }
    }

    override fun onForwardingError(message: String) {
        Log.d(TAG, "Reacting for error in forwarding!")
    }

    override fun onForwardingSuccessful() {
        Log.d(TAG, "Reacting for successfully forwarded msg!")
    }

    companion object {
        val TAG = "MainActivity"
    }
}
```
3. Happy coding! :)
