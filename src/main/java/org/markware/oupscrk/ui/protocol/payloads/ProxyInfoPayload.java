package org.markware.oupscrk.ui.protocol.payloads;

import org.json.JSONObject;

/**
 * Proxy info : Proxy -> Client {hostname, port}
 * @author citestra
 *
 */
public class ProxyInfoPayload {

	/**
	 * Hostname
	 */
	private String hostname;
	
	/**
	 * Port
	 */
	private int port;
	
	/**
	 * Constuctor
	 * @param hostname
	 * @param port
	 */
	public ProxyInfoPayload(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * @param payloadJson
	 * @return json converted to object payload
	 */
	public static String payloadEncoder(String hostname, int port) {
		JSONObject jsonObj = new JSONObject(
				new ProxyInfoPayload(hostname != null ? hostname : "localhost", port));
		return jsonObj.toString();
	}

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

	
}
