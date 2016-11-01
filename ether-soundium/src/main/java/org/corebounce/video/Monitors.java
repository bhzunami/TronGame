package org.corebounce.video;

import java.util.ArrayList;
import java.util.List;

import org.corebounce.soundium.Subsystem;

import ch.fhnw.ether.platform.IMonitor;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.ClassUtilities;

public class Monitors extends Subsystem {
	public Monitors(String[] args) {
		super(CFG_PREFIX, args);
	}
	
	public IMonitor getSoundiumMonitor() {
		IMonitor result = MONITORS[0];
		try {result = MONITORS[Integer.parseInt(configuration.get("sndm"))];} catch(Throwable t) {}
		return result;
	}

	public IMonitor[] getEngineMonitors() {
		List<IMonitor> result = new ArrayList<>();
		for(int cam = 0; ; cam++) {
			try{
				result.add(MONITORS[Integer.parseInt(configuration.get("cam"+cam))]);
			}catch(Throwable t){
				break;
			}
		}
		if(result.isEmpty())
			result.add(MONITORS[0]);
		return result.toArray(new IMonitor[result.size()]);
	}
	
	private static final IMonitor[] MONITORS = Platform.get().getMonitors();
	public static final String      CFG_PREFIX = "mon";
	public static       String[]    CFG_OPTIONS = ClassUtilities.EMPTY_StringA;

	static {
		int monCount = 0;
		for(IMonitor mon : MONITORS)
			CFG_OPTIONS = ArrayUtilities.cat(CFG_OPTIONS, new String[] {"sndm="+monCount++, mon.toString()});
		StringBuilder cams = new StringBuilder("cam<n>=");
		for(int mon = 0; mon < monCount; mon++)
			cams.append(mon == 0 ? "" : "|").append(Integer.toString(mon));
		CFG_OPTIONS = ArrayUtilities.append(CFG_OPTIONS, cams.toString());		
		cams.setLength(0);
		cams.append("Assign engine camera <n> to monitor <m>. Monitors:");
		monCount = 0;
		for(IMonitor mon : MONITORS)
			cams.append(" " + monCount++ + ":'" + mon.toString()+"'");
		CFG_OPTIONS = ArrayUtilities.append(CFG_OPTIONS, cams.toString());		
	}
	
	
}
