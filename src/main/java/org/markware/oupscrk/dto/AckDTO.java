package org.markware.oupscrk.dto;

public class AckDTO {

	private boolean success;
	
	private String message;
	
	public AckDTO(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
	
	
}
