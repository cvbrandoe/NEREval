package fr.matriciel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Capeyron, Dominguès & Brando
 * 
 * Evaluation of the task for named-entity detection (classification is not considered) according to the State of art (Nouvel et al. 2015)
 * The following is an algorithm for alignment of named-entities annotations (and their context) in two texts.
 * - contents of both input files must be identical except obviously their annotations
 * - parenthesis can only be used for indicating the presence of geographic coordinates which are actually not mandatory
 * 
 */
public class NEREval {
	
	public static void main(String[] args) {
		
		//we're sure these two files correspond perfectly
		String content1 = cleanUpInput(args[0]);
		String content2 = cleanUpInput(args[1]); 
		
		//match context of NE in both texts
		List<String> matches1 = matchENS(content1);
		List<String> matches2 = matchENS(content2);
	
		//C : total number of annotated objects in the annotated file that are correct
		Double c = 0.0;
		//I : total number of adds performed by the system, in other words, objects that are not named-entities but the system considered as ones
		Double i = 0.0;
		//D: total number of omissions (deletions) performed by the system, in other words, non-detected entities
		Double d = 0.0;
		//total number of named-entity (mentions?) in the gold
		Double n = 0.0;
			
		Double[] te = calculateForRappel(c, d, matches1, matches2);
		d = te[0];
		c = te[1];
		i = calculateForPrecision(i, matches1, matches2);
		n = te[2];
				
		System.out.println();
		System.out.println("Values for NER evaluation");
		System.out.println("C (total number of annotated objects in the annotated file that are correct): "+c);
		System.out.println("D (total number of omissions (deletions) performed by the system): "+d);
		System.out.println("I (total number of adds performed by the system): "+i);
		System.out.println("N (total number of named-entity (mentions?) in the gold): "+n); //TODO unique entities or mentions? 
		
		// précision : le ratio entre le nombre de réponses correctes et toutes les réponses données par un système. 
		// ou bien precision = right tagging / (right tagging + wrong tagging)
		System.out.println();
		Double p = c / (c + i);
		System.out.println("Precision P = C / (C + I): "+ p);
		
		// rappel : le ratio entre le nombre de réponses correctes et le nombre des réponses attendues. 
		// ou bien recall = right tagging / (right tagging + omitted tagging)
		System.out.println();
		Double r = c / (c + d);
		System.out.println("Recall R = C / (C + D): "+r);
		
		//f-score
		System.out.println();
		System.out.println("F-mesure F = 2 * ( (P * R) / (P + R) ): "+ 2 * ( (p * r) / (p + r) ));
		//TODO here equal importance is given to recall and precision, is this what we want?
		
		//errors
		System.out.println();
		System.out.println("ERR(v1) = D + I / C + D + I: "+ (d + i) / (c + d + i));
		
		System.out.println();
		System.out.println("ERR(v2) = D + I / N + I: "+ (d + i) / (n + i));
		
		System.out.println();
		System.out.println("SER = D + I / R (for us N) : "+ (d + i) / n );
		//TODO here, we give igual importance to each error, again is this right for us?
		
		//ETER: for nested named entities (TODO could eventually be useful?)  
	}
	
	/**
	 * Computes insertions performed by the system by comparing the gold and the hypothesis.
	 * @param i
	 * @param matches1
	 * @param matches2
	 * @return
	 */
	private static Double calculateForPrecision(Double i, List<String> matches1, List<String> matches2) {
	
		for (String annot : matches2) {
			String[] valsAnnot = annot.split("\\|");
			List<String> nesAnnot = extractENS(valsAnnot[2]);

			for (String neAnnot : nesAnnot) {
				
				Boolean foundInAnnot2 = false;
				for (String gold : matches1) {
					
					String[] valsGold = gold.split("\\|");
					List<String> nesGold = extractENS(valsGold[2]);
					int index = 0;
					Boolean foundInAnnot = false;
					while (!foundInAnnot && index < nesGold.size() ) {
						
						String neGold = nesGold.get(index);						
						//match context
						if (overlaps(Integer.parseInt(valsAnnot[0].trim()), Integer.parseInt(valsAnnot[1].trim()), 
								Integer.parseInt(valsGold[0].trim()), Integer.parseInt(valsGold[1].trim())) ) {
							
							if (neAnnot.equals(neGold)) {
								foundInAnnot = true;	
								foundInAnnot2 = true;
							}
						}						
						index++;
					}					
				}
				if (!foundInAnnot2) {
					i++;	
					System.out.println("Wrong match, system added: "+ neAnnot +"(context:"+valsAnnot[0].trim() + " - "+ valsAnnot[1].trim() + ")");
					System.out.println();
				}
			}				
		}
		return i;
	}

	/**
	 * Computes correct entities detected and deletions performed by the system by comparing the gold and the hypothesis.
	 * @param c
	 * @param d
	 * @param matches1
	 * @param matches2
	 * @return
	 */
	private static Double[] calculateForRappel(Double c, Double d, List<String> matches1, List<String> matches2) {
		
		Double n = 0.0;
		for (String gold : matches1) {
			String[] valsGold = gold.split("\\|");
			List<String> nesGold = extractENS(valsGold[2]);

			for (String neGold : nesGold) {
				n++;
				Boolean foundInAnnot2 = false;
				for (String annot : matches2) {
					
					String[] valsAnnot = annot.split("\\|");
					List<String> nesAnnot = extractENS(valsAnnot[2]);
					int index = 0;
					Boolean foundInAnnot = false;
					String neAnnotStore = "";
					while (!foundInAnnot && index < nesAnnot.size() ) {
						
						String neAnnot = nesAnnot.get(index);						
						//match context
						if (overlaps(Integer.parseInt(valsGold[0].trim()), Integer.parseInt(valsGold[1].trim()), 
								Integer.parseInt(valsAnnot[0].trim()), Integer.parseInt(valsAnnot[1].trim())) ) {
							
							if (neGold.equals(neAnnot)) {
								foundInAnnot = true;	
								neAnnotStore = neAnnot;
								foundInAnnot2 = true;
							}
						}						
						index++;
					}
					//found match of neGold in annot
					if (foundInAnnot) {							
						System.out.println("Good match: gold (context:"+valsGold[0].trim() + " - "+ valsGold[1].trim() + ") is "+neGold+ 
								" and annot (context:"+valsAnnot[0].trim() + " - "+ valsAnnot[1].trim() + ") is "+neAnnotStore);
						System.out.println();
						c++;
					}
				}
				if (!foundInAnnot2) {
					d++;
					System.out.println("Wrong match, system ommited: "+ neGold +"(context:"+valsGold[0].trim() + " - "+ valsGold[1].trim() + ")");
					System.out.println();
				}
			}				
		}
		Double [] out = {d, c, n};
		return out;
	}

	/**
	 * Verifies whether two text segments overlap according to their position in both texts.
	 * @param ini1
	 * @param fin1
	 * @param ini2
	 * @param fin2
	 * @return
	 */
	public static boolean overlaps(Integer ini1, Integer fin1, Integer ini2, Integer fin2) {
		//none overlapping
		if ( (ini1 > ini2 && ini1 > fin2 ) && (fin1 > ini2 && fin1 > fin2) )
			return false;
		else if ( (ini2 > ini1 && ini2 > fin1 ) && (fin2 > ini1 && fin2 > fin1) )
			return false;
		//complete overlapping one-side
		else if (ini1 <= ini2 && fin1 >= fin2 )
			return true;
		//complete overlapping other-side
		else if (ini2 <= ini1 && fin2 >= fin1 )
			return true;
		//partial overlapping one-side
		else if (ini1 < ini2 && fin1 < fin2 )
			return true;
		//partial overlapping other-side
		else if (ini2 < ini1 && fin2 < fin1 )
			return true;		
		return false;
	}

	/**
	 * Extracts list of segments containing a named-entity and its context enclosed by curly brackets, 
	 * also information about context is given such as start and end indexes. 
	 * @param contentF
	 * @return
	 */
	public static List<String> matchENS(String contentF) {
		
		//System.out.println(contentF);
		List<String> result = new ArrayList<String>();
	    String patternString = "\\{([^}]+)\\}";
        Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(contentF);
		int countC = 0;
	    while(matcher.find()) {
	        String valENS = matcher.group();
	        int ini = matcher.start() - countC;
	        countC = countC + StringUtils.countMatches(valENS, "}") + 
	        		StringUtils.countMatches(valENS, "{") + StringUtils.countMatches(valENS, "(") + 
	        		StringUtils.countMatches(valENS, ")") + StringUtils.countMatches(valENS, "]") +
	        		StringUtils.countMatches(valENS, "[");
	        int fin = matcher.end() - countC;	        
	        result.add(ini + " | " + fin + " | " + valENS);
	        //System.out.println(ini + " - " + fin + " - " + valENS);
	    }
	    return result;
	}
	
	/**
	 * Extracts named entities enclosed by [].
	 * @param contentF
	 * @return
	 */
	public static List<String> extractENS(String contentF) {
		
		List<String> result = new ArrayList<String>();
	    String patternString = "\\[([^]]+)\\]";
        Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(contentF);
	    while(matcher.find()) {
	        String valENS = matcher.group();
	        result.add(valENS);	       
	    }
	    return result;
	}
	
	/**
	 * Removes line feed and geographic coordinates in parenthesis.
	 * @param fileN
	 * @return
	 */
	public static String cleanUpInput(String fileN) {
		
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileN));
			//stores content into single string
			StringBuilder  stringBuilder = new StringBuilder();
			while ((sCurrentLine = br.readLine()) != null) {			
				stringBuilder.append( sCurrentLine );
			}		    		    
		    //remove coordinates
	        String contentF = stringBuilder.toString().replaceAll("\\([^\\)]+\\)", "");
	        return contentF;
	        
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

}
