package org.markware.oupscrk.ui.protocol.payloads;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ReplayAttackPayload {

	/**
	 * Hostname
	 */
	private String hostname;
	
	/**
	 * Port
	 */
	private int port;
	
	/**
	 * Random seed
	 */
	private String seed;
	
	/**
	 * Word list
	 */
	private List<String> wordList;
	
	/**
	 * Constuctor
	 * @param hostname
	 * @param port
	 */
	public ReplayAttackPayload(
			String hostname, 
			int port,
			String seed,
			List<String> wordList) {
		this.hostname = hostname;
		this.port = port;
		this.seed = seed;
		this.wordList = wordList;
	}

	/**
	 * @param payloadJson
	 * @return json converted to object payload
	 */
	public static ReplayAttackPayload payloadDecoder(String payloadJson) {
		JSONObject obj = new JSONObject(payloadJson);
		return new ReplayAttackPayload(
				obj.getString("hostname"), 
				obj.getInt("port"),
				obj.getString("seed"),
				toList(obj.getJSONArray("wordList")));
		
	}
	
	/**
	 * Convert jsonArray to list of strings
	 * @param jsonArr
	 * @return list of strings
	 */
	private static List<String> toList(JSONArray jsonArr) {
		List<String> list = new ArrayList<String>();
		if (jsonArr != null) {
			jsonArr.forEach((elt) -> {
				list.add((String) elt);
			});
		}
		return list;
	}
	///////////////////////////////////// GETTERS/SETTERS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getSeed() {
		return seed;
	}

	public void setSeed(String seed) {
		this.seed = seed;
	}

	public List<String> getWordList() {
		return wordList;
	}

	public void setWordList(List<String> wordList) {
		this.wordList = wordList;
	}
	
	
}
