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

import java.util.function.Consumer;
import java.util.function.Supplier;

import ch.fhnw.ether.render.gl.Program;
import ch.fhnw.ether.render.variable.IShaderUniform;

public final class StateInject extends AbstractVariable<Object> implements IShaderUniform<Object> {
	private final Consumer<Program> enable;
	private final Consumer<Program> disable;

	public StateInject(String id, Consumer<Program> enable) {
		this(id, enable, null);
	}

	public StateInject(String id, Consumer<Program> enable, Consumer<Program> disable) {
		super(id, null);
		this.enable = enable;
		this.disable = disable;
	}

	@Override
	public boolean isLinked() {
		return true;
	}

	@Override
	public void setSupplier(Supplier<?> supplier) {
	}
	
	@Override
	public void setIndex(int index) {
	}
	
	@Override
	public void update(Object[] data) {
	}

	@Override
	public void enable(Program program) {
		if (enable != null)
			enable.accept(program);
	}

	@Override
	public void disable(Program program) {
		if (disable != null)
			disable.accept(program);
	}

	@Override
	protected int resolveShaderIndex(Program program, String shaderName) {
		return -1;
	}

	@Override
	public String toString() {
		return super.toString() + "[" + enable + ", " + disable + "]";
	}
}
