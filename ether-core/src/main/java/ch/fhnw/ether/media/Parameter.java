/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.fhnw.ether.media;

import java.util.Arrays;

import ch.fhnw.ether.scene.mesh.material.IMaterial.IMaterialAttribute;
import ch.fhnw.util.EnumUtilities;
import ch.fhnw.util.TextUtilities;

public class Parameter implements IMaterialAttribute<Float> {
	public enum Type {RANGE, ITEMS, VALUES, BOOL, BITMAP}

	private static final String   ZERO     = "0";
	private static final String   ONE      = "1";
	private static final String[] BOOL     = {ZERO, ONE};
	public static final  String[] BITMAP8  = {};
	public static final  String[] BITMAP16 = {};
	public static final  String[] BITMAP24 = {};
	public static final  String[] BITMAP32 = {};

	private final String   name;
	private final String   description;
	private final float    min;
	private final float    max;
	private final String[] items;
	private final float[]  values;
	private       float    val;
	private       int      idx = -1;

	public Parameter(String name, String description, float min, float max, float val) {
		this(name, description, min, max, val, null, null);
	}

	public Parameter(String name, String description, int val, String ... items) {
		this(name, description, 0, items.length, val, null, items);
	}

	public Parameter(String name, String description, int index, float[] values, String ... labels) {
		this(name, description, 0, labels.length, index, values, labels);
	}

	public Parameter(String name, String description, boolean value) {
		this(name, description, 0, 1, value ? 1 : 0, null, BOOL);
	}

	public  <E extends Enum<E>> Parameter(String name, String description, int ordinal, Class<E> enumCls) {
		this(name, description, 0, EnumUtilities.toStringArrayName(enumCls).length, ordinal, null, EnumUtilities.toStringArrayName(enumCls));
	}

	public Parameter(String name, String description, float min, float max, float val, float[] values, String[] items) {
		if(!name.equals(TextUtilities.cleanForId(name)))
			throw new IllegalArgumentException("Illegal (non-id) characters in name '" + name + "'");
		this.name        = name;
		this.description = description;
		this.min         = min;
		if     (items == BITMAP8)  this.max = 0xFF;
		else if(items == BITMAP16) this.max = 0xFFFF;
		else if(items == BITMAP24) this.max = 0xFFFFFF;
		else if(items == BITMAP32) this.max = 0xFFFFFFFF;
		else                       this.max = max;
		this.val         = val;
		this.values      = values;
		this.items       = items;
	}

	private Parameter(Parameter p) {
		this(p.name, p.description, p.min, p.max, p.val, p.values, p.items);
		this.idx = p.idx;
	}

	@Override
	final public String toString() {
		return name;
	}

	final public String getName() {
		return name;
	}

	final public String getDescription() {
		return description;
	}

	final public float getMin() {
		return min;
	}

	final public float getMax() {
		return max;
	}

	final float getVal() {
		return val;
	}

	final void setVal(float val) {
		this.val = val;
	}

	public int getValuesIndexFor(float val) {
		int   result = 0;
		float d      = Float.MAX_VALUE;
		for(int i = values.length; --i >= 0;) {
			if(Math.abs(values[i]-val) < d) {
				d      = Math.abs(values[i]-val);
				result = i;
			}
		}
		return result;
	}

	final void setIdx(int idx) {
		if(this.idx == idx) return;
		if(this.idx != -1)
			throw new IllegalArgumentException(name + ": parameter already in use");
		this.idx = idx;
	}

	final int getIdx() {
		return idx;
	}

	protected Parameter copy() {
		return new Parameter(this);
	}

	public Type getType() {
		if(items == null)
			return Type.RANGE;
		else if(items == BOOL)
			return Type.BOOL;
		else if(items == BITMAP8 || items == BITMAP16 || items == BITMAP24 || items == BITMAP32)
			return Type.BITMAP;
		else if(values != null)
			return Type.VALUES;
		else
			return Type.ITEMS; 
	}

	public String[] getItems() {
		return items;
	}

	public float[] getValues() {
		return values;
	}

	@Override
	public String id() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Parameter)) return false;
		Parameter p = (Parameter)obj;
		return name.equals(p.name) &&
				description.equals(p.description) &&
				min == p.min &&
				max == p.max && 
				Arrays.equals(values, p.values) &&
				Arrays.equals(items, p.items);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
