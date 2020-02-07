package com.battperc.zero.battpercserv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView percentage;
    private TextView statusx;
    private EditText editTextC;
    private TextView TimeElapsedView;
    private  TextView PercentElapsed;
    private Button ExitButton;
    private EditText LowerValue;
    private EditText alarmVolumeView;
    private TextView textView;
    private TextView textView3;
    private Switch switch1;

    boolean ScreenSettingOn;
    //Long TimeCreatedByLaunch = System.currentTimeMillis();

    boolean holdDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = new Intent(this,MyService.class);
        startService(i);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("intentKey"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                MyKiller, new IntentFilter("MainActivityKiller"));

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);


        percentage = (TextView) this.findViewById(R.id.percentage);
        statusx = (TextView) this.findViewById(R.id.statusx);
        TimeElapsedView = (TextView) this.findViewById(R.id.TimeElapsedView);
        PercentElapsed = (TextView) this.findViewById(R.id.PercentElapsed);
        editTextC = (EditText)this.findViewById(R.id.editText);
        ExitButton = (Button) this.findViewById(R.id.button2);
        ExitButton.setOnClickListener(handleClick);
        ExitButton.setOnLongClickListener(handleLongClick);
        LowerValue = (EditText)this.findViewById(R.id.LowerValue);
        alarmVolumeView = (EditText)this.findViewById(R.id.alarmVolume);
        textView = (TextView)this.findViewById(R.id.textView);
        textView3 = (TextView)this.findViewById(R.id.textView3);
        switch1 = (Switch)this.findViewById(R.id.switch1);

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    SharedPreferences DltSS = getSharedPreferences("ProximityStatus", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor Editor1 = DltSS.edit();
                    Editor1.clear();
                    Editor1.commit();

                    SharedPreferences SaveScreen = getSharedPreferences("ProximityStatus", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor Editor = SaveScreen.edit();
                    Editor.putBoolean("On",b);
                    Editor.commit();
                    DelayServiceRestart();
                }else{
                    SharedPreferences DltSS = getSharedPreferences("ProximityStatus", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor Editor1 = DltSS.edit();
                    Editor1.clear();
                    Editor1.commit();

                    SharedPreferences SaveScreen = getSharedPreferences("ProximityStatus", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor Editor = SaveScreen.edit();
                    Editor.putBoolean("Off",b);
                    Editor.commit();

                    Context context = getApplicationContext();
                    Intent mStartActivity = new Intent(context, MainActivity.class);
                    int mPendingIntentId = 123456;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,mStartActivity,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC,System.currentTimeMillis()+100,mPendingIntent);
                    System.exit(0);
                }
            }
        });

        SharedPreferences getScreenSettings = getSharedPreferences("ProximityStatus",
                Activity.MODE_PRIVATE);
        if (getScreenSettings.contains("On")) {
            ScreenSettingOn = getScreenSettings.getBoolean("On", true);
            switch1.setChecked(true);
        }
        if (getScreenSettings.contains("Off")) {
            ScreenSettingOn = getScreenSettings.getBoolean("Off", false);
            switch1.setChecked(false);
        }

        SharedPreferences SaveElapsed = getSharedPreferences("UpperLimitLog", Activity.MODE_PRIVATE);
        if (SaveElapsed.contains("UpperLimitP")){
            editTextC.setText(String.valueOf(SaveElapsed.getInt("UpperLimitP",-1)));
        }

        SharedPreferences getLowerLimit = getSharedPreferences("LowerLimitLog", Activity.MODE_PRIVATE);
        if (getLowerLimit.contains("LowerLimitP")){
            LowerValue.setText(String.valueOf(getLowerLimit.getInt("LowerLimitP",-1)));
        }

        SharedPreferences getAlarmVolume = getSharedPreferences("alarmVolumeLog",Activity.MODE_PRIVATE);
        if(getAlarmVolume.contains("alarmVolumeP")){
            alarmVolumeView.setText(String.valueOf(getAlarmVolume.getInt("alarmVolumeP", -1)));
        }
    }

    private View.OnClickListener handleClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SharedPreferences SaveElapsed = getSharedPreferences("exitButtonDialogue", Activity.MODE_PRIVATE);
            if (SaveElapsed.contains("exitButtonDialogueP")){
                finishAffinity();
                getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Toaster("Service running in the background");
            }else{
                myDialog();

            }
        }
    };
    private View.OnLongClickListener handleLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            Intent i = new Intent(getApplicationContext(),MyService.class);
            stopService(i);
            //this.finish();
            finishAffinity();
            getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Toaster("Service stopped");
            System.exit(0);
            return true;

        }
    };

    public void OK(View v)
    {
            String editTextStr = editTextC.getText().toString();

            if (Integer.parseInt(editTextStr)>100){
                editTextStr = "100";
            }

            InputMethodManager iM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            iM.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            SharedPreferences SaveElapsed = getSharedPreferences("UpperLimitLog", Activity.MODE_PRIVATE);
            SharedPreferences.Editor Editor = SaveElapsed.edit();
            Editor.putInt("UpperLimitP", Integer.parseInt(editTextStr));
            Editor.commit();
            DelayServiceRestart();
            Toaster(editTextStr + "% set");
    }
    public void customCritical(View v){
        String LowerValueStr = LowerValue.getText().toString();
        if(Integer.parseInt(LowerValueStr)>100){
            LowerValueStr = "100";
        }

        InputMethodManager iM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        iM.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        SharedPreferences SaveElapsed = getSharedPreferences("LowerLimitLog", Activity.MODE_PRIVATE);
        SharedPreferences.Editor Editor = SaveElapsed.edit();
        Editor.putInt("LowerLimitP", Integer.parseInt(LowerValueStr));
        Editor.commit();
        DelayServiceRestart();
        Toaster(LowerValueStr + "% set");
    }
    public void alarmVolume(View v){
        String alarmVolumeStr = alarmVolumeView.getText().toString();

        if(Integer.parseInt(alarmVolumeStr) > 100){
            alarmVolumeStr = "100";
        }

            InputMethodManager iM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            iM.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            SharedPreferences SaveElapsed = getSharedPreferences("alarmVolumeLog", Activity.MODE_PRIVATE);
            SharedPreferences.Editor Editor = SaveElapsed.edit();
            Editor.putInt("alarmVolumeP", Integer.parseInt(alarmVolumeStr));
            Editor.commit();
            DelayServiceRestart();
            Toaster(alarmVolumeStr + " set");
    }



    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent sintent) {
            // Get extra data included in the Intent
            String message = sintent.getStringExtra("batt_level");
            percentage.setText(message);
            String message2 = sintent.getStringExtra("batt_status");
            statusx.setText(message2);
            if(message2.equals("Charging")){
                textView.setText("Since power connected");
                textView3.setText("% gained");
            }else if(message2.equals("Not Charging")){
                textView.setText("Since power disconnected");
                textView3.setText("% used");
            }
            String message3 = sintent.getStringExtra("TimeElapsedMessage");
            TimeElapsedView.setText(message3);
            String message4 = sintent.getStringExtra("PercentUsedMessage");
            PercentElapsed.setText(message4);
            // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        }
    };
    private BroadcastReceiver MyKiller = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent MyKillerIntent) {
            finishAffinity();
        }
    };

    public void Toaster(String toastContent){
        Toast.makeText(this, toastContent, Toast.LENGTH_SHORT).show();
    }

    public void myDialog(){

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tip")
                .setMessage("Long-press EXIT button to stop background service")
                .setPositiveButton("Close", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();
                        Toaster("Service running in the background");
                    }
                })
                .setNeutralButton("Close and never show again", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences SaveElapsed = getSharedPreferences("exitButtonDialogue", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor Editor = SaveElapsed.edit();
                        Editor.putInt("exitButtonDialogueP", 1);
                        Editor.commit();

                        finishAffinity();
                        Toaster("Service running in the background");
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .create();
        dialog.show();
    }
    public void DelayServiceRestart(){
        final Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                //AlertTone();
                Intent i = new Intent(getApplicationContext(), MyService.class);
                stopService(i);
                startService(i);

            }
        }, 1000);
    }
}
