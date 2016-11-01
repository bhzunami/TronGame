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

package ch.fhnw.ether.examples.mapping;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.util.ArrayList;

import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.net.UDPServer;
import ch.fhnw.util.net.osc.IOSCHandler;
import ch.fhnw.util.net.osc.OSCServer;

// experimental and quite defunct at the moment...
@SuppressWarnings("unused")
public class GeometryServer {
	private MappingController controller;
	private Vec3 sunPosition = Vec3.Z;

	public GeometryServer(MappingController controller) {
		this.controller = controller;

		// runOSCScan(32000);
		runOSCSun(7777);
		runUDP(7000);
	}

	private void runOSCScan(int port) {
		try {
			OSCServer oscServerScan = new OSCServer(port, "224.0.1.0");
			oscServerScan.addHandler("/", new IOSCHandler() {
				@Override
				public Object[] handle(String[] address, int addrIdx, StringBuilder typeString, long timestamp, Object... args) {
					try {
						System.out.println("osc args: " + args.length);
						// /address[1]/address[2]/...
						// double someValue =
						// ((Number)args[0]).doubleValue();
						ArrayList<ArrayList<Number>> objects = new ArrayList<>();
						ArrayList<Number> object = new ArrayList<>();
						for (int i = 1; i < args.length;) {
							if (args[i] instanceof String) {
								objects.add(object);
								object = new ArrayList<>();
								i++;
							} else {
								object.add((Number) args[i]);
								object.add((Number) args[i + 1]);
								object.add((Number) args[i + 2]);
								i += 3;
							}
						}
						createGeometry(objects);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runOSCSun(int port) {
		sunPosition = controller.getLightPosition();
		try {
			controller.animate(new IEventScheduler.IAnimationAction() {
				@Override
				public void run(double time, double interval) {
					Vec3 p = controller.getLightPosition();
					Vec3 q = new Vec3(sunPosition.x - p.x, sunPosition.y - p.y, 0).scale(0.1f);
					controller.setLightPosition(q);
				}
			});

			OSCServer oscServerSun = new OSCServer(port, null);
			// oscserver = new OSCServer(port, "224.0.1.0");
			oscServerSun.addHandler("/", new IOSCHandler() {
				@Override
				public Object[] handle(String[] address, int addrIdx, StringBuilder typeString, long timestamp, Object... args) {
					try {
						// /address[1]/address[2]/...
						// double someValue =
						if (args.length >= 2) {
							sunPosition = new Vec3(((Float) args[0]).floatValue() / 100.0f * 5, ((Float) args[1]).floatValue() / 100.0f * 5, 0);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runUDP(int port) {
		try {
			UDPServer udpserver = new UDPServer(port, new UDPServer.UDPHandler() {
				@Override
				public void handle(DatagramPacket packet) {
					try {
						// format: /x,y,z,r,g,b;x,y,z,r,g,b;x,y,z,r,g,b/x,y,z,r,g,b;...
						System.out.println("got data");
						InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()),
								Charset.forName("UTF-8"));
						StringBuilder sb = new StringBuilder();
						for (int value; (value = input.read()) != -1;)
							sb.append((char) value);
						String message = sb.toString();
						String[] triangles = message.split("/");
						float[] vertices = new float[9 * (triangles.length - 1)];
						float[] colors = new float[9 * (triangles.length - 1)];
						int k = 0;
						for (int i = 1; i < triangles.length; ++i) {
							String[] v = triangles[i].split(";");
							// System.out.print("triangle: ");
							for (int j = 0; j < 3; ++j) {
								String[] vv = v[j].split(",");
								vertices[k] = Float.parseFloat(vv[0]) / 1000f;
								vertices[k + 1] = Float.parseFloat(vv[1]) / 1000f;
								vertices[k + 2] = Float.parseFloat(vv[2]) / 1000f;
								colors[k] = Float.parseFloat(vv[3]) / 256f;
								colors[k + 1] = Float.parseFloat(vv[4]) / 256f;
								colors[k + 2] = Float.parseFloat(vv[5]) / 256f;
								// System.out.print(vertices[k+0] + " " + vertices[k+1] + " " + vertices[k+2] + " | ");
								// System.out.print(colors[k+0] + " " + colors[k+1] + " " + colors[k+2] + " | ");
								k += 3;
							}
							// System.out.println();
						}
						if (vertices.length > 0) {
							System.out.println("udp: model updated");
							// ---> update scene
							controller.repaint();
						} else {
							System.out.println("udp: no data received");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createGeometry(ArrayList<ArrayList<Number>> objects) {
		ArrayList<float[]> geometry = new ArrayList<>();
		for (ArrayList<Number> object : objects) {
			int i = 0;
			int n = object.size();
			float z = object.get(2).floatValue() / 1000f;
			float[] triangles = new float[object.size() * 6];
			for (int j = 0; j < n; j += 3) {
				triangles[i++] = object.get(j).floatValue() / 1000f;
				triangles[i++] = object.get(j + 1).floatValue() / 1000f;
				triangles[i++] = 0;
				triangles[i++] = object.get((j + 3) % n).floatValue() / 1000f;
				triangles[i++] = object.get((j + 4) % n).floatValue() / 1000f;
				triangles[i++] = 0;
				triangles[i++] = object.get((j + 3) % n).floatValue() / 1000f;
				triangles[i++] = object.get((j + 4) % n).floatValue() / 1000f;
				triangles[i++] = z;

				triangles[i++] = object.get(j).floatValue() / 1000f;
				triangles[i++] = object.get(j + 1).floatValue() / 1000f;
				triangles[i++] = 0;
				triangles[i++] = object.get((j + 3) % n).floatValue() / 1000f;
				triangles[i++] = object.get((j + 4) % n).floatValue() / 1000f;
				triangles[i++] = z;
				triangles[i++] = object.get(j).floatValue() / 1000f;
				triangles[i++] = object.get(j + 1).floatValue() / 1000f;
				triangles[i++] = z;
			}
			geometry.add(triangles);
		}
		int n = 0;
		for (float[] triangles : geometry) {
			n += triangles.length;
		}
		int i = 0;
		float[] allTriangles = new float[n];
		for (float[] triangles : geometry) {
			System.arraycopy(triangles, 0, allTriangles, i, triangles.length);
			i += triangles.length;
		}
		if (allTriangles.length > 0) {
			// ---> send triangles / meshes to scene here
		} else {
			System.out.println("empty model");
		}
	}
}
