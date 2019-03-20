package com.example.notificationscheduler;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import java.util.Calendar;
import java.util.List;

import static com.example.notificationscheduler.NotificationSchedulerApplication.CHANNEL_ID;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ENABLE_NOTIFICATIONS_PREF_KEY = "notifications_enable";
    private static final String NOTIFICATION_TIME_PREF_KEY = "notifications_time";

    // This request code is used for all PendingIntents so that PendingIntents previously submitted
    // to the AlarmManager can be looked up for cancellation or modification.
    private static final int REQUEST_CODE = 0;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(value.toString());

                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else if (preference instanceof TimePreference) {
                TimePreference timePreference = (TimePreference) preference;
                preference.setSummary(timePreference.valueToSummary((int) value));
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(value.toString());
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                preference.getContext());
        Object value;
        if (preference instanceof TimePreference) {
            value = sharedPrefs.getInt(preference.getKey(), 0);
        } else {
            value = sharedPrefs.getString(preference.getKey(), "");
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        // Listen for changes to any preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // We only want to take action if these particular preferences change.
        if (!s.equals(ENABLE_NOTIFICATIONS_PREF_KEY) && !s.equals(NOTIFICATION_TIME_PREF_KEY)) {
            return;
        }

        // If the preference was changed to false then we should cancel any pending notifications.
        if (s.equals(ENABLE_NOTIFICATIONS_PREF_KEY) && !sharedPreferences.getBoolean(s, false)) {
            cancelIntents();
            return;
        }

        // Get the time at which to schedule notifications.
        Calendar startTime = TimePreference.valueToCalendar(
                sharedPreferences.getInt(NOTIFICATION_TIME_PREF_KEY, 0));

        // If the time has already passed today, start notifications tomorrow.
        Calendar now = Calendar.getInstance();
        if (now.after(startTime)) {
            startTime.add(Calendar.DATE, 1);
        }

        // Build a notification, add it to the intent we'll schedule, and schedule it.
//        Notification notification = buildNotification();
//        scheduleNotification(notification, startTime);

        // Schedule an intent, and build and publish the notification in the future whenever
        // the intent is received.
        scheduleNotification(startTime);
    }

    private PendingIntent getNotificationPublisherIntent(Notification notification) {
        Intent intent = new Intent(this, NotificationPublisher.class);
        if (notification != null) {
            intent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        }

        return PendingIntent.getBroadcast(
                this, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, com.example.notificationscheduler.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Hello I'm a notification!")
                .setContentText("Well look at that, it's content 111")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        return builder.build();
    }

    private void scheduleNotification(Notification notification, Calendar startTime) {
        scheduleIntent(getNotificationPublisherIntent(notification), startTime);
    }

    private void scheduleNotification(Calendar startTime) {
        scheduleIntent(getNotificationPublisherIntent(null), startTime);
    }

    private void scheduleIntent(PendingIntent intent, Calendar startTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, intent);
    }

    private void cancelIntents() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getNotificationPublisherIntent(null));
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summary of the TimePreference to its value. When the value changes, the
            // summary is updated to reflect the new value, per the Android Design guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_time"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
