package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Interfaces.OnCallbackListener;
import happs.NH.Food.alarm.Interfaces.OnDataBaseInsertListener;
import happs.NH.Food.alarm.Interfaces.OnStepChangeListener;
import happs.NH.Food.alarm.Network.InputStreamVolleyRequest;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Service.MQTTService;
import happs.NH.Food.alarm.Utils.Constant;
import happs.NH.Food.alarm.Utils.PBKDF2Generator;
import happs.NH.Food.alarm.Utils.PreferenceBuilder;
import happs.NH.Food.alarm.Utils.TopicBuilder;
import happs.NH.Food.alarm.Utils.TopicConstant;

/**
 * Created by SH on 2016-03-20.
 */
public class InitSettingDialogFragment4 extends Fragment implements OnStepChangeListener {

    private LinearLayout loadingPrompt;

    /// 생성자
    public static InitSettingDialogFragment4 newInstance() {
        return new InitSettingDialogFragment4();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init_setting_step4, container, false);

        // object allocation
        loadingPrompt = (LinearLayout)view.findViewById(R.id.loadingPrompt);
        final OnCallbackListener callback = new OnCallbackListener() {
            @Override
            public void onSuccess() {
                loadingPrompt.setVisibility(View.GONE);
            }

            @Override
            public void onFail() {
                loadingPrompt.setVisibility(View.GONE);
                Toast.makeText(getActivity().getApplicationContext(),
                        getString(R.string.prompt_saving_failed), Toast.LENGTH_LONG).show();
                __changeToPreviousStep();
            }
        };

        // do it
        _saveInDataBase(new OnDataBaseInsertListener() {
            @Override
            public void onSuccess() {
                Log.i("DB", "save complete");
                _startPushService(callback);
            }

            @Override
            public void onDuplicated() {
                Log.i("DB", "duplicated");
                _startPushService(callback);
            }

            @Override
            public void onFail() {
                loadingPrompt.setVisibility(View.GONE);
                Toast.makeText(getActivity().getApplicationContext(),
                        getString(R.string.prompt_saving_failed), Toast.LENGTH_LONG).show();
                __changeToPreviousStep();
            }

        });

        return view;
    }


    /// privates
    private void _saveInDataBase(final OnDataBaseInsertListener callback){

        final String url = Constant.HTTP + Constant.SERVER_URL + Constant.API_URL + Constant.CURRENT_API_VERSION + Constant.API_REGISTER;

        StringRequest r = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // REST API 분석
                    Log.i("res", response);
                    JSONObject o =  new JSONObject(response);
                    int status = o.getInt("status");

                    // PREFERENCE 삭제
                    PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                            .getPreference().edit().remove("pref_extra_info");

                    // 0 : success, 2: duplicated(이미가입됨)
                    switch(status){
                        case 0  : callback.onSuccess(); return;
                        case 2  : callback.onDuplicated(); return;
                        default : break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                callback.onFail();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onFail();
            }
        }){
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                final String did = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_device", "");
                final String device = Constant.DEVICE_TYPE;
                final String deviceID = __generatePassword(did);
                final String uid = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_userid", "");
                final String roomNum = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_roomNumber", "");
                final String extra = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getPreference().getString("pref_extra_info", "");

                params.put("userid", uid);
                params.put("room", roomNum);
                params.put("key", deviceID);
                params.put("device", device);
                params.put("extra", extra);

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

        VolleyQueue.getInstance(getActivity().getApplicationContext()).addObjectToQueue(r);

    }

    private void _startPushService(final OnCallbackListener callback){

        // 서비스 시작
        getActivity().startService(new Intent(getActivity().getApplicationContext(), MQTTService.class));

        // 토픽을 구독해보자
        final String topic = Constant.TEST_TOPIC;

        final TopicBuilder tb = new TopicBuilder(getActivity().getApplicationContext());
        tb.subscribe(TopicConstant.READONLY, new OnDataBaseInsertListener() {
            @Override
            public void onSuccess() {
                tb.unsubscribe(topic, new OnCallbackListener() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onFail() {
                        callback.onFail();
                    }
                });
            }

            @Override
            public void onDuplicated() {
                tb.unsubscribe(topic, new OnCallbackListener() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onFail() {
                        callback.onFail();
                    }
                });
            }

            @Override
            public void onFail() {
            }

        }, topic);

    }

    private void _startSubAPKdownload(final OnCallbackListener callback){
        InputStreamVolleyRequest r = new InputStreamVolleyRequest(
                Request.Method.POST, url, new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                try {
                    // REST API 분석
                    Log.i("res", response);
                    JSONObject o =  new JSONObject(response);
                    int status = o.getInt("status");

                    // PREFERENCE 삭제
                    PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                            .getPreference().edit().remove("pref_extra_info");

                    // 0 : success, 2: duplicated(이미가입됨)
                    switch(status){
                        case 0  : callback.onSuccess(); return;
                        case 2  : callback.onDuplicated(); return;
                        default : break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                callback.onFail();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onFail();
            }
        }){
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                final String did = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_device", "");
                final String device = Constant.DEVICE_TYPE;
                final String deviceID = __generatePassword(did);
                final String uid = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_userid", "");
                final String roomNum = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_roomNumber", "");
                final String extra = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getPreference().getString("pref_extra_info", "");

                params.put("userid", uid);
                params.put("room", roomNum);
                params.put("key", deviceID);
                params.put("device", device);
                params.put("extra", extra);

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

        VolleyQueue.getInstance(getActivity().getApplicationContext()).addObjectToQueue(r);
    }

    private String __generatePassword(String plaintext){
        return PBKDF2Generator.generatePassword(plaintext);
    }

    @Override
    public void __changeToNextStep(){
        // Fragment 변경
        ((InitSettingDialogActivity) getActivity())
                .replaceFragment(InitSettingDialogFragment4.newInstance());
    }

    public void __changeToPreviousStep(){
        // Fragment 변경
        ((InitSettingDialogActivity) getActivity())
                .replaceFragment(InitSettingDialogFragment3.newInstance());
    }

}