package org.markware.oupscrk;

/**
 * Main class
 * @author citestra
 *
 */
public class Oupscrk {

	/**
	 * CA resources
	 */
	private final static String CA_FOLDER = "CA/";

	/**
	 * Main method
	 * @param args [0] port (optional)
	 */
	public static void main(String[] args) {
		int port = 9999;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("Proxy listening on port : " + port);

		ProxyServer.listen(port, new SSLConfig(CA_FOLDER));

		System.out.println("Proxy stopped listening on port : " + port);
	}

}
