package happs.NH.Food.alarm.Activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;

import happs.NH.Food.alarm.Fragment.InitSettingDialogFragment.InitSettingDialogFragment1;
import happs.NH.Food.alarm.R;

/**
 * Created by SH on 2016-03-20.
 */
public class InitSettingDialogActivity extends AppCompatActivity {

    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 애니메이션 없음.
        overridePendingTransition(android.R.anim.fade_in, 0);
        setContentView(R.layout.activity_init_setting);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // 첫 Fragment 고정.
        fm = getSupportFragmentManager();

        fm.beginTransaction()
                .replace(R.id.init_fragment_holder, InitSettingDialogFragment1.newInstance())
                .commit();
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event){
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();overridePendingTransition(0, R.anim.fadeout);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void replaceFragment(Fragment fragment){
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.init_fragment_holder, fragment);
        t.commit();
    }

}
