/*
 * Copyright 2013 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rootbox.rootboxota;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.rootbox.rootboxota.helpers.SettingsHelper;
import com.rootbox.rootboxota.updater.Updater;
import com.rootbox.rootboxota.updater.Updater.PackageInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static final String FILES_INFO = "com.rootbox.rootboxota.RomUtils.FILES_INFO";
    public static final String CHECK_DOWNLOADS_FINISHED = "com.rootbox.rootboxota.RomUtils.CHECK_DOWNLOADS_FINISHED";
    public static final String MOD_VERSION = "ro.rootbox.version";
    public static final int ROM_ALARM_ID = 122303221;
    public static final int GAPPS_ALARM_ID = 122303222;

    public static class NotificationInfo implements Serializable {

        public int mNotificationId;
        public PackageInfo[] mPackageInfos;
    }

    public static String getProp(String prop) {
        try {
            Process process = Runtime.getRuntime().exec("getprop " + prop);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
            }
            return log.toString();
        } catch (IOException e) {
            // Runtime error
        }
        return null;
    }

    public static String getReadableVersionRom(String version) {
        try {
            String number = version.substring(version.indexOf("-") + 1,
                    version.endsWith(".zip") ? version.lastIndexOf(".") : version.lastIndexOf("-"));
            number = number.substring(number.lastIndexOf('-') + 1);

            if ("v".equalsIgnoreCase(number.substring(0,1))) {
                number = number.substring(1) + " - Stable";
            }
            else {
                number = "Nightly";
            }

            String date = version.substring(version.lastIndexOf("-") + 1,
                    version.endsWith(".zip") ? version.lastIndexOf(".") : version.length());

            SimpleDateFormat curFormater = new SimpleDateFormat("yyyyMMdd");
            Date dateObj = null;
            try {
                dateObj = curFormater.parse(date);
            } catch (ParseException e) {
                // ignore
            }
            SimpleDateFormat postFormater = new SimpleDateFormat("MMMM dd, yyyy");

            if (dateObj == null) {
                return number;
            }
            String newDateStr = postFormater.format(dateObj);

            StringBuilder b = new StringBuilder(newDateStr);
            int i = 0;
            do {
                b.replace(i, i + 1, b.substring(i, i + 1).toUpperCase());
                i = b.indexOf(" ", i) + 1;
            } while (i > 0 && i < b.length());
            return number + " - " + b.toString();
        } catch (Exception ex) {
            // unknown version format
            return version;
        }
    }

    public static String getReadableVersion(String version) {
        try {
            version = version.replace("-jb", "-4.2").replace("-full", "").replace("-signed", "").replace("pa_", "");
            String number = version.substring(version.indexOf("-") + 1, version.lastIndexOf("-"));
            number = number.substring(0,1) + "." + number.substring(1);
            number = number.replace("..", ".");
            String date = version.substring(version.lastIndexOf("-") + 1,
                    version.endsWith(".zip") ? version.lastIndexOf(".") : version.length());

            SimpleDateFormat curFormater = new SimpleDateFormat("yyyyMMdd");
            Date dateObj = null;
            try {
                dateObj = curFormater.parse(date);
            } catch (ParseException e) {
                // ignore
            }
            SimpleDateFormat postFormater = new SimpleDateFormat("MMMM dd, yyyy");

            if (dateObj == null) {
                return number;
            }
            String newDateStr = postFormater.format(dateObj);

            StringBuilder b = new StringBuilder(newDateStr);
            int i = 0;
            do {
                b.replace(i, i + 1, b.substring(i, i + 1).toUpperCase());
                i = b.indexOf(" ", i) + 1;
            } while (i > 0 && i < b.length());
            return number + " - " + b.toString();
        } catch (Exception ex) {
            // unknown version format
            return version;
        }
    }

    public static String getDateAndTime() {
        return new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss").format(new Date(System
                .currentTimeMillis()));
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void setAlarm(Context context, boolean trigger, boolean isRom) {

        SettingsHelper helper = new SettingsHelper(context);
        long time = isRom ? helper.getCheckTimeRom() : helper.getCheckTimeGapps();
        setAlarm(context, time, trigger, isRom);
    }

    public static void setAlarm(Context context, long time, boolean trigger, boolean isRom) {

        Intent i = new Intent(context, NotificationAlarm.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getBroadcast(context,
                isRom ? ROM_ALARM_ID : GAPPS_ALARM_ID, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        if (time > 0) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, trigger ? 0 : time, time, pi);
        }
    }

    public static boolean alarmExists(Context context, boolean isRom) {
        return (PendingIntent.getBroadcast(context, isRom ? ROM_ALARM_ID
                : GAPPS_ALARM_ID, new Intent(context, NotificationAlarm.class),
                PendingIntent.FLAG_NO_CREATE) != null);
    }

    public static void showToastOnUiThread(final Context context, final int resourceId) {
        ((Activity) context).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(context, resourceId, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showToastOnUiThread(final Context context, final String string) {
        ((Activity) context).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showNotification(Context context, Updater.PackageInfo[] infos,
            int notificationId, int contentTitleResourceId) {
        Resources resources = context.getResources();

        Intent intent = new Intent(context, MainActivity.class);
        NotificationInfo fileInfo = new NotificationInfo();
        fileInfo.mNotificationId = notificationId;
        fileInfo.mPackageInfos = infos;
        intent.putExtra(FILES_INFO, fileInfo);
        PendingIntent pIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(resources.getString(contentTitleResourceId))
                .setSmallIcon(R.drawable.ic_notification).setContentIntent(pIntent);

        if (infos.length == 1) {
            builder.setContentText(resources.getString(R.string.new_package_name,
                    new Object[] { infos[0].getFilename() }));
        } else {
            builder.setContentText(resources.getString(R.string.new_packages,
                    new Object[] { infos.length }));
        }

        Notification notif = builder.build();

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Service.NOTIFICATION_SERVICE);

        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        if(SettingsHelper.getSoundMode()) {
            notif.defaults |= Notification.DEFAULT_SOUND;
        }
        if(SettingsHelper.getVibrationMode()) {
            notif.defaults |= Notification.DEFAULT_VIBRATE;
        }

        notificationManager.notify(notificationId, notif);
    }

    public static boolean isSystemApp(Context context) throws Exception {
        PackageManager pm = context.getPackageManager();
        android.content.pm.PackageInfo info = pm.getPackageInfo("com.rootbox.rootboxota",
                PackageManager.GET_ACTIVITIES);
        ApplicationInfo aInfo = info.applicationInfo;
        String path = aInfo.sourceDir.substring(0, aInfo.sourceDir.lastIndexOf("/"));
        boolean isSystemApp = path.contains("system/app");
        return isSystemApp;
    }
}
