
package eu.deustotech.internet.linkedqr.android.ui;

import java.util.Locale;

import eu.deustotech.internet.linkedqr.android.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class LanguageActivity extends LinkedQRActivity  {

    public static final String PREFS_NAME = "LanguagePreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
         setContentView(R.layout.idioma);
        
        ImageButton castellano = (ImageButton)findViewById(R.id.castellanoButton);
        ImageButton euskara = (ImageButton)findViewById(R.id.euskaraButton);
        ImageButton english = (ImageButton)findViewById(R.id.englishButton);
        ImageButton french = (ImageButton)findViewById(R.id.frenchButton);
        
        castellano.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{	
				changeLanguage("es");	
				onHomeClick(v);
			}

			
		});
        
        euskara.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				changeLanguage("eu");
				
				onHomeClick(v);
			}
		});
        
        english.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				changeLanguage("en");
				
				onHomeClick(v);
			}
		});
        
        french.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				changeLanguage("fr");
				
				onHomeClick(v);
			}
		});
        
    }

    private void changeLanguage(String language) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("lang", language);
	    editor.commit();
	}

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
    	//TODO: modificar
        //UIUtils.goHome(this);
    }

	public static void forceLocale(String lang, Context context) {
		Locale locale = new Locale(lang);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
	}
   
}
