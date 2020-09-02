package net.sharksystem.asap.android.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ASAPInitialExampleActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ASAPExampleApplication.initializeASAPExampleApplication(this);

        // launch real first activity
        this.finish();
        Intent intent = new Intent(this, ASAPExampleActivity.class);
        this.startActivity(intent);
    }
}
