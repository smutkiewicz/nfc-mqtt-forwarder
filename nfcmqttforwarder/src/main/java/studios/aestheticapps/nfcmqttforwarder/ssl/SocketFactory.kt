package studios.aestheticapps.nfcmqttforwarder.ssl

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

    private val factory: SSLSocketFactory

    private val tmf: TrustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

    private val trustManagers: Array<TrustManager>
        get() = tmf.trustManagers

    class SocketFactoryOptions {

        var tlsVersion: TLSVersion = TLSVersion.TLSv1
            private set
        var caCrtInputStream: InputStream? = null
            private set
        var caClientP12InputStream: InputStream? = null
            private set
        var caClientP12Password: String? = null
            private set

        fun withCaInputStream(tlsVersion: TLSVersion, stream: InputStream): SocketFactoryOptions {
            this.tlsVersion = tlsVersion
            this.caCrtInputStream = stream
            return this
        }

        fun withClientP12InputStream(tlsVersion: TLSVersion, stream: InputStream): SocketFactoryOptions {
            this.tlsVersion = tlsVersion
            this.caClientP12InputStream = stream
            return this
        }

        fun withClientP12Password(tlsVersion: TLSVersion, password: String): SocketFactoryOptions {
            this.tlsVersion = tlsVersion
            this.caClientP12Password = password
            return this
        }

        fun hasCaCrt() = caCrtInputStream != null

        fun hasClientP12Crt() = caClientP12Password != null

        fun hasClientP12Password() = caClientP12Password != null && caClientP12Password != ""
    }

    init {
        val kmf = KeyManagerFactory.getInstance("X509")


        if (options.hasCaCrt()) {
            val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            caKeyStore.load(null, null)

            val caCF = CertificateFactory.getInstance("X.509")
            val ca = caCF.generateCertificate(options.caCrtInputStream) as X509Certificate
            val alias = ca.subjectX500Principal.name

            caKeyStore.setCertificateEntry(alias, ca)
            tmf.init(caKeyStore)

        } else {
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)
            tmf.init(keyStore)
        }

        if (options.hasClientP12Crt()) {
            val clientKeyStore = KeyStore.getInstance("PKCS12")

            clientKeyStore.load(
                options.caClientP12InputStream,
                if (options.hasClientP12Password())
                    options.caClientP12Password!!.toCharArray() else CharArray(0)
            )

            kmf.init(
                clientKeyStore,
                if (options.hasClientP12Password())
                    options.caClientP12Password!!.toCharArray() else CharArray(0)
            )

        } else {
            kmf.init(null, null)
        }

        // Create an SSLContext that uses our TrustManager
        val context = SSLContext.getInstance(options.tlsVersion.value)
        context.init(kmf.keyManagers, trustManagers, null)
        factory = context.socketFactory
    }

    override fun getDefaultCipherSuites() = factory.defaultCipherSuites

    override fun getSupportedCipherSuites() = factory.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(): Socket = (factory.createSocket() as SSLSocket).also {
        it.enabledProtocols = enabledProtocols
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket =
        (factory.createSocket(s, host, port, autoClose) as SSLSocket).also {
            it.enabledProtocols = enabledProtocols
        }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket =
        (factory.createSocket(host, port) as SSLSocket).also {
            it.enabledProtocols = enabledProtocols
        }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        (factory.createSocket(host, port, localHost, localPort) as SSLSocket).also {
            it.enabledProtocols = enabledProtocols
        }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket =
        (factory.createSocket(host, port) as SSLSocket).also {
            it.enabledProtocols = enabledProtocols
        }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        (factory.createSocket(address, port, localAddress, localPort) as SSLSocket).also {
            it.enabledProtocols = enabledProtocols
        }

    private companion object {
        private const val TAG = "SocketFactory"
        private val enabledProtocols = arrayOf("TLSv1", "TLSv1.1", "TLSv1.2")
    }
}