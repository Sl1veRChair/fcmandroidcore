package me.carda.awesome_notifications_fcm.core;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.ArrayList;

import me.carda.awesome_notifications_fcm.core.licenses.LicenseManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            LicenseManager
                    .getInstance()
                    .isLicenseKeyValid(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}