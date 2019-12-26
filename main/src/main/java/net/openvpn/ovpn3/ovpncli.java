package net.openvpn.ovpn3;

public class ovpncli {
    public static String getOVPN_RSA_PKCS1_PADDING() {
        return ovpncliJNI.OVPN_RSA_PKCS1_PADDING_get();
    }

    public static String getOVPN_RSA_NO_PADDING() {
        return ovpncliJNI.OVPN_RSA_NO_PADDING_get();
    }
}
