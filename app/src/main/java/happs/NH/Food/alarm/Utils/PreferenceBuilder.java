package happs.NH.Food.alarm.Utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by SH on 2016-03-17.
 */
public class PreferenceBuilder {

    private static Context ctx;

    private static PreferenceBuilder ourInstance = new PreferenceBuilder();

    public static PreferenceBuilder getInstance(Context context) {
        ctx = context;
        return ourInstance;
    }

    public ObscuredSharedPreferences getSecuredPreference(){
        return new ObscuredSharedPreferences(ctx,
                ctx.getSharedPreferences(Constant.ENCRYPT_PREFERENCE_NAME, 0));
    }

    public SharedPreferences getPreference(){
        return ctx.getSharedPreferences(Constant.NORMAL_PREFERENCE_NAME, 0);
    }

    private PreferenceBuilder() {
    }

}
