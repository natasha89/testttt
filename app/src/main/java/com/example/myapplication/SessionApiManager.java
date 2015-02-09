package com.example.myapplication;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SessionApiManager {

    private String TAG = "SessionApiManager";

    private GoogleApiClient mClient;

    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";

    Session session;
    int cnt = 0;
    boolean resultFlag;

    long startTime, endTime;
    String sessionName = "Runnable";

    public SessionApiManager (GoogleApiClient client) {
        this.mClient = client;
    }

    public void startSession() {
        cnt++;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        startTime = new Date().getTime();

        Session newSession = new Session.Builder()
                .setName(sessionName)
                .setIdentifier(dateFormat.format(new Date()) + " " + String.valueOf(cnt))
                .setDescription("Runnable " + dateFormat.format(new Date()) + " " + String.valueOf(cnt))
                .setStartTime(new Date().getTime(), TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.RUNNING)
                .build();

        session = newSession;

        Fitness.SessionsApi.startSession(mClient, session)
                .setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if(status.isSuccess()) {
                    resultFlag = true;
                    Log.i(TAG, "Successfully start session " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                } else {
                    resultFlag = false;
                    Log.i(TAG, "Failed to start session " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                }
            }
        });
    }

    public void stopSession() {

        Fitness.SessionsApi.stopSession(mClient, session.getIdentifier())
                .setResultCallback(new ResultCallback<SessionStopResult>() {
            @Override
            public void onResult(SessionStopResult sessionStopResult) {

                for (Session session : sessionStopResult.getSessions()) {

                    Log.i(TAG, "Successfully stop session");
                    Log.i(TAG,  new SimpleDateFormat("HH:mm:ss").format(session.getStartTime(TimeUnit.MILLISECONDS))
                            + " ~ "
                            + new SimpleDateFormat("HH:mm:ss").format(session.getEndTime(TimeUnit.MILLISECONDS)));

                    readSessionData(session.getStartTime(TimeUnit.MILLISECONDS), session.getEndTime(TimeUnit.MILLISECONDS));
                }
            }
        });
    }

    public void readSessionData(long startTime, long endTime) {
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .setSessionName(sessionName)
                .build();

            Fitness.SessionsApi.readSession(mClient, readRequest).setResultCallback(new ResultCallback<SessionReadResult>() {
                @Override
                public void onResult(SessionReadResult sessionReadResult) {

                    Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                            + sessionReadResult.getSessions().size());
                    for (Session session : sessionReadResult.getSessions()) {
                        // Process the session
                        dumpSession(session);

                        // Process the data sets for this session
                        List<DataSet> dataSets = sessionReadResult.getDataSet(session);
                        for (DataSet dataSet : dataSets) {
                            dumpDataSet(dataSet);
                        }
                    }
                }
            }
        );
    }

    private void dumpDataSet(DataSet dataSet) {
        String TAG = "dumpDataSet";

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
                    + " / " + dp.getDataType().getFields().get(0).getName()
                    + " : " + dp.getValue(dp.getDataType().getFields().get(0)));
        }
    }

    private void dumpSession(Session session) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    }
}
