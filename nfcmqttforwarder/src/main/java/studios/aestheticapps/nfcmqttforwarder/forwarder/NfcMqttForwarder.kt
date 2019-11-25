package studios.aestheticapps.nfcmqttforwarder.forwarder

import android.app.Application
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import studios.aestheticapps.nfcmqttforwarder.R
import studios.aestheticapps.nfcmqttforwarder.ssl.SocketFactory
import studios.aestheticapps.nfcmqttforwarder.ssl.TLSCertificateType
import studios.aestheticapps.nfcmqttforwarder.util.JsonSerializer
import studios.aestheticapps.nfcmqttforwarder.util.StringConverter
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Simple NFC tag message MQTT forwarder based on Paho Android Service.
 * Processes NFC intent and sends its content directly to MQTT Broker.
 * 
 * @property brokerUri Valid MQTT broker uri, ex. ssl://mqtt.eclipse.org:1883.
 * @property defaultTopic Specify default topic that forwarder can push its messages to. You can override this by calling forwarder.processNfcIntent(topic = "your-new-topic").
 * @property isTlsEnabled Set as true if your connection has to be tls-secured (ca.crt file required) set as false by default. If true, specify your caInputStream.
 * @property caInputStream Specify input stream to your ssl client cert file if isTlsEnabled is set to true.
 * @property messageType See {@MessageType}. MessageType.PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON set as default.
 * @property trimUnwantedBytesFromPayload Sometimes payloads come with text of kind "\u0002enSomeText", the option gets rid of them in result message string. Set default as true.
 */
class NfcMqttForwarder(private val application: Application,
                       private val brokerUri: String,
                       private val clientId: String = MqttClient.generateClientId(),
                       private val clientUsername: String = "",
                       private val clientPassword: String = "",
                       private val defaultTopic: String,
                       private val isTlsEnabled: Boolean = false,
                       private val tlsCertificateType: TLSCertificateType = TLSCertificateType.CA_SIGNED_SERVER_CERTIFICATE,
                       private val caInputStream: InputStream? = null,
                       private val messageType: MessageType = MessageType.PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON,
                       private val timeout: Int = 2,
                       private val trimUnwantedBytesFromPayload: Boolean = true) {

    init {
        check(!(isTlsEnabled
                && tlsCertificateType == TLSCertificateType.SELF_SIGNED_CERTIFICATE
                && caInputStream == null)) {
            application.getString(R.string.error_provide_inputstream)
        }
    }

    /**
     * Specify listener if you want to receive callback on your forwarded messages.
     */
    var onResultListener: OnNfcMqttForwardingResultListener? = null

    private val client by lazy { MqttAndroidClient(application, brokerUri, clientId) }

    // Needed to block any multiple connection callbacks.
    private var wantsToForwardMsg = false

    /**
     * Pushes raw String message containing all NDEF records as Json list (for NDEF_ACTION intents type) or
     * NFC low-level tag UID/RID as single-entry Json list with tagUid (when Intent action is of type TECH_ACTION or TAG_ACTION)
     * directly to topic on MQTT Server provided by user (specified by brokerUri).
     * Automatically connects user to broker.
     *
     * @param intent Intent forwarded to the library from any Activity supporting NFC Intents.
     * @param topic Specify topic that should be used to forward message. If not specified, defaultTopic is used.
     * @param additionalMessages Will be added to message JSON if messageType is MessageType.PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON. Map empty by default.
     */
    fun processNfcIntent(intent: Intent, topic: String = defaultTopic,
                         additionalMessages: Map<String, String> = mutableMapOf()) {

        Log.i(TAG, "Processing intent of type " + intent.action + ".")

        // needed to block any multiple connection callbacks
        wantsToForwardMsg = true

        when {

            isActionAnNdefNfcIntent(intent.action!!) -> {
                if (messageType == MessageType.ONLY_UID_RID_ONE_ENTRY_ARRAY) processTagFrom(intent, topic)
                else processNdefMessageFrom(intent, topic, additionalMessages.toMutableMap())
            }

            isActionAnNfcIntent(intent.action!!) -> processTagFrom(intent, topic)
            else -> Log.d(
                TAG, application.getString(
                    R.string.non_nfc_intent_detected
                ))
        }
    }

    /**
     * Disconnect manually only when you are troubleshooting your forwarder or subscriber.
     * All connections are managed automatically by the Forwarder, but please notice that:
     * Forwarder assumes that message forwarded to your broker gets immediate reply.
     * If subscribeForAResponse is false, then connection will be closed automatically after successful forwarded message.
     * However, if subscribeForAResponse is true, then automatic disconnection
     * will be done only if MQTT broker will send any message on responseTopic or subscribtionTimeout will expire.
     */
    fun disconnectFromBroker() {
        client.takeIf { it.isConnected }?.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully disconnected from $brokerUri.")

                // notify observers
                onResultListener?.onDisconnectSuccessful()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(
                    TAG, application.resources.getString(
                        R.string.broker_disconnection_failure
                    ))

                // notify observers
                onResultListener?.onDisconnectError(application.resources.getString(R.string.broker_disconnection_failure))
            }
        })
    }

    /**
     * Parses all NDEF Messages from the intent and sends it to broker.
     */
    private fun processNdefMessageFrom(intent: Intent, topic: String,
                                       additionalMessages: MutableMap<String, String> = mutableMapOf()) {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMsgs ->
            rawMsgs.forEach {
                (it as NdefMessage).apply {
                    val message = createMessage(records, additionalMessages)
                    connectToServerAndTryToPublishMessage(message, topic)
                }
            }
        }
    }

    /**
     * Parses non-NDEF tag message as tag stable unique identifier (UID) from the intent and sends it to broker
     * as no parsable NDEF messages were detected.
     *
     * The tag identifier is a low level serial number, used for anti-collision and identification.
     * Most tags have UID, but some tags will generate a random ID every time they are discovered (RID)
     * and there are some tags with no ID at all (the byte array will be zero-sized),
     * so do not rely on such a method of identifying tags unless you're sure your tags have stable UID.
     */
    private fun processTagFrom(intent: Intent, topic: String,
                               additionalMessages: MutableMap<String, String> = mutableMapOf()) {
        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.also {
            val message = createMessage(arrayOf(it.id.toString()), additionalMessages)
            connectToServerAndTryToPublishMessage(message, topic)
        }
    }

    /**
     * Creates message for MQTT broker of type defined in MessageType.
     */
    private fun createMessage(records: Array<NdefRecord>,
                              additionalMessages: MutableMap<String, String> = mutableMapOf()) : String {
        val serializer = JsonSerializer()
        var attributeIndex = 0

        val message = when (messageType) {

            MessageType.NDEF_MESSAGE_ARRAY -> {
                serializer.arrayToJson(records.asList())
            }

            MessageType.ONLY_PAYLOAD_ARRAY -> {
                serializer.arrayToJson(records.map { convertBytesToString(it.payload) })
            }

            MessageType.PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON -> {
                val map = mutableMapOf<String, String>()
                records.forEach {
                    map["attr$attributeIndex"] = convertBytesToString(it.payload)
                    attributeIndex++
                }
                map.putAll(additionalMessages)
                serializer.mapToJson(map)
            }

            else -> ""
        }

        Log.i(TAG, "Created message of type " + messageType.name + " = \"$message\"")

        return message
    }

    /**
     * Creates message for MQTT broker of type MessageType.ONLY_UID_RID_ONE_ENTRY_ARRAY, used when non-NDEF tag was detected."
     */
    private fun createMessage(records: Array<String>, additionalMessages: MutableMap<String, String> = mutableMapOf()) : String {
        val serializer = JsonSerializer()
        var attributeIndex = 0

        val map = mutableMapOf<String, String>()
        records.forEach {
            map["attr$attributeIndex"] = StringConverter.bytesToHexString(it.toByteArray())
            attributeIndex++
        }

        if (messageType == MessageType.PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON) {
            map.putAll(additionalMessages)
        }

        val message = serializer.mapToJson(map)

        Log.d(TAG, "Created message of type " + MessageType.ONLY_UID_RID_ONE_ENTRY_ARRAY.name + " = \"$message\"")

        return message
    }

    private fun convertBytesToString(byteArray: ByteArray): String {
        return if (trimUnwantedBytesFromPayload) {
            String(byteArray.copyOfRange(3, byteArray.size), Charset.forName("UTF-8"))
        } else {
            String(byteArray, Charset.forName("UTF-8"))
        }
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

    private fun connectToServerAndTryToPublishMessage(payload: String, topic: String) {
        // check if client is already connected
        client.takeIf { it.isConnected }?.let {
            // try to publish message
            publishMessage(payload, topic)
            return
        }

        // connect to MQTT Server
        client.connect(obtainOptions(), null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully connected to $brokerUri.")

                // notify observers
                onResultListener?.onConnectSuccessful()

                // try to publish message
                publishMessage(payload, topic)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(
                    TAG, application.getString(
                        R.string.broker_connection_failure
                    ))

                wantsToForwardMsg = false

                // notify observers
                onResultListener?.onConnectError(application.getString(R.string.broker_connection_failure))
                onResultListener?.onForwardingError(application.getString(R.string.broker_connection_failure))
            }
        })
    }

    private fun obtainOptions() : MqttConnectOptions {
        val options = MqttConnectOptions()
        options.connectionTimeout = timeout

        if (clientUsername.isNotEmpty()) options.userName = clientUsername
        if (clientPassword.isNotEmpty()) options.password = clientPassword.toCharArray()

        if (isTlsEnabled) {
            when (tlsCertificateType) {
                TLSCertificateType.SELF_SIGNED_CERTIFICATE -> {
                    if (caInputStream != null) {
                        caInputStream.reset()
                        val socketFactoryOptions = SocketFactory.SocketFactoryOptions()
                        val sfo = socketFactoryOptions.withCaInputStream(caInputStream)
                        options.socketFactory = SocketFactory(sfo)
                    } else {
                        onResultListener?.onConnectError(application.getString(R.string.broker_null_ssl_cert_failure))
                        onResultListener?.onForwardingError(application.getString(R.string.broker_null_ssl_cert_failure))
                    }
                }

                TLSCertificateType.CA_SIGNED_SERVER_CERTIFICATE -> {
                    val socketFactoryOptions = SocketFactory.SocketFactoryOptions()
                    val sfo = socketFactoryOptions.withClientP12Password(clientPassword)
                    options.socketFactory = SocketFactory(sfo)
                }
            }
        }

        return options
    }

    private fun publishMessage(payload: String, topic: String) {
        // if msg is already published, don't try to do this again
        if (!wantsToForwardMsg) return
        else wantsToForwardMsg = false

        val message = MqttMessage(payload.toByteArray())
        client.publish(topic, message, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.w(TAG, "Successfully published to $brokerUri.")

                // notify observers
                onResultListener?.onPublishSuccessful()

                // successfully connected and published message
                onResultListener?.onForwardingSuccessful()

                // disconnect if needed
                // if user wishes to subscribe for a response, than disconnection will be handled by Subscriber
                disconnectFromBroker()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(
                    TAG, application.resources.getString(
                        R.string.broker_publish_failure
                    ))

                wantsToForwardMsg = false

                // notify observers
                onResultListener?.onPublishError(application.resources.getString(R.string.broker_publish_failure))
                onResultListener?.onForwardingError(application.resources.getString(R.string.broker_publish_failure))
            }
        })
    }

    companion object {
        private val TAG = NfcMqttForwarder::class.java.simpleName

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