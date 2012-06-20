package eu.deustotech.internet.linkedqr.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.turtle.TurtleParser;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class QRUtils {

	public static Collection<Statement> deserializeJSON(String jsonString) {

		Collection<Statement> collection = new ArrayList<Statement>();
		
		Object obj = JSONValue.parse(jsonString);
		JSONArray array = (JSONArray) obj;
		
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> arrayIterator = array.iterator();
		
		while(arrayIterator.hasNext()) {
			JSONObject jsonObject = arrayIterator.next();
			
			JSONObject subjectObject = (JSONObject) jsonObject.get("subject");
			String subject = subjectObject.get("uriString").toString();
			Resource subjectResource = new URIImpl(subject);
			
			JSONObject predicateObject = (JSONObject) jsonObject.get("predicate");
			String predicate = predicateObject.get("uriString").toString();
			URI predicateURI = new URIImpl(predicate);
			
			Value objectValue = null;
			JSONObject objectObject = (JSONObject) jsonObject.get("object");
			if(objectObject.containsKey("uriString")){
				String object = objectObject.get("uriString").toString();
				objectValue = new URIImpl(object);
			} else {
				String object = objectObject.get("label").toString();
				if(objectObject.containsKey("language")){
					objectValue = new LiteralImpl(object, objectObject.get("language").toString());
				} else {
					objectValue = new LiteralImpl(object);
				}
			}
			
			Statement st = new StatementImpl(subjectResource, predicateURI, objectValue);
			collection.add(st);
		}

		return collection;
	}

	public static PackageInfo searchPackageByURI(String packageURI,
			Context context) throws NameNotFoundException {
		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(packageURI,
					PackageManager.GET_ACTIVITIES);
			Logger.getLogger("QrActivity").info(pi.toString());
			return pi;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			throw new NameNotFoundException();
		}

	}

	public static InputStream getDataInputStream(String uri)
			throws URISyntaxException, ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet();
		request.addHeader("Accept", "text/turtle");
		request.setURI(new java.net.URI(uri));
		HttpResponse response = client.execute(request);
		return response.getEntity().getContent();
	} 
	
	public static Collection<Statement> getStatements(InputStream inputStream,
			String uri) throws MalformedURLException, IOException,
			RDFParseException, RDFHandlerException {
		StatementCollector collector = new StatementCollector();
		RDFParser rdfParser = new TurtleParser();
		rdfParser.setRDFHandler(collector);
		rdfParser.parse(inputStream, uri);

		Collection<Statement> col = collector.getStatements();
		return col;
	}
	
	public static Collection<Statement> SPARQLQuery(String query,
			java.net.URI endpoint) throws ClientProtocolException, IOException, RDFParseException, RDFHandlerException {
		
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(endpoint + "?query=" + java.net.URLEncoder.encode(query) + "&format=text/n3");
		//request.setURI(endpoint);
		//request.addHeader("Accept", "sparql");
		HttpParams params = new BasicHttpParams();
		params.setParameter("query", query);
		params.setParameter("format", "text/rdf+n3");
		request.setParams(params);
		
		HttpResponse response = client.execute(request);
		
		InputStream inputStream = response.getEntity().getContent();
	    /*BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
	    StringBuilder sb = new StringBuilder();
	    String line = "";
	    while ((line = reader.readLine()) != null) {
	      sb.append(line + "\n");
	    }
	    inputStream.close();

	    System.out.println(sb.toString());*/
		
		StatementCollector collector = new StatementCollector();
		RDFParser rdfParser = new TurtleParser();
		rdfParser.setRDFHandler(collector);
		rdfParser.parse(inputStream, "http://trololo");

		Collection<Statement> col = collector.getStatements();
		
		return col;
	}
	
	public static String translateProperty(String propertyURL, String lang) throws ClientProtocolException, RDFParseException, RDFHandlerException, IOException, URISyntaxException {
		
		String query = "DESCRIBE <" + propertyURL + ">";
		Collection<Statement> result = SPARQLQuery(query, new java.net.URI("http://dev.morelab.deusto.es/tourspheres/sparql"));
		
		for (Statement st : result) {
			if (st.getObject() instanceof LiteralImpl) {
				LiteralImpl literalObject = (LiteralImpl) st.getObject();
				if (literalObject.getLanguage() != null) {
					if (literalObject.getLanguage().equals(lang)) {
						return literalObject.stringValue();
					}
				}
			}
		}
		
		return propertyURL;
		
	}
}