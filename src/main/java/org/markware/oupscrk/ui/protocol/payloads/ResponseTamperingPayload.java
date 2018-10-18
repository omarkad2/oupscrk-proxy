package org.markware.oupscrk.ui.protocol.payloads;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Response tampering payload Client -> Proxy
 * @author citestra
 *
 */
public class ResponseTamperingPayload {

	/*
	 { headers: [
	 		"key": "value"
	 	],
	 	replaceText: [
	 		"text1" : "replacement1",
	 		"text2" : "replacement2"
	 	],
	 	replaceImages: [
	 		"extesion": "image"
	 	]
	 */
	/**
	 * Headers to tamper with or to add
	 */
	private Map<String, String> headersToTamper;
	
	/**
	 * Text to replace
	 */
	private Map<String, String> bodyReplacements;
	
	/**
	 * Constructor
	 * @param headers
	 * @param bodyTexts
	 */
	private ResponseTamperingPayload(
			Map<String, String> headers, 
			Map<String, String> bodyTexts) {
		this.headersToTamper = headers;
		this.bodyReplacements = bodyTexts;
	}
	
	/**
	 * @param payloadJson
	 * @return json converted to object payload
	 */
	public static ResponseTamperingPayload payloadDecoder(String payloadJson) {
		JSONObject obj = new JSONObject(payloadJson);
		return new ResponseTamperingPayload(
				toMap((JSONObject) obj.get("headersToTamper")), 
				toMap((JSONObject) obj.get("bodyReplacements")));
	}
	
	/**
	 * @param jsonobj
	 * @return Convert json object to map
	 * @throws JSONException
	 */
	public static Map<String, String> toMap(JSONObject jsonobj)  throws JSONException {
       Map<String, String> map = new HashMap<String, String>();
       if (jsonobj != null) {
    	   jsonobj.keySet().stream().forEach((key) -> {
    		   Object value = jsonobj.get(key);
    		   if (value instanceof String) {
    			   map.put(key, (String) value);
    		   }
    	   });
       }
       return map;
   }

	/////////////////////////////// GETTERS/SETTERS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	public Map<String, String> getHeadersToTamper() {
		return headersToTamper;
	}

	public Map<String, String> getBodyReplacements() {
		return bodyReplacements;
	}
	
}
