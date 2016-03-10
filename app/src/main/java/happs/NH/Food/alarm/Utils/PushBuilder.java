package happs.NH.Food.alarm.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import happs.NH.Food.alarm.R;

/**
 * Created by SH on 2016-03-08.
 */
public class PushBuilder {

    public int NUM = 0;

    public void makeNotification(Context ctx, Intent i, String msg){

        if( msg == null ) return;

        NotificationManager mNotificationManager = (NotificationManager)
                ctx.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        //PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, i, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setTicker("농협장학관에서 알려드립니다.")
                        .setContentTitle("농협장학관")
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        //mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(++NUM, mBuilder.build());
    }

    public void makePopupNotification(Context ctx, Intent i, String msg){

        i.putExtra("msg", msg);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_ONE_SHOT);

        try {
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            Log.i("PendingIntent", "cancel Expetion");
        }

    }

}
