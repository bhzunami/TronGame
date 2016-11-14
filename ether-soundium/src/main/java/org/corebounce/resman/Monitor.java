package org.corebounce.resman;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.platform.IMonitor;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.ClassUtilities;

public class Monitor extends Subsystem {
	public Monitor(String[] args) {
		super(CFG_PREFIX, args);
	}

	public static String   CFG_PREFIX = "mon";
	public static String[] CFG_OPTIONS = ClassUtilities.EMPTY_StringA;

	public IMonitor getSoundiumMonitor() {
		IMonitor result = Platform.get().getMonitors()[0];
		try {result = Platform.get().getMonitors()[Integer.parseInt(configuration.get("sndm"))];} catch(Throwable t) {}
		return result;
	}

	public IMonitor[] getEngineMonitors() {
		List<IMonitor> result = new ArrayList<>();
		for(int cam = 0; ; cam++) {
			try{
				result.add(Platform.get().getMonitors()[Integer.parseInt(configuration.get("cam"+cam))]);
			}catch(Throwable t){
				break;
			}
		}
		if(result.isEmpty())
			result.add(Platform.get().getMonitors()[0]);
		return result.toArray(new IMonitor[result.size()]);
	}
	
	static {
		int monCount = 0;
		for(IMonitor mon : Platform.get().getMonitors())
			CFG_OPTIONS = ArrayUtilities.cat(CFG_OPTIONS, new String[] {"sndm="+monCount++, mon.toString()});
		StringBuilder cams = new StringBuilder("cam<n>=");
		for(int mon = 0; mon < monCount; mon++)
			cams.append(mon == 0 ? "" : "|").append(Integer.toString(mon));
		CFG_OPTIONS = ArrayUtilities.append(CFG_OPTIONS, cams.toString());		
		cams.setLength(0);
		cams.append("Assign engine camera <n> to monitor <m>. Monitors:");
		monCount = 0;
		for(IMonitor mon : Platform.get().getMonitors())
			cams.append(" " + monCount++ + ":'" + mon.toString()+"'");
		CFG_OPTIONS = ArrayUtilities.append(CFG_OPTIONS, cams.toString());		
	}
}
