package happs.NH.Food.alarm.Fragment.InitSettingDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import happs.NH.Food.alarm.Activity.InitSettingDialogActivity;
import happs.NH.Food.alarm.Interfaces.OnStepChangeListener;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Utils.PreferenceBuilder;

/**
 * Created by SH on 2016-03-20.
 */
public class InitSettingDialogFragment5 extends Fragment implements OnStepChangeListener {

    private Button btnConfirm;
    private Context ctx;

    public static InitSettingDialogFragment5 newInstance() {
        return new InitSettingDialogFragment5();
    }

    @Override
    public void onAttach(Context context) {
        this.ctx = context;
        super.onAttach(context);
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init_setting_step5, container, false);

        // object allocation
        btnConfirm = (Button)view.findViewById(R.id.btn_confirm);

        // initialize
        _setDefaultActions();

        return view;
    }

    private void _setDefaultActions(){
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                __changeToNextStep();
            }
        });
    }

    @Override
    public void __changeToNextStep(){
        // 첫실행을 false로
        PreferenceBuilder pb = PreferenceBuilder.getInstance(ctx);
        pb.getSecuredPreference().edit().putBoolean("pref_isFirstVisit", false).apply();

        // activity 종료
        ((InitSettingDialogActivity)ctx).finishActivity();
    }

}
