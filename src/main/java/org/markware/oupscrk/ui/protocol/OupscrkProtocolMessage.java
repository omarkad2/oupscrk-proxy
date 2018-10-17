package org.markware.oupscrk.ui.protocol;

/**
 * Oupscrk Protocol
 * @author citestra
 *
 */
public class OupscrkProtocolMessage {

	/**
	 * Command
	 */
	private CommandEnum command;
	
	/**
	 * Payload
	 */
	private String payload;
	
	/**
	 * Constructor
	 * @param command
	 * @param payload
	 */
	public OupscrkProtocolMessage(
			String command, 
			String payload) {
		this.command = CommandEnum.getByValue(command);
		this.payload = payload;
	}

	////////////////////////////////////GETTERS/SETTERS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	public CommandEnum getCommand() {
		return command;
	}

	public void setCommand(CommandEnum command) {
		this.command = command;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
}
