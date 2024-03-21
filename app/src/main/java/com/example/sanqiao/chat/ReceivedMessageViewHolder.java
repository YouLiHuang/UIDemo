package com.example.sanqiao.chat;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.sanqiao.R;


public class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
    private RelativeLayout left_layout;
    private RelativeLayout right_layout;

    TextView leftMsg;
    TextView rightMsg;

    public ReceivedMessageViewHolder(View itemView) {
        super(itemView);
        left_layout = itemView.findViewById(R.id.left_layout_total);
        right_layout = itemView.findViewById(R.id.right_layout_total);

        leftMsg = itemView.findViewById(R.id.text_left_msg);
        rightMsg = itemView.findViewById(R.id.text_right_msg);
    }

    public void bind(Message message) {

        /*隐藏右侧*/
        left_layout.setVisibility(View.VISIBLE);
        right_layout.setVisibility(View.GONE);

        leftMsg.setText(message.getText());
        // 在这里你可以继续设置其他视图元素，比如时间戳等
    }
}

