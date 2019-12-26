package de.blinkt.openvpn.core;

import java.util.Iterator;
import net.openvpn.ovpn3.ClientAPI_Config;
import net.openvpn.ovpn3.ClientAPI_EvalConfig;
import net.openvpn.ovpn3.ClientAPI_Event;
import net.openvpn.ovpn3.ClientAPI_ExternalPKICertRequest;
import net.openvpn.ovpn3.ClientAPI_ExternalPKISignRequest;
import net.openvpn.ovpn3.ClientAPI_LogInfo;
import net.openvpn.ovpn3.ClientAPI_OpenVPNClient;
import net.openvpn.ovpn3.ClientAPI_ProvideCreds;
import net.openvpn.ovpn3.ClientAPI_Status;
import net.openvpn.ovpn3.ClientAPI_StringVec;
import net.openvpn.ovpn3.ClientAPI_TransportStats;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.OpenVPNManagement.PausedStateCallback;
import de.blinkt.openvpn.core.OpenVPNManagement.pauseReason;

/* renamed from: de.blinkt.openvpn.core.OpenVPNThreadv3 */
public class OpenVPNThreadv3 extends ClientAPI_OpenVPNClient implements Runnable, OpenVPNManagement {
    static final long EmulateExcludeRoutes = 65536;
    private OpenVPNService mService;
    private VpnProfile mVp;

    /* renamed from: de.blinkt.openvpn.core.OpenVPNThreadv3$StatusPoller */
    class StatusPoller implements Runnable {
        private long mSleeptime;
        boolean mStopped = false;

        public StatusPoller(long j) {
            this.mSleeptime = j;
        }

        public void run() {
            while (!this.mStopped) {
                try {
                    Thread.sleep(this.mSleeptime);
                } catch (InterruptedException unused) {
                }
                ClientAPI_TransportStats transport_stats = OpenVPNThreadv3.this.transport_stats();
                VpnStatus.updateByteCount(transport_stats.getBytesIn(), transport_stats.getBytesOut());
            }
        }

        public void stop() {
            this.mStopped = true;
        }
    }

    public void setPauseCallback(PausedStateCallback pausedStateCallback) {
    }

    @Override
    public void sendCRResponse(String response) {

    }

    public boolean tun_builder_new() {
        return true;
    }

    public boolean tun_builder_set_layer(int i) {
        return i == 3;
    }

    static {
        System.loadLibrary("ovpn3");
    }

    public void run() {
        if (setConfig(this.mVp.getConfigFile(this.mService, true))) {
            setUserPW();
            VpnStatus.logInfo(platform());
            VpnStatus.logInfo(copyright());
            StatusPoller statusPoller = new StatusPoller(2000);
            new Thread(statusPoller, "Status Poller").start();
            ClientAPI_Status connect = connect();
            if (connect.getError()) {
                VpnStatus.logError(String.format("connect() error: %s: %s", new Object[]{connect.getStatus(), connect.getMessage()}));
            } else {
                VpnStatus.updateStateString("NOPROCESS", "OpenVPN3 thread finished", R.string.state_noprocess, ConnectionStatus.LEVEL_NOTCONNECTED);
            }
            statusPoller.stop();
        }
    }

    public boolean tun_builder_set_remote_address(String str, boolean z) {
        this.mService.setMtu(1500);
        return true;
    }

    public boolean tun_builder_set_mtu(int i) {
        this.mService.setMtu(i);
        return true;
    }

    public boolean tun_builder_add_dns_server(String str, boolean z) {
        this.mService.addDNS(str);
        return true;
    }

    public boolean tun_builder_add_route(String str, int i, int i2, boolean z) {
        if (str.equals("remote_host")) {
            return false;
        }
        if (z) {
            OpenVPNService openVPNService = this.mService;
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("/");
            sb.append(i);
            openVPNService.addRoutev6(sb.toString(), "tun");
        } else {
            this.mService.addRoute(new CIDRIP(str, i), true);
        }
        return true;
    }

    public boolean tun_builder_exclude_route(String str, int i, int i2, boolean z) {
        if (z) {
            OpenVPNService openVPNService = this.mService;
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("/");
            sb.append(i);
            openVPNService.addRoutev6(sb.toString(), "wifi0");
        } else {
            this.mService.addRoute(new CIDRIP(str, i), false);
        }
        return true;
    }

    public boolean tun_builder_add_search_domain(String str) {
        this.mService.setDomain(str);
        return true;
    }

    public int tun_builder_establish() {
        return this.mService.openTun().detachFd();
    }

    public boolean tun_builder_set_session_name(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("We should call this session");
        sb.append(str);
        VpnStatus.logDebug(sb.toString());
        return true;
    }

    public boolean tun_builder_add_address(String str, int i, String str2, boolean z, boolean z2) {
        if (!z) {
            this.mService.setLocalIP(new CIDRIP(str, i));
        } else {
            OpenVPNService openVPNService = this.mService;
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("/");
            sb.append(i);
            openVPNService.setLocalIPv6(sb.toString());
        }
        return true;
    }

    public boolean tun_builder_reroute_gw(boolean z, boolean z2, long j) {
        if ((j & EmulateExcludeRoutes) != 0) {
            return true;
        }
        if (z) {
            this.mService.addRoute("0.0.0.0", "0.0.0.0", "127.0.0.1", OpenVPNService.VPNSERVICE_TUN);
        }
        if (z2) {
            this.mService.addRoutev6("::/0", OpenVPNService.VPNSERVICE_TUN);
        }
        return true;
    }

    private boolean setConfig(String str) {
        ClientAPI_Config clientAPI_Config = new ClientAPI_Config();
        if (this.mVp.getPasswordPrivateKey() != null) {
            clientAPI_Config.setPrivateKeyPassword(this.mVp.getPasswordPrivateKey());
        }
        clientAPI_Config.setContent(str);
        clientAPI_Config.setTunPersist(this.mVp.mPersistTun);
        clientAPI_Config.setGuiVersion(this.mVp.getVersionEnvString(this.mService));
        clientAPI_Config.setExternalPkiAlias("extpki");
        clientAPI_Config.setCompressionMode("asym");
        clientAPI_Config.setInfo(true);
        clientAPI_Config.setAllowLocalLanAccess(this.mVp.mAllowLocalLAN);
        ClientAPI_EvalConfig eval_config = eval_config(clientAPI_Config);
        if (eval_config.getExternalPki()) {
            VpnStatus.logDebug("OpenVPN3 core assumes an external PKI config");
        }
        if (eval_config.getError()) {
            StringBuilder sb = new StringBuilder();
            sb.append("OpenVPN config file parse error: ");
            sb.append(eval_config.getMessage());
            VpnStatus.logError(sb.toString());
            return false;
        }
        clientAPI_Config.setContent(str);
        return true;
    }

    public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest clientAPI_ExternalPKICertRequest) {
        VpnStatus.logDebug("Got external PKI certificate request from OpenVPN core");
        String[] externalCertificates = this.mVp.getExternalCertificates(this.mService);
        if (externalCertificates == null) {
            clientAPI_ExternalPKICertRequest.setError(true);
            clientAPI_ExternalPKICertRequest.setErrorText("Error in pki cert request");
            return;
        }
        String str = externalCertificates[0];
        if (externalCertificates[1] != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("\n");
            sb.append(externalCertificates[1]);
            str = sb.toString();
        }
        clientAPI_ExternalPKICertRequest.setSupportingChain(str);
        clientAPI_ExternalPKICertRequest.setCert(externalCertificates[2]);
        clientAPI_ExternalPKICertRequest.setError(false);
    }

    public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest clientAPI_ExternalPKISignRequest) {
        boolean z;
        StringBuilder sb = new StringBuilder();
        sb.append("Got external PKI signing request from OpenVPN core for algorithm ");
        sb.append(clientAPI_ExternalPKISignRequest.getPadding());
        VpnStatus.logDebug(sb.toString());
        if (clientAPI_ExternalPKISignRequest.getPadding().equals("RSA_PKCS1_PADDING")) {
            z = true;
        } else if (clientAPI_ExternalPKISignRequest.getPadding().equals("RSA_NO_PADDING")) {
            z = false;
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Illegal padding in sign request");
            sb2.append(clientAPI_ExternalPKISignRequest.getPadding());
            throw new IllegalArgumentException(sb2.toString());
        }
        clientAPI_ExternalPKISignRequest.setSig(this.mVp.getSignedData(this.mService, clientAPI_ExternalPKISignRequest.getData(), z));
    }

    /* access modifiers changed from: 0000 */
    public void setUserPW() {
        if (this.mVp.isUserPWAuth()) {
            ClientAPI_ProvideCreds clientAPI_ProvideCreds = new ClientAPI_ProvideCreds();
            clientAPI_ProvideCreds.setCachePassword(true);
            clientAPI_ProvideCreds.setPassword(this.mVp.getPasswordAuth());
            clientAPI_ProvideCreds.setUsername(this.mVp.mUsername);
            provide_creds(clientAPI_ProvideCreds);
        }
    }

    public boolean socket_protect(int i, String str, boolean z) {
        return this.mService.protect(i);
    }

    public OpenVPNThreadv3(OpenVPNService openVPNService, VpnProfile vpnProfile) {
        init_process();
        this.mVp = vpnProfile;
        this.mService = openVPNService;
    }

    public boolean stopVPN(boolean z) {
        stop();
        return false;
    }

    public void networkChange(boolean z) {
        reconnect(1);
    }

    public void log(ClientAPI_LogInfo clientAPI_LogInfo) {
        String text = clientAPI_LogInfo.getText();
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        VpnStatus.logInfo(text);
    }

    public void event(ClientAPI_Event clientAPI_Event) {
        String name = clientAPI_Event.getName();
        String info = clientAPI_Event.getInfo();
        if (name.equals("INFO")) {
            VpnStatus.logInfo(R.string.info_from_server, info);
            if (info.startsWith("OPEN_URL:")) {
               //TODO this.mService.trigger_url_open(info);
            }
        } else {
            VpnStatus.updateStateString(name, info);
        }
        if (clientAPI_Event.getError()) {
            VpnStatus.logError(String.format("EVENT(Error): %s: %s", new Object[]{name, info}));
        }
    }

    public ClientAPI_StringVec tun_builder_get_local_networks(boolean z) {
        ClientAPI_StringVec clientAPI_StringVec = new ClientAPI_StringVec();
        Iterator it = NetworkUtils.getLocalNetworks(this.mService, z).iterator();
        while (it.hasNext()) {
            clientAPI_StringVec.add((String) it.next());
        }
        return clientAPI_StringVec;
    }

    public boolean pause_on_connection_timeout() {
        VpnStatus.logInfo("pause on connection timeout?! ");
        return true;
    }

    public void stop() {
        super.stop();
        this.mService.openvpnStopped();
    }

    public void reconnect() {
        reconnect(1);
    }

    public void pause(pauseReason pausereason) {
        super.pause(pausereason.toString());
    }
}