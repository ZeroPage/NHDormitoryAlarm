package happs.NH.Food.alarm.Utils;

/**
 * Created by SH on 2016-03-09.
 */
public interface Constant {

    ///// 공통 사항 /////
    String CURRENT_API_VERSION = "1";
    String SALT = "leesnhyun123";

    String ENCRYPT_PREFERENCE_NAME = "pref1";
    String NORMAL_PREFERENCE_NAME = "pref2";
    String DEVICE_TYPE = "a";

    String HTTP = "http://";
    String SERVER_URL = "leesnhyun.iptime.org";
    String ANDROID_INFO = "/android/info.json";

    String API_URL = "/api/";
    String API_REGISTER = "/reg";
    String API_SUBSCRIBE = "/subscribe";
    String API_UNSUBSCRIBE = "/unsubscribe";

    ///// 로그인 /////
    String JAEDAN_LOGIN_URL = "http://jaedan.nonghyup.com/site/mobile/login_proc.asp";

    ///// 개인정보수집동의 /////
    String TERMS_URL = "file:///android_asset/terms.html";

    ///// 사용자확인 /////
    String JAEDAN_OVERNIGHT_URL = "http://jaedan.nonghyup.com/site/mobile/sub03_write.asp";

    ///// MQTT TOPIC /////
    String SERVICE_NAME = "happs.NH.Food.alarm.Service.MQTTService";
    String TEST_TOPIC = "welcome";

}
