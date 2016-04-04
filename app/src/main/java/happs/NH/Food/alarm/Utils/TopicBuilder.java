package happs.NH.Food.alarm.Utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import happs.NH.Food.alarm.Database.Topic;
import happs.NH.Food.alarm.Interfaces.OnCallbackListener;
import happs.NH.Food.alarm.Interfaces.OnDataBaseInsertListener;
import happs.NH.Food.alarm.Interfaces.OnPostExecuteListener;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.Service.MQTTService;
import happs.NH.Food.alarm.Service.MQTTServiceBinder;

/**
 * Created by SH on 2016-03-27.
 */
public class TopicBuilder {

    private static Context ctx;
    private static boolean mBound;
    private static boolean err = false;

    private static MQTTService mService;
    private static ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MQTTServiceBinder b = (MQTTServiceBinder) service;
            mService = b.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
    private static TopicBuilder instance = new TopicBuilder();

    public static TopicBuilder getInstance(Context context) {
        ctx = context;

        ctx.startService(new Intent(ctx, MQTTService.class));
        Intent i = new Intent(ctx, MQTTService.class);
        ctx.bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        return instance;
    }
    private TopicBuilder(){}

    public void destroy(){
        if(mBound) {
            ctx.unbindService(mConnection);
            mBound=false;
        }
    }

    // subscribe
    public synchronized void subscribeFromSQLite(final String... topic){
        if( mService != null && _isServiceRunning(Constant.SERVICE_NAME) ) {
            mService.subscribe(topic);
        }
    }
    public synchronized void subscribe(final Topic... topic){

        final String url = Constant.HTTP + Constant.SERVER_URL + Constant.API_URL + Constant.CURRENT_API_VERSION + Constant.API_SUBSCRIBE;

        for(final Topic t : topic) {
            StringRequest r = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        // REST API 분석
                        Log.i("subscribe_res", response);
                        JSONObject o = new JSONObject(response);
                        int status = o.getInt("status");

                        // 0 : success, 1: db error
                        if (status == 0 && _isServiceRunning(Constant.SERVICE_NAME)) {
                            mService.subscribe(t.getName());
                        } else if (status == 1) {
                            Log.i("SUBSCRIBE", "DB ERROR");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("SUBSCRIBE", "DB ERROR");
                }
            }) {
                @Override
                public Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();

                    final String uid = PreferenceBuilder.getInstance(ctx.getApplicationContext())
                            .getSecuredPreference().getString("pref_device", "");

                    params.put("userid", uid);
                    params.put("rw", t.getMode() + "");
                    params.put("topic", t.getName());

                    return params;
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    try {
                        String result = new String(response.data, "UTF-8");

                        return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    return Response.error(new VolleyError());
                }
            };

            VolleyQueue.getInstance(ctx.getApplicationContext()).addObjectToQueue(r);
        }
    }
    public synchronized void subscribe(final OnPostExecuteListener onPostExecuteListener, final OnDataBaseInsertListener callback, final Topic... topic){

        final String url = Constant.HTTP + Constant.SERVER_URL + Constant.API_URL + Constant.CURRENT_API_VERSION + Constant.API_SUBSCRIBE;

        for(final Topic t : topic) {
            StringRequest r = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        // REST API 분석
                        Log.i("subscribe_res", response);
                        JSONObject o = new JSONObject(response);
                        int status = o.getInt("status");

                        // 0 : success, 1: db error
                        if (mService != null && status == 0 && _isServiceRunning(Constant.SERVICE_NAME)) {

                            mService.subscribe(t.getName());
                            callback.onSuccess(); err=false; return;

                        } else if (status == 1) {
                            Log.i("SUBSCRIBE", "DB ERROR");
                        } else if (status == 2 && mService != null && _isServiceRunning(Constant.SERVICE_NAME)) {

                            mService.subscribe(t.getName());
                            callback.onDuplicated(); err=false; return;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(onPostExecuteListener == null) callback.onFail();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("SUBSCRIBE", "DB ERROR");
                    if(onPostExecuteListener == null) callback.onFail();
                }
            }) {
                @Override
                public Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();

                    final String uid = PreferenceBuilder.getInstance(ctx.getApplicationContext())
                            .getSecuredPreference().getString("pref_device", "");

                    params.put("userid", uid);
                    params.put("rw", t.getMode() + "");
                    params.put("topic", t.getName());

                    return params;
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    try {
                        String result = new String(response.data, "UTF-8");

                        return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    return Response.error(new VolleyError());
                }
            };

            VolleyQueue.getInstance(ctx.getApplicationContext()).addObjectToQueue(r);
        }

        if(onPostExecuteListener != null ){
            if( err ){ onPostExecuteListener.onPostExecute(true); }
            else{ onPostExecuteListener.onPostExecute(false); err=false; }
        }

    }

    // unsubscribe
    public synchronized void unsubscribe(final String topic){

        final String url = Constant.HTTP + Constant.SERVER_URL + Constant.API_URL + Constant.CURRENT_API_VERSION + Constant.API_UNSUBSCRIBE;

        StringRequest r = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // REST API 분석
                    Log.i("UNSUB_res", response);
                    JSONObject o =  new JSONObject(response);
                    int status = o.getInt("status");

                    // 0 : success, 1: db error
                    if( status == 0 && _isServiceRunning(Constant.SERVICE_NAME) ) {
                        mService.unsubscribe(topic);
                    }
                    else if ( status == 1 ){
                        Log.i("UNSUBSCRIBE", "DB ERROR");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("SUBSCRIBE", "DB ERROR");
            }
        }){
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                final String uid = PreferenceBuilder.getInstance(ctx.getApplicationContext())
                        .getSecuredPreference().getString("pref_device", "");

                params.put("userid", uid);
                params.put("topic", topic);

                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String result = new String(response.data, "UTF-8");

                    return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                return Response.error(new VolleyError());
            }
        };

        VolleyQueue.getInstance(ctx.getApplicationContext()).addObjectToQueue(r);

    }
    public synchronized void unsubscribe(final String topic, final OnCallbackListener callback){

        final String url = Constant.HTTP + Constant.SERVER_URL + Constant.API_URL + Constant.CURRENT_API_VERSION + Constant.API_UNSUBSCRIBE;

        StringRequest r = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // REST API 분석
                    Log.i("UNSUB_res", response);
                    JSONObject o =  new JSONObject(response);
                    int status = o.getInt("status");

                    // 0 : success, 1: db error
                    if( status == 0 && _isServiceRunning(Constant.SERVICE_NAME) ) {
                        mService.unsubscribe(topic);
                        callback.onSuccess(); return;
                    }
                    else if ( status == 1 ){
                        Log.i("UNSUBSCRIBE", "DB ERROR");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                callback.onFail();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("SUBSCRIBE", "DB ERROR");
                callback.onFail();
            }
        }){
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                final String uid = PreferenceBuilder.getInstance(ctx.getApplicationContext())
                        .getSecuredPreference().getString("pref_device", "");

                params.put("userid", uid);
                params.put("topic", topic);

                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String result = new String(response.data, "UTF-8");

                    return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                return Response.error(new VolleyError());
            }
        };

        VolleyQueue.getInstance(ctx.getApplicationContext()).addObjectToQueue(r);

    }

    // publish
    public synchronized void publish(final String topic, final int qos, final boolean retain, final String msg){
        mService.publish(topic, qos, retain, msg);
    }
    public synchronized void publish(final String topic, final int qos, final boolean retain, final String msg, final OnCallbackListener callback) {
        try {
            mService.publish(topic, qos, retain, msg);
            callback.onSuccess();
        } catch (Exception e){
            callback.onFail();
        }
    }


    private boolean _isServiceRunning(String serviceName) {
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(runningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}