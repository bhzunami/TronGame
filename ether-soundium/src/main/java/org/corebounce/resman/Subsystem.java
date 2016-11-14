package org.corebounce.resman;

import java.util.HashMap;
import java.util.Map;

import ch.fhnw.util.TextUtilities;

public abstract class Subsystem {
	protected final Map<String, String> configuration = new HashMap<>();
	
	public Subsystem(String cfgPrefix, String ... args) {
		for(String arg : args) {
			if(arg.startsWith(cfgPrefix + ":")) {
				String[] cfg = TextUtilities.split(arg.substring(cfgPrefix.length() + 1), ',');
				for(int i = 0; i < cfg.length; i++) {
					int split = cfg[i].indexOf('=');
					if(split <= 0)	configuration.put(cfg[i], cfg[i]);
					else			configuration.put(cfg[i].substring(0, split), cfg[i].substring(split+1));
				}
			}
		}
	}		
}
