package studios.aestheticapps.nfcmqttforwarder.forwarder

import org.eclipse.paho.client.mqttv3.MqttMessage

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
    fun onForwardingError()

    /**
     * Called when response subscription is enabled
     */
    fun onSubscriptionMessageArrived(topic: String?, message: MqttMessage?) {}

    fun onConnectSuccessful() {}

    fun onConnectError(message: String) {}

    fun onPublishSuccessful() {}

    fun onPublishError(message: String) {}

    fun onDisconnectSuccessful() {}

    fun onDisconnectError(message: String) {}
}