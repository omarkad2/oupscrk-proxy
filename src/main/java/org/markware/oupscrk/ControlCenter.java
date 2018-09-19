package org.markware.oupscrk;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.markware.oupscrk.ui.impl.TCPClientExpositionStartegy;

/**
 * Control center
 * @author omarkad
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

	private ProxyServer proxyServer;
	
	private String sslConfigFolder;
	
	/**
	 * Constructor
	 * @param port
	 * @throws IOException
	 */
	public ControlCenter(int port, int proxyPort, String sslConfigFolder) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.sslConfigFolder = sslConfigFolder;
		this.proxyServer = new ProxyServer(proxyPort, new SSLConfig(this.sslConfigFolder));
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

	private void handleCommand(Socket clientSocket, String data) throws IOException {
		CommandDTO request = null;
		boolean terminate = true;
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
				ackToSend = new AckDTO(true, "");
				break;
			case "checkProxy":
				ackToSend = new AckDTO(true, this.proxyServer.isProxyOn() ? "Proxy is Up" : "Proxy is Down");
				break;
			case "startExpose":
				if (proxyServer.isProxyOn()) {
					proxyServer.setExpositionStrategy(new TCPClientExpositionStartegy(clientSocket));
					terminate = false;
					ackToSend = new AckDTO(true, "");
				} else {
					ackToSend = new AckDTO(false, "");
				}
				break;
			case "stopExpose":
				break;
			default:
				break;
			}
		}
		clientSocket.getOutputStream().write(new JSONObject(ackToSend).toString().getBytes(StandardCharsets.UTF_8));
		if (terminate) {
			clientSocket.close();
		}
	}

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
