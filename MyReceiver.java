package com.battperc.zero.battpercserv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent MyIntent = new Intent(context, MyService.class);
        //context.startService(MyIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(MyIntent);
        }else {
            context.startService(MyIntent);
        }

    }
}
