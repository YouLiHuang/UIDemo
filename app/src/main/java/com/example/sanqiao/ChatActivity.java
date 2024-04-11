package com.example.sanqiao;

import static com.example.sanqiao.util.queryUtil.queryRequest;
import static com.example.sanqiao.util.queryUtil.readJsonFile;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sanqiao.chat.ChatAdapter;
import com.example.sanqiao.chat.Message;
import com.example.sanqiao.chat.chatFragment;
import com.example.sanqiao.chat.keyboardFragment;
import com.example.sanqiao.util.CirclePgBar;
import com.example.sanqiao.util.CommonUtils;
import com.example.sanqiao.util.FileUtil;
import com.example.sanqiao.util.HttpRequest;
import com.example.sanqiao.util.Weldinginfo;
import com.example.sanqiao.util.oneGroupInfo;
import com.example.sanqiao.util.queryUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatActivity extends AppCompatActivity implements chatFragment.convert_finisher_listener, chatFragment.long_press_listener, chatFragment.pgBar_listener, CirclePgBar.timeout_listener {

    private Handler mainHandler = new Handler(Looper.getMainLooper());   // 主线程
    private Thread networkThread;
    private String id = null;
    final String url = "http://61.142.130.81:8001/api/session/";
    private RelativeLayout main_layout;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messages;
    private Button sendButton;
    private EditText edit_text_input;
    static private String record_file_path;
    static private String parse_file_path;
    static private String WebResponsePath;
    static private String id_file_path;
    static private String query_file_path;
    static private String filename; //保存文件时的文件名
    private int AUDIO_CODE = 101;
    final private int VIBRATE_CODE = 102;
    static private String response;
    private static String machine_id;
    private CirclePgBar pgBar;
    private boolean pgBar_pause = false;

    public interface pgBar_value_listener {
        void onValueChange(int value);
    }

    pgBar_value_listener pgBar_value_listener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        /*机器型号*/
        machine_id = Build.MODEL;
        /*解析结果文件路径*/
        parse_file_path = file_init("parse.json");
        /*服务器响应文件路径*/
        WebResponsePath = file_init("WebResponsePath.json");
        /*id文件路径*/
        id_file_path = file_init("session_id.json");
        /*查询文件路径*/
        query_file_path = file_init("query.json");
        /*录音文件路径*/
        record_file_path = getSaveFilePath("ideal");
        /*向键盘碎片传递路径*/
        keyboardFragment keyboard = new keyboardFragment();
        Bundle bundle = new Bundle();
        bundle.putString("recordPath", record_file_path);
        bundle.putString("parsePath", parse_file_path);
        bundle.putString("WebResponsePath", WebResponsePath);
        keyboard.setArguments(bundle);
        /*检查权限*/
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 如果权限未被授予，向用户请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_CODE);
        }
        /*获取recyclerView视图对象*/
        recyclerView = findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));//设置为垂直方向
        /*消息列表*/
        messages = new ArrayList<>();
        // 添加一些消息到列表中，测试用
        messages.add(new Message("欢迎使用三乔智能数据库！请你提供想要查询的问题？示例：我要焊接板厚为0.1的钢材，请告诉我焊接参数有哪些？", Message.TYPE_RECEIVED)); //接收到的消息
        adapter = new ChatAdapter(messages);//创建一个适配器
        recyclerView.setAdapter(adapter);//给recyclerView视图对象设置适配器
        recyclerView.scrollToPosition(messages.size() - 1);//视图滚到最后一行

        /*进度条*/
        pgBar = findViewById(R.id.pgBar);
        pgBar.setVisibility(View.GONE);//一开始隐藏进度条
        /*加载键盘输入布局*/
        getSupportFragmentManager().beginTransaction().replace(R.id.chat_fragment, keyboard).commit();
        /*隐藏系统顶部UI*/
        Hide_SystemUI();
        /*主布局，设置监听*/
        main_layout = findViewById(R.id.chat_activity_main_layout);
        Set_Listener();


    }

    /*焦点变更时收起键盘*/
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Hide_SystemUI();
        View v = getCurrentFocus();//获取焦点的View，当前用户正在与之交互的View
        if (v != null && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_MOVE) && v instanceof EditText) {//抬起或移动事件，且判断当前焦点是否为EditText
            int[] sourceLocation = new int[2];//存储View在屏幕上的位置
            v.getLocationOnScreen(sourceLocation);//数组的第一个元素是视图左上角的 X 坐标，第二个元素是视图左上角的 Y 坐标（视图原点）

            float x = event.getRawX() + v.getLeft() - sourceLocation[0];//计算触摸事件的X坐标相对于EditText左上角的偏移量（手指坐标+视图 v 左边缘的偏移量-视图左上角的 X 坐标）
            float y = event.getRawY() + v.getTop() - sourceLocation[1];

            if (x < v.getLeft() || x >= v.getRight() || y < v.getTop() || y > v.getBottom()) {//检查触摸事件是否发生在EditText之外
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
        return super.dispatchTouchEvent(event);
    }


    private void Set_Listener() {

        findViewById(R.id.chat_clear).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                id = null;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int count = messages.size();
                        messages.clear();//清空message
                        // 通知 Adapter 数据已经改变
                        adapter.notifyItemRangeChanged(0, count);
                    }
                });

                return false;
            }
        });
        findViewById(R.id.top_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();//关闭页面
            }
        });
        /*发送按钮的监听*/
        sendButton = findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText messageEditText = findViewById(R.id.chat_edittext); // 消息输入框
                String messageText = messageEditText.getText().toString();//获取输入的信息
                // 创建一个新的 Message 对象
                Message newMessage = new Message(messageText, Message.TYPE_SENT);//true false决定 发送/接受
                // 将新消息添加到消息列表中
                messages.add(newMessage);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 通知 Adapter 数据已经改变
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                        // 清空消息输入框
                        messageEditText.setText("");
                    }
                });

                /*文本查询*/
                Query(messageText);


            }
        });

    }

    private String getFilename(String response) {
        // 使用正则表达式提取关键信息
        String pattern = "焊接方法是(.*?)，.*?焊接材料是(.*?)，.*?焊接厚度是(.*?)。";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);
        String material = "";
        String thickness = "";
        String method = "";
        if (matcher.find()) {
            method = matcher.group(1);
            material = matcher.group(2);
            thickness = matcher.group(3);
        }
        return material + "_" + thickness + "_" + method + ".db";
    }

    private String file_init(String filename) {
        String filepath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + filename;
        File parsefile = new File(filepath);//创建js文件
        if (!parsefile.exists()) {
            try {
                parsefile.createNewFile();
                JSONObject json = new JSONObject();
                json.put("response", "");
                FileUtil.saveText(filepath, json.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            FileWriter fw = null;
            /*存在就清空文件*/
            try {
                fw = new FileWriter(parsefile);
                fw.write("");//文件存在则清空文件
                fw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return filepath;
    }

    /*
     * 参数：无
     * 返回值：录音文件路径
     * */
    private String getSaveFilePath(String filename) {

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Audio");
        if (!file.exists()) {
            file.mkdirs();
        }
        File wavFile = new File(file, filename + ".wav");
        return wavFile.getAbsolutePath();

    }

    /*进度条动作监听*/
    @Override
    public void onTimeout() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                pgBar.setVisibility(View.GONE);
                CommonUtils.showShortMsg(ChatActivity.this, "录制超时！");
            }
        });


    }

    @Override
    public void onPgBar_process(boolean on_off) {

        if (on_off == true)//开启进度条
        {
            pgBar_pause = false;
            /*打开进度条*/
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    pgBar.setVisibility(View.VISIBLE);
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true && !pgBar_pause) {
                        int value = pgBar.get_mProgress();
                        if (value < 60) {
                            pgBar.set_mProgress(value + 1);//修改进度值
                        } else {
                            /*超时*/
                            pgBar_pause = true;
                            CommonUtils.showShortMsg(ChatActivity.this, "录音超时！");
                            pgBar.set_mProgress(0);//修改进度值
                            //关闭进度条
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    pgBar.setVisibility(View.GONE);
                                }
                            });

                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }).start();
        } else {
            pgBar_pause = true;
            //关闭进度条
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    pgBar.set_mProgress(0);//修改进度值
                    pgBar.setVisibility(View.GONE);
                }
            });
        }
    }

    /*长按监听*/
    @Override
    public void onLongPress() {
        if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            // 如果权限未被授予，向用户请求权限
            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.VIBRATE}, VIBRATE_CODE);
        } else {
            // 获取系统的振动服务?
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(100);// 振动 100 毫秒
        }
    }

    /*语音转换完成监听*/
    @Override
    public void onCompleted() {

        String final_response = null;
        String final_time = null;
        response = readJsonFile(parse_file_path);//读取post得到的json文件转为字符串;
        try {
            JSONObject obj = new JSONObject(response);
            final_response = obj.getString("response");
            final_time = obj.getString("time");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 创建一个新的 Message 对象
        Message responseMessage = new Message("识别结果：" + final_response, Message.TYPE_SENT);//回显解析结果
        //Message timeMessage = new Message("耗时："+final_time, Message.TYPE_RECEIVED);//回显解析结果
        // 将新消息添加到消息列表中
        messages.add(responseMessage);
        //messages.add(timeMessage);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                // 通知 Adapter 数据已经改变
                adapter.notifyItemInserted(messages.size() - 1);
                recyclerView.scrollToPosition(messages.size() - 1);
            }
        });

        /*转换完成，向服务器请求*/
        Query(final_response);
    }


    private void Hide_SystemUI() {
        View view = getWindow().getDecorView();
        WindowInsetsController insetsController = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController = view.getWindowInsetsController();
        }
        if (insetsController != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController.hide(WindowInsets.Type.statusBars());
            }
        }

    }

    public void Query(String query) {
        /*开始查询*/
        networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (id == null) query_idNull(query);
                    else query_withId(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        networkThread.start();//开启线程
    }


    /*id为空的查询方法，参数：查询语句/语音识别结果*/
    private void query_idNull(String query) {
        /*----------------------------准备工作，查询id----------------------------*/
        try {
            FileWriter fw = new FileWriter(id_file_path);
            fw.write("");
            fw.flush();
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        /*发起空的post请求*/
        File post_output = null;
        post_output = new File(id_file_path);//输出流，解析得到的查询命令
        /*设置请求*/
        try {
            HttpRequest request = new HttpRequest("http://61.142.130.81:8001/api/session/start", "POST");
            request.connectTimeout(10000);//10s超时
            request.acceptJson();//设置header
            request.contentType("application/json");//设置contentType
            request.receive(post_output);//上传查询请求，获取响应并拷贝到本地文件
        } catch (RuntimeException e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Message responseMessage = new Message("网络超时！", Message.TYPE_RECEIVED);
                    messages.add(responseMessage);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    // 通知 Adapter 数据已经改变
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });//网络超时
        }

        /*解析id*/
        String session_id = readJsonFile(id_file_path);//读取id的json文件
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(session_id);
        } catch (JSONException e) {
            e.printStackTrace();//js报错
        }
        Iterator<String> keys = jsonObject.keys();
        String keyname = String.valueOf(keys.next());
        id = jsonObject.optString(keyname);

        /*获取id成功，并构建新的url进行查新*/
        if (id != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(url).append(id).append("/?query=").append(query);/*构建新的url*/

            /*修改查询文件，随后发起新的请求*/
            try {
                JSONObject query_js = new JSONObject();
                query_js.put("query", response);
                FileWriter fw = new FileWriter(query_file_path);
                fw.write("");//清空
                fw.flush();
                fw.write(query_js.toString());//修改
                fw.flush();
                fw.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            /*发起查询*/
            try {
                queryRequest(query_file_path, WebResponsePath, sb.toString());//post查询
            } catch (RuntimeException e) {//超时
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Message responseMessage = new Message("网络超时！", Message.TYPE_RECEIVED);
                        messages.add(responseMessage);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        // 通知 Adapter 数据已经改变
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });//网络超时
            }

            /*解析查询结果*/
            String response_str = null;//清空字符串
            response_str = readJsonFile(WebResponsePath);//读取post得到的json文件转为字符串
            /*查询结果为空*/
            if (response_str == null)
            {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Message responseMessage = new Message("查询失败！", Message.TYPE_RECEIVED);
                        messages.add(responseMessage);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        // 通知 Adapter 数据已经改变
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });
            }
            else
            {
                JSONObject jsonObject_data = null;
                try {
                    JSONObject jsonObject2 = new JSONObject(response_str);//创建响应文件json对象
                    //尝试获取json下response节点下的data节点，并进行数据解析
                    try {
                        jsonObject_data = jsonObject2.optJSONObject("response").optJSONObject("data");
                        /*data存在且非空*/
                        if (jsonObject_data != null && jsonObject_data.length() != 0)
                        {
                            Iterator<String> response_keys = jsonObject2.optJSONObject("response").keys();//取response节点的键值对
                            String keyName = String.valueOf(response_keys.next());//取键名
                            String response = jsonObject2.optJSONObject("response").optString(keyName);//取值
                            //filename = getFilename(response);
                            /*显示对话*/
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Message responseMessage = new Message(response, Message.TYPE_RECEIVED);
                                    messages.add(responseMessage);
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    // 通知 Adapter 数据已经改变
                                    adapter.notifyItemInserted(messages.size() - 1);
                                    recyclerView.scrollToPosition(messages.size() - 1);
                                }
                            });

                            List<Weldinginfo> weldinginfoList = queryUtil.parseJson(response_str);//将str转换为参数列表
                            //String result = queryUtil.getResult(weldinginfoList);
                            List_to_String(weldinginfoList);
                        }
                        else {
                            /*显示对话*/
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Message responseMessage = new Message("抱歉，数据库暂无此类数据", Message.TYPE_RECEIVED);
                                    messages.add(responseMessage);
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    // 通知 Adapter 数据已经改变
                                    adapter.notifyItemInserted(messages.size() - 1);
                                    recyclerView.scrollToPosition(messages.size() - 1);
                                    id=null;
                                }
                            });
                        }//数据库无数据
                    }
                    catch (Exception e) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Iterator<String> keys2 = jsonObject2.keys();//取键,此时文件节点下仅有一个键值对
                                String keyname2 = String.valueOf(keys2.next());//取键名
                                String response = jsonObject2.optString(keyname2);//取值
                                Message responseMessage = new Message(response, Message.TYPE_RECEIVED);
                                messages.add(responseMessage);
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                // 通知 Adapter 数据已经改变
                                adapter.notifyItemInserted(messages.size() - 1);
                                recyclerView.scrollToPosition(messages.size() - 1);
                            }
                        });
                    }/*没有data节点*/
                }
                catch (Exception e) {
                    /*js报错*/
                    e.printStackTrace();
                }

            }

        }
        /*获取id 失败*/
        else
        {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Message responseMessage = new Message("获取id失败！", Message.TYPE_RECEIVED);
                    messages.add(responseMessage);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    // 通知 Adapter 数据已经改变
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });
        }

    }

    private void query_withId(String query) {

        StringBuilder sb = new StringBuilder();
        sb.append(url).append(id).append("/?query=").append(query);/*构建新的url*/

        try {
            /*修改查询文件*/
            JSONObject query_js = new JSONObject();
            query_js.put("query", query);
            FileWriter fw = new FileWriter(query_file_path);
            fw.write("");//清空
            fw.flush();
            fw.write(query_js.toString());//修改
            fw.flush();
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        /*发起查询*/
        try {
            queryRequest(query_file_path, WebResponsePath, sb.toString());//post查询
        } catch (RuntimeException e) {//超时
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Message responseMessage = new Message("网络超时！", Message.TYPE_RECEIVED);
                    messages.add(responseMessage);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    // 通知 Adapter 数据已经改变
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });//网络超时
        }

        /*解析查询结果*/
        String response_str = null;//清空字符串
        response_str = readJsonFile(WebResponsePath);//读取post得到的json文件转为字符串
        /*查询结果为空*/
        if (response_str == null)
        {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Message responseMessage = new Message("查询失败！", Message.TYPE_RECEIVED);
                    messages.add(responseMessage);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    // 通知 Adapter 数据已经改变
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });
        }
        else
        {
            JSONObject jsonObject_data = null;
            try {
                JSONObject jsonObject2 = new JSONObject(response_str);//创建响应文件json对象
                //尝试获取json下response节点下的data节点，并进行数据解析
                try {
                    jsonObject_data = jsonObject2.optJSONObject("response").optJSONObject("data");
                    /*data存在且非空*/
                    if (jsonObject_data != null && jsonObject_data.length() != 0)
                    {
                        Iterator<String> response_keys = jsonObject2.optJSONObject("response").keys();//取response节点的键值对
                        String keyName = String.valueOf(response_keys.next());//取键名
                        String response = jsonObject2.optJSONObject("response").optString(keyName);//取值
                        //filename = getFilename(response);
                        /*显示对话*/
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Message responseMessage = new Message(response, Message.TYPE_RECEIVED);
                                messages.add(responseMessage);
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                // 通知 Adapter 数据已经改变
                                adapter.notifyItemInserted(messages.size() - 1);
                                recyclerView.scrollToPosition(messages.size() - 1);
                            }
                        });

                        List<Weldinginfo> weldinginfoList = queryUtil.parseJson(response_str);//将str转换为参数列表
                        //String result = queryUtil.getResult(weldinginfoList);
                        List_to_String(weldinginfoList);
                    }
                    else {
                        /*显示对话*/
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Message responseMessage = new Message("抱歉，数据库暂无此类数据", Message.TYPE_RECEIVED);
                                messages.add(responseMessage);
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                // 通知 Adapter 数据已经改变
                                adapter.notifyItemInserted(messages.size() - 1);
                                recyclerView.scrollToPosition(messages.size() - 1);
                                id=null;
                            }
                        });
                    }//数据库无数据
                }
                catch (Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Iterator<String> keys2 = jsonObject2.keys();//取键,此时文件节点下仅有一个键值对
                            String keyname2 = String.valueOf(keys2.next());//取键名
                            String response = jsonObject2.optString(keyname2);//取值
                            Message responseMessage = new Message(response, Message.TYPE_RECEIVED);
                            messages.add(responseMessage);
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            // 通知 Adapter 数据已经改变
                            adapter.notifyItemInserted(messages.size() - 1);
                            recyclerView.scrollToPosition(messages.size() - 1);
                        }
                    });
                }/*没有data节点*/
            }
            catch (Exception e) {
                /*js报错*/
                e.printStackTrace();
            }

        }


    }

    //将list转换为字符
    private void List_to_String(List<Weldinginfo> wList) {
        for (Weldinginfo witem : wList) {
            StringBuilder stringBuilder = new StringBuilder();
            String wireDiameter = witem.getWireDiameter();
            stringBuilder.append(wireDiameter).append("\n");

            List<oneGroupInfo> groupInfoList = witem.getWeldingList();

            int maxParamNameLength = 0;
            for (oneGroupInfo item : groupInfoList) {
                String paramName = item.getParamName();
                maxParamNameLength = Math.max(maxParamNameLength, paramName.length());
            }

            // 拼接参数信息
            for (int i = 0; i < groupInfoList.size(); i++) {
                oneGroupInfo item = groupInfoList.get(i);
                String paramName = item.getParamName();
                String paramValue = item.getParamValue();

                stringBuilder.append(paramName);
                // 添加空格，以保证参数值左对齐
                int addTabs = maxParamNameLength - paramName.length() + 4;
                for (int j = 0; j < addTabs; j++) {
                    stringBuilder.append(" ");
                }

                stringBuilder.append(paramValue);
                // 如果不是最后一行，则添加换行符
                if (i < groupInfoList.size() - 1) {
                    stringBuilder.append("\n");
                }
            }

            String result = stringBuilder.toString();
            /*显示对话*/
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Message responseMessage = new Message(result, Message.TYPE_RECEIVED);
                    messages.add(responseMessage);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    // 通知 Adapter 数据已经改变
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });
        }
    }

}