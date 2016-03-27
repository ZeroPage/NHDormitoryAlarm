package happs.NH.Food.alarm.Interfaces;

/**
 * Created by SH on 2016-03-27.
 */
public interface OnDataBaseInsertListener {

    void onSuccess();
    void onDuplicated();
    void onFail();

}
