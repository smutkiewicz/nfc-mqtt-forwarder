package studios.aestheticapps.nfcmqttforwarder

/**
 * Interface helping developer react on forwarding results.
 */
interface OnForwardResultListener {

    /**
     * Called when whole forwarding process (connect-publish-disconnect) succeeds.
     */
    fun onForwardingSuccessful()

    /**
     * Called when whole forwarding process (connect-publish-disconnect) fails.
     */
    fun onForwardingError()

    fun onConnectSuccessful() {}

    /**
     * Needs calling super when overrided.
     */
    fun onConnectError(message: String) {
        onForwardingError()
    }

    fun onPublishSuccessful() {}

    /**
     * Needs calling super when overrided.
     */
    fun onPublishError(message: String) {
        onForwardingError()
    }

    fun onDisconnectSuccessful() {}

    /**
     * Needs calling super when overrided.
     */
    fun onDisconnectError(message: String) {
        onForwardingError()
    }
}