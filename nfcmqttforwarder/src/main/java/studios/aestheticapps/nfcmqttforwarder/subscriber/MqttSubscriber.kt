package studios.aestheticapps.nfcmqttforwarder.subscriber

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import studios.aestheticapps.nfcmqttforwarder.R
import studios.aestheticapps.nfcmqttforwarder.forwarder.OnNfcMqttForwardingResultListener
import studios.aestheticapps.nfcmqttforwarder.ssl.SocketFactory
import java.io.InputStream

internal class MqttSubscriber(private val application: Application,
                              private val brokerUri: String,
                              private val defaultSubscriptionTopic: String,
                              private val clientId: String = MqttClient.generateClientId(),
                              private val isTlsEnabled: Boolean = false,
                              private val caInputStream: InputStream? = null,
                              private val subscribtionTimeout: Int = 10
) : MqttCallback {

    var onSubscriptionListener: OnNfcMqttForwardingResultListener? = null

    private val client by lazy { MqttAndroidClient(application, brokerUri, clientId) }
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
        unsubscribeFromAllTopicsAndDisconnect()

        // notify observers
        onSubscriptionListener?.onSubscriptionMessageArrived(topic, message)
    }

    override fun connectionLost(cause: Throwable?) {}

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    fun subscribeToTopicAndReceiveResponse(subscriptionTopic: String = defaultSubscriptionTopic) {
        connectToBrokerAndSubscribe(subscriptionTopic)
    }

    fun unsubscribeFromTopic(topic: String) {
        client.takeIf { it.isConnected }?.unsubscribe(topic, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully unsubscribed $topic.")
                subscribedTo.remove(topic)
                Log.d(TAG, "Subscriber of = $subscribedTo")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Failed unsubscribing $topic.")
            }
        })
    }

    fun unsubscribeFromAllTopicsAndDisconnect() {
        subscribedTo.forEach { unsubscribeFromTopic(it.key) }
        client.takeIf { it.isConnected }?.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully disconnected from $brokerUri.")

                // notify observers
                onSubscriptionListener?.onDisconnectSuccessful()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(
                    TAG, application.resources.getString(
                        R.string.broker_disconnection_failure
                    ))

                // notify observers
                onSubscriptionListener?.onDisconnectError(application.resources.getString(R.string.broker_disconnection_failure))
            }
        })
    }

    private fun connectToBrokerAndSubscribe(topic: String) {
        // check if client is already connected
        client.takeIf { it.isConnected }?.let {
            subscribeToTopic(topic)
            return
        }

        // connect to MQTT Broker
        client.connect(obtainOptions(), null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully connected to $brokerUri.")

                // notify observers
                onSubscriptionListener?.onConnectSuccessful()

                // try to subscribe
                subscribeToTopic(topic)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Failed to connect to $brokerUri!")

                // notify observers
                onSubscriptionListener?.onConnectError(application.getString(R.string.broker_connection_failure))
                onSubscriptionListener?.onSubscriptionError(application.getString(R.string.broker_connection_failure))
            }
        })
    }

    private fun obtainOptions() : MqttConnectOptions {
        val options = MqttConnectOptions()
        options.apply {
            mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
            options.keepAliveInterval = 30
            options.isAutomaticReconnect = true
        }

        if (isTlsEnabled) {
            if (caInputStream != null) {
                caInputStream.reset()
                val socketFactoryOptions = SocketFactory.SocketFactoryOptions()
                val sfo = socketFactoryOptions.withCaInputStream(caInputStream)
                options.socketFactory = SocketFactory(sfo)
            } else {
                onSubscriptionListener?.onConnectError(application.getString(R.string.broker_null_ssl_cert_failure))
                onSubscriptionListener?.onSubscriptionError(application.getString(R.string.broker_null_ssl_cert_failure))
            }
        }

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

                launchTimeoutTimer(topic)

                Log.d(TAG, "Subscriber of = $subscribedTo")

                // notify observers
                onSubscriptionListener?.onSubscriptionSuccess()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Subscription to $topic failed!")

                // notify observers
                onSubscriptionListener?.onSubscriptionError("Subscription to $topic failed!")
            }
        })
    }

    private fun launchTimeoutTimer(topic: String) {
        object : CountDownTimer(subscribtionTimeout * 1000L, subscribtionTimeout * 1000L) {

            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // check if we're expecting messages from this topic
                if (subscribedTo[topic] == false) return

                unsubscribeFromAllTopicsAndDisconnect()
            }

        }.start()
    }


    companion object {
        private val TAG = MqttSubscriber::class.java.simpleName
    }
}