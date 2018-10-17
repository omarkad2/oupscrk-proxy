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

	private String hostname;
	
	private int port;
	
	private Socket expositionSocket;
	
	public TCPClientExpositionStartegy(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	public TCPClientExpositionStartegy(Socket clientSocket) {
		this.expositionSocket = clientSocket;
	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 */
	@Override
	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException{
		OutputStreamWriter out = null;
		try {
			this.expositionSocket = new Socket(hostname, port);
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

}
