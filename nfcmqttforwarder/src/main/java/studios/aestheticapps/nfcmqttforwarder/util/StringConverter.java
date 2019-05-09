package studios.aestheticapps.nfcmqttforwarder.util;

public class StringConverter {

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (byte aSrc : src) {
            buffer[0] = Character.forDigit((aSrc >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(aSrc & 0x0F, 16);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }

    public static String bytesToHexdumpString(byte[] src) {
        String hexdump = "";
        for (byte aSrc : src) {
            String x = Integer.toHexString(((int) aSrc & 0xff));
            if (x.length() == 1) {
                x = '0' + x;
            }
            hexdump += x + ' ';
        }


        return hexdump;
    }

}
