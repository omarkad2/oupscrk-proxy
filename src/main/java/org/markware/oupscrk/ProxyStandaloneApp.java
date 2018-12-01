package org.markware.oupscrk;

import java.io.IOException;

import org.markware.oupscrk.config.SSLConfig;
import org.markware.oupscrk.proxy.ProxyServer;

/**
 * Main class
 * @author citestra
 *
 */
public class ProxyStandaloneApp {

	/**
	 * Main method
	 * @param args [0] port (optional) by default 9999
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		int port = 9999;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("Proxy listening on port : " + port);
		
		try {
			ProxyServer proxyServer = new ProxyServer(port, new SSLConfig());
			proxyServer.setProxyOn(true);
			proxyServer.listen();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Proxy stopped listening on port : " + port);
		}
		
	}

}
