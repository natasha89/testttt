package com.example.myapplication;

import android.os.Handler;
import android.os.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeThread extends Thread {
    Handler mTimeHandler;

    int mTime;
    int mMinute;
    int mSecond;

    TimeThread (Handler handler) {
        mTimeHandler = handler;
    }
    public void run() {
        while (true) {
            mTime = Integer.parseInt(new SimpleDateFormat("HH").format(new Date()));
            mMinute = Integer.parseInt(new SimpleDateFormat("mm").format(new Date()));
            mSecond = Integer.parseInt(new SimpleDateFormat("ss").format(new Date()));

            Message msg = Message.obtain(mTimeHandler, mTime, mMinute, mSecond, 0);
            mTimeHandler.sendMessage(msg);
            try { Thread.sleep(1000); }
            catch (InterruptedException e) { ; }
        }
    }
}
