package checkout.checkout_android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.checkout.android_sdk.Models.BillingModel;
import com.checkout.android_sdk.Models.PhoneModel;
import com.checkout.android_sdk.PaymentForm;
import com.checkout.android_sdk.PaymentForm.PaymentFormCallback;
import com.checkout.android_sdk.Response.CardTokenisationFail;
import com.checkout.android_sdk.Response.CardTokenisationResponse;
import com.checkout.android_sdk.Utils.CardUtils.Cards;
import com.checkout.android_sdk.Utils.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DemoActivity extends Activity {

    //Include the module in your class
    private PaymentForm mPaymentForm;
    private ProgressDialog mProgressDialog;
    private String redirectUrl;




    // Callback used for the Payment Form interaction
    private final PaymentFormCallback mFormListener = new PaymentFormCallback() {
        @Override
        public void onFormSubmit() {
            mProgressDialog.show(); // show loader
        }

        @Override
        public void onTokenGenerated(final CardTokenisationResponse response) {
            mProgressDialog.dismiss(); // dismiss the loader
            mPaymentForm.clearForm(); // clear the form
            displayMessage("Token", response.getToken());

            makePaymentRequest(response.getToken());
        }

        @Override
        public void onError(CardTokenisationFail response) {
            displayMessage("Token Error", response.getErrorType());
        }

        @Override
        public void onNetworkError(VolleyError error) {
            displayMessage("Network Error", String.valueOf(error));
        }

        @Override
        public void onBackPressed() {
            displayMessage("Back", "The user decided to leave the payment page.");
        }
    };

    PaymentForm.On3DSFinished m3DSecureListener =
        new PaymentForm.On3DSFinished() {
            @Override
            public void onSuccess(String sessionID) {
                // success
                Log.d("CKO_3DS - 3ds success", sessionID);
                Intent successIntent = new Intent (DemoActivity.this, SuccessPageActivity.class);
                successIntent.putExtra("SESSION_ID", sessionID);
                startActivity(successIntent);
            }
            @Override
            public void onError(String errorMessage) {
                // fail
                Log.d("CKO_3DS - 3ds error", errorMessage);
                Intent failIntent = new Intent (DemoActivity.this, FailPageActivity.class);
                startActivity(failIntent);
            }
        };



    private void makePaymentRequest(String token) {
        //Post request
        String url = "https://api.sandbox.checkout.com/payments";
        RequestQueue queue = Volley.newRequestQueue(DemoActivity.this);

        String reqString =
                "{\"source\": " +
                        "{\"type\": \"token\", " +
                        "\"token\": \""+token+"\"}, " +
                "\"3ds\": {\"enabled\": \"true\"},"+
                "\"amount\": 1700, " +
                "\"currency\": \"GBP\", " +
                "\"reference\": \"android\"}";

        JSONObject reqJson = null;
        try {
            reqJson = new JSONObject(reqString);
        }catch (JSONException err){
            Log.d("Error", err.toString());
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST,
                url, reqJson,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String response_value = response.toString();
                        JSONObject respJson = null;

                        try {
                            respJson =  new JSONObject(response_value);
                            Log.i("CKO_3DS - Status", respJson.get("status").toString());

                            if( respJson.get("status").toString().equals("Pending")) { //3DS
                                JSONObject linksJson = new JSONObject(respJson.get("_links").toString());
                                JSONObject redirectJson = new JSONObject(linksJson.get("redirect").toString());
                                redirectUrl = redirectJson.get("href").toString();
                                Log.i("CKO_3DS - Href", redirectUrl);

                                mPaymentForm = findViewById(R.id.checkout_card_form);
                                mPaymentForm.set3DSListener(m3DSecureListener); // pass the callback

                                mPaymentForm.handle3DS(
                                        redirectUrl, // the 3D Secure URL
                                        "http://manos.com/success",// the Redirection URL
                                        "http://manos.com/fail" // the Redirection Fail URL
                                );
                            } else if( respJson.get("approved").toString().equals( "true" )) {
                                JSONObject riskJson = new JSONObject(respJson.get("risk").toString());
                                Log.i("CKO_3DS - Risk Flag", riskJson.get("flagged").toString());
                                if (riskJson.get("flagged").toString().equals("true")){
                                    Toast.makeText(DemoActivity.this, "Payment Flagged", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(DemoActivity.this, "Payment Successful", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(DemoActivity.this, "Payment Declined : " +
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
        queue.add(postRequest);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        // initialise the loader
        mProgressDialog = new ProgressDialog(DemoActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("Loading...");

        mPaymentForm = findViewById(R.id.checkout_card_form);

        mPaymentForm
                .setFormListener(mFormListener)
                .setEnvironment(Environment.SANDBOX)
                .setKey("pk_test_7d9921be-b71f-47fa-b996-29515831d911")
                .setDefaultBillingCountry(Locale.UK);

    }

    private void displayMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}