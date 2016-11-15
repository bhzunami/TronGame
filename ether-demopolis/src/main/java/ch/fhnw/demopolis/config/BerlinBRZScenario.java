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
import java.util.Arrays;
import java.util.List;

import ch.fhnw.demopolis.model.entities.IDesignEntity;
import ch.fhnw.util.math.Vec3;

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
	
	@Override
	public List<Vec3> getIntroCameraVertices() {
		return Arrays.asList(INTRO_BRZ);
	}
	
	@Override
	public List<Vec3> getLoopCameraVertices() {
		return Arrays.asList(LOOP_BRZ);
	}

	private String path(String asset) {
		return modelPath + "/" + asset;
	}
	
	
	private static final Vec3[] INTRO_BRZ = new Vec3[] {
		// intro
		new Vec3(-1200.0, -1200.0, 200.0),
		new Vec3(-1040.0, -1040.0, 200.0),
		new Vec3(-880.0, -880.0, 200.0),
		new Vec3(-720.0, -720.0, 200.0),
		new Vec3(-560.0, -560.0, 200.0),
		new Vec3(-400.0, -400.0, 200.0),
		new Vec3(-240.0, -240.0, 200.0),
		new Vec3(-80.0, -80.0, 200.0),
		new Vec3(80.0, 80.0, 200.0),
		new Vec3(240.0, 240.0, 200.0),
		new Vec3(493.36746, 467.88876, 200.0),
		new Vec3(867.09937, 695.69257, 200.0),
		new Vec3(1626.22, 1090.26, 150.0),
		new Vec3(1140.4432, 1146.7141, 100.0),
		new Vec3(802.13116, 960.58325, 50.0),
		new Vec3(629.64246, 798.37146, 20.0),
		new Vec3(532.41113, 675.93604, 10.0),
		new Vec3(466.70566, 590.3576, 5.0),
		new Vec3(409.6342, 514.03174, 5.0),
		new Vec3(355.4573, 438.89584, 5.0),
	};
	
	
	private static final Vec3[] LOOP_BRZ = new Vec3[] {
		// loop
		new Vec3(409.6342, 514.03174, 5.0),
		new Vec3(355.4573, 438.89584, 5.0),
		new Vec3(317.29266, 392.75934, 5.0),
		new Vec3(277.0665, 345.7264, 5.0),
		new Vec3(234.94186, 299.32367, 5.0),
		new Vec3(193.04176, 255.09348, 5.0),
		new Vec3(130.45024, 196.80164, 5.0),
		new Vec3(67.16792, 149.34465, 5.0),
		new Vec3(0.11019015, 97.992035, 5.0),
		new Vec3(-66.35626, 44.114475, 5.0),
		new Vec3(-142.82806, -17.07383, 5.0),
		new Vec3(-230.262, -85.79602, 5.0000153),
		new Vec3(-319.40765, -153.6788, 4.999996),
		new Vec3(-403.71494, -218.66223, 5.0000076),
		new Vec3(-473.6577, -289.81778, 5.000061),
		new Vec3(-510.429, -375.48642, 9.999985),
		new Vec3(-482.33762, -455.04883, 15.0),
		new Vec3(-439.71225, -520.23413, 19.999847),
		new Vec3(-373.54547, -601.02185, 24.999939),
		new Vec3(-282.09488, -657.23157, 30.00003),
		new Vec3(-142.15686, -681.77264, 35.000137),
		new Vec3(-27.723007, -591.964, 40.0),
		new Vec3(54.282234, -497.8966, 40.0),
		new Vec3(94.77945, -393.581, 40.0),
		new Vec3(67.89926, -314.7067, 40.0),
		new Vec3(9.200182, -253.63564, 40.0),
		new Vec3(8.610334, -209.85616, 40.0),
		new Vec3(71.17022, -157.58075, 40.0),
		new Vec3(157.13065, -94.73383, 40.0),
		new Vec3(248.62645, -26.171307, 35.0),
		new Vec3(365.0304, 70.293655, 30.0),
		new Vec3(451.59625, 155.54239, 25.0),
		new Vec3(537.0101, 246.38788, 20.0),
		new Vec3(614.5287, 350.36176, 15.0),
		new Vec3(684.8281, 435.5135, 10.0),
		new Vec3(689.4689, 473.8651, 5.0),
		new Vec3(614.2683, 527.2068, 5.0),
		new Vec3(535.38586, 578.5085, 5.0),
		new Vec3(487.19922, 601.7118, 5.0),
		new Vec3(448.80707, 575.38544, 5.0),
	};
}
