package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.app.Activity;
import android.content.Context;
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;
import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Interfaces.OnCallbackListener;
import happs.NH.Food.alarm.Interfaces.OnDataBaseInsertListener;
import happs.NH.Food.alarm.Interfaces.OnResponseListener;
import happs.NH.Food.alarm.Interfaces.OnStepChangeListener;
import happs.NH.Food.alarm.Network.InputStreamVolleyRequest;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Service.MQTTService;
import happs.NH.Food.alarm.Utils.Constant;
import happs.NH.Food.alarm.Utils.DefaultSettings;
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

        // set Callbacks (역순으로 호출됨)
        final OnResponseListener<String> subAPKCallback = new OnResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                Log.i("Checksum check start", response);

                // 체크섬 저장.
                PreferenceBuilder pb = PreferenceBuilder.getInstance(getActivity().getApplicationContext());
                String cs = pb.getSecuredPreference().getString(DefaultSettings.SUB_VERSION_CHECKSUM, "");
                pb.getSecuredPreference().edit().putString("pref_checksum", response).apply();

                if( !response.equals(cs) ) pb.getSecuredPreference().edit().putString(DefaultSettings.IS_EVENT_ENABLE, "false").apply();
                loadingPrompt.setVisibility(View.GONE);
                test();
            }

            @Override
            public void onFail() {
                __onFail();
            }
        };
        final OnCallbackListener pushCallback = new OnCallbackListener() {
            @Override
            public void onSuccess() {
                // 여기에서는 첫실행이라는 가정하에 진행하므로
                // 무조건 다운로드하게 한다.
                _startSubAPKDown(subAPKCallback);
            }

            @Override
            public void onFail() {
                __onFail();
            }
        };
        final OnDataBaseInsertListener dbCallback = new OnDataBaseInsertListener() {
            @Override
            public void onSuccess() {
                Log.i("DB", "save complete");
                _startPushService(pushCallback);
            }

            @Override
            public void onDuplicated() {
                Log.i("DB", "duplicated");
                _startPushService(pushCallback);
            }

            @Override
            public void onFail() {
                __onFail();
            }

        };

        // do it
        _saveInDataBase(dbCallback);

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
                final String uid = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_userid", "");
                final String roomNum = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getSecuredPreference().getString("pref_roomNumber", "");
                final String extra = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                        .getPreference().getString("pref_extra_info", "");

                params.put("userid", uid);
                params.put("room", roomNum);
                params.put("did", did);
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

        // 자기자신의 토픽을 구독하고 퍼블리쉬해보자
        final String uid = PreferenceBuilder.getInstance(getActivity()).getSecuredPreference()
                .getString("pref_userid", "");
        final String topic = Constant.THIS_YEAR + uid;

        final TopicBuilder tb = new TopicBuilder(getActivity().getApplicationContext());
        tb.subscribe(TopicConstant.READWRITE, new OnDataBaseInsertListener() {
            @Override
            public void onSuccess() {
                tb.publish(topic, 1, false, getString(R.string.mqtt_success_msg), new OnCallbackListener() {
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
                tb.publish(topic, 1, false, getString(R.string.mqtt_success_msg), new OnCallbackListener() {
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
                __onFail();
            }

        }, topic);

    }
    private void _startSubAPKDown(final OnResponseListener<String> callback){

        final String URL = PreferenceBuilder.getInstance(getActivity()).getSecuredPreference()
                    .getString(DefaultSettings.SUB_APK_URL, "");

        final InputStreamVolleyRequest r = new InputStreamVolleyRequest(Request.Method.GET, URL, new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                InputStream input = null;
                BufferedOutputStream output = null;

                try {
                    if (response != null) {
                        String filename = URL.replaceAll("^.*\\/", "");

                        //covert response to input stream
                        input = new ByteArrayInputStream(response);
                        File path = getActivity().getDir(Constant.SUB_APK_DIR, Context.MODE_PRIVATE);

                        File file = new File(path, filename);
                        Log.i("fileName", file.toString() + "/path:" + file.getAbsolutePath());

                        output = new BufferedOutputStream(new FileOutputStream(file));
                        byte data[] = new byte[1024], md5Bytes[];

                        // GENERATE CHECKSUM
                        MessageDigest digest = MessageDigest.getInstance("MD5");

                        int count = 0;
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                            if (count > 0) digest.update(data, 0, count);
                        }

                        output.flush();
                        md5Bytes = digest.digest();

                        callback.onSuccess(__convertHashToString(md5Bytes));
                        return;
                    }

                } catch (Exception e) {
                    Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                    e.printStackTrace();
                } finally {
                    try {
                        if (output != null) output.close();
                        if (input != null) input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                callback.onFail();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onFail();
            }
        });

        VolleyQueue.getInstance(getActivity().getApplicationContext()).addObjectToQueue(r);

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


    private void __onFail(){
        Activity activity = getActivity();

        if(activity != null) {
            loadingPrompt.setVisibility(View.GONE);
            Toast.makeText(getActivity(), getString(R.string.prompt_saving_failed), Toast.LENGTH_LONG).show();
            __changeToPreviousStep();
        }
    }
    private String __convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString(( md5Bytes[i] & 0xff ) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }

    private void test(){

        try {
            // optimized directory, the applciation and package directory
            final File optimizedDexOutputPath = getActivity().getDir(Constant.SUB_APK_DIR, Context.MODE_PRIVATE);
            final String filename = PreferenceBuilder.getInstance(getActivity().getApplicationContext())
                    .getSecuredPreference().getString(DefaultSettings.SUB_APK_URL, "").replaceAll("^.*\\/", "");

            // DexClassLoader to get the file and write it to the optimised directory
            DexClassLoader cl = new DexClassLoader(optimizedDexOutputPath.getPath()+"/"+filename,
                    optimizedDexOutputPath.getPath(), null, getActivity().getClassLoader());

            Class<?> clz = cl.loadClass("happs.NH.Food.alarm.sub.SubClass");

            // MyTestClass has a constructor with no arguments
            Constructor<?> cons = clz.getConstructor();
            Object obj = cons.newInstance();

            Method m = clz.getMethod("getEvent", Context.class);
            boolean re = (boolean)m.invoke(obj, getActivity().getApplicationContext());

            Method m2 = clz.getMethod("getVersionCode");
            int vc = (int)m2.invoke(obj);

            //Log.i("SUBVERSION_CODE", vc+"");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}