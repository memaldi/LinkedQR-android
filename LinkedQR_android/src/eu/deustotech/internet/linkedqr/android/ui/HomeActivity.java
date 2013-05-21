package eu.deustotech.internet.linkedqr.android.ui;

import eu.deustotech.internet.linkedqr.android.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HomeActivity extends LinkedQRActivity {
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final int QR_REQUEST_CODE = 14567;
	public static final String PREFS_NAME = "LanguagePreferences";
	public static final String PREFS = "Settings_Perfil";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		final Button button = (Button) findViewById(R.id.transparente);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				

				SharedPreferences sharedPreferences = getApplicationContext()
						.getSharedPreferences(PREFS, 0);
				
				SharedPreferences.Editor editor = sharedPreferences
						.edit();
				editor.putBoolean("perfil", false);
				editor.commit();
				Toast toast = Toast.makeText(getApplicationContext(), "CAMBIADO",
						Toast.LENGTH_LONG);
				toast.show();
			}
		});
	}

	/** Handle "qr" action. */
	public void onQrClick(View v) {
		    Intent intent = new Intent(this, QrActivity.class);
            intent.putExtra("getBarcode", true);

			startActivityForResult(intent,
					QR_REQUEST_CODE);

	}

	/** Handle "idioma" action. */
	public void onIdiomaClick(View v) {		
		startActivity(new Intent(this, LanguageActivity.class));
	}

	/** Handle "salir" action. */
	public void onSalirClick(View v) {
		goExit(v);

	}

	private void goExit(View v) {
		//TODO: cambiar esto
		//UIUtils.goLogin(this);

	}

}
