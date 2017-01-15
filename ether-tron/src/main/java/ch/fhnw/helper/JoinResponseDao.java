package ch.fhnw.helper;

import java.util.Arrays;

public class JoinResponseDao {

    private String id;
    private float[][] blocks;
    private float[][] powerUps;

    public JoinResponseDao() {
    }

    public JoinResponseDao(String id, float[][] blocks, float[][] powerUps) {
        this.id = id;
        this.blocks = blocks;
        this.powerUps = powerUps;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float[][] getBlocks() {
        return blocks;
    }

    public void setBlocks(float[][] blocks) {
        this.blocks = blocks;
    }

    public float[][] getPowerUps() {
        return powerUps;
    }

    public void setPowerUps(float[][] powerUps) {
        this.powerUps = powerUps;
    }

    @Override
    public String toString() {
        return "JoinResponseDao [id=" + id + ", blocks=" + Arrays.toString(blocks) + ", powerUps="
                + Arrays.toString(powerUps) + "]";
    }

}
