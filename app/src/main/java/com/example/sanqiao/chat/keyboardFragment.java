package com.example.sanqiao.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.sanqiao.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link keyboardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class keyboardFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String recordPath;
    private String parsePath;
    static private String WebResponsePath;

    public keyboardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment keyboardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static keyboardFragment newInstance(String param1, String param2) {
        keyboardFragment fragment = new keyboardFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_keyboard, container, false);

        /**/
        view.findViewById(R.id.bnt_voice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*向录音布局传递路径*/
                chatFragment fragment=new chatFragment();
                Bundle bundle = new Bundle();
                bundle.putString("recordPath", recordPath);
                bundle.putString("parsePath", parsePath);
                bundle.putString("WebResponsePath", WebResponsePath);
                fragment.setArguments(bundle);

                // 获取FragmentManager
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                // 开启一个事务
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                // 将当前Fragment替换为新的Fragment
                transaction.replace(R.id.chat_fragment, fragment);

                // 提交事务
                transaction.commit();
            }
        });

        view.findViewById(R.id.chat_edittext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        return view;
    }
}