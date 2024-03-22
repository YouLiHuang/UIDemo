package com.example.sanqiao.chat;


import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.sanqiao.ChatActivity;
import com.example.sanqiao.R;
import com.example.sanqiao.record.IdealRecorder;
import com.example.sanqiao.record.StatusListener;
import com.example.sanqiao.webapi.Web_Api;


import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link chatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class chatFragment extends Fragment implements Web_Api.covert_finished_listener{

    /*Wbi_Api listener接口实现*/
    @Override
    public void onTaskCompleted() {
        listener.onCompleted();
    }
    final String url = "http://61.142.130.81:8001/api/session/";
    private IdealRecorder idealRecorder;
    private IdealRecorder.RecordConfig recordConfig;
    static public Set<String> data_list=new HashSet<>();
    static private Web_Api Api;
    public interface convert_finisher_listener{
        void onCompleted();
    }
    private convert_finisher_listener listener;

    public interface long_press_listener{
        void onLongPress();
    }
    long_press_listener long_press_listener;

    public interface pgBar_listener{
        void onPgBar_process(boolean on_off);
    }
    pgBar_listener pgBar_listener;



    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener=(convert_finisher_listener) context;//转换完成监听接口
        long_press_listener=(long_press_listener) context;//长按完成监听（接口）
        pgBar_listener=(pgBar_listener) context;//进度条处理监听（接口）
    }


    private StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {

        }
        @Override
        public void onRecordData(short[] data, int length) {

        }
        @Override
        public void onVoiceVolume(int volume) {
            double myVolume = (volume - 40) * 4;
        }
        @Override
        public void onRecordError(int code, String errorMsg) {
        }
        @Override
        public void onFileSaveFailed(String error) {
        }
        @Override
        public void onFileSaveSuccess(String fileUri) {
        }
        @Override
        public void onStopRecording() {
        }
    };


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    static private String recordPath;
    static private String WebResponsePath;
    static private String parsePath;

    public chatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment chatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static chatFragment newInstance(String param1, String param2) {
        chatFragment fragment = new chatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            recordPath = getArguments().getString("recordPath");
            parsePath=getArguments().getString("parsePath");
            WebResponsePath=getArguments().getString("WebResponsePath");
        }
        //创建接口对象
        Api=new Web_Api(data_list,recordPath,parsePath,this);
        /*录音配置*/
        idealRecorder = IdealRecorder.getInstance();
        recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        /*切换到按键输入按钮*/
        view.findViewById(R.id.bnt_keyboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*切换键盘时传递路径*/
                keyboardFragment fragment = new keyboardFragment();//创建一个键盘输入布局
                Bundle bundle = new Bundle();
                bundle.putString("recordPath", recordPath);
                bundle.putString("parsePath", parsePath);
                fragment.setArguments(bundle);
                // 获取FragmentManager
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                // 开启一个事务
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                // 将当前Fragment替换为新的Fragment
                transaction.replace(R.id.chat_fragment, fragment);
/*                // 将事务添加到返回栈，这样用户可以通过返回按钮返回到之前的Fragment
                transaction.addToBackStack(null);*/
                // 提交事务
                transaction.commit();

            }
        });

        /*录音按钮*/
        view.findViewById(R.id.chat_voice_record).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /*录音准备*/
                        idealRecorder.setRecordFilePath(recordPath);
                        //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
                        idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(20000).setVolumeInterval(200);
                        //设置录音配置 最长录音时长 以及音量回调的时间间隔
                        idealRecorder.setStatusListener(statusListener);
                        /*开始录音*/
                        idealRecorder.start();
                        /*activity振动回调*/
                        long_press_listener.onLongPress();
                        /*进度条处理回调*/
                        pgBar_listener.onPgBar_process(true);
                    }
                }).start();
                return false;

            }
        });

        view.findViewById(R.id.chat_voice_record).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_UP:
                        idealRecorder.stop();//停止录音
                        /*进度条处理回调*/
                        pgBar_listener.onPgBar_process(false);
                        try {
                            String authUrl = Web_Api.getAuthUrl(Web_Api.hostUrl, Web_Api.apiKey, Web_Api.apiSecret);
                            OkHttpClient client = new OkHttpClient.Builder().build();
                            String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
                            Request request = new Request.Builder().url(url).build();
                            WebSocket webSocket = client.newWebSocket(request, Api);

                        }catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                }
                return false;
            }
        });


        return view;
    }










}