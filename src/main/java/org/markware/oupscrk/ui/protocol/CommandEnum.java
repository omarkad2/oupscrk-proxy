package org.markware.oupscrk.ui.protocol;

public enum CommandEnum {

	START_PROXY("startProxy"),

	STOP_PROXY("stopProxy"),

	CHECK_PROXY("checkProxy"),

	START_EXPOSE("startExpose"),

	STOP_EXPOSE("stopExpose"),

	START_TAMPER_REQUEST("startTamperRequest"),

	STOP_TAMPER_REQUEST("stopTamperRequest"),
	
	START_TAMPER_RESPONSE("startTamperResponse"),

	STOP_TAMPER_RESPONSE("stopTamperResponse"),
	
	START_REPLAY_ATTACK("startReplayAttack"),
	
	STOP_REPLAY_ATTACK("stopReplayAttack");

	/**
	 * command value
	 */
	private String value;

	/**
	 * Constructor
	 * @param value
	 */
	CommandEnum(String value) {
		this.value = value;
	}

	/**
	 * @return command value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * @param command
	 * @return Enum value by its attribute
	 */
	public static CommandEnum getByValue(String command) {
		for (CommandEnum c : values()) {
			if (c.value.equals(command)) {
				return c;
			}
		}
		throw new IllegalArgumentException(command);
	}
}
