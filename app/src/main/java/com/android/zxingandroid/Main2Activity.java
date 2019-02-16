package com.android.zxingandroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
////        //弹窗占满状态得高度
//     //  attributes.flags= WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
////        attributes.type= ;
////        int matchParent = WindowManager.LayoutParams.MATCH_PARENT;
////        attributes.width = matchParent-50 ;
////        attributes.height = WindowManager.LayoutParams.MATCH_PARENT;
////
 // attributes.dimAmount = 0.2f;
  attributes.alpha=0.985f;
       attributes.gravity = Gravity.RIGHT;



   window.setAttributes(attributes);


    }
}
