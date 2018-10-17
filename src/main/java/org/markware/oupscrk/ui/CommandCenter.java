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
import org.markware.oupscrk.proxy.ProxyServer;
import org.markware.oupscrk.ui.protocol.OupscrkProtocolAck;
import org.markware.oupscrk.ui.protocol.OupscrkProtocolMessage;
import org.markware.oupscrk.ui.strategy.impl.TCPClientExpositionStartegy;

/**
 * Command center (uses TCP socket to communicate with third party users) 
 * @author citestra
 *
 */
public class CommandCenter {

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
	public CommandCenter(int port, int proxyPort) throws IOException {
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
		OupscrkProtocolMessage request = null;
		// parse to json
		JSONObject jsonObj = new JSONObject(data);
		OupscrkProtocolAck ackToSend = new OupscrkProtocolAck(false, "no command threated");
		if (jsonObj != null) {
			request = new OupscrkProtocolMessage((String) jsonObj.get("command"), (String) jsonObj.get("command"));
			switch(request.getCommand()) {
			case START_PROXY:
				proxyServer.setProxyOn(true);
				new Thread(new Runnable() {
					public void run() {
						proxyServer.listen();
					}
				}).start();
				ackToSend.setSuccess(true);
				ackToSend = new OupscrkProtocolAck(true, String.valueOf(this.proxyServer.getPort()));
				break;
			case STOP_PROXY:
				proxyServer.setProxyOn(false);
				ackToSend = new OupscrkProtocolAck(true, String.valueOf(this.proxyServer.getPort()));
				break;
			case CHECK_PROXY:
				if (this.proxyServer.isProxyOn()) {
					ackToSend = new OupscrkProtocolAck(true, String.valueOf(this.proxyServer.getPort()));
				} else {
					ackToSend = new OupscrkProtocolAck(false, String.valueOf(this.proxyServer.getPort()));
				}
				break;
			case START_EXPOSE:
				if (proxyServer.isProxyOn()) {
					proxyServer.setExpositionStrategy(
							new TCPClientExpositionStartegy(request.getPayload()));
					ackToSend = new OupscrkProtocolAck(true, "");
				} else {
					ackToSend = new OupscrkProtocolAck(false, "");
				}
				break;
			case STOP_EXPOSE:
				if (proxyServer.isProxyOn()) {
					proxyServer.setExpositionStrategy(null);
					ackToSend = new OupscrkProtocolAck(true, "");
				} else {
					ackToSend = new OupscrkProtocolAck(false, "");
				}
				break;
			case START_TAMPER_REQUEST:
				if (proxyServer.isProxyOn()) {
//					proxyServer.setRequestHandlingStrategy(request.getPayload());
					ackToSend = new OupscrkProtocolAck(true, "");
				} else {
					ackToSend = new OupscrkProtocolAck(false, "");
				}
				break;
			case STOP_TAMPER_REQUEST:
				if (proxyServer.isProxyOn()) {
					proxyServer.setRequestHandlingStrategy(null);
					ackToSend = new OupscrkProtocolAck(true, "");
				} else {
					ackToSend = new OupscrkProtocolAck(false, "");
				}
				break;
			case START_TAMPER_RESPONSE:
				if (proxyServer.isProxyOn()) {
//					proxyServer.setResponseHandlingStrategy(request.getPayload());
					ackToSend = new OupscrkProtocolAck(true, "");
				} else {
					ackToSend = new OupscrkProtocolAck(false, "");
				}
				break;
			case STOP_TAMPER_RESPONSE:
				if (proxyServer.isProxyOn()) {
					proxyServer.setResponseHandlingStrategy(null);
					ackToSend = new OupscrkProtocolAck(true, "");
				} else {
					ackToSend = new OupscrkProtocolAck(false, "");
				}
				break;
			default:
				break;
			}
		}
		clientSocket.getOutputStream().write(new JSONObject(ackToSend).toString().getBytes(StandardCharsets.UTF_8));
		clientSocket.close();
	}

}
