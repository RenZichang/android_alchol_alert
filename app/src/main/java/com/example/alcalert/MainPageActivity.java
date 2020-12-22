package com.example.alcalert;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainPageActivity extends AppCompatActivity {

    // 调试变量
    String debug_tag = "rzc_MainPageActivity";

    // 显示在下拉菜单中的用户房间号
    private List<String> user_location = new ArrayList<String>();
    // 控件变量
    private TextView user_select_text;
    private Spinner user_select_spinner;
    private Button start_check_button;
    private Button stop_check_button;
    private TextView user_status_text;
    private EditText service_delay_edittext;
    private EditText concentration_threshlod_edittext;
    // 用于获取选择的用户号码
    public int user_num;
    // 一个适配器
    private ArrayAdapter<String> adapter;
    // 本地广播接收
    private LocalBroadcastManager localBroadcastManager;

    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            user_status_text.setText(String.format("浓度：%.4fppm", intent.getDoubleExtra("concen", -1)));
        }
    }

    private LocalReceiver localReceiver;

    // onCreate()方法
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        // 初始化控件变量
        user_select_text = (TextView) findViewById(R.id.user_select_text);
        user_select_spinner = (Spinner) findViewById(R.id.user_select_spinner);
        start_check_button = (Button) findViewById(R.id.start_check_button);
        stop_check_button = (Button) findViewById(R.id.stop_check_button);
        user_status_text = (TextView) findViewById(R.id.user_status_text);
        service_delay_edittext = (EditText) findViewById(R.id.service_delay_edittext);
        concentration_threshlod_edittext = (EditText) findViewById(R.id.concentration_threshlod_edittext);
        /* 下拉菜单的处理逻辑如下 */
        // 初始化用户单元号和房间号
        for (int j = 1; j <= 2; j++) {
            for (int i = 0; i <= 11; i++) {
                user_location.add(String.format("%d单元%03d房间", j, i));
            }
        }
        // 初始化适配器
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, user_location);
        // 设置下拉列表下拉时的菜单样式
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 将适配器添加到下拉列表上
        user_select_spinner.setAdapter(adapter);
        // 添加监听器，为下拉列表设置事件的响应
        user_select_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> argO, View argl, int arg2, long arg3) {
                user_select_text.setText(String.format("你现在选择的是\n%s，第%d个用户", adapter.getItem(arg2), arg2));
                user_num = arg2;
                argO.setVisibility(View.VISIBLE);
            }

            public void onNothingSelected(AdapterView<?> argO) {
                user_select_text.setText("没有选择任何用户");
                argO.setVisibility(View.VISIBLE);
            }
        });
        /* 开启服务的按钮逻辑如下 */
        start_check_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                try {
                    user_status_text.setText("服务已开启");
                    Intent intent = new Intent(MainPageActivity.this, UserCheckService.class);
                    stopService(intent);
                    intent.putExtra("user_num", user_num);
                    int delay = Integer.parseInt(service_delay_edittext.getText().toString());
                    intent.putExtra("delay", delay);
                    double threshold = Float.parseFloat(concentration_threshlod_edittext.getText().toString());
                    intent.putExtra("threshold", threshold);
                    startService(intent);
                } catch (Exception e) {
                }
            }
        });
        /* 停止服务的按钮逻辑如下 */
        stop_check_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                Intent intent = new Intent(MainPageActivity.this, UserCheckService.class);
                stopService(intent);
                user_status_text.setText("服务已停止");
            }
        });
        /* 注册本地广播接收器 */
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("rzc_UserCheckService");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter);
    }

    // onDestroy()方法
    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(localReceiver);
    }
}