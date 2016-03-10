package happs.NH.Food.alarm.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by SH on 2016-03-08.
 */
public class MQTTServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, MQTTService.class);
            context.startService(i);
        }

        if(intent.getAction().equals("ACTION.RESTART.MQTTService")){
            Log.i("RestartService", "ACTION_RESTART_PERSISTENTSERVICE");
            Intent i = new Intent(context, MQTTService.class);
            context.startService(i);
        }
    }
}
