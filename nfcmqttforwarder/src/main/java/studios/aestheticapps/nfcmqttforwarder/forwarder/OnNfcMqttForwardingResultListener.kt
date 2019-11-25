package studios.aestheticapps.nfcmqttforwarder.forwarder

/**
 * Interface helping developer react on forwarding results.
 */
interface OnNfcMqttForwardingResultListener {

    /**
     * Called when whole forwarding process (connect-publish-disconnect) succeeds.
     */
    fun onForwardingSuccessful()

    /**
     * Called when whole forwarding process (connect-publish-disconnect) fails.
     */
    fun onForwardingError(message: String)


    fun onConnectSuccessful() {}

    fun onConnectError(message: String) {}

    fun onPublishSuccessful() {}

    fun onPublishError(message: String) {}

    fun onDisconnectSuccessful() {}

    fun onDisconnectError(message: String) {}
}