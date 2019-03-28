package studios.aestheticapps.nfcmqttforwarder

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val forwarder: NfcMqttForwarder by lazy { NfcMqttForwarder(application, "exampleServerUri", "exampleTopic") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            forwarder.processNfcIntent(intent)
        }
    }
}

