package happs.NH.Food.alarm.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import happs.NH.Food.alarm.DialogFragment.InitSettingDialog;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Service.MQTTService;

/**
 * Created by SH on 2016-03-16.
 */
public class InitialSettingActivity extends AppCompatActivity {

    private Button btn_init_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_layout_1);

        // 이동 애니메이션
        overridePendingTransition(0, 0);

        // initialize
        _createObjects();
        _initActions();

        startService(new Intent(InitialSettingActivity.this, MQTTService.class));

    }

    private void _createObjects(){
        btn_init_start = (Button)findViewById(R.id.btn_init_start);
    }

    private void _initActions(){
        btn_init_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InitSettingDialog isd = new InitSettingDialog();
                isd.show(getSupportFragmentManager(), "initial Setting");
            }
        });
    }

}
