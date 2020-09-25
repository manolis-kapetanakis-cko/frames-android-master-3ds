package checkout.checkout_android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SuccessPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_page);

        String sessionId = getIntent().getStringExtra("SESSION_ID");
        Log.i("CKO_3DS - SessionID", sessionId);
        getDetails(sessionId);

    }

    private void getDetails(String sessionId) {
        //Get request
        String url = "https://api.sandbox.checkout.com/payments/" + sessionId;
        RequestQueue queue = Volley.newRequestQueue(SuccessPageActivity.this);

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String response_value = response.toString();
                        Log.i("CKO_3DS - Risk Flag", response_value);

                        try {
                            JSONObject respJson =  new JSONObject(response_value);
                            Log.i("CKO_3DS - Status", respJson.get("status").toString());
                            Toast.makeText(SuccessPageActivity.this, "Payment Successful : ", Toast.LENGTH_SHORT).show();
                            setContentView(R.layout.activity_success_page);

                            if( respJson.get("approved").toString().equals( "true" )) {
                                TextView tv_status = findViewById(R.id.payment_status);
                                tv_status.setText(respJson.get("status").toString());

                                TextView tv_pid = findViewById(R.id.payment_id);
                                tv_pid.setText(respJson.get("id").toString());
                            } else {
                                Toast.makeText(SuccessPageActivity.this, "Payment Declined : " +
                                        respJson.get("response_summary").toString(), Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("CKO_3DS - JSONPost", "Error: " + error.getMessage());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                params.put("Authorization","sk_test_07fa5e52-3971-4bab-ae6b-a8e26007fccc\n");
                return params;
            }
        };
        queue.add(getRequest);
    }
}