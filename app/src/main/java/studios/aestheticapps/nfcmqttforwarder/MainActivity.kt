package studios.aestheticapps.nfcmqttforwarder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

private const val serverUri = "tcp://10.10.80.52:1883"

class MainActivity : AppCompatActivity() {

    private val forwarder: NfcMqttForwarder by lazy { NfcMqttForwarder(
        application, serverUri, "couriers/test",
        messageType = NfcMqttForwarder.MessageType.ONLY_PAYLOAD)
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
}

