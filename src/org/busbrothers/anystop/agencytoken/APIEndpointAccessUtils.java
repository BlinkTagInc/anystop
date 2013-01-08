/** This class contains a bunch of static methods that are useful for doing various 
 * internet operations, like fetching the text of a document residing in a given 
 * URL, and fetching multiple URLs using multi-threaded access
 */

package org.busbrothers.anystop.agencytoken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class APIEndpointAccessUtils {
	
	/** This method will return the JSON-formatted string residing at URL "urlString"
	 * @param urlString String representing URL to fetch from
	 * @return String located at the URL represeted by urlString
	 */
	public static String jsonStringFromURLString(String urlString) {
		URL targetURL;
		try { targetURL = new URL(urlString); } 
		catch (MalformedURLException e) { System.err.println(e); return(null);	}
		
		InputStream is;
		try { is = targetURL.openStream(); }
		catch (IOException e) { System.err.println(e); return(null); }
		
		try {
		  BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		  String jsonText = readAll(rd);
		  is.close();
		  return(jsonText);
		} catch (Exception e) { System.err.println(e); return(null); } 		
	}
	
	//This method lifted from http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
		/** This method returns the String representation that can be extracted from the Reader 'rd'
		 * 
		 * @param rd Reader to extract String from
		 * @return String extracted from rd.
		 * @throws IOException
		 */
		private static String readAll(Reader rd) throws IOException {
		    StringBuilder sb = new StringBuilder();
		    int cp;
		    while ((cp = rd.read()) != -1) {
		      sb.append((char) cp);
		    }
		    return sb.toString();
		}
}
