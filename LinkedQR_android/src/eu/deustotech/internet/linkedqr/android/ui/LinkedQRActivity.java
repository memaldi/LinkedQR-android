package eu.deustotech.internet.linkedqr.android.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

public class LinkedQRActivity extends Activity {
	
    public static final String PREFS_NAME = "LanguagePreferences";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String lang = settings.getString("lang", "es");
		LanguageActivity.forceLocale(lang, getBaseContext());
	}

}
