package com.orbs.info;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.appupdate.AppUpdateManager;

public class AboutActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AboutActivity";

    static public final int APP_UPDATE_REQUEST_CODE = 1234;
    private AppUpdateManager appUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView versionName = findViewById(R.id.text_version);
        versionName.setText(BuildConfig.VERSION_NAME);

        TextView website = findViewById(R.id.text_website);
        website.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = ((TextView)v).getText().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        // Open source license
        LinearLayout layoutLicense = findViewById(R.id.layout_license);
        Button btnLicense = findViewById(R.id.button_license);
        btnLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutLicense.setVisibility(View.VISIBLE);
            }
        });

        Button closeLicense = findViewById(R.id.button_close_license);
        closeLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutLicense.setVisibility(View.GONE);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String title = "About";
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}