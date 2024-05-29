package com.AcouListener.pytorchandroid;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;

public class Result extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        获取从上一个活动传递过来的照片数据，并将其转换为Bitmap对象。
        Bitmap imageBitmap = (Bitmap) getIntent().getParcelableExtra("imagedata");

        String pred = getIntent().getStringExtra("pred");  //获取"pred"

        ImageView imageView = findViewById(R.id.image);
        imageView.setImageBitmap(imageBitmap);

        TextView textView = findViewById(R.id.label);
        textView.setText(pred);

    }

}
