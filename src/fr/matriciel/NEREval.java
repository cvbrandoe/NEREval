package fr.matriciel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import scala.collection.mutable.HashSet;

/**
 * 
 * @author Capeyron, Dominguès & Brando
 * 
 * Evaluation of the task for named-entity detection (classification is not considered) according to the State of art (Nouvel et al. 2015)
 * The following is an algorithm for alignment of named-entities annotations (and their context) in two texts.
 * - places are annotated using the following symbols: {[place1]} and/or {[#place2#]} 
 * - contents of both input files must be identical except obviously their annotations
 * - parenthesis can only be used for indicating the presence of geographic coordinates which are actually not mandatory 
 */
public class NEREval {
	
	public static void main(String[] args) {
		
		//we're sure these two files correspond perfectly
		String content1 = cleanUpInput(args[0]);
		String content2 = cleanUpInput(args[1]); 
		
		//C : total number of annotated objects in the annotated file that are correct
		Double c = 0.0;
		//I : total number of adds performed by the system, in other words, objects that are not named-entities but the system considered as ones
		Double i = 0.0;
		//D: total number of omissions (deletions) performed by the system, in other words, non-detected entities
		Double d = 0.0;
		//total number of named-entity (mentions?) in the gold
		Double n = 0.0;
			
		Double[] te = countMetrics(content1, content2, Integer.parseInt(args[2]));
		//Character threshold represents the difference between both input files in terms of characters, 
		//1 or 2 indicate both files are almost identical (besides characters used by the annotations such as [ or })
		//7 or 8 indicate that both files are quite different, besides annotations, there are sometimes letters or punctuation that differs considerably
		d = te[0];
		c = te[1];		
		n = te[2];
		i = te[3];
				
		System.out.println();
		System.out.println("Values for NER evaluation");
		System.out.println("C (total number of annotated objects in the annotated file that are correct): "+c);
		System.out.println("D (total number of omissions (deletions) performed by the system): "+d);
		System.out.println("I (total number of adds performed by the system): "+i);
		System.out.println("N (total number of named-entity mentions in the gold): "+n); 
		
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
		//here equal importance is given to recall and precision
		
		//errors
		System.out.println();
		System.out.println("ERR(v1) = D + I / C + D + I: "+ (d + i) / (c + d + i));
		
		System.out.println();
		System.out.println("ERR(v2) = D + I / N + I: "+ (d + i) / (n + i));
		
		//System.out.println();
		//System.out.println("SER = D + I / R (for us N) : "+ (d + i) / n );
		//here, we give equal importance to each error
	
	}
	
	/**
	 * Computes correct entities detected and deletions performed by the system by comparing the gold and the hypothesis.
	 * @param c
	 * @param d
	 * @param matches1
	 * @param matches2
	 * @return
	 */
	private static Double[] countMetrics(String content1, String content2, int charthreshold) {

		//match context of NE in both texts
		List<String> matches1 = matchENS(content1);
		List<String> matches2 = matchENS(content2);
		System.out.println("Nb of entities in the reference: "+matches1.size());
		System.out.println("Nb of entities in the hypothesis: "+matches2.size());

		Double n = 0.0, c = 0.0, d = 0.0, i = 0.0;
		HashSet<Integer> seenIndices = new HashSet<Integer>();
		for (String gold : matches1) {
			n++;
			String[] valsGold = gold.split("\\|");
			String goldNE = valsGold[2];
			Boolean found = false;
			for (String annot : matches2) {
				String[] valsAnnot = annot.split("\\|");
				String annotNE = valsAnnot[2];
				
				if (overlaps(Integer.parseInt(valsGold[0].trim()), Integer.parseInt(valsGold[1].trim()), 
						Integer.parseInt(valsAnnot[0].trim()), Integer.parseInt(valsAnnot[1].trim()), charthreshold) ) {
					goldNE = goldNE.replaceAll("#", "");
					if (goldNE.equals(annotNE)) {
						found = true;
						seenIndices.add(matches2.indexOf(annot));
						System.out.println("Good match: gold (indices:"+valsGold[0].trim() + " - "+ valsGold[1].trim() + " indicesOrig:"+valsGold[3].trim() + " - "+ valsGold[4].trim() + ") is "+goldNE+ 
								" and annot (indices:"+valsAnnot[0].trim() + " - "+ valsAnnot[1].trim() + " indicesOrig:"+valsAnnot[3].trim() + " - "+ valsAnnot[4].trim() + ") is "+annotNE);
						System.out.println();
						c++;
					}
				}				
			}
			if (!found) {
				d++;
				System.out.println("Wrong match, system ommited: "+ goldNE +"(indices:"+valsGold[0].trim() + " - "+ valsGold[1].trim() + " indicesOrig:"+valsGold[3].trim() + " - "+ valsGold[4].trim() + ") ");
				System.out.println();
			}
			
		}
		
		i = new Double (matches2.size() - seenIndices.size());
		for (int l = 0 ; l < matches2.size(); l++) {
			if (!seenIndices.contains(l)) {	
				String t[] = matches2.get(l).split("\\|");
				System.out.println("Wrong match, system added: "+ t[2] +"(indices:"+t[0].trim() + " - "+ t[1].trim() + " indicesOrig:"+t[3].trim() + " - "+ t[4].trim() + ")");
				System.out.println();
		
				
			}
		}
		Double [] out = {d, c, n, i};
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
	public static boolean overlaps(Integer ini1, Integer fin1, Integer ini2, Integer fin2, int dummy) {
		ini1 = ini1 - dummy;
		fin1 = fin1 + dummy;
		
		ini2 = ini2 - dummy;
		fin2 = fin2 + dummy;
		
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
	 * Extracts named entities enclosed by square brackets and provides information about start and end indexes. 
	 * @param contentF
	 * @return
	 */
	public static List<String> matchENS(String contentF) {
		
		contentF = contentF.replaceAll("\\{", "");
        contentF = contentF.replaceAll("\\}", "");
		//System.out.println(contentF);		
		List<String> result = new ArrayList<String>();
		String patternString = "\\[([^]]+)\\]";		
        Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(contentF);
		int countC = 0;
	    while(matcher.find()) {
	        String valENS = matcher.group();
	        int iniOrig = matcher.start();
	        int finOrig = matcher.end();	     
	        int ini = iniOrig - countC;	        
	        countC = countC + StringUtils.countMatches(valENS, "]") +
	        		StringUtils.countMatches(valENS, "[") +
	        		StringUtils.countMatches(valENS, "#");	        
	        int fin = finOrig - countC;	
	        if (!valENS.contains("#")) {
	        	result.add(ini + " | " + fin + " | " + valENS+ " | " + iniOrig + " | " + finOrig);
	        }
	        //System.out.println(ini + " - " + fin + " - " + valENS);
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
	        contentF = contentF.replaceAll("  ", " ");
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
