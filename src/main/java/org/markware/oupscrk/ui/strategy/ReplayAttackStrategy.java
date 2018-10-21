package org.markware.oupscrk.ui.strategy;

import org.markware.oupscrk.http.HttpRequest;

/**
 * Replay requests containing seed
 * @author citestra
 *
 */
public interface ReplayAttackStrategy {

	/*
	 * Client sends: 'startReplayAttack + payload "seed (random: single or multiple?) + wordlist + port to send data to" 
	 * Proxy detects seed in request save it and replay it and sends data back to client
	 * 
	 */
	public void replay(HttpRequest HttpRequestCandidate);
	
	public boolean isEngaged();
}
