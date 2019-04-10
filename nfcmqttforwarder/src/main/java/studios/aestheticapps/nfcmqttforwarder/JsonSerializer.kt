package studios.aestheticapps.nfcmqttforwarder

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class JsonSerializer {

    fun <T> arrayToJson(stream: List<T>): String =
        GsonBuilder()
            .create()
            .toJson(stream, object : TypeToken<List<T>>() {}.type)

    fun <T> jsonToArray(stream: String): List<T> =
        GsonBuilder()
            .create()
            .fromJson(stream, object : TypeToken<List<T>>() {}.type)

}