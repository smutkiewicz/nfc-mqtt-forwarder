package studios.aestheticapps.nfcmqttforwarder.forwarder

/**
 * Formats content sent in messages to MQTT server.
 */
enum class MessageType {

    /**
     * ONLY_UID_RID_ONE_ENTRY_ARRAY sets Forwarder to send raw unconverted low-level UID/RID of an nfc tag,
     * used automatically when no NDEF content is detected.
     */
    ONLY_UID_RID_ONE_ENTRY_ARRAY,

    /**
     * ONLY_PAYLOAD_ARRAY sets Forwarder to send only String UTF-converted payload content read from NdefRecord.
     */
    ONLY_PAYLOAD_ARRAY,

    /**
     * PAYLOAD_AND_ADDITIONAL_MESSAGE_ARRAY sets Forwarder to send String UTF-converted payload content
     * read from NdefRecord and additional messages from outside nfc tag.
     */
    PAYLOAD_AND_ADDITIONAL_MESSAGE_ARRAY,

    /**
     * NDEF_MESSAGE_ARRAY sets Forwarder to send also raw unconverted info about tnf, type and payload.
     */
    NDEF_MESSAGE_ARRAY
}