package com.example.alcalert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserCheckService extends Service {
    public UserCheckService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String debug_tag = "rzc_UserCheckServicer";
    private Thread thread;
    private LocalBroadcastManager localBroadcastManager;
    private String my_url = "http://121.199.76.43:5000/get_concen/";
    private OkHttpClient client = new OkHttpClient();
    private Request request;
    private Gson gson = new Gson();
    class ResponseData {
        public double concentration;
        public String done;
        public int user;
    }
    NotificationManager manager;

    @Override
    public void onCreate() {
        super.onCreate();
        // 广播
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        // 通知
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 每次点击开始服务按钮，先尝试停止已存在的线程
        try {
            thread.interrupt();
        } catch (Exception e) {
        }
        // 获取必要数据
        int user_num = intent.getIntExtra("user_num", -1);
        int delay = intent.getIntExtra("delay", 2);
        double threshold = intent.getDoubleExtra("threshold", 500.0);
        request = new Request.Builder().url(my_url+String.format("?%s=%d", "user_num", user_num)).build();
        // 新线程定义
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(delay*1000);
                        // 网络访问
                        Response response = client.newCall(request).execute();
                        String response_body = response.body().string();
                        ResponseData responseData = gson.fromJson(response_body, ResponseData.class);
                        // 在支线程中进行本地广播，内容为concen的值
                        Intent bc_intent = new Intent("rzc_UserCheckService");
                        bc_intent.putExtra("concen", responseData.concentration);
                        localBroadcastManager.sendBroadcast(bc_intent);
                        // 如果超过预警值，则显示通知
                        if (responseData.concentration > threshold) {
                            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                                    .setContentTitle("警告！")
                                    .setContentText(String.format("浓度超标：%.4fppm", responseData.concentration))
                                    .setWhen(System.currentTimeMillis())
                                    .setSmallIcon(R.drawable.ic)
                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic));
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel("1","alc_alert_channel", NotificationManager.IMPORTANCE_HIGH);
                                channel.setVibrationPattern(new long[] {0, 1000, 1000, 1000});
                                channel.setLightColor(Color.RED);
                                channel.setShowBadge(true);
                                manager.createNotificationChannel(channel);
                                builder.setChannelId("1");
                            }
                            manager.notify(1, builder.build());
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        });
        // 开启新线程
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.interrupt();
    }
}