package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//Google Service Package
//Google Fit APIs are part of Google Play Services
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.Scopes;

import com.google.android.gms.fitness.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends ActionBarActivity {

    String TAG = "Google Service Test : ";

    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;
    RecordApiManager mRecordMgr;

    // for myClock View
    TimeThread mTimeThread;
    TextView mTVHour, mTVMinute, mTVSecond, mTVNoon, mTVSteps;

    SessionApiManager sessionMgr;
    Button mBtnSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Put application specific code here.

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        // set myClockView
        mTVHour = (TextView)findViewById(R.id.text_Hour);
        mTVMinute = (TextView)findViewById(R.id.text_Minute);
        mTVSecond = (TextView)findViewById(R.id.text_Second);
        mTVNoon = (TextView)findViewById(R.id.text_Noon);

        // set & start thread for clock
        mTimeThread = new TimeThread(mainHandler);
        mTimeThread.setDaemon(true);
        mTimeThread.start();

        mBtnSession = (Button)findViewById(R.id.btn_session);
        mTVSteps = (TextView)findViewById(R.id.text_Steps);
        buildFitnessClient();
    }

    Handler mainHandler = new Handler() {
        public void handleMessage (Message msg) {
            mTVHour.setText(String.valueOf(msg.what));
            mTVMinute.setText(String.valueOf(msg.arg1));
            mTVSecond.setText(String.valueOf(msg.arg2));

            if (msg.what <= 12)
                mTVNoon.setText(R.string.am);
            else
                mTVNoon.setText(R.string.pm);
        }
    };

    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");

                                // Now you can make calls to the Fitness APIs.
                                // Put application specific code here.
                                mRecordMgr.subscribeStep();
                                mRecordMgr.subscribeDistance();
                                //mRecordMgr.subscribeSpeed();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                    new GoogleApiClient.OnConnectionFailedListener() {
                        // Called whenever the API client fails to connect.
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Connection failed. Cause: " + result.toString());
                            if (!result.hasResolution()) {
                                // Show the localized error dialog
                                GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                        MainActivity.this, 0).show();
                                return;
                            }
                            // The failure has a resolution. Resolve it.
                            // Called typically when the app is not yet authorized, and an
                            // authorization dialog is displayed to the user.
                            if (!authInProgress) {
                                try {
                                    Log.i(TAG, "Attempting to resolve failed connection");
                                    authInProgress = true;
                                    result.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e(TAG, "Exception while starting resolution activity", e);
                                }
                            }
                        }
                    }
                )
                .build();

        mRecordMgr = new RecordApiManager(mClient, this);
        sessionMgr = new SessionApiManager(mClient);

        mClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS)
        {
            //실패
            GooglePlayServicesUtil.getErrorDialog(result, this, 0, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            }).show();
        }
        else
        {
            Toast.makeText(this, "service success", Toast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Connecting...");
        mClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mClient.isConnected()) {
            mClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    boolean clickFlag = false;
    public void mOnClick(View v) {

        if (v.getId() == R.id.btn_session) {
            if (!clickFlag) {
                clickFlag = true;
                mBtnSession.setText("Session start");
                sessionMgr.startSession();
            } else {
                clickFlag = false;
                mBtnSession.setText("click");
                sessionMgr.stopSession();
            }
        }
    }
}
