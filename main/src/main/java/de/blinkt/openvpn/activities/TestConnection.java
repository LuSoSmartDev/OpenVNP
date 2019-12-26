/*
 * Copyright (c) 2012-2019 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

public class TestConnection extends Activity implements VpnStatus.StateListener {

    VpnProfile mResult;
    Button ConnectBtn;
    TextView textView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_layout);

        //load profile first

        try {
            InputStream reader = getAssets().open("congaubeo.ovpn");
            doImport(reader);
        }catch (Exception ex){}
        ConnectBtn = findViewById(R.id.connection);
        textView = findViewById(R.id.LogEvent);
        ConnectBtn.setOnClickListener(v -> {
            if (VpnStatus.isVPNActive() && mResult.getUUIDString().equals(VpnStatus.getLastConnectedVPNProfile())) {
                Intent disconnectVPN = new Intent(TestConnection.this, DisconnectVPN.class);
                startActivity(disconnectVPN);

            } else {
                startVPN(mResult);
            }
        });
    }
    private void startVPN(VpnProfile profile) {
        ProfileManager pm = ProfileManager.getInstance(this);
        pm.saveProfile(this, profile);
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }
    public void doImport(InputStream inputStream) {
        ConfigParser configParser = new ConfigParser();
        try {
            configParser.parseConfig(new InputStreamReader(inputStream));
            this.mResult = configParser.convertProfile();
            ProfileManager pm = ProfileManager.getInstance(this);
            pm.addProfile(mResult);
            pm.saveProfileList(this);
            pm.saveProfile(this, mResult);
            //mResult = pm.getProfileByName(Build.MODEL);


            Log.e("VPNStatus",mResult.getName());

            //embedFiles(configParser);
        } catch (ConfigParser.ConfigParseError | IOException e) {
            //log(R.string.error_reading_config_file, new Object[0]);
            //log(e.getLocalizedMessage());
            this.mResult = null;
        }
    }


    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {
           runOnUiThread(() -> {
                String mLastStatusMessage = VpnStatus.getLastCleanLogMessage(TestConnection.this);
                if (textView!=null){
                    textView.setText(mLastStatusMessage);
                    if (VpnStatus.isVPNActive()){
                        ConnectBtn.setText("Connected");
                    }

                }
            });

    }

    @Override
    public void setConnectedVPN(String uuid) {

    }
}
