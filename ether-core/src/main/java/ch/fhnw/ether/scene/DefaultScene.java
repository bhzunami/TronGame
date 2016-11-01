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

package ch.fhnw.ether.scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.render.IRenderManager;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.mesh.IMesh;

public class DefaultScene implements IScene {

	private final IController controller;

	private final List<I3DObject> objects = new ArrayList<>();

	public DefaultScene(IController controller) {
		this.controller = controller;
	}

	@Override
	public final void add3DObject(I3DObject object) {
		if (objects.contains(object))
			throw new IllegalArgumentException("object already in scene: " + object);

		IRenderManager rm = controller.getRenderManager();
		if (object instanceof ILight)
			rm.addLight((ILight) object);
		else if (object instanceof IMesh)
			rm.addMesh((IMesh) object);
		objects.add(object);
	}

	@Override
	public void add3DObjects(I3DObject... objects) {
		for (I3DObject object : objects)
			add3DObject(object);
	}
	
	@Override
	public void add3DObjects(Collection<? extends I3DObject> objects) {
		for (I3DObject object : objects)
			add3DObject(object);
	}

	@Override
	public final void remove3DObject(I3DObject object) {
		if (!objects.contains(object))
			throw new IllegalArgumentException("object not in scene: " + object);

		IRenderManager rm = controller.getRenderManager();
		if (object instanceof ILight)
			rm.removeLight((ILight) object);
		else if (object instanceof IMesh)
			rm.removeMesh((IMesh) object);
		objects.remove(object);
	}

	@Override
	public void remove3DObjects(I3DObject... objects) {
		for (I3DObject object : objects)
			remove3DObject(object);
	}
	
	@Override
	public void remove3DObjects(Collection<? extends I3DObject> objects) {
		for (I3DObject object : objects)
			remove3DObject(object);
	}

	@Override
	public final List<I3DObject> get3DObjects() {
		return Collections.unmodifiableList(objects);
	}

	@Override
	public final List<ICamera> getCameras() {
		return objects.stream().filter(p -> p instanceof ICamera).map(p -> (ICamera) p).collect(Collectors.toList());		
	}

	@Override
	public final List<ILight> getLights() {
		return objects.stream().filter(p -> p instanceof ILight).map(p -> (ILight) p).collect(Collectors.toList());		
	}

	@Override
	public final List<IMesh> getMeshes() {
		return objects.stream().filter(p -> p instanceof IMesh).map(p -> (IMesh) p).collect(Collectors.toList());		
	}

	protected final IController getController() {
		return controller;
	}
}
