package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

class GeofencesScanner implements GoogleApiClient.ConnectionCallbacks,
                                         GoogleApiClient.OnConnectionFailedListener,
                                         LocationListener
{

    private final GoogleApiClient mGoogleApiClient;
    private final Context context;
    private final DataWrapper dataWrapper;

    private final Location lastLocation;

    private LocationRequest mLocationRequest;
    private static boolean useGPS = true; // must be static
    boolean mUpdatesStarted = false;
    boolean mTransitionsUpdated = false;

    // Bool to track whether the app is already resolving an error
    boolean mResolvingError = false;
    // Request code to use when launching the resolution activity
    static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    static final String DIALOG_ERROR = "dialog_error";

    GeofencesScanner(Context context) {
        this.context = context;
        dataWrapper = new DataWrapper(context, false, false, 0);

        // Create a GoogleApiClient instance
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        lastLocation = new Location("GL");
    }

    void connect(boolean resetUseGPS) {
        PPApplication.logE("GeofenceScanner.connect", "mResolvingError="+mResolvingError);
        if (!mResolvingError) {
            //if (dataWrapper.getDatabaseHandler().getGeofenceCount() > 0)
            if (resetUseGPS)
                useGPS = true;
            mGoogleApiClient.connect();
        }
    }

    void connectForResolve() {
        PPApplication.logE("GeofenceScanner.disconnect", "xxx");
        try {
            if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                PPApplication.logE("GeofenceScanner.disconnect", "not connected, connect it");
                //if (dataWrapper.getDatabaseHandler().getGeofenceCount() > 0)
                mGoogleApiClient.connect();
            }
        } catch (Exception ignored) {}
    }

    void disconnect() {
        PPApplication.logE("GeofenceScanner.disconnect", "xxx");
        try {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
            }
            mGoogleApiClient.disconnect();
            //useGPS = true; diconnect is called from scren on/off broadcast therefore not change this
        } catch (Exception ignored) {}
    }

    @Override
    public void onConnected(Bundle bundle) {
        PPApplication.logE("GeofenceScanner.onConnected", "xxx");
        try {
            if (mGoogleApiClient.isConnected()) {
                useGPS = true;
                clearAllEventGeofences();
                updateTransitionsByLastKnownLocation(false);
                if (PPApplication.getApplicationStarted(context, true)) {
                    PPApplication.logE("GeofenceScanner.mUpdatesStarted=false", "from GeofenceScanner.onConnected");
                    mUpdatesStarted = false;
                    PPApplication.logE("GeofenceScanner.scheduleJob", "from GeofenceScanner.onConnected");
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onConnectionSuspended(int i) {
        PPApplication.logE("GeofenceScanner.onConnectionSuspended", "xxx");
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        //Log.d("GeofencesScanner.onConnectionSuspended", "xxx");
        //mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        PPApplication.logE("GeofenceScanner.onConnectionFailed", "xxx");
        //noinspection StatementWithEmptyBody
        if (mResolvingError) {
            // Already attempting to resolve an error.
            //return;
        } else if (connectionResult.hasResolution()) {
            /*try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }*/
            showErrorNotification(connectionResult.getErrorCode());
            mResolvingError = true;
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorNotification(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    boolean isConnected() {
        try {
            return mGoogleApiClient.isConnected();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        PPApplication.logE("##### GeofenceScanner.onLocationChanged", "location=" + location);
        CallsCounter.logCounter(context, "GeofencesScanner.onLocationChanged", "GeofencesScanner.onLocationChanged");

        synchronized (PPApplication.geofenceScannerLastLocationMutex) {
            lastLocation.set(location);
            //updateGeofencesInDB();
        }
    }

    void updateGeofencesInDB() {
        synchronized (PPApplication.geofenceScannerLastLocationMutex) {
            List<Geofence> geofences = dataWrapper.getDatabaseHandler().getAllGeofences();

            //boolean change = false;

            for (Geofence geofence : geofences) {

                Location geofenceLocation = new Location("GL");
                geofenceLocation.setLatitude(geofence._latitude);
                geofenceLocation.setLongitude(geofence._longitude);

                float distance = lastLocation.distanceTo(geofenceLocation);
                float radius = lastLocation.getAccuracy() + geofence._radius;

                int transitionType;
                if (distance <= radius)
                    transitionType = com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
                else
                    transitionType = com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;

                int savedTransition = dataWrapper.getDatabaseHandler().getGeofenceTransition(geofence._id);

                if (savedTransition != transitionType) {
                    PPApplication.logE("GeofenceScanner.updateGeofencesInDB", "geofence._name=" + geofence._name);
                    PPApplication.logE("GeofenceScanner.updateGeofencesInDB", "transitionType=" + transitionType);
                    PPApplication.logE("GeofenceScanner.updateGeofencesInDB", "savedTransition=" + savedTransition);

                    dataWrapper.getDatabaseHandler().updateGeofenceTransition(geofence._id, transitionType);
                    //change = true;
                }
            }

            mTransitionsUpdated = true;
        }
    }

    void clearAllEventGeofences() {
        // clear all geofence transitions
        dataWrapper.getDatabaseHandler().clearAllGeofenceTransitions();
        mTransitionsUpdated = false;
    }

    //-------------------------------------------

    private void createLocationRequest() {
        //Log.d("GeofenceScanner.createLocationRequest", "xxx");

        // check power save mode
        //boolean powerSaveMode = PPApplication.isPowerSaveMode;
        boolean isPowerSaveMode = DataWrapper.isPowerSaveMode(context);
        if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode(context).equals("2")) {
            mLocationRequest = null;
            return;
        }

        mLocationRequest = new LocationRequest();

        /*
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        int interval = 25;
        if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode(context).equals("1"))
            interval = 2 * interval;
        final long UPDATE_INTERVAL_IN_MILLISECONDS = interval * 1000;

        /*
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;


        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        // batched location (better for Android 8.0)
        mLocationRequest.setMaxWaitTime(UPDATE_INTERVAL_IN_MILLISECONDS * 4);

        if ((!ApplicationPreferences.applicationEventLocationUseGPS(context)) || isPowerSaveMode || (!useGPS)) {
            PPApplication.logE("GeofenceScanner.createLocationRequest","PRIORITY_BALANCED_POWER_ACCURACY");
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
        else {
            PPApplication.logE("GeofenceScanner.createLocationRequest","PRIORITY_HIGH_ACCURACY");
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        synchronized (PPApplication.geofenceScannerMutex) {
            try {
                if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {

                    if (Permissions.checkLocation(context)) {
                        try {
                            PPApplication.logE("****** GeofenceScanner.startLocationUpdates", "xxx");
                            createLocationRequest();
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                            PPApplication.logE("****** GeofenceScanner.startLocationUpdates", "mUpdatesStarted=true");
                            mUpdatesStarted = true;
                        } catch (SecurityException securityException) {
                            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                            PPApplication.logE("****** GeofenceScanner.startLocationUpdates", "mUpdatesStarted=false");
                            mUpdatesStarted = false;
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                PPApplication.logE("****** GeofenceScanner.startLocationUpdates", "mUpdatesStarted=false");
                mUpdatesStarted = false;
            }
        }

        if (ApplicationPreferences.applicationEventLocationUseGPS(context)) {
            // recursive call this for switch usage of GPS
            int delay = 60000; // one minute with GPS ON
            if (!useGPS)
                delay = 60000 * 30;  // 30 minutes with GPS OFF
            final Handler handler = new Handler(context.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    useGPS = !useGPS;
                    stopLocationUpdates();
                    startLocationUpdates();
                }
            }, delay);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).

        synchronized (PPApplication.geofenceScannerMutex) {
            try {
                if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
                        PPApplication.logE("##### GeofenceScanner.stopLocationUpdates", "xxx");
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                        PPApplication.logE("GeofenceScannerJob.mUpdatesStarted=false", "from GeofenceScanner.stopLocationUpdates");
                        mUpdatesStarted = false;
                }
            } catch (Exception ignored) {}
        }
    }

    /*
    void resetLocationUpdates(boolean forScreenOn) {
        stopLocationUpdates();
        createLocationRequest();
        PPApplication.logE("GeofenceScanner.scheduleJob", "from GeofenceScanner.resetLocationUpdates");
        // startLocationUpdates is called from GeofenceScannerJob
        if (PhoneProfilesService.instance != null)
            PhoneProfilesService.instance.scheduleGeofenceScannerJob(true, false, true, forScreenOn, true);
    }
    */

    @SuppressLint("MissingPermission")
    void updateTransitionsByLastKnownLocation(final boolean startEventsHandler) {
        try {
            if (Permissions.checkLocation(context) && mGoogleApiClient.isConnected()) {
                PPApplication.logE("GeofenceScanner.updateTransitionsByLastKnownLocation", "xxx");
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
                final Context appContext = context.getApplicationContext();
                //noinspection MissingPermission
                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        PPApplication.logE("GeofenceScanner.updateTransitionsByLastKnownLocation", "onSuccess");
                        if (location != null) {
                            synchronized (PPApplication.geofenceScannerLastLocationMutex) {
                                lastLocation.set(location);
                                updateGeofencesInDB();
                                if (startEventsHandler) {
                                    // start job
                                    //EventsHandlerJob.startForSensor(appContext, EventsHandler.SENSOR_TYPE_LOCATION_MODE);
                                    EventsHandler eventsHandler = new EventsHandler(appContext);
                                    eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_LOCATION_MODE, false);
                                }
                            }
                        }
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    //-------------------------------------------

    private void showErrorNotification(int errorCode) {
        String nTitle = context.getString(R.string.event_preferences_location_google_api_connection_error_title);
        String nText = context.getString(R.string.event_preferences_location_google_api_connection_error_text);
        if (android.os.Build.VERSION.SDK_INT < 24) {
            nTitle = context.getString(R.string.app_name);
            nText = context.getString(R.string.event_preferences_location_google_api_connection_error_title)+": "+
                    context.getString(R.string.event_preferences_location_google_api_connection_error_text);
        }
        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_exclamation_notify) // notification icon
                .setContentTitle(nTitle) // title for notification
                .setContentText(nText) // message for notification
                .setAutoCancel(true); // clear notification after click
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(nText));
        Intent intent = new Intent(context, GeofenceScannerErrorActivity.class);
        intent.putExtra(DIALOG_ERROR, errorCode);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            mBuilder.setCategory(Notification.CATEGORY_RECOMMENDATION);
            mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null)
            mNotificationManager.notify(PPApplication.GEOFENCE_SCANNER_ERROR_NOTIFICATION_ID, mBuilder.build());
    }

}
