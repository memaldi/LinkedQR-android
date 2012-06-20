package eu.deustotech.internet.linkedqr.android.ui;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

import eu.deustotech.internet.linkedqr.android.R;
import eu.deustotech.internet.linkedqr.android.R.id;
import eu.deustotech.internet.linkedqr.android.util.QRUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


/**
 * @author mikel
 * 
 */
public class QrActivity extends LinkedQRActivity {

	private static final String BS_PACKAGE = "com.google.zxing.client.android";
	private static final int QR_CODE_REQUEST_CODE = 5678;
	private static final int MARKET_REQUEST_CODE = 4932;
	private static final int LINKED_DATA_CODE = 496874;

	//private Collection<Statement> statementCollection;
	private String QRURI;
	private long initMillis;
	private static final String PREFS_NAME = "LanguagePreferences";
	private String mp3URI = "";
	private MediaPlayer mp = new MediaPlayer();
	private boolean isPaused = false;
	
	private List<String> sourceList = new ArrayList<String>();
	//private List<Statement> statementList = new ArrayList<Statement>();
	private List<String> excludedProperties = new ArrayList<String>();
	private String pictureURL = "";
	private String lang = "";
	private static final String SPARQL_ENDPOINT = "http://dev.morelab.deusto.es/tourspheres/sparql";
	private String guideText = "";
	
	private String artworkId = "";
	
	private int numOfTriples;
	/**
	 * The creation of QrActivity
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.qr);

		this.initMillis = -1;

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		this.lang = settings.getString("lang", "es");
		
		// TODO: Esto hay que hacerlo mas eficiente
		this.excludedProperties = fillExcludedProperties();
		
		getBarcodeScanner();

		TextView guideTextView = (TextView) findViewById(id.guidetextView);
		guideTextView.setVisibility(View.INVISIBLE);
		ToggleButton guideButton = (ToggleButton) findViewById(id.audioToggleButton);
		guideButton.setVisibility(View.INVISIBLE);
		Button stopButton = (Button) findViewById(id.stopButton);
		stopButton.setVisibility(View.INVISIBLE);
				
		Button moreInfoButton = (Button) findViewById(R.id.moreInfoButton);
		moreInfoButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(getApplicationContext(),
						LinkedDataActivity.class);

				intent.putExtra("QRUri", QRURI);
				intent.putExtra("text", guideText);
				intent.putExtra("picture", pictureURL);
				intent.putExtra("artworkId", artworkId);
				intent.putExtra("initMillis", initMillis);
				startActivity(intent);
			}
		});

		final ToggleButton audioToggleButton = (ToggleButton) findViewById(id.audioToggleButton);
		
		//Button stopButton = (Button) findViewById(id.stopButton);
		stopButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				mp.reset();	
				audioToggleButton.setChecked(false);
			}
		});
		
		audioToggleButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				//MediaPlayer mp = MediaPlayer.create(QrActivity.this,
				//		Uri.parse(mp3URI));
				if (audioToggleButton.isChecked()) {

					try {
						if (!isPaused){
							mp.reset();
						}
						mp.setDataSource(QrActivity.this, Uri.parse(mp3URI));
						mp.prepare();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mp.start();
					mp.setOnCompletionListener(new OnCompletionListener() {

						public void onCompletion(MediaPlayer mp) {
							audioToggleButton.setChecked(false);
						}
					});
				} else {
					if (mp.isPlaying()) {
						//mp.stop();
						mp.pause();
						isPaused = true;
					}
				}
			}

		});
	}
	
	/**
	 * Launches the Barcode Scanner. If the user has not the Barcode Scanner
	 * instaled, the app launches the Android Market to download it.
	 */
	private void getBarcodeScanner() {
		try {
			PackageInfo pi = QRUtils.searchPackageByURI(BS_PACKAGE, this);
			Logger.getLogger("QrActivity").info(pi.toString());
			Intent bsIntent = new Intent(BS_PACKAGE + ".SCAN");
			bsIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(bsIntent, QR_CODE_REQUEST_CODE);
		} catch (NameNotFoundException e) {
			final AlertDialog bsAlertDialog = new AlertDialog.Builder(this).create();
			bsAlertDialog.setTitle(R.string.bs_popup_title);
			bsAlertDialog.setMessage(getString(R.string.bs_popup_message));
			bsAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
					getString(R.string.bs_OK_button),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							Uri bsMarket = Uri.parse("market://details?id="
									+ BS_PACKAGE);
							Intent marketIntent = new Intent(
									Intent.ACTION_VIEW, bsMarket);
							bsAlertDialog.cancel();
							startActivityForResult(marketIntent,
									MARKET_REQUEST_CODE);
						}

					});
			bsAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
					getString(R.string.bs_CANCEL_button),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							//TODO: Cambiar
							//UIUtils.goHome(QrActivity.this);

						}
					});
			bsAlertDialog.setIcon(R.drawable.ic_launcher);
			bsAlertDialog.show();
		}
	}

	/**
	 * * Evaluates the returns of external activities like Barcode Scanner or
	 * Android Market
	 * 
	 * @param requestCode
	 *            The request code asigned to the requested intent
	 * @param resultCode
	 *            The code with the result of the execution of the intent
	 * @param data
	 *            The data returned by the intent
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == MARKET_REQUEST_CODE) {
			getBarcodeScanner();
		} else if (requestCode == QR_CODE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				

				
					this.QRURI = data.getStringExtra("SCAN_RESULT");
					
					try {

						List<Statement> statementList = getResource(this.QRURI);
						showStatements(statementList);
						
					} catch (Exception e) {
						e.printStackTrace();
						
						Context context = getApplicationContext();
						int duration = Toast.LENGTH_LONG;
						Toast toast = Toast.makeText(context, R.string.qr_error, duration);
						toast.show();
						//TODO: cambiar
						//UIUtils.goHome(this);
					}
			} else {
				//TODO: cambiar
				//UIUtils.goHome(this);
			}
		}
	}
	
	private List<String> fillExcludedProperties() {
		List<String> props = new ArrayList<String>();
		props.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		props.add("http://www.w3.org/2000/01/rdf-schema#label");
		props.add("http://xmlns.com/foaf/0.1/primaryTopic");
		props.add("http://www.w3.org/2000/01/rdf-schema#seeAlso");
		//props.add("http://purl.org/dc/elements/1.1/identifier");
		return props;
	}
	
	private List<Statement> evaluateStatement(List<Statement> stList) throws URISyntaxException {
		this.numOfTriples += stList.size();
		List<Statement> outStList = new ArrayList<Statement>();		
		for (Statement st : stList) {
		
			//URIImpl subject = (URIImpl) st.getSubject();
			URIImpl predicate = (URIImpl) st.getPredicate();
			Value object = st.getObject(); 
			
			if (!this.excludedProperties.contains(predicate.stringValue()) && !this.sourceList.contains(object.stringValue())) {
				if (predicate.stringValue().equals("http://xmlns.com/foaf/0.1/depiction")) {
					this.pictureURL = object.stringValue();
				} else if (predicate.stringValue().equals("http://purl.org/dc/elements/1.1/identifier")) {
					Intent intent = this.getIntent();
					intent.putExtra("artwork_id", object.stringValue());
					this.artworkId = object.stringValue();
				} else {
					if (st.getObject() instanceof URIImpl) {
						//URIImpl uriObject = (URIImpl) st.getObject();
						URI uriObject = new URI(object.stringValue());
						URI qrURI = new URI(this.QRURI);
						if (uriObject.getHost().equals(qrURI.getHost())) {
							this.sourceList.add(object.stringValue());
							//return evaluateStatement(st);		
							outStList.addAll(evaluateStatement(getResource(object.stringValue())));
						}
					} else if (st.getObject() instanceof LiteralImpl) {
						LiteralImpl literalObject = (LiteralImpl) st.getObject();
						if (literalObject.getLanguage() != null) {
							if (literalObject.getLanguage().equals(lang)) {
								outStList.add(st);
								//return st;
							}
						} else {
							outStList.add(st);
							//return st;
						}
					}
				}
			}
		}
		return outStList;
	}
	
	private void showStatements(List<Statement> stList) throws ClientProtocolException, RDFParseException, RDFHandlerException, IOException, URISyntaxException {
		LinearLayout headLinearLayout = (LinearLayout) findViewById(id.headLinearLayout);
		LinearLayout footerLinearLayout = (LinearLayout) findViewById(id.footerLinearLayout);
		
		for (Statement st : stList){
			URIImpl subject = (URIImpl) st.getSubject();
			URIImpl predicate = (URIImpl) st.getPredicate();
			Value object = st.getObject();
			if (predicate.stringValue().equals("http://purl.org/dc/terms/language")) {
				if (object.stringValue().equals(lang)) {
					this.mp3URI = getAudio(subject);
				}
			} else if (predicate.stringValue().equals("http://dbpedia.org/property/height") || predicate.stringValue().equals("http://dbpedia.org/property/width")) {
				String strObject = object.stringValue();
				float value = Float.parseFloat(strObject);
				TextView textView = new TextView(this);
				textView.setText(QRUtils.translateProperty(predicate.stringValue(), this.lang) + ": " + String.valueOf(value));	
				footerLinearLayout.addView(textView);
			} else if (predicate.stringValue().equals("http://purl.org/ontology/mo/text")) {
				this.guideText = object.stringValue();
			} else if (predicate.stringValue().equals("http://purl.org/dc/elements/1.1/title") || predicate.stringValue().equals("http://xmlns.com/foaf/0.1/name")) {
				TextView textView = new TextView(this);
				textView.setText(QRUtils.translateProperty(predicate.stringValue(), this.lang) + ": " + object.stringValue());
				textView.setTypeface(null, Typeface.BOLD);
				headLinearLayout.addView(textView);
			} else {
				TextView textView = new TextView(this);
				textView.setText(QRUtils.translateProperty(predicate.stringValue(), this.lang) + ": " + object.stringValue());
				footerLinearLayout.addView(textView);
			}
		}
	}
	
	private String getAudio(URIImpl subject) {
		String query = "SELECT ?o WHERE { <" + subject.stringValue() + "> <http://purl.org/ontology/mo/available_as> ?o }";
		String mp3 = "";
		try {
			Collection<Statement> result = QRUtils.SPARQLQuery(query, new URI(SPARQL_ENDPOINT));
			for (Statement st : result) {
				if (st.getPredicate().stringValue().equals("http://www.w3.org/2005/sparql-results#value")) {
					mp3 = st.getObject().stringValue();
					TextView guideTextView = (TextView) findViewById(id.guidetextView);
					guideTextView.setVisibility(View.VISIBLE);
					ToggleButton guideButton = (ToggleButton) findViewById(id.audioToggleButton);
					guideButton.setVisibility(View.VISIBLE);
					Button stopButton = (Button) findViewById(id.stopButton);
					stopButton.setVisibility(View.VISIBLE);
				}
			}
			return mp3;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RDFParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RDFHandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private List<Statement> getResource(String uri) {
		//this.sourceList.add(this.QRURI);
		List<Statement> statementList = new ArrayList<Statement>();
		Collection<Statement> statementCollection;
		
			try {
				statementCollection = QRUtils.getStatements(
						QRUtils.getDataInputStream(uri), uri);
				for (Statement st : statementCollection) {
					statementList.add(st);
				}
				
				return evaluateStatement(statementList);	
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RDFParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RDFHandlerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			//return statementList;
			//showStatements(this.statementList);
		
		return statementList;
	}
	
	/*
	public void getResource(String uri) {

		try {

			this.statementCollection = QRUtils.getStatements(
					QRUtils.getDataInputStream(uri), uri);
			Iterator<Statement> it = this.statementCollection.iterator();

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String lang = settings.getString("lang", "es");

			String title = "";
			String author = "";

			while (it.hasNext()) {
				Statement statement = it.next();
				if (statement.getPredicate().stringValue()
						.equals("http://purl.org/dc/elements/1.1/title")) {
					if (statement.getObject() instanceof LiteralImpl) {
						LiteralImpl literalObj = (LiteralImpl) statement.getObject();
						title = literalObj.stringValue();
						
						if (literalObj.getLanguage() != null) {
							if (literalObj.getLanguage().equals(lang)) {
								TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
								titleTextView.setText(title);
								if (!author.equals("")) {
									break;
								}
							}
						} else {
							title = literalObj.stringValue();
							TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
							titleTextView.setText(title);
							if (!author.equals("")) {
								break;
							}
						}
					}
				} else if (statement.getPredicate().stringValue()
						.equals("http://purl.org/dc/elements/1.1/creator")) {
					if (statement.getObject().getClass().equals(URIImpl.class)) {
						Collection<Statement> authorCol = QRUtils
								.getStatements(QRUtils
										.getDataInputStream(statement
												.getObject().stringValue()),
										statement.getObject().stringValue());
						Iterator<Statement> authorIt = authorCol.iterator();
						while (authorIt.hasNext()) {
							Statement authorStatement = authorIt.next();
							if (authorStatement.getPredicate().stringValue()
									.equals("http://xmlns.com/foaf/0.1/name")) {
								author = authorStatement.getObject()
										.stringValue();
								break;
							}
						}
					} else {
						author = statement.getObject().stringValue();
					}
					TextView authorTextView = (TextView) findViewById(R.id.authorTextView);
					authorTextView.setText(author);
					if (!title.equals("")) {
						break;
					}
				} else if (statement.getPredicate().stringValue()
						.equals("http://purl.org/dc/elements/1.1/description")
						&& statement.getObject() instanceof URIImpl) {
					URIImpl mp3uri = (URIImpl) statement.getObject();
					boolean validLang = false;
					String localMp3URI = "";
					try {
						Collection<Statement> uriStatementCollection = QRUtils
								.getStatements(QRUtils
										.getDataInputStream(mp3uri
												.stringValue()), mp3uri
										.stringValue());
						// this.sourceList.add(uri.stringValue());

						Iterator<Statement> uriIt = uriStatementCollection
								.iterator();
						while (uriIt.hasNext()) {
							Statement mp3St = uriIt.next();
							if (mp3St
									.getPredicate()
									.stringValue()
									.equals("http://purl.org/dc/terms/language")
									&& mp3St.getObject().stringValue()
											.equals(lang)) {
								validLang = true;
							} else if (mp3St
									.getPredicate()
									.stringValue()
									.equals("http://purl.org/ontology/mo/available_as")) {
								localMp3URI = mp3St.getObject().stringValue();
							}
						}
						if (validLang) {
							this.mp3URI = localMp3URI;
							TextView guideTextView = (TextView) findViewById(id.guidetextView);
							guideTextView.setVisibility(View.VISIBLE);
							ToggleButton guideButton = (ToggleButton) findViewById(id.audioToggleButton);
							guideButton.setVisibility(View.VISIBLE);
						}
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RDFParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RDFHandlerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (statement
						.getPredicate().stringValue()
						.equals("http://purl.org/dc/elements/1.1/identifier")) {
					Intent intent = this.getIntent();
					intent.putExtra("artwork_id", statement.getObject().stringValue());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();

			Context context = getApplicationContext();
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(context, R.string.qr_error, duration);
			toast.show();

			UIUtils.goHome(this);
		}

		// playGuide();
	}
	*/
	
	@Override
	public void onResume() {
		super.onResume();
		if (this.initMillis == -1) {
			this.initMillis = System.currentTimeMillis();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		this.mp.stop();
		ToggleButton audioToggleButton = (ToggleButton) findViewById(id.audioToggleButton);
		audioToggleButton.setChecked(false);
		Intent intent = this.getIntent();
		intent.putExtra("initMillis", this.initMillis);
		this.setResult(RESULT_OK, intent);
	}

	/*private void playGuide(String uri) {
		final ToggleButton audioToggleButton = (ToggleButton) findViewById(R.id.audioToggleButton);
		final MediaPlayer mp = MediaPlayer.create(QrActivity.this,
				Uri.parse(uri));
		audioToggleButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				if (audioToggleButton.isChecked()) {

					try {
						mp.prepare();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mp.start();
					mp.setOnCompletionListener(new OnCompletionListener() {

						public void onCompletion(MediaPlayer mp) {
							audioToggleButton.setChecked(false);
						}
					});
				} else {
					mp.stop();
				}
			}

		});
	}*/

	public void onHomeClick(View v) {
		//TODO: cambiar
		//UIUtils.goHome(this);
	}

}
