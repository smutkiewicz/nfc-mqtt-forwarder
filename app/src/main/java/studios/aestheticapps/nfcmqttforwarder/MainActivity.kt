package studios.aestheticapps.nfcmqttforwarder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity : AppCompatActivity(), OnNfcMqttForwardingResultListener, MqttSubscriber.OnSubscriptionListener {

    // should be a result of login or sth
    private val clientId = "courier001"

    private val forwarder: NfcMqttForwarder by lazy { NfcMqttForwarder(
        application,
        serverUri = resources.getString(R.string.serverUri),
        defaultTopic = resources.getString(R.string.defaultTopic, clientId),
        clientId = clientId,
        messageType = NfcMqttForwarder.MessageType.ONLY_PAYLOAD_ARRAY,
        connectionTimeout = 2,
        automaticDisconnectAfterForwarding = false,
        encryptionEnabled = true,
        encryptionKey = resources.getString(R.string.secretKey),
        onResultListener = this)
    }

    private val subscriber: MqttSubscriber by lazy { MqttSubscriber(application,
        serverUri = resources.getString(R.string.serverUri, clientId),
        defaultSubscriptionTopic = resources.getString(R.string.defaultSubscriptionTopic, clientId),
        clientId = clientId,
        onSubscriptionListener = this)
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
            subscriber.subscribeToTopicAndReceiveResponse()
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
        Toast.makeText(this,
            "Message arrived on topic = $topic with msg = \"$message\".", Toast.LENGTH_SHORT).show()
        subscriber.unsubscribeFromTopic(topic!!)
        forwarder.disconnectFromServer()
    }

    companion object {
        val TAG = "MainActivity"
    }
}

