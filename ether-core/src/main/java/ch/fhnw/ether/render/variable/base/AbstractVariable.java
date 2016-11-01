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

package ch.fhnw.ether.render.variable.base;

import ch.fhnw.ether.render.gl.Program;
import ch.fhnw.ether.render.variable.IShaderVariable;
import ch.fhnw.ether.scene.attribute.ITypedAttribute;
import ch.fhnw.util.Log;

public abstract class AbstractVariable<T> implements IShaderVariable<T> {
	private static final Log LOG = Log.create();
	
	private final String id;
	private final String shaderName;
	private int shaderIndex = -1;

	protected AbstractVariable(ITypedAttribute<T> attribute, String shaderName) {
		this(attribute.id(), shaderName);
	}

	protected AbstractVariable(String id, String shaderName) {
		this.id = id;
		this.shaderName = shaderName;
	}

	@Override
	public final String id() {
		return id;
	}

	protected final String getShaderName() {
		return shaderName;
	}

	protected final int getShaderIndex(Program program) {
		if (shaderIndex == -1) {
			shaderIndex = resolveShaderIndex(program, shaderName);
			if (shaderIndex == -1) {
				LOG.warning("shader variable " + id + ": cannot resolve glsl name " + shaderName);
				shaderIndex = -2;
			}
		}
		return shaderIndex;
	}

	protected abstract int resolveShaderIndex(Program program, String shaderName);
	
	@Override
	public String toString() {
		return id + "(" + shaderName + ")";
	}
}
