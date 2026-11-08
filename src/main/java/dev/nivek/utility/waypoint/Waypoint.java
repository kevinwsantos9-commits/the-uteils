package dev.nivek.utility.waypoint;

public class Waypoint {
    public static final String[] TYPES = {"Casa", "Vila", "Mina", "Fortaleza", "Portal", "Outro"};

    public String id;
    public String name;
    public String dimension;
    public int x;
    public int y;
    public int z;
    public int type;

    public Waypoint() {
    }

    public Waypoint(String id, String name, String dimension, int x, int y, int z, int type) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public String typeName() {
        return TYPES[Math.max(0, Math.min(TYPES.length - 1, type))];
    }

    public double distanceTo(double px, double py, double pz) {
        double dx = x + 0.5 - px, dy = y - py, dz = z + 0.5 - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
