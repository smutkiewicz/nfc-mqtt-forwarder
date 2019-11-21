package studios.aestheticapps.nfcmqttforwarder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.MqttMessage
import studios.aestheticapps.nfcmqttforwarder.forwarder.MessageType
import studios.aestheticapps.nfcmqttforwarder.forwarder.NfcMqttForwarder
import studios.aestheticapps.nfcmqttforwarder.forwarder.OnNfcMqttForwardingResultListener

// You can implement Listener in your Activity
class MainActivity : AppCompatActivity(), OnNfcMqttForwardingResultListener {

    // Initialize Forwarder class
    private val forwarder: NfcMqttForwarder by lazy {
        NfcMqttForwarder(
            application,
            brokerUri = getString(R.string.brokerUri),
            defaultTopic = getString(R.string.defaultTopic),
            subscribeForAResponse = true,
            responseTopic = getString(R.string.defaultSubscriptionTopic),
            subscriptionTimeout = 5,
            isTlsEnabled = true,
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

    override fun onSubscriptionMessageArrived(topic: String?, message: MqttMessage?) {
        Log.d(TAG, "Message arrived on topic = $topic with msg = \"$message\".")
        Toast.makeText(this,
            "Message arrived on topic = $topic with msg = \"$message\".", Toast.LENGTH_SHORT).show()
    }

    companion object {
        val TAG = "MainActivity"
    }
}

