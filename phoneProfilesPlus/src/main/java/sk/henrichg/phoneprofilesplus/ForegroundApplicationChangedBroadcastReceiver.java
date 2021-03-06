package sk.henrichg.phoneprofilesplus;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

import static android.content.Context.POWER_SERVICE;

public class ForegroundApplicationChangedBroadcastReceiver extends BroadcastReceiver {

    static final String ACTION_FOREGROUND_APPLICATION_CHANGED = "sk.henrichg.phoneprofilesplusextender.ACTION_FOREGROUND_APPLICATION_CHANGED";
    static final String ACTION_ACCESSIBILITY_SERVICE_UNBIND = "sk.henrichg.phoneprofilesplusextender.ACTION_ACCESSIBILITY_SERVICE_UNBIND";
    static final String ACCESSIBILITY_SERVICE_PERMISSION = "sk.henrichg.phoneprofilesplusextender.ACCESSIBILITY_SERVICE_PERMISSION";

    private static final String EXTENDER_ACCESSIBILITY_SERVICE_ID = "sk.henrichg.phoneprofilesplusextender/.PPPEAccessibilityService";

    private static final String EXTRA_PACKAGE_NAME = "sk.henrichg.phoneprofilesplus.package_name";
    private static final String EXTRA_CLASS_NAME = "sk.henrichg.phoneprofilesplus.class_name";

    private static final String PREF_APPLICATION_IN_FOREGROUND = "application_in_foreground";

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context appContext = context.getApplicationContext();

        CallsCounter.logCounter(context.getApplicationContext(), "ForegroundApplicationChangedBroadcastReceiver.onReceive", "ForegroundApplicationChangedBroadcastReceiver_onReceive");

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        if ((intent == null) || (intent.getAction() == null))
            return;

        PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.onReceive", "action="+intent.getAction());

        if (intent.getAction().equals(ACTION_FOREGROUND_APPLICATION_CHANGED)) {
            final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            final String className = intent.getStringExtra(EXTRA_CLASS_NAME);

            PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.onReceive", "packageName="+packageName);
            PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.onReceive", "className="+className);

            try {
                ComponentName componentName = new ComponentName(packageName, className);

                ActivityInfo activityInfo = tryGetActivity(appContext, componentName);
                boolean isActivity = activityInfo != null;
                if (isActivity) {
                    setApplicationInForeground(appContext, packageName);

                    if (Event.getGlobalEventsRunning(appContext)) {
                        //EventsHandlerJob.startForSensor(context, EventsHandler.SENSOR_TYPE_APPLICATION);
                        PhoneProfilesService.startHandlerThread();
                        final Handler handler = new Handler(PhoneProfilesService.handlerThread.getLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                                PowerManager.WakeLock wakeLock = null;
                                if (powerManager != null) {
                                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundApplicationChangedBroadcastReceiver.onReceive");
                                    wakeLock.acquire(10 * 60 * 1000);
                                }

                                EventsHandler eventsHandler = new EventsHandler(appContext);
                                eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_APPLICATION, false);

                                if (wakeLock != null)
                                    wakeLock.release();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("ForegroundApplicationChangedBroadcastReceiver.onReceive", e.toString());
            }
        }
        else
        if (intent.getAction().equals(ACTION_ACCESSIBILITY_SERVICE_UNBIND)) {
            setApplicationInForeground(appContext, "");

            //EventsHandlerJob.startForSensor(context, EventsHandler.SENSOR_TYPE_APPLICATION);
            PhoneProfilesService.startHandlerThread();
            final Handler handler = new Handler(PhoneProfilesService.handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundApplicationChangedBroadcastReceiver.onReceive");
                        wakeLock.acquire(10 * 60 * 1000);
                    }

                    EventsHandler eventsHandler = new EventsHandler(appContext);
                    eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_APPLICATION, false);

                    if (wakeLock != null)
                        wakeLock.release();
                }
            });
        }
    }

    private ActivityInfo tryGetActivity(Context context, ComponentName componentName) {
        try {
            return context.getPackageManager().getActivityInfo(componentName, 0);
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isAccessibilityServiceEnabled(Context context) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<AccessibilityServiceInfo> runningServices =
                    manager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);

            for (AccessibilityServiceInfo service : runningServices) {
                PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.isAccessibilityServiceEnabled", "serviceId="+service.getId());
                if (EXTENDER_ACCESSIBILITY_SERVICE_ID.equals(service.getId())) {
                    PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.isAccessibilityServiceEnabled", "true");
                    return true;
                }
            }
            PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.isAccessibilityServiceEnabled", "false");
            return false;
        }
        PPApplication.logE("ForegroundApplicationChangedBroadcastReceiver.isAccessibilityServiceEnabled", "false");
        return false;
    }

    static boolean isExtenderInstalled(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getApplicationInfo("sk.henrichg.phoneprofilesplusextender", 0).enabled;
        }
        catch (Exception e) {
            return false;
        }
    }

    static boolean isEnabled(Context context) {
        boolean installed = isExtenderInstalled(context);
        boolean enabled = false;
        if (installed)
            enabled = isAccessibilityServiceEnabled(context);
        return  installed && enabled;
    }

    static public String getApplicationInForeground(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getString(PREF_APPLICATION_IN_FOREGROUND, "");
    }

    static public void setApplicationInForeground(Context context, String application)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putString(PREF_APPLICATION_IN_FOREGROUND, application);
        editor.apply();
    }

}
