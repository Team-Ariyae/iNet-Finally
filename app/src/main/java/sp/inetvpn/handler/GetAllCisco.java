package sp.inetvpn.handler;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import sp.inetvpn.data.GlobalData;

/**
 * by MehrabSp
 */
public class GetAllCisco {

    public interface CiscoCallback {
        void onCiscoVResult(String retCiscoV);
    }

    public static void setRetCiscoV(Context context, CiscoCallback callback) {
        VolleySingleton volleySingleton = new VolleySingleton(context);
        StringRequest sr = new StringRequest(Request.Method.POST, GlobalData.ApiAdress,
                callback::onCiscoVResult,
                error -> callback.onCiscoVResult(null)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("query", GlobalData.ApiCiscoName);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", GlobalData.ApiKey);
                return params;
            }
        };
        volleySingleton.addToRequestQueue(sr);
    }
}
