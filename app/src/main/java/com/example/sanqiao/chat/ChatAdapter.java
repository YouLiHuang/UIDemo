package com.example.sanqiao.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sanqiao.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> messages;//适配器的消息列表

    // 构造函数
    public ChatAdapter(List<Message> messages) {

        this.messages=messages;
    }


    /*获取视图类型，取决于消息类型（发送/接收）*/
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getType()==Message.TYPE_SENT) {
            return R.layout.item_sent_message;
        } else {
            return R.layout.item_received_message;
        }
    }

    /*创建一个试图持有者，根据试图类型决定创建发送/接收两类持有者*/
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (viewType == R.layout.item_sent_message) {
            return new SentMessageViewHolder(view);
        } else {
            return new ReceivedMessageViewHolder(view);
        }
    }

    /*根据当前试图持有者调用不同的绑定接口*/
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder.getItemViewType() == R.layout.item_sent_message) {

            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}

