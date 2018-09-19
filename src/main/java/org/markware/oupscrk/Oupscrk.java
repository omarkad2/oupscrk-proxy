package org.markware.oupscrk;

import java.io.IOException;

/**
 * Main class
 * @author citestra
 *
 */
public class Oupscrk {

	/**
	 * CA resources
	 */
	private final static String CA_FOLDER = "/CA/";

	/**
	 * Main method
	 * @param args [0] port (optional)
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		int port = 9999;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("Proxy listening on port : " + port);
		
		try {
			ProxyServer proxyServer = new ProxyServer(port, new SSLConfig(CA_FOLDER));
			proxyServer.setProxyOn(true);
			proxyServer.listen();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Proxy stopped listening on port : " + port);
		}
		
	}

}
