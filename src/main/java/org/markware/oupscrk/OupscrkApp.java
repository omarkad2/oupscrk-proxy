package org.markware.oupscrk;

import java.io.IOException;

import org.markware.oupscrk.ui.CommandCenter;

/**
 * Main program
 * 
 * @author citestra
 *
 */
public class OupscrkApp {

	/**
	 * Main method for Proxy UI
	 * 
	 * @param args [0] port (optional)
	 */
	public static void main(String[] args) {
		int ccPort = 10001;
		int proxyPort = 9999;
		if (args.length > 1) {
			try {
				ccPort = Integer.parseInt(args[0]);
				proxyPort = Integer.parseInt(args[1]);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Control center on port : " + ccPort);

		try {
			CommandCenter controlCenter = new CommandCenter(ccPort, proxyPort);
			controlCenter.start();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Control center stopped listening on port : " + ccPort);
		}

	}

}
