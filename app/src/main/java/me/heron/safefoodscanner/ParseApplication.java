package me.heron.safefoodscanner;

import android.app.Application;

import com.parse.Parse;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class ParseApplication extends Application{

    @Override
    public void onCreate() {
        parseSetup();
        super.onCreate();
        Fabric.with(this, new Crashlytics());
    }

    private void parseSetup() {
        Parse.enableLocalDatastore(this);
        Parse.initialize(this);
    }

}
