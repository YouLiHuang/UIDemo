package com.example.sanqiao;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sanqiao.util.CommonUtils;
import com.example.sanqiao.util.LvDataAdapter2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class localQueryActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView btn_vback;
    private ImageButton bt_material, bt_diameter, bt_method;
    private EditText et_material, et_diameter, et_method;
    private View rl_material, rl_diameter, rl_method;
    private String s_material, s_diameter, s_method;
    private Handler mainHandler ;     // 主线程
    private final String DATABASE_NAME = "SQLite.db";
    private SQLiteDatabase db;
    private final PopupWindow[] popupWindows = new PopupWindow[3]; // 存储三个 PopupWindow

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_local_query);
        initView();

        // 检查数据库文件是否存在，如果不存在则从 assets 目录中复制
        boolean isDbExist = checkDatabase();
        if(!isDbExist) {
            copyDatabase();
        }

        LvDataAdapter2 adapter = new LvDataAdapter2(localQueryActivity.this, getMaterials(), et_material);
        ListView lv_materials = new ListView(localQueryActivity.this);
        lv_materials.setAdapter(adapter);
        bt_material.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow(0, lv_materials, rl_material);
            }
        });

        LvDataAdapter2 adapter2 = new LvDataAdapter2(localQueryActivity.this, getDiameters(), et_diameter);
        ListView lv_diameters = new ListView(localQueryActivity.this);
        lv_diameters.setAdapter(adapter2);
        bt_diameter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPopupWindow(1, lv_diameters, rl_diameter);
            }
        });

        LvDataAdapter2 adapter3 = new LvDataAdapter2(localQueryActivity.this, getMethods(), et_method);
        ListView lv_methods = new ListView(localQueryActivity.this);
        lv_methods.setAdapter(adapter3);
        bt_method.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPopupWindow(2, lv_methods, rl_method);
            }
        });
    }

    private void initView(){
        rl_material = findViewById(R.id.rl_material);
        bt_material = (ImageButton)findViewById(R.id.bt_material);
        et_material = (EditText)findViewById(R.id.et_material);

        rl_diameter = findViewById(R.id.rl_diameter);
        bt_diameter = (ImageButton)findViewById(R.id.bt_diameter);
        et_diameter = (EditText)findViewById(R.id.et_diameter);

        rl_method = findViewById(R.id.rl_method);
        bt_method = (ImageButton)findViewById(R.id.bt_method);
        et_method = (EditText)findViewById(R.id.et_method);

        // 查询按钮
        Button btn_query = findViewById(R.id.btn_query);
        btn_query.setOnClickListener(this);
        mainHandler = new Handler(getMainLooper());   // 获取主线程

        btn_vback = findViewById(R.id.btn_vback2);
        btn_vback.setOnClickListener(this);
    }

    private boolean checkDatabase() {
        File file = new File(getDatabasePath(DATABASE_NAME).getPath());
        return file.exists();
    }

    private void copyDatabase() {
        try {
            InputStream inputStream = getAssets().open(DATABASE_NAME);
            String outFileName = getDatabasePath(DATABASE_NAME).getPath();
            OutputStream outputStream = Files.newOutputStream(Paths.get(outFileName));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException ignored) {
        }
    }

    private List<String> getMaterials() {
        List<String> materials = new ArrayList<>();
        materials.add("Al-Si-5");
        materials.add("Al-Mg-5");
        materials.add("Al-99.5");
        materials.add("Steel");
        materials.add("Cr-Ni-199");
        materials.add("Cu-Si-3");
        return materials;
    }

    private List<String> getDiameters() {
        List<String> diameters = new ArrayList<>();
        diameters.add("0.8");
        diameters.add("1");
        diameters.add("1.2");
        diameters.add("1.6");
        return diameters;
    }

    private List<String> getMethods() {
        List<String> methods = new ArrayList<>();
        methods.add("MIG");
        methods.add("GMAW");
        return methods;
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btn_query) {
            doQuery();
        } else if (v.getId() == R.id.btn_vback2) {
            finish();//关闭页面
        }
    }

    private void doQuery(){
        s_material = et_material.getText().toString().trim();
        s_diameter = et_diameter.getText().toString().trim();
        s_method = et_method.getText().toString().trim();
        if(TextUtils.isEmpty(s_material)){
            CommonUtils.showShortMsg(this, "请输入焊接材料");
            et_material.requestFocus();
        }else if(TextUtils.isEmpty(s_diameter)){
            CommonUtils.showShortMsg(this, "请输入焊接直径");
            et_diameter.requestFocus();
        }else if(TextUtils.isEmpty(s_method)){
            CommonUtils.showShortMsg(this, "请输入焊接方法");
            et_method.requestFocus();
        }else{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            db = SQLiteDatabase.openDatabase(getDatabasePath(DATABASE_NAME).getPath(), null, SQLiteDatabase.OPEN_READWRITE);
                            String selection = "WeldingMaterial = ? and WireDiameter = ? and WeldingMethod = ?"; // 查询条件
                            String[] selectionArgs = {s_material, s_diameter, s_method};
                            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM paramsdatatable WHERE " + selection, selectionArgs);
                            int count = 0;
                            if (cursor.moveToFirst()) {
                                count = cursor.getInt(0);
                            }
                            cursor.close();
                            db.close();

                            if(count != 0){
                                Intent intent = new Intent(localQueryActivity.this, query2Activity.class);
                                Bundle bundle = new Bundle(); // 创建一个新包裹
                                bundle.putString("material", s_material);//将参数传递给下一个页面
                                bundle.putString("diameter", s_diameter);
                                bundle.putString("method", s_method);
                                intent.putExtras(bundle); // 把快递包裹塞给意图
                                startActivity(intent); // 跳转到意图指定的活动页面
                            } else {
                                CommonUtils.showLonMsg(localQueryActivity.this, "查询失败");
                            }
                        }
                    });
                }
            }).start();
        }
    }

    private void showPopupWindow(int index, ListView listView, View anchorView) {
        if (popupWindows[index] == null) {
            popupWindows[index] = new PopupWindow(listView, anchorView.getWidth(), listView.getCount() * anchorView.getHeight());
            popupWindows[index].showAsDropDown(anchorView);
            popupWindows[index].setFocusable(true);
            popupWindows[index].setOutsideTouchable(true);
            popupWindows[index].setBackgroundDrawable(new BitmapDrawable());
        } else {
            if (popupWindows[index].isShowing()) {
                popupWindows[index].dismiss();
            } else {
                popupWindows[index].showAsDropDown(anchorView);
            }
        }
    }
}