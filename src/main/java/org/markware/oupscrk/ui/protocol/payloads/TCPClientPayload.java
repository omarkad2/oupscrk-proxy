package org.markware.oupscrk.ui.protocol.payloads;

import org.json.JSONObject;

/**
 * TCP Client Info Client -> Proxy {hostname, port}
 * @author citestra
 *
 */
public class TCPClientPayload {

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
	public TCPClientPayload(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * @param payloadJson
	 * @return json converted to object payload
	 */
	public static TCPClientPayload payloadDecoder(String payloadJson) {
		JSONObject obj = new JSONObject(payloadJson);
		return new TCPClientPayload(obj.getString("hostname"), obj.getInt("port"));
		
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}
}
