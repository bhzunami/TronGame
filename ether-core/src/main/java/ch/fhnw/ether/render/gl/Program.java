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

package ch.fhnw.ether.render.gl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.shader.base.AbstractShader;
import ch.fhnw.util.math.IVec2;
import ch.fhnw.util.math.IVec3;
import ch.fhnw.util.math.IVec4;

/**
 * GLSL shader program abstraction.
 *
 * @author radar
 */
// NOTE: currently we do not plan to dispose programs
public final class Program {
	public enum ShaderType {
		//@formatter:off
		VERTEX(GL20.GL_VERTEX_SHADER),
		TESS_CONTROL(GL40.GL_TESS_CONTROL_SHADER),
		TESS_EVAL(GL40.GL_TESS_EVALUATION_SHADER),
		GEOMETRY(GL32.GL_GEOMETRY_SHADER),
		FRAGMENT(GL20.GL_FRAGMENT_SHADER);
		//@formatter:on

		ShaderType(int glType) {
			this.glType = glType;
		}

		int getGLType() {
			return glType;
		}

		private final int glType;
	}

	private final static Class<?> LIBRARY_BASE = IShader.class;
	private final static String LIBRARY_PATH = "/shaders/lib";

	private static final class Shader {
		static final Map<String, Shader> SHADERS = new HashMap<>();

		final Class<?> root;
		final String path;
		int shaderObject;

		Shader(Class<?> root, String path, ShaderType type, PrintStream out) throws IOException {
			this.root = root;
			this.path = path;

			StringBuilder code = new StringBuilder();
			if(path.startsWith(AbstractShader.INLINE))
				code.append(path);
			else {
				URL url = root.getResource(path);
				if (url == null) {
					out.println("file not found: " + this);
					throw new FileNotFoundException("file not found: " + this);
				}
				new GLSLReader(LIBRARY_BASE, LIBRARY_PATH, url, code, out);
			}

			// FIXME: use GLObject here for auto disposal in case not used anymore
			shaderObject = GL20.glCreateShader(type.glType);

			GL20.glShaderSource(shaderObject, code.toString());
			GL20.glCompileShader(shaderObject);

			if (!checkStatus(shaderObject, GL20.GL_COMPILE_STATUS, out)) {
				out.println("failed to compile shader: " + this);
				throw new IllegalArgumentException("failed to compile shader: " + this);
			}
		}

		@Override
		public String toString() {
			return root.getSimpleName() + ":" + path;
		}

		static Shader create(Class<?> root, String path, ShaderType type, PrintStream out) throws IOException {
			String key = key(root, path);
			Shader shader = SHADERS.get(key);
			if (shader == null) {
				shader = new Shader(root, path, type, out);
				SHADERS.put(key, shader);
			}
			return shader;
		}

		static String key(Class<?> root, String path) {
			return root.hashCode() + "/" + path;
		}
	}

	private static final Map<String, Program> PROGRAMS = new HashMap<>();

	private final String id;
	private final GLObject programObject;

	private Program(PrintStream out, Shader... shaders) {
		programObject = new GLObject(Type.PROGRAM);

		String id = "";
		for (Shader shader : shaders) {
			if (shader != null) {
				GL20.glAttachShader(programObject.getId(), shader.shaderObject);
				id += shader.path + " ";
			}
		}
		this.id = id;

		GL20.glLinkProgram(programObject.getId());
		if (!checkStatus(programObject.getId(), GL20.GL_LINK_STATUS, out)) {
			out.println("failed to link program: " + this);
			throw new IllegalArgumentException("failed to link program: " + this);
		}

		GL20.glValidateProgram(programObject.getId());
		if (!checkStatus(programObject.getId(), GL20.GL_VALIDATE_STATUS, out)) {
			out.println("failed to validate program: " + this);
			throw new IllegalArgumentException("failed to validate program: " + this);
		}
	}

	public void enable() {
		GL20.glUseProgram(programObject.getId());
	}

	public void disable() {
		GL20.glUseProgram(0);
	}

	public void setUniform(int index, boolean value) {
		if(index >= 0)
			GL20.glUniform1i(index, value ? 1 : 0);
	}

	public void setUniform(int index, int value) {
		if (index >= 0)
			GL20.glUniform1i(index, value);
	}

	public void setUniform(int index, float value) {
		if (index >= 0)
			GL20.glUniform1f(index, value);
	}

	public void setUniformVec2(int index, IVec2 value) {
		if (value != null && index >= 0)
			GL20.glUniform2f(index, value.x(), value.y());
	}

	public void setUniformVec2(int index, float x, float y) {
		if (index >= 0)
			GL20.glUniform2f(index, x, y);
	}

	public void setUniformVec2(int index, float[] value) {
		if (value != null && index >= 0)
			GL20.glUniform2fv(index, value);
	}

	public void setUniformVec3(int index, IVec3 value) {
		if (value != null && index >= 0)
			GL20.glUniform3f(index, value.x(), value.y(), value.z());
	}

	public void setUniformVec3(int index, float x, float y, float z) {
		if (index >= 0)
			GL20.glUniform3f(index, x, y, z);
	}

	public void setUniformVec3(int index, float[] value) {
		if (value != null && index >= 0)
			GL20.glUniform3fv(index, value);
	}

	public void setUniformVec4(int index, IVec4 value) {
		if (value != null && index >= 0)
			GL20.glUniform4f(index, value.x(), value.y(), value.z(), value.w());
	}

	public void setUniformVec4(int index, float x, float y, float z, float w) {
		if (index >= 0)
			GL20.glUniform4f(index, x, y, z, w);
	}

	public void setUniformVec4(int index, float[] value) {
		if (value != null && index >= 0)
			GL20.glUniform4fv(index, value);
	}

	public void setUniformMat3(int index, float[] value) {
		if (value != null && index >= 0)
			GL20.glUniformMatrix3fv(index, false, value);
	}

	public void setUniformMat4(int index, float[] value) {
		if (value != null && index >= 0)
			GL20.glUniformMatrix4fv(index, false, value);
	}

	public void setUniformSampler(int index, int unit) {
		if(index >= 0)
			setUniform(index, unit);
	}

	public int getAttributeLocation(String name) {
		return GL20.glGetAttribLocation(programObject.getId(), name);
	}

	public int getUniformLocation(String name) {
		return GL20.glGetUniformLocation(programObject.getId(), name);
	}

	public int getUniformBlockIndex(String name) {
		return GL31.glGetUniformBlockIndex(programObject.getId(), name);
	}

	public void bindUniformBlock(int index, int bindingPoint) {
		GL31.glUniformBlockBinding(programObject.getId(), index, bindingPoint);
	}

	@Override
	public String toString() {
		return id;
	}

	public static Program create(Class<?> root, String vertShader, String fragShader, String geomShader, PrintStream out) throws IOException {
		String key = key(root, vertShader, fragShader, geomShader);
		Program program = PROGRAMS.get(key);
		if (program == null) {
			Shader vert = Shader.create(root, vertShader, ShaderType.VERTEX, out);
			Shader frag = Shader.create(root, fragShader, ShaderType.FRAGMENT, out);
			Shader geom = null;
			if (geomShader != null && root.getResource(geomShader) != null) {
				geom = Shader.create(root, geomShader, ShaderType.GEOMETRY, out);
			}
			program = new Program(out, vert, frag, geom);
			PROGRAMS.put(key, program);
		}
		return program;
	}

	private static String key(Class<?> root, String... paths) {
		String key = "" + root.hashCode();
		for (String path : paths) {
			if (path != null)
				key += ":" + path;
		}
		return key;
	}

	private static boolean checkStatus(int object, int statusType, PrintStream out) {
		int status = 0;
		if (statusType == GL20.GL_COMPILE_STATUS) {
			status = GL20.glGetShaderi(object, statusType);
			if (status != 1) {
				status = GL20.glGetShaderi(object, GL20.GL_INFO_LOG_LENGTH);
				if (status > 0) {
					out.println(GL20.glGetShaderInfoLog(object));
				} else {
					out.println("unknown compile status");
				}
				return false;
			}
		} else if (statusType == GL20.GL_LINK_STATUS || statusType == GL20.GL_VALIDATE_STATUS) {
			status = GL20.glGetProgrami(object, statusType);
			if (status != 1) {
				status = GL20.glGetProgrami(object, GL20.GL_INFO_LOG_LENGTH);
				if (status > 0) {
					out.println(GL20.glGetProgramInfoLog(object));
				} else {
					out.println("unknown link / validate status");
				}
				return false;
			}
		}
		return true;
	}
}
