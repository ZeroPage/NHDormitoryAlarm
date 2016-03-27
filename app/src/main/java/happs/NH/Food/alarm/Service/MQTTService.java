package happs.NH.Food.alarm.Service;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

import happs.NH.Food.alarm.Activity.PushPopupActivity;
import happs.NH.Food.alarm.Utils.PreferenceBuilder;

public class MQTTService extends Service {

    private static final String TAG = "MQTTService";
    private static boolean hasWifi = false;
    private static boolean hasMobile = false;

    private MQTTServiceBinder mBinder = new MQTTServiceBinder(this);
    private MQTTBroadcastReceiver receiver;

    private Thread thread;
    private ConnectivityManager mConnMan;
    private volatile IMqttAsyncClient mqttClient;
    private volatile IMqttToken token;

    class MQTTBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            IMqttToken token;
            boolean hasConnectivity, hasChanged = false;
            NetworkInfo[] infos = mConnMan.getAllNetworkInfo();

            for (NetworkInfo network: infos){
                int type = network.getType();

                switch (type){
                    case ConnectivityManager.TYPE_MOBILE:
                        if((network.isConnected() != hasMobile)){
                            hasChanged = true;
                            hasMobile = network.isConnected();
                        } break;

                    case ConnectivityManager.TYPE_WIFI:
                        if((network.isConnected() != hasWifi)){
                            hasChanged = true;
                            hasWifi = network.isConnected();
                        } break;
                }
            }

            hasConnectivity = hasMobile || hasWifi;
            Log.i(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - "+(mqttClient == null || !mqttClient.isConnected()));

            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                connect();
            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "doDisconnect()");
                try {
                    token = mqttClient.disconnect();
                    token.waitForCompletion(1000);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate() {
        IntentFilter intentf = new IntentFilter();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        unregisterRestartAlarm();

        receiver = new MQTTBroadcastReceiver();
        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        registerRestartAlarm();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged()");
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);
    }

    public void publish(String topic, int qos, boolean retained, String msg){

        try {
            mqttClient.publish(topic, msg.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void unsubscribe(String topic){
        try {
            mqttClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean subscribe(String... topics){

        try {
            for(String t : topics) {
                token = mqttClient.subscribe(t, 0);
                token.waitForCompletion(5000);
            }
            return true;
        } catch (MqttException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean connect(){
        Log.d(TAG, "connect()");

        // client id & password
        final String cid = PreferenceBuilder.getInstance(getApplicationContext())
                .getSecuredPreference().getString("pref_userid","");
        final String cpw = PreferenceBuilder.getInstance(getApplicationContext())
                .getSecuredPreference().getString("pref_device","");

        // MQTT Options
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setConnectionTimeout(5000);
        options.setKeepAliveInterval(20 * 60 * 1000); // 20분 keep-alive interval
        options.setUserName(cid);
        options.setPassword(cpw.toCharArray());

        // Logging..
        Log.i("Interval", options.getKeepAliveInterval() + "");

        try {
            mqttClient = new MqttAsyncClient("tcp://leesnhyun.iptime.org:1883", cid, new MemoryPersistence());
            token = mqttClient.connect(options);
            token.waitForCompletion(3500);
            mqttClient.setCallback(new MqttEventCallback());

            return true;

        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" + e.getMessage());
                    e.printStackTrace();
                    break;

                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b" + e.getMessage());
                    break;

                default:
                    Log.e(TAG, "a" + e.getMessage());
            }
        }

        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand()");
        return START_STICKY;
    }

    private class MqttEventCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i("MQTT", "Connection lost");
            connect();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.i("MQTT", "Delivery Complete");
        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Log.i(TAG, "Message arrived from topic : " + topic);
            Handler h = new Handler(getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(MQTTService.this, PushPopupActivity.class);

                    i.putExtra("msg", new String(msg.getPayload()));
                    PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_ONE_SHOT);

                    try {
                        pi.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.i("PendingIntent", "cancel Expetion");
                    }

                    //PushBuilder b = new PushBuilder();
                    //b.makeNotification(getApplicationContext(), null, new String(msg.getPayload()));
                    //b.makePopupNotification(getApplicationContext(), i, new String(msg.getPayload()));

                }
            });
        }
    }

    public String getThread(){
        return Long.valueOf(thread.getId()).toString();
    }


    // support persistent of Service
    public void registerRestartAlarm() {
        Log.d("PersistentService", "registerRestartAlarm");
        Intent intent = new Intent(MQTTService.this, MQTTBroadcastReceiver.class);
        intent.setAction("ACTION.RESTART.MQTTService");
        PendingIntent sender = PendingIntent.getBroadcast(MQTTService.this, 0, intent, 0);
        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 10 * 1000; // 20분 후에 알람이벤트 발생
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 10 * 1000, sender);
    }

    public void unregisterRestartAlarm() {
        Log.d("PersistentService", "unregisterRestartAlarm");
        Intent intent = new Intent(MQTTService.this, MQTTBroadcastReceiver.class);
        intent.setAction("ACTION.RESTART.MQTTService");
        PendingIntent sender = PendingIntent.getBroadcast(MQTTService.this, 0, intent, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");
        return mBinder;
    }

}