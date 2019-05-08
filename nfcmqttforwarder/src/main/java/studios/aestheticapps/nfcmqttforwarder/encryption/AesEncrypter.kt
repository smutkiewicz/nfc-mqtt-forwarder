package studios.aestheticapps.nfcmqttforwarder.encryption

import android.util.Base64
import android.util.Base64.*
import android.util.Log
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

internal object AesEncrypter {

    private const val charsetName = "UTF-8"
    private const val algorithm = "AES"
    private const val keySize = 16

    private val TAG = AesEncrypter::class.java.simpleName
    private var secretKey: SecretKeySpec? = null
    private var key: ByteArray? = null

    fun encrypt(strToEncrypt: String, secret: String): String {
        try {
            setKey(secret)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return encodeToString(cipher.doFinal(strToEncrypt.toByteArray(charset("UTF-8"))), DEFAULT)
        } catch (e: Exception) {
            Log.d(TAG,"Error while encrypting: $e")
        }

        return ""
    }

    fun decrypt(strToDecrypt: String, secret: String): String {
        try {
            setKey(secret)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            return String(cipher.doFinal(decode(strToDecrypt, Base64.DEFAULT)))
        } catch (e: Exception) {
            Log.d(TAG,"Error while decrypting: $e")
        }

        return ""
    }

    private fun setKey(myKey: String) {
        val sha = MessageDigest.getInstance("SHA-1")

        try {
            key = myKey.toByteArray(charset(charsetName))
            key = sha.digest(key)
            key = key!!.copyOf(keySize)
            secretKey = SecretKeySpec(
                key!!,
                algorithm
            )
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }
}