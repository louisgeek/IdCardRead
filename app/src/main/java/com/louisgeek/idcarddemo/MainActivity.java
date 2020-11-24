package com.louisgeek.idcarddemo;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.louisgeek.idcard.IdCardInfo;
import com.louisgeek.idcard.IdCardManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private IdCardManager mIdCardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIdCardManager = new IdCardManager();
        mIdCardManager.init(this);

        TextView id_tv = findViewById(R.id.id_tv);
        ImageView id_iv = findViewById(R.id.id_iv);
        id_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IdCardInfo idCardInfo = mIdCardManager.readIdCard();
                id_tv.setText(new Gson().toJson(idCardInfo));
                //
                id_iv.setImageBitmap(idCardInfo.photo);
            }
        });

    }


}