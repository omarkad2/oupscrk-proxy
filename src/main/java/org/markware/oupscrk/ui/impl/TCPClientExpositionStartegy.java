package org.markware.oupscrk.ui.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.markware.oupscrk.ui.ExpositionStrategy;
import org.markware.oupscrk.utils.HttpRequest;
import org.markware.oupscrk.utils.HttpResponse;

public class TCPClientExpositionStartegy implements ExpositionStrategy {

	private Socket expositionSocket;
	
	public TCPClientExpositionStartegy(String hostname, int port) {
		try {
			this.expositionSocket = new Socket(hostname, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			out = new OutputStreamWriter(this.expositionSocket.getOutputStream(), StandardCharsets.UTF_8);
			JSONObject combined = new JSONObject();
			combined.put("request", new JSONObject(httpRequest));
			combined.put("response", new JSONObject(httpResponse));
			out.write(combined.toString());
			out.flush();
		} finally {
			if (out != null) {
				out.close();
			}
		}
		
	}

}
