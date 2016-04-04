package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.securepreferences.SecurePreferences;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONArray;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Database.TopicDBHelper;
import happs.NH.Food.alarm.Database.Topic;
import happs.NH.Food.alarm.Interfaces.OnBackPressedListener;
import happs.NH.Food.alarm.Interfaces.OnCallbackListener;
import happs.NH.Food.alarm.Interfaces.OnDataBaseInsertListener;
import happs.NH.Food.alarm.Interfaces.OnPostExecuteListener;
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
    private Context ctx;

    /// 생성자
    public static InitSettingDialogFragment4 newInstance() {
        return new InitSettingDialogFragment4();
    }

    @Override
    public void onAttach(Context context) {
        this.ctx = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init_setting_step4, container, false);

        // object allocation
        loadingPrompt = (LinearLayout)view.findViewById(R.id.loadingPrompt);

        // set Callbacks (역순으로 호출됨)
        final OnPostExecuteListener sendPushTestCallback = new OnPostExecuteListener() {
            @Override
            public void onPostExecute(boolean err) {
                if(err) __onFail();
                else {
                    loadingPrompt.setVisibility(View.GONE);
                    __changeToNextStep();
                }
            }
        };
        final OnResponseListener<String> subAPKCallback = new OnResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                Log.i("subAPKDownload", "Success");

                // 체크섬 저장.
                PreferenceBuilder pb = PreferenceBuilder.getInstance(ctx);
                String cs = pb.getSecuredPreference().getString(DefaultSettings.SUB_VERSION_CHECKSUM, "");
                pb.getSecuredPreference().edit().putString("pref_checksum", response).apply();

                if( !response.equals(cs) ) pb.getSecuredPreference().edit().putString(DefaultSettings.IS_EVENT_ENABLE, "false").apply();

                _sendPushTest(sendPushTestCallback);
            }

            @Override
            public void onFail() {
                __onFail();
            }
        };
        final OnResponseListener<ArrayList<Topic>> loadDataCallback = new OnResponseListener<ArrayList<Topic>>() {
            @Override
            public void onSuccess(ArrayList<Topic> response) {
                Log.i("Dup", "여기진입");
                final TopicDBHelper topicDBHelper = new TopicDBHelper(ctx, Constant.DATABASE_NAME, null, Constant.DATABASE_VERSION);
                for(Topic t : response) {
                    topicDBHelper.insert(t.getName(), t.getMode());
                }

                final TopicBuilder tb = TopicBuilder.getInstance(ctx);

                // Local DB에 잘 저장되었는지 확인하기 위해서 해봄.
                final List<Topic> list = topicDBHelper.getTopicLists();

                String[] topics = new String[list.size()];
                for( int i=0; i<list.size(); i++ ){
                    topics[i] = list.get(i).getName();
                }

                tb.subscribeFromSQLite(topics);
                _startSubAPKDown(subAPKCallback);
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
                ctx.startService(new Intent(ctx, MQTTService.class));
                _startPushService(pushCallback);
            }

            @Override
            public void onDuplicated() {
                Log.i("DB", "duplicated");
                ctx.startService(new Intent(ctx, MQTTService.class));
                _loadDataFromServer(loadDataCallback);
            }

            @Override
            public void onFail() {
                __onFail();
            }

        };

        // do it
        new Thread(new Runnable() {
            @Override
            public void run() {
                _saveInPreference(dbCallback);
            }
        }).start();

        return view;
    }

    /// privates
    private void _saveInPreference(final OnDataBaseInsertListener callback){

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
                    PreferenceBuilder.getInstance(ctx)
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

                final String did = PreferenceBuilder.getInstance(ctx)
                        .getSecuredPreference().getString("pref_device", "");
                final String device = Constant.DEVICE_TYPE;
                final String uid = PreferenceBuilder.getInstance(ctx)
                        .getSecuredPreference().getString("pref_userid", "");
                final String roomNum = PreferenceBuilder.getInstance(ctx)
                        .getSecuredPreference().getString("pref_roomNumber", "");
                final String extra = PreferenceBuilder.getInstance(ctx)
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

        r.setRetryPolicy(new DefaultRetryPolicy(
                Constant.NETWORK_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyQueue.getInstance(ctx).addObjectToQueue(r);

    }
    private void _startPushService(final OnCallbackListener callback){

        // 기본토픽들을 구독하자
        final SecurePreferences spref = PreferenceBuilder.getInstance(ctx).getSecuredPreference();
        final String uid = spref.getString("pref_userid", "");
        final String uroom = spref.getString("pref_roomNumber", "");

        final TopicDBHelper topicDBHelper = new TopicDBHelper(ctx, Constant.DATABASE_NAME, null, Constant.DATABASE_VERSION);
        final TopicBuilder tb = TopicBuilder.getInstance(ctx);
        final Topic[] topic = new Topic[5];

        topic[0] = new Topic(Constant.THIS_YEAR + "members/" + uid, TopicConstant.READWRITE); // 개인토픽
        topic[1] = new Topic(Constant.THIS_YEAR + Constant.TOTAL_TOPIC, TopicConstant.READONLY); // 사생전체
        topic[2] = new Topic(Constant.THIS_YEAR + "rooms/" + uroom, TopicConstant.READWRITE); // 각자호실
        topic[3] = new Topic(Constant.THIS_YEAR + "floors/" + uroom.substring(0,1), TopicConstant.READWRITE); // 층별
        topic[4] = new Topic(Constant.THIS_YEAR + "NoticePolling", TopicConstant.READONLY); // 공지사항 폴링

        // local DB insert
        for(Topic t : topic) topicDBHelper.insert(t.getName(), t.getMode());

        // remote DB insert && subscribe!
        final OnDataBaseInsertListener dataBaseInsertListener = new OnDataBaseInsertListener() {
            @Override
            public void onSuccess() {
                Log.i("DBInsert", "success");
            }

            @Override
            public void onDuplicated() {
                // 여기에 진입했다는 것은,
                // 아이디가 없는상태에서 acl에 있다는 소리임.
                Log.i("subscribe", "duplicated");
                __onFail();
            }

            @Override
            public void onFail() {
                Log.i("subscribe", "fail..");
                __onFail();
            }
        };

        // 서버DB에 저장하기 위해서 subscribe를 호출한다.
        tb.subscribe(new OnPostExecuteListener() {
            @Override
            public void onPostExecute(boolean err) {
                if(!err) callback.onSuccess();
                else callback.onFail();
            }
        }, dataBaseInsertListener, topic);

        ((InitSettingDialogActivity)ctx).setOnBackPressedListener(new OnBackPressedListener() {
            @Override
            public void onBack() {
                Log.i("back", "pressed");
                tb.destroy();
            }
        });
    }
    private void _startSubAPKDown(final OnResponseListener<String> callback){

        final String URL = PreferenceBuilder.getInstance(ctx).getSecuredPreference().getString(DefaultSettings.SUB_APK_URL, "");

        Log.i("url", URL);

        final InputStreamVolleyRequest r = new InputStreamVolleyRequest(Request.Method.GET, URL, new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                InputStream input = null;
                BufferedOutputStream output = null;

                try {
                    if (response != null ) {
                        String filename = URL.replaceAll("^.*\\/", "");

                        //covert response to input stream
                        input = new ByteArrayInputStream(response);
                        File path = ctx.getDir(Constant.SUB_APK_DIR, Context.MODE_PRIVATE);

                        File file = new File(path, filename);
                        Log.i("fileName", file.toString() + "/path:" + file.getAbsolutePath());

                        output = new BufferedOutputStream(new FileOutputStream(file));
                        byte data[] = new byte[1024], md5Bytes[];

                        // GENERATE CHECKSUM
                        MessageDigest digest = MessageDigest.getInstance("MD5");

                        int count;
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
                        Log.i("finally", "executed");
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

        VolleyQueue.getInstance(ctx).addObjectToQueue(r);

    }
    private void _loadDataFromServer(final OnResponseListener<ArrayList<Topic>> callback){

        final String url = Constant.HTTP + Constant.SERVER_URL + Constant.API_URL + Constant.CURRENT_API_VERSION + Constant.API_GET_TOPICS;

        StringRequest r = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // {"status":0,"msg":"success","data":"[{\"topic\":\"2016/members/leesnhyun\",\"chmod\":2},{\"topic\":\"2016/members/test\",\"chmod\":2}]"}
                    Log.i("loadDataFromServer", response);

                    // status 분석
                    JSONObject o =  new JSONObject(response);
                    int status = o.getInt("status");

                    //topic list 생성
                    if( status == 0 ) {
                        ArrayList<Topic> list = new ArrayList<>();
                        JSONArray data = new JSONArray(o.getString("data"));

                        for( int i=0; i<data.length(); i++ ){
                            JSONObject tmp = data.getJSONObject(i);
                            list.add(new Topic(tmp.getString("topic"), tmp.getInt("chmod")));
                        }

                        callback.onSuccess(list); return;
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

                final String did = PreferenceBuilder.getInstance(ctx)
                        .getSecuredPreference().getString("pref_device", "");

                params.put("userid", did);

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

        VolleyQueue.getInstance(ctx).addObjectToQueue(r);

    }
    private void _sendPushTest(final OnPostExecuteListener callback){

        final SecurePreferences spref = PreferenceBuilder.getInstance(ctx).getSecuredPreference();
        final String uid = spref.getString("pref_userid", "");

        final TopicBuilder tb = TopicBuilder.getInstance(ctx);

        // 개인토픽
        final String testTopic = Constant.THIS_YEAR + "members/" + uid;
        tb.publish(testTopic, 1, false, getString(R.string.mqtt_success_msg), new OnCallbackListener() {
            @Override
            public void onSuccess() {
                callback.onPostExecute(false);
            }

            @Override
            public void onFail() {
                callback.onPostExecute(true);
            }
        });
    }

    @Override
    public void __changeToNextStep(){
        // Fragment 변경
        ((InitSettingDialogActivity) ctx).replaceFragment(InitSettingDialogFragment5.newInstance());
    }
    public void __changeToPreviousStep(){
        // Fragment 변경
        ((InitSettingDialogActivity) ctx).replaceFragment(InitSettingDialogFragment3.newInstance());
    }


    private void __onFail(){
        loadingPrompt.setVisibility(View.GONE);
        Toast.makeText(ctx, getString(R.string.prompt_saving_failed), Toast.LENGTH_LONG).show();
        __changeToPreviousStep();
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
            final File optimizedDexOutputPath = ctx.getDir(Constant.SUB_APK_DIR, Context.MODE_PRIVATE);
            final String filename = PreferenceBuilder.getInstance(ctx)
                    .getSecuredPreference().getString(DefaultSettings.SUB_APK_URL, "").replaceAll("^.*\\/", "");

            // DexClassLoader to get the file and write it to the optimised directory
            DexClassLoader cl = new DexClassLoader(optimizedDexOutputPath.getPath()+"/"+filename,
                    optimizedDexOutputPath.getPath(), null, ctx.getClassLoader());

            Class<?> clz = cl.loadClass("happs.NH.Food.alarm.sub.SubClass");

            // MyTestClass has a constructor with no arguments
            Constructor<?> cons = clz.getConstructor();
            Object obj = cons.newInstance();

            Method m = clz.getMethod("getEvent", Context.class);
            boolean re = (boolean)m.invoke(obj, ctx);

            Method m2 = clz.getMethod("getVersionCode");
            int vc = (int)m2.invoke(obj);

            //Log.i("SUBVERSION_CODE", vc+"");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}