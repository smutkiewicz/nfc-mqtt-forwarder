package studios.aestheticapps.nfcmqttforwarder.ssl

enum class TLSCertificateType {

    /**
     * For this type of TLS cert, only login and password credentials are required.
     */
    CA_SIGNED_SERVER_CERTIFICATE,

    /**
     * For this type of TLS, ca certificate file on device is required.
     */
    SELF_SIGNED_CERTIFICATE
}