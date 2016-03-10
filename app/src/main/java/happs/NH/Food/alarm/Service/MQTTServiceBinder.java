package happs.NH.Food.alarm.Service;

import android.os.Binder;

/**
 * Created by SH on 2016-03-07.
 */
public class MQTTServiceBinder extends Binder {

    private MQTTService mqttService;

    public MQTTServiceBinder(MQTTService mqttService) {
        this.mqttService = mqttService;
    }

    public MQTTService getService() {
        return mqttService;
    }

}
