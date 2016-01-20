package me.heron.safefoodscanner;

import android.app.Application;

import com.parse.Parse;

public class ParseApplication extends Application{

    @Override
    public void onCreate() {
        parseSetup();
        super.onCreate();
    }

    private void parseSetup() {
        Parse.enableLocalDatastore(this);
        Parse.initialize(this);
    }

}
