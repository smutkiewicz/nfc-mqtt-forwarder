package studios.aestheticapps.nfcmqttforwarder

import android.app.Application
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.nio.charset.StandardCharsets

/**
 * Simple NFC tag message MQTT forwarder based on Paho Android Service.
 * Processes NFC intent and sends its content directly to MQTT Server.
 */
class NfcMqttForwarder(private val application: Application,
                       private val serverUri: String,
                       private val topic: String,
                       private val clientId: String = MqttClient.generateClientId(),
                       private val messageType: MessageType = MessageType.ONLY_PAYLOAD) {

    private val client by lazy { MqttAndroidClient(application, serverUri, clientId) }

    /**
     * Pushes raw String message containing all NDEF records as Json list (for NDEF_ACTION intents type) or
     * NFC low-level tag UID/RID as single-entry Json list with tagUid (when Intent action is of type TECH_ACTION or TAG_ACTION)
     * directly to topic on MQTT Server provided by user (specified by serverUri).
     *
     * @param intent Intent forwarded to the library from any Activity supporting NFC Intents.
     */
    fun processNfcIntent(intent: Intent) {
        Log.d(TAG, "Processing intent of type " + intent.action + ".")
        when {
            isActionAnNdefNfcIntent(intent.action!!) -> processNdefMessageFrom(intent)
            isActionAnNfcIntent(intent.action!!) -> processTagFrom(intent)
            else -> Log.d(TAG, application.getString(R.string.non_nfc_intent_detected))
        }
    }

    /**
     * Parses all NDEF Messages from the intent and sends it to server.
     */
    private fun processNdefMessageFrom(intent: Intent) {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMsgs ->
            rawMsgs.forEach {
                (it as NdefMessage).apply {
                    val message = createMessage(records)
                    connectToServerAndTryToPublishMessage(message)
                }
            }
        }
    }

    /**
     * Parses non-NDEF tag message as tag stable unique identifier (UID) from the intent and sends it to server
     * as no parsable NDEF messages were detected.
     *
     * The tag identifier is a low level serial number, used for anti-collision and identification.
     * Most tags have UID, but some tags will generate a random ID every time they are discovered (RID)
     * and there are some tags with no ID at all (the byte array will be zero-sized),
     * so do not rely on such a method of identifying tags unless you're sure your tags have stable UID.
     */
    private fun processTagFrom(intent: Intent) {
        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.also {
            val message = createMessage(arrayOf(it.id.toString()))
            connectToServerAndTryToPublishMessage(message)
        }
    }

    /**
     * Creates message for MQTT server of type defined in MessageType."
     */
    private fun createMessage(records: Array<NdefRecord>) : String {
        val serializer = JsonSerializer()
        val message = when (messageType) {
            MessageType.FULL_NDEF_MESSAGE -> serializer.arrayToJson(records.asList())
            MessageType.ONLY_PAYLOAD -> serializer.arrayToJson(records.map { convertBytesToString(it.payload) })
            MessageType.ONLY_UID_RID -> serializer.arrayToJson(records.map { it.id })
        }

        Log.d(TAG, "Created message of type " + messageType.name + " = \"$message\"")
        return message
    }

    /**
     * Creates message for MQTT server of type MessageType.ONLY_UID_RID, used when non-NDEF tag was detected."
     */
    private fun createMessage(records: Array<String>) : String {
        val serializer = JsonSerializer()
        val message = serializer.arrayToJson(records.map { convertBytesToString(it.toByteArray()) })

        Log.d(TAG, "Created message of type " + MessageType.ONLY_UID_RID.name + " = \"$message\"")
        return message
    }

    private fun convertBytesToString(byteArray: ByteArray) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        String(byteArray, StandardCharsets.UTF_8)
    } else {
        String(byteArray)
    }

    /**
     * Specifies if intent is compatible with standard NDEF Message - a container for one or more NDEF Records
     * that can store data of well known mimetypes.
     */
    private fun isActionAnNdefNfcIntent(action: String) : Boolean = NfcAdapter.ACTION_NDEF_DISCOVERED == action

    /**
     * Specifies if intent is compatible with any NFC technology.
     */
    private fun isActionAnNfcIntent(action: String) : Boolean =
        NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action

    private fun connectToServerAndTryToPublishMessage(payload: String) {
        val options = MqttConnectOptions()
        options.userName = "username"
        options.password = "password".toCharArray()

        // connect to MQTT Server
        client.connect(options).actionCallback = object : IMqttActionListener {
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

    /**
     * Formats content sent in messages to MQTT server.
     */
    enum class MessageType {

        /**
         * ONLY_UID_RID sets Forwarder to send raw unconverted low-level UID/RID of an nfc tag,
         * used automatically when no NDEF content is detected.
         */
        ONLY_UID_RID,

        /**
         * ONLY_PAYLOAD sets Forwarder to send only String UTF-converted payload content read from NdefRecord.
         */
        ONLY_PAYLOAD,

        /**
         * FULL_NDEF_MESSAGE sets Forwarder to send also raw unconverted info about tnf, type and payload.
         */
        FULL_NDEF_MESSAGE
    }

    companion object {
        private val TAG = NfcMqttForwarder::class.java.simpleName
        private const val MSG_SEPARATOR = ";"

        /**
         * Filters on intents supported by the library.
         * Can be used before passing intent to be processed by Forwarder.
         */
        fun isIntentsNfcActionSupported(intent: Intent) : Boolean =
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
                    NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
                    NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
    }
}