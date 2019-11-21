package studios.aestheticapps.nfcmqttforwarder.forwarder

/**
 * Formats content sent in messages to MQTT server.
 */
enum class MessageType {

    /**
     * ONLY_UID_RID_ONE_ENTRY_ARRAY sets Forwarder to send raw unconverted low-level UID/RID of an nfc tag,
     * used automatically when non-NDEF content is detected. It should be rare situation.
     * Example: {"attr0":"0x5b424062336263633133"}
     */
    ONLY_UID_RID_ONE_ENTRY_ARRAY,

    /**
     * ONLY_PAYLOAD_ARRAY sets Forwarder to send only String UTF-converted payload content read from NdefRecord as a simple array.
     * Example: ["1234", "HEAVY"]
     */
    ONLY_PAYLOAD_ARRAY,

    /**
     * PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON sets Forwarder to send String UTF-converted payload content
     * read from NdefRecord and user additional messages map in JSON format.
     * Example (attrX are records read from NFC tag, every other is additional message content, passed
     * via addidtionalMessages map to processIntent() function):
     * {"attr0":"nfc-content", "attr1":"sth", "id":"1234", "weight":"HEAVY"}
     */
    PAYLOAD_AND_ADDITIONAL_MESSAGE_JSON,

    /**
     * NDEF_MESSAGE_ARRAY sets Forwarder to send raw unconverted info about tnf, type and payload of every record.
     * Example: "[{"mId":[],"mPayload":[2,101,110,50],"mType":[84],"mTnf":1},{"mId":[],"mPayload":[2,101,110,82,97,107,105,101,116,97],"mType":[84],"mTnf":1}]"
     */
    NDEF_MESSAGE_ARRAY
}