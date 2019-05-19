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
                              private val serverUri: String,
                              private val defaultSubscriptionTopic: String,
                              private val clientId: String = MqttClient.generateClientId(),
                              private val isTlsEnabled: Boolean = false,
                              private val caInputStream: InputStream? = null,
                              private val subscribtionTimeout: Int = 10,
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
        unsubscribeFromAllTopicsAndDisconnect()

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

                // notify observers
                onSubscriptionListener.onDisconnectSuccessful()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(
                    TAG, application.resources.getString(
                        R.string.server_disconnection_failure
                    ))

                // notify observers
                onSubscriptionListener.onDisconnectError(application.resources.getString(R.string.server_disconnection_failure))
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
        client.connect(obtainOptions()).actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "Successfully connected to $serverUri.")

                // notify observers
                onSubscriptionListener.onConnectSuccessful()

                // try to subscribe
                subscribeToTopic(topic)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Failed to connect to $serverUri!")

                // notify observers
                onSubscriptionListener.onConnectError(application.getString(R.string.server_connection_failure))
                onSubscriptionListener.onSubscriptionError(application.getString(R.string.server_connection_failure))
            }
        }
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
                onSubscriptionListener.onConnectError(application.getString(R.string.server_null_ssl_cert_failure))
                onSubscriptionListener.onSubscriptionError(application.getString(R.string.server_null_ssl_cert_failure))
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
                onSubscriptionListener.onSubscriptionSuccess()
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "Subscription to $topic failed!")

                // notify observers
                onSubscriptionListener.onSubscriptionError("Subscription to $topic failed!")
            }
        })
    }

    private fun launchTimeoutTimer(topic: String) {
        val timer = object : CountDownTimer(subscribtionTimeout * 1000L, subscribtionTimeout * 1000L) {

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