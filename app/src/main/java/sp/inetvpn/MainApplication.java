package sp.inetvpn;

import android.app.Activity;
import android.content.Context;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class MainApplication extends app.openconnect.Application implements Configuration.Provider {
    public static MainApplication application;
    public static String device_id;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        application = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

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
}

