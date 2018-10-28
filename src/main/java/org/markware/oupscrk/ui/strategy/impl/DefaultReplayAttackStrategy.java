package org.markware.oupscrk.ui.strategy.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;
import org.markware.oupscrk.http.parser.HttpResponseParser;
import org.markware.oupscrk.ui.protocol.payloads.ReplayAttackPayload;
import org.markware.oupscrk.ui.strategy.ReplayAttackStrategy;
/**
 * Default strategy TODO: Use factory only one instance is allowed
 * @author citestra
 *
 */
public class DefaultReplayAttackStrategy implements ReplayAttackStrategy {

	/**
	 * Headers to remove
	 */
	private static final List<String> HEADERS_TO_REMOVE = Collections.unmodifiableList(
			Arrays.asList("Connection", "Proxy-Authenticate", "Keep-Alive", "Content-Length",
					"Proxy-Authorization", "te", "Trailers", "Transfer-Encoding", "Upgrade"));
	 
	/**
	 * Payload
	 */
	private ReplayAttackPayload payload;
	
	/**
	 * Is it already replaying ?
	 */
	private boolean engaged;
	
	/**
	 * Constructor
	 * @param payload
	 */
	public DefaultReplayAttackStrategy(String payload) {
		this.payload = ReplayAttackPayload.payloadDecoder(payload);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void replay(HttpRequest initialHttpRequest) {
		
		if (this.payload != null && this.payload.getWordList() != null
				&& initialHttpRequest.containString(this.payload.getSeed())) {
			this.engaged = true;
			this.payload.getWordList().forEach((word) -> {
				new Thread(() -> {
					OutputStreamWriter out = null;
					Socket attackFeedSocket = null;
					try {
						// Init output socket Proxy -> Client
						attackFeedSocket = new Socket(payload.getHostname(), payload.getPort());
						out = new OutputStreamWriter(attackFeedSocket.getOutputStream(), StandardCharsets.UTF_8);
						
						// Set word instead of seed
						HttpRequest httpRequest = 
								initialHttpRequest.replaceString(this.payload.getSeed(), word);
						
						// Send request and get response Proxy -> Server
						String protocolHttp = httpRequest.getScheme();

						URL url = httpRequest.getUrl();
						// HTTP Connection
						HttpURLConnection conn;
						if ("https".equals(protocolHttp)) {
							conn = (HttpsURLConnection)url.openConnection();
						} else {
							conn = (HttpURLConnection)url.openConnection();
						}
						
						// Set Request Method
						conn.setRequestMethod(httpRequest.getRequestType());

						// Set headers (filtering out proxy headers)
						if (httpRequest.getHeaders() != null) {
							filterReplayHeaders(httpRequest.getHeaders()).forEach((key, value) -> {
								conn.setRequestProperty(key, value);
							});
						}

						// Send body if there is one
						String requestBody = httpRequest.getMessageBody();
						System.out.println("Word : " + requestBody);
						if (requestBody != null && !requestBody.isEmpty()) {
							conn.setDoOutput(true);
							OutputStream os = conn.getOutputStream();
							OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);    
							osw.write(requestBody);
							osw.flush();
							osw.close(); 
							os.close();
						}

						HttpResponse httpResponse = HttpResponseParser.parseResponse(conn);
						
						// Send results back (request + response)
						JSONObject combined = new JSONObject();
						combined.put("word", word);
						combined.put("request", new JSONObject(httpRequest));
						combined.put("response", new JSONObject(httpResponse));
						out.write(combined.toString());
						out.flush();
					} catch (IOException | DataFormatException e) {
						System.out.println("*** [REPLAY] DOGET EXCEPTION ***: " + initialHttpRequest.getHostname() + " : " + e);
					} finally {
						if (attackFeedSocket != null) {
							try {
								out.close();
								attackFeedSocket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}).start();
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEngaged() {
		return this.engaged;
	}
	
	/**
	 * Filter-out proxy specific headers
	 * @param headers
	 * @return filtered headers
	 */
	private Map<String, String> filterReplayHeaders(Hashtable<String, String> headers) {
		Entry<String, String> allowedEncodings = 
				new AbstractMap.SimpleEntry<String, String>("Accept-Encoding", "gzip, deflate, identity, x-gzip");
		return headers.entrySet().stream().filter((header) -> !HEADERS_TO_REMOVE.contains(header.getKey()))
									.map((header) -> "Accept-Encoding".equals(header.getKey()) ? allowedEncodings : header)
								   .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

}
