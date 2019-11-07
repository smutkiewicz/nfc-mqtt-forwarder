package studios.aestheticapps.nfcmqttforwarder.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

internal class JsonSerializer {

    fun <T> arrayToJson(stream: List<T>): String =
        GsonBuilder()
            .create()
            .toJson(stream, object : TypeToken<List<T>>() {}.type)

    fun <K, V> mapToJson(stream: Map<K, V>): String =
        GsonBuilder()
            .create()
            .toJson(stream, object : TypeToken<Map<K, V>>() {}.type)

    fun <T> jsonToArray(stream: String): List<T> =
        GsonBuilder()
            .create()
            .fromJson(stream, object : TypeToken<List<T>>() {}.type)

}