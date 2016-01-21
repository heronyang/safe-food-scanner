package me.heron.safefoodscanner;

import android.app.Application;

import com.parse.Parse;

import me.heron.safefoodscanner.Parse.ParseAPIAdaptor;

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
