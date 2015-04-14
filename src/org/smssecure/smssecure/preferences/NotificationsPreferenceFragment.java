package org.smssecure.smssecure.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import com.doomonafireball.betterpickers.hmspicker.HmsPickerBuilder;
import com.doomonafireball.betterpickers.hmspicker.HmsPickerDialogFragment;

import org.smssecure.smssecure.ApplicationPreferencesActivity;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.util.SMSSecurePreferences;

import java.util.concurrent.TimeUnit;

public class NotificationsPreferenceFragment extends ListSummaryPreferenceFragment {

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);
    addPreferencesFromResource(R.xml.preferences_notifications);

    this.findPreference(SMSSecurePreferences.LED_COLOR_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(SMSSecurePreferences.LED_BLINK_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(SMSSecurePreferences.RINGTONE_PREF)
        .setOnPreferenceChangeListener(new RingtoneSummaryListener());
    this.findPreference(SMSSecurePreferences.REPEAT_ALERTS_PREF)
        .setOnPreferenceChangeListener(new RepeatAlertClickListener());
    this.findPreference(SMSSecurePreferences.REPEAT_ALERTS_INTERVAL_PREF)
        .setOnPreferenceClickListener(new RepeatAlertIntervalClickListener());
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__notifications);

    initializeListSummary((ListPreference) findPreference(SMSSecurePreferences.LED_COLOR_PREF));
    initializeListSummary((ListPreference) findPreference(SMSSecurePreferences.LED_BLINK_PREF));
    initializeListSummary((ListPreference) findPreference(SMSSecurePreferences.REPEAT_ALERTS_PREF));
    initializeRingtoneSummary((RingtonePreference) findPreference(SMSSecurePreferences.RINGTONE_PREF));
    initializeRepeatIntervalItem();
  }

  private class RingtoneSummaryListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      String value = (String) newValue;

      if (TextUtils.isEmpty(value)) {
        preference.setSummary(R.string.preferences__default);
      } else {
        Ringtone tone = RingtoneManager.getRingtone(getActivity(), Uri.parse(value));
        if (tone != null) {
          preference.setSummary(tone.getTitle(getActivity()));
        }
      }

      return true;
    }
  }

  private void initializeRingtoneSummary(RingtonePreference pref) {
    RingtoneSummaryListener listener =
      (RingtoneSummaryListener) pref.getOnPreferenceChangeListener();
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

    listener.onPreferenceChange(pref, sharedPreferences.getString(pref.getKey(), ""));
  }

  private void initializeRepeatIntervalItem() {
    Preference alertInterval = this.findPreference(SMSSecurePreferences.REPEAT_ALERTS_INTERVAL_PREF);
    int intervalSeconds      = SMSSecurePreferences.getRepeatAlertsInterval(getActivity());
    boolean repeatAlert      = SMSSecurePreferences.getRepeatAlertsCount(getActivity()) != 0;

    alertInterval.setEnabled(repeatAlert);
    alertInterval.setSummary(getString(R.string.AppProtectionPreferenceFragment_seconds, intervalSeconds));
  }

  public static CharSequence getSummary(Context context) {
    final int onCapsResId   = R.string.ApplicationPreferencesActivity_On;
    final int offCapsResId  = R.string.ApplicationPreferencesActivity_Off;

    return context.getString(SMSSecurePreferences.isNotificationsEnabled(context) ? onCapsResId : offCapsResId);
  }

  private class RepeatAlertClickListener extends ListSummaryListener {

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
      NotificationsPreferenceFragment.this.findPreference(SMSSecurePreferences.REPEAT_ALERTS_INTERVAL_PREF)
              .setEnabled(!value.equals("0"));

      return super.onPreferenceChange(preference, value);
    }
  }

  private class RepeatAlertIntervalClickListener implements Preference.OnPreferenceClickListener, HmsPickerDialogFragment.HmsPickerDialogHandler {

    @Override
    public boolean onPreferenceClick(Preference preference) {
      int[]      attributes = {R.attr.better_pickers_style};
      TypedArray hmsStyle   = getActivity().obtainStyledAttributes(attributes);

      new HmsPickerBuilder().setFragmentManager(getFragmentManager())
              .setStyleResId(hmsStyle.getResourceId(0, R.style.BetterPickersDialogFragment_Light))
              .addHmsPickerDialogHandler(this)
              .show();

      hmsStyle.recycle();

      return true;
    }

    @Override
    public void onDialogHmsSet(int reference, int hours, int minutes, int seconds) {
      int timeoutSeconds = Math.max((int) TimeUnit.HOURS.toSeconds(hours) +
              (int) TimeUnit.MINUTES.toSeconds(minutes) +
              seconds, 10);

      SMSSecurePreferences.setRepeatAlertsInterval(getActivity(), timeoutSeconds);
      initializeRepeatIntervalItem();
    }
  }
}
