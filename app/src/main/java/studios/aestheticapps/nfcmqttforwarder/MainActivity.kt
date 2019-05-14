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

class MainActivity : AppCompatActivity(),
    OnNfcMqttForwardingResultListener {

    // should be a result of login or sth
    private val clientId = "courier001"

    private val forwarder: NfcMqttForwarder by lazy {
        NfcMqttForwarder(
            application,
            serverUri = getString(R.string.serverUri),
            clientId = clientId,
            defaultTopic = getString(R.string.defaultTopic, clientId),
            subscribeForAResponse = false,
            responseTopic = getString(R.string.defaultSubscriptionTopic, clientId),
            messageType = MessageType.ONLY_PAYLOAD_ARRAY,
            connectionTimeout = 2,
            automaticDisconnectAfterForwarding = false,
            onResultListener = this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        processNfcOperations(intent)
    }

    override fun onStop() {
        super.onStop()
        forwarder.disconnectFromServer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processNfcOperations(intent)
    }

    private fun processNfcOperations(intent: Intent) {
        Log.d("TAG", "New NFC intent")
        if (NfcMqttForwarder.isIntentsNfcActionSupported(intent)) {
            forwarder.processNfcIntent(intent)
        }
    }

    override fun onForwardingError() {
        Log.d(TAG, "Reacting for error in forwarding!")
    }

    override fun onForwardingSuccessful() {
        Log.d(TAG, "Reacting for successfully forwarded msg!")
    }

    override fun onSubscriptionMessageArrived(topic: String?, message: MqttMessage?) {
        Log.d(TAG, "Message arrived on topic = $topic with msg = \"$message\".")
        Toast.makeText(this,
            "Message arrived on topic = $topic with msg = \"$message\".", Toast.LENGTH_SHORT).show()

        forwarder.disconnectFromServer()
    }

    companion object {
        val TAG = "MainActivity"
    }
}

