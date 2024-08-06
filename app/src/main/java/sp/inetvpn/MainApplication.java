package sp.inetvpn;

import static sp.inetvpn.handler.AsyncIPFetcher.getIPAddress;

import android.app.Activity;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

import sp.inetvpn.data.GlobalData;

public class MainApplication extends sp.vpn.module.VpnApplication implements Configuration.Provider {
    public static MainApplication application;
    public static String device_id;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        application = this;
    }

    @Override
    protected String getContentTitle() {
        return "Hamrah";
    }

    @Override
    protected @NotNull String getChannelID() {
        return "sp.inetvpn";
    }

    @Override
    protected @NotNull String getChannelIDName() {
        return "spinetvpn";
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            GlobalData.ApiAdress = getIPAddress();
        } catch (InterruptedException | ExecutionException e) {
            Log.d("MRBT! ", "ERR");
            throw new RuntimeException(e);
        }

        if (BuildConfig.DEBUG)
            StrictMode.enableDefaults();

        sp.inetvpn.setup.MainApplication setup = new sp.inetvpn.setup.MainApplication(this);
        setup.setupClass();
    }

    // work 1.8.1 manager
    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setDefaultProcessName(BuildConfig.APPLICATION_ID + ":bg").build();
    }

    @NonNull
    @Override
    protected String angPackage() {
        return BuildConfig.APPLICATION_ID;
    }
}

