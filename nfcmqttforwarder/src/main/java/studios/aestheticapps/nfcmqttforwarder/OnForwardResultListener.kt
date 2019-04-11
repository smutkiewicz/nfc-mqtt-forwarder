package studios.aestheticapps.nfcmqttforwarder

/**
 * Interface helping developer react on forwarding results.
 */
interface OnForwardResultListener {

    fun onConnectSuccessful() {}

    fun onConnectError(message: String) {}

    fun onPublishSuccessful() {}

    fun onPublishError(message: String) {}

    fun onDisconnectSuccessful() {}

    fun onDisconnectError(message: String) {}

}