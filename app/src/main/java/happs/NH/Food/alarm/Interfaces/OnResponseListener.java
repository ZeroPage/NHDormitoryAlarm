package happs.NH.Food.alarm.Interfaces;

/**
 * Created by SH on 2016-03-21.
 */
public interface OnResponseListener<T> {

    void onSuccess(T response);
    void onFail();

}
