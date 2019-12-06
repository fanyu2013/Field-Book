package com.fieldbook.tracker.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.brapi.BrAPIService;
import com.fieldbook.tracker.brapi.BrapiControllerResponse;
import com.fieldbook.tracker.utilities.Utils;


public class PreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    PreferenceManager prefMgr;
    Context context;
    PreferenceCategory brapiPrefCategory;
    private Preference brapiAuthButton;
    private Preference brapiLogoutButton;
    private Preference brapiURLPreference;

    public static String BRAPI_BASE_URL = "BRAPI_BASE_URL";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName("Settings");
        SharedPreferences sharedPreferences;
        addPreferencesFromResource(R.xml.preferences);
        brapiPrefCategory = (PreferenceCategory) prefMgr.findPreference("brapi_category");
        brapiAuthButton = findPreference("authorizeBrapi");
        brapiLogoutButton = findPreference("revokeBrapiAuth");
        brapiURLPreference = findPreference("BRAPI_BASE_URL");

        brapiURLPreference.setOnPreferenceChangeListener(this);
        registerBrapiButtonListeners();
    }

    // Support for > API 23
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Occurs before the on create function. We get the context this way.
        PreferencesFragment.this.context = context;

    }

    // Support for < API 23
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Occurs before the on create function. We get the context this way.
        PreferencesFragment.this.context = activity;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.equals(brapiURLPreference)) {

            // This is done after this function, but set the value for our brapi function
            SharedPreferences.Editor editor = prefMgr.getSharedPreferences().edit();
            editor.putString(PreferencesActivity.BRAPI_BASE_URL, newValue.toString());
            editor.apply();

            // Call our brapi authorize function
            if (brapiPrefCategory != null) {

                // Start our login process
                BrapiControllerResponse brapiControllerResponse  = BrAPIService.authorizeBrAPI(prefMgr.getSharedPreferences(), context, null);

                // Show our error message if it exists
                processResponseMessage(brapiControllerResponse);

                // Set our button visibility and text
                setButtonView();
            }
        }

        return true;
    }

    public void processResponseMessage(BrapiControllerResponse brapiControllerResponse) {
        // Only show the error message
        if (brapiControllerResponse.status != null) {
            if (!brapiControllerResponse.status) {
                Toast.makeText(context, R.string.brapi_auth_error_starting, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void registerBrapiButtonListeners() {

        if (brapiAuthButton != null) {
            String brapiToken = prefMgr.getSharedPreferences().getString(PreferencesActivity.BRAPI_TOKEN, null);
            String brapiHost = prefMgr.getSharedPreferences().getString(BRAPI_BASE_URL, null);

            brapiAuthButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String brapiHost = prefMgr.getSharedPreferences().getString(BRAPI_BASE_URL, null);
                    if (brapiHost != null) {
                        // Start our login process
                        BrapiControllerResponse brapiControllerResponse = BrAPIService.authorizeBrAPI(prefMgr.getSharedPreferences(), context, null);

                        // Show our error message if it exists
                        processResponseMessage(brapiControllerResponse);
                    }
                    return true;
                }
            });

            // Set our button visibility and text
            setButtonView();
        }

        if (brapiLogoutButton != null) {
            brapiLogoutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences preferences = prefMgr.getSharedPreferences();

                    // Clear our brapi token
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(PreferencesActivity.BRAPI_TOKEN, null);
                    editor.apply();

                    // Set our button visibility and text
                    setButtonView();

                    return true;
                }
            });
        }
    }

    public void setButtonView() {

        String brapiToken = prefMgr.getSharedPreferences().getString(PreferencesActivity.BRAPI_TOKEN, null);
        String brapiHost = prefMgr.getSharedPreferences().getString(BRAPI_BASE_URL, null);

        if(brapiHost != null && !brapiHost.equals(getString(R.string.brapi_base_url_default))) {

            brapiPrefCategory.addPreference(brapiAuthButton);

            if (brapiToken != null) {
                // Show our reauthorize button and remove logout button
                brapiAuthButton.setTitle(R.string.brapi_reauthorize);
                brapiAuthButton.setSummary(getString(R.string.brapi_btn_auth_summary, brapiHost));
                // Show if our logout button if it is not shown already
                brapiPrefCategory.addPreference(brapiLogoutButton);
            }
            else {
                // Show authorize button and remove our logout button
                brapiAuthButton.setTitle(R.string.brapi_authorize);
                brapiAuthButton.setSummary(null);
                brapiPrefCategory.removePreference(brapiLogoutButton);
            }

        } else {
            brapiPrefCategory.removePreference(brapiAuthButton);
            brapiPrefCategory.removePreference(brapiLogoutButton);
        }

    }
}