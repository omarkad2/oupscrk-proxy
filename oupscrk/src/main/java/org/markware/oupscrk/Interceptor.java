package org.markware.oupscrk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Interceptor {

	private boolean running = false;
	
	private ServerSocket proxySocket;
	
	
	public static void main(String[] args) {
		int port = 9999;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		
		Interceptor interceptor = new Interceptor(port);
		interceptor.listen();
		
	}
	
	/**
	 * Constructor
	 * @param port
	 */
	public Interceptor(int port) {
		try {
			this.proxySocket = new ServerSocket(port);
			this.running = true;
		} catch (IOException e) {
			System.out.println("Couldn't create proxy socket");
			e.printStackTrace();
		}
	}
	
	/**
	 * Listen to client connections
	 */
	public void listen() {
		while(this.running) {
			try {
				Socket clientSocket = this.proxySocket.accept();
				Thread t = new Thread(new ConnectionHandler(clientSocket));
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
