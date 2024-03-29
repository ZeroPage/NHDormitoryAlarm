package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Interfaces.OnResponseListener;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Utils.Constant;
import happs.NH.Food.alarm.Utils.PreferenceBuilder;

/**
 * Created by SH on 2016-03-20.
 */
public class InitSettingDialogFragment1 extends Fragment {

    private EditText userid, userpw;
    private Button btnLogin;
    private Context ctx;

    public static InitSettingDialogFragment1 newInstance() {
        return new InitSettingDialogFragment1();
    }
    
    @Override
    public void onAttach(Context context) {
        this.ctx = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init_setting_step1, container, false);

        // object allocation
        userid = (EditText)view.findViewById(R.id.jaedan_id);
        userpw = (EditText)view.findViewById(R.id.jaedan_pw);
        btnLogin = (Button)view.findViewById(R.id.btn_login);

        // attach actions
        _initActions();

        return view;
    }

    private void _initActions(){
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String uid = userid.getText().toString();
                final String upw = userpw.getText().toString();

                // 잠시 버튼 정지 && 프롬포트 안내
                btnLogin.setText("로그인 중입니다.");
                btnLogin.setEnabled(false);

                // 로그인 시도
                __doLogin(uid, upw, new OnResponseListener<String>() {
                    @Override
                    public void onSuccess(String response) {
                        ___changeToNextStep(response, uid, upw);
                    }

                    @Override
                    public void onFail() {
                        Toast.makeText(ctx, "로그인에 실패하였습니다.", Toast.LENGTH_LONG).show();
                        btnLogin.setEnabled(true); btnLogin.setText("로그인");
                    }
                });
            }
        });
    }

    private void __doLogin(final String id, final String pw, final OnResponseListener<String> callback){

        StringRequest r = new StringRequest(Request.Method.POST, Constant.JAEDAN_LOGIN_URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if( ___getLoginStatus(response) ){
                            callback.onSuccess(response.split("__;__")[0]);
                        } else {
                            callback.onFail();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ctx, getString(R.string.msg_login_fail), Toast.LENGTH_LONG).show();
                        callback.onFail();
                    }
                }){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("userid", id);
                params.put("password", pw);
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String utf8String = new String(response.data, "UTF-8");
                    String sessionId = response.headers.get("Set-Cookie");
                    String result = sessionId+"__;__"+utf8String;

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

    private void ___changeToNextStep(String cookie, String uid, String upw){

        // sharedPreference 에 정보 저장
        PreferenceBuilder
                .getInstance(ctx)
                .getSecuredPreference().edit()
                .putString("pref_cookie", cookie)
                .putString("pref_userid", uid)
                .putString("pref_userpw", upw)
                .apply();

        // Fragment 변경
        ((InitSettingDialogActivity)ctx).replaceFragment(InitSettingDialogFragment2.newInstance());

    }

    private boolean ___getLoginStatus(String source){
        return !(source.contains("alert") || source.contains("error"));
    }

}