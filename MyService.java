package com.battperc.zero.battpercserv;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.os.PowerManager;
import android.widget.Toast;

public class MyService extends Service {
    int level;
    String PluggedIn;
    int NotifyCritical = 0;
    int NotifyCharging = 0;
    int InitialNotification = 0;
    int Icon;
    int DefaultLimit = 100;
    long TimeStart;
    long TimeEnd;
    long TimeElapsed;
    double TimeElapsedToSeconds;
    int TimeElapsedToMinutes;
    int TimeElapsedToHours;
    int MinuteRemainder;
    int alarmVolume = 30;

    boolean ScreenIsOn = true;
    boolean ProximitySettingOn;

    ////////////////////////////
    String FinalString = "00:00";
    int LastCharge;
    String PercentUsed = "0";
    boolean wasCharging = false;
    int criticalLevel = 35;
    private int field = 0x00000020;
    boolean isCharging;
    int FifthUpdate;

    long critTimerS = 0;

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            if (isCharging) {

                if (NotifyCharging == 0) {
                    PluggedIn = "Charging";

                    SharedPreferences getLoggedUpperLimit = getSharedPreferences("UpperLimitLog",
                            Activity.MODE_PRIVATE);
                    if (getLoggedUpperLimit.contains("UpperLimitP")) {
                        DefaultLimit = getLoggedUpperLimit.getInt("UpperLimitP", -1);
                    }

                    InitialNotification = 0;
                    NotifyCritical = 0;
                    //AlertTone();
                    Icon = getResources().getIdentifier("charging", "drawable",
                            "com.battperc.zero.battpercserv");
                    Noti(Icon,String.valueOf(level) + "%");
                    NotifyCharging = 1;
                    wasCharging = true;

                    TimeStart = System.currentTimeMillis();
                    LastCharge = level;
                    FifthUpdate = level;
                }
                if (level >= DefaultLimit) {
                    AlertTone();
                }
                TimeMaster();
                Noti(Icon,String.valueOf(level) + "%");
                if(TimeElapsedToMinutes > 10){
                    SharedPreferences SaveElapsed = getSharedPreferences("Elapsed", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor Editor = SaveElapsed.edit();
                    Editor.clear();
                    Editor.commit();
                }
            } else {
                if (InitialNotification == 0) {

                    PluggedIn = "Not Charging";
                    LastCharge = level;
                    SharedPreferences SaveElapsed = getSharedPreferences("Elapsed", Activity.MODE_PRIVATE);
                    if (SaveElapsed.contains("TimeStarted")) {
                        TimeStart = SaveElapsed.getLong("TimeStarted", -1);

                    } else {
                        TimeStart = System.currentTimeMillis();
                    }
                    if (SaveElapsed.contains("PercentStarted")) {
                        LastCharge = SaveElapsed.getInt("PercentStarted", -1);
                        if (LastCharge < level) {
                            LastCharge = level;
                            TimeStart = System.currentTimeMillis();
                        }
                    } else {
                        LastCharge = level;
                    }
                    SharedPreferences.Editor Editor = SaveElapsed.edit();
                    Editor.putLong("TimeStarted", TimeStart);
                    Editor.putInt("PercentStarted", LastCharge);
                    Editor.commit();
                }

                SharedPreferences getLoggedLowerLimit = getSharedPreferences("LowerLimitLog",
                        Activity.MODE_PRIVATE);
                if (getLoggedLowerLimit.contains("LowerLimitP")) {
                    if (getLoggedLowerLimit.getInt("LowerLimitP", -1) > level) {
                        criticalLevel = level;
                    } else {
                        criticalLevel = getLoggedLowerLimit.getInt("LowerLimitP", -1);
                    }
                }

                TimeEnd = System.currentTimeMillis();
                if (wasCharging) {
                    AlertTone();
                    wasCharging = false;
                }
                Icon = getResources().getIdentifier("full", "drawable",
                        "com.battperc.zero.battpercserv");
                //Noti(Icon, PluggedIn + " - " + FinalString +" - ",String.valueOf(level) + "%");
                Noti(Icon,String.valueOf(level) + "%");

                NotifyCharging = 0;
                InitialNotification = 1;
                NotifyCritical = 0;


                TimeMaster();
                //Noti(Icon,PluggedIn +" - " + FinalString +" - ",String.valueOf(level) + "%");
                Noti(Icon,String.valueOf(level) + "%");


                if (level == criticalLevel) {
                    if (NotifyCritical == 0) {
                        Icon = getResources().getIdentifier("belowsixty", "drawable",
                                "com.battperc.zero.battpercserv");
                        //Noti(Icon,PluggedIn +" - " + FinalString +" - ",String.valueOf(level) + "%");
                        Noti(Icon,String.valueOf(level) + "%");

                        NotifyCritical = 1;
                        NotifyCharging = 0;
                    }

                }

                if (level == criticalLevel) {
                    AlertTone();
                    criticalLevel = criticalLevel - 1;
                    critTimerS = System.currentTimeMillis();

                }

            }

            Intent DataToMainActivityIntent = new Intent("intentKey");
            // You can also include some extra data.
            DataToMainActivityIntent.putExtra("batt_level", String.valueOf(level) + "%");
            DataToMainActivityIntent.putExtra("batt_status", PluggedIn);
            DataToMainActivityIntent.putExtra("TimeElapsedMessage", FinalString);
            DataToMainActivityIntent.putExtra("PercentUsedMessage", PercentUsed);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(DataToMainActivityIntent);

            SharedPreferences getScreenSettings = getSharedPreferences("ProximityStatus",
                    Activity.MODE_PRIVATE);
            if (getScreenSettings.contains("On")) {
                ProximitySettingOn = getScreenSettings.getBoolean("On", true);
            }
            if (getScreenSettings.contains("Off")) {
                ProximitySettingOn = getScreenSettings.getBoolean("Off", false);
            }
            ScreenMagic();
        }
    };

    private BroadcastReceiver ShuttingDown = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( !isCharging ) {
                SharedPreferences SaveElapsed = getSharedPreferences("Elapsed", Activity.MODE_PRIVATE);
                SharedPreferences.Editor Editor = SaveElapsed.edit();
                Editor.putLong("TimeStarted", TimeStart);
                Editor.putInt("PercentStarted", LastCharge);
                Editor.commit();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


       this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        this.registerReceiver(this.ShuttingDown, new IntentFilter(Intent.ACTION_SHUTDOWN));

        ScreenMagic();

        this.registerReceiver(this.ScreenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
        this.registerReceiver(this.ScreenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //if ( !isCharging ) {
            SharedPreferences SaveElapsed = getSharedPreferences("Elapsed", Activity.MODE_PRIVATE);
            SharedPreferences.Editor Editor = SaveElapsed.edit();
            Editor.putLong("TimeStarted", TimeStart);
            Editor.putInt("PercentStarted", LastCharge);
            Editor.commit();

        Intent ServiceOff = new Intent("ServiceIsOff");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(ServiceOff);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Basic Notification";
            String description = "bmmulandi@gmail.com";
            //int importance = NotificationManager.IMPORTANCE_MIN;
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void AlertTone() {

        SharedPreferences getAlarmVolume = getSharedPreferences("alarmVolumeLog", Activity.MODE_PRIVATE);
        if (getAlarmVolume.contains("alarmVolumeP")) {
            alarmVolume = getAlarmVolume.getInt("alarmVolumeP", -1);
        }

        //final ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_ALARM, alarmVolume);
        //toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);
            /*final ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_ALARM, alarmVolume);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    toneGen1.release();
                }
            }, 1000);*/
            MediaPlayer mp = MediaPlayer.create(this,R.raw.beep);

        //mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.metoo,alarmVolume);
        //mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        //mediaPlayer.setLooping(false);

        mp.start();

    }

    private void Noti(int IconCall,String level) {
        createNotificationChannel();
        Context context = getApplicationContext();
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CHANNEL_ID");

        builder.setSmallIcon(IconCall);
        builder.setPriority(Notification.PRIORITY_LOW);
        //builder.setContentText(nTimeElapsed + level);
        builder.setContentTitle(PluggedIn + " - " + level);
        //builder.setStyle(new NotificationCompat.BigTextStyle().bigText(nTimeElapsed));
        builder.setOngoing(true);

        final Intent notificationIntent = new Intent(context, MainActivity.class);
        final PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        builder.setContentIntent(pi);

        final Notification notification = builder.build();
        startForeground(111, notification);
    }


    public void TimeMaster() {
        TimeEnd = System.currentTimeMillis();
        TimeElapsed = TimeEnd - TimeStart;
        TimeElapsedToSeconds = TimeElapsed / 1000;
        TimeElapsedToMinutes = (int) (TimeElapsedToSeconds / 60);
        TimeElapsedToHours = TimeElapsedToMinutes / 60;
        MinuteRemainder = TimeElapsedToMinutes % 60;
        if (TimeElapsedToMinutes > 59) {
            if (MinuteRemainder < 10) {
                FinalString = (String.valueOf(TimeElapsedToHours) + "h " +
                        String.valueOf(MinuteRemainder) + "m");
            } else {
                FinalString = (String.valueOf(TimeElapsedToHours) + "h " +
                        String.valueOf(MinuteRemainder) + "m");
            }
        } else {
            if (TimeElapsedToMinutes < 10) {
                FinalString = (String.valueOf(TimeElapsedToHours) + "h " +
                        String.valueOf(TimeElapsedToMinutes) + "m");
            } else {
                FinalString = (String.valueOf(TimeElapsedToHours) + "h " +
                        String.valueOf(TimeElapsedToMinutes) + "m");
            }
        }
        if( !isCharging) {
            PercentUsed = String.valueOf(LastCharge - level);
        }else if(isCharging){
            PercentUsed = String.valueOf(level - LastCharge);
        }
    }

    /*
    TO MANIFEST
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    */

    public void ScreenMagic() {
        SharedPreferences getScreenSettings = getSharedPreferences("ProximityStatus",
                Activity.MODE_PRIVATE);
        if (getScreenSettings.contains("On")) {
            ProximitySettingOn = getScreenSettings.getBoolean("On", true);
        }
        if (getScreenSettings.contains("Off")) {
            ProximitySettingOn = getScreenSettings.getBoolean("Off", false);
        }

        if(ProximitySettingOn) {

            try {
                // Yeah, this is hidden field.
                field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            } catch (Throwable ignored) {
            }

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(field, MyService.this.getClass().getName());

            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

    }


    private BroadcastReceiver ScreenOn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            ScreenMagic();
            //BatteryRegister(true);
            TimeMaster();
            ScreenIsOn = true;

        }
    };
    private BroadcastReceiver ScreenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScreenIsOn = false;
        }

    };

    boolean JustUnder60 = false;
    boolean JustUnder50 = false;
    boolean JustUnder40 = false;



}