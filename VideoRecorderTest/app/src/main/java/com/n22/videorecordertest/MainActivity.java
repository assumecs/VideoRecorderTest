package com.n22.videorecordertest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        findViewById(R.id.list_btn).setOnClickListener(this);
        findViewById(R.id.new_btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.list_btn:
                startActivity(new Intent(this, ListActivity.class));
                break;
            case R.id.new_btn:
                startActivity(new Intent(this, RecordActivity.class));
                break;
        }
    }
}

