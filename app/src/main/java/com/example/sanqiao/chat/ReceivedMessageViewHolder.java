package com.example.sanqiao.chat;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.sanqiao.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

        //leftMsg.setText(message.getText());
        leftMsg.setTypeface(Typeface.MONOSPACE);//设置为等宽字体，方便对齐
        // 在这里你可以继续设置其他视图元素，比如时间戳等

        /*特定部分字体高亮*/
        SpannableString spannableString = new SpannableString(message.getText());
        //Pattern pattern = Pattern.compile("'(.*?)'");
        Pattern pattern = Pattern.compile("'(.*?)'|\\s(\\d+(\\.\\d+)?)\\b");
        Matcher matcher = pattern.matcher(message.getText());
        while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            spannableString.setSpan(new ForegroundColorSpan(Color.RED), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        leftMsg.setText(spannableString);
        leftMsg.setVisibility(View.VISIBLE);
    }
}

