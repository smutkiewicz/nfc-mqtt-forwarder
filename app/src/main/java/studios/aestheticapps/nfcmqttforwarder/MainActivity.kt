package studios.aestheticapps.nfcmqttforwarder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), OnForwardResultListener {

    private val forwarder: NfcMqttForwarder by lazy { NfcMqttForwarder(
        application,
        resources.getString(R.string.serverUri),
        resources.getString(R.string.defaultTopic),
        clientId = "courier001",
        messageType = NfcMqttForwarder.MessageType.ONLY_PAYLOAD_ARRAY,
        onResultListener = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        processNfcOperations(intent)
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

    companion object {
        val TAG = "MainActivity"
    }
}

