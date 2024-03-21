package com.example.sanqiao.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import com.example.sanqiao.R;
import com.example.sanqiao.webapi.Web_Api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class demoActivity extends AppCompatActivity {

    private TextView demo_text;
    public static String filepath;;
    private static Set<String > list=new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        filepath=getSaveFilePath("test");


        demo_text = findViewById(R.id.demo_text);
        findViewById(R.id.demo_bnt2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        findViewById(R.id.demo_bnt).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {


                /*获取资源文件*/
                InputStream is = getResources().openRawResource(R.raw.ideal);
                // 存储测试音频到指定路径
                File outFile = new File(getSaveFilePath("test"));
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(outFile);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                // 读取输入流并写入输出流
                byte[] buffer = new byte[1024];
                int read;
                try {
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }catch (Exception e){
                    throw new RuntimeException(e);
                }


                // 关闭流
                try {
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }




            }
        });
    }




    /*
     * 参数：无
     * 返回值：录音文件路径
     * */
    private String getSaveFilePath(String filename) {

        File file = new File( getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Audio");
        if (!file.exists()) {
            file.mkdirs();
        }
        File wavFile = new File(file, filename+".wav");
        return wavFile.getAbsolutePath();

    }
}