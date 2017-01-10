package ch.fhnw.model;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.shader.base.AbstractShader;
import ch.fhnw.ether.render.variable.base.FloatUniform;
import ch.fhnw.ether.render.variable.base.Mat4FloatUniform;
import ch.fhnw.ether.render.variable.builtin.ColorArray;
import ch.fhnw.ether.render.variable.builtin.PositionArray;
import ch.fhnw.ether.render.variable.builtin.ViewUniformBlock;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.AbstractMaterial;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.scene.mesh.material.ICustomMaterial;
import ch.fhnw.ether.scene.mesh.material.IMaterial.MaterialAttribute;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class PowerUpShader {
    static final class PowerUpMaterial extends AbstractMaterial implements ICustomMaterial {
        private static class Shader extends AbstractShader {
            public Shader() {
                super(PowerUpShader.class, "custom_shader_example.custom_shader", "/shaders/custom_shader",
                        Primitive.TRIANGLES);
                addArray(new PositionArray());
                addArray(new ColorArray());

                addUniform(new ViewUniformBlock());
                addUniform(new FloatUniform("custom.red_gain", "redGain"));
            }
        }

        private final IShader shader = new Shader();
        private float redGain;

        public PowerUpMaterial(float redGain) {
            super(provide(new MaterialAttribute<Float>("custom.red_gain")), require(IGeometry.POSITION_ARRAY));
            this.redGain = redGain;
        }

        public float getRedGain() {
            return redGain;
        }

        public void setRedGain(float redGain) {
            this.redGain = redGain;
            updateRequest();
        }

        @Override
        public IShader getShader() {
            return shader;
        }

        @Override
        public Object[] getData() {
            return data(redGain);
        }
    }


    public static List<IMesh> makeColoredTriangle() {
        
        float[][] vertices = {
            {
                5, 5, 5, 
                0, 5, 0,
                5, 0, 0,
            },
            {
                5, 5, 5, 
                5, 10, 0,
                0, 5, 0,
            },
            {
                5, 5, 5, 
                10, 5, 0,
                5, 10, 0
            },
            {
                5, 5, 5, 
                5, 0, 0,
                10, 5, 0,
            },
        };
        float[][] colors = {
                {
                    1, 0, 0, 1, 
                    0, 1, 0, 1, 
                    0, 0, 1, 1
                },
                {
                    1, 0, 0, 1, 
                    0, 1, 0, 1, 
                    0, 0, 1, 1
                },
                {
                    1, 0, 0, 1, 
                    0, 1, 0, 1, 
                    0, 0, 1, 1
                },
                {
                    1, 0, 0, 1, 
                    0, 1, 0, 1, 
                    0, 0, 1, 1
                },
        };
        
        List<IMesh> meshes = new ArrayList<>();
        
        for(int i = 0; i < colors.length; i++) {
            DefaultGeometry g = DefaultGeometry.createVC(vertices[i], colors[i]);
            meshes.add(new DefaultMesh(Primitive.TRIANGLES, new PowerUpMaterial(2), g));
        }
        
        
        return meshes;
    }
}
