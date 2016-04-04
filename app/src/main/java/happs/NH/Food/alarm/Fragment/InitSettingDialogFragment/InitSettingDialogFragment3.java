package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Interfaces.OnResponseListener;
import happs.NH.Food.alarm.Interfaces.OnStepChangeListener;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Utils.Constant;
import happs.NH.Food.alarm.Utils.PreferenceBuilder;


/**
 * Created by SH on 2016-03-20.
 */
public class InitSettingDialogFragment3 extends Fragment implements OnStepChangeListener {

    private EditText userRoom, userExtra;
    private LinearLayout loadingPrompt;
    private Button btnConfirm;
    private Context ctx;

    public static InitSettingDialogFragment3 newInstance() {
        return new InitSettingDialogFragment3();
    }

    @Override
    public void onAttach(Context context) {
        this.ctx = context;
        super.onAttach(context);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init_setting_step3, container, false);

        // object allocation
        userRoom = (EditText)view.findViewById(R.id.et_roomNumber);
        userExtra = (EditText)view.findViewById(R.id.et_extra);
        loadingPrompt = (LinearLayout)view.findViewById(R.id.loadingPrompt);
        btnConfirm = (Button)view.findViewById(R.id.btn_confirm);

        // load Data
        _getUserInformation(new OnResponseListener() {
            @Override
            public void onSuccess(Object response) {
                if(getActivity() != null) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_loading_success), Toast.LENGTH_LONG).show();
                }
                userRoom.setText((String)response); userRoom.setEnabled(false);
                userExtra.requestFocus(); btnConfirm.setEnabled(true);
            }

            @Override
            public void onFail() {
                if(getActivity() != null) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_loading_failed), Toast.LENGTH_LONG).show();
                }
                userRoom.requestFocus();
            }
        });

        _setDefaultActions();

        return view;
    }

    private void _getUserInformation(final OnResponseListener callback){

        StringRequest r = new StringRequest(Request.Method.GET, Constant.JAEDAN_OVERNIGHT_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String result = __parseUserData(response);

                if( result != null ){
                    callback.onSuccess(result);
                } else {
                    callback.onFail();
                }

                loadingPrompt.setVisibility(View.GONE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onFail();
                loadingPrompt.setVisibility(View.GONE);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> result = new HashMap<>();
                String cookie = PreferenceBuilder.getInstance(ctx.getApplicationContext())
                        .getSecuredPreference().getString("pref_cookie", "");
                result.put("Cookie", cookie);

                return result;
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

    private void _setDefaultActions(){

        // 확인버튼 (비)활성화 리스너등록
        userRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if( userRoom.getText().toString().length() >= 3 ){
                    btnConfirm.setEnabled(true);
                } else {
                    btnConfirm.setEnabled(false);
                }
            }
        });

        // 확인버튼에 리스너등록
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                __changeToNextStep();
            }
        });

    }

    private String __parseUserData(String src){
        Source source = new Source(src);

        try {
            // 첫번째 <table> -> 첫번째 <tr> -> 두번째<td>
            Element table = source.getFirstElement(HTMLElementName.TABLE);
            Element tr = table.getFirstElement(HTMLElementName.TR);
            Element td = tr.getAllElements(HTMLElementName.TD).get(1);

            return td.getContent().toString().replace("호","").trim();

        } catch (Exception e) {
            Log.i("Exception", "at the ParseUserData");
        }

        return null;
    }

    @Override
    public void __changeToNextStep(){

        // Device ID를 이 단계에서 생성해서 저장하자
        final String dId = android.provider.Settings.Secure
                .getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);

        // SecuredSharedPreference 정보저장
        PreferenceBuilder
                .getInstance(ctx)
                .getSecuredPreference().edit()
                .putString("pref_roomNumber", userRoom.getText().toString())
                .putString("pref_device", dId)
                .apply();

        // SharedPreference 에 정보저장
        PreferenceBuilder
                .getInstance(ctx)
                .getPreference().edit()
                .putString("pref_extra_info", userExtra.getText().toString())
                .apply();

        // Fragment 변경
        ((InitSettingDialogActivity) ctx).replaceFragment(InitSettingDialogFragment4.newInstance());

    }

}
