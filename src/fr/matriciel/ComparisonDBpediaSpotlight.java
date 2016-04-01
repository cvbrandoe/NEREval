package fr.matriciel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.Text;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Relevant information concerning DBpedia spotlight: 
 * http://succeed-project.eu/wiki/index.php/DBPedia_Spotlight.
 * @author Brando
 *
 */
public class ComparisonDBpediaSpotlight {

	private final static String API_URL = "http://localhost:2222/"; //a local instance of the server must be available
	//private final static String API_URL = "http://spotlight.sztaki.hu:2225/"; //French DBpedia spotlight server
	private static final double CONFIDENCE = 0.0;
	//private static final int SUPPORT = 0;
	
	public List<HashMap<String, String>> extract(Text text)
			throws AnnotationException {

		List<HashMap<String, String>> resources = new ArrayList<HashMap<String, String>>();		
		try {
			GetMethod getMethod = new GetMethod(API_URL + "rest/annotate/?"
					+ "confidence=" + CONFIDENCE
					+ "&text=" + URLEncoder.encode(text.text(), "utf-8"));
			getMethod.addRequestHeader(new Header("Accept", "application/json"));
			String spotlightResponse = AnnotationClient.request(getMethod);
			assert spotlightResponse != null;
			JSONObject resultJSON = null;
			JSONArray entities = null;			
			if (spotlightResponse.startsWith("{")) {
				resultJSON = new JSONObject(spotlightResponse);
				if (resultJSON.has("Resources")) {
					entities = resultJSON.getJSONArray("Resources");
				}
			}			
			if (entities != null) {
				for (int i = 0; i < entities.length(); i++) {
					JSONObject entity = entities.getJSONObject(i);
					if (entity.getString("@types").toLowerCase().contains("place")) {					
						HashMap<String, String> resource = new HashMap<String, String>();
						resource.put("uri", entity.getString("@URI"));
						resource.put("support", entity.getString("@support"));
						resource.put("types", entity.getString("@types"));
						resource.put("surfaceForm", entity.getString("@surfaceForm"));
						resource.put("offset", entity.getString("@offset"));
						resources.add(resource);
					}
				}
			}
		} catch (Exception e) {
			throw new AnnotationException(e);
		}
		return resources;
	}

	public static void main(String[] args) throws Exception {

		FileWriterWithEncoding out = new FileWriterWithEncoding("out/oscar-annot.txt", "UTF-8");
		FileWriterWithEncoding out2 = new FileWriterWithEncoding("out/oscar-selectedEnt.txt", "UTF-8");
		BufferedReader br = new BufferedReader(new FileReader("corpus/Oscar/original.txt"));
		try {
		    String line;
			while ( (line = br.readLine() ) != null ) {
		        ComparisonDBpediaSpotlight c = new ComparisonDBpediaSpotlight();
		        StringBuilder [] phrases = splitPhrases(line, 200); //whole text impossible, but limited by REST
		        String[] words = null;
		        for (StringBuilder phrase : phrases) {
		        	
		        	if (phrase != null) {
						List<HashMap<String, String>> responses = c.extract(new Text(phrase.toString()));
						if (responses.size() > 0) {
						
							int countResponses = 0;
							HashMap<String, String> response = responses.get(countResponses);
							int latestOffset = Integer.parseInt(response.get("offset"));
							words = phrase.toString().split(" "); 
							int countWords = 0;
							int countCharsInWord = words[countWords].length();
							int countSize = 0;
							while (countWords < words.length) {
								
								if (countCharsInWord + 1 == latestOffset) { 
									
									//write token found before the presumed entity
									if (countSize == 0) {
										out.write(words[countWords]);
										out.write(" ");	
									}
									//write the presumed entity
									String presumedEntity = response.get("surfaceForm");
									out.write("{["+presumedEntity+"]}");
									countWords++;
									countCharsInWord += 1 + presumedEntity.length();
									countSize = presumedEntity.split(" ").length;
									out2.write(presumedEntity+ "\n");
									out2.write(response.get("uri")+ "\n");
									out2.write(response.get("offset")+ "\n");
									out.write(" ");
									//for next time
									countResponses++;								
									if (countResponses < responses.size()) {									
										response = responses.get(countResponses);
									}
									latestOffset = Integer.parseInt(response.get("offset"));									
								} else {
									if (countSize == 0) {
										out.write(words[countWords]);
										out.write(" ");
									} else
										countSize--;
									//for next time
									countWords++;
									if (countWords < words.length) {
										countCharsInWord += 1 + words[countWords].length();
									}
								}								
							}
							
						}
		        	}
				}		       
			}
		} finally {
		    br.close();
		}
		out.close();
		out2.close();
	}

	public static StringBuilder[] splitPhrases(String input, int maxWordsInPhrase) {

		int size = input.split(" ").length;
		StringBuilder [] phrases = new StringBuilder[ ( size / maxWordsInPhrase) + 1];
		StringTokenizer tok = new StringTokenizer(input, " ");
		int countW = 0, newIndex = 0;
		while (tok.hasMoreTokens()) {
			String word = tok.nextToken();
			if ( (size / maxWordsInPhrase) ==  (countW / maxWordsInPhrase) ) //it's the last one
				newIndex = (countW / maxWordsInPhrase) - 1;
			else
				newIndex = countW / maxWordsInPhrase;
			
			if (phrases[newIndex] == null) {
				phrases[newIndex] = new StringBuilder();
			} 
			phrases[newIndex].append(word + " ");
			countW++;			
		}
		return phrases;
	}

}
