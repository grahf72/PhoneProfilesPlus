package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

@SuppressLint("NewApi")
public class ProfileListWidgetProvider extends AppWidgetProvider {

    private DataWrapper dataWrapper;

    public static final String INTENT_REFRESH_LISTWIDGET = "sk.henrichg.phoneprofilesplus.REFRESH_LISTWIDGET";

    private boolean isLargeLayout;
    private boolean isKeyguard;

    @SuppressWarnings("deprecation")
    private RemoteViews buildLayout(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean largeLayout)
    {
        Intent svcIntent=new Intent(context, ProfileListWidgetService.class);

        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews widget;

        if (largeLayout)
        {
            if (PPApplication.applicationWidgetListHeader)
            {
                if (!PPApplication.applicationWidgetListGridLayout)
                {
                    if (PPApplication.applicationWidgetListPrefIndicator)
                        widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget);
                    else
                        widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget_no_indicator);
                }
                else
                {
                    if (PPApplication.applicationWidgetListPrefIndicator)
                        widget=new RemoteViews(context.getPackageName(), R.layout.profile_grid_widget);
                    else
                        widget=new RemoteViews(context.getPackageName(), R.layout.profile_grid_widget_no_indicator);
                }
            }
            else
            {
                if (!PPApplication.applicationWidgetListGridLayout)
                    widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget_no_header);
                else
                    widget=new RemoteViews(context.getPackageName(), R.layout.profile_grid_widget_no_header);
            }
        }
        else
        {
            if (isKeyguard)
            {
                if (PPApplication.applicationWidgetListPrefIndicator)
                    widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget_small_keyguard);
                else
                    widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget_small_no_indicator_keyguard);
            }
            else
            {
                if (PPApplication.applicationWidgetListPrefIndicator)
                    widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget_small);
                else
                    widget=new RemoteViews(context.getPackageName(), R.layout.profile_list_widget_small_no_indicator);
            }
        }

        // set background
        int red = 0;
        int green;
        int blue;
        if (PPApplication.applicationWidgetListLightnessB.equals("0")) red = 0x00;
        if (PPApplication.applicationWidgetListLightnessB.equals("25")) red = 0x40;
        if (PPApplication.applicationWidgetListLightnessB.equals("50")) red = 0x80;
        if (PPApplication.applicationWidgetListLightnessB.equals("75")) red = 0xC0;
        if (PPApplication.applicationWidgetListLightnessB.equals("100")) red = 0xFF;
        green = red; blue = red;
        int alpha = 0x40;
        if (PPApplication.applicationWidgetListBackground.equals("0")) alpha = 0x00;
        if (PPApplication.applicationWidgetListBackground.equals("25")) alpha = 0x40;
        if (PPApplication.applicationWidgetListBackground.equals("50")) alpha = 0x80;
        if (PPApplication.applicationWidgetListBackground.equals("75")) alpha = 0xC0;
        if (PPApplication.applicationWidgetListBackground.equals("100")) alpha = 0xFF;
        widget.setInt(R.id.widget_profile_list_root, "setBackgroundColor", Color.argb(alpha, red, green, blue));


        // header
        if (PPApplication.applicationWidgetListHeader || (!largeLayout))
        {
            int monochromeValue = 0xFF;
            if (PPApplication.applicationWidgetListIconLightness.equals("0")) monochromeValue = 0x00;
            if (PPApplication.applicationWidgetListIconLightness.equals("25")) monochromeValue = 0x40;
            if (PPApplication.applicationWidgetListIconLightness.equals("50")) monochromeValue = 0x80;
            if (PPApplication.applicationWidgetListIconLightness.equals("75")) monochromeValue = 0xC0;
            if (PPApplication.applicationWidgetListIconLightness.equals("100")) monochromeValue = 0xFF;

            Profile profile = dataWrapper.getDatabaseHandler().getActivatedProfile();

            boolean isIconResourceID;
            String iconIdentifier;
            String profileName;
            if (profile != null)
            {
                profile.generateIconBitmap(context,
                        PPApplication.applicationWidgetListIconColor.equals("1"),
                        monochromeValue);
                profile.generatePreferencesIndicator(context,
                        PPApplication.applicationWidgetListIconColor.equals("1"),
                        monochromeValue);
                isIconResourceID = profile.getIsIconResourceID();
                iconIdentifier = profile.getIconIdentifier();
                profileName = dataWrapper.getProfileNameWithManualIndicator(profile, true, true, false);
            }
            else
            {
                // create empty profile and set icon resource
                profile = new Profile();
                profile._name = context.getResources().getString(R.string.profiles_header_profile_name_no_activated);
                profile._icon = Profile.PROFILE_ICON_DEFAULT+"|1|0|0";

                profile.generateIconBitmap(context,
                        PPApplication.applicationWidgetListIconColor.equals("1"),
                        monochromeValue);
                profile.generatePreferencesIndicator(context,
                        PPApplication.applicationWidgetListIconColor.equals("1"),
                        monochromeValue);
                isIconResourceID = profile.getIsIconResourceID();
                iconIdentifier = profile.getIconIdentifier();
                profileName = profile._name;
            }
            if (isIconResourceID)
            {
                if (profile._iconBitmap != null)
                    widget.setImageViewBitmap(R.id.widget_profile_list_header_profile_icon, profile._iconBitmap);
                else {
                    int iconResource = context.getResources().getIdentifier(iconIdentifier, "drawable", context.getPackageName());
                    widget.setImageViewResource(R.id.widget_profile_list_header_profile_icon, iconResource);
                }
            }
            else
            {
                widget.setImageViewBitmap(R.id.widget_profile_list_header_profile_icon, profile._iconBitmap);
            }
            //if (PPApplication.applicationWidgetListIconColor.equals("1"))
            //{
                red = 0xFF;
                if (PPApplication.applicationWidgetListLightnessT.equals("0")) red = 0x00;
                if (PPApplication.applicationWidgetListLightnessT.equals("25")) red = 0x40;
                if (PPApplication.applicationWidgetListLightnessT.equals("50")) red = 0x80;
                if (PPApplication.applicationWidgetListLightnessT.equals("75")) red = 0xC0;
                if (PPApplication.applicationWidgetListLightnessT.equals("100")) red = 0xFF;
                green = red; blue = red;
                widget.setTextColor(R.id.widget_profile_list_header_profile_name, Color.argb(0xFF, red, green, blue));
            //}
            //else
            //{
            //	widget.setTextColor(R.id.widget_profile_list_header_profile_name, Color.parseColor("#33b5e5"));
            //}
            widget.setTextViewText(R.id.widget_profile_list_header_profile_name, profileName);
            if (PPApplication.applicationWidgetListPrefIndicator)
            {
                widget.setImageViewBitmap(R.id.widget_profile_list_header_profile_pref_indicator, profile._preferencesIndicator);
            }
            if (largeLayout)
            {
                red = 0xFF;
                if (PPApplication.applicationWidgetListLightnessT.equals("0")) red = 0x00;
                if (PPApplication.applicationWidgetListLightnessT.equals("25")) red = 0x40;
                if (PPApplication.applicationWidgetListLightnessT.equals("50")) red = 0x80;
                if (PPApplication.applicationWidgetListLightnessT.equals("75")) red = 0xC0;
                if (PPApplication.applicationWidgetListLightnessT.equals("100")) red = 0xFF;
                green = red; blue = red;
                widget.setInt(R.id.widget_profile_list_header_separator, "setBackgroundColor", Color.argb(alpha, red, green, blue));
            }

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_events_restart);
            bitmap = BitmapManipulator.monochromeBitmap(bitmap, monochromeValue);
            widget.setImageViewBitmap(R.id.widget_profile_list_header_restart_events, bitmap);

        }
        ////////////////////////////////////////////////

        // clicks
        if (largeLayout)
        {
            Intent intent = new Intent(context, EditorProfilesActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent,
                                                        PendingIntent.FLAG_UPDATE_CURRENT);
            widget.setOnClickPendingIntent(R.id.widget_profile_list_header, pendingIntent);

            Intent intentRE = new Intent(context, RestartEventsFromNotificationActivity.class);
            PendingIntent pIntentRE = PendingIntent.getActivity(context, 0, intentRE, PendingIntent.FLAG_CANCEL_CURRENT);
            widget.setOnClickPendingIntent(R.id.widget_profile_list_header_restart_events, pIntentRE);

            if (!PPApplication.applicationWidgetListGridLayout)
                widget.setRemoteAdapter(appWidgetId, R.id.widget_profile_list, svcIntent);
            else
                widget.setRemoteAdapter(appWidgetId, R.id.widget_profile_grid, svcIntent);

            // The empty view is displayed when the collection has no items.
            // It should be in the same layout used to instantiate the RemoteViews
            // object above.
            if (!PPApplication.applicationWidgetListGridLayout)
                widget.setEmptyView(R.id.widget_profile_list, R.id.widget_profiles_list_empty);
            else
                widget.setEmptyView(R.id.widget_profile_grid, R.id.widget_profiles_list_empty);

            Intent clickIntent=new Intent(context, BackgroundActivateProfileActivity.class);
            clickIntent.putExtra(PPApplication.EXTRA_STARTUP_SOURCE, PPApplication.STARTUP_SOURCE_WIDGET);
            PendingIntent clickPI=PendingIntent.getActivity(context, 0,
                                                        clickIntent,
                                                        PendingIntent.FLAG_UPDATE_CURRENT);

            if (!PPApplication.applicationWidgetListGridLayout)
                widget.setPendingIntentTemplate(R.id.widget_profile_list, clickPI);
            else
                widget.setPendingIntentTemplate(R.id.widget_profile_grid, clickPI);
        }
        else
        {
            Intent intent = new Intent(context, LauncherActivity.class);
            intent.putExtra(PPApplication.EXTRA_STARTUP_SOURCE, PPApplication.STARTUP_SOURCE_WIDGET);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent,
                                                        PendingIntent.FLAG_UPDATE_CURRENT);
            widget.setOnClickPendingIntent(R.id.widget_profile_list_header, pendingIntent);

            Intent intentRE = new Intent(context, RestartEventsFromNotificationActivity.class);
            PendingIntent pIntentRE = PendingIntent.getActivity(context, 0, intentRE, PendingIntent.FLAG_CANCEL_CURRENT);
            widget.setOnClickPendingIntent(R.id.widget_profile_list_header_restart_events, pIntentRE);
        }

        return widget;
    }

    public void createProfilesDataWrapper(Context context)
    {
        PPApplication.loadPreferences(context);
        if (dataWrapper == null)
        {
            dataWrapper = new DataWrapper(context, false, false, 0);
        }
    }

    private void doOnUpdate(Context ctxt, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        Bundle myOptions;
        if (android.os.Build.VERSION.SDK_INT >= 16)
            myOptions = appWidgetManager.getAppWidgetOptions (appWidgetId);
        else
            myOptions = null;
        setLayoutParams(ctxt, appWidgetManager, appWidgetId, myOptions);
        RemoteViews widget = buildLayout(ctxt, appWidgetManager, appWidgetId, isLargeLayout);
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        //Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        createProfilesDataWrapper(context);

        for (int appWidgetId : appWidgetIds) {
            doOnUpdate(context, appWidgetManager, appWidgetId);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        if (dataWrapper != null)
            dataWrapper.invalidateDataWrapper();
        dataWrapper = null;

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        super.onReceive(context, intent);

        String action = intent.getAction();

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        createProfilesDataWrapper(context);

        if ((action != null) &&
            (action.equalsIgnoreCase("com.motorola.blur.home.ACTION_SET_WIDGET_SIZE")))
        {
            int spanX = intent.getIntExtra("spanX", 1);
            int spanY = intent.getIntExtra("spanY", 1);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            setLayoutParamsMotorola(context, spanX, spanY, appWidgetId);
            RemoteViews layout;
            layout = buildLayout(context, appWidgetManager, appWidgetId, isLargeLayout);
            appWidgetManager.updateAppWidget(appWidgetId, layout);
        }
        else
        if ((action != null) &&
            (action.equalsIgnoreCase(INTENT_REFRESH_LISTWIDGET)))
            updateWidgets(context);

        if (dataWrapper != null)
            dataWrapper.invalidateDataWrapper();
        dataWrapper = null;

    }

    private void setLayoutParams(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions)
    {
        String preferenceKey = "isLargeLayout_"+appWidgetId;
        SharedPreferences preferences = context.getSharedPreferences(PPApplication.APPLICATION_PREFS_NAME, Context.MODE_PRIVATE);

        AppWidgetProviderInfo appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);

        int minHeight;
        if (newOptions != null)
        {
            // Get the value of OPTION_APPWIDGET_HOST_CATEGORY
            int category = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
            // If the value is WIDGET_CATEGORY_KEYGUARD, it's a lockscreen widget
            isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;

            //int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            //int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            //int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

            if ((minHeight == 0) && (appWidgetProviderInfo != null))
            {
                minHeight = appWidgetProviderInfo.minHeight;
            }

        }
        else
        {
            isKeyguard = false;
            if (appWidgetProviderInfo != null)
                minHeight = appWidgetProviderInfo.minHeight;
            else
                minHeight = 0;

            //if (minHeight == 0)
            //	return;
        }

        if (isKeyguard)
        {
            isLargeLayout = minHeight >= 250;
        }
        else
        {
            isLargeLayout = minHeight >= 110;
        }
        
        if (preferences.contains(preferenceKey))
            isLargeLayout = preferences.getBoolean(preferenceKey, true);
        else
        {
            Editor editor = preferences.edit();
            editor.putBoolean(preferenceKey, isLargeLayout);
            editor.commit();
        }
        
    }

    private void setLayoutParamsMotorola(Context context, int spanX, int spanY, int appWidgetId)
    {
        isKeyguard = false;
        isLargeLayout = spanY != 1;
        
        String preferenceKey = "isLargeLayout_"+appWidgetId;
        SharedPreferences preferences = context.getSharedPreferences(PPApplication.APPLICATION_PREFS_NAME, Context.MODE_PRIVATE);

        Editor editor = preferences.edit();
        editor.putBoolean(preferenceKey, isLargeLayout);
        editor.commit();
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) 
    {
        //Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        createProfilesDataWrapper(context);

        String preferenceKey = "isLargeLayout_"+appWidgetId;
        SharedPreferences preferences = context.getSharedPreferences(PPApplication.APPLICATION_PREFS_NAME, Context.MODE_PRIVATE);

        // remove preference, will by reseted in setLayoutParams
        Editor editor = preferences.edit();
        editor.remove(preferenceKey);
        editor.commit();


        updateWidget(context, appWidgetId);

        if (dataWrapper != null)
            dataWrapper.invalidateDataWrapper();
        dataWrapper = null;

    }	

    private void updateWidget(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        doOnUpdate(context, appWidgetManager, appWidgetId);
        if (isLargeLayout)
        {
            if (!PPApplication.applicationWidgetListGridLayout)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_profile_list);
            else
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_profile_grid);
        }
    }

    private void updateWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(context, ProfileListWidgetProvider.class));

        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetId);
        }
    }

}
