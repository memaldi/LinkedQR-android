package eu.deustotech.internet.linkedqr.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import eu.deustotech.internet.linkedqr.android.R;
import eu.deustotech.internet.linkedqr.android.layout.Layout;
import eu.deustotech.internet.linkedqr.android.model.Widget;
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


/**
 * @author mikel
 * 
 */
public class LinkedQR {

    private Context context;
    private Activity activity;
    private  InputStream inputStream;

    public LinkedQR(Context context, Activity activity, InputStream inputStream) {
        this.context = context;
        this.activity = activity;
        this.inputStream = inputStream;
    }

	public void applyTemplate(String URI) {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try {

			docBuilder = docBuilderFactory.newDocumentBuilder();

			Document doc = docBuilder.parse(this.inputStream);
			
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
            View view = this.activity.findViewById(widgetID);
            if (ViewGroup.class.isAssignableFrom(view.getClass())) {
                ViewGroup viewGroup = (ViewGroup) view;
                viewGroup.removeView(viewGroup.getChildAt(0));
            }
        }
    }

    private void setLayout(Map<String, String> prefix2uriMap, Map<String, List<Statement>> predicateMap, String className, Map<String, Widget> propertyMap) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<Layout> layoutClass = (Class<Layout>) Class.forName(className);
        Layout layout = layoutClass.newInstance();

        this.activity.setContentView(layout.getLayout());

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
                        Object view = this.activity.findViewById(viewID);

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
        //SharedPreferences settings = this.acgetSharedPreferences(PREFS_NAME, 0);
        //String lang = settings.getString("lang", "es");

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

                List<Statement> main = getMain(object, this.inputStream);

                try {
                    Statement statement = main.get(0);
                    text = String.format(template, statement.getObject().stringValue().replaceAll("\\u0000", ""));

                    textView.setOnClickListener( new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(context.getApplicationContext(), LinkedQR.class);
                            intent.putExtra("getBoolean", false);
                            intent.putExtra("URI", object);

                            activity.startActivity(intent);
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

    private List<Statement> getMain(String URI, InputStream inputStream){
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {

            docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputStream);

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
