package ch.fhnw.ether.media;

import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.IObjectID;
import ch.fhnw.util.math.MathUtilities;

public class Parametrizable  implements IObjectID {
	private final long id = ClassUtilities.createObjectID();

	protected final Parameter[]    parameters;
	private   final String         groupLabel;
	
	public Parametrizable(Parameter ... parameters) {
		this(null, parameters);
	}
	
	public Parametrizable(String groupLabel, Parameter ... parameters) {
		this.groupLabel  = groupLabel;
		this.parameters  = new Parameter[parameters.length];
		for(int i = 0; i < parameters.length; i++) {
			parameters[i].setIdx(i);
			this.parameters[i] = parameters[i].copy();
		}
	}

	public Parameter getParameter(String name) {
		for(Parameter p : parameters)
			if(p.getName().equals(name))
				return p;
		return null;
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public String getName(Parameter p) {
		return parameters[p.getIdx()].getName();
	}

	public String getDescription(Parameter p) {
		return parameters[p.getIdx()].getDescription();
	}

	public float getMin(Parameter p) {
		return parameters[p.getIdx()].getMin();
	}

	public float getMax(Parameter p) {
		return parameters[p.getIdx()].getMax();
	}

	public float getVal(Parameter p) {
		return parameters[p.getIdx()].getVal();
	}

	public void setVal(Parameter p, float val) {
		parameters[p.getIdx()].setVal(val);
	}

	public void incVal(Parameter p, int steps) {
		float range = (getMax(p) - getMin(p));
		incVal(p, steps, range / (range < 2 ? 100 : 500.0f));
	}

	public void incVal(Parameter p, int steps, float stepSize) {
		setVal(p, MathUtilities.clamp(
				getVal(p) + steps * stepSize, 
				getMin(p), 
				getMax(p)));
	}

	public float getMin(String p) {
		return parameters[getParameter(p).getIdx()].getMin();
	}

	public float getMax(String p) {
		return parameters[getParameter(p).getIdx()].getMax();
	}

	public float getVal(String p) {
		return parameters[getParameter(p).getIdx()].getVal();
	}

	public void setVal(String p, float val) {
		parameters[getParameter(p).getIdx()].setVal(val);
	}
	
	@Override
	public final long getObjectID() {
		return id;
	}

	@Override
	public final int hashCode() {
		return (int) id;
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof Parametrizable && ((Parametrizable)obj).id == id;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("[ ");
		for(Parameter p : parameters) {
			result.append(p.getName()).append('=');
			result.append(getVal(p));
			result.append(' ');
		}
		result.append(']');
		return result.toString();
	}

	public String getGroupLabel() {
		return groupLabel == null ? toString() : groupLabel;
	}
}
