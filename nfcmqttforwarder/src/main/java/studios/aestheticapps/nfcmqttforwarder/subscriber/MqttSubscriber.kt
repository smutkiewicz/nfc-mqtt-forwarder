package studios.aestheticapps.nfcmqttforwarder.subscriber

import android.app.Application
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import studios.aestheticapps.nfcmqttforwarder.R
import studios.aestheticapps.nfcmqttforwarder.forwarder.OnNfcMqttForwardingResultListener

internal class MqttSubscriber(private val application: Application,
                              private val serverUri: String,
                              private val defaultSubscriptionTopic: String,
                              private val clientId: String = MqttClient.generateClientId(),
                              private val onSubscriptionListener: OnNfcMqttForwardingResultListener
) : MqttCallback {

    private val client by lazy { MqttAndroidClient(application, serverUri, clientId) }
    private val subscribedTo: HashMap<String, Boolean> = HashMap()

    init {
        client.setCallback(this)
    }

    override fun messageArrived(topic: String, message: MqttMessage?) {
        // check if we're expecting messages from this topic
        if (subscribedTo[topic] == false) return

        Log.w(TAG, "Message arrived on topic = $topic with msg = \"$message\".")

        // release topic
        subscribedTo[topic] = false

        // unsubscribe
        unsubscribeFromTopic(topic)

        // notify observers
        onSubscriptionListener.onSubscriptionMessageArrived(topic, message)
    }

    override fun connectionLost(cause: Throwable?) {}

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    fun subscribeToTopicAndReceiveResponse(subscriptionTopic: String = defaultSubscriptionTopic) {
        connectToServerAndSubscribe(subscriptionTopic)
    }

    fun unsubscribeFromTopic(topic: String) {
        client.takeIf { it.isConnected }?.unsubscribe(topic)?.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully unsubscribed $topic.")
                subscribedTo.remove(topic)
                Log.d(TAG, "Subscriber of = $subscribedTo")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Failed unsubscribing $topic.")
            }
        }
    }

    fun unsubscribeFromAllTopicsAndDisconnect() {
        subscribedTo.forEach { unsubscribeFromTopic(it.key) }
        client.takeIf { it.isConnected }?.disconnect()?.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully disconnected from $serverUri.")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(
                    TAG, application.resources.getString(
                        R.string.server_disconnection_failure
                    ))
            }
        }
    }

    private fun connectToServerAndSubscribe(topic: String) {
        // check if client is already connected
        client.takeIf { it.isConnected }?.let {
            subscribeToTopic(topic)
            return
        }

        // connect to MQTT Server
        client.connect().actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully connected to $serverUri.")
                subscribeToTopic(topic)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Failed to connect to $serverUri!")
            }
        }
    }

    private fun obtainOptions() : MqttConnectOptions {
        val options = MqttConnectOptions()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        options.keepAliveInterval = 30
        options.isAutomaticReconnect = true

        return options
    }

    private fun subscribeToTopic(topic: String) {
        // don't try to subscribe to already subscribed
        if (subscribedTo.containsKey(topic) && subscribedTo[topic] == true) return

        client.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Subscribed to $topic!")

                // mark waiting for response
                subscribedTo[topic] = true

                Log.d(TAG, "Subscriber of = $subscribedTo")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Subscription to $topic failed!")
            }
        })
    }

    companion object {
        private val TAG = MqttSubscriber::class.java.simpleName
    }
}