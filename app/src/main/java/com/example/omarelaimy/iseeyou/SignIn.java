package com.example.omarelaimy.iseeyou;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;



/**
 * Created by Omar on 3/15/2017.
 */

public class SignIn extends AppCompatActivity {
    private TextView btnSignUp;
    private Button btnSignIn;
    private View EmailSeparator,PasswordSeparator;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private EditText Email,Password;
    private static final String TAG = "LoginActivity";
    private static final String URL_FOR_LOGIN = "https://icu.000webhostapp.com/login.php";
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
           getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        changeStatusBarColor();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);
        //Get the sign up button to navigate to the sign up page//
        btnSignUp = (TextView) findViewById(R.id.signup1);

        //Get the sign in button
        btnSignIn = (Button) findViewById(R.id.sign_in);

        //Get the separator lines
        EmailSeparator =  (View) findViewById(R.id.email_separator);
        PasswordSeparator = (View) findViewById(R.id.password_separator);

        //Get the Email and password texts
        Email = (EditText) findViewById(R.id.email_text);
        Password = (EditText) findViewById(R.id.password_text);

        //Progress dialog to show when accessing the database
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        ChangeSeparatorStatus(Email,EmailSeparator);
        ChangeSeparatorStatus(Password,PasswordSeparator);

        //If Sign up is pressed, go to sign up page
     btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSignUp();
               // startActivity(new Intent(SignIn.this, CreateProfile.class));
               // finish();
            }
        });

    //If Sign in is pressed, check credentials if good, go to choose profile
    btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                loginUser(Email.getText().toString(), Password.getText().toString());

            }
        });
    }

    private void loginUser( final String email, final String password)
    {
        final String token = FirebaseInstanceId.getInstance().getToken();
        // Tag used to cancel the request
        String cancel_req_tag = "login";
        progressDialog.setMessage("Logging you in...");
        showDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST, URL_FOR_LOGIN, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Login Response: " + response.toString());
                hideDialog();
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    if (!error)
                    {
                        String user = jObj.getJSONObject("user").getString("name");
                        String userID = jObj.getJSONObject("user").getString("userID");



                        //Service for Heart Rate
                        Intent heartrateintent= new Intent(getApplicationContext(), NotificationHeartService.class);
                        heartrateintent.putExtra("caregiverid", userID);

                        //Service for Slots not taken and pills running out
                        Intent slotspillintent= new Intent(getApplicationContext(), NotificationSlotPillService.class);
                        slotspillintent.putExtra("caregiverid", userID);


                        //Set the Alarm timer for the heart rate every 5 minutes
                       SetHeartAlarmTimer(heartrateintent);
                        SetSlotPillAlarmTimer(slotspillintent);

                        Config.CAREGIVERID = userID;
                        // Launch Choose profile activity
                       Intent intent = new Intent(SignIn.this,ChooseProfile.class);
                        //Send parameters to the ChooseProfile Activity
                        Bundle extras = new Bundle();
                        extras.putString("caregiver_name",user);
                        extras.putString("caregiver_id",userID);
                        extras.putString("caregiver_email",Email.getText().toString());
                        intent.putExtras(extras);
                        startActivity(intent);
                        //finish();
                    }
                    else
                    {
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("password", password);
                params.put("token", token);
                return params;
            }
        };
        // Adding request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(strReq,cancel_req_tag);
    }

    //Function for showing the progress dialog
    private void showDialog()
    {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }
    //Function for hiding the progress dialog
    private void hideDialog()
    {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void ChangeSeparatorStatus(final EditText editText, final View view)
    {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    view.setBackgroundColor(getResources().getColor(R.color.SeparatorFocused));
                }
                else
                {
                    view.setBackgroundColor(getResources().getColor(R.color.SeparatorColor));
                }
            }
        });
    }
    //Launch SignUp Page
    private void launchSignUp()
    {
        startActivity(new Intent(SignIn.this, SignUp.class));
       // finish();
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.NotificationBar));
        }
    }

    private void SetSlotPillAlarmTimer(Intent intent)
    {
        //Starting a service with alarm manager
        Calendar cur_cal = Calendar.getInstance();
        cur_cal.setTimeInMillis(System.currentTimeMillis());
        cur_cal.add(Calendar.SECOND, 10);
        Config.SlotPill_PENDING_INTENT = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
        //Config.SlotPill_PENDING_INTENT =  PendingIntent.getBroadcast(getApplicationContext(), 0, intent,0);
        Config.SlotPill_ALARM_MANAGER = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Config.SlotPill_ALARM_MANAGER.set(AlarmManager.RTC, cur_cal.getTimeInMillis(), Config.SlotPill_PENDING_INTENT);
        Config.SlotPill_ALARM_MANAGER.setRepeating(AlarmManager.RTC_WAKEUP, cur_cal.getTimeInMillis(), Config.slotspillmillisecs, Config.SlotPill_PENDING_INTENT);
        getApplicationContext().startService(intent);
    }

    private void SetHeartAlarmTimer(Intent intent)
    {
        //Starting a service with alarm manager
        Calendar cur_cal = Calendar.getInstance();
        cur_cal.setTimeInMillis(System.currentTimeMillis());
        cur_cal.add(Calendar.SECOND, 10);
        Config.HEART_PENDING_INTENT = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
        //Config.HEART_PENDING_INTENT = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        Config.HEART_ALARM_MANAGER = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Config.HEART_ALARM_MANAGER.set(AlarmManager.RTC, cur_cal.getTimeInMillis(), Config.HEART_PENDING_INTENT);
        Config.HEART_ALARM_MANAGER.setRepeating(AlarmManager.RTC_WAKEUP, cur_cal.getTimeInMillis(), Config.heartmillisecs, Config.HEART_PENDING_INTENT);
        getApplicationContext().startService(intent);
    }


}