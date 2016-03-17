package happs.NH.Food.alarm.DialogFragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import happs.NH.Food.alarm.R;

/**
 * Created by SH on 2016-03-17.
 */

public class InitSettingDialog extends DialogFragment {

    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // dialog setting
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        
        // inflate
        view = inflater.inflate(R.layout.fragment_dialog_init, container);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        double width = this.getResources().getDisplayMetrics().widthPixels * 0.9;
        double height = this.getResources().getDisplayMetrics().heightPixels * 0.9;
        getDialog().getWindow().setLayout((int)width, (int)height);
    }
}
