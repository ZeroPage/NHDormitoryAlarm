package happs.NH.Food.alarm.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.securepreferences.SecurePreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import happs.NH.Food.alarm.Interfaces.OnCallbackListener;
import happs.NH.Food.alarm.Interfaces.OnPostExecuteListener;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Utils.Constant;
import happs.NH.Food.alarm.Utils.PreferenceBuilder;

/**
 * Created by SH on 2016-03-05.
 */
public class SplashActivity extends Activity {

    private final String INFO_URL = Constant.HTTP + Constant.SERVER_URL + Constant.ANDROID_INFO;
    private final Context ctx = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 비동기처리시작
        // 1. 정보저장
        // 2. 첫실행체크
        // 3.
        new Thread(new Runnable() {
            @Override
            public void run() {
                _getPrefixedInformation(new OnPostExecuteListener() {
                    @Override
                    public void onPostExecute(boolean err) {
                        // 에러 여부에 상관이 없이 첫실행체크
                        if (__initCheck()) {
                            // 첫실행일경우
                            Intent i = new Intent(ctx, InitSettingActivity.class);
                            finish();
                            startActivity(i);
                        } else {
                            // 첫실행이 아닐경우
                            Intent i = new Intent(ctx, MainActivity.class);
                            finish();
                            startActivity(i);
                        }
                    }
                });
            }
        }).start();

    }

    private boolean __initCheck(){
        return PreferenceBuilder.getInstance(getApplicationContext())
                .getSecuredPreference().getBoolean("pref_isFirstVisit", true);
    }

    /* HTTP REQUESTS */
    private void _getPrefixedInformation(final OnPostExecuteListener callback){

        StringRequest r = new StringRequest(Request.Method.GET, INFO_URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        SecurePreferences.Editor pref = PreferenceBuilder
                                .getInstance(getApplicationContext())
                                .getSecuredPreference().edit();
                        JSONObject o;

                        try {
                            o = new JSONObject(response);
                            Iterator<String> itr = o.keys();

                            while (itr.hasNext()) {
                                String key = itr.next();
                                String value = o.get(key).toString();
                                pref.putString(key, value).apply();
                                Log.i("pref", "k :"+key+" v :"+value);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        callback.onPostExecute(false);

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_network_error), Toast.LENGTH_LONG).show();
                        callback.onPostExecute(true);
                    }
                });

        r.setRetryPolicy(new DefaultRetryPolicy(
                Constant.NETWORK_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyQueue.getInstance(getApplicationContext()).addObjectToQueue(r);
    }

}
