//package sp.inetvpn.api;
//
//import android.content.Context;
//import android.util.Log;
//
//import com.android.volley.AuthFailureError;
//import com.android.volley.Request;
//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.StringRequest;
//import com.android.volley.toolbox.Volley;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import sp.inetvpn.data.GlobalData;
//
//public class SendFeedback {
//
//    //feedback,more,email
//
//    public static void sendFeedBack(Context context, String feedback, String more, String email) {
//        RequestQueue queue = Volley.newRequestQueue(context);
//        StringRequest sr = new StringRequest(Request.Method.POST, GlobalData.ApiAdress,
//                response -> {
//                    Log.d("RESSS Feed", response);
//                },
//                error -> {
//                    //
//                }) {
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("query", GlobalData.ApiFeedBack);
//                params.put("feedback", feedback);
//                params.put("more", more);
//                params.put("email", email);
//                return params;
//            }
//
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("Authorization", GlobalData.ApiKey);
//                return params;
//            }
//        };
//        queue.add(sr);
//    }
//}
