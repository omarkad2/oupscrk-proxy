package org.markware.oupscrk;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Interceptor {

	private final static String CA_FOLDER = "CA/";
	
	private boolean running = false;
	
	private ServerSocket proxySocket;
	
	private String caKey;
	
	private String caCert;
	
	private String certKey;
	
	private Path certsFolder;
	
	
	public static void main(String[] args) {
		int port = 9999;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		
		System.out.println("Proxy listening on port : " + port);
		
		Interceptor interceptor = new Interceptor(port);
		interceptor.listen();
		
		System.out.println("Proxy stopped listening on port : " + port);
	}
	
	/**
	 * Constructor
	 * @param port
	 */
	public Interceptor(int port) {
		File file;
		try {
			this.proxySocket = new ServerSocket(port);
			this.running = true;
			ClassLoader classLoader = getClass().getClassLoader();
			/* CA KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "ca.key").getFile());
			this.caKey = new String(Files.readAllBytes(file.toPath()));
			/* CA CERT */
			file = new File(classLoader.getResource(CA_FOLDER + "ca.cert").getFile());
			this.caCert = new String(Files.readAllBytes(file.toPath()));
			/* CERT KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "cert.key").getFile());
			this.certKey = new String(Files.readAllBytes(file.toPath()));
			/* CERTS FOLDER */
			this.certsFolder = Files.createTempDirectory(Paths.get(""), "certs");
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
				Thread t = new Thread(new ConnectionHandler(clientSocket, this.caKey, this.caCert, this.certKey, this.certsFolder));
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
