package com.example.sanqiao.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sanqiao.R;

import java.util.List;

/**
 * 自定义数据适配器类
 */
public class LvDataAdapter2 extends BaseAdapter {

    private Context context;
    private LayoutInflater layoutInflater;
    private List<String> dataList;
    private TextView content;
    private EditText editText;


    public LvDataAdapter2(Context context, List<String> dataList, EditText editText) {
        this.context = context;
        this.dataList = dataList;
        this.editText = editText;

    }

    public int getCount() {
        return dataList.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        layoutInflater = LayoutInflater.from(context);
        convertView = layoutInflater.inflate(R.layout.list_row, null);

        content = convertView.findViewById(R.id.text_row);
        String editContent = dataList.get(position);
        content.setText(dataList.get(position));

        content.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                editText.setText(editContent);
                return false;
            }
        });

        return convertView;
    }
}