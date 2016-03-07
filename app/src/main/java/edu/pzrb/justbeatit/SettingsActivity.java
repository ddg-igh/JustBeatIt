package edu.pzrb.justbeatit;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

/**
 * Created by René on 06.03.2016.
 */
public class SettingsActivity extends PreferenceActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceFragment preferenceFragment = new PreferenceFragment() {
            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.preferences);
            }
        };

        getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();
    }

}
