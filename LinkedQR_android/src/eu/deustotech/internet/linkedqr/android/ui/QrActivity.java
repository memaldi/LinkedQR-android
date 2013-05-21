package eu.deustotech.internet.linkedqr.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import eu.deustotech.internet.linkedqr.android.R;
import eu.deustotech.internet.linkedqr.android.layout.Layout;
import eu.deustotech.internet.linkedqr.android.model.Widget;
import eu.deustotech.internet.linkedqr.android.util.QRUtils;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;


/**
 * @author mikel
 * 
 */
public class QrActivity extends LinkedQRActivity {

	private static final String BS_PACKAGE = "com.google.zxing.client.android";
	private static final int QR_CODE_REQUEST_CODE = 5678;
	private static final int MARKET_REQUEST_CODE = 4932;

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
        if (getIntent().getBooleanExtra("getBarcode", false)) {
		    getBarcodeScanner();
        } else {
            applyTemplate(getIntent().getStringExtra("URI"));
        }

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
	 *            The request code assigned to the requested intent
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

                String QRURI = data.getStringExtra("SCAN_RESULT");
					
					try {

						applyTemplate(QRURI);
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
	
	private void applyTemplate(String URI) {
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

            Collection<Statement> statementCollection = getStatements(URI);

            List<String> typeList = new ArrayList<String>();

            Map<String, List<Statement>> predicateMap = getPredicateMap(statementCollection, typeList);

            List<String> prefixedTypeList = new ArrayList<String>();

            for (String type : typeList) {

                prefixedTypeList.add(String.format("%s:%s", uri2prefixMap.get(getPrefix(type)), type.split(getPrefix(type))[1]));
            }

            NodeList pageItemList = doc.getElementsByTagName("pageItem");

            Node pageNode = null;
            String className = null;
            for (int i = 0; i < pageItemList.getLength(); i++) {
                Node node = pageItemList.item(i);
                if (node.getNodeName().equals("pageItem")) {
                    Element element = (Element) node;
                    String classAttribute = element.getAttribute("type");
                    if (prefixedTypeList.contains(classAttribute)) {
                        pageNode = node;
                        className = element.getAttribute("class");
                    }
                }
            }

            Map<String, Widget> propertyMap = getPropertyMap(pageNode);

           setLayout(prefix2uriMap, predicateMap, className, propertyMap);



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

    private void clearLayouts(Map<String, Integer> widgetMap) {
        for (String key : widgetMap.keySet()) {
            Integer widgetID = widgetMap.get(key);
            View view = findViewById(widgetID);
            if (ViewGroup.class.isAssignableFrom(view.getClass())) {
                ViewGroup viewGroup = (ViewGroup) view;
                viewGroup.removeView(viewGroup.getChildAt(0));
            }
        }
    }

    private void setLayout(Map<String, String> prefix2uriMap, Map<String, List<Statement>> predicateMap, String className, Map<String, Widget> propertyMap) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<Layout> layoutClass = (Class<Layout>) Class.forName(className);
        Layout layout = layoutClass.newInstance();

        setContentView(layout.getLayout());

        Map<String, Integer> widgetMap = layout.getWidgets();

        for (String property : propertyMap.keySet()) {
            String extendedProperty = prefix2uriMap.get(property.split(":")[0]) + property.split(":")[1];
            if (predicateMap.keySet().contains(extendedProperty)) {
                Widget widget = propertyMap.get(property);
                String id = widget.getId();
                if(widgetMap.keySet().contains(id)) {
                    List<Statement> statementList = predicateMap.get(extendedProperty);
                    for (Statement statement : statementList) {
                        // Mirar los literales con varios idiomas y eso weis
                        String object = statement.getObject().stringValue().replaceAll("\\u0000", "");

                        // Aquí hay que mirar las condiciones de tipo y eso
                        int viewID = widgetMap.get(id);
                        Object view = findViewById(viewID);

                        setView(object, view, null, widget);


                    }
                }
            }
        }

        clearLayouts(widgetMap);

    }

    private Map<String, Widget> getPropertyMap(Node pageNode) {
        Map<String, Widget> propertyMap = new HashMap<String, Widget>();

        NodeList pageNodeChildren = pageNode.getChildNodes();
        for (int i = 0; i < pageNodeChildren.getLength(); i++) {
            Node node = pageNodeChildren.item(i);
            if (node.getNodeName().equals("items")) {
                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node itemNode = childNodes.item(j);
                    NodeList itemNodeChildren = itemNode.getChildNodes();
                    if (itemNode.getNodeName().equals("item")) {

                        String property = "";
                        String id = itemNode.getAttributes().getNamedItem("id").getNodeValue();
                        boolean linkable = false;
                        if (itemNode.getAttributes().getNamedItem("linkable") != null) {
                            linkable = Boolean.valueOf(itemNode.getAttributes().getNamedItem("linkable").getNodeValue());
                        }

                        boolean main = false;
                        if (itemNode.getAttributes().getNamedItem("main") != null) {
                            main = Boolean.valueOf(itemNode.getAttributes().getNamedItem("main").getNodeValue());
                        }

                        Widget widget = new Widget(id, linkable, main);
                        for (int k = 0; k < itemNodeChildren.getLength(); k++) {
                            Node element = itemNodeChildren.item(k);
                            if (element.getNodeName().equals("property")) {
                                property = element.getFirstChild().getNodeValue();
                            }
                            if (!"".equals(property) && !"".equals(id)) {
                                propertyMap.put(property, widget);
                            }

                        }
                    }

                }
            }
        }
        return propertyMap;
    }

    private Map<String, List<Statement>> getPredicateMap(Collection<Statement> statementCollection, List<String> typeList) {
        Map<String, List<Statement>> predicateMap = new HashMap<String, List<Statement>>();
        for (Statement statement : statementCollection) {
            String predicate = statement.getPredicate().stringValue().replaceAll("\\u0000", "");
            if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                typeList.add(statement.getObject().stringValue().replaceAll("\\u0000", ""));
            } else {
                List<Statement> statementList = new ArrayList<Statement>();
                if (predicateMap.keySet().contains(predicate)) {
                    statementList = predicateMap.get(predicate);
                }
                statementList.add(statement);
                predicateMap.put(predicate, statementList);
            }
        }
        return predicateMap;
    }

    private Collection<Statement> getStatements(String URI) throws IOException, RDFParseException, RDFHandlerException {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String lang = settings.getString("lang", "es");

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(URI);
        request.addHeader("Accept", "application/rdf+xml");

        HttpResponse response = client.execute(request);
        InputStream inputStream = response.getEntity().getContent();

        StatementCollector statementCollector = new StatementCollector();
        RDFParser rdfParser = new RDFXMLParser();
        rdfParser.setRDFHandler(statementCollector);
        rdfParser.parse(inputStream, "http://linkedqr/");

        return statementCollector.getStatements();
    }

    private void setView(final String object, Object view, Object originalView, Widget widget) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            String template = "";
            if (originalView == null) {
                template = textView.getText().toString();
            } else {
                TextView originalTextView = (TextView) originalView;
                template = originalTextView.getText().toString();
            }
            String text = "";
            if (widget.isLinkable()) {

                List<Statement> main = getMain(object);

                try {
                    Statement statement = main.get(0);
                    text = String.format(template, statement.getObject().stringValue().replaceAll("\\u0000", ""));

                    textView.setOnClickListener( new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(getApplicationContext(), QrActivity.class);
                            intent.putExtra("getBoolean", false);
                            intent.putExtra("URI", object);

                            startActivity(intent);
                        }
                    });
                } catch (NullPointerException e) {
                    text = String.format(template, object);
                }


            } else {
                text = String.format(template, object);
            }

            textView.setText(text);



        } else if (view instanceof ViewGroup) {
            ViewGroup layoutView = (ViewGroup) view;
            View child = layoutView.getChildAt(0);
            View childCopy = null;
            try {
                String childClassStr = child.getClass().getCanonicalName();
                System.out.println(childClassStr);
                Class<View> childClass = (Class<View>) Class.forName(child != null ? child.getClass().getCanonicalName() : null);

                Class[] constructorArgs = new Class[1];
                constructorArgs[0] = Context.class;
                Constructor<View> constructor = childClass.getDeclaredConstructor(constructorArgs);
                childCopy = constructor.newInstance(this);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            setView(object, childCopy, child, widget);
            childCopy.setVisibility(View.VISIBLE);
            childCopy.setLayoutParams(new ViewGroup.LayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)));
            layoutView.addView(childCopy);


        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            try {
                imageView.setImageBitmap(BitmapFactory.decodeStream(new URL(object).openConnection().getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    private List<Statement> getMain(String URI){
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

            Collection<Statement> statementCollection = getStatements(URI);

            List<String> typeList = new ArrayList<String>();

            Map<String, List<Statement>> predicateMap = getPredicateMap(statementCollection, typeList);

            List<String> prefixedTypeList = new ArrayList<String>();

            for (String type : typeList) {

                prefixedTypeList.add(String.format("%s:%s", uri2prefixMap.get(getPrefix(type)), type.split(getPrefix(type))[1]));
            }

            NodeList pageItemList = doc.getElementsByTagName("pageItem");

            Node pageNode = null;
            for (int i = 0; i < pageItemList.getLength(); i++) {
                Node node = pageItemList.item(i);
                if (node.getNodeName().equals("pageItem")) {
                    Element element = (Element) node;
                    String classAttribute = element.getAttribute("type");
                    if (prefixedTypeList.contains(classAttribute)) {
                        pageNode = node;
                    }
                }
            }

            //Map<String, Widget> propertyMap = getPropertyMap(pageNode);

            Map<String, Widget> propertyMap = new HashMap<String, Widget>();

            NodeList pageNodeChildren = pageNode.getChildNodes();
            for (int i = 0; i < pageNodeChildren.getLength(); i++) {
                Node node = pageNodeChildren.item(i);
                if (node.getNodeName().equals("items")) {
                    NodeList childNodes = node.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node itemNode = childNodes.item(j);
                        NodeList itemNodeChildren = itemNode.getChildNodes();
                        if (itemNode.getNodeName().equals("item")) {

                            String property = "";
                            String id = itemNode.getAttributes().getNamedItem("id").getNodeValue();
                            boolean linkable = false;
                            if (itemNode.getAttributes().getNamedItem("linkable") != null) {
                                linkable = Boolean.valueOf(itemNode.getAttributes().getNamedItem("linkable").getNodeValue());
                            }

                            boolean main = false;
                            if (itemNode.getAttributes().getNamedItem("main") != null) {
                                main = Boolean.valueOf(itemNode.getAttributes().getNamedItem("main").getNodeValue());

                            }

                            Widget widget = new Widget(id, linkable, main);
                            for (int k = 0; k < itemNodeChildren.getLength(); k++) {
                                Node element = itemNodeChildren.item(k);
                                if (element.getNodeName().equals("property")) {
                                    property = element.getFirstChild().getNodeValue();
                                }
                                if (!"".equals(property) && !"".equals(id)) {
                                    propertyMap.put(property, widget);
                                    if (widget.isMain()) {
                                        String extendedProperty = prefix2uriMap.get(property.split(":")[0]) + property.split(":")[1];
                                        return predicateMap.get(extendedProperty);
                                    }
                                }

                            }
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
        }
        return null;
    }

}
