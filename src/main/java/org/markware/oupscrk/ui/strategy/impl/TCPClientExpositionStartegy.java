package org.markware.oupscrk.ui.strategy.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;
import org.markware.oupscrk.ui.strategy.ExpositionStrategy;

public class TCPClientExpositionStartegy implements ExpositionStrategy {

	private Payload payload;
	
	private Socket expositionSocket;
	
	public TCPClientExpositionStartegy(String payload) {
		this.payload = Payload.payloadDecoder(payload);
	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 */
	@Override
	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException{
		OutputStreamWriter out = null;
		try {
			this.expositionSocket = new Socket(payload.getHostname(), payload.getPort());
			out = new OutputStreamWriter(this.expositionSocket.getOutputStream(), StandardCharsets.UTF_8);
			JSONObject combined = new JSONObject();
			combined.put("request", new JSONObject(httpRequest));
			combined.put("response", new JSONObject(httpResponse));
			out.write(combined.toString());
			out.flush();
		} finally {
			if (this.expositionSocket != null) {
				out.close();
				this.expositionSocket.close();
			}
		}
		
	}
	
	/**
	 * Payload
	 * @author citestra
	 *
	 */
	private static class Payload {
		
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
		public Payload(String hostname, int port) {
			this.hostname = hostname;
			this.port = port;
		}
		
		/**
		 * @param payloadJson
		 * @return json converted to object payload
		 */
		public static Payload payloadDecoder(String payloadJson) {
			JSONObject obj = new JSONObject(payloadJson);
			return new Payload(obj.getString("hostname"), obj.getInt("port"));
			
		}

		public String getHostname() {
			return hostname;
		}

		public int getPort() {
			return port;
		}
	}

}
