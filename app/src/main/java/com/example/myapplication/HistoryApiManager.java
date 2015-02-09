package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HistoryApiManager {

    private String TAG = "HistoryApiManager";

    private GoogleApiClient mClient;
    private Context mContext;
    long startBucketTime, endBucketTime;
    long startTime, endTime;

    public HistoryApiManager (GoogleApiClient client, Context context) {
        this.mClient = client;
        this.mContext = context;

        setTimeRange();
    }

    public void setTimeRange() {
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();

        cal.setTime(now);
        endTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR, -2);
        startTime = cal.getTimeInMillis();

        cal.setTime(now);
        endBucketTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        startBucketTime = cal.getTimeInMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("(MM/dd) HH:mm:ss");
        Log.i(TAG, "Range Bucket Time: " + dateFormat.format(startBucketTime) + " ~ " + dateFormat.format(endBucketTime));
        Log.i(TAG, "Range Time: " + dateFormat.format(startTime) + " ~ " + dateFormat.format(endTime));
    }

    private void dumpDataSet(DataSet dataSet) {
        String TAG = "dumpDataSet";

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
                    + " / " + dp.getDataType().getFields().get(0).getName()
                    + " : " + dp.getValue(dp.getDataType().getFields().get(0)));
        }
    }

    public void queryFitnessData(DataType dataType) {
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(dataType)
                .build();

        Fitness.HistoryApi.readData(mClient, dataReadRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult dataReadResult) {
                Log.i(TAG, "queryFitnessData()");
                printData(dataReadResult);
            }
        });
    }

    public void queryAggregateFitnessData(DataType inputType, DataType outputType) {
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .aggregate(inputType, outputType)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startBucketTime, endBucketTime, TimeUnit.MILLISECONDS)
                .build();

        Fitness.HistoryApi.readData(mClient, dataReadRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult dataReadResult) {
                Log.i(TAG, "queryAggregateFitnessData()");
                printData(dataReadResult);
            }
        });
    }

    private void printData(DataReadResult dataReadResult) {

        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
    }

    private DataSet insertFitnessData() {
        Log.i(TAG, "Creating a new data insert request");

        // Create a data source
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(mContext)
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setName(TAG + " - step count")
                .setType(DataSource.TYPE_RAW)
                .build();

        // Create a data set
        int stepCountDelta = 1000;
        DataSet dataSet = DataSet.create(dataSource);

        DataPoint dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta);
        dataSet.add(dataPoint);

        return dataSet;
    }

    public void deleteFitnessData() {
        Log.i(TAG, "Deleting specified step count data");

        long deleteStart, deleteEnd;

        Calendar calStart = new GregorianCalendar(2015, 01, 06, 11, 17, 26);
        Calendar calEnd = new GregorianCalendar(2015, 01, 06, 11, 17, 34);

        deleteEnd = calEnd.getTimeInMillis();
        deleteStart = calStart.getTimeInMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
        Log.i(TAG, dateFormat.format(deleteStart) + " " +  dateFormat.format(deleteEnd));

        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(deleteStart, deleteEnd, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build();

        Fitness.HistoryApi.deleteData(mClient, request)
                .setResultCallback(new ResultCallback<Status>() {
                       @Override
                       public void onResult(Status status) {
                           if (status.isSuccess()) {
                               Log.i(TAG, "Successfully deleted specified step count data");
                           } else {
                               Log.i(TAG, "Failed to delete specified step count data");
                           }
                       }
                }
        );
    }
}
