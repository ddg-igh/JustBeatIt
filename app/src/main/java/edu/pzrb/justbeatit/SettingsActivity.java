package edu.pzrb.justbeatit;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by René on 06.03.2016.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SeekBarPreference thresholdPreference;
    private SeekBarPreference graphTimePreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        thresholdPreference = (SeekBarPreference) this.findPreference(getString(R.string.preference_threshold_key));
        graphTimePreference = (SeekBarPreference) this.findPreference(getString(R.string.preference_graph_time_key));


        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        String thresholdRadius = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.preference_threshold_key),
                getString(R.string.preference_threshold_default));
        thresholdPreference.setSummary(this.getString(R.string.preference_threshold_summary).replace("$1", thresholdRadius));

        String graphTimeRadius = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.preference_graph_time_key),
                getString(R.string.preference_graph_time_default));
        graphTimePreference.setSummary(this.getString(R.string.preference_graph_time_summary).replace("$1", graphTimeRadius));

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String thresholdKey=getString(R.string.preference_threshold_key);
        String graphTImeKey=getString(R.string.preference_graph_time_key);


        if (thresholdKey.equals(key)) {
            String radius = PreferenceManager.getDefaultSharedPreferences(this).getString(thresholdKey,
                    getString(R.string.preference_threshold_default));
            thresholdPreference.setSummary(this.getString(R.string.preference_threshold_summary).replace("$1",radius));
        }

        if (graphTImeKey.equals(key)) {
            String radius = PreferenceManager.getDefaultSharedPreferences(this).getString(graphTImeKey,
                    getString(R.string.preference_graph_time_default));
            graphTimePreference.setSummary(this.getString(R.string.preference_graph_time_summary).replace("$1",radius));
        }
    }
}
