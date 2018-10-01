package org.markware.oupscrk;

import java.io.IOException;

/**
 * Main program
 * @author citestra
 *
 */
public class OupscrkUI {

	/**
	 * CA resources
	 */
	private final static String CA_FOLDER = "/CA/";
	
	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		int ccPort = 10001;
		int proxyPort = 9999;
		if (args.length > 1) {
			try {
				ccPort = Integer.parseInt(args[0]);
				proxyPort = Integer.parseInt(args[1]);
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Control center on port : " + ccPort);
		
		try {
			ControlCenter controlCenter = new ControlCenter(ccPort, proxyPort, CA_FOLDER);
			controlCenter.start();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Control center stopped listening on port : " + ccPort);
		}

		
	}

}
