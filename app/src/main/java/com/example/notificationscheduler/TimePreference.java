package com.example.notificationscheduler;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * TimePreference is a Preference that allows the user to choose a time value using a TimePicker
 * widget.
 *
 * This class is based on those at https://stackoverflow.com/a/10608622/4308045 and
 * https://github.com/jakobulbrich/preferences-demo (see also
 * https://medium.com/@JakobUlbrich/building-a-settings-screen-for-android-part-3-ae9793fd31ec).
 */
public class TimePreference extends DialogPreference {

    // This is the same as the default value set in the XML. Keep them in sync.
    private static final int DEFAULT_TIME = 7 * 60;  // 0700

    // The currently chosen time stored as the number of minutes after midnight.
    private int time;

    private TimePicker picker;

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimePreference(Context context) {
        super(context);
    }

    /**
     * valueToSummary takes the raw value of the preference and converts it into a human-readable
     * string fit for use in e.g. the preference's summary.
     *
     * @param  value The raw value of the preference.
     * @return       The time formatted according to the current settings (locale, 12/24 hour clock)
     */
    public String valueToSummary(int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getHours(value));
        calendar.set(Calendar.MINUTE, getMinutes(value));
        calendar.set(Calendar.SECOND, 0);

        return DateFormat.getTimeFormat(getContext()).format(calendar.getTime());
    }

    private void setTime(int minAfterMidnight) {
        time = minAfterMidnight;
        persistInt(time);
        notifyChanged();
    }

    private int getHours(int minAfterMidnight) {
        return minAfterMidnight / 60;
    }

    private int getMinutes(int minAfterMidnight) {
        return minAfterMidnight % 60;
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        return picker;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        if (Build.VERSION.SDK_INT >= 23) {
            picker.setHour(getHours(time));
            picker.setMinute(getMinutes(time));
        } else {
            picker.setCurrentHour(getHours(time));
            picker.setCurrentMinute(getMinutes(time));
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int hours, minutes;
            if (Build.VERSION.SDK_INT >= 23) {
                hours = picker.getHour();
                minutes = picker.getMinute();
            } else {
                hours = picker.getCurrentHour();
                minutes = picker.getCurrentMinute();
            }

            int newTime = hours * 60 + minutes;
            if (callChangeListener(newTime)) {
                setTime(newTime);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_TIME);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        setTime(restorePersistedValue ? getPersistedInt(time) : (int) defaultValue);
    }
}
