package org.markware.oupscrk.ui.protocol;

/**
 * Oupscrk protocol acknowledgement 
 * @author citestra
 *
 */
public class OupscrkProtocolAck {

	/**
	 * Success ?
	 */
	private boolean success;
	
	/**
	 * response JSON
	 */
	private String payload; 
	
	/**
	 * Constructor
	 * @param success
	 * @param payload
	 */
	public OupscrkProtocolAck(
			boolean success, 
			String payload) {
		this.success = success;
		this.payload = payload;
	}

	//////////////////////////////////// GETTERS/SETTERS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
}
