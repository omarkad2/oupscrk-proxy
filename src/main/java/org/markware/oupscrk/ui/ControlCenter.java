package org.markware.oupscrk.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.markware.oupscrk.config.SSLConfig;
import org.markware.oupscrk.dto.AckDTO;
import org.markware.oupscrk.dto.CommandDTO;
import org.markware.oupscrk.proxy.ProxyServer;
import org.markware.oupscrk.ui.strategy.impl.TCPClientExpositionStartegy;

/**
 * Control center
 * @author citestra
 *
 */
public class ControlCenter {

	/**
	 * Server Socket
	 */
	private ServerSocket serverSocket;

	/**
	 * is Running
	 */
	private boolean running;

	/**
	 * Proxy Server
	 */
	private ProxyServer proxyServer;
	
	/**
	 * Constructor
	 * @param port
	 * @throws IOException
	 */
	public ControlCenter(int port, int proxyPort) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.proxyServer = new ProxyServer(proxyPort, new SSLConfig());
		this.running = true;
	}

	/**
	 * Start listening for remote connections & handle commands
	 */
	public void start() {
		try {
			while(running) {
				Socket clientSocket = this.serverSocket.accept();
				InputStream is = clientSocket.getInputStream();
				byte[] buffer = new byte[1024];
				int read;
				StringBuffer sb = new StringBuffer();
				while((read = is.read(buffer)) != -1) {
					sb.append(new String(buffer, 0, read));
				}
				handleCommand(clientSocket, sb.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handle client requests and sends data back
	 * @param clientSocket
	 * @param data
	 * @throws IOException
	 */
	private void handleCommand(Socket clientSocket, String data) throws IOException {
		CommandDTO request = null;
		// parse to json
		JSONObject jsonObj = new JSONObject(data);
		AckDTO ackToSend = new AckDTO(false, "no command threated");
		if (jsonObj != null) {
			request = new CommandDTO((String) jsonObj.get("command"), jsonArrToList(jsonObj.get("args")));
			switch(request.getCommand()) {
			case "startProxy":
				proxyServer.setProxyOn(true);
				new Thread(new Runnable() {
					public void run() {
						proxyServer.listen();
					}
				}).start();
				ackToSend.setSuccess(true);
				ackToSend = new AckDTO(true, String.valueOf(this.proxyServer.getPort()));
				break;
			case "stopProxy":
				proxyServer.setProxyOn(false);
				ackToSend = new AckDTO(true, String.valueOf(this.proxyServer.getPort()));
				break;
			case "checkProxy":
				if (this.proxyServer.isProxyOn()) {
					ackToSend = new AckDTO(true, String.valueOf(this.proxyServer.getPort()));
				} else {
					ackToSend = new AckDTO(false, String.valueOf(this.proxyServer.getPort()));
				}
				break;
			case "startExpose":
				if (proxyServer.isProxyOn()) {
					proxyServer.setExpositionStrategy(
							new TCPClientExpositionStartegy("127.0.0.1", Integer.parseInt(request.getArgs().get(0))));
					ackToSend = new AckDTO(true, "");
				} else {
					ackToSend = new AckDTO(false, "");
				}
				break;
			case "stopExpose":
				if (proxyServer.isProxyOn()) {
					proxyServer.setExpositionStrategy(null);
					ackToSend = new AckDTO(true, "");
				} else {
					ackToSend = new AckDTO(false, "");
				}
				break;
			default:
				break;
			}
		}
		clientSocket.getOutputStream().write(new JSONObject(ackToSend).toString().getBytes(StandardCharsets.UTF_8));
		clientSocket.close();
	}

	/**
	 * Convert json array to ArrayList
	 * @param jsonObject
	 * @return
	 */
	private ArrayList<String> jsonArrToList(Object jsonObject) {
		ArrayList<String> list = new ArrayList<String>();   
		JSONArray jsonArray = (JSONArray)jsonObject;
		if (jsonArray != null) { 
		   int len = jsonArray.length();
		   for (int i=0;i<len;i++){ 
		    list.add(jsonArray.get(i).toString());
		   } 
		} 
		return list;
	}
}
