package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class RecordApiManager {
    private String TAG = "RecordApiManager";

    private GoogleApiClient mClient;
    private Context mContext;

    HistoryApiManager mHistoryMgr;

    DataType mIinputType, mOutputType;

    final DataType step = DataType.TYPE_STEP_COUNT_DELTA;
    final DataType distance = DataType.TYPE_DISTANCE_DELTA;
    final DataType speed = DataType.TYPE_SPEED;
    final DataType aggregateStep = DataType.AGGREGATE_STEP_COUNT_DELTA;
    final DataType aggregateDistance = DataType.AGGREGATE_DISTANCE_DELTA;
    final DataType aggregateSpeed = DataType.AGGREGATE_SPEED_SUMMARY;

    public RecordApiManager (GoogleApiClient client, Context context) {
        this.mClient = client;
        this.mContext = context;

        mHistoryMgr = new HistoryApiManager(mClient, mContext);
    }

    public void subscribeStep() {

        if (mClient != null) {
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status status) {
                       if (status.isSuccess()) {
                           if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                               Log.i(TAG, "Step / " + "Existing subscription for activity detected.");
                           } else {
                               Log.i(TAG, "Step / " + "Successfully subscribed!");
                           }

                           mHistoryMgr.queryFitnessData(step);
                           mHistoryMgr.queryAggregateFitnessData(step, aggregateStep);

                           SessionApiManager sessionMgr = new SessionApiManager(mClient);

                           //^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                           Calendar calStart = new GregorianCalendar(2015, 01, 06, 17, 01, 30);
                           Calendar calEnd = new GregorianCalendar(2015, 01, 06, 17, 03, 28);

                           long sessionStart = calStart.getTimeInMillis();
                           long sessionEnd = calEnd.getTimeInMillis();

                           SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
                           Log.i("***********************", dateFormat.format(sessionStart) + " " +  dateFormat.format(sessionEnd));

                           sessionMgr.readSessionData(sessionStart, sessionEnd);
                       } else {
                           Log.i(TAG, "Step / " + "There was a problem subscribing. -> " + status.getStatusMessage());
                       }
                    }
                }
            );
        }
    }

    public void subscribeDistance() {

        if (mClient != null) {
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_DISTANCE_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {

                       @Override
                       public void onResult(Status status) {
                           if (status.isSuccess()) {
                               if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                   Log.i(TAG, "Distance / " + "Existing subscription for activity detected.");
                               } else {
                                   Log.i(TAG, "Distance / " +"Successfully subscribed!");
                               }

                               mHistoryMgr.queryFitnessData(distance);
                               mHistoryMgr.queryAggregateFitnessData(distance, aggregateDistance);
                           } else {
                               Log.i(TAG, "Distance / " +"There was a problem subscribing. -> " + status.getStatusMessage());
                           }
                       }
                   }
            );
        }
    }

    public void subscribeSpeed() {

        if (mClient != null) {
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_SPEED)
                    .setResultCallback(new ResultCallback<Status>() {

                           @Override
                           public void onResult(Status status) {
                               if (status.isSuccess()) {
                                   if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                       Log.i(TAG, "Speed / " + "Existing subscription for activity detected.");
                                   } else {
                                       Log.i(TAG, "Speed / " + "Successfully subscribed!");
                                   }

                                   mHistoryMgr.queryFitnessData(speed);
                                   mHistoryMgr.queryAggregateFitnessData(speed, aggregateSpeed);
                               } else {
                                   Log.i(TAG, "Speed / " + "There was a problem subscribing. -> " + status.getStatusMessage());
                               }
                           }
                       }
                    );
        }
    }

    public void getListSubscription() {
        Fitness.RecordingApi.listSubscriptions(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {

                                       @Override
                                       public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                                           for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                                               DataType dt = sc.getDataType();
                                               Log.i(TAG, "Active subscription for data type: " + dt.getName());
                                           }
                                       }
                                   }
                );
    }

    public void unsubscribe() {
        Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {

                                       @Override
                                       public void onResult(Status status) {
                                           String dataTypeStr = DataType.TYPE_STEP_COUNT_DELTA.toString();

                                           if (status.isSuccess()) {
                                               Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr);
                                           } else {
                                               Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr);
                                           }
                                       }
                                   }
                );
    }
}
