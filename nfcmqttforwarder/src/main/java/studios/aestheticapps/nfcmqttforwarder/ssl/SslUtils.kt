package studios.aestheticapps.nfcmqttforwarder.ssl

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

internal object SslUtil {

    fun getSocketFactory(password: String, clientKeyPath: String, serverCertPath: String): SSLSocketFactory {
        // client key
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(FileInputStream(clientKeyPath), password.toCharArray())

        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(ks, password.toCharArray())

        // server certificate
        val tks = KeyStore.getInstance("JKS")
        // created the key store with
        // keytool -importcert -alias rmq -file ./server_certificate.pem -keystore ./jvm_keystore
        tks.load(FileInputStream(serverCertPath), password.toCharArray())

        val tmf = TrustManagerFactory.getInstance("SunX509")
        tmf.init(tks)

        val ctx = SSLContext.getInstance("SSLv3")
        ctx.init(kmf.keyManagers, tmf.trustManagers, null)
        return ctx.socketFactory
    }

}