package studios.aestheticapps.nfcmqttforwarder

import android.app.Application
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttSubscriber(private val application: Application,
                     private val serverUri: String,
                     private val defaultSubscriptionTopic: String,
                     private val clientId: String = MqttClient.generateClientId(),
                     private val onSubscriptionListener: OnSubscriptionListener) : MqttCallback {

    private val client by lazy { MqttAndroidClient(application, serverUri, clientId) }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        Log.i(TAG, "Message arrived on topic = $topic with msg = \"$message\".")
        unsubscribeFromTopic(topic!!)

        // notify observers
        onSubscriptionListener.onSubscriptionMessageArrived(topic, message)

        // finish tasks by disconnecting
        client.disconnect()
    }

    override fun connectionLost(cause: Throwable?) {
        Log.e(TAG, "Connection lost!")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    fun subscribeToTopicAndReceiveResponse(subscriptionTopic: String = defaultSubscriptionTopic) {
        connectToServer(subscriptionTopic)
    }

    private fun connectToServer(topic: String) {
        // connect to MQTT Server
        client.setCallback(this)
        client.connect(obtainOptions()).actionCallback = object : IMqttActionListener {
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
        client.subscribe(topic, 1, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Subscribed to $topic!")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Subscription to $topic failed!")
            }
        })
    }

    private fun unsubscribeFromTopic(topic: String) {
        client.unsubscribe(topic)
    }

    interface OnSubscriptionListener {

        fun onSubscriptionMessageArrived(topic: String?, message: MqttMessage?)

    }

    companion object {
        private val TAG = MqttSubscriber::class.java.simpleName
    }
}