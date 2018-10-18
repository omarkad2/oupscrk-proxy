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
	private OupscrkProtocolAck(
			boolean success, 
			String payload) {
		this.success = success;
		this.payload = payload;
	}

	/**
	 * @param payload
	 * @return Success Ack
	 */
	public static OupscrkProtocolAck successAck(String payload) {
		return new OupscrkProtocolAck(true, payload);
	}
	
	/**
	 * @param payload
	 * @return Fail Ack
	 */
	public static OupscrkProtocolAck failAck(String payload) {
		return new OupscrkProtocolAck(false, payload);
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
