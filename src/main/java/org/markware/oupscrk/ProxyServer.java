package org.markware.oupscrk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.markware.oupscrk.ui.impl.LogFileExpositionStrategy;

/**
 * Main thread
 * @author citestra
 *
 */
public class ProxyServer {

	private static ServerSocket proxySocket;

	/**
	 * Listen to client connections
	 * @throws IOException 
	 */
	public static void listen(int port, SSLConfig sslResource) {
		try {
			proxySocket = new ServerSocket(port);
			boolean running = true;
			while(running) {
				try {
					Socket clientSocket = proxySocket.accept();
					Thread t = new Thread(new ConnectionHandler(clientSocket, sslResource, new LogFileExpositionStrategy()));
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
