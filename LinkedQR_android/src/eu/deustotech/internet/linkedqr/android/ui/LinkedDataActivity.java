package eu.deustotech.internet.linkedqr.android.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import eu.deustotech.internet.linkedqr.android.R;
import eu.deustotech.internet.linkedqr.android.R.id;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LinkedDataActivity extends LinkedQRActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.linked_data);

		ImageButton backImageButton = (ImageButton) findViewById(id.backImageButton);
		backImageButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				LinkedDataActivity.this.finish();				
			}
		});
		
		Intent intent = this.getIntent();
		//String jsonString = intent.getStringExtra("statementCollection");
		String guideText = intent.getStringExtra("text");
		String pictureURL = intent.getStringExtra("picture");

		TextView textView = new TextView(this);
		textView.setText(guideText);
		
		LinearLayout contentLayout = (LinearLayout) findViewById(id.contentLayout);
		contentLayout.addView(textView);
		
		ImageView pictureImageView = (ImageView) findViewById(id.pictureImageView);
		InputStream is = null;
		try {
			is = (InputStream) this.fetch(pictureURL);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Drawable d = Drawable.createFromStream(is, "src");
		pictureImageView.setImageDrawable(d);		
	}
	
	public Object fetch(String address) throws MalformedURLException,IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}

	
	public void onHomeClick(View v) {
		//TODO: cambiar
		//UIUtils.goHome(this);
	}
}
