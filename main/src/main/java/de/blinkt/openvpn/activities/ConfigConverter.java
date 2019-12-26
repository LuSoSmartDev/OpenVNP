/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.github.mikephil.charting.BuildConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.fragments.Utils.FileType;
import de.blinkt.openvpn.views.FileSelectLayout;
import de.blinkt.openvpn.views.FileSelectLayout.FileSelectCallback;

/* renamed from: de.blinkt.openvpn.activities.ConfigConverter */
public class ConfigConverter extends BaseActivity implements FileSelectCallback, OnClickListener {
    private static final int CHOOSE_FILE_OFFSET = 1000;
    public static final String IMPORT_PROFILE = "de.blinkt.openvpn.IMPORT_PROFILE";
    private static final int PERMISSION_REQUEST_EMBED_FILES = 37231;
    private static final int PERMISSION_REQUEST_READ_URL = 37232;
    private static final int RESULT_INSTALLPKCS12 = 7;
    public static final String VPNPROFILE = "vpnProfile";
    private Map<FileType, FileSelectLayout> fileSelectMap = new HashMap();
    private String mAliasName = null;
    private String mEmbeddedPwFile;
    private AsyncTask<Void, Void, Integer> mImportTask;
    /* access modifiers changed from: private */
    public Vector<String> mLogEntries = new Vector<>();
    /* access modifiers changed from: private */
    public LinearLayout mLogLayout;
    private transient List<String> mPathsegments;
    /* access modifiers changed from: private */
    public EditText mProfilename;
    /* access modifiers changed from: private */
    public TextView mProfilenameLabel;
    /* access modifiers changed from: private */
    public VpnProfile mResult;
    private Uri mSourceUri;

    public void onClick(View view) {
        if (view.getId() == R.id.fab_save) {
            userActionSaveProfile();
        }
        if (view.getId() == R.id.permssion_hint && VERSION.SDK_INT == 23) {
            doRequestSDCardPermission(PERMISSION_REQUEST_EMBED_FILES);
        }
    }

    @TargetApi(23)
    private void doRequestSDCardPermission(int i) {
        requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, i);
    }

    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        if (iArr.length != 0) {
            int i2 = 0;
            if (iArr[0] != -1) {
                findViewById(R.id.files_missing_hint).setVisibility(View.GONE);
                findViewById(R.id.permssion_hint).setVisibility(View.GONE);
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.config_convert_root);
                while (i2 < linearLayout.getChildCount()) {
                    if (linearLayout.getChildAt(i2) instanceof FileSelectLayout) {
                        linearLayout.removeViewAt(i2);
                    } else {
                        i2++;
                    }
                }
                if (i == PERMISSION_REQUEST_EMBED_FILES) {
                    embedFiles(null);
                } else if (i == PERMISSION_REQUEST_READ_URL) {
                    Uri uri = this.mSourceUri;
                    if (uri != null) {
                        doImportUri(uri);
                    }
                }
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.cancel) {
            setResult(0);
            finish();
        } else if (menuItem.getItemId() == R.id.ok) {
            return userActionSaveProfile();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private boolean userActionSaveProfile() {
        VpnProfile vpnProfile = this.mResult;
        if (vpnProfile == null) {
            log(R.string.import_config_error, new Object[0]);
            Toast.makeText(this, R.string.import_config_error, Toast.LENGTH_LONG).show();
            return true;
        }
        vpnProfile.mName = this.mProfilename.getText().toString();
        if (ProfileManager.getInstance(this).getProfileByName(this.mResult.mName) != null) {
            this.mProfilename.setError(getString(R.string.duplicate_profile_name));
            return true;
        }
        Intent installPKCS12 = installPKCS12();
        if (installPKCS12 != null) {
            startActivityForResult(installPKCS12, 7);
        } else {
            saveProfile();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        VpnProfile vpnProfile = this.mResult;
        if (vpnProfile != null) {
            bundle.putSerializable(VPNPROFILE, vpnProfile);
        }
        bundle.putString("mAliasName", this.mAliasName);
        Vector<String> vector = this.mLogEntries;
        bundle.putStringArray("logentries", (String[]) vector.toArray(new String[vector.size()]));
        int[] iArr = new int[this.fileSelectMap.size()];
        int i = 0;
        for (FileType value : this.fileSelectMap.keySet()) {
            iArr[i] = value.getValue();
            i++;
        }
        bundle.putIntArray("fileselects", iArr);
        bundle.putString("pwfile", this.mEmbeddedPwFile);
        bundle.putParcelable("mSourceUri", this.mSourceUri);
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 7 && i2 == -1) {
            showCertDialog();
        }
        if (i2 == -1 && i >= 1000) {
            FileType fileTypeByValue = FileType.getFileTypeByValue(i + NotificationManagerCompat.IMPORTANCE_UNSPECIFIED);
            FileSelectLayout fileSelectLayout = (FileSelectLayout) this.fileSelectMap.get(fileTypeByValue);
            fileSelectLayout.parseResponse(intent, this);
            String data = fileSelectLayout.getData();
            switch (fileTypeByValue) {
                case USERPW_FILE:
                    this.mEmbeddedPwFile = data;
                    break;
                case PKCS12:
                    this.mResult.mPKCS12Filename = data;
                    break;
                case TLS_AUTH_FILE:
                    this.mResult.mTLSAuthFilename = data;
                    break;
                case CA_CERTIFICATE:
                    this.mResult.mCaFilename = data;
                    break;
                case CLIENT_CERTIFICATE:
                    this.mResult.mClientCertFilename = data;
                    break;
                case KEYFILE:
                    this.mResult.mClientKeyFilename = data;
                    break;
                case CRL_FILE:
                    this.mResult.mCrlFilename = data;
                    break;
                default:
                    throw new RuntimeException("Type is wrong somehow?");
            }
        }
        super.onActivityResult(i, i2, intent);
    }

    /* access modifiers changed from: private */
    public void saveProfile() {
        Intent intent = new Intent();
        ProfileManager instance = ProfileManager.getInstance(this);
        if (!TextUtils.isEmpty(this.mEmbeddedPwFile)) {
            ConfigParser.useEmbbedUserAuth(this.mResult, this.mEmbeddedPwFile);
        }
        instance.addProfile(this.mResult);
        instance.saveProfile(this, this.mResult);
        instance.saveProfileList(this);
        intent.putExtra(VpnProfile.EXTRA_PROFILEUUID, this.mResult.getUUID().toString());
        setResult(-1, intent);
        finish();
    }

    public void showCertDialog() {
        try {
            KeyChain.choosePrivateKeyAlias(this, new KeyChainAliasCallback() {
                public void alias(String str) {
                    ConfigConverter.this.mResult.mAlias = str;
                    ConfigConverter.this.saveProfile();
                }
            }, new String[]{"RSA"}, null, this.mResult.mServerName, -1, this.mAliasName);
        } catch (ActivityNotFoundException unused) {
            Builder builder = new Builder(this);
            builder.setTitle(R.string.broken_image_cert_title);
            builder.setMessage(R.string.broken_image_cert);
            //builder.setPositiveButton(R.id.ok, null);
            builder.show();
        }
    }

    private Intent installPKCS12() {
        if (!((CheckBox) findViewById(R.id.importpkcs12)).isChecked()) {
            setAuthTypeToEmbeddedPKCS12();
            return null;
        }
        String str = this.mResult.mPKCS12Filename;
        if (!VpnProfile.isEmbedded(str)) {
            return null;
        }
        Intent createInstallIntent = KeyChain.createInstallIntent();
        createInstallIntent.putExtra("PKCS12", Base64.decode(VpnProfile.getEmbeddedContent(str), 0));
        if (this.mAliasName.equals(BuildConfig.FLAVOR)) {
            this.mAliasName = null;
        }
        String str2 = this.mAliasName;
        if (str2 != null) {
            createInstallIntent.putExtra("name", str2);
        }
        return createInstallIntent;
    }

    private void setAuthTypeToEmbeddedPKCS12() {
        if (VpnProfile.isEmbedded(this.mResult.mPKCS12Filename)) {
            if (this.mResult.mAuthenticationType == 7) {
                this.mResult.mAuthenticationType = 6;
            }
            if (this.mResult.mAuthenticationType == 2) {
                this.mResult.mAuthenticationType = 1;
            }
        }
    }

    /* access modifiers changed from: private */
    public String getUniqueProfileName(String str1) {
        int i;
        ProfileManager instance = ProfileManager.getInstance(this);
        if (this.mResult.mName == null || ConfigParser.CONVERTED_PROFILE.equals(this.mResult.mName)) {
            i = 0;
        } else {
            str1 = this.mResult.mName;
            i = 0;
        }
        String str = str1;
        while (true) {
            if (str != null && instance.getProfileByName(str) == null) {
                return str;
            }
            i++;
            if (i == 1) {
                str = getString(R.string.converted_profile);
            } else {
                str = getString(R.string.converted_profile_i, new Object[]{Integer.valueOf(i)});
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.import_menu, menu);
        return true;
    }

    private String embedFile(String str, FileType fileType, boolean z) {
        if (str == null) {
            return null;
        }
        if (VpnProfile.isEmbedded(str)) {
            return str;
        }
        File findFile = findFile(str, fileType);
        if (findFile == null) {
            if (z) {
                return null;
            }
            return str;
        } else if (z) {
            return findFile.getAbsolutePath();
        } else {
            return readFileContent(findFile, fileType == FileType.PKCS12);
        }
    }

    private Pair<Integer, String> getFileDialogInfo(FileType fileType) {
        int i;
        String str = null;
        switch (fileType) {
            case USERPW_FILE:
                i = R.string.userpw_file;
                str = this.mEmbeddedPwFile;
                break;
            case PKCS12:
                i = R.string.client_pkcs12_title;
                VpnProfile vpnProfile = this.mResult;
                if (vpnProfile != null) {
                    str = vpnProfile.mPKCS12Filename;
                    break;
                }
                break;
            case TLS_AUTH_FILE:
                i = R.string.tls_auth_file;
                VpnProfile vpnProfile2 = this.mResult;
                if (vpnProfile2 != null) {
                    str = vpnProfile2.mTLSAuthFilename;
                    break;
                }
                break;
            case CA_CERTIFICATE:
                i = R.string.ca_title;
                VpnProfile vpnProfile3 = this.mResult;
                if (vpnProfile3 != null) {
                    str = vpnProfile3.mCaFilename;
                    break;
                }
                break;
            case CLIENT_CERTIFICATE:
                i = R.string.client_certificate_title;
                VpnProfile vpnProfile4 = this.mResult;
                if (vpnProfile4 != null) {
                    str = vpnProfile4.mClientCertFilename;
                    break;
                }
                break;
            case KEYFILE:
                i = R.string.client_key_title;
                VpnProfile vpnProfile5 = this.mResult;
                if (vpnProfile5 != null) {
                    str = vpnProfile5.mClientKeyFilename;
                    break;
                }
                break;
            case CRL_FILE:
                i = R.string.crl_file;
                str = this.mResult.mCrlFilename;
                break;
            default:
                i = 0;
                break;
        }
        return Pair.create(Integer.valueOf(i), str);
    }

    private File findFile(String str, FileType fileType) {
        File findFileRaw = findFileRaw(str);
        if (findFileRaw == null && str != null && !str.equals(BuildConfig.FLAVOR)) {
            log(R.string.import_could_not_open, str);
        }
        this.fileSelectMap.put(fileType, null);
        return findFileRaw;
    }

    /* access modifiers changed from: private */
    public void addMissingFileDialogs() {
        for (Entry entry : this.fileSelectMap.entrySet()) {
            if (entry.getValue() == null) {
                addFileSelectDialog((FileType) entry.getKey());
            }
        }
    }

    private void addFileSelectDialog(FileType fileType) {
        Pair fileDialogInfo = getFileDialogInfo(fileType);
        FileSelectLayout fileSelectLayout = new FileSelectLayout(this, getString(((Integer) fileDialogInfo.first).intValue()), fileType == FileType.CA_CERTIFICATE || fileType == FileType.CLIENT_CERTIFICATE, false);
        this.fileSelectMap.put(fileType, fileSelectLayout);
        fileSelectLayout.setLayoutParams(new LayoutParams(-1, -2));
        ((LinearLayout) findViewById(R.id.config_convert_root)).addView(fileSelectLayout, 2);
        findViewById(R.id.files_missing_hint).setVisibility(View.VISIBLE);
        if (VERSION.SDK_INT == 23) {
            checkPermission();
        }
        fileSelectLayout.setData((String) fileDialogInfo.second, this);
        fileSelectLayout.setCaller(this, getFileLayoutOffset(fileType), fileType);
    }

    @TargetApi(23)
    private void checkPermission() {
        if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            findViewById(R.id.permssion_hint).setVisibility(View.VISIBLE);
            findViewById(R.id.permssion_hint).setOnClickListener(this);
        }
    }

    private int getFileLayoutOffset(FileType fileType) {
        return fileType.getValue() + 1000;
    }

    private File findFileRaw(String str) {
        if (str == null || str.equals(BuildConfig.FLAVOR)) {
            return null;
        }
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File file = new File("/");
        HashSet hashSet = new HashSet();
        for (int size = this.mPathsegments.size() - 1; size >= 0; size--) {
            String str2 = BuildConfig.FLAVOR;
            for (int i = 0; i <= size; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(str2);
                sb.append("/");
                sb.append((String) this.mPathsegments.get(i));
                str2 = sb.toString();
            }
            if (str2.indexOf(58) != -1 && str2.lastIndexOf(47) > str2.indexOf(58)) {
                String substring = str2.substring(str2.indexOf(58) + 1, str2.length());
                try {
                    substring = URLDecoder.decode(substring, "UTF-8");
                } catch (UnsupportedEncodingException unused) {
                }
                hashSet.add(new File(externalStorageDirectory, substring.substring(0, substring.lastIndexOf(47))));
            }
            hashSet.add(new File(str2));
        }
        hashSet.add(externalStorageDirectory);
        hashSet.add(file);
        String[] split = str.split("/");
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            File file2 = (File) it.next();
            String str3 = BuildConfig.FLAVOR;
            int length = split.length - 1;
            while (true) {
                if (length >= 0) {
                    if (length == split.length - 1) {
                        str3 = split[length];
                    } else {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(split[length]);
                        sb2.append("/");
                        sb2.append(str3);
                        str3 = sb2.toString();
                    }
                    File file3 = new File(file2, str3);
                    if (file3.canRead()) {
                        return file3;
                    }
                    length--;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: 0000 */
    public String readFileContent(File file, boolean z) {
        String str;
        try {
            byte[] readBytesFromFile = readBytesFromFile(file);
            if (z) {
                str = Base64.encodeToString(readBytesFromFile, 0);
            } else {
                str = new String(readBytesFromFile);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(VpnProfile.DISPLAYNAME_TAG);
            sb.append(file.getName());
            sb.append(VpnProfile.INLINE_TAG);
            sb.append(str);
            return sb.toString();
        } catch (IOException e) {
            log(e.getLocalizedMessage());
            return null;
        }
    }

    private byte[] readBytesFromFile(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        long length = file.length();
        if (length <= VpnProfile.MAX_EMBED_FILE_SIZE) {
            byte[] bArr = new byte[((int) length)];
            int i = 0;
            while (i < bArr.length) {
                int read = fileInputStream.read(bArr, i, bArr.length - i);
                if (read < 0) {
                    break;
                }
                i += read;
            }
            fileInputStream.close();
            return bArr;
        }
        throw new IOException("File size of file to import too large.");
    }

    /* access modifiers changed from: 0000 */
    public void embedFiles(ConfigParser configParser) {
        if (this.mResult.mPKCS12Filename != null) {
            File findFileRaw = findFileRaw(this.mResult.mPKCS12Filename);
            if (findFileRaw != null) {
                this.mAliasName = findFileRaw.getName().replace(".p12", BuildConfig.FLAVOR);
            } else {
                this.mAliasName = "Imported PKCS12";
            }
        }
        VpnProfile vpnProfile = this.mResult;
        vpnProfile.mCaFilename = embedFile(vpnProfile.mCaFilename, FileType.CA_CERTIFICATE, false);
        VpnProfile vpnProfile2 = this.mResult;
        vpnProfile2.mClientCertFilename = embedFile(vpnProfile2.mClientCertFilename, FileType.CLIENT_CERTIFICATE, false);
        VpnProfile vpnProfile3 = this.mResult;
        vpnProfile3.mClientKeyFilename = embedFile(vpnProfile3.mClientKeyFilename, FileType.KEYFILE, false);
        VpnProfile vpnProfile4 = this.mResult;
        vpnProfile4.mTLSAuthFilename = embedFile(vpnProfile4.mTLSAuthFilename, FileType.TLS_AUTH_FILE, false);
        VpnProfile vpnProfile5 = this.mResult;
        vpnProfile5.mPKCS12Filename = embedFile(vpnProfile5.mPKCS12Filename, FileType.PKCS12, false);
        VpnProfile vpnProfile6 = this.mResult;
        vpnProfile6.mCrlFilename = embedFile(vpnProfile6.mCrlFilename, FileType.CRL_FILE, true);
        if (configParser != null) {
            this.mEmbeddedPwFile = configParser.getAuthUserPassFile();
            this.mEmbeddedPwFile = embedFile(configParser.getAuthUserPassFile(), FileType.USERPW_FILE, false);
        }
    }

    /* access modifiers changed from: private */
    public void updateFileSelectDialogs() {
        for (Entry entry : this.fileSelectMap.entrySet()) {
            ((FileSelectLayout) entry.getValue()).setData((String) getFileDialogInfo((FileType) entry.getKey()).second, this);
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.config_converter);
        ImageButton imageButton = (ImageButton) findViewById(R.id.fab_save);
        if (imageButton != null) {
            imageButton.setOnClickListener(this);
            findViewById(R.id.fab_footerspace).setVisibility(View.VISIBLE);
        }
        this.mLogLayout = (LinearLayout) findViewById(R.id.config_convert_root);
        this.mProfilename = (EditText) findViewById(R.id.profilename);
        this.mProfilenameLabel = (TextView) findViewById(R.id.profilename_label);
        if (bundle == null || !bundle.containsKey(VPNPROFILE)) {
            Intent intent = getIntent();
            if (intent != null) {
                doImportIntent(intent);
                setIntent(null);
            }
            return;
        }
        this.mResult = (VpnProfile) bundle.getSerializable(VPNPROFILE);
        this.mAliasName = bundle.getString("mAliasName");
        this.mEmbeddedPwFile = bundle.getString("pwfile");
        this.mSourceUri = (Uri) bundle.getParcelable("mSourceUri");
        this.mProfilename.setText(this.mResult.mName);
        if (bundle.containsKey("logentries")) {
            for (String log : bundle.getStringArray("logentries")) {
                log(log);
            }
        }
        if (bundle.containsKey("fileselects")) {
            for (int fileTypeByValue : bundle.getIntArray("fileselects")) {
                addFileSelectDialog(FileType.getFileTypeByValue(fileTypeByValue));
            }
        }
    }

    private void doImportIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            this.mSourceUri = data;
            doImportUri(data);
        }
    }

    private void doImportUri(Uri uri) {
        String str;
        log(R.string.importing_config, uri.toString());
        if ((uri.getScheme() == null || !uri.getScheme().equals("file")) && (uri.getLastPathSegment() == null || (!uri.getLastPathSegment().endsWith(".ovpn") && !uri.getLastPathSegment().endsWith(".conf")))) {
            str = null;
        } else {
            str = uri.getLastPathSegment();
            if (str.lastIndexOf(47) != -1) {
                str = str.substring(str.lastIndexOf(47) + 1);
            }
        }
        this.mPathsegments = uri.getPathSegments();
        Cursor query = getContentResolver().query(uri, null, null, null, null);
        if (query != null) {
            try {
                if (query.moveToFirst()) {
                    int columnIndex = query.getColumnIndex("_display_name");
                    if (columnIndex != -1) {
                        String string = query.getString(columnIndex);
                        if (string != null) {
                            str = string;
                        }
                    }
                    int columnIndex2 = query.getColumnIndex("mime_type");
                    if (columnIndex2 != -1) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Mime type: ");
                        sb.append(query.getString(columnIndex2));
                        log(sb.toString());
                    }
                }
            } catch (Throwable th) {
                if (query != null) {
                    query.close();
                }
                throw th;
            }
        }
        if (query != null) {
            query.close();
        }
        if (str != null) {
            str = str.replace(".ovpn", BuildConfig.FLAVOR).replace(".conf", BuildConfig.FLAVOR);
        }
        startImportTask(uri, str);
    }

    private void startImportTask(final Uri uri, final String str) {
        this.mImportTask = new AsyncTask<Void, Void, Integer>() {
            private ProgressBar mProgress;

            /* access modifiers changed from: protected */
            public void onPreExecute() {
                this.mProgress = new ProgressBar(ConfigConverter.this);
                ConfigConverter.this.addViewToLog(this.mProgress);
            }

            /* access modifiers changed from: protected */
            public Integer doInBackground(Void... voidArr) {
                try {
                    InputStream openInputStream = ConfigConverter.this.getContentResolver().openInputStream(uri);
                    ConfigConverter.this.doImport(openInputStream);
                    openInputStream.close();
                    if (ConfigConverter.this.mResult == null) {
                        return Integer.valueOf(-3);
                    }
                    return Integer.valueOf(0);
                } catch (IOException | SecurityException e) {
                    ConfigConverter configConverter = ConfigConverter.this;
                    StringBuilder sb = new StringBuilder();
                    sb.append("2131624164:");
                    sb.append(e.getLocalizedMessage());
                    configConverter.log(sb.toString());
                    if (VERSION.SDK_INT >= 23) {
                        ConfigConverter.this.checkMarschmallowFileImportError(uri);
                    }
                    return Integer.valueOf(-2);
                }
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Integer num) {
                ConfigConverter.this.mLogLayout.removeView(this.mProgress);
                ConfigConverter.this.addMissingFileDialogs();
                ConfigConverter.this.updateFileSelectDialogs();
                if (num.intValue() == 0) {
                    ConfigConverter.this.displayWarnings();
                    ConfigConverter.this.mResult.mName = ConfigConverter.this.getUniqueProfileName(str);
                    ConfigConverter.this.mProfilename.setVisibility(View.VISIBLE);
                    ConfigConverter.this.mProfilenameLabel.setVisibility(View.VISIBLE);
                    ConfigConverter.this.mProfilename.setText(ConfigConverter.this.mResult.getName());
                    ConfigConverter.this.log(R.string.import_done, new Object[0]);
                }
            }
        }.execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    @TargetApi(23)
    public void checkMarschmallowFileImportError(Uri uri) {
        if (!(checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED || uri == null || !"file".equals(uri.getScheme()))) {
            doRequestSDCardPermission(PERMISSION_REQUEST_READ_URL);
        }
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
    }

    /* access modifiers changed from: private */
    public void log(final String str) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView textView = new TextView(ConfigConverter.this);
                ConfigConverter.this.mLogEntries.add(str);
                textView.setText(str);
                ConfigConverter.this.addViewToLog(textView);
            }
        });
    }

    /* access modifiers changed from: private */
    public void addViewToLog(View view) {
        LinearLayout linearLayout = this.mLogLayout;
        linearLayout.addView(view, linearLayout.getChildCount() - 1);
    }

    /* access modifiers changed from: private */
    public void doImport(InputStream inputStream) {
        ConfigParser configParser = new ConfigParser();
        try {
            configParser.parseConfig(new InputStreamReader(inputStream));
            this.mResult = configParser.convertProfile();
            embedFiles(configParser);
        } catch (ConfigParseError | IOException e) {
            log(R.string.error_reading_config_file, new Object[0]);
            log(e.getLocalizedMessage());
            this.mResult = null;
        }
    }

    /* access modifiers changed from: private */
    public void displayWarnings() {
        if (this.mResult.mUseCustomConfig) {
            log(R.string.import_warning_custom_options, new Object[0]);
            String str = this.mResult.mCustomConfigOptions;
            if (str.startsWith("#")) {
                str = str.substring(str.indexOf(10) + 1);
            }
            log(str);
        }
        if (this.mResult.mAuthenticationType == 2 || this.mResult.mAuthenticationType == 7) {
            findViewById(R.id.importpkcs12).setVisibility(View.VISIBLE);
        }
    }

    /* access modifiers changed from: private */
    public void log(int i, Object... objArr) {
        log(getString(i, objArr));
    }
}
