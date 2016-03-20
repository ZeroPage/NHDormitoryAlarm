package happs.NH.Food.alarm.Network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.Volley;

/**
 * Created by SH on 2016-03-08.
 */
public class VolleyQueue {

    private static VolleyQueue volleyQueue;
    private static RequestQueue requestQueue;
    private static Context ctx;

    private VolleyQueue(Context ctx){
        VolleyQueue.ctx = ctx;
    }

    public static synchronized VolleyQueue getInstance(Context ctx){
        if( volleyQueue == null ){
            volleyQueue = new VolleyQueue(ctx);
        }

        return volleyQueue;
    }

    public RequestQueue getRequestQueue(){
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public RequestQueue getRequestQueue(HttpStack httpStack){
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext(), httpStack);
        }
        return requestQueue;
    }


    public void addObjectToQueue(Request reqObj){
        this.getRequestQueue().add(reqObj);
    }

}
