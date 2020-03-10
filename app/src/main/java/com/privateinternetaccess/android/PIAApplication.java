/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;

import com.privateinternetaccess.android.model.exceptions.CustomExceptionHandler;
import com.privateinternetaccess.android.pia.PIABuilder;
import com.privateinternetaccess.android.pia.connection.ConnectionResponder;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.SubscriptionHandler;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.subscription.Base64DecoderException;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.PasswordObfuscation;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig;
import com.privateinternetaccess.android.receivers.OnAutoConnectNetworkReceiver;
import com.privateinternetaccess.android.receivers.OnNetworkChangeReceiver;
import com.privateinternetaccess.android.tunnel.PIANotifications;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Setups up {@link PIABuilder} and updates all the old variables and issues created along the years in {@link #updateOrResetValues()}
 *
 * 
 *
 * Created by half47 on 2/5/16.
 */
public class PIAApplication extends Application {

    public static final String HAS_RESET_ALLOWED_APPS = "hasResetAllowedApps3";
    public static final String UPDATE_DIALOG_VERSION = "update_dialog_version";
    public static final String HAS_RESET_MACE_GOOGLE = "hasResetMaceGoogle";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    PIACallbacks mCallBacks = new PIACallbacks() {
        @Override
        public boolean isKillSwitchEnabled(Context c) {
            SharedPreferences pref = c.getSharedPreferences(PiaPrefHandler.PREFNAME, MODE_MULTI_PROCESS);
            return pref.getBoolean("killswitch", false);
        }

        @Override
        public VpnProfile getAlwaysOnProfile() {
            return PIAApplication.this.getAlwaysOnProfile();
        }

        @Override
        public Class<? extends Activity> getMainClass() {
            if (isAndroidTV(getApplicationContext())) {
                return DashboardActivity.class;
            }

            return MainActivity.class;
        }
    };

    public static final String TAG = "PIAApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        CustomExceptionHandler crashHandler = new CustomExceptionHandler(getApplicationContext().getFilesDir().getAbsolutePath(),"");
        crashHandler.setDefaultUEH(Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(crashHandler);

        boolean notRelease = !BuildConfig.BUILD_TYPE.equals("release");
        boolean debugMode = PiaPrefHandler.getDebugMode(getApplicationContext());
        int debugLevel = PiaPrefHandler.getDebugLevel(getApplicationContext());
        VpnStatus.StateListener listener = ConnectionResponder.initConnection(getApplicationContext(), R.string.requestingportfw);

        PIABuilder.init(getApplicationContext())
                .enabledTileService()
                .createNotificationChannel(getString(R.string.pia_channel_name), getString(R.string.pia_channel_description))
                .initVPNLibrary(new PIANotifications(), mCallBacks, listener)
                .enableLogging(notRelease)
                .setDebugParameters(debugMode, debugLevel, getApplicationContext().getFilesDir());

        PIAServerHandler.startup(getApplicationContext());
        SubscriptionHandler.startup(getApplicationContext());

        // Registering the receiver since api 24+ won't call the receiver if in the manifest.
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        getApplicationContext().registerReceiver(new OnNetworkChangeReceiver(), intentFilter);
        getApplicationContext().registerReceiver(new OnAutoConnectNetworkReceiver(), intentFilter);

        updateOrResetValues();

        ThemeHandler.setAppTheme(this);

        //enable Calligraphy for fonts
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-RobotoRegular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

    }

    public VpnProfile getAlwaysOnProfile() {
        try {
            return PiaOvpnConfig.generateVpnProfile(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigParser.ConfigParseError configParseError) {
            configParseError.printStackTrace();
        }
        return null;
    }

    public void updateOrResetValues() {
        
        PRNGFixes.apply();
        //setup Dlog and VPN Cache
        Prefs prefs = new Prefs(getApplicationContext());
        // this resets the allowed apps issue once. From 1.3 - 1.4 issue
        boolean reset = prefs.getBoolean(HAS_RESET_ALLOWED_APPS);
        if (!reset) {
            DLog.w("ResetAllowedApps", "true");
            prefs.set(HAS_RESET_ALLOWED_APPS, true);
            prefs.set(PiaPrefHandler.VPN_PER_APP_ARE_ALLOWED, false); // reset toggle
            prefs.set(PiaPrefHandler.VPN_PER_APP_PACKAGES, new HashSet<String>()); // reset list
        }

        String currentCipher = prefs.get("cipher", "null");
        // Remove blowfish completely
        if (currentCipher.equals("BF-CBC")) { // 1.3.3.x
            prefs.set("cipher", "none");
        } else if (currentCipher.equals("null")) {
            prefs.set("cipher", "AES-128-CBC");
        }

        // Removes a duplicate password in 1.3 clients
        String password = prefs.getString("password");
        String obs_password = prefs.getString("obs_password");
        if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(obs_password)) {
            String deobs_password = null;
            try {
                deobs_password = PasswordObfuscation.deobfuscate(prefs.getString("password"));
            } catch (GeneralSecurityException | IOException | Base64DecoderException e) {
                e.printStackTrace();
            }
            if (!TextUtils.isEmpty(deobs_password) && deobs_password.equals(password)) {
                prefs.remove("password");
            }
        }

        if(BuildConfig.FLAVOR_store.equals("playstore") && !prefs.getBoolean(HAS_RESET_MACE_GOOGLE)){
            Prefs.with(getApplicationContext()).set(PiaPrefHandler.PIA_MACE, false);
            prefs.set(HAS_RESET_MACE_GOOGLE, true);
        }

        // Fixes a break in per app settings on 1.4 clients
        resetPerAppSettings(prefs);

        // We were saving lport as an integer, should just be a string
        // Fixes an issue on 1.3 clients
        try {
            String lport = prefs.get("lport", "auto");
        } catch (Exception e) {
            int port = prefs.get("lport", 0);
            if (port != 0) {
                prefs.set("lport", "" + port);
            }
        }
        prefs.set(PiaPrefHandler.PROXY_ORBOT, false);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Prefs.with(this).set(PiaPrefHandler.TRUST_WIFI, false);
        }
    }

    public void resetPerAppSettings(Prefs prefs) {
        //reset per app settings

        String resetKey = "resetPerAppSettings2";
        if(!prefs.get(resetKey, false)) {
            prefs.set(PiaPrefHandler.VPN_PER_APP_ARE_ALLOWED, false);
            Set<String> set = new HashSet<>();
            Prefs.with(getApplicationContext()).set(PiaPrefHandler.VPN_PER_APP_PACKAGES, set);
            prefs.set(resetKey, true);
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isPlayStoreSupported(PackageManager manager) {
        for (PackageInfo pkg : manager.getInstalledPackages(0)) {
            if (pkg.packageName.equals("com.android.vending"))
                return true;
        }
        return false;
    }

    public static boolean isRelease(){
        return BuildConfig.FLAVOR_pia.equals("production") && BuildConfig.BUILD_TYPE.equals("release");
    }

    public static boolean isQA() {
        return BuildConfig.FLAVOR_pia.equals("qa");
    }

    public static boolean isAndroidTV(Context context){
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        boolean androidTV = false;
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            androidTV = true;
        }
        return androidTV;
    }

    /**
     * Issue:
     * Switching themes more than once, will cause the activity stack to be lost and onBack will send the user to the home screen instead
     * of the main Activity.
     *
     * Fix:
     * This is only affecting amazon devices right now so fixing by this method.
     *
     * @return if Build.BRAND == amazon
     */
    public static boolean isAmazon(){
        String brand = Build.BRAND;
        DLog.d("isAmazon"," " + Build.MODEL + " " + Build.DEVICE + " " + Build.BRAND + " " + Build.PRODUCT + " " + Build.BOARD);
        if(!TextUtils.isEmpty(brand))
            return brand.toLowerCase().contains("amazon");
        else
            return false;
    }

    public static boolean isChromebook(Context context){
        return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
    }

    public static String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        if (infos != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : infos) {
                if (processInfo.pid == pid) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }
}