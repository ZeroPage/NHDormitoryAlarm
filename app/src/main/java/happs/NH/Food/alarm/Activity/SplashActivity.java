package happs.NH.Food.alarm.Activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import happs.NH.Food.alarm.Network.APKdownAsyncTask;
import happs.NH.Food.alarm.Network.OnPostExecuteListener;
import happs.NH.Food.alarm.Network.VolleyQueue;
import happs.NH.Food.alarm.R;
import happs.NH.Food.alarm.Utils.Constant;

/**
 * Created by SH on 2016-03-05.
 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final String INFO_URL = Constant.HTTP + Constant.SERVER_URL + Constant.ANDROID_INFO;

        StringRequest r =
                new StringRequest(Request.Method.GET, INFO_URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        VolleyQueue.getInstance(getApplicationContext()).addObjectToQueue(r);

        new APKdownAsyncTask(new OnPostExecuteListener(){

            @Override
            public void onSuccess() {
                test();
            }

            @Override
            public void onFail() {
                Log.i("Appdown", "fail");
            }
        }).execute("http://happs.gtz.kr/apk/Test.apk");

    }

    @SuppressWarnings("NewApi")
    private void test(){

        try {
            // optimized directory, the applciation and package directory
            final File optimizedDexOutputPath = getDir("outdex", 0);

            // DexClassLoader to get the file and write it to the optimised directory
            DexClassLoader cl = new DexClassLoader(Environment.getExternalStorageDirectory().getPath()+"/DexTest.apk",
                    optimizedDexOutputPath.getPath(), null, getClassLoader());

            Log.i("Path", Environment.getExternalStorageDirectory().getPath());
            Log.i("Path", optimizedDexOutputPath.getPath());

            Class<?> clz = cl.loadClass("happs.NH.Food.alarm.sub.SubClass");

            // MyTestClass has a constructor with no arguments
            Constructor<?> cons = clz.getConstructor();
            Object obj = cons.newInstance();

            Method m = clz.getMethod("getEvent", Context.class);


            int re = (int)m.invoke(obj, getApplicationContext());
            Log.i("class loading", re + "");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
