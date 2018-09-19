package org.markware.oupscrk;

import java.io.IOException;

public class OupscrkUI {

	/**
	 * CA resources
	 */
	private final static String CA_FOLDER = "/CA/";
	
	public static void main(String[] args) {
		int port = 10001;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("Control center on port : " + port);
		
		try {
			ControlCenter controlCenter = new ControlCenter(port, 9999, CA_FOLDER);
			controlCenter.start();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Control center stopped listening on port : " + port);
		}

		
	}

}
