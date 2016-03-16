package happs.NH.Food.alarm.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import happs.NH.Food.alarm.R;

/**
 * Created by SH on 2016-03-08.
 */
public class PushPopupActivity extends Activity {

    private TextView msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 나올얘는 fade_in, 기존얘는 애니메이션 없음.
        overridePendingTransition(R.anim.fadein, 0);
        setContentView(R.layout.activity_popup);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        // 객체생성
        msg = (TextView)findViewById(R.id.push_content);

        // 전달받자
        Intent i = getIntent();
        msg.setText(i.getExtras().getString("msg"));

    }

    @Override
    protected void onResume() {
        super.onResume();

        /* 5.5초후 닫기 */
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                // 나올얘는 애니메이션 없음, 사라질얘는 fade_out.
                overridePendingTransition(0, R.anim.fadeout);
            }
        }, 5500);
    }
}
