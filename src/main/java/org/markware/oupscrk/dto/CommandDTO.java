package org.markware.oupscrk.dto;

import java.util.List;

public class CommandDTO {

	private String command;
	
	private List<String> args;

	public CommandDTO(String command, List<String> args) {
		this.command = command;
		this.args = args;
	}
	
	public String getCommand() {
		return command;
	}

	public List<String> getArgs() {
		return args;
	}
	
	
}
