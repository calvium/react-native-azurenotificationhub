package com.azure.reactnative.notificationhub;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.HeadlessJsTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.microsoft.windowsazure.notifications.NotificationsHandler;

import java.util.List;
import java.util.Set;

public class ReactNativeNotificationsHandler extends NotificationsHandler {
    public static final String TAG = "ReactNativeNotification";

    private static final String NOTIFICATION_CHANNEL_ID = "rn-push-notification-channel-id";
    private static final String CHANNEL_ID = "channel_01";
    private static final long DEFAULT_VIBRATION = 300L;

    private Context context;

    @Override
    public void onReceive(Context context, Bundle bundle) {
        this.context = context;
        if (!isAppOnForeground(context)) {
            runBackgroundTask(context, bundle);
        }
        sendBroadcast(context, bundle, 0);
    }

    private void runBackgroundTask(Context context, Bundle bundle) {
        this.context = context;
        String taskName = NotificationHubUtil.getInstance().getBackgroundTaskName(context);
        Log.i(TAG, "Got a notification to run with background task " + taskName);
        if (taskName != null) {
            Log.i(TAG, "Got a notification to run with background task " + taskName);
            sendToBackground(context, bundle, taskName);
        } else {
            Log.d(TAG, "No task name");
        }
    }

    private void sendToBackground(Context context, final Bundle bundle, final String taskName) {
        HeadlessJsTaskService.acquireWakeLockNow(context);
        Intent service = new Intent(context, ReactNativeBackgroundNotificationService.class);
        Bundle serviceBundle = new Bundle(bundle);
        serviceBundle.putString("taskName", taskName);
        service.putExtras(serviceBundle);
        context.startService(service);
    }

    private boolean isAppOnForeground(Context context) {
        /**
         We need to check if app is in foreground otherwise the app will crash.
         http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
         **/
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void sendBroadcast(final Context context, final Bundle bundle, final long delay) {
        (new Thread() {
            public void run() {
                try {
                    Thread.currentThread().sleep(delay);
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                json.put(key, bundle.get(key));
            } catch (JSONException e) {}
        }

        Intent event= new Intent(TAG);
        event.putExtra("event", ReactNativeNotificationHubModule.DEVICE_NOTIF_EVENT);
        event.putExtra("data", json.toString());
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.sendBroadcast(event);
    }                
    catch (Exception e) {}
            }
        }).start();
    }
        private Class getMainActivityClass() {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private NotificationManager notificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static boolean channelCreated = false;

    private static void checkOrCreateChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        if (channelCreated)
            return;
        if (manager == null)
            return;
        final CharSequence name = "rn-push-notification-channel";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.enableLights(true);
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
        channelCreated = true;
    }
}
