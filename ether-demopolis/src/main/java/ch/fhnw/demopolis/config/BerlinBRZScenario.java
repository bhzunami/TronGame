/*
 * Copyright (c) 2015 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2015 - 2016 FHNW & ETH Zurich
 * All rights reserved.
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

package ch.fhnw.demopolis.config;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.demopolis.model.entities.IDesignEntity;

public class BerlinBRZScenario extends BerlinScenario {
	
	private static final String BRZ_PATH ="brz_path.obj";
	private static final String BRZ_ROAD ="brz_road.obj";
	private static final String BRZ_GREEN = "brz_ground.obj";
	private static final String BRZ_LAKE_BORDER = "brz_lake_border.obj";
	private static final String BRZ_WATER = "brz_lake.obj";
	private static final String BRZ_BLOCKS = "brz_blocks_static.obj";
	private static final String BRZ_STATIC_BUILDINGS = "brz_buildings_static.obj";
	private static final String BRZ_BUILDING_LOD200_LO = "brz_building_lod200_lo.obj";
	private static final String BRZ_BUILDING_LOD200_HI = "brz_building_lod200_hi.obj";
	private static final String BRZ_BUILDING_LOD300 =  "brz_building_lod300.obj";
	
	private final String modelPath;
	
	public BerlinBRZScenario(boolean alexanderplatzOnly, String modelPath) {
		super(alexanderplatzOnly);
		this.modelPath = modelPath;
	}
	
	@Override
	public String getName() {
		return alexanderplatzOnly ? "BRZ Alexanderplatz Only" : "BRZ Alexanderplatz and Rathausforum";
	}
	
	@Override
	public String[] getStaticGround() {
		return new String[] { STATIC_GROUND, path(BRZ_LAKE_BORDER), path(BRZ_ROAD) };
	}

	@Override
	public String[] getStaticWater() {
		return new String[] { STATIC_WATER, path(BRZ_WATER) };
	}

	@Override
	public String[] getStaticBlocks() {
		List<String> b = new ArrayList<>();
		b.add(path(BRZ_BLOCKS));
		b.add(path(BRZ_PATH));
		for (IDesignEntity e : BLOCKS_NOP)
			b.add(e.getAsset());
		if (alexanderplatzOnly) {
			for (IDesignEntity e : BLOCKS_RF)
				b.add(e.getAsset());
		}
		return b.toArray(new String[0]);
	}

	@Override
	public String[] getStaticGreen() {
		return new String[] { STATIC_GREEN, path(BRZ_GREEN) };
	}

	@Override
	public String[] getStaticBuildings() {
		List<String> b = new ArrayList<>();
		b.add(path(BRZ_STATIC_BUILDINGS));
		b.add(path(BRZ_BUILDING_LOD200_LO));
		b.add(path(BRZ_BUILDING_LOD200_HI));
		b.add(path(BRZ_BUILDING_LOD300));
		for (IDesignEntity e : BUILDINGS_NOP)
			b.add(e.getAsset());
		if (alexanderplatzOnly) {
			for (IDesignEntity e : BUILDINGS_RF)
				b.add(e.getAsset());
		}
		return b.toArray(new String[0]);
	}
	
	private String path(String asset) {
		return modelPath + "/" + asset;
	}
}
