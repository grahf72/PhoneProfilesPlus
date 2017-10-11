package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

class EventsHandler {
    
    Context context;
    private DataWrapper dataWrapper;
    private String sensorType;
    private boolean interactive;

    private int callEventType;
    private static int oldRingerMode;
    private static int oldSystemRingerMode;
    private static int oldZenMode;
    private static String oldRingtone;
    //private static String oldNotificationTone;
    private static int oldSystemRingerVolume;

    private String eventSMSPhoneNumber;
    private long eventSMSDate;
    private String eventNotificationPostedRemoved;
    private String eventNFCTagName;
    private long eventNFCDate;
    private int eventRadioSwitchType;
    private boolean eventRadioSwitchState;

    static final String SENSOR_TYPE_RADIO_SWITCH = "radioSwitch";
    static final String SENSOR_TYPE_RESTART_EVENTS = "restartEvents";
    static final String SENSOR_TYPE_START_EVENTS_SERVICE = "startEventsService";
    static final String SENSOR_TYPE_PHONE_CALL = "phoneCall";
    static final String SENSOR_TYPE_CALENDAR_PROVIDER_CHANGED = "calendarProviderChanged";
    static final String SENSOR_TYPE_SEARCH_CALENDAR_EVENTS = "searchCalendarEvents";
    static final String SENSOR_TYPE_SMS = "sms";
    static final String SENSOR_TYPE_NOTIFICATION = "notification";
    static final String SENSOR_TYPE_NFC_TAG = "nfcTag";
    static final String SENSOR_TYPE_EVENT_DELAY_START = "eventDelayStart";
    static final String SENSOR_TYPE_EVENT_DELAY_END = "eventDelayEnd";
    static final String SENSOR_TYPE_BATTERY = "battery";
    static final String SENSOR_TYPE_BLUETOOTH_CONNECTION = "bluetoothConnection";
    static final String SENSOR_TYPE_BLUETOOTH_STATE = "bluetoothState";
    static final String SENSOR_TYPE_DOCK_CONNECTION = "dockConnection";
    static final String SENSOR_TYPE_CALENDAR = "calendar";
    static final String SENSOR_TYPE_TIME = "time";
    static final String SENSOR_TYPE_APPLICATION = "application";
    static final String SENSOR_TYPE_HEADSET_CONNECTION = "headsetConnection";
    static final String SENSOR_TYPE_NOTIFICATION_EVENT_END = "notificationEventEnd";
    static final String SENSOR_TYPE_SMS_EVENT_END = "smsEventEnd";
    static final String SENSOR_TYPE_WIFI_CONNECTION = "wifiConnection";
    static final String SENSOR_TYPE_WIFI_STATE = "wifiState";
    static final String SENSOR_TYPE_POWER_SAVE_MODE = "powerSaveMode";
    static final String SENSOR_TYPE_GEOFENCES_SCANNER = "geofenceScanner";
    static final String SENSOR_TYPE_LOCATION_MODE = "locationMode";
    static final String SENSOR_TYPE_DEVICE_ORIENTATION = "deviceOrientation";
    static final String SENSOR_TYPE_PHONE_STATE = "phoneState";
    static final String SENSOR_TYPE_NFC_EVENT_END = "nfcEventEnd";
    static final String SENSOR_TYPE_WIFI_SCANNER = "wifiScanner";
    static final String SENSOR_TYPE_BLUETOOTH_SCANNER = "bluetoothScanner";
    static final String SENSOR_TYPE_SCREEN = "screen";
    static final String SENSOR_TYPE_DEVICE_IDLE_MODE = "deviceIdleMode";

    public EventsHandler(Context context) {
        this.context = context;
    }
    
    void handleEvents(String sensorType, boolean _interactive) {
        synchronized (PPApplication.eventsHandlerMutex) {
            CallsCounter.logCounter(context, "EventsHandler.handleEvents", "EventsHandler_handleEvents");

            if (!PPApplication.getApplicationStarted(context, true))
                // application is not started
                return;

            PPApplication.logE("#### EventsHandler.handleEvents", "-- start --------------------------------");

            this.sensorType = sensorType;
            PPApplication.logE("#### EventsHandler.handleEvents", "sensorType=" + this.sensorType);
            CallsCounter.logCounterNoInc(context, "EventsHandler.handleEvents->sensorType=" + this.sensorType, "EventsHandler_handleEvents");

            //restartAtEndOfEvent = false;

            // disabled for firstStartEvents
            //if (!PPApplication.getApplicationStarted(context))
            // application is not started
            //	return;

            //PPApplication.setApplicationStarted(context, true);

            dataWrapper = new DataWrapper(context, true, false, 0);

            ApplicationPreferences.getSharedPreferences(context);
            callEventType = ApplicationPreferences.preferences.getInt(PhoneCallJob.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallJob.CALL_EVENT_UNDEFINED);

            oldRingerMode = ActivateProfileHelper.getRingerMode(context);
            oldZenMode = ActivateProfileHelper.getZenMode(context);
            final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            oldSystemRingerMode = audioManager.getRingerMode();
            oldSystemRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
                if (uri != null)
                    oldRingtone = uri.toString();
                else
                    oldRingtone = "";
            } catch (SecurityException e) {
                Permissions.grantPlayRingtoneNotificationPermissions(context, true, false);
                oldRingtone = "";
            } catch (Exception e) {
                oldRingtone = "";
            }

            /*
            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                if (uri != null)
                    oldNotificationTone = uri.toString();
                else
                    oldNotificationTone = "";
            } catch (SecurityException e) {
                Permissions.grantPlayRingtoneNotificationPermissions(context, true, false);
                oldNotificationTone = "";
            } catch (Exception e) {
                oldNotificationTone = "";
            }
            */

            /*
            if (PhoneProfilesService.instance != null) {
                // start of GeofenceScanner
                if (!PhoneProfilesService.isGeofenceScannerStarted())
                    PPApplication.startGeofenceScanner(context);
                // start of CellTowerScanner
                if (!PhoneProfilesService.isPhoneStateScannerStarted()) {
                    PPApplication.logE("EventsHandler.handleEvents", "startPhoneStateScanner");
                    //PPApplication.sendMessageToService(this, PhoneProfilesService.MSG_START_PHONE_STATE_SCANNER);
                    PPApplication.startPhoneStateScanner(context);
                }
            }
            */

            if (!Event.getGlobalEventsRunning(context)) {
                // events are globally stopped

                doEndHandler();
                dataWrapper.invalidateDataWrapper();

                return;
            }

            /*
            // start orientation listener only when events exists
            if (PhoneProfilesService.instance != null) {
                if (!PhoneProfilesService.isOrientationScannerStarted()) {
                    if (dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_ORIENTATION) > 0)
                        PPApplication.startOrientationScanner(context);
                }
            }
            */

            if (!eventsExists(sensorType)) {
                // events not exists

                doEndHandler();
                dataWrapper.invalidateDataWrapper();

                PPApplication.logE("@@@ EventsHandler.handleEvents", "-- end: not events found --------------------------------");

                return;
            }

            dataWrapper.getActivateProfileHelper().initialize(dataWrapper, context);

            List<Event> eventList = dataWrapper.getEventList();

            boolean isRestart = sensorType.equals(SENSOR_TYPE_RESTART_EVENTS);

            this.interactive = (!isRestart) || _interactive;

            if (sensorType.equals(SENSOR_TYPE_CALENDAR_PROVIDER_CHANGED) ||
                    sensorType.equals(SENSOR_TYPE_SEARCH_CALENDAR_EVENTS)) {
                // search for calendar events
                PPApplication.logE("[CALENDAR] EventsHandler.handleEvents", "search for calendar events");
                for (Event _event : eventList) {
                    if ((_event._eventPreferencesCalendar._enabled) && (_event.getStatus() != Event.ESTATUS_STOP)) {
                        PPApplication.logE("[CALENDAR] EventsHandler.handleEvents", "event._id=" + _event._id);
                        _event._eventPreferencesCalendar.saveStartEndTime(dataWrapper);
                    }
                }
            }

            // "push events"
            if (isRestart) {
                // for restart events, set startTime to 0
                for (Event _event : eventList) {
                    _event._eventPreferencesSMS._startTime = 0;
                    dataWrapper.getDatabaseHandler().updateSMSStartTime(_event);
                    _event._eventPreferencesNotification._startTime = 0;
                    dataWrapper.getDatabaseHandler().updateNotificationStartTime(_event);
                    _event._eventPreferencesNFC._startTime = 0;
                    dataWrapper.getDatabaseHandler().updateNFCStartTime(_event);
                }
            } else {
                // for no-restart events, stet startTime to actual time
                if (sensorType.equals(SENSOR_TYPE_SMS)) {
                    // search for sms events, save start time
                    PPApplication.logE("EventsHandler.handleEvents", "search for sms events");
                    for (Event _event : eventList) {
                        if (_event.getStatus() != Event.ESTATUS_STOP) {
                            if (_event._eventPreferencesSMS._enabled) {
                                PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                                _event._eventPreferencesSMS.saveStartTime(dataWrapper, eventSMSPhoneNumber, eventSMSDate);
                            }
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (sensorType.equals(SENSOR_TYPE_NOTIFICATION)) {
                        // search for notification events, save start time
                        PPApplication.logE("EventsHandler.handleEvents", "search for notification events");
                        for (Event _event : eventList) {
                            if (_event.getStatus() != Event.ESTATUS_STOP) {
                                if (_event._eventPreferencesNotification._enabled) {
                                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                                /*_event._eventPreferencesNotification.saveStartTime(dataWrapper,
                                intent.getStringExtra(PPApplication.EXTRA_EVENT_NOTIFICATION_PACKAGE_NAME),
                                intent.getLongExtra(PPApplication.EXTRA_EVENT_NOTIFICATION_TIME, 0));*/
                                    if (eventNotificationPostedRemoved.equals("posted"))
                                        _event._eventPreferencesNotification.saveStartTime(dataWrapper);

                                }
                            }
                        }
                    }
                }
                if (sensorType.equals(SENSOR_TYPE_NFC_TAG)) {
                    // search for nfc events, save start time
                    PPApplication.logE("EventsHandler.handleEvents", "search for nfc events");
                    for (Event _event : eventList) {
                        if (_event.getStatus() != Event.ESTATUS_STOP) {
                            if (_event._eventPreferencesNFC._enabled) {
                                PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                                _event._eventPreferencesNFC.saveStartTime(dataWrapper, eventNFCTagName, eventNFCDate);
                            }
                        }
                    }
                }
            }

            boolean forDelayStartAlarm = sensorType.equals(SENSOR_TYPE_EVENT_DELAY_START);
            boolean forDelayEndAlarm = sensorType.equals(SENSOR_TYPE_EVENT_DELAY_END);

            //PPApplication.logE("@@@ EventsHandler.handleEvents","isRestart="+isRestart);
            PPApplication.logE("@@@ EventsHandler.handleEvents", "forDelayStartAlarm=" + forDelayStartAlarm);
            PPApplication.logE("@@@ EventsHandler.handleEvents", "forDelayEndAlarm=" + forDelayEndAlarm);

            // no refresh notification and widgets
            ActivateProfileHelper.lockRefresh = true;

            Profile mergedProfile = DataWrapper.getNonInitializedProfile("", "", 0);
            //mergedProfiles = new ArrayList<>();

            //Profile activatedProfile0 = null;

            int runningEventCount0;

            if (isRestart) {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "restart events");

                //oldActivatedProfile = null;

                // 1. pause events
                dataWrapper.sortEventsByStartOrderDesc();
                for (Event _event : eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state PAUSE");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP)
                        // len pauzuj eventy
                        // pauzuj aj ked uz je zapauznuty
                        dataWrapper.doHandleEvents(_event, true, true, interactive, forDelayStartAlarm, forDelayEndAlarm, true, mergedProfile, sensorType);
                }

                // get running events count
                List<EventTimeline> _etl = dataWrapper.getEventTimelineList();
                runningEventCount0 = _etl.size();

                // 2. start events
                dataWrapper.sortEventsByStartOrderAsc();
                for (Event _event : eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state RUNNING");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP)
                        // len spustaj eventy
                        // spustaj vsetky
                        dataWrapper.doHandleEvents(_event, false, true, interactive, forDelayStartAlarm, forDelayEndAlarm, true, mergedProfile, sensorType);
                }
            } else {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "NO restart events");

                //oldActivatedProfile = dataWrapper.getActivatedProfile();

                //activatedProfile0 = dataWrapper.getActivatedProfileFromDB();

                //1. pause events
                dataWrapper.sortEventsByStartOrderDesc();
                for (Event _event : eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state PAUSE");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP)
                        // len pauzuj eventy
                        // pauzuj len ak este nie je zapauznuty
                        //noinspection ConstantConditions
                        dataWrapper.doHandleEvents(_event, true, false, interactive, forDelayStartAlarm, forDelayEndAlarm, false, mergedProfile, sensorType);
                }

                // get running events count
                List<EventTimeline> _etl = dataWrapper.getEventTimelineList();
                runningEventCount0 = _etl.size();

                //2. start events
                dataWrapper.sortEventsByStartOrderAsc();
                for (Event _event : eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state RUNNING");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP)
                        // len spustaj eventy
                        // spustaj len ak este nebezi - musi to takto byt, lebo inac to bude furt menit veci v mobile
                        // napr. ked hlasitosti zmenene manualne tlacitkami.
                        //noinspection ConstantConditions
                        dataWrapper.doHandleEvents(_event, false, false, interactive, forDelayStartAlarm, forDelayEndAlarm, true, mergedProfile, sensorType);
                }
            }

            ActivateProfileHelper.lockRefresh = false;

            if (mergedProfile._id == 0)
                PPApplication.logE("$$$ EventsHandler.handleEvents", "no profile for activation");
            else
                PPApplication.logE("$$$ EventsHandler.handleEvents", "profileName=" + mergedProfile._name);

            //if ((!restartAtEndOfEvent) || isRestart) {
            //    // No any paused events has "Restart events" at end of event

            //////////////////
            //// when no events are running or manual activation,
            //// activate background profile when no profile is activated

            // get running events count
            List<EventTimeline> eventTimelineList = dataWrapper.getEventTimelineList();
            int runningEventCountE = eventTimelineList.size();

            Profile activatedProfile = dataWrapper.getActivatedProfileFromDB();

            if (!dataWrapper.getIsManualProfileActivation()) {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "active profile is NOT activated manually");
                PPApplication.logE("$$$ EventsHandler.handleEvents", "runningEventCountE=" + runningEventCountE);
                // no manual profile activation
                if (runningEventCountE == 0) {
                    PPApplication.logE("$$$ EventsHandler.handleEvents", "no events running");
                    // no events running
                    long profileId = Long.valueOf(ApplicationPreferences.applicationBackgroundProfile(context));
                    if (profileId != Profile.PROFILE_NO_ACTIVATE) {
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "default profile is set");
                        long activatedProfileId = 0;
                        if (activatedProfile != null)
                            activatedProfileId = activatedProfile._id;
                        if ((activatedProfileId != profileId) || isRestart) {
                            mergedProfile.mergeProfiles(profileId, dataWrapper);
                            PPApplication.logE("$$$ EventsHandler.handleEvents", "activated default profile");
                        }
                    }
                /*else
                if (activatedProfile == null)
                {
                    mergedProfile.mergeProfiles(0, dataWrapper);
                    PPApplication.logE("### EventsHandler.handleEvents", "not activated profile");
                }*/
                }
            } else {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "active profile is activated manually");
                // manual profile activation
                long profileId = Long.valueOf(ApplicationPreferences.applicationBackgroundProfile(context));
                if (profileId != Profile.PROFILE_NO_ACTIVATE) {
                    if (activatedProfile == null) {
                        // if not profile activated, activate Default profile
                        mergedProfile.mergeProfiles(profileId, dataWrapper);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "not activated profile");
                    }
                }
            }
            ////////////////

            String eventNotificationSound = "";
            boolean eventNotificationVibrate = false;

            if ((!isRestart) && (runningEventCountE > runningEventCount0)) {
                // only when not restart events and running events is increased, play event notification sound

                EventTimeline eventTimeline = eventTimelineList.get(runningEventCountE - 1);
                Event event = dataWrapper.getEventById(eventTimeline._fkEvent);
                if (event != null) {
                    eventNotificationSound = event._notificationSound;
                    eventNotificationVibrate = event._notificationVibrate;
                }
            }

            PPApplication.logE("$$$ EventsHandler.handleEvents", "mergedProfile=" + mergedProfile);

            PPApplication.logE("$$$ EventsHandler.handleEvents", "mergedProfile._id=" + mergedProfile._id);
            if (mergedProfile._id != 0) {
                // activate merged profile
                PPApplication.logE("$$$ EventsHandler.handleEvents", "profileName=" + mergedProfile._name);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "profileId=" + mergedProfile._id);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "profile._deviceRunApplicationPackageName=" + mergedProfile._deviceRunApplicationPackageName);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "interactive=" + interactive);
                dataWrapper.getDatabaseHandler().saveMergedProfile(mergedProfile);
                dataWrapper.activateProfileFromEvent(mergedProfile._id, interactive, false, true);

                if (PhoneProfilesService.instance != null)
                    PhoneProfilesService.instance.playEventNotificationSound(eventNotificationSound, eventNotificationVibrate);

                // wait for profile activation
                //try { Thread.sleep(500); } catch (InterruptedException e) { }
                //SystemClock.sleep(500);
                PPApplication.sleep(500);
            } else {
                /*long prId0 = 0;
                long prId = 0;
                if (activatedProfile0 != null) prId0 = activatedProfile0._id;
                if (activatedProfile != null) prId = activatedProfile._id;
                if ((prId0 != prId) || (prId == 0))*/
                dataWrapper.updateNotificationAndWidgets(activatedProfile);

                if (PhoneProfilesService.instance != null)
                    PhoneProfilesService.instance.playEventNotificationSound(eventNotificationSound, eventNotificationVibrate);

            }

            //}

            //restartAtEndOfEvent = false;

            doEndHandler();

            // refresh GUI
            /*Intent refreshIntent = new Intent();
            refreshIntent.setAction(RefreshGUIBroadcastReceiver.INTENT_REFRESH_GUI);
            context.sendBroadcast(refreshIntent);*/
            LocalBroadcastManager.getInstance(context).registerReceiver(PPApplication.refreshGUIBroadcastReceiver, new IntentFilter("RefreshGUIBroadcastReceiver"));
            Intent refreshIntent = new Intent("RefreshGUIBroadcastReceiver");
            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);


            dataWrapper.invalidateDataWrapper();

            PPApplication.logE("@@@ EventsHandler.handleEvents", "-- end --------------------------------");
        }
    }

    private boolean eventsExists(String broadcastReceiverType) {
        int eventType = 0;
        if (broadcastReceiverType.equals(SENSOR_TYPE_BATTERY))
            eventType = DatabaseHandler.ETYPE_BATTERY;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_BLUETOOTH_CONNECTION))
            eventType = DatabaseHandler.ETYPE_BLUETOOTHCONNECTED;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_BLUETOOTH_SCANNER))
            eventType = DatabaseHandler.ETYPE_BLUETOOTHINFRONT;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_BLUETOOTH_STATE))
            eventType = DatabaseHandler.ETYPE_BLUETOOTHCONNECTED;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_CALENDAR_PROVIDER_CHANGED))
            eventType = DatabaseHandler.ETYPE_CALENDAR;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_DOCK_CONNECTION))
            eventType = DatabaseHandler.ETYPE_PERIPHERAL;
        else
        /*if (broadcastReceiverType.equals(SENSOR_TYPE_EVENT_DELAY_START))
            eventType = DatabaseHandler.ETYPE_????;
        else*/
        /*if (broadcastReceiverType.equals(SENSOR_TYPE_EVENT_DELAY_END))
            eventType = DatabaseHandler.ETYPE_????;
        else*/
        if (broadcastReceiverType.equals(SENSOR_TYPE_CALENDAR))
            eventType = DatabaseHandler.ETYPE_CALENDAR;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_TIME))
            eventType = DatabaseHandler.ETYPE_TIME;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_APPLICATION))
            eventType = DatabaseHandler.ETYPE_APPLICATION;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_HEADSET_CONNECTION))
            eventType = DatabaseHandler.ETYPE_PERIPHERAL;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_NOTIFICATION))
            eventType = DatabaseHandler.ETYPE_NOTIFICATION;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_NOTIFICATION_EVENT_END))
            eventType = DatabaseHandler.ETYPE_NOTIFICATION;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_PHONE_CALL))
            eventType = DatabaseHandler.ETYPE_CALL;
        else
        /*if (broadcastReceiverType.equals(SENSOR_TYPE_RESTART_EVENTS))
            eventType = DatabaseHandler.ETYPE_???;
        else*/
                // call doEventService for all screen on/off changes
        /*if (broadcastReceiverType.equals(SENSOR_TYPE_SCREEN))
            eventType = DatabaseHandler.ETYPE_SCREEN;
        else*/
        if (broadcastReceiverType.equals(SENSOR_TYPE_SEARCH_CALENDAR_EVENTS))
            eventType = DatabaseHandler.ETYPE_CALENDAR;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_SMS))
            eventType = DatabaseHandler.ETYPE_SMS;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_SMS_EVENT_END))
            eventType = DatabaseHandler.ETYPE_SMS;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_WIFI_CONNECTION))
            eventType = DatabaseHandler.ETYPE_WIFICONNECTED;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_WIFI_SCANNER))
            eventType = DatabaseHandler.ETYPE_WIFIINFRONT;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_WIFI_STATE))
            eventType = DatabaseHandler.ETYPE_WIFICONNECTED;
        else
        /*if (broadcastReceiverType.equals(SENSOR_TYPE_DEVICE_IDLE_MODE))
            eventType = DatabaseHandler.ETYPE_????;
        else*/
        if (broadcastReceiverType.equals(SENSOR_TYPE_POWER_SAVE_MODE))
            eventType = DatabaseHandler.ETYPE_BATTERY;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_GEOFENCES_SCANNER))
            eventType = DatabaseHandler.ETYPE_LOCATION;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_LOCATION_MODE))
            eventType = DatabaseHandler.ETYPE_LOCATION;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_DEVICE_ORIENTATION))
            eventType = DatabaseHandler.ETYPE_ORIENTATION;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_PHONE_STATE))
            eventType = DatabaseHandler.ETYPE_MOBILE_CELLS;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_NFC_TAG))
            eventType = DatabaseHandler.ETYPE_NFC;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_NFC_EVENT_END))
            eventType = DatabaseHandler.ETYPE_NFC;
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_RADIO_SWITCH))
            eventType = DatabaseHandler.ETYPE_RADIO_SWITCH;

        if (eventType > 0)
            return dataWrapper.getDatabaseHandler().getTypeEventsCount(eventType) > 0;
        else
            return true;
    }

    private void doEndHandler() {
        PPApplication.logE("EventsHandler.doEndService","sensorType="+sensorType);
        PPApplication.logE("EventsHandler.doEndService","callEventType="+callEventType);

        if (sensorType.equals(SENSOR_TYPE_PHONE_CALL)) {

            if (!PhoneCallJob.linkUnlinkExecuted) {
                // no profile is activated from EventsHandler
                // link, unlink volumes for activated profile
                boolean linkUnlink = false;
                if (callEventType == PhoneCallJob.CALL_EVENT_INCOMING_CALL_RINGING)
                    linkUnlink = true;
                if (callEventType == PhoneCallJob.CALL_EVENT_INCOMING_CALL_ENDED)
                    linkUnlink = true;
                if (linkUnlink) {
                    Profile profile = dataWrapper.getActivatedProfile();
                    profile = Profile.getMappedProfile(profile, context);
                    if (profile != null) {
                        PPApplication.logE("EventsHandler.doEndService", "callEventType=" + callEventType);
                        ExecuteVolumeProfilePrefsJob.start(profile._id, false, false);
                        // wait for link/unlink
                        //try { Thread.sleep(1000); } catch (InterruptedException e) { }
                        //SystemClock.sleep(1000);
                        PPApplication.sleep(1000);
                    }
                }
            } else
                PhoneCallJob.linkUnlinkExecuted = false;

            if ((android.os.Build.VERSION.SDK_INT >= 21) && (callEventType == PhoneCallJob.CALL_EVENT_INCOMING_CALL_RINGING)) {
                // start PhoneProfilesService for ringing call simulation
                try {
                    Intent lIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_START_ON_BOOT, false);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_SIMULATE_RINGING_CALL, true);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_OLD_RINGER_MODE, oldRingerMode);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_OLD_ZEN_MODE, oldZenMode);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_OLD_RINGTONE, oldRingtone);
                    lIntent.putExtra(PhoneProfilesService.EXTRA_OLD_SYSTEM_RINGER_VOLUME, oldSystemRingerVolume);
                    //TODO Android O
                    //if (Build.VERSION.SDK_INT < 26)
                    context.startService(lIntent);
                    //else
                    //    context.startForegroundService(lIntent);
                } catch (Exception ignored) {}
            }

            if (!PhoneCallJob.speakerphoneOnExecuted) {
                // no profile is activated from EventsHandler
                // set speakerphone ON for activated profile
                if ((callEventType == PhoneCallJob.CALL_EVENT_INCOMING_CALL_ANSWERED) ||
                        (callEventType == PhoneCallJob.CALL_EVENT_OUTGOING_CALL_ANSWERED)) {
                    Profile profile = dataWrapper.getActivatedProfile();
                    profile = Profile.getMappedProfile(profile, context);
                    PhoneCallJob.setSpeakerphoneOn(profile, context);
                }
            } else
                PhoneCallJob.speakerphoneOnExecuted = false;

            if ((callEventType == PhoneCallJob.CALL_EVENT_INCOMING_CALL_ENDED) ||
                    (callEventType == PhoneCallJob.CALL_EVENT_OUTGOING_CALL_ENDED)) {
                ApplicationPreferences.getSharedPreferences(context);
                SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                editor.putInt(PhoneCallJob.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallJob.CALL_EVENT_UNDEFINED);
                editor.putString(PhoneCallJob.PREF_EVENT_CALL_PHONE_NUMBER, "");
                editor.apply();
            }
        }
        /*else
        if (broadcastReceiverType.equals(SENSOR_TYPE_SMS)) {
            // start PhoneProfilesService for notification tone simulation
            Intent lIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
            lIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
            lIntent.putExtra(PhoneProfilesService.EXTRA_START_ON_BOOT, false);
            lIntent.putExtra(EXTRA_SIMULATE_NOTIFICATION_TONE, true);
            lIntent.putExtra(EXTRA_OLD_RINGER_MODE, oldRingerMode);
            lIntent.putExtra(EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
            lIntent.putExtra(EXTRA_OLD_ZEN_MODE, oldZenMode);
            lIntent.putExtra(EXTRA_OLD_NOTIFICATION_TONE, oldNotificationTone);
            context.startService(lIntent);
        }
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_NOTIFICATION)) {
            if ((android.os.Build.VERSION.SDK_INT >= 21) && intent.getStringExtra(EXTRA_EVENT_NOTIFICATION_POSTED_REMOVED).equals("posted")) {
                // start PhoneProfilesService for notification tone simulation
                Intent lIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
                lIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                lIntent.putExtra(PhoneProfilesService.EXTRA_START_ON_BOOT, false);
                lIntent.putExtra(EXTRA_SIMULATE_NOTIFICATION_TONE, true);
                lIntent.putExtra(EXTRA_OLD_RINGER_MODE, oldRingerMode);
                lIntent.putExtra(EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
                lIntent.putExtra(EXTRA_OLD_ZEN_MODE, oldZenMode);
                lIntent.putExtra(EXTRA_OLD_NOTIFICATION_TONE, oldNotificationTone);
                context.startService(lIntent);
            }
        }*/
    }

    void setEventSMSParameters(String phoneNumber, long date) {
        eventSMSPhoneNumber = phoneNumber;
        eventSMSDate = date;
    }

    void setEventNotificationParameters(String postedRemoved) {
        eventNotificationPostedRemoved = postedRemoved;
    }

    void setEventNFCParameters(String tagName, long date) {
        eventNFCTagName = tagName;
        eventNFCDate = date;
    }

    void setEventRadioSwitchParameters(int type, boolean state) {
        eventRadioSwitchType = type;
        eventRadioSwitchState = state;
    }
}