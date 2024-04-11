package com.example.sanqiao.webapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sanqiao.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import okhttp3.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Web_Api extends WebSocketListener {

    public static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; //中英文，http url 不支持解析 ws/wss schema
    // private static final String hostUrl = "https://iat-niche-api.xfyun.cn/v2/iat";//小语种
    public static final String appid = "ff9d26aa"; //在控制台-我的应用获取
    public static final String apiSecret = "YTlhZDY0M2ZhYzk4ZjcwMGMyMDhhYThj"; //在控制台-我的应用-语音听写（流式版）获取
    public static final String apiKey = "d78a8f15d26324aacc0614a4e16a8b50";
    private String recordPath; // 中文
    private String parsePath;
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();
    Web_Api.Decoder decoder = new Web_Api.Decoder();
    // 开始时间
    private static Date dateBegin = new Date();
    // 结束时间
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    public Set<String> data_list;

    public interface covert_finished_listener{
        void onTaskCompleted();
    }
    covert_finished_listener listener;

    public Web_Api(Set<String> list, String recordPath,String parsePath,covert_finished_listener listener) {
        this.data_list = list;
        if (!data_list.isEmpty()) {
            data_list.clear();
        }
        this.recordPath = recordPath;
        this.parsePath=parsePath;
        this.listener=listener;

    }


    public Set<String> get_data() {
        return this.data_list;
    }


    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        /*解析失，无数据*/
        try {
            JSONObject json = new JSONObject();
            json.put("response", "网络错误，请检查网络是否连接");
            FileUtil.saveText(parsePath, json.toString());
            listener.onTaskCompleted();//回到activity当中实现UI
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        new Thread(() -> {
            //连接成功，开始发送数据
            int frameSize = 1280 * 16; //每一帧音频的大小,建议每 40ms 发送 122B
            int intervel = 20;
            int status = 0;  // 音频的状态
            byte[] buffer = new byte[frameSize];
            int len;
            boolean flag = true;
            try {
                FileInputStream fs = new FileInputStream(recordPath);
                while (true) {
                    if (status != StatusLastFrame) {
                        len = fs.read(buffer);
                        if (len == -1) {
                            status = StatusLastFrame;
                        }
                        switch (status) {
                            case StatusFirstFrame:   // 第一帧音频status = 0
                                JsonObject frame = new JsonObject();
                                JsonObject business = new JsonObject();  //第一帧必须发送
                                JsonObject common = new JsonObject();  //第一帧必须发送
                                JsonObject data = new JsonObject();  //每一帧都要发送
                                // 填充common
                                common.addProperty("app_id", appid);
                                //填充business
                                business.addProperty("language", "zh_cn");
                                business.addProperty("domain", "iat");
                                business.addProperty("accent", "mandarin");//中文方言请在控制台添加试用，添加后即展示相应参数值
                                business.addProperty("dwa", "wpgs");//动态修正(若未授权不生效，在控制台可免费开通)
                                business.addProperty("vad_eos",5000);
                                //填充data
                                data.addProperty("status", StatusFirstFrame);
                                data.addProperty("format", "audio/L16;rate=16000");
                                data.addProperty("encoding", "raw");
                                data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                                //填充frame
                                frame.add("common", common);
                                frame.add("business", business);
                                frame.add("data", data);
                                webSocket.send(frame.toString());
                                status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                                break;
                            case StatusContinueFrame:  //中间帧status = 1
                                JsonObject frame1 = new JsonObject();
                                JsonObject data1 = new JsonObject();
                                data1.addProperty("status", StatusContinueFrame);
                                data1.addProperty("format", "audio/L16;rate=16000");
                                data1.addProperty("encoding", "raw");
                                data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                                frame1.add("data", data1);
                                webSocket.send(frame1.toString());
                                //data_list.add("send continue");
                                break;
                            case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                                JsonObject frame2 = new JsonObject();
                                JsonObject data2 = new JsonObject();
                                data2.addProperty("status", StatusLastFrame);
                                data2.addProperty("audio", "");
                                data2.addProperty("format", "audio/L16;rate=16000");
                                data2.addProperty("encoding", "raw");
                                frame2.add("data", data2);
                                webSocket.send(frame2.toString());
                                //data_list.add("all data is send");
                                break;
                        }
                        Thread.sleep(intervel); //模拟音频采样延时

                    } else Thread.sleep(1000); //模拟音频采样延时
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        //System.out.println(text);
        Web_Api.ResponseData resp = json.fromJson(text, Web_Api.ResponseData.class);
        if (resp != null) {
            if (resp.getCode() != 0) {
                data_list.add("code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
                data_list.add("错误码查询链接：https://www.xfyun.cn/document/error-code");
                return;
            }
            if (resp.getData() != null) {
                if (resp.getData().getResult() != null) {
                    Web_Api.Text te = resp.getData().getResult().getText();
                    //System.out.println(te.toString());
                    try {
                        decoder.decode(te);
                        //data_list.add( "中间识别结果 ==》" +decoder.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (resp.getData().getStatus() == 2) {
                    // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    dateEnd = new Date();
/*                    data_list.add(sdf.format(dateBegin) + "开始");
                    data_list.add(sdf.format(dateEnd) + "结束");*/
/*                    data_list.add( decoder.toString());
                    data_list.add("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");*/
                    //data_list.add("本次识别sid ==》" + resp.getSid());

                    try {
                        JSONObject json = new JSONObject();
                        json.put("response", decoder.toString());
                        json.put("time", (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                        FileUtil.saveText(parsePath, json.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    decoder.discard();
                    webSocket.close(1000, "");
                    listener.onTaskCompleted();//回到activity当中实现UI
                } else {
                    // todo 根据返回的数据处理
                    /*解析失，无数据*/
                    try {
                        JSONObject json = new JSONObject();
                        json.put("response", "解析失败，无数据");
                        FileUtil.saveText(parsePath, json.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        //System.out.println(builder);
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        //System.out.println(sha);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        //System.out.println(authorization);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Web_Api.Data data;

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return this.message;
        }

        public String getSid() {
            return sid;
        }

        public Web_Api.Data getData() {
            return data;
        }
    }

    public static class Data {
        private int status;
        private Web_Api.Result result;

        public int getStatus() {
            return status;
        }

        public Web_Api.Result getResult() {
            return result;
        }
    }

    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Web_Api.Ws[] ws;
        boolean ls;
        JsonObject vad;

        public Web_Api.Text getText() {
            Web_Api.Text text = new Web_Api.Text();
            StringBuilder sb = new StringBuilder();
            for (Web_Api.Ws ws : this.ws) {
                sb.append(ws.cw[0].w);
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.sn = this.sn;
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad == null ? null : this.vad;
            return text;
        }
    }

    public static class Ws {
        Web_Api.Cw[] cw;
        int bg;
        int ed;
    }

    public static class Cw {
        int sc;
        String w;
    }

    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JsonObject vad;

        @Override
        public String toString() {
            return "Text{" +
                    "bg=" + bg +
                    ", ed=" + ed +
                    ", ls=" + ls +
                    ", sn=" + sn +
                    ", text='" + text + '\'' +
                    ", pgs=" + pgs +
                    ", rg=" + Arrays.toString(rg) +
                    ", deleted=" + deleted +
                    ", vad=" + (vad == null ? "null" : vad.getAsJsonArray("ws").toString()) +
                    '}';
        }
    }

    //解析返回数据，仅供参考
    public static class Decoder {
        private Web_Api.Text[] texts;
        private int defc = 10;

        public Decoder() {
            this.texts = new Web_Api.Text[this.defc];
        }

        public synchronized void decode(Web_Api.Text text) {
            if (text.sn >= this.defc) {
                this.resize();
            }
            if ("rpl".equals(text.pgs)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    this.texts[i].deleted = true;
                }
            }
            this.texts[text.sn] = text;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Web_Api.Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }

        public void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Web_Api.Text[] old = this.texts;
            this.texts = new Web_Api.Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }

        public void discard() {
            for (int i = 0; i < this.texts.length; i++) {
                this.texts[i] = null;
            }
        }
    }


}
