package org.markware.oupscrk.ui.strategy.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;
import org.markware.oupscrk.ui.protocol.payloads.TCPClientPayload;
import org.markware.oupscrk.ui.strategy.ExpositionStrategy;

public class TCPClientExpositionStartegy implements ExpositionStrategy {

	/**
	 * Payload
	 */
	private TCPClientPayload payload;
	
	/**
	 * Constructor
	 * @param payload
	 */
	public TCPClientExpositionStartegy(String payload) {
		this.payload = TCPClientPayload.payloadDecoder(payload);
	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 */
	@Override
	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse, String contentType) throws IOException{
		OutputStreamWriter out = null;
		Socket expositionSocket = null;
		try {
			expositionSocket = new Socket(payload.getHostname(), payload.getPort());
			out = new OutputStreamWriter(expositionSocket.getOutputStream(), StandardCharsets.UTF_8);
			JSONObject combined = new JSONObject();
			combined.put("request", new JSONObject(httpRequest));
			combined.put("response", new JSONObject(httpResponse));
			combined.put("contentType", contentType);
			out.write(combined.toString());
			out.flush();
		} finally {
			if (expositionSocket != null) {
				out.close();
				expositionSocket.close();
			}
		}
		
	}
	
}
