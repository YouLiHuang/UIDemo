package com.example.sanqiao.chat;

public class Message {
    private String text;//消息内容
    private int type;//消息类型（发送/接收）

    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;

    public Message(String text, int type) {
        this.text = text;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return  type;
    }
}
