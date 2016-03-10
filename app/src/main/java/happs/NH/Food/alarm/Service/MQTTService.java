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
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import happs.NH.Food.alarm.Activity.PushPopupActivity;
import happs.NH.Food.alarm.Utils.PushBuilder;

public class MQTTService extends Service {

    private static final String TAG = "MQTTService";
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;

    private final int MAX_TRY = 3;
    private int tryCnt = 0;
    private boolean isSucess = false;

    private MQTTServiceBinder mBinder = new MQTTServiceBinder(this);
    private MQTTBroadcastReceiver receiver;

    private Thread thread;
    private ConnectivityManager mConnMan;
    private String deviceId;
    private volatile IMqttAsyncClient mqttClient;

    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IMqttToken token;
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo[] infos = mConnMan.getAllNetworkInfo();

            for (int i = 0; i < infos.length; i++){
                if (infos[i].getTypeName().equalsIgnoreCase("MOBILE")){
                    if((infos[i].isConnected() != hasMmobile)){
                        hasChanged = true;
                        hasMmobile = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                } else if ( infos[i].getTypeName().equalsIgnoreCase("WIFI") ){
                    if((infos[i].isConnected() != hasWifi)){
                        hasChanged = true;
                        hasWifi = infos[i].isConnected();
                    }
                     Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                }
            }

            hasConnectivity = hasMmobile || hasWifi;
            Log.i(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - "+(mqttClient == null || !mqttClient.isConnected()));

            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                doConnect();
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
        setClientID();
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

    public void unSubscribe(String topic){
        try {
            mqttClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void setClientID(){
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        deviceId = wInfo.getMacAddress();

        if(deviceId == null){
            deviceId = MqttAsyncClient.generateClientId();
        }

        Log.i("ID", deviceId);
    }

    private void doConnect(){
        Log.d(TAG, "doConnect()");
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setConnectionTimeout(5000);
        options.setKeepAliveInterval(20 * 60 * 1000); // 20분 keep-alive interval
        Log.i("Interval", options.getKeepAliveInterval() + "");

        while(true) {

            try {
                mqttClient = new MqttAsyncClient("tcp://125.128.223.83:1883", deviceId, new MemoryPersistence());
                token = mqttClient.connect();
                token.waitForCompletion(3500);
                mqttClient.setCallback(new MqttEventCallback());
                token = mqttClient.subscribe("test", 0);
                token.waitForCompletion(5000);
                token = mqttClient.subscribe("test/test", 0);
                token.waitForCompletion(5000);
                isSucess = true;
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

                tryCnt++;
            }

            if ( isSucess ) break;
            if ( tryCnt == MAX_TRY ) {
                Toast.makeText(getApplicationContext(), "서버 접속 실패", Toast.LENGTH_LONG).show();
                break;
            }
        }
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
            doConnect();
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