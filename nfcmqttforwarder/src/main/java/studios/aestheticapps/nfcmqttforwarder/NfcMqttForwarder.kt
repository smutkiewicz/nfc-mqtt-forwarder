package studios.aestheticapps.nfcmqttforwarder

import android.app.Application
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * Simple NFC tag message MQTT forwarder based on Paho Android Service.
 * Processes NFC intent and sends its content directly to MQTT Server.
 */
class NfcMqttForwarder(private val application: Application,
                       private val serverUri: String,
                       private val topic: String,
                       private val clientId: String = MqttClient.generateClientId()) {

    private val client by lazy { MqttAndroidClient(application, serverUri, clientId) }

    /**
     * Pushes raw message in format "mimeType;record1;record2;...;" (for NDEF_ACTION intents) or
     * NfcAdapter.EXTRA_ID as NFC low-level tag ID (when Intent action is of type TECH_ACTION or TAG_ACTION)
     * directly to MQTT Server provided by user (specified by serverUri).
     * @param intent Intent forwarded to the library from onNewIntent() method in Activity.
     */
    fun processNfcIntent(intent: Intent) {
        when {
            isActionAnNdefNfcIntent(intent.action!!) -> processNdefMessageFrom(intent)
            isActionAnNfcIntent(intent.action!!) -> processTagFrom(intent)
            else -> Log.d(TAG, application.getString(R.string.non_nfc_intent_detected))
        }
    }

    /**
     * Parses the NDEF Message from the intent and sends it to server.
     * Processes one message sent during the beam.
     */
    private fun processNdefMessageFrom(intent: Intent) {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMsgs ->
            rawMsgs.forEach {
                (it as NdefMessage).apply {
                    // record 0 contains the MIME type, record 1 is the AAR, if present
                    val message = createMessage(records)
                    connectToServerAndTryToPublishMessage(message)
                }
            }
        }
    }

    /**
     * Parses non-NDEF tag message as tag id from the intent and sends it to server.
     */
    private fun processTagFrom(intent: Intent) {
        intent.getStringExtra(NfcAdapter.EXTRA_ID)?.also {
            connectToServerAndTryToPublishMessage(it)
        }
    }

    /**
     * Creates message for MQTT server. Format standard is "mimeType;record1;record2;...;"
     */
    private fun createMessage(records: Array<out NdefRecord>) = records.joinToString(MSG_SEPARATOR)

    /**
     * Specifies if intent is compatible with standard NDEF Message - a container for one or more NDEF Records.
     */
    private fun isActionAnNdefNfcIntent(action: String) : Boolean = NfcAdapter.ACTION_NDEF_DISCOVERED == action

    /**
     * Specifies if intent is compatible with any NFC technology.
     */
    private fun isActionAnNfcIntent(action: String) : Boolean =
        NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action

    private fun connectToServerAndTryToPublishMessage(payload: String) {
        // connect to MQTT Server
        client.connect().actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.d(TAG, "Successfully connected to $serverUri.")

                // try to publish message
                publishMessage(payload)

                // disconnect
                disconnectFromServer()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.d(TAG, application.getString(R.string.server_connection_failure))
            }
        }
    }

    private fun publishMessage(payload: String) {
        val message = MqttMessage(payload.toByteArray())
        client.publish(topic, message).actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.d(TAG, "Successfully published to $serverUri.")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.d(TAG, application.resources.getString(R.string.server_publish_failure))
            }
        }
    }

    private fun disconnectFromServer() {
        client.disconnect().actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.d(TAG, "Successfully disconnected from $serverUri.")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.d(TAG, application.resources.getString(R.string.server_disconnection_failure))
            }
        }
    }

    private companion object {
        val TAG = NfcMqttForwarder::class.java.name
        const val MSG_SEPARATOR = ";"
    }
}