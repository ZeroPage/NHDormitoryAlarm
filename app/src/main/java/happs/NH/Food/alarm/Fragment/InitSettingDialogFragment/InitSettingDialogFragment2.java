package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Interfaces.OnStepChangeListener;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Utils.Constant;

/**
 * Created by SH on 2016-03-20.
 */
public class InitSettingDialogFragment2 extends Fragment implements OnStepChangeListener {

    private WebView termsWebView;
    private CheckBox termsAcceptCheck;

    public static InitSettingDialogFragment2 newInstance() {
        return new InitSettingDialogFragment2();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init_setting_step2, container, false);

        // object allocation
        termsWebView = (WebView)view.findViewById(R.id.TermsWebView);
        termsAcceptCheck = (CheckBox)view.findViewById(R.id.termsAcceptCheckBox);

        // initialize
        _init();
        _setDefaultActions();

        return view;
    }

    private void _init(){

        // webview 설정 및 초기화
        termsWebView.getSettings().setDefaultTextEncodingName("UTF-8");
        termsWebView.loadUrl(Constant.TERMS_URL);

    }

    private void _setDefaultActions(){
        termsAcceptCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    __changeToNextStep();
                }

            }
        });
    }

    @Override
    public void __changeToNextStep(){

        // Fragment 변경
        ((InitSettingDialogActivity)getActivity())
                .replaceFragment(InitSettingDialogFragment3.newInstance());

    }

}
