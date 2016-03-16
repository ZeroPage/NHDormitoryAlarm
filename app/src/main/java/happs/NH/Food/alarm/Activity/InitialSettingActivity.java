package happs.NH.Food.alarm.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Service.MQTTService;

/**
 * Created by SH on 2016-03-16.
 */
public class InitialSettingActivity extends Activity {

    private Button btn_init_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_layout_1);

        btn_init_start = (Button)findViewById(R.id.btn_init_start);

        startService(new Intent(InitialSettingActivity.this, MQTTService.class));

    }
}
