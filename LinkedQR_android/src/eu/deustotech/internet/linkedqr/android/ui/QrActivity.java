package eu.deustotech.internet.linkedqr.android.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import android.view.View;
import android.widget.Button;
import eu.deustotech.internet.linkedqr.android.layout.Layout;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eu.deustotech.internet.linkedqr.android.R;
import eu.deustotech.internet.linkedqr.android.util.QRUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;


/**
 * @author mikel
 * 
 */
public class QrActivity extends LinkedQRActivity {

	private static final String BS_PACKAGE = "com.google.zxing.client.android";
	private static final int QR_CODE_REQUEST_CODE = 5678;
	private static final int MARKET_REQUEST_CODE = 4932;
	
	private String QRURI;
	private static final String PREFS_NAME = "LanguagePreferences";
	/**
	 * The creation of QrActivity
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.qr);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		settings.getString("lang", "es");
		
		// TODO: Esto hay que hacerlo mas eficiente
		//this.excludedProperties = fillExcludedProperties();
		
		getBarcodeScanner();

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

						applyTemplate(this.QRURI, null);
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
	
	private void applyTemplate(String URI, String templateID) {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try {

			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(getResources().openRawResource(R.raw.template));
			
			NodeList prefixesList = doc.getElementsByTagName("prefixes");
            Node prefixes = prefixesList.item(0);

            Map<String, String> uri2prefixMap = new HashMap<String, String>();
            Map<String, String> prefix2uriMap = new HashMap<String, String>();
            prefix2uriMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            uri2prefixMap.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");

            NodeList prefixesChildren = prefixes.getChildNodes();

            for(int i = 0; i < prefixesChildren.getLength(); i++) {
                Node prefixItem = prefixesChildren.item(i);
                if (prefixItem.getNodeName().equals("prefixItem")) {
                    NodeList prefixChildren = prefixItem.getChildNodes();
                    String prefix = "";
                    String uri = "";
                    for(int j = 0; j < prefixChildren.getLength(); j++) {
                        if (prefixChildren.item(j).getNodeName().equals("prefix")) {
                            prefix = prefixChildren.item(j).getFirstChild().getNodeValue();
                        } else if (prefixChildren.item(j).getNodeName().equals("uri")) {
                            uri = prefixChildren.item(j).getFirstChild().getNodeValue();
                        }
                    }
                    prefix2uriMap.put(prefix, uri);
                    uri2prefixMap.put(uri, prefix);
                }
            }

            /*NodeList endpoints = doc.getElementsByTagName("endpoint");
            String endpoint = endpoints.item(0).getFirstChild().getNodeValue();*/

            /*NodeList queries = doc.getElementsByTagName("query");
            String query = queries.item(0).getNodeValue().replace("%URI%", String.format("<%s>", URI));*/

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            String lang = settings.getString("lang", "es");

            //String typeQuery = String.format("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  CONSTRUCT { <%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o } WHERE { <%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o } ", URI, URI);

            HttpClient client = new DefaultHttpClient();
            //HttpGet request = new HttpGet(String.format("%s?query=%s&output=xml", endpoint, URLEncoder.encode(typeQuery, "utf-8")));
            HttpGet request = new HttpGet(URI);
            request.addHeader("Accept", "application/rdf+xml");

            HttpResponse response = client.execute(request);
            InputStream inputStream = response.getEntity().getContent();

            StatementCollector statementCollector = new StatementCollector();
            RDFParser rdfParser = new RDFXMLParser();
            rdfParser.setRDFHandler(statementCollector);
            rdfParser.parse(inputStream, "http://linkedqr/");

            Collection<Statement> statementCollection = statementCollector.getStatements();

            String type = "";

            if (templateID == null) {
                for (Statement statement : statementCollection) {
                    if (statement.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        type = statement.getObject().stringValue().replaceAll("\\u0000", "");
                        break;
                    }
                }
            }

            String prefixedType = String.format("%s:%s", uri2prefixMap.get(getPrefix(type)), type.split(getPrefix(type))[1]);

            NodeList pageItemList = doc.getElementsByTagName("pageItem");

            Node pageNode = null;
            String className = null;
            for (int i = 0; i < pageItemList.getLength(); i++) {
                Node node = pageItemList.item(i);
                if (node.getNodeName().equals("pageItem")) {
                    Element element = (Element) node;
                    String classAttribute = element.getAttribute("type");
                    if (classAttribute.equals(prefixedType)) {
                        pageNode = node;
                        className = element.getAttribute("class");
                    }
                }
            }


            Class layoutClass = Class.forName(className);
            Layout layout = (Layout) layoutClass.newInstance();
            setContentView(layout.getLayout());

            List<View> widgetList = new ArrayList<View>();

            NodeList pageNodeChildren = pageNode.getChildNodes();
            for (int i = 0; i < pageNodeChildren.getLength(); i++) {
                Node node = pageNodeChildren.item(i);
                if (node.getNodeName().equals("items")) {
                    NodeList childNodes = node.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node itemNode = childNodes.item(j);
                        NodeList itemNodeChildren = itemNode.getChildNodes();
                        if (itemNode.getNodeName().equals("item")) {

                            String name = "";
                            String property = "";
                            for (int k = 0; k < itemNodeChildren.getLength(); k++) {
                                Node element = itemNodeChildren.item(k);
                                if (element.getNodeName().equals("name")) {

                                } else if (element.getNodeName().equals("property")) {

                                }

                            }
                        } else if (itemNode.getNodeName().equals("separator")) {
                            //TODO: Separators
                        }

                    }
                }
            }
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RDFHandlerException e) {
            e.printStackTrace();
        } catch (RDFParseException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private String getPrefix(String URI) {
        if (URI.contains("#")) {
            return URI.split("#")[0];
        } else {
            String [] splitURI = URI.split("/");
            String prefixURI = "";
            for (int i = 0; i < splitURI.length - 1; i++) {
                prefixURI += splitURI[i] + "/";
            }
            return prefixURI;
        }
    }

}
