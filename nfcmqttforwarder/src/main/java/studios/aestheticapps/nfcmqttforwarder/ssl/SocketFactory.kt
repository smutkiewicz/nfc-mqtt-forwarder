package studios.aestheticapps.nfcmqttforwarder.ssl

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*
import javax.security.cert.CertificateException

/**
 * Original SocketFactory file taken from https://github.com/owntracks/android
 */
internal class SocketFactory @Throws(
    KeyStoreException::class,
    NoSuchAlgorithmException::class,
    IOException::class,
    KeyManagementException::class,
    CertificateException::class,
    UnrecoverableKeyException::class
)
@JvmOverloads constructor(options: SocketFactoryOptions = SocketFactoryOptions()) : SSLSocketFactory() {
    private val factory: javax.net.ssl.SSLSocketFactory


    private val tmf: TrustManagerFactory

    private val trustManagers: Array<TrustManager>
        get() = tmf.trustManagers

    class SocketFactoryOptions {

        var caCrtInputStream: InputStream? = null
            private set
        var caClientP12InputStream: InputStream? = null
            private set
        var caClientP12Password: String? = null
            private set

        fun withCaInputStream(stream: InputStream): SocketFactoryOptions {
            this.caCrtInputStream = stream
            return this
        }

        fun withClientP12InputStream(stream: InputStream): SocketFactoryOptions {
            this.caClientP12InputStream = stream
            return this
        }

        fun withClientP12Password(password: String): SocketFactoryOptions {
            this.caClientP12Password = password
            return this
        }

        fun hasCaCrt() = caCrtInputStream != null

        fun hasClientP12Crt() = caClientP12Password != null

        fun hasClientP12Password() = caClientP12Password != null && caClientP12Password != ""
    }

    init {
        Log.v(TAG, "initializing CustomSocketFactory")

        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val kmf = KeyManagerFactory.getInstance("X509")


        if (options.hasCaCrt()) {
            Log.v(this.toString(), "MQTT_CONNECTION_OPTIONS.hasCaCrt(): true")

            val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            caKeyStore.load(null, null)

            val caCF = CertificateFactory.getInstance("X.509")
            val ca = caCF.generateCertificate(options.caCrtInputStream) as X509Certificate
            val alias = ca.subjectX500Principal.name

            // Set propper alias name
            caKeyStore.setCertificateEntry(alias, ca)
            tmf.init(caKeyStore)

            Log.v(TAG, "Certificate Owner: " + ca.subjectDN.toString())
            Log.v(TAG, "Certificate Issuer: " + ca.issuerDN.toString())
            Log.v(TAG, "Certificate Serial Number: " + ca.serialNumber.toString())
            Log.v(TAG, "Certificate Algorithm: " + ca.sigAlgName)
            Log.v(TAG, "Certificate Version: " + ca.version)
            Log.v(TAG, "Certificate OID: " + ca.sigAlgOID)

            val aliasesCA = caKeyStore.aliases()
            while (aliasesCA.hasMoreElements()) {
                val o = aliasesCA.nextElement()
                Log.v(TAG, "Alias: $o" +
                        " isKeyEntry:" + caKeyStore.isKeyEntry(o) +
                        " isCertificateEntry: " + caKeyStore.isCertificateEntry(o)
                )
            }

        } else {
            Log.v(TAG, "CA sideload: false, using system keystore")
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)
            tmf.init(keyStore)
        }

        if (options.hasClientP12Crt()) {
            Log.v(TAG, "MQTT_CONNECTION_OPTIONS.hasClientP12Crt(): true")

            val clientKeyStore = KeyStore.getInstance("PKCS12")

            clientKeyStore.load(
                options.caClientP12InputStream,
                if (options.hasClientP12Password()) options.caClientP12Password!!.toCharArray() else CharArray(0)
            )
            kmf.init(
                clientKeyStore,
                if (options.hasClientP12Password()) options.caClientP12Password!!.toCharArray() else CharArray(0)
            )

            Log.v(this.toString(), "Client .p12 Keystore content: ")
            val aliasesClientCert = clientKeyStore.aliases()
            while (aliasesClientCert.hasMoreElements()) {
                val o = aliasesClientCert.nextElement()
                Log.v(TAG, "Alias: $o")
            }

        } else {
            Log.v(TAG, "Client .p12 sideload: false, using null CLIENT cert")
            kmf.init(null, null)
        }

        // Create an SSLContext that uses our TrustManager
        val context = SSLContext.getInstance("TLSv1")
        context.init(kmf.keyManagers, trustManagers, null)
        this.factory = context.socketFactory

    }

    override fun getDefaultCipherSuites() = this.factory.defaultCipherSuites

    override fun getSupportedCipherSuites() = this.factory.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(): Socket {
        val r = this.factory.createSocket() as SSLSocket
        r.enabledProtocols = enabledProtocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val r = this.factory.createSocket(s, host, port, autoClose) as SSLSocket
        r.enabledProtocols = enabledProtocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val r = this.factory.createSocket(host, port) as SSLSocket
        r.enabledProtocols = enabledProtocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        val r = this.factory.createSocket(host, port, localHost, localPort) as SSLSocket
        r.enabledProtocols = enabledProtocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        val r = this.factory.createSocket(host, port) as SSLSocket
        r.enabledProtocols = enabledProtocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        val r = this.factory.createSocket(address, port, localAddress, localPort) as SSLSocket
        r.enabledProtocols = enabledProtocols
        return r
    }

    private companion object {
        private const val TAG = "SocketFactory"
        private val enabledProtocols = arrayOf("TLSv1", "TLSv1.1", "TLSv1.2")
    }
}