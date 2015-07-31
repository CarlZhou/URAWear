package com.example.carl.urawear;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.app.Activity;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
//import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataMap;
import com.mariux.teleport.lib.TeleportClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = "Sensor";
    private static String tripStartTime = "";
    private static boolean isTripStart;

    //UI Elements
    private CircledImageView mCircledImageView;
    private TextView mTextView;
    private Button mButton;

    //Sensor and SensorManager
    Sensor mHeartRateSensor;
    SensorManager mSensorManager;
    SensorNames mSensorNames;

    // API
    TeleportClient mTeleportClient;
    TeleportClient.OnGetMessageTask mMessageTask;

    boolean isFromNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heart_layout);

//        setAmbientEnabled();
//        changeButtonText();

        mTeleportClient = new TeleportClient(this);

        mTeleportClient.setOnSyncDataItemTask(new ShowToastHelloWorldTask());

        mMessageTask = new ShowToastFromOnGetMessageTask();

        mTeleportClient.setOnGetMessageTask(mMessageTask);

//        mTeleportClient.connect();

        isFromNotification = getIntent().getBooleanExtra("FromNotification", false);

        //Sensor and sensor manager
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

//        Log.v(TAG ,"Sensor: " + mHeartRateSensor.toString());

        //View
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mCircledImageView = (CircledImageView) stub.findViewById(R.id.circle);
                mTextView = (TextView) stub.findViewById(R.id.value);
                mButton = (Button) stub.findViewById(R.id.startTripBtn);
            }
        });

        startService(new Intent(this, SensorService.class));

        retrieveTripInfo();

//        stopService(new Intent(this, SensorService.class));
//
//        if (!isFromNotification) {
//            startService(new Intent(this, SensorService.class));
//        } else {
//            boolean isTripStart = getIntent().getBooleanExtra("isTripStart", false);
//            if (isTripStart) {
//                startService(new Intent(this, SensorService.class).putExtra("TripStart", false));
//            } else {
//                startService(new Intent(this, SensorService.class).putExtra("TripStart", false));
//            }
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        retrieveTripInfo();
        //Register the listener
        if (mSensorManager != null){
//            mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (mTeleportClient == null) {
            mTeleportClient = new TeleportClient(this);
            mTeleportClient.setOnSyncDataItemTask(new ShowToastHelloWorldTask());
        }

        mTeleportClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Unregister the listener
        if (mSensorManager!=null)
            mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Update your data. This check is very raw. You should improve it when the sensor is unable to calculate the heart rate
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            if ((int)event.values[0]>0) {
                mCircledImageView.setCircleColor(getResources().getColor(R.color.green));
                mTextView.setText("" + (int) event.values[0]);
            }
        }
        Log.d(TAG, event.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onStart() {
        super.onStart();
//        retrieveTripInfo();
        mTeleportClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTeleportClient.disconnect();

    }

//    public void syncDataItem(View v) {
//        //Let's sync a String!
//        if (mTeleportClient != null) {
//            mTeleportClient.connect();
//            mTeleportClient.syncString("hello", "Hello, World!");
//        } else {
//            mTeleportClient = new TeleportClient(this);
//            mTeleportClient.setOnSyncDataItemTask(new ShowToastHelloWorldTask());
//            mTeleportClient.connect();
//            mTeleportClient.syncString("hello", "Hello, World!");
//        }
//    }

    public void sendMessage(View v) {

        mTeleportClient.setOnGetMessageTask(new ShowToastFromOnGetMessageTask());

        if (!isTripStart) {
            startTrip();
        } else {
            stopTrip();
        }
    }

    public class ShowToastHelloWorldTask extends TeleportClient.OnSyncDataItemTask {

        @Override
        protected void onPostExecute(DataMap result) {
            String hello = result.getString("hello");

            Toast.makeText(getApplicationContext(),hello, Toast.LENGTH_SHORT).show();
        }
    }

    //Task that shows the path of a received message
    public class ShowToastFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {

        @Override
        protected void onPostExecute(String  path) {

            Toast.makeText(getApplicationContext(),"Message - "+path,Toast.LENGTH_SHORT).show();

            //let's reset the task (otherwise it will be executed only once)
            mTeleportClient.setOnGetMessageTask(new ShowToastFromOnGetMessageTask());
        }
    }

    public void retrieveTripInfo(){
        SharedPreferences settings;
        settings = getSharedPreferences("TRIP_INFO", Context.MODE_PRIVATE);

        tripStartTime = settings.getString("START_TIME", "");
        isTripStart = settings.getBoolean("IS_TRIP_START", false);
        changeButtonText();
    }

    public void changeButtonText(){
//        Button p1_button = (Button)findViewById(R.id.startTripBtn);
        if (mButton != null) {
            mButton.setText(isTripStart ? "End Trip" : "Start Trip");
        }
    }

    public void startTrip(){
        SharedPreferences settings;
        settings = getSharedPreferences("TRIP_INFO", Context.MODE_PRIVATE);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss ");
        String strDate = sdf.format(c.getTime());

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("START_TIME", strDate);
        editor.putBoolean("IS_TRIP_START", true);
        editor.commit();

        tripStartTime = strDate;
        isTripStart = true;
        changeButtonText();

        mTeleportClient.sendMessage("From Watch - Trip Start At: " + strDate, null);
    }

    public void stopTrip(){
        SharedPreferences settings;
        settings = getSharedPreferences("TRIP_INFO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("IS_TRIP_START", false);
        editor.commit();

        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss ");
        String strDate = sdf.format(c.getTime());

        isTripStart = false;
        changeButtonText();

        mTeleportClient.sendMessage("From Watch - Trip Stop At: " + strDate, null);
    }
}
